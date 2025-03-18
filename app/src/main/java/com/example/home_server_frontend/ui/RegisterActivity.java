package com.example.home_server_frontend.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


import com.example.home_server_frontend.R;
import com.example.home_server_frontend.api.ApiClient;
import com.example.home_server_frontend.api.models.RegisterClientKeyRequest;
import com.example.home_server_frontend.api.models.RegisterClientKeyResponse;
import com.example.home_server_frontend.api.models.RegistrationRequest;
import com.example.home_server_frontend.api.models.RegistrationResponse;
import com.example.home_server_frontend.crypto.CryptoUtils;
import com.example.home_server_frontend.crypto.KeyManager;
import com.example.home_server_frontend.utils.PreferenceManager;

import org.json.JSONObject;

import java.security.PublicKey;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {
    private static final String TAG = "RegisterActivity";

    private EditText etUsername;
    private EditText etPassword;
    private EditText etConfirmPassword;
    private Button btnRegister;
    private TextView tvLoginLink;
    private ProgressBar progressBar;

    private KeyManager keyManager;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize views
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnRegister = findViewById(R.id.btn_register);
        tvLoginLink = findViewById(R.id.tv_login_link);
        progressBar = findViewById(R.id.progress_bar);

        // Initialize managers
        keyManager = new KeyManager(this);
        preferenceManager = new PreferenceManager(this);

        // Set up click listeners
        btnRegister.setOnClickListener(v -> attemptRegistration());
        tvLoginLink.setOnClickListener(v -> finish());
    }

    /**
     * Validate user input and attempt to register
     */
    private void attemptRegistration() {
        // Validate input
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save username for future use
        preferenceManager.setUsername(username);

        // Make sure we have the server's public key
        String serverPublicKeyPem = keyManager.getServerPublicKey();
        if (serverPublicKeyPem == null) {
            Toast.makeText(this, "Server key not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Register client key first
        registerClientKey(username, () -> {
            // After key registration, proceed with user registration
            registerUser(username, password);
        });
    }

    /**
     * Register client's public key with the server
     */
    private void registerClientKey(String username, Runnable onSuccess) {
        showProgress(true);

        String publicKeyPem = keyManager.getPublicKeyPem();
        RegisterClientKeyRequest request = new RegisterClientKeyRequest(username, publicKeyPem);

        ApiClient.getApiService(preferenceManager.getBaseUrl()).registerClientKey(request).enqueue(new Callback<RegisterClientKeyResponse>() {
            @Override
            public void onResponse(Call<RegisterClientKeyResponse> call, Response<RegisterClientKeyResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    // Key registered successfully, proceed with registration
                    onSuccess.run();
                } else {
                    showProgress(false);
                    String message = response.body() != null ? response.body().getMessage() : "Failed to register key";
                    Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<RegisterClientKeyResponse> call, Throwable t) {
                showProgress(false);
                Toast.makeText(RegisterActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Register a new user
     */
    private void registerUser(String username, String password) {
        try {
            // Get server's public key
            String serverPublicKeyPem = keyManager.getServerPublicKey();
            PublicKey serverPublicKey = CryptoUtils.publicKeyFromPem(serverPublicKeyPem);

            // Create registration payload
            JSONObject registrationJson = new JSONObject();
            registrationJson.put("username", username);
            registrationJson.put("password", password);

            // Encrypt the registration payload
            String encryptedData = CryptoUtils.encryptWithPublicKey(serverPublicKey, registrationJson.toString());

            // Create registration request
            RegistrationRequest registrationRequest = new RegistrationRequest(encryptedData);

            // Send registration request
            ApiClient.getApiService(preferenceManager.getBaseUrl()).register(registrationRequest).enqueue(new Callback<RegistrationResponse>() {
                @Override
                public void onResponse(Call<RegistrationResponse> call, Response<RegistrationResponse> response) {
                    showProgress(false);

                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        // Registration initiated, proceed to verification
                        handleRegistrationSuccess(response.body());
                    } else {
                        String message = response.body() != null ? response.body().getMessage() : "Registration failed";
                        Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<RegistrationResponse> call, Throwable t) {
                    showProgress(false);
                    Toast.makeText(RegisterActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            showProgress(false);
            Log.e(TAG, "Error during registration", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Handle successful registration response
     */
    private void handleRegistrationSuccess(RegistrationResponse response) {
        try {
            // Decrypt the verification code
            String decryptedJson = CryptoUtils.decryptHybridPackage(
                    response.getEncryptedVerificationCode(),
                    keyManager.getPrivateKey()
            );

            if (decryptedJson == null) {
                Toast.makeText(this, "Error decrypting verification code", Toast.LENGTH_SHORT).show();
                return;
            }

            // Parse the verification code
            JSONObject jsonData = new JSONObject(decryptedJson);
            String verificationCode = jsonData.getString("verificationCode");

            // Navigate to verification screen
            Intent intent = new Intent(this, VerifyActivity.class);
            intent.putExtra("verificationCode", verificationCode);
            intent.putExtra("username", preferenceManager.getUsername());
            startActivity(intent);

        } catch (Exception e) {
            Log.e(TAG, "Error processing registration response", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    /**
     * Show or hide the progress indicator
     */
    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!show);
        tvLoginLink.setEnabled(!show);
        etUsername.setEnabled(!show);
        etPassword.setEnabled(!show);
        etConfirmPassword.setEnabled(!show);
    }
}
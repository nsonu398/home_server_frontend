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
import com.example.home_server_frontend.api.models.KeyExchangeResponse;
import com.example.home_server_frontend.api.models.LoginRequest;
import com.example.home_server_frontend.api.models.LoginResponse;
import com.example.home_server_frontend.api.models.RegisterClientKeyRequest;
import com.example.home_server_frontend.api.models.RegisterClientKeyResponse;
import com.example.home_server_frontend.crypto.CryptoUtils;
import com.example.home_server_frontend.crypto.KeyManager;
import com.example.home_server_frontend.utils.PreferenceManager;

import org.json.JSONObject;

import java.security.PublicKey;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";

    private EditText etUsername;
    private EditText etPassword;
    private Button btnLogin;
    private TextView tvRegister;
    private ProgressBar progressBar;

    private KeyManager keyManager;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize views
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvRegister = findViewById(R.id.tv_register);
        progressBar = findViewById(R.id.progress_bar);

        // Initialize managers
        keyManager = new KeyManager(this);
        preferenceManager = new PreferenceManager(this);

        //check of auth token exists
        checkUserLoggedIn();

        // Set up click listeners
        btnLogin.setOnClickListener(v -> attemptLogin());
        tvRegister.setOnClickListener(v -> navigateToRegister());

        // Check if key pair exists, if not generate it
        if (!keyManager.generateKeyPairIfNeeded()) {
            Toast.makeText(this, "Failed to generate encryption keys", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Check if we have the server's public key
        if (keyManager.getServerPublicKey() == null) {
            // Go back to server settings if we don't have server key
            Toast.makeText(this, "Server connection not established", Toast.LENGTH_SHORT).show();
            navigateToServerSettings();
            return;
        }
    }

    private void checkUserLoggedIn() {
        if(preferenceManager.getAuthToken()!=null && !preferenceManager.getAuthToken().isEmpty()){
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
    }

    /**
     * Exchange public keys with the server
     */
    private void exchangeKeys() {
        showProgress(true);

        String baseUrl = preferenceManager.getBaseUrl();

        // Get server's public key
        ApiClient.getApiService(baseUrl).getServerPublicKey().enqueue(new Callback<KeyExchangeResponse>() {
            @Override
            public void onResponse(Call<KeyExchangeResponse> call, Response<KeyExchangeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Store the server's public key
                    keyManager.storeServerPublicKey(response.body().getPublicKey());

                    // Register our public key with the server
                    registerClientKey();
                } else {
                    showProgress(false);
                    Toast.makeText(LoginActivity.this, "Failed to get server key", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<KeyExchangeResponse> call, Throwable t) {
                showProgress(false);
                Toast.makeText(LoginActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Register our public key with the server
     */
    private void registerClientKey() {
        String username = preferenceManager.getUsername();
        if (username == null || username.isEmpty()) {
            // We don't have a username yet, that's fine for now
            showProgress(false);
            return;
        }

        String publicKeyPem = keyManager.getPublicKeyPem();
        RegisterClientKeyRequest request = new RegisterClientKeyRequest(username, publicKeyPem);

        String baseUrl = preferenceManager.getBaseUrl();
        ApiClient.getApiService(baseUrl).registerClientKey(request).enqueue(new Callback<RegisterClientKeyResponse>() {
            @Override
            public void onResponse(Call<RegisterClientKeyResponse> call, Response<RegisterClientKeyResponse> response) {
                showProgress(false);
                if (!response.isSuccessful() || response.body() == null || !response.body().isSuccess()) {
                    Log.w(TAG, "Failed to register client key: " +
                            (response.body() != null ? response.body().getMessage() : "Unknown error"));
                }
            }

            @Override
            public void onFailure(Call<RegisterClientKeyResponse> call, Throwable t) {
                showProgress(false);
                Log.e(TAG, "Network error registering client key: " + t.getMessage());
            }
        });
    }

    /**
     * Attempt to log in with the provided credentials
     */
    private void attemptLogin() {
        // Validate input
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter both username and password", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save username for future use
        preferenceManager.setUsername(username);

        // Make sure we have the server's public key
        String serverPublicKeyPem = keyManager.getServerPublicKey();
        if (serverPublicKeyPem == null) {
            Toast.makeText(this, "Server key not found, please restart the app", Toast.LENGTH_SHORT).show();
            navigateToServerSettings();
            return;
        }

        showProgress(true);

        try {
            // Get server's public key
            PublicKey serverPublicKey = CryptoUtils.publicKeyFromPem(serverPublicKeyPem);

            // Create login payload
            JSONObject loginJson = new JSONObject();
            loginJson.put("username", username);
            loginJson.put("password", password);

            // Encrypt the login payload
            String encryptedData = CryptoUtils.encryptWithPublicKey(serverPublicKey, loginJson.toString());

            // Create login request
            LoginRequest loginRequest = new LoginRequest(encryptedData,keyManager.getPublicKeyPem());

            // Send login request
            String baseUrl = preferenceManager.getBaseUrl();
            ApiClient.getApiService(baseUrl).login(loginRequest).enqueue(new Callback<LoginResponse>() {
                @Override
                public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                    showProgress(false);

                    if (response.isSuccessful() && response.body() != null) {
                        handleLoginResponse(response.body());
                    } else {
                        Toast.makeText(LoginActivity.this, "Login failed", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<LoginResponse> call, Throwable t) {
                    showProgress(false);
                    Toast.makeText(LoginActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            showProgress(false);
            Log.e(TAG, "Error during login", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Handle the encrypted login response
     */
    private void handleLoginResponse(LoginResponse response) {
        try {
            // Decrypt the hybrid encryption package
            String decryptedJson = CryptoUtils.decryptHybridPackage(
                    response.getEncryptedResponse(),
                    keyManager.getPrivateKey()
            );

            if (decryptedJson == null) {
                Toast.makeText(this, "Error decrypting response", Toast.LENGTH_SHORT).show();
                return;
            }

            // Parse the JSON
            JSONObject jsonResponse = new JSONObject(decryptedJson);
            boolean success = jsonResponse.getBoolean("success");

            if (success) {
                // Save auth token
                String token = jsonResponse.getString("token");
                preferenceManager.setAuthToken(token);

                // Navigate to main activity
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                String message = jsonResponse.optString("message", "Login failed");
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing login response", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    /**
     * Navigate to registration screen
     */
    private void navigateToRegister() {
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }

    /**
     * Navigate to server settings screen
     */
    private void navigateToServerSettings() {
        Intent intent = new Intent(this, ServerSettingsActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * Show or hide the progress indicator
     */
    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!show);
        tvRegister.setEnabled(!show);
    }
}
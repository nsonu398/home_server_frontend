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
import com.example.home_server_frontend.api.models.VerificationRequest;
import com.example.home_server_frontend.api.models.VerificationResponse;
import com.example.home_server_frontend.crypto.CryptoUtils;
import com.example.home_server_frontend.crypto.KeyManager;
import com.example.home_server_frontend.utils.PreferenceManager;

import org.json.JSONObject;

import java.security.PublicKey;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VerifyActivity extends AppCompatActivity {
    private static final String TAG = "VerifyActivity";

    private EditText etVerificationCode;
    private Button btnVerify;
    private ProgressBar progressBar;
    private TextView tvInstructions;

    private KeyManager keyManager;
    private PreferenceManager preferenceManager;

    private String encryptedVerificationCode;
    private String verificationCode;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify);

        // Initialize views
        etVerificationCode = findViewById(R.id.et_verification_code);
        btnVerify = findViewById(R.id.btn_verify);
        progressBar = findViewById(R.id.progress_bar);
        tvInstructions = findViewById(R.id.tv_verification_instructions);

        // Initialize managers
        keyManager = new KeyManager(this);
        preferenceManager = new PreferenceManager(this);

        // Get extras from intent
        encryptedVerificationCode = getIntent().getStringExtra("encryptedVerificationCode");
        username = getIntent().getStringExtra("username");

        if (encryptedVerificationCode == null || username == null) {
            Toast.makeText(this, "Missing verification data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Try to decrypt the verification code
        try {
            String decryptedData = CryptoUtils.decryptWithPrivateKey(
                    keyManager.getPrivateKey(), encryptedVerificationCode);

            JSONObject jsonData = new JSONObject(decryptedData);
            verificationCode = jsonData.getString("verificationCode");

            // For development purposes, pre-fill the verification code
            // In production, you'd remove this and let the user enter it
            etVerificationCode.setText(verificationCode);

        } catch (Exception e) {
            Log.e(TAG, "Error decrypting verification code", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        // Set up click listener
        btnVerify.setOnClickListener(v -> attemptVerification());
    }

    /**
     * Attempt to verify the registration with the entered code
     */
    private void attemptVerification() {
        String enteredCode = etVerificationCode.getText().toString().trim();

        if (enteredCode.isEmpty()) {
            Toast.makeText(this, "Please enter the verification code", Toast.LENGTH_SHORT).show();
            return;
        }

        // Make sure we have the server's public key
        String serverPublicKeyPem = keyManager.getServerPublicKey();
        if (serverPublicKeyPem == null) {
            Toast.makeText(this, "Server key not found", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgress(true);

        try {
            // Get server's public key
            PublicKey serverPublicKey = CryptoUtils.publicKeyFromPem(serverPublicKeyPem);

            // Create verification payload
            JSONObject verificationJson = new JSONObject();
            verificationJson.put("username", username);
            verificationJson.put("verificationCode", enteredCode);

            // Encrypt the verification payload
            String encryptedData = CryptoUtils.encryptWithPublicKey(
                    serverPublicKey, verificationJson.toString());

            // Create verification request
            VerificationRequest verificationRequest = new VerificationRequest(encryptedData);

            // Send verification request
            ApiClient.getApiService(preferenceManager.getBaseUrl()).verifyRegistration(verificationRequest)
                    .enqueue(new Callback<VerificationResponse>() {
                        @Override
                        public void onResponse(Call<VerificationResponse> call, Response<VerificationResponse> response) {
                            showProgress(false);

                            if (response.isSuccessful() && response.body() != null) {
                                handleVerificationResponse(response.body());
                            } else {
                                Toast.makeText(VerifyActivity.this, "Verification failed", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<VerificationResponse> call, Throwable t) {
                            showProgress(false);
                            Toast.makeText(VerifyActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

        } catch (Exception e) {
            showProgress(false);
            Log.e(TAG, "Error during verification", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Handle the encrypted verification response
     */
    private void handleVerificationResponse(VerificationResponse response) {
        try {
            // Decrypt the response
            String decryptedJson = CryptoUtils.decryptHybridPackage(
                    response.getEncryptedResponse(),
                    keyManager.getPrivateKey()
            );

            if (decryptedJson == null) {
                Toast.makeText(this, "Error decrypting response", Toast.LENGTH_SHORT).show();
                return;
            }

            // Parse JSON
            JSONObject jsonResponse = new JSONObject(decryptedJson);
            boolean success = jsonResponse.getBoolean("success");

            if (success) {
                Toast.makeText(this, "Registration successful! You can now login", Toast.LENGTH_SHORT).show();

                // Navigate back to login
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            } else {
                String message = jsonResponse.optString("message", "Verification failed");
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing verification response", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    /**
     * Show or hide the progress indicator
     */
    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnVerify.setEnabled(!show);
        etVerificationCode.setEnabled(!show);
    }
}
package com.example.home_server_frontend.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.home_server_frontend.R;
import com.example.home_server_frontend.api.ApiClient;
import com.example.home_server_frontend.api.models.KeyExchangeResponse;
import com.example.home_server_frontend.crypto.KeyManager;
import com.example.home_server_frontend.utils.Constants;
import com.example.home_server_frontend.utils.PreferenceManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ServerSettingsActivity extends AppCompatActivity {
    private static final String TAG = "ServerSettingsActivity";

    private EditText etServerIp;
    private EditText etServerPort;
    private Button btnConnect;
    private ProgressBar progressBar;

    private KeyManager keyManager;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_settings);

        // Initialize views
        etServerIp = findViewById(R.id.et_server_ip);
        etServerPort = findViewById(R.id.et_server_port);
        btnConnect = findViewById(R.id.btn_connect);
        progressBar = findViewById(R.id.progress_bar);

        // Initialize managers
        keyManager = new KeyManager(this);
        preferenceManager = new PreferenceManager(this);

        // Check if key pair exists, if not generate it
        if (!keyManager.generateKeyPairIfNeeded()) {
            Toast.makeText(this, "Failed to generate encryption keys", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Pre-fill saved server info if available
        String savedHost = preferenceManager.getServerHost();
        String savedPort = preferenceManager.getServerPort();

        if (savedHost != null && !savedHost.equals(Constants.DEFAULT_HOST)) {
            etServerIp.setText(savedHost);
        }

        if (savedPort != null && !savedPort.equals(Constants.DEFAULT_PORT)) {
            etServerPort.setText(savedPort);
        }

        // Set up click listener
        btnConnect.setOnClickListener(v -> connectToServer());

        // Check if user is already logged in
        if (preferenceManager.isLoggedIn()) {
            // Skip to main activity
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    /**
     * Attempt to connect to server and exchange keys
     */
    private void connectToServer() {
        // Validate input
        String host = etServerIp.getText().toString().trim();
        String port = etServerPort.getText().toString().trim();

        if (host.isEmpty()) {
            etServerIp.setError("Please enter server IP address");
            return;
        }

        if (!Patterns.IP_ADDRESS.matcher(host).matches() &&
                !host.equals("localhost") &&
                !host.equals("10.0.2.2")) {
            etServerIp.setError("Please enter a valid IP address");
            return;
        }

        if (port.isEmpty()) {
            port = Constants.DEFAULT_PORT;
            etServerPort.setText(port);
        }

        // Save server connection details
        preferenceManager.setServerHost(host);
        preferenceManager.setServerPort(port);

        showProgress(true);

        // Test connection by getting server's public key
        String baseUrl = preferenceManager.getBaseUrl();
        ApiClient.getApiService(baseUrl).getServerPublicKey().enqueue(new Callback<KeyExchangeResponse>() {
            @Override
            public void onResponse(Call<KeyExchangeResponse> call, Response<KeyExchangeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Store the server's public key
                    keyManager.storeServerPublicKey(response.body().getPublicKey());

                    // Connection successful, proceed to login screen
                    showProgress(false);
                    Toast.makeText(ServerSettingsActivity.this,
                            "Connected to server", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(ServerSettingsActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    showProgress(false);
                    Toast.makeText(ServerSettingsActivity.this,
                            "Failed to connect to server", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<KeyExchangeResponse> call, Throwable t) {
                showProgress(false);
                Toast.makeText(ServerSettingsActivity.this,
                        "Server connection error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Show or hide the progress indicator
     */
    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnConnect.setEnabled(!show);
        etServerIp.setEnabled(!show);
        etServerPort.setEnabled(!show);
    }
}
package com.example.home_server_frontend.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.home_server_frontend.R;
import com.example.home_server_frontend.api.ApiClient;
import com.example.home_server_frontend.utils.Constants;
import com.example.home_server_frontend.utils.PreferenceManager;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private TextView tvWelcome;
    private TextView tvServerAddress;
    private Button btnLogout;
    private Button btnServerSettings;
    private ProgressBar progressBar;
    private CardView cardViewServerInfo;

    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        tvWelcome = findViewById(R.id.tv_welcome);
        tvServerAddress = findViewById(R.id.tv_server_address);
        btnLogout = findViewById(R.id.btn_logout);
        btnServerSettings = findViewById(R.id.btn_server_settings);
        progressBar = findViewById(R.id.progress_bar);
        cardViewServerInfo = findViewById(R.id.card_server_info);

        // Initialize preference manager
        preferenceManager = new PreferenceManager(this);

        // Check if user is logged in
        if (!preferenceManager.isLoggedIn()) {
            navigateToLogin();
            return;
        }

        // Display welcome message
        String username = preferenceManager.getUsername();
        tvWelcome.setText("Welcome, " + (username != null ? username : "User") + "!");

        // Display server info
        String host = preferenceManager.getServerHost();
        String port = preferenceManager.getServerPort();
        tvServerAddress.setText("Connected to: " + host + ":" + port);

        // Set up logout button
        btnLogout.setOnClickListener(v -> logout());

        // Set up server settings button
        btnServerSettings.setOnClickListener(v -> navigateToServerSettings());
    }

    /**
     * Log out the user
     */
    private void logout() {
        showProgress(true);

        // Clear user data
        preferenceManager.clearUserData();

        // Navigate to login screen
        showProgress(false);
        navigateToLogin();
    }

    /**
     * Navigate to login screen
     */
    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Navigate to server settings screen
     */
    private void navigateToServerSettings() {
        Intent intent = new Intent(this, ServerSettingsActivity.class);
        startActivity(intent);
    }

    /**
     * Show or hide the progress indicator
     */
    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnLogout.setEnabled(!show);
        btnServerSettings.setEnabled(!show);
    }
}
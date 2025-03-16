package com.example.home_server_frontend.crypto;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.spec.MGF1ParameterSpec;

import javax.security.auth.x500.X500Principal;

/**
 * Manages RSA key generation, storage and retrieval using Android Keystore
 */
public class KeyManager {
    private static final String TAG = "KeyManager";
    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";
    private static final String KEY_ALIAS = "SecureAppRSAKey";
    private static final String SERVER_KEY_PREF = "server_public_key";

    private final Context context;

    public KeyManager(Context context) {
        this.context = context;
    }

    /**
     * Generate an RSA key pair if it doesn't exist
     * @return true if successful, false otherwise
     */
    public boolean generateKeyPairIfNeeded() {
        try {
            // Check if key already exists
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
            keyStore.load(null);

            if (keyStore.containsAlias(KEY_ALIAS)) {
                Log.d(TAG, "Key pair already exists");
                return true;
            }

            // Create key pair generator
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_RSA, KEYSTORE_PROVIDER);

            // Configure key parameters
            KeyGenParameterSpec keyGenParameterSpec = new KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                    .setRandomizedEncryptionRequired(true)
                    .setKeySize(2048)
                    .setCertificateSubject(new X500Principal("CN=SecureApp"))
                    .build();

            keyPairGenerator.initialize(keyGenParameterSpec);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            Log.d(TAG, "Key pair generated successfully");
            return true;

        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException |
                 IOException | NoSuchProviderException | InvalidAlgorithmParameterException e) {
            Log.e(TAG, "Error generating key pair", e);
            return false;
        }
    }

    /**
     * Get the device's public key in PEM format
     * @return Public key as a PEM string, or null if error
     */
    public String getPublicKeyPem() {
        try {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
            keyStore.load(null);

            PublicKey publicKey = keyStore.getCertificate(KEY_ALIAS).getPublicKey();
            byte[] publicKeyBytes = publicKey.getEncoded();
            String publicKeyBase64 = Base64.encodeToString(publicKeyBytes, Base64.NO_WRAP);

            return "-----BEGIN PUBLIC KEY-----\n" +
                    publicKeyBase64 +
                    "\n-----END PUBLIC KEY-----";

        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
            Log.e(TAG, "Error getting public key", e);
            return null;
        }
    }

    /**
     * Get the device's private key
     * @return PrivateKey object, or null if error
     */
    public PrivateKey getPrivateKey() {
        try {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
            keyStore.load(null);

            return (PrivateKey) keyStore.getKey(KEY_ALIAS, null);

        } catch (Exception e) {
            Log.e(TAG, "Error getting private key", e);
            return null;
        }
    }

    /**
     * Store the server's public key in preferences
     * @param serverPublicKey Server's public key in PEM format
     */
    public void storeServerPublicKey(String serverPublicKey) {
        context.getSharedPreferences("secure_app_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString(SERVER_KEY_PREF, serverPublicKey)
                .apply();

        Log.d(TAG, "Server public key stored");
    }

    /**
     * Get the server's public key from preferences
     * @return Server's public key in PEM format, or null if not found
     */
    public String getServerPublicKey() {
        return context.getSharedPreferences("secure_app_prefs", Context.MODE_PRIVATE)
                .getString(SERVER_KEY_PREF, null);
    }
}
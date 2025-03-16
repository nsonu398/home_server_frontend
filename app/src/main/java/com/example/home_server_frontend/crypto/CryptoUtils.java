package com.example.home_server_frontend.crypto;

import android.util.Base64;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

/**
 * Utilities for encryption and decryption operations
 */
public class CryptoUtils {
    private static final String TAG = "CryptoUtils";
    private static final String RSA_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";

    /**
     * Convert PEM formatted public key to PublicKey object
     * @param pemKey PEM formatted public key
     * @return PublicKey object
     */
    public static PublicKey publicKeyFromPem(String pemKey) {
        try {
            // Strip PEM header and footer
            String keyContent = pemKey
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");

            // Decode Base64
            byte[] keyBytes = Base64.decode(keyContent, Base64.DEFAULT);

            // Create PublicKey
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(keySpec);

        } catch (Exception e) {
            Log.e(TAG, "Error converting PEM to PublicKey", e);
            return null;
        }
    }

    /**
     * Encrypt data with a public key
     * @param publicKey PublicKey to encrypt with
     * @param data String data to encrypt
     * @return Base64 encoded encrypted data
     */
    public static String encryptWithPublicKey(PublicKey publicKey, String data) {
        try {
            Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);

            // Use OAEP with SHA-256
            OAEPParameterSpec oaepParams = new OAEPParameterSpec(
                    "SHA-256",
                    "MGF1",
                    MGF1ParameterSpec.SHA256,
                    PSource.PSpecified.DEFAULT
            );

            cipher.init(Cipher.ENCRYPT_MODE, publicKey, oaepParams);
            byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP);

        } catch (Exception e) {
            Log.e(TAG, "Error encrypting data", e);
            return null;
        }
    }

    /**
     * Decrypt data with a private key
     * @param privateKey PrivateKey to decrypt with
     * @param encryptedData Base64 encoded encrypted data
     * @return Decrypted string data
     */
    public static String decryptWithPrivateKey(PrivateKey privateKey, String encryptedData) {
        try {
            Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);

            // Use OAEP with SHA-256
            OAEPParameterSpec oaepParams = new OAEPParameterSpec(
                    "SHA-256",
                    "MGF1",
                    MGF1ParameterSpec.SHA256,
                    PSource.PSpecified.DEFAULT
            );

            cipher.init(Cipher.DECRYPT_MODE, privateKey, oaepParams);
            byte[] encryptedBytes = Base64.decode(encryptedData, Base64.NO_WRAP);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);

        } catch (Exception e) {
            Log.e(TAG, "Error decrypting data", e);
            return null;
        }
    }
}
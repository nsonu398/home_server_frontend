package com.example.home_server_frontend.crypto;

import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import com.example.home_server_frontend.api.models.HybridEncryptionPackage;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;

/**
 * Utilities for encryption and decryption operations
 */
public class CryptoUtils {
    private static final String TAG = "CryptoUtils";
    private static final String RSA_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    private static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_AUTH_TAG_LENGTH = 128; // in bits

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
     * Convert PKCS8 formatted private key to PrivateKey object
     * @param pkcs8Key PKCS8 formatted private key
     * @return PrivateKey object
     */
    public static PrivateKey privateKeyFromPkcs8(String pkcs8Key) {
        try {
            // Strip PEM header and footer
            String keyContent = pkcs8Key
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");

            // Decode Base64
            byte[] keyBytes = Base64.decode(keyContent, Base64.DEFAULT);

            // Create PrivateKey
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(keySpec);

        } catch (Exception e) {
            Log.e(TAG, "Error converting PKCS8 to PrivateKey", e);
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

    /**
     * Low-level RSA decryption for byte arrays
     * @param privateKey PrivateKey to decrypt with
     * @param data Encrypted bytes
     * @return Decrypted bytes
     */
    public static byte[] decryptWithRSA(PrivateKey privateKey, byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
            OAEPParameterSpec oaepParams = new OAEPParameterSpec(
                    "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT
            );
            cipher.init(Cipher.DECRYPT_MODE, privateKey, oaepParams);
            return cipher.doFinal(data);
        } catch (Exception e) {
            Log.e(TAG, "Error in low-level RSA decryption", e);
            return null;
        }
    }

    /**
     * Generate a random AES key
     * @return AES SecretKey
     */
    public static SecretKey generateAESKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            return keyGen.generateKey();
        } catch (Exception e) {
            Log.e(TAG, "Error generating AES key", e);
            return null;
        }
    }

    /**
     * Encrypt data with AES-GCM
     * @param key AES key
     * @param data String data to encrypt
     * @return Object containing IV, encrypted data, and auth tag
     */
    public static AESEncryptionResult encryptWithAES(SecretKey key, String data) {
        try {
            // Generate random IV
            byte[] iv = new byte[12]; // 12 bytes IV for GCM
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            // Initialize cipher
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_AUTH_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);

            // Encrypt
            byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

            // Extract auth tag (last 16 bytes)
            int tagLength = GCM_AUTH_TAG_LENGTH / 8; // Convert from bits to bytes
            byte[] ciphertext = Arrays.copyOfRange(encryptedBytes, 0, encryptedBytes.length - tagLength);
            byte[] authTag = Arrays.copyOfRange(encryptedBytes, encryptedBytes.length - tagLength, encryptedBytes.length);

            return new AESEncryptionResult(
                    Base64.encodeToString(ciphertext, Base64.NO_WRAP),
                    Base64.encodeToString(iv, Base64.NO_WRAP),
                    Base64.encodeToString(authTag, Base64.NO_WRAP)
            );
        } catch (Exception e) {
            Log.e(TAG, "Error encrypting with AES", e);
            return null;
        }
    }

    /**
     * Decrypt data with AES-GCM
     * @param key AES key
     * @param ivBase64 Base64 encoded IV
     * @param encryptedDataBase64 Base64 encoded encrypted data
     * @param authTagBase64 Base64 encoded authentication tag
     * @return Decrypted string data
     */
    public static String decryptWithAES(SecretKey key, String ivBase64, String encryptedDataBase64, String authTagBase64) {
        try {
            // Decode from Base64
            byte[] iv = Base64.decode(ivBase64, Base64.NO_WRAP);
            byte[] encryptedData = Base64.decode(encryptedDataBase64, Base64.NO_WRAP);
            byte[] authTag = Base64.decode(authTagBase64, Base64.NO_WRAP);

            // Combine ciphertext and auth tag
            byte[] combined = new byte[encryptedData.length + authTag.length];
            System.arraycopy(encryptedData, 0, combined, 0, encryptedData.length);
            System.arraycopy(authTag, 0, combined, encryptedData.length, authTag.length);

            // Initialize cipher
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_AUTH_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);

            // Decrypt
            byte[] decryptedData = cipher.doFinal(combined);
            return new String(decryptedData, StandardCharsets.UTF_8);
        } catch (Exception e) {
            Log.e(TAG, "Error decrypting with AES", e);
            return null;
        }
    }

    /**
     * Decrypt a hybrid encryption package
     * @param encryptedPackage Hybrid encryption package from server
     * @param privateKey Client's private key
     * @return Decrypted string data
     */
    public static String decryptHybridPackage(HybridEncryptionPackage encryptedPackage, PrivateKey privateKey) {
        try {
            // Decrypt the AES key with RSA
            byte[] encryptedKeyBytes = Base64.decode(encryptedPackage.getEncryptedKey(), Base64.NO_WRAP);
            byte[] aesKeyBytes = decryptWithRSA(privateKey, encryptedKeyBytes);

            if (aesKeyBytes == null) {
                Log.e(TAG, "Failed to decrypt AES key");
                return null;
            }

            // Create AES key from bytes
            SecretKey aesKey = new SecretKeySpec(aesKeyBytes, "AES");

            // Decrypt the data with AES
            return decryptWithAES(
                    aesKey,
                    encryptedPackage.getIv(),
                    encryptedPackage.getEncryptedData(),
                    encryptedPackage.getAuthTag()
            );
        } catch (Exception e) {
            Log.e(TAG, "Error decrypting hybrid package", e);
            return null;
        }
    }

    /**
     * Result class for AES encryption
     */
    public static class AESEncryptionResult {
        private final String encryptedData;
        private final String iv;
        private final String authTag;

        public AESEncryptionResult(String encryptedData, String iv, String authTag) {
            this.encryptedData = encryptedData;
            this.iv = iv;
            this.authTag = authTag;
        }

        public String getEncryptedData() { return encryptedData; }
        public String getIv() { return iv; }
        public String getAuthTag() { return authTag; }
    }
}
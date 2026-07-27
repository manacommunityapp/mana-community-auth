package com.manacommunity.auth.service;

import com.manacommunity.common.exception.EncryptionException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * AES-256-GCM field-level encryption for PII (government IDs, etc.).
 * Uses the same master password as {@code EncryptablePropertyProcessor}.
 */
@Slf4j
@Service
public class FieldEncryptionService {

    private static final int SALT_LENGTH = 16;
    private static final int IV_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final int KEY_LENGTH = 256;
    private static final int ITERATIONS = 65536;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${jasypt.encryptor.password:#{null}}")
    private String masterPassword;

    @PostConstruct
    void validate() {
        if (masterPassword == null || masterPassword.isBlank()) {
            String envPw = System.getenv("JASYPT_ENCRYPTOR_PASSWORD");
            if (envPw != null && !envPw.isBlank()) {
                masterPassword = envPw;
            } else {
                log.warn("No encryption master password configured — PII field encryption is disabled. "
                       + "Set JASYPT_ENCRYPTOR_PASSWORD for production use.");
            }
        }
    }

    public boolean isEnabled() {
        return masterPassword != null && !masterPassword.isBlank();
    }

    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) return plaintext;
        if (!isEnabled()) {
            throw new EncryptionException(
                    "Cannot encrypt PII: no master password configured. Set JASYPT_ENCRYPTOR_PASSWORD.");
        }
        try {
            byte[] salt = new byte[SALT_LENGTH];
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(salt);
            secureRandom.nextBytes(iv);

            SecretKey key = deriveKey(masterPassword, salt);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[SALT_LENGTH + IV_LENGTH + ciphertext.length];
            System.arraycopy(salt, 0, combined, 0, SALT_LENGTH);
            System.arraycopy(iv, 0, combined, SALT_LENGTH, IV_LENGTH);
            System.arraycopy(ciphertext, 0, combined, SALT_LENGTH + IV_LENGTH, ciphertext.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (EncryptionException e) {
            throw e;
        } catch (Exception e) {
            throw new EncryptionException("PII encryption failed", e);
        }
    }

    public String decrypt(String base64Cipher) {
        if (base64Cipher == null || base64Cipher.isBlank()) return base64Cipher;
        if (!isEnabled()) {
            throw new EncryptionException(
                    "Cannot decrypt PII: no master password configured. Set JASYPT_ENCRYPTOR_PASSWORD.");
        }
        try {
            byte[] combined = Base64.getDecoder().decode(base64Cipher);
            if (combined.length < SALT_LENGTH + IV_LENGTH + 1) {
                throw new EncryptionException("Ciphertext too short or corrupted");
            }

            byte[] salt = new byte[SALT_LENGTH];
            byte[] iv = new byte[IV_LENGTH];
            byte[] ciphertext = new byte[combined.length - SALT_LENGTH - IV_LENGTH];
            System.arraycopy(combined, 0, salt, 0, SALT_LENGTH);
            System.arraycopy(combined, SALT_LENGTH, iv, 0, IV_LENGTH);
            System.arraycopy(combined, SALT_LENGTH + IV_LENGTH, ciphertext, 0, ciphertext.length);

            SecretKey key = deriveKey(masterPassword, salt);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (EncryptionException e) {
            throw e;
        } catch (Exception e) {
            throw new EncryptionException("PII decryption failed", e);
        }
    }

    private static SecretKey deriveKey(String password, byte[] salt) throws Exception {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }
}

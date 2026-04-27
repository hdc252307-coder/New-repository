package com.example.demo;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Converter
public class StringEncryptConverter implements AttributeConverter<String, String> {

    private static final String ALGO = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;

    /*
     * 本番は環境変数 APP_CRYPTO_KEY を必ず設定する。
     * 未設定時は開発用デフォルト値で起動し、既存環境を壊さないようにする。
     */
    private static final SecretKeySpec KEY = buildKey();
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public String convertToDatabaseColumn(String attribute) 

    
    {
        if (attribute == null || attribute.isBlank()) return attribute;
        try 
        {
            byte[] iv = new byte[IV_LENGTH];
            RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, KEY, new GCMParameterSpec(TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));

            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(payload);
        } catch (Exception e) 
        {
            throw new IllegalStateException("Failed to encrypt column", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) 

    
    {
        if (dbData == null || dbData.isBlank()) return dbData;
        try 
        {
            byte[] payload = Base64.getDecoder().decode(dbData);
            if (payload.length <= IV_LENGTH) return dbData;
            byte[] iv = Arrays.copyOfRange(payload, 0, IV_LENGTH);
            byte[] encrypted = Arrays.copyOfRange(payload, IV_LENGTH, payload.length);

            Cipher cipher = Cipher.getInstance(ALGO);
            cipher.init(Cipher.DECRYPT_MODE, KEY, new GCMParameterSpec(TAG_LENGTH, iv));
            byte[] plain = cipher.doFinal(encrypted);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) 
        {
            /*
             * 既存データが平文で保存されている環境でも即時移行できるように、
             * 復号失敗時はいったん平文として扱う（後続更新で暗号化保存される）。
             */
            return dbData;
        }
    }

    private static SecretKeySpec buildKey() 

    

    {
        try 
        {
            String raw = System.getenv().getOrDefault("APP_CRYPTO_KEY", "dev-only-default-change-me");
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] key = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(key, "AES");
        } catch (Exception e) 
        {
            throw new IllegalStateException("Failed to initialize encryption key", e);
        }
    }
}

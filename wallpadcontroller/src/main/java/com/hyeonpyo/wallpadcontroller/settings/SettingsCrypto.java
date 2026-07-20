package com.hyeonpyo.wallpadcontroller.settings;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;

import org.springframework.stereotype.Component;

@Component
public class SettingsCrypto {
    private static final SecureRandom RANDOM = new SecureRandom();
    private byte[] key;

    public void initialize(String encodedKey) { key = Base64.getDecoder().decode(encodedKey); }
    public static String newKey() { byte[] value = new byte[32]; RANDOM.nextBytes(value); return Base64.getEncoder().encodeToString(value); }
    public String encrypt(String plain) {
        if (plain == null || plain.isEmpty()) return "";
        try { byte[] iv = new byte[12]; RANDOM.nextBytes(iv); Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE, new javax.crypto.spec.SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv)); byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8)); byte[] packed = new byte[iv.length + encrypted.length]; System.arraycopy(iv, 0, packed, 0, iv.length); System.arraycopy(encrypted, 0, packed, iv.length, encrypted.length); return Base64.getEncoder().encodeToString(packed); } catch (Exception e) { throw new IllegalStateException("설정 암호화 실패", e); }
    }
    public String decrypt(String encoded) {
        if (encoded == null || encoded.isEmpty()) return "";
        try { byte[] packed = Base64.getDecoder().decode(encoded); byte[] iv = java.util.Arrays.copyOfRange(packed, 0, 12); byte[] encrypted = java.util.Arrays.copyOfRange(packed, 12, packed.length); Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.DECRYPT_MODE, new javax.crypto.spec.SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv)); return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8); } catch (Exception e) { throw new IllegalStateException("설정 복호화 실패", e); }
    }
}

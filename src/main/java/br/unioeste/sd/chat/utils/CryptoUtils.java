package br.unioeste.sd.chat.utils;

import lombok.experimental.UtilityClass;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.util.Base64;

@UtilityClass
public class CryptoUtils {
    public KeyPair generateRSAKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");

        keyGen.initialize(2048);

        return keyGen.generateKeyPair();
    }

    public String encryptRSA(byte[] data, PublicKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");

        cipher.init(Cipher.ENCRYPT_MODE, key);

        return Base64.getEncoder().encodeToString(cipher.doFinal(data));
    }

    public byte[] decryptRSA(String encryptedData, PrivateKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");

        cipher.init(Cipher.DECRYPT_MODE, key);

        return cipher.doFinal(Base64.getDecoder().decode(encryptedData));
    }

    public SecretKey generateAESKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");

        keyGen.init(256);

        return keyGen.generateKey();
    }

    public String encryptAES(String plaintext, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);

        cipher.init(Cipher.ENCRYPT_MODE, key, spec);

        byte[] cipherText = cipher.doFinal(plaintext.getBytes());

        return Base64.getEncoder().encodeToString(cipherText);
    }

    public String decryptAES(String ciphertext, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);

        cipher.init(Cipher.DECRYPT_MODE, key, spec);

        byte[] plainText = cipher.doFinal(Base64.getDecoder().decode(ciphertext));

        return new String(plainText);
    }

    public byte[] generateIV() {
        byte[] iv = new byte[12];
        SecureRandom random = new SecureRandom();

        random.nextBytes(iv);

        return iv;
    }

    public SecretKey bytesToAES(byte[] keyBytes) {
        return new SecretKeySpec(keyBytes, "AES");
    }
}

package ru.justagod.vk.backend.db;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Random;

public class PasswordsManager {

    public static final byte[] SALT = new byte[16];

    static {
        new Random(777).nextBytes(SALT);
    }

    public static String hashed(String password) {
        try {
            KeySpec spec = new PBEKeySpec(password.toCharArray(), SALT, 65536, 128);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            return new BigInteger(1, factory.generateSecret(spec).getEncoded()).toString(16);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

}

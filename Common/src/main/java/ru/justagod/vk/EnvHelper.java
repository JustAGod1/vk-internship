package ru.justagod.vk;

public class EnvHelper {
    public static int intEnv(String key, int def) {
        int result = def;
        String override = System.getenv(key);
        if (override != null) {
            try {
                result = Integer.parseInt(override);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }
    public static String stringEnv(String key, String def) {
        String result = def;
        String override = System.getenv(key);
        if (override != null) {
            try {
                result = override;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }
}

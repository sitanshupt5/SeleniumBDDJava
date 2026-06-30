package com.automation.ui.utilities;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class ReadProperty {

    // Parsed as section.key -> value
    private static final Map<String, String> CONFIG = new HashMap<>();

    static {
        try (InputStream is = ReadProperty.class.getClassLoader().getResourceAsStream("config.ini")) {
            if (is != null) {
                parseIni(is);
            }
        } catch (Exception e) {
            // config not found - fall back to defaults
        }
    }

    private static void parseIni(InputStream is) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            String currentSection = "";
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) {
                    continue;
                }
                if (line.startsWith("[") && line.endsWith("]")) {
                    currentSection = line.substring(1, line.length() - 1).trim().toLowerCase();

                } else if (line.contains("=")) {
                    int eq = line.indexOf('=');
                    String key = line.substring(0, eq).trim().toLowerCase();
                    String value = line.substring(eq + 1).trim();
                    CONFIG.put(currentSection + "." + key, value);
                }
            }
        }
    }

    private static String get(String section, String key) {
        return CONFIG.get(section.toLowerCase() + "." + key.toLowerCase());
    }

    public static String getEnv(String defaultVal) {
        String sys = System.getProperty("env");
        if (sys != null && !sys.isEmpty()) {
            return sys;
        }
        String val = get("environment", "type");
        return val != null ? val : defaultVal;
    }

    public static String getApplicationUrl() {
        String env = getEnv("qa");
        return get("common_info", env + "_baseurl");
    }

    public static String getApplicationUsername() {
        String env = getEnv("qa");
        return get("common_info", env + "_username");
    }

    public static String getApplicationPassword() {
        String env = getEnv("qa");
        return get("common_info", env + "_password");
    }

    public static int getWaitTime10Sec() {
        try {
            String val = get("wait", "sec10");
            return val != null ? Integer.parseInt(val.trim()) : 10;
        } catch (Exception e) {
            return 10;
        }
    }

    public static int getWaitTime5Sec() {
        try {
            String val = get("wait", "sec5");
            return val != null ? Integer.parseInt(val.trim()) : 5;
        } catch (Exception e) {
            return 5;
        }
    }

    public static String getBrowser(String defaultVal) {
        String sys = System.getProperty("browser");
        if (sys != null && !sys.isEmpty()) {
            return sys;
        }
        String val = get("driver configuration", "browser");
        return val != null ? val : defaultVal;
    }

    public static boolean getHeadless(boolean defaultVal) {
        String sys = System.getProperty("headless");
        if (sys != null && !sys.isEmpty()) {
            return sys.trim().toLowerCase().matches("true|yes|y|on|1");
        }
        String val = get("driver configuration", "headless");
        if (val != null) {
            return val.trim().toLowerCase().matches("true|yes|y|on|1");
        }
        return defaultVal;
    }
}
package com.playgamesinteractive.lushvotes.util;

import java.io.InputStream;
import java.util.Properties;

public final class VersionInfo {

    private static final String COMMIT;
    private static final String BUILT;

    static {
        String commit = "unknown";
        String built = "unknown";
        try (InputStream in = VersionInfo.class.getClassLoader().getResourceAsStream("version.properties")) {
            if (in != null) {
                Properties properties = new Properties();
                properties.load(in);
                commit = properties.getProperty("commit", commit);
                built = properties.getProperty("built", built);
            }
        } catch (Exception ignored) {
        }
        COMMIT = commit;
        BUILT = built;
    }

    private VersionInfo() {
    }

    public static String commit() {
        return COMMIT;
    }

    public static String built() {
        return BUILT;
    }
}

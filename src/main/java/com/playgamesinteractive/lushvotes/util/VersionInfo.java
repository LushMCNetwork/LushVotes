package com.playgamesinteractive.lushvotes.util;

import java.io.InputStream;
import java.util.Properties;

/**
 * Reads version.properties, generated at build time from the current git
 * commit (see pom.xml's buildnumber-maven-plugin) - so a running proxy can
 * report exactly which commit it was built from instead of leaving it to
 * guesswork whether a deploy is actually current. Same convention as
 * LushShop/LushRaft's VersionInfo, adapted for Velocity: there's no
 * {@code Plugin} object to call {@code getResource()} on here, so this
 * reads straight off the classloader instead.
 */
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
            // Falls back to "unknown" - a dev build run straight from an IDE
            // isn't going through the normal Maven packaging that generates this file.
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

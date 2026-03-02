package io.github.AaditS22.asmsimulator.frontend.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// DISCLAIMER: This class was largely written with the help of LLMs
public class CodePersistence {

    private static final String APP_NAME  = "AsmSimulator";
    private static final String FILE_NAME = "draft.asm";

    private static Path resolveStorageDir() {
        String os = System.getProperty("os.name", "").toLowerCase();

        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData != null) return Paths.get(appData, APP_NAME);
        } else if (os.contains("mac")) {
            return Paths.get(System.getProperty("user.home"),
                    "Library", "Application Support", APP_NAME);
        }
        // Linux / fallback
        String xdg = System.getenv("XDG_DATA_HOME");
        if (xdg != null && !xdg.isBlank()) return Paths.get(xdg, APP_NAME);
        return Paths.get(System.getProperty("user.home"), ".local", "share", APP_NAME);
    }

    public static void save(String code) {
        try {
            Path dir = resolveStorageDir();
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(FILE_NAME), code, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    public static String load() {
        try {
            Path file = resolveStorageDir().resolve(FILE_NAME);
            if (Files.exists(file)) return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
        return null;
    }
}
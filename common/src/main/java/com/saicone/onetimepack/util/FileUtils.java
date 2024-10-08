package com.saicone.onetimepack.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class FileUtils {

    FileUtils() {
    }

    @NotNull
    public static File getFile(@NotNull File folder, @NotNull String path) {
        File file = folder;
        for (String s : path.split("/")) {
            file = new File(file, s);
        }
        return file;
    }

    @Nullable
    public static InputStream getResource(@NotNull String path) {
        return getResource(FileUtils.class, path);
    }

    @Nullable
    public static InputStream getResource(@NotNull Class<?> clazz, @NotNull String path) {
        return clazz.getClassLoader().getResourceAsStream(path);
    }

    @Nullable
    public static File saveResource(@NotNull File folder, @NotNull String path, boolean replace) {
        return saveResource(FileUtils.class, folder, path, replace);
    }

    @Nullable
    public static File saveResource(@NotNull Class<?> clazz, @NotNull File folder, @NotNull String path, boolean replace) {
        final InputStream input = getResource(clazz, path);
        if (input != null) {
            final File file = getFile(folder, path);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            if (file.exists() && !replace) {
                return file;
            }
            try {
                Files.copy(input, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                return file;
            } catch (IOException ignored) { }
        }
        return null;
    }

    @Nullable
    public static String readFromFile(@NotNull File file) {
        try {
            return String.join("", Files.readAllLines(file.toPath()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Nullable
    public static String readFromUrl(@NotNull String url) {
        try {
            return readFromUrl(new URL(url));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Nullable
    public static String readFromUrl(@NotNull URL url) {
        try (InputStream in = url.openStream(); BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            final StringBuilder out = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line);
            }

            return out.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}

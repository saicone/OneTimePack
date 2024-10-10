package com.saicone.onetimepack.module;

import com.saicone.onetimepack.util.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class TinySettings {

    private final String fileName;
    private final Map<String, Object> data;

    public TinySettings(@NotNull String fileName) {
        this(fileName, new LinkedHashMap<>());
    }

    public TinySettings(@NotNull String fileName, @NotNull Map<String, Object> data) {
        this.fileName = fileName;
        this.data = data;
    }

    @NotNull
    public String getFileName() {
        return fileName;
    }

    @NotNull
    public Map<String, Object> getData() {
        return data;
    }

    @Nullable
    public Object get(@NotNull String path) {
        return get(path.toLowerCase().split("\\."));
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public Object get(@NotNull String[] path) {
        if (path.length < 1) {
            return null;
        }
        if (path.length == 1) {
            return data.get(path[0]);
        }
        Map<String, Object> map = data;
        for (int i = 0; i < path.length; i++) {
            if (i + 1 >= path.length) {
                break;
            }
            final Object object = map.get(path[i]);
            if (object instanceof Map) {
                map = (Map<String, Object>) object;
            } else {
                return null;
            }
        }
        return map.get(path[path.length - 1]);
    }

    @Nullable
    public String getString(@NotNull String path) {
        final Object object = get(path);
        return object != null ? String.valueOf(object) : null;
    }

    @NotNull
    public String getString(@NotNull String path, @NotNull String def) {
        final Object object = get(path);
        return object != null ? String.valueOf(object) : def;
    }

    public int getInt(@NotNull String path) {
        return parseInt(get(path), -1);
    }

    public int getInt(@NotNull String path, int def) {
        return parseInt(get(path), def);
    }

    public boolean getBoolean(@NotNull String path) {
        return String.valueOf(get(path)).equalsIgnoreCase("true");
    }

    public boolean getBoolean(@NotNull String path, boolean def) {
        final Object object = get(path);
        if (object == null) {
            return def;
        }
        return String.valueOf(object).equalsIgnoreCase("true");
    }

    public void load(@NotNull File folder) {
        data.clear();
        final File file = FileUtils.saveResource(folder, fileName, false);
        if (file != null) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                final Object result = read(reader);
                if (result instanceof Map<?,?> map) {
                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        data.put(String.valueOf(entry.getKey()), entry.getValue());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void save(@NotNull File file) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            write(writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Nullable
    public abstract Object read(@NotNull Reader reader) throws IOException;

    public abstract void write(@NotNull Writer writer) throws IOException;

    private int parseInt(@Nullable Object object, int def) {
        if (object == null) {
            return def;
        }
        try {
            return Integer.parseInt(String.valueOf(object));
        } catch (NumberFormatException e) {
            return def;
        }
    }
}

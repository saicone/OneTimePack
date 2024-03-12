package com.saicone.onetimepack.module;

import com.saicone.onetimepack.util.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TinySettings {

    private final String fileName;

    private final Map<String, Object> paths = new HashMap<>();

    public TinySettings(@NotNull String fileName) {
        this.fileName = fileName;
    }

    @NotNull
    public String getFileName() {
        return fileName;
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
            return paths.get(path[0]);
        }
        Map<String, Object> map = paths;
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

    public Map<String, Object> getPaths() {
        return paths;
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
        paths.clear();
        final File file = FileUtils.saveResource(folder, fileName, false);
        if (file != null) {
            try {
                final List<String> lines = Files.readAllLines(file.toPath());
                read(lines, paths, 0, 0);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private int read(@NotNull List<String> lines, @NotNull Map<String, Object> paths, int index, int startSpaces) throws IOException {
        for (int i = index; i < lines.size(); i++) {
            final String rawLine = lines.get(i);
            final String line = rawLine.trim();
            final int charIndex = separatorIndex(line);
            if (charIndex < 0) {
                continue;
            }
            final int lineSpaces = countSpaces(rawLine);
            if (lineSpaces < startSpaces) {
                return i - index;
            }

            // Build key
            final String key = line.substring(0, charIndex).toLowerCase();
            // Build value
            final String value = readValue(line.substring(charIndex + 1).trim());

            // Check if the value is a Map
            if (value.isEmpty() && i + 1 < lines.size()) {
                boolean isMap = false;
                for (int i1 = i + 1; i1 < lines.size(); i1++) {
                    final String s = lines.get(i1);
                    if (separatorIndex(s.trim()) < 0) {
                        // Not value line
                        continue;
                    }
                    // Sub value of map
                    if (countSpaces(s) > lineSpaces) {
                        isMap = true;
                        final Map<String, Object> map = new HashMap<>();
                        i = i1 + read(lines, map, i1, lineSpaces + 1) - 1;
                        paths.put(key, map);
                    }
                    break;
                }
                if (isMap) {
                    continue;
                }
            }

            // Add simple value to current paths
            if (value.equals("{}")) {
                paths.put(key, new HashMap<>());
            } else {
                paths.put(key, value);
            }
        }
        return lines.size() - 1;
    }

    private String readValue(@NotNull String value) {
        final char[] chars = value.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '#') {
                return value.substring(0, i);
            }
        }
        return value;
    }

    private int separatorIndex(@NotNull String line) {
        final int separatorIndex;
        if (line.isEmpty() || line.charAt(0) == '#' || (separatorIndex = line.indexOf(':')) < 1) {
            return -1;
        }
        return separatorIndex;
    }

    private int countSpaces(@NotNull String s) {
        int spaces = 0;
        for (char c : s.toCharArray()) {
            if (c != ' ') {
                break;
            }
            spaces++;
        }
        return spaces;
    }

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

package com.saicone.onetimepack.module;

import com.saicone.onetimepack.util.InfoReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class TinyYaml extends TinySettings {

    private static final int ROOT = 0;
    private static final int MAP = 1;
    private static final int LIST = 2;
    private static final int SCALAR = 3;
    private static final int LITERAL_BLOCK_SCALAR = 4;

    public TinyYaml(@NotNull String fileName) {
        super(fileName);
    }

    public TinyYaml(@NotNull String fileName, @NotNull Map<String, Object> data) {
        super(fileName, data);
    }

    public @Nullable Object read(@NotNull Reader reader) throws IOException {
        return read(new InfoReader(reader, s -> s.startsWith("#")), 0, ROOT);
    }

    @Nullable
    protected Object read(@NotNull InfoReader reader, int prefix, int state) throws IOException {
        if (state == ROOT || state == MAP) {
            final Map<String, Object> map = new LinkedHashMap<>();
            String line;
            while ((line = reader.walk()) != null) {
                final int spaces = spaces(line);
                if (state == MAP && spaces < prefix) {
                    reader.with(line);
                    break;
                }
                line = line.trim();
                final int separator = line.indexOf(':');
                if (separator < 1) {
                    continue;
                }
                final Object value;
                if (line.length() - 1 == separator || line.substring(separator + 1).trim().startsWith("#")) {
                    final String nextLine = reader.fly();
                    final int nextSpaces;
                    if (nextLine == null || (nextSpaces = spaces(nextLine)) < (state == ROOT ? 1 : prefix)) {
                        value = "";
                    } else {
                        if (nextLine.trim().startsWith("-")) {
                            value = read(reader, prefix, LIST);
                        } else {
                            value = read(reader, nextSpaces, MAP);
                        }
                    }
                } else {
                    value = read(reader.with(line.substring(separator + 1)), prefix, SCALAR);
                }
                if (value != null) {
                    map.put(line.substring(0, separator), value);
                }
            }
            return map;
        } else if (state == LIST) {
            final List<Object> list = new ArrayList<>();
            String line;
            while ((line = reader.walk()) != null) {
                final int spaces = spaces(line);
                if (spaces < prefix) {
                    reader.with(line);
                    break;
                }
                final String trim = line.trim();
                if (trim.isEmpty()) {
                    continue;
                }
                if (trim.charAt(0) != '-') {
                    reader.with(line);
                    break;
                }
                line = trim.substring(1);
                final int totalSpaces = spaces + spaces(line) + 1;
                line = line.trim();
                if (line.isEmpty()) {
                    list.add("");
                    continue;
                }
                final char first = line.charAt(0);
                final Object value;
                if (first == '-') {
                    value = read(reader.with(" ".repeat(totalSpaces) + line), totalSpaces, LIST);
                } else if (line.indexOf(':') > 0 && first != '\"' && first != '\'') {
                    value = read(reader.with(" ".repeat(totalSpaces) + line), totalSpaces, MAP);
                } else {
                    value = read(reader.with(line), spaces + 1, SCALAR);
                }
                if (value != null) {
                    list.add(value);
                }
            }
            return list;
        } else if (state == SCALAR) {
            final String line = reader.walk().trim();
            final Object value = value(line);
            if (!line.isEmpty() && line.charAt(0) != '\"' && line.charAt(0) != '\'') {
                if (value == "|-") {
                    return read(reader, prefix + 1, LITERAL_BLOCK_SCALAR);
                } else if (value == "{}") {
                    return new LinkedHashMap<>();
                } else if (value == "[]") {
                    return new ArrayList<>();
                }
            }
            return value;
        } else if (state == LITERAL_BLOCK_SCALAR) {
            final StringJoiner joiner = new StringJoiner("\n");
            String line;
            while ((line = reader.walk()) != null) {
                final int spaces = spaces(line);
                if (spaces < prefix) {
                    reader.with(line);
                    break;
                }
                joiner.add(line.trim());
            }
            return joiner.toString();
        }
        return null;
    }

    @NotNull
    private static Object value(@NotNull String s) {
        String trim = s.trim();
        if (trim.isEmpty()) {
            return "";
        }
        final char first = trim.charAt(0);
        if (first == '\"' || first == '\'') {
            if (trim.length() == 1) {
                return "";
            }
            final int single = trim.lastIndexOf(first, 1);
            final int comment = trim.lastIndexOf('#', 1);
            if (single > 0 && comment > 0) {
                if (comment > single) {
                    trim = trim.substring(0, comment).trim();
                }
            }
            return trim.substring(1);
        }
        final int comment = trim.indexOf('#');
        if (comment == 0) {
            return "";
        } else if (comment > 0) {
            trim = trim.substring(0, comment);
        }
        if (trim.equals("true")) {
            return true;
        } else if (trim.equals("false")) {
            return false;
        } else {
            try {
                return Long.parseLong(trim);
            } catch (NumberFormatException ignored) { }
            try {
                return Double.parseDouble(trim);
            } catch (NumberFormatException ignored) { }
        }
        return trim;
    }

    private static int spaces(@NotNull String s) {
        int spaces = 0;
        for (char c : s.toCharArray()) {
            if (c != ' ') {
                break;
            }
            spaces++;
        }
        return spaces;
    }

    public void write(@NotNull Writer writer) throws IOException {
        write(writer, "  ");
    }

    public void write(@NotNull Writer writer, @NotNull String indent) throws IOException {
        if (this.getData().isEmpty()) {
            writer.write("");
            return;
        }
        write(
                writer instanceof BufferedWriter ? (BufferedWriter) writer : new BufferedWriter(writer),
                indent.isEmpty() ? "  " : indent,
                "",
                this.getData(),
                ROOT
        );
    }

    protected int write(@NotNull BufferedWriter writer, @NotNull String indent, @NotNull String prefix, @NotNull Object object, int state) throws IOException {
        if (object instanceof Map<?, ?> map) {
            if (map.isEmpty()) {
                writer.write(" {}");
                return SCALAR;
            }
            // map:
            //   key:
            if (state == MAP) {
                writer.newLine();
            }
            boolean first = state == LIST;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (first) {
                    // list:
                    //   - key: value
                    writer.write(" " + entry.getKey() + ":");
                    first = false;
                } else {
                    // key: value
                    writer.write(prefix + entry.getKey() + ":");
                }
                final int result = write(writer, indent, prefix + indent, entry.getValue(), MAP);
                if (result == SCALAR) {
                    writer.newLine();
                }
                // key1: value1
                //
                // key2: value2
                if (state == ROOT) {
                    writer.newLine();
                }
            }
            return MAP;
        } else if (object instanceof List<?> list) {
            if (list.isEmpty()) {
                writer.write(" []");
                return SCALAR;
            }
            // map:
            //   -
            if (state == MAP) {
                writer.newLine();
            }
            boolean first = state == LIST;
            for (Object value : list) {
                if (first) {
                    // list:
                    //   - - value
                    writer.write(" -");
                    first = false;
                } else {
                    // - value
                    writer.write(prefix + indent + "-");
                }
                final int result = write(writer, indent, prefix + indent + "  ", value, LIST);
                if (result == SCALAR) {
                    writer.newLine();
                }
            }
            return LIST;
        } else if (object instanceof Boolean || object instanceof Number) {
            writer.write(" " + object);
        } else if (object instanceof String str && str.contains("\n") && !str.equals("\n")) {
            // key: |-
            //   line1
            //   line2
            writer.write(" |-");
            for (String line : str.split("\n")) {
                writer.newLine();
                writer.write(prefix + indent + line);
            }
        } else {
            writer.write(" \"" + object + "\"");
        }
        return SCALAR;
    }
}

package com.saicone.onetimepack.module;

import com.saicone.onetimepack.util.FileUtils;
import org.jetbrains.annotations.Contract;
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
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

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
    @Contract("_, !null, _ -> !null")
    public <T> T get(@NotNull String path, @Nullable T def, @NotNull Function<Object, T> parser) {
        return get(path.toLowerCase().split("\\."), def, parser);
    }

    @Nullable
    @Contract("_, !null, _ -> !null")
    public <T> T get(@NotNull String[] path, @Nullable T def, @NotNull Function<Object, T> parser) {
        final Object object = get(path);
        final T parsed = object == null ? null : parser.apply(object);
        return parsed == null ? def : parsed;
    }

    @Nullable
    public String getString(@NotNull String path) {
        return getString(path, null);
    }

    @Nullable
    @Contract("_, !null -> !null")
    public String getString(@NotNull String path, @Nullable String def) {
        return get(path, def, String::valueOf);
    }

    @NotNull
    public List<String> getStringList(@NotNull String path) {
        return getList(path, String::valueOf);
    }

    @Nullable
    public Integer getInt(@NotNull String path) {
        return getInt(path, null);
    }

    @Nullable
    @Contract("_, !null -> !null")
    public Integer getInt(@NotNull String path, @Nullable Integer def) {
        return get(path, def, object -> {
            if (object instanceof Number number) {
                return number.intValue();
            }
            try {
                return Integer.parseInt(String.valueOf(object));
            } catch (NumberFormatException e) {
                return null;
            }
        });
    }

    @Nullable
    public Boolean getBoolean(@NotNull String path) {
        return getBoolean(path, null);
    }

    @Nullable
    @Contract("_, !null -> !null")
    public Boolean getBoolean(@NotNull String path, @Nullable Boolean def) {
        return get(path, def, object -> {
            if (object instanceof Number number) {
                if (number.longValue() == 1) {
                    return true;
                } else if (number.longValue() == 0) {
                    return false;
                } else {
                    return null;
                }
            }
            return switch (String.valueOf(object)) {
                case "true", "yes", "y" -> true;
                case "false", "no", "n" -> false;
                default -> null;
            };
        });
    }

    @NotNull
    public <T> List<T> getList(@NotNull String path, @NotNull Function<Object, T> function) {
        return getList(path.toLowerCase().split("\\."), function);
    }

    @NotNull
    public <T> List<T> getList(@NotNull String[] path, @NotNull Function<Object, T> function) {
        final List<T> list = new ArrayList<>();
        final Object object = get(path);
        if (object == null) {
            return list;
        }
        final Consumer<Object> executor = value -> {
            if (value != null) {
                final T t = function.apply(object);
                if (t != null) {
                    list.add(t);
                }
            }
        };
        if (object instanceof Iterable<?> iterable) {
            for (Object value : iterable) {
                executor.accept(value);
            }
        } else if (object instanceof Object[] array) {
            for (Object value : array) {
                executor.accept(value);
            }
        } else if (object.getClass().isArray()) {
            final int length = Array.getLength(object);
            for (int i = 0; i < length; i++) {
                final Object value = Array.get(object, i);
                executor.accept(value);
            }
        } else {
            executor.accept(object);
        }
        return list;
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
}

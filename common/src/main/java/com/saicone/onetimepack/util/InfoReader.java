package com.saicone.onetimepack.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class InfoReader implements Closeable {

    private final BufferedReader reader;
    private final Predicate<String> comment;

    private transient final List<String> toConsume = new ArrayList<>();

    public InfoReader(@NotNull Reader reader) {
        this(reader, s -> false);
    }

    public InfoReader(@NotNull Reader reader, @NotNull Predicate<String> comment) {
        this.reader = reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);
        this.comment = comment;
    }

    @NotNull
    public BufferedReader getReader() {
        return reader;
    }

    @NotNull
    public Predicate<String> getComment() {
        return comment;
    }

    @Nullable
    protected String readNextLine() throws IOException {
        String line;
        while ((line = this.reader.readLine()) != null) {
            final String trim = line.trim();
            if (trim.isEmpty() || this.comment.test(trim)) {
                continue;
            }
            break;
        }
        return line;
    }

    @NotNull
    @Contract("_ -> this")
    public InfoReader with(@NotNull String s) {
        toConsume.add(0, s);
        return this;
    }

    @Nullable
    public String walk() throws IOException {
        if (!toConsume.isEmpty()) {
            return toConsume.remove(0);
        }
        return readNextLine();
    }

    @Nullable
    public String fly() throws IOException {
        final String line = readNextLine();
        if (line != null) {
            toConsume.add(line);
        }
        return line;
    }

    @Override
    public void close() throws IOException {
        this.reader.close();
    }
}

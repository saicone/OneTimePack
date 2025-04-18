package com.saicone.onetimepack.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@FunctionalInterface
public interface ValueComparator<E> {

    @NotNull
    static <E> ValueComparator<E> read(@NotNull String str, @NotNull Provider<E> provider) {
        ValueComparator<E> result = null;
        for (String block : str.split("(?i) (AND|&&) ")) {
            ValueComparator<E> append = null;
            if (block.contains(" OR ")) {
                for (String optional : block.split("(?i) (OR|[|][|]) ")) {
                    final ValueComparator<E> comparator = provider.readComparator(optional);
                    if (append == null) {
                        append = comparator;
                    } else if (comparator != null) {
                        append = append.or(comparator);
                    }
                }
            } else {
                append = provider.readComparator(block);
            }

            if (append != null) {
                if (result == null) {
                    result = append;
                } else {
                    result = result.and(append);
                }
            }
        }
        return result != null ? result : e -> true;
    }

    @Nullable
    Object getValue(@NotNull E e);

    default boolean matches(@NotNull E e1, @NotNull E e2) {
        return Objects.equals(getValue(e1), getValue(e2));
    }

    @NotNull
    default ValueComparator<E> nonNull() {
        return new ValueComparator<E>() {
            @Override
            public @Nullable Object getValue(@NotNull E e) {
                return ValueComparator.this.getValue(e);
            }

            @Override
            public boolean matches(@NotNull E e1, @NotNull E e2) {
                final Object value1 = getValue(e1);
                if (value1 == null) return false;
                final Object value2 = getValue(e2);
                if (value2 == null) return false;
                return value1.equals(value2);
            }
        };
    }

    @NotNull
    default ValueComparator<E> and(@NotNull ValueComparator<E> comparator) {
        return new ValueComparator<E>() {
            @Override
            public @Nullable Object getValue(@NotNull E e) {
                return ValueComparator.this.getValue(e);
            }

            @Override
            public boolean matches(@NotNull E e1, @NotNull E e2) {
                return ValueComparator.this.matches(e1, e2) && comparator.matches(e1, e2);
            }
        };
    }

    @NotNull
    default ValueComparator<E> or(@NotNull ValueComparator<E> comparator) {
        return new ValueComparator<E>() {
            @Override
            public @Nullable Object getValue(@NotNull E e) {
                return ValueComparator.this.getValue(e);
            }

            @Override
            public boolean matches(@NotNull E e1, @NotNull E e2) {
                return ValueComparator.this.matches(e1, e2) || comparator.matches(e1, e2);
            }
        };
    }

    interface Provider<E> {
        @Nullable
        ValueComparator<E> readComparator(@NotNull String input);
    }
}

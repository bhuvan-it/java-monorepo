package com.acme.common.core;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;

/**
 * A success-or-failure carrier for <em>expected</em> failures.
 *
 * <p>Exceptions stay reserved for genuinely exceptional conditions; anything a caller is expected to
 * handle (validation, missing records) travels as a {@code Result} so it shows up in the signature.
 */
public sealed interface Result<T> permits Result.Ok, Result.Err {

    static <T> Result<T> ok(T value) {
        return new Ok<>(value);
    }

    static <T> Result<T> err(String message) {
        return new Err<>(message);
    }

    default boolean isOk() {
        return this instanceof Ok<T>;
    }

    default T orElseThrow() {
        return switch (this) {
            case Ok<T> ok -> ok.value();
            case Err<T> e -> throw new NoSuchElementException(e.message());
        };
    }

    default T orElse(T fallback) {
        return switch (this) {
            case Ok<T> ok -> ok.value();
            case Err<T> ignored -> fallback;
        };
    }

    default <R> Result<R> map(Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return switch (this) {
            case Ok<T> ok -> Result.ok(mapper.apply(ok.value()));
            case Err<T> e -> Result.err(e.message());
        };
    }

    default <R> Result<R> flatMap(Function<? super T, Result<R>> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return switch (this) {
            case Ok<T> ok -> mapper.apply(ok.value());
            case Err<T> e -> Result.err(e.message());
        };
    }

    record Ok<T>(T value) implements Result<T> {
        public Ok {
            Objects.requireNonNull(value, "value");
        }
    }

    record Err<T>(String message) implements Result<T> {
        public Err {
            Objects.requireNonNull(message, "message");
        }
    }
}

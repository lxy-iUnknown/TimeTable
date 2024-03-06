package com.lxy.timetable.contract;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lxy.timetable.BuildConfig;

import timber.log.Timber;

public class Contract {
    private static final Object[] EMPTY_ARGS = new Object[0];

    public static <T extends Comparable<T>> void requireOperation(
            @NonNull Value<T> left, @NonNull Value<T> right, @NonNull Operator op) {
        Contract.requireNonNull(left);
        Contract.requireNonNull(right);
        Contract.requireNonNull(op);
        Contract.require(
                op.test(left.getValue(), right.getValue()),
                left + op.getNegatedOpName() + right
        );
    }

    private static <T> T failInternal(@NonNull String message, @Nullable Throwable cause) {
        if (BuildConfig.DEBUG) {
            Timber.wtf(cause, message, EMPTY_ARGS);
        }
        throw new RuntimeException(message, cause);
    }

    @SuppressLint("TimberExceptionLogging")
    @NonNull
    public static <T> T fail(@NonNull String format, @Nullable Throwable cause, Object... args) {
        return failInternal(String.format(format, args), cause);
    }

    @NonNull
    public static <T> T fail(@NonNull String message) {
        return fail(message, (Throwable) null);
    }

    @NonNull
    public static <T> T fail(@NonNull String message, @Nullable Throwable cause) {
        return failInternal(message, cause);
    }

    @NonNull
    public static <T> T fail(@NonNull String message, Object... args) {
        return fail(message, null, args);
    }

    @NonNull
    public static <T> T unreachable(String message, int value) {
        return fail(message + value);
    }

    public static void require(boolean value, String message) {
        if (!value) {
            fail("Contract failed, %s", message);
        }
    }

    @SuppressWarnings("DataFlowIssue")
    @NonNull
    public static <T> T requireNonNull(T value) {
        if (BuildConfig.DEBUG) {
            require(value != null, "Null value");
        }
        return value;
    }

    public static void validateIndex(int index, int length) {
        if (BuildConfig.DEBUG) {
            var indexValue = new Value<>("index", index);
            requireOperation(indexValue, Value.ZERO_I, Operator.GE);
            requireOperation(indexValue, new Value<>("length", length), Operator.LT);
        }
    }

    public static void validateLength(int length, int maxLength) {
        if (BuildConfig.DEBUG) {
            var lengthValue = new Value<>("length", length);
            requireOperation(lengthValue, Value.ZERO_I, Operator.GE);
            requireOperation(lengthValue, new Value<>("maxLength", maxLength), Operator.LE);
        }
    }
}

package com.lxy.termdate.contract;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lxy.termdate.BuildConfig;

import timber.log.Timber;

@SuppressWarnings({"UnusedReturnValue", "unused"})
public class Contract {
    private static final Object[] EMPTY_ARGS = new Object[0];

    private static <T extends Comparable<T>> void requireOperationInternal(
            @NonNull Value<T> left, @NonNull Value<T> right, @NonNull Operator op) {
        Contract.requireNonNull(left);
        Contract.requireNonNull(right);
        Contract.requireNonNull(op);
        if (!op.test(left.getValue(), right.getValue())) {
            Contract.fail(left + op.getNegatedOpName() + right);
        }
    }

    public static <T extends Comparable<T>> void requireOperation(
            @NonNull Value<T> left, @NonNull Value<T> right, @NonNull Operator op) {
        if (BuildConfig.DEBUG) {
            requireOperationInternal(left, right, op);
        }
    }

    private static <T> T failInternal(@NonNull String message, @Nullable Throwable cause) {
        if (BuildConfig.DEBUG) {
            Timber.wtf(cause, message, EMPTY_ARGS);
        }
        throw new RuntimeException(message, cause);
    }

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
    public static <T> T fail(String message, int value) {
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

    public static <T extends Comparable<T>> void requireInRangeInclusive(
            @NonNull Value<T> value, @NonNull T min, @NonNull T max) {
        if (BuildConfig.DEBUG) {
            var minValue = new Value<>(min);
            var maxValue = new Value<>(max);
            requireOperationInternal(minValue, maxValue, Operator.LE);
            requireOperationInternal(value, minValue, Operator.GE);
            requireOperationInternal(value, maxValue, Operator.LE);
        }
    }

    public static void requireValidIndex(int index, int length) {
        requireValidFromToIndex(0, index, length);
    }

    public static void requireValidIndex(long index, long length) {
        requireValidFromToIndex(0L, index, length);
    }

    public static void requireValidFromIndexSize(int fromIndex, int size, int length) {
        requireValidFromToIndex(fromIndex, fromIndex + size, length);
    }

    public static void requireValidFromIndexSize(long fromIndex, long size, long length) {
        requireValidFromToIndex(fromIndex, fromIndex + size, length);
    }

    public static <T extends Comparable<T>> void requireValidFromToIndex(
            int fromIndex, int toIndex, int length) {
        if (BuildConfig.DEBUG) {
            var fromIndexValue = new Value<>("fromIndex", fromIndex);
            var toIndexValue = new Value<>("toIndex", toIndex);
            requireOperationInternal(fromIndexValue, Value.ZERO_I, Operator.GE);
            requireOperationInternal(fromIndexValue, toIndexValue, Operator.LE);
            requireOperationInternal(toIndexValue, new Value<>("length", length), Operator.LT);
        }
    }

    public static <T extends Comparable<T>> void requireValidFromToIndex(
            long fromIndex, long toIndex, long length) {
        if (BuildConfig.DEBUG) {
            var fromIndexValue = new Value<>("fromIndex", fromIndex);
            var toIndexValue = new Value<>("toIndex", toIndex);
            requireOperationInternal(fromIndexValue, Value.ZERO_L, Operator.GE);
            requireOperationInternal(fromIndexValue, toIndexValue, Operator.LE);
            requireOperationInternal(toIndexValue, new Value<>("length", length), Operator.LT);
        }
    }
}

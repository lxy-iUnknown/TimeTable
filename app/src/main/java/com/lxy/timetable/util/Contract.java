package com.lxy.timetable.util;

import com.lxy.timetable.BuildConfig;
import timber.log.Timber;

public class Contract {
    public static void require(boolean value, String message) {
        if (!value) {
            if (BuildConfig.DEBUG) {
                Timber.wtf("Contract failed: \"%s\"", message);
            }
            throw new RuntimeException(message);
        }
    }

    public static <T> T requireNonNull(T value) {
        if (BuildConfig.DEBUG) {
            require(value != null, "Null value");
        }
        return value;
    }

    public static void validateIndex(int index, int length) {
        if (BuildConfig.DEBUG) {
            require(index >= 0 && index < length,
                    "Index " + index + " out of range [0, " + length + ")");
        }
    }

    public static void validateLength(int length, int maxLength) {
        if (BuildConfig.DEBUG) {
            require(length >= 0 && length <= maxLength,
                    "Length " + length + " out of range [0, " + maxLength + "]");
        }
    }
}

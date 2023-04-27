package com.lxy.timetable.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lxy.timetable.BuildConfig;

import timber.log.Timber;

public class TimberUtil {
    private static final Object[] EMPTY_ARGS = new Object[0];

    @NonNull
    public static RuntimeException errorAndThrowException(@Nullable Throwable cause, @NonNull String message) {
        if (BuildConfig.DEBUG) {
            Timber.e(cause, message, EMPTY_ARGS);
        }
        return new RuntimeException(message, cause);
    }

    @NonNull
    public static RuntimeException errorAndThrowException(@NonNull String message) {
        return errorAndThrowException(null, message);
    }
}

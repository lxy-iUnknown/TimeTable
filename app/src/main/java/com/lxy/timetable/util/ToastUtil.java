package com.lxy.timetable.util;

import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.lxy.timetable.GlobalContext;
import com.lxy.timetable.contract.Contract;

public class ToastUtil {
    public static void toast(@StringRes int resId) {
        toast(GlobalContext.getResourceString(resId));
    }

    @SuppressWarnings("unused")
    public static void toast(int resId, @NonNull Object... args) {
        toast(GlobalContext.getResourceString(resId, Contract.requireNonNull(args)));
    }

    @SuppressWarnings("unused")
    public static void toast(@NonNull String message, @NonNull Object... args) {
        toast(String.format(message, Contract.requireNonNull(args)));
    }

    public static void toast(@NonNull String message) {
        Toast.makeText(GlobalContext.get(), Contract.requireNonNull(message), Toast.LENGTH_SHORT).show();
    }
}
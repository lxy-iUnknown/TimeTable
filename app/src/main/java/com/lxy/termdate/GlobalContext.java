package com.lxy.termdate;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.lxy.termdate.contract.Contract;

import timber.log.Timber;

public class GlobalContext extends Application {
    @SuppressLint("StaticFieldLeak")
    private static Context context = null;

    static {
        if (BuildConfig.DEBUG) {
            Timber.plant(cast(new Timber.DebugTree()));
        }
    }

    @NonNull
    public static Context get() {
        if (BuildConfig.DEBUG) {
            return Contract.requireNonNull(context);
        }
        return context;
    }

    @NonNull
    public static Resources getResource() {
        return Contract.requireNonNull(get().getResources());
    }

    @NonNull
    public static String getResourceString(@StringRes int id) {
        return Contract.requireNonNull(getResource().getString(id));
    }

    @NonNull
    public static String getResourceString(@StringRes int id, @NonNull Object... args) {
        return Contract.requireNonNull(getResource().getString(id, args));
    }

    @SuppressWarnings("ConstantConditions")
    @NonNull
    private static Timber.Tree cast(@NonNull Timber.DebugTree tree) {
        // https://github.com/JakeWharton/timber/issues/459
        return (Timber.Tree) (Object) tree;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = Contract.requireNonNull(getApplicationContext());
    }
}

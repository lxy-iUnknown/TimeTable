package com.lxy.timetable;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.lxy.timetable.contract.Contract;

import timber.log.Timber;

public class GlobalContext extends Application {
    @SuppressLint("StaticFieldLeak")
    private static Context context = null;
    private static BroadcastReceiver receiver;

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

    public static void registerBroadcastReceiver(@NonNull BroadcastReceiver broadcastReceiver,
                                                 @NonNull IntentFilter filter) {
        if (BuildConfig.DEBUG) {
            Timber.d("Register new broadcast receiver");
        }
        var context = get();
        if (receiver != null) {
            if (BuildConfig.DEBUG) {
                Timber.d("Clean up old broadcast receiver");
            }
            context.unregisterReceiver(receiver);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(Contract.requireNonNull(broadcastReceiver),
                    Contract.requireNonNull(filter), Context.RECEIVER_NOT_EXPORTED);
        }
        receiver = broadcastReceiver;
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

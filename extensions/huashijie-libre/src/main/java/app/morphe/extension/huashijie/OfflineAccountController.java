package app.morphe.extension.huashijie;

import android.util.Log;

import java.lang.reflect.Field;

/**
 * Huashijie Libre offline account and feature privileges controller.
 * Manages standalone offline identity (HHUser), unlocking local creation
 * tools (PDF export, brushes, canvas templates) without cloud dependencies.
 */
public final class OfflineAccountController {
    private static final String TAG = "OfflineAccount";

    private OfflineAccountController() {}

    public static boolean isOfflineProfileActive() {
        return LibreConfig.isFeatureActive(LibreConfig.KEY_OFFLINE_USER_STUB);
    }

    /**
     * Populates the GlobalData user instance for standalone offline capability:
     * (a) Unlocks PDF Export, Templates, and Canvas Presets without remote account login.
     * (b) Avoids unwanted login redirects (PreLoginActivity).
     * (c) Fixes SimpleDateFormat crash in MainDrawerLayout by providing valid timestamps.
     */
    public static void setupOfflineUserStub() {
        try {
            if (!isOfflineProfileActive()) {
                return;
            }

            Class<?> gdClz = Class.forName("o00OO0o0.o0OOO0o");
            Class<?> hhClz = Class.forName("net.huanci.hsjpro.model.result.user.HHUser");
            Field uField = LibreResourceResolver.findField(gdClz, "OooO0oO", hhClz, null);
            if (uField == null) {
                return;
            }
            Object user = uField.get(null);
            Class<?> luClz = Class.forName("net.huanci.hsjpro.model.LoginUser");
            if (user == null) {
                user = luClz.getDeclaredConstructor().newInstance();
                uField.set(null, user);
            }

            Class<?> buClz = Class.forName("net.huanci.hsjpro.model.result.user.BaseUser");
            setIntField(buClz, user, "id", 76);
            setIntField(buClz, user, "vipId", 3);
            setIntField(buClz, user, "vipLevel", 3);
            setIntField(buClz, user, "level", 1);
            setIntField(buClz, user, "identityLevel", 101);
            setObjField(buClz, user, "nick", "Libre Studio");
            setIntField(buClz, user, "headBox", 0);

            setObjField(hhClz, user, "vipExpireTime", "2099-12-31 23:59:59");
            setObjField(hhClz, user, "vipStartTime", "2024-01-01 00:00:00");
            setObjField(hhClz, user, "createTime", "2024-01-01 00:00:00");
            setObjField(hhClz, user, "unregistTime", "");

            setIntField(luClz, user, "status", 1);

            Log.i(TAG, "Offline artist profile active: ID=76, VIP=3, identityLevel=101, vipExpireTime=2099-12-31");
        } catch (Throwable t) {
            Log.w(TAG, "Failed to setup offline artist profile", t);
        }
    }

    private static void setIntField(Class<?> cls, Object target, String fieldName, int value) {
        try {
            Field f = LibreResourceResolver.findField(cls, fieldName, null, null);
            if (f != null) {
                f.setInt(target, value);
            }
        } catch (Throwable ignored) {}
    }

    private static void setObjField(Class<?> cls, Object target, String fieldName, Object value) {
        try {
            Field f = LibreResourceResolver.findField(cls, fieldName, null, null);
            if (f != null) {
                f.set(target, value);
            }
        } catch (Throwable ignored) {}
    }
}

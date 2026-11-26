package app.morphe.extension.huashijie;

import android.app.Activity;
import android.util.Log;
import android.view.View;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;

/**
 * Huashijie Libre offline account and feature privileges controller.
 * Manages standalone offline identity (HHUser), unlocking local creation
 * tools (PDF export, brushes, canvas templates) without cloud dependencies.
 */
public final class OfflineAccountController {
    private static final String TAG = "OfflineAccount";
    private static final int ID_ACCOUNT_SAFE_VIEW = 0x7f090049;
    private static final int TAG_HIDDEN_BY_OFFLINE_PROFILE = 0x7e098810;
    private static volatile WeakReference<View> sLastDrawerRef = new WeakReference<>(null);

    private OfflineAccountController() {}

    public static boolean isOfflineProfileActive() {
        return LibreConfig.isFeatureActive(LibreConfig.KEY_OFFLINE_USER_STUB);
    }

    /**
     * Hides or restores the Account & Security drawer item (account_safe_view)
     * in synchronization with the offline artist profile toggle without affecting
     * sibling drawer entries or leaving layout gaps.
     */
    public static void syncDrawerAccountSafeVisibility(Object target) {
        syncDrawerAccountSafeVisibility(target, false);
    }

    public static void syncLastDrawerAccountVisibility() {
        View lastRoot = sLastDrawerRef.get();
        if (lastRoot != null) {
            syncDrawerAccountSafeVisibility(lastRoot, true);
        }
    }

    private static void syncDrawerAccountSafeVisibility(Object target, boolean restoreVisibleNow) {
        try {
            View root = null;
            if (target instanceof View) {
                root = (View) target;
            } else if (target instanceof Activity) {
                Activity act = (Activity) target;
                if (act.getWindow() != null) {
                    root = act.getWindow().getDecorView();
                }
            }
            if (root == null) return;

            View accountSafe = LibreResourceResolver.findViewByName(
                    root, "account_safe_view", ID_ACCOUNT_SAFE_VIEW);
            if (accountSafe == null) return;

            sLastDrawerRef = new WeakReference<>(root);
            if (isOfflineProfileActive()) {
                accountSafe.setTag(TAG_HIDDEN_BY_OFFLINE_PROFILE, Boolean.TRUE);
                WorkspaceUiController.hideViewCleanly(accountSafe);
            } else if (Boolean.TRUE.equals(accountSafe.getTag(TAG_HIDDEN_BY_OFFLINE_PROFILE))) {
                accountSafe.setTag(TAG_HIDDEN_BY_OFFLINE_PROFILE, null);
                if (!accountSafe.isEnabled()) {
                    accountSafe.setEnabled(true);
                }
                if (!accountSafe.isClickable()) {
                    accountSafe.setClickable(true);
                }
                if (restoreVisibleNow && accountSafe.getVisibility() != View.VISIBLE) {
                    accountSafe.setVisibility(View.VISIBLE);
                    accountSafe.requestLayout();
                }
            }
        } catch (Throwable ignored) {}
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

            Class<?> gdClz = LibreResourceResolver.loadRegisteredClass("GlobalData.class", null);
            if (gdClz == null) {
                return;
            }
            Class<?> hhClz = Class.forName("net.huanci.hsjpro.model.result.user.HHUser");
            Field uField = LibreResourceResolver.findRegisteredField(
                    gdClz, "GlobalData.userField", null, hhClz, null);
            if (uField == null) {
                return;
            }
            Object user = uField.get(null);
            Class<?> buClz = Class.forName("net.huanci.hsjpro.model.result.user.BaseUser");
            if (user != null) {
                Field idField = LibreResourceResolver.findField(buClz, "id", null, null);
                if (idField != null) {
                    int existingId = idField.getInt(user);
                    if (existingId > 0 && existingId != 76) {
                        Log.i(TAG, "Preserving existing signed-in user session");
                        return;
                    }
                }
            }

            Class<?> luClz = Class.forName("net.huanci.hsjpro.model.LoginUser");
            if (user == null) {
                user = luClz.getDeclaredConstructor().newInstance();
                uField.set(null, user);
            }

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

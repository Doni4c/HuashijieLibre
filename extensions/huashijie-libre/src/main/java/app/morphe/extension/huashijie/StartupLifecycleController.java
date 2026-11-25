package app.morphe.extension.huashijie;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Huashijie Libre startup and lifecycle controller.
 * Manages FlashActivity lifecycle, splash ad suppression,
 * and watchdog flag synchronization with BaseActivity.
 */
public final class StartupLifecycleController {

    private StartupLifecycleController() {}

    private static void initSingletonManager(
            String classKey,
            String getterKey,
            String initMethodKey,
            boolean passFalseArg
    ) {
        try {
            Class<?> mgrClz = LibreResourceResolver.loadRegisteredClass(classKey, null);
            if (mgrClz == null) {
                return;
            }
            Object mgr = LibreResourceResolver.getRegisteredStaticSingleton(mgrClz, getterKey, null);
            if (mgr == null) {
                return;
            }
            if (passFalseArg) {
                Method initMethod = LibreResourceResolver.findRegisteredMethod(
                        mgrClz, initMethodKey, null, void.class, false, boolean.class);
                if (initMethod != null) {
                    initMethod.invoke(mgr, false);
                }
            } else {
                Method initMethod = LibreResourceResolver.findRegisteredMethod(
                        mgrClz, initMethodKey, null, void.class, false);
                if (initMethod != null) {
                    initMethod.invoke(mgr);
                }
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Intercepts FlashActivity.onCreate to bypass splash ad countdown
     * and transition immediately to MainActivity.
     *
     * @param flashActivity FlashActivity instance
     * @return true if navigation was handled and FlashActivity should terminate immediately.
     */
    public static boolean handleFlashLifecycle(Activity flashActivity) {
        if (flashActivity == null || flashActivity.isFinishing()) {
            return false;
        }

        try {
            LibreConfig.init(flashActivity);
            boolean instantLaunch = LibreConfig.isFeatureActive(flashActivity, LibreConfig.KEY_INSTANT_LAUNCH);
            if (!instantLaunch) {
                return false;
            }

            Intent origIntent = flashActivity.getIntent();
            if (origIntent != null) {
                String action = origIntent.getAction();
                if ((action != null && !Intent.ACTION_MAIN.equals(action))
                        || origIntent.getData() != null
                        || (Build.VERSION.SDK_INT >= 16 && origIntent.getClipData() != null)
                        || origIntent.hasExtra(Intent.EXTRA_STREAM)) {
                    return false;
                }
            }

            // Synchronize DrawApplication watchdog flag before starting MainActivity
            try {
                Class<?> appClz = LibreResourceResolver.loadRegisteredClass(
                        "DrawApplication.class", "net.huanci.hsjpro.DrawApplication");
                if (appClz != null) {
                    Object app = flashActivity.getApplication();
                    if (app == null || !appClz.isInstance(app)) {
                        app = LibreResourceResolver.getRegisteredStaticSingleton(
                                appClz, "DrawApplication.getInstance", null);
                    }
                    Field f = LibreResourceResolver.findRegisteredField(
                            appClz, "DrawApplication.watchdogField", null, boolean.class, null);
                    if (f != null) {
                        if (Modifier.isStatic(f.getModifiers())) {
                            f.setBoolean(null, true);
                        } else if (app != null) {
                            f.setBoolean(app, true);
                        }
                    }
                }
            } catch (Throwable ignored) {}

            // Initialize essential drawing managers usually started during splash
            initSingletonManager("BrushPkgManager.class", "BrushPkgManager.getInstance", "BrushPkgManager.init", true);
            initSingletonManager("ColorSpaceManager.class", "ColorSpaceManager.getInstance", "ColorSpaceManager.init", false);
            initSingletonManager("HistoryBrushManager.class", "HistoryBrushManager.getInstance", "HistoryBrushManager.init", false);

            if (!flashActivity.isTaskRoot()) {
                flashActivity.finish();
                return true;
            }

            Intent intent = new Intent();
            intent.setClassName(flashActivity.getPackageName(), "net.huanci.hsjpro.activities.MainActivity");

            if (origIntent != null) {
                if (origIntent.getAction() != null) {
                    intent.setAction(origIntent.getAction());
                }
                if (origIntent.getData() != null) {
                    intent.setData(origIntent.getData());
                }
                if (origIntent.getExtras() != null) {
                    intent.putExtras(origIntent.getExtras());
                }
            }

            flashActivity.startActivity(intent);
            flashActivity.finish();
            try {
                flashActivity.overridePendingTransition(0, 0);
            } catch (Throwable ignored) {}
            return true;
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return false;
    }

    public static boolean isInstantLaunchActive() {
        return LibreConfig.isFeatureActive(LibreConfig.KEY_INSTANT_LAUNCH);
    }

    public static boolean isSuppressSplashAdActive() {
        return LibreConfig.isFeatureActive(LibreConfig.KEY_SUPPRESS_SPLASH_AD);
    }

    /**
     * Hook called from FlashActivity splash ad gate (canLoadAd).
     * Suppresses ad initialization independently when Suppress Splash Ad Loading is active.
     */
    public static boolean shouldAllowSplashAd(boolean originalCanLoadAd) {
        try {
            if (isSuppressSplashAdActive()) {
                return false;
            }
        } catch (Throwable ignored) {}
        return originalCanLoadAd;
    }
}

package app.morphe.extension.huashijie;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Huashijie Libre central runtime configuration.
 * Manages preferences independently via "huashijie_libre_prefs.xml",
 * avoiding any collision with the original application preferences.
 */
public final class LibreConfig {
    private static final String PREF_NAME = "huashijie_libre_prefs";
    private static final String KEY_FLUSH_SEQ = "_libre_flush_seq";

    // Base module
    public static final String KEY_ENABLE_ANIMATIONS = "libre_enable_animations";

    // Pillar 1: Localization and typography
    public static final String KEY_DYNAMIC_I18N = "libre_dynamic_i18n";
    public static final String KEY_LANG_FILE = "libre_lang_file";
    public static final String KEY_UI_FONT_FILE = "libre_ui_font_file";

    // Pillar 2: Domestic bloat removal and global compatibility
    public static final String KEY_INSTANT_LAUNCH = "pure_studio_instant_launch";
    public static final String KEY_SUPPRESS_SPLASH_AD = "pure_studio_suppress_splash_ad";
    public static final String KEY_HIDE_COMMUNITY_TABS = "pure_studio_hide_community_tabs";
    public static final String KEY_HIDE_TOPBAR_CLOUD = "pure_studio_hide_topbar_cloud";
    public static final String KEY_HIDE_CANVAS_POPUP_CLOUD = "pure_studio_hide_canvas_popup_cloud";
    public static final String KEY_HIDE_DRAFT_CLOUD_TICK = "pure_studio_hide_draft_cloud_tick";
    public static final String KEY_OPTIMIZE_CREATE_CANVAS = "pure_studio_optimize_create_canvas";
    public static final String KEY_HIDE_CANVAS_EXIT_CLOUD = "pure_studio_hide_canvas_exit_cloud";
    public static final String KEY_HIDE_BRUSH_SHOP = "pure_studio_hide_brush_shop";
    public static final String KEY_HIDE_BRUSH_LIBRARY_SHOP = "pure_studio_hide_brush_library_shop";
    public static final String KEY_BLOCK_BRUSH_UPGRADE_TIP = "pure_studio_block_brush_upgrade_tip";
    public static final String KEY_HIDE_BAIDU_BACKUP = "pure_studio_hide_baidu_backup";
    public static final String KEY_HIDE_PERSONALIZED_ADS = "pure_studio_hide_personalized_ads";
    public static final String KEY_HIDE_DRAWER_VIP = "pure_studio_hide_drawer_vip";
    public static final String KEY_HIDE_EXPORT_SOCIAL_ICONS = "pure_studio_hide_domestic_export";
    public static final String KEY_HIDE_TIMELAPSE_EXPORT_SOCIAL = "pure_studio_hide_timelapse_export_social";
    public static final String KEY_SYSTEM_SHARE = "libre_system_share_sheet";
    public static final String KEY_REPLAY_SYSTEM_SHARE = "libre_replay_system_share";
    public static final String KEY_EXPORT_SHARE_BUTTON = "libre_export_system_share_button";
    public static final String KEY_BLOCK_TELEMETRY = "pure_studio_block_telemetry";
    public static final String KEY_BLOCK_UMENG_SDK = "pure_studio_block_umeng_sdk";
    public static final String KEY_BLOCK_BUGLY_SDK = "pure_studio_block_bugly_sdk";

    // Legacy key aliases
    public static final String KEY_PURE_STUDIO_LEGACY = "libre_pure_studio";
    public static final String KEY_INSTANT_LAUNCH_LEGACY = "libre_instant_launch";
    public static final String KEY_HIDE_CLOUD_BUTTON_LEGACY = "pure_studio_hide_cloud_button";

    // Pillar 3: Offline account and local member sovereignty
    public static final String KEY_OFFLINE_USER_STUB = "libre_offline_user_stub";
    public static final String KEY_LOCAL_BRUSH_MANAGER = "libre_local_brush_studio";
    public static final String KEY_EXTERNAL_BRUSH_IMPORT = "libre_external_brush_import";

    // Pillar 4: Studio workflow and UX ergonomics
    public static final String KEY_TOUCH_OUTSIDE_DISMISS = "libre_touch_outside_dismiss";
    public static final String KEY_FREE_REF_ROTATION = "libre_free_ref_rotation";
    public static final String KEY_FLOATING_BRUSH_SLIDERS = "libre_floating_brush_sliders";
    public static final String KEY_SLIDER_POS_X = "libre_slider_pos_x";
    public static final String KEY_SLIDER_POS_Y = "libre_slider_pos_y";
    public static final String KEY_SLIDER_NORM_X = "libre_slider_norm_x";
    public static final String KEY_SLIDER_NORM_Y = "libre_slider_norm_y";
    public static final String KEY_CLEAN_SPEEDPAINT = "libre_clean_speedpaint";
    public static final String KEY_PUBLIC_STORAGE = "storage_transparency_mode";
    public static final String KEY_CANVAS_HSJ_PATH_ROW = "libre_canvas_hsj_path_row";
    public static final String KEY_TEXT_AUTO_FIT = "libre_text_auto_fit";
    public static final String KEY_CANVAS_POPUP_POLISH = "libre_canvas_popup_polish";

    private static volatile LibreConfig sInstance;
    private final SharedPreferences mPrefs;
    private final CopyOnWriteArrayList<ConfigChangeListener> mListeners = new CopyOnWriteArrayList<>();
    private final java.util.concurrent.ConcurrentHashMap<String, Boolean> mBoolCache =
            new java.util.concurrent.ConcurrentHashMap<>();

    public interface ConfigChangeListener {
        void onConfigChanged(String key);
    }

    private LibreConfig(Context context) {
        Context appContext = context.getApplicationContext() != null ? context.getApplicationContext() : context;
        this.mPrefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static void init(Context context) {
        if (context == null) return;
        LibreResourceResolver.init(context);
        if (sInstance == null) {
            synchronized (LibreConfig.class) {
                if (sInstance == null) {
                    sInstance = new LibreConfig(context);
                }
            }
        }
    }

    public static LibreConfig get(Context context) {
        if (sInstance == null && context != null) {
            init(context);
        }
        return sInstance;
    }

    public static LibreConfig getInstance() {
        return sInstance;
    }

    /**
     * Checks whether the toggle patch is compiled in and enabled by the user at runtime.
     */
    public static boolean isFeatureActive(String key) {
        return isFeatureActive(key, true);
    }

    public static boolean isFeatureActive(String key, boolean defaultValue) {
        try {
            if (!LibreSettingsInjector.isToggleRegistered(key)) {
                return false;
            }
            LibreConfig cfg = sInstance;
            return cfg != null ? cfg.isEnabled(key, defaultValue) : defaultValue;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isFeatureActive(Context context, String key) {
        if (sInstance == null && context != null) {
            init(context);
        }
        return isFeatureActive(key, true);
    }

    public boolean isEnabled(String key, boolean defaultValue) {
        if (mPrefs == null || key == null) return defaultValue;
        Boolean cached = mBoolCache.get(key);
        if (cached != null) {
            return cached;
        }
        boolean result = defaultValue;
        if (mPrefs.contains(key)) {
            result = mPrefs.getBoolean(key, defaultValue);
        } else if (KEY_INSTANT_LAUNCH.equals(key)) {
            result = mPrefs.getBoolean(KEY_INSTANT_LAUNCH_LEGACY, defaultValue);
        } else if (KEY_HIDE_COMMUNITY_TABS.equals(key)) {
            result = mPrefs.getBoolean(KEY_PURE_STUDIO_LEGACY, defaultValue);
        } else if (KEY_HIDE_TOPBAR_CLOUD.equals(key)) {
            result = mPrefs.getBoolean(KEY_HIDE_CLOUD_BUTTON_LEGACY, defaultValue);
        }
        mBoolCache.put(key, result);
        return result;
    }

    public void setEnabled(String key, boolean enabled) {
        if (mPrefs != null && key != null) {
            mBoolCache.put(key, enabled);
            mPrefs.edit().putBoolean(key, enabled).apply();
            notifyChanged(key);
        }
    }

    public String getSelectedLanguageFile() {
        return mPrefs != null ? mPrefs.getString(KEY_LANG_FILE, "vi.json") : "vi.json";
    }

    public void setSelectedLanguageFile(String fileName) {
        if (mPrefs != null) {
            mPrefs.edit().putString(KEY_LANG_FILE, fileName).apply();
            notifyChanged(KEY_LANG_FILE);
        }
    }

    public String getSelectedFontFile() {
        return mPrefs != null ? mPrefs.getString(KEY_UI_FONT_FILE, "") : "";
    }

    public void setSelectedFontFile(String fileName) {
        if (mPrefs != null) {
            mPrefs.edit().putString(KEY_UI_FONT_FILE, fileName).apply();
            notifyChanged(KEY_UI_FONT_FILE);
        }
    }

    public float getSliderPosX(float defaultX) {
        return mPrefs != null ? mPrefs.getFloat(KEY_SLIDER_POS_X, defaultX) : defaultX;
    }

    public float getSliderPosY(float defaultY) {
        return mPrefs != null ? mPrefs.getFloat(KEY_SLIDER_POS_Y, defaultY) : defaultY;
    }

    public void setSliderPos(float x, float y) {
        if (mPrefs != null) {
            mPrefs.edit().putFloat(KEY_SLIDER_POS_X, x).putFloat(KEY_SLIDER_POS_Y, y).apply();
        }
    }

    public float getSliderNormX(float defaultVal) {
        return mPrefs != null ? mPrefs.getFloat(KEY_SLIDER_NORM_X, defaultVal) : defaultVal;
    }

    public float getSliderNormY(float defaultVal) {
        return mPrefs != null ? mPrefs.getFloat(KEY_SLIDER_NORM_Y, defaultVal) : defaultVal;
    }

    public void setSliderNormPos(float normX, float normY) {
        if (mPrefs != null) {
            mPrefs.edit().putFloat(KEY_SLIDER_NORM_X, normX).putFloat(KEY_SLIDER_NORM_Y, normY).apply();
        }
    }

    /**
     * Commits a changing sequence value synchronously so pending apply() edits flush to disk
     * before process termination.
     */
    public static void flushSync() {
        try {
            LibreConfig cfg = sInstance;
            if (cfg != null && cfg.mPrefs != null) {
                long prev = cfg.mPrefs.getLong(KEY_FLUSH_SEQ, 0L);
                long seq = Math.max(System.currentTimeMillis(), prev + 1L);
                cfg.mPrefs.edit().putLong(KEY_FLUSH_SEQ, seq).commit();
            }
        } catch (Throwable ignored) {}
    }

    public void addListener(ConfigChangeListener listener) {
        if (listener != null && !mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }

    public void removeListener(ConfigChangeListener listener) {
        mListeners.remove(listener);
    }

    private void notifyChanged(String key) {
        for (ConfigChangeListener l : mListeners) {
            try {
                l.onConfigChanged(key);
            } catch (Throwable ignored) {}
        }
    }
}

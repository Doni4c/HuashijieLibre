package app.morphe.extension.huashijie;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Huashijie Libre: Dynamic External Translation Engine.
 * Loads and hot-reloads language packs from external storage:
 * Primary: /storage/emulated/0/HuashijiePro/lang/*.json
 * Fallback: Android/data/net.huanci.hsjpro/files/lang/*.json
 * Asset fallback: assets/lang/*.json
 *
 * Guarantees Zero-Crash: any missing key or parse error falls back to the original string.
 */
public final class TranslationEngine {
    private static final String TAG = "LibreTranslationEngine";
    private static final String AUDIT_TAG = "LIBRE_UI_AUDIT";
    private static final String SDCARD_DIR_NAME = "HuashijiePro";
    private static final String LANG_FOLDER_NAME = "lang";

    // Zero-New-Method-Ref Bridge operation codes for classes4.dex.
    // Reserved future opCode ranges (each returns Boolean.TRUE for true and null for false):
    // 10..49: Pillar 2 (Domestic Bloat Removal and Global Compatibility).
    // 50..69: Pillar 3 (Offline Account and Local Member Perks).
    // 70..99: Pillar 4 (Studio Workflow and UX Ergonomics).
    public static final int OP_TRANSLATE_VIEW = 1;
    public static final int OP_ATTACH_OBSERVER = 2;
    public static final int OP_TRANSLATE_AND_OBSERVE = 3;
    public static final int OP_DIALOG_PRE_SHOW = 4;
    public static final int OP_TRANSLATE_STRING_ARRAY = 5;
    public static final int OP_TRANSLATE_STRING = 6;
    public static final int OP_ON_TOAST_TEXT = 7;
    public static final int OP_HIDE_TOPBAR_CLOUD_SYNC = 10;
    public static final int OP_IS_HIDE_TOPBAR_CLOUD = 11;
    public static final int OP_HIDE_BRUSH_SHOP = 12;
    public static final int OP_SHOULD_BLOCK_BRUSH_UPGRADE_TIP = 13;
    public static final int OP_HIDE_EXPORT_SOCIAL_ICONS = 14;
    public static final int OP_HIDE_VIP_DRAWER = 15;
    public static final int OP_HIDE_BRUSH_LIBRARY_SHOP = 16;
    public static final int OP_HIDE_TIMELAPSE_EXPORT_SOCIAL = 17;
    public static final int OP_SHOULD_ALLOW_BRUSH_EDITING = 50;
    public static final int OP_ENSURE_BRUSH_PACKAGE_LIST = 51;
    public static final int OP_SYNC_DRAWER_ACCOUNT_VISIBILITY = 52;
    public static final int OP_IS_REF_ROTATION_ENABLED = 70;
    public static final int OP_IS_CLEAN_SPEEDPAINT = 71;
    public static final int OP_APPLY_TEXT_AUTO_FIT = 72;
    public static final int OP_CANVAS_POPUP_POLISH = 73;
    public static final int OP_FILTER_REF_ROTATION = 74;
    public static final int OP_BEFORE_LEFT_SETTING_LAYOUT = 75;
    public static final int OP_AFTER_LEFT_SETTING_LAYOUT = 76;
    public static final int OP_ON_LIUYEBI_INIT = 77;
    public static final int OP_ON_TOGGLE_SWITCH_UPDATE = 78;
    public static final int OP_DRAW_TOGGLE_SWITCH_LABELS = 79;
    public static final int OP_DRAW_HSV_NAME = 80;

    private static final int TAG_KEY_ORIG_TEXT = 0x7e090d00;
    private static final int TAG_KEY_ORIG_HINT = 0x7e090d01;
    private static final int TAG_KEY_ORIG_DESC = 0x7e090d02;
    private static final int TAG_KEY_ALIGNED = 0x7e090d03;
    private static final int TAG_KEY_DIALOG_ROOT = 0x7e090d04;
    private static final int TAG_KEY_HOST_ACTIVITY_CLASS = 0x7e090d05;
    private static final int TAG_KEY_ATTACH_LISTENER = 0x7e090d06;
    private static final Pattern PATTERN_DYNAMIC_NUMERIC =
            Pattern.compile("^[+-]?\\d+(?:[.,]\\d+)?\\s*(?:%|px|dp|sp|fps|pt|mm|cm|x|X)?$");
    private static final int TAG_KEY_ORIG_SWITCH_ON = 0x7e098610;
    private static final int TAG_KEY_ORIG_SWITCH_OFF = 0x7e098611;
    private static final int TAG_KEY_OBSERVED = 0x7e098612;
    private static final int TAG_KEY_ORIG_TEXT_SIZE = 0x7e098613;
    private static final int TAG_KEY_LAST_FIT_SIG = 0x7e098614;
    private static final int TAG_KEY_AUTOFIT_OBSERVED = 0x7e098615;
    private static final int TAG_KEY_LAST_TRANSLATED = 0x7e098616;
    private static final int TAG_KEY_TEXT_WATCHER = 0x7e098620;
    private static final int TAG_KEY_WATCHER_GUARD = 0x7e098621;
    private static final int TAG_KEY_ORIG_PAD_RIGHT = 0x7e098622;
    private static final int TAG_KEY_ORIG_MAX_LINES = 0x7e098623;
    private static final int TAG_KEY_ITEM_ONE_SHOT_PENDING = 0x7e098624;
    private static final int TAG_KEY_LAST_FIT_QUICK = 0x7e098625;
    private static final int TAG_KEY_ITEM_DETACH_GUARD = 0x7e098626;
    private static final int TAG_KEY_ORIG_PAD_LEFT = 0x7e098627;
    private static final int TAG_KEY_ORIG_SCALE_X = 0x7e098628;
    private static final int TAG_KEY_ORIG_ELLIPSIZE = 0x7e09862a;
    private static final int TAG_KEY_ORIG_SINGLE_LINE = 0x7e09862b;
    private static final int TAG_KEY_ORIG_MAX_WIDTH = 0x7e09862c;
    private static final Object SENTINEL_ELLIPSIZE_NONE = new Object();

    private static final Pattern PATTERN_ACTION_NUM = Pattern.compile("^\\u64cd\\u4f5c\\s*(\\d+)$");
    private static final Pattern PATTERN_BRUSH_NUM = Pattern.compile("^(?:\\u753b\\u7b14|\\u7b14\\u5237)\\s*(\\d+)$");
    private static final Pattern PATTERN_COLOR_NUM = Pattern.compile("^(?:\\u989c\\u8272|\\u8272\\u677f)\\s*(\\d+)$");
    private static final Pattern PATTERN_PRESET_NUM = Pattern.compile("^\\u9884\\u8bbe\\s*(\\d+)$");
    private static final Pattern PATTERN_GROUP_NUM = Pattern.compile("^(?:\\u5206\\u7ec4|\\u7ec4)\\s*(\\d+)$");
    private static final Pattern PATTERN_FRAME_ORDINAL = Pattern.compile("^(?:\\u7b2c\\s*)?(\\d+)\\s*\\u5e27$");
    private static final Pattern PATTERN_LAYER_ORDINAL = Pattern.compile("^\\u7b2c\\s*(\\d+)\\s*\\u5c42$");
    private static final Pattern PATTERN_PAGE_ORDINAL = Pattern.compile("^(?:\\u7b2c\\s*)?(\\d+)\\s*\\u9875$");
    private static final Pattern PATTERN_UNIT_PX = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*\\u50cf\\u7d20");
    private static final Pattern PATTERN_UNIT_FRAMES = Pattern.compile("(\\d+)\\s*(?:\\u5f20|\\u5e27)");
    private static final Pattern PATTERN_UNIT_LAYERS = Pattern.compile("(\\d+)\\s*\\u5c42");
    private static final Pattern PATTERN_UNIT_SEC = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*\\u79d2");
    private static final Pattern PATTERN_INLINE_LAYER = Pattern.compile("(\\d+)\\s*\\u56fe\\u5c42");
    private static final Pattern PATTERN_INLINE_DAY = Pattern.compile("(\\d+)\\s*\\u5929");
    private static final Pattern PATTERN_INLINE_EXPIRE_DATE =
            Pattern.compile("(\\d{4})\\u5e74(\\d{1,2})\\u6708(\\d{1,2})\\u65e5\\u5230\\u671f");
    private static final Pattern PATTERN_INLINE_DATE =
            Pattern.compile("(\\d{4})\\u5e74(\\d{1,2})\\u6708(\\d{1,2})\\u65e5");

    private static final int RUNTIME_CACHE_MAX_SIZE = 2048;
    private static final ConcurrentHashMap<Integer, String> sIdMap = new ConcurrentHashMap<>(4096);
    private static final ConcurrentHashMap<String, String> sTextMap = new ConcurrentHashMap<>(8192);
    private static final ConcurrentHashMap<String, String> sRuntimeTranslationCache = new ConcurrentHashMap<>(512);
    private static final ConcurrentHashMap<Integer, Bitmap> sCleanArrowBitmapCache = new ConcurrentHashMap<>(8);
    private static final ConcurrentHashMap<Class<?>, Field> sItemViewFieldCache = new ConcurrentHashMap<>();
    private static final Set<String> sLoggedAuditKeys = Collections.synchronizedSet(new HashSet<>());
    private static final Set<WeakReference<Activity>> sTrackedActivities =
            Collections.synchronizedSet(new HashSet<>());

    private static volatile boolean sInitialized = false;
    private static volatile boolean sLifecycleRegistered = false;
    private static volatile boolean sInternalConfigUpdate = false;
    private static volatile boolean sTemplateCopyScheduled = false;
    private static volatile boolean sFlagListenerRegistered = false;
    private static volatile boolean sCachedFlagsReady = false;
    private static volatile boolean sCachedDynamicI18n = true;
    private static volatile boolean sCachedTextAutoFit = true;
    private static volatile boolean sCachedCleanSpeedpaint = true;
    private static volatile int sCachedLangKind = 0;
    private static volatile boolean sWmgResolved = false;
    private static volatile Object sWmgInstance = null;
    private static volatile Field sWmgViewsField = null;
    private static volatile boolean sSyncingWindows = false;
    private static volatile int sLayoutDirtyEpoch = 1;
    private static volatile int sProtectedSdcardRootId = 0x7f110803;
    private static volatile int sProtectedAlbumFolderId = 0x7f110077;
    private static volatile boolean sProtectedIdsResolved = false;
    private static Context sAppContext;

    public static void markLayoutDirty() {
        sLayoutDirtyEpoch++;
    }

    private static long sLastLoadedFileMod = 0;
    private static long sLastOverrideFileMod = 0;
    private static String sCurrentLoadedFileName = "";
    private static volatile String sLoadedMetadataLocale = "";
    private static volatile int sPackGeneration = 1;
    private static volatile String sActiveLocaleTag = "zh-CN";
    private static volatile Locale sActiveLocale = Locale.SIMPLIFIED_CHINESE;

    private static volatile int sD3FbResId = 0x7f08016c;
    private static volatile int sD3LrResId = 0x7f08016d;
    private static volatile int sD3UdResId = 0x7f080175;
    private static volatile int sD3RotateLrResId = 0x7f080171;
    private static volatile int sD3RotateUdResId = 0x7f080173;
    private static volatile int sIcAutoSaveResId = 0x7f0e007d;
    private static volatile int sIcCoverLostResId = 0x7f0e00d1;
    private static volatile int sAutoSaveHintResId = 0x7f080073;
    private static volatile int sDraftCoverLostResId = 0x7f080185;
    private static volatile boolean sBitmapResIdsResolved = false;

    private static final Handler sUiHandler = new Handler(Looper.getMainLooper());

    private static void invalidateCachedFlags() {
        sCachedFlagsReady = false;
        refreshCachedLangKind();
    }

    private static void refreshCachedFlags() {
        LibreConfig cfg = LibreConfig.getInstance();
        if (cfg != null) {
            sCachedDynamicI18n = LibreConfig.isFeatureActive(LibreConfig.KEY_DYNAMIC_I18N);
            sCachedTextAutoFit = LibreConfig.isFeatureActive(LibreConfig.KEY_TEXT_AUTO_FIT);
            sCachedCleanSpeedpaint = LibreConfig.isFeatureActive(LibreConfig.KEY_CLEAN_SPEEDPAINT);
            refreshCachedLangKind();
            sCachedFlagsReady = true;
        }
    }

    private static void refreshCachedLangKind() {
        String loc = sLoadedMetadataLocale;
        if (loc != null && !loc.isEmpty()) {
            String lower = loc.toLowerCase(Locale.ROOT);
            sActiveLocaleTag = loc;
            sActiveLocale = TypographyNormalizer.parseLocaleTag(loc, Locale.ROOT);
            if (lower.startsWith("vi")) {
                sCachedLangKind = 1;
                return;
            } else if (lower.startsWith("en")) {
                sCachedLangKind = 2;
                return;
            } else if (lower.startsWith("zh")) {
                sCachedLangKind = 3;
                return;
            } else {
                sCachedLangKind = 0;
                return;
            }
        }
        String f = getEffectiveFileNameLower();
        if (f.startsWith("vi")) {
            sCachedLangKind = 1;
            sActiveLocaleTag = "vi-VN";
            sActiveLocale = TypographyNormalizer.parseLocaleTag("vi-VN", Locale.ENGLISH);
        } else if (f.startsWith("en")) {
            sCachedLangKind = 2;
            sActiveLocaleTag = "en-US";
            sActiveLocale = Locale.US;
        } else if (f.startsWith("zh") || f.startsWith("_template_zh")) {
            sCachedLangKind = 3;
            sActiveLocaleTag = "zh-CN";
            sActiveLocale = Locale.SIMPLIFIED_CHINESE;
        } else {
            sCachedLangKind = 0;
            String base = f.endsWith(".json") ? f.substring(0, f.length() - 5) : f;
            if (!base.isEmpty()) {
                sActiveLocaleTag = base;
                sActiveLocale = TypographyNormalizer.parseLocaleTag(base, Locale.ROOT);
            } else {
                sActiveLocaleTag = "zh-CN";
                sActiveLocale = Locale.SIMPLIFIED_CHINESE;
            }
        }
    }

    public static Locale getActiveLocale() {
        if (!isDynamicI18nEnabled()) {
            Locale def = Locale.getDefault();
            return def != null ? def : Locale.SIMPLIFIED_CHINESE;
        }
        Locale loc = sActiveLocale;
        return loc != null ? loc : Locale.getDefault();
    }

    public static String getActiveLocaleTag() {
        if (!isDynamicI18nEnabled()) {
            Locale def = Locale.getDefault();
            return def != null ? def.toString() : "default";
        }
        String tag = sActiveLocaleTag;
        return tag != null ? tag : "";
    }

    private static final LibreConfig.ConfigChangeListener sConfigListener = key -> {
        if (sInternalConfigUpdate) {
            return;
        }
        if (LibreConfig.KEY_DYNAMIC_I18N.equals(key) || LibreConfig.KEY_LANG_FILE.equals(key)) {
            loadSelectedLanguagePack();
            retranslateActiveViews(sAppContext);
        } else if (LibreConfig.KEY_TEXT_AUTO_FIT.equals(key)
                || LibreConfig.KEY_CANVAS_POPUP_POLISH.equals(key)
                || LibreConfig.KEY_UI_FONT_FILE.equals(key)) {
            invalidateCachedFlags();
            markLayoutDirty();
            retranslateActiveViews(sAppContext);
        }
    };

    private static volatile boolean sSwitchCompatMethodsResolved = false;
    private static Method sSwitchCompatGetOn;
    private static Method sSwitchCompatSetOn;
    private static Method sSwitchCompatGetOff;
    private static Method sSwitchCompatSetOff;

    private TranslationEngine() {}

    /**
     * Single dispatcher bridge for classes4.dex hooks to avoid adding new MethodReferences
     * near the 65,535 method ceiling.
     * Resource IDs (opCode >= 0x7f000000 or opCode < 0) route directly to ID translation.
     */
    public static Object dispatch(int opCode, Object arg) {
        try {
            if (opCode >= 0x7f000000 || opCode < 0) {
                if (arg instanceof android.content.res.Resources) {
                    android.content.res.Resources res = (android.content.res.Resources) arg;
                    String typeName = res.getResourceTypeName(opCode);
                    if ("array".equals(typeName)) {
                        return translateArrayById(opCode, res.getStringArray(opCode));
                    }
                    return translateById(opCode, res.getString(opCode));
                }
                if (arg instanceof android.content.Context) {
                    android.content.res.Resources res = ((android.content.Context) arg).getResources();
                    if (res != null) {
                        String typeName = res.getResourceTypeName(opCode);
                        if ("array".equals(typeName)) {
                            return translateArrayById(opCode, res.getStringArray(opCode));
                        }
                        return translateById(opCode, res.getString(opCode));
                    }
                    return null;
                }
                if (arg instanceof String[]) {
                    return translateArrayById(opCode, (String[]) arg);
                }
                if (arg instanceof CharSequence) {
                    String orig = arg.toString();
                    String trans = translateById(opCode, orig);
                    return orig.equals(trans) ? arg : trans;
                }
                if (arg instanceof Bitmap) {
                    return translateBitmapById(opCode, (Bitmap) arg);
                }
                if (arg instanceof Drawable) {
                    return translateDrawableById(opCode, (Drawable) arg);
                }
                return arg;
            }
            switch (opCode) {
                case OP_TRANSLATE_VIEW: {
                    if (arg instanceof TextView
                            && !(arg instanceof android.widget.Switch)
                            && !isSwitchCompat((View) arg)) {
                        TextView tv = (TextView) arg;
                        if (!sInitialized) {
                            ensureInitialized();
                        }
                        translateTextView(tv);
                        return null;
                    }
                    View v = resolveRootView(arg);
                    if (v != null) {
                        translateViewHierarchy(v);
                    }
                    return null;
                }
                case OP_ATTACH_OBSERVER: {
                    View v = resolveRootView(arg);
                    if (v != null) {
                        if (isRecyclerHolderOrItem(arg, v)) {
                            attachOneShotItemObserver(v);
                        } else {
                            attachViewTreeObserver(v);
                        }
                    }
                    return null;
                }
                case OP_TRANSLATE_AND_OBSERVE: {
                    View v = resolveRootView(arg);
                    if (v != null) {
                        onBubblePopupPreShow(v);
                    }
                    return null;
                }
                case OP_DIALOG_PRE_SHOW:
                    if (arg instanceof Dialog) {
                        onDialogPreShow((Dialog) arg);
                    }
                    return null;
                case OP_TRANSLATE_STRING_ARRAY:
                    if (arg instanceof String[]) {
                        return translateArrayById(0, (String[]) arg);
                    }
                    return arg;
                case OP_TRANSLATE_STRING:
                    if (arg instanceof CharSequence) {
                        if (!isDynamicI18nEnabled()) return arg;
                        String s = arg.toString();
                        String out = containsChinese(s) ? translate(s) : s;
                        return TypographyNormalizer.wrapForCanvas(out, getActiveLocale());
                    }
                    return arg;
                case OP_ON_TOAST_TEXT:
                    if (arg instanceof CharSequence) {
                        return onToastText((CharSequence) arg);
                    }
                    return arg;
                case OP_HIDE_TOPBAR_CLOUD_SYNC:
                    if (arg instanceof View && LibreConfig.isFeatureActive(LibreConfig.KEY_HIDE_TOPBAR_CLOUD)) {
                        if (arg instanceof ViewGroup) {
                            WorkspaceUiController.hideCloudSyncEntry((View) arg);
                        } else {
                            WorkspaceUiController.hideCloudView((View) arg);
                        }
                        return Boolean.TRUE;
                    }
                    return null;
                case OP_IS_HIDE_TOPBAR_CLOUD:
                    return LibreConfig.isFeatureActive(LibreConfig.KEY_HIDE_TOPBAR_CLOUD) ? Boolean.TRUE : null;
                case OP_HIDE_BRUSH_SHOP:
                    if (arg instanceof View) {
                        WorkspaceUiController.hideBrushShopIfEnabled((View) arg);
                    }
                    return null;
                case OP_SHOULD_BLOCK_BRUSH_UPGRADE_TIP:
                    return LibreConfig.isFeatureActive(LibreConfig.KEY_BLOCK_BRUSH_UPGRADE_TIP) ? Boolean.TRUE : null;
                case OP_HIDE_EXPORT_SOCIAL_ICONS:
                    if (arg instanceof View) {
                        WorkspaceUiController.optimizeExportDialog((View) arg);
                    } else if (arg instanceof Object[]) {
                        Object[] arr = (Object[]) arg;
                        if (arr.length >= 3) {
                            GlobalCompatibilityController.sanitizeExportLists(
                                    arr[0],
                                    arr[1] instanceof ArrayList ? (ArrayList) arr[1] : null,
                                    arr[2] instanceof ArrayList ? (ArrayList) arr[2] : null);
                        }
                    }
                    return null;
                case OP_HIDE_VIP_DRAWER:
                    if (arg instanceof View) {
                        WorkspaceUiController.hideDrawerVipIfEnabled((View) arg);
                    }
                    return null;
                case OP_HIDE_BRUSH_LIBRARY_SHOP:
                    if (arg instanceof View) {
                        WorkspaceUiController.hideBrushLibraryShopIfEnabled((View) arg);
                    }
                    return null;
                case OP_HIDE_TIMELAPSE_EXPORT_SOCIAL:
                    if (arg instanceof View) {
                        WorkspaceUiController.optimizeTimelapseExportDialog((View) arg);
                    }
                    return null;
                case OP_SHOULD_ALLOW_BRUSH_EDITING:
                    return LocalBrushController.shouldAllowBrushEditing(false) ? Boolean.TRUE : null;
                case OP_ENSURE_BRUSH_PACKAGE_LIST:
                    return LocalBrushController.ensureBrushPackageList(arg);
                case OP_SYNC_DRAWER_ACCOUNT_VISIBILITY:
                    OfflineAccountController.syncDrawerAccountSafeVisibility(arg);
                    return null;
                case OP_IS_REF_ROTATION_ENABLED:
                    return UiUxController.isReferenceRotationEnabled(false) ? Boolean.TRUE : null;
                case OP_IS_CLEAN_SPEEDPAINT:
                    return isCleanSpeedpaintActive() ? Boolean.TRUE : null;
                case OP_APPLY_TEXT_AUTO_FIT: {
                    if (isTextAutoFitActive()) {
                        View v = resolveRootView(arg);
                        if (v != null && v.getTag(TAG_KEY_OBSERVED) == null) {
                            attachAutoFitObserver(v);
                        }
                    }
                    return null;
                }
                case OP_CANVAS_POPUP_POLISH: {
                    if (WorkspaceUiController.isCanvasPopupPolishActive()) {
                        View v = resolveRootView(arg);
                        if (v != null) {
                            WorkspaceUiController.compactPaintSettingsScrollArea(v);
                            WorkspaceUiController.normalizeThreeButtonCluster(v);
                            WorkspaceUiController.normalizeExportFormatRows(v);
                            WorkspaceUiController.optimizeBlendRow(v);
                            WorkspaceUiController.centerAllIconButtonsRecursively(v);
                            WorkspaceUiController.resizePaintSettingsPopup(v);
                            WorkspaceUiController.expandLeftToolGridIfApplicable(v);
                            WorkspaceUiController.optimizeFastKeyPopups(v);
                        }
                    }
                    return null;
                }
                case OP_FILTER_REF_ROTATION:
                    return UiUxController.filterReferenceRotationDelta(arg);
                case OP_BEFORE_LEFT_SETTING_LAYOUT:
                    UiUxController.onBeforeLeftSettingLayout(arg);
                    return null;
                case OP_AFTER_LEFT_SETTING_LAYOUT:
                    UiUxController.onAfterLeftSettingLayout(arg);
                    return null;
                case OP_ON_LIUYEBI_INIT:
                    WorkspaceUiController.onLiuyebiInitData(arg);
                    return null;
                case OP_ON_TOGGLE_SWITCH_UPDATE:
                    WorkspaceUiController.onToggleSwitchUpdate(arg);
                    return null;
                case OP_DRAW_TOGGLE_SWITCH_LABELS:
                    return WorkspaceUiController.onDrawToggleSwitchLabels(arg);
                case OP_DRAW_HSV_NAME:
                    return WorkspaceUiController.onDrawHsvSeekbarName(arg);
                default:
                    return null;
            }
        } catch (Throwable ignored) {
            if (arg instanceof android.content.res.Resources || arg instanceof android.content.Context) {
                return null;
            }
            if (opCode >= 0x7f000000 || opCode < 0
                    || opCode == OP_TRANSLATE_STRING_ARRAY
                    || opCode == OP_TRANSLATE_STRING
                    || opCode == OP_ON_TOAST_TEXT) {
                return arg;
            }
            return null;
        }
    }

    public static synchronized void init(Context context) {
        if (context == null) return;
        try {
            if (context instanceof Activity) {
                trackActivity((Activity) context);
            }
            boolean needsLoad = !sInitialized || sAppContext == null;
            if (sAppContext == null) {
                sAppContext = context.getApplicationContext() != null ? context.getApplicationContext() : context;
            }
            LibreConfig.init(sAppContext);
            registerActivityTrackerIfNeeded(sAppContext);

            LibreConfig cfg = LibreConfig.getInstance();
            if (cfg != null) {
                cfg.addListener(sConfigListener);
                if (!sFlagListenerRegistered) {
                    sFlagListenerRegistered = true;
                    cfg.addListener(key -> invalidateCachedFlags());
                }
            }
            refreshCachedFlags();

            if (needsLoad) {
                loadSelectedLanguagePack();
                sInitialized = true;
            } else {
                checkAndReloadIfChanged();
            }
        } catch (Throwable t) {
            Log.e(TAG, "Failed to initialize TranslationEngine", t);
        }
    }

    private static void registerActivityTrackerIfNeeded(Context context) {
        if (sLifecycleRegistered || !(context instanceof Application)) {
            return;
        }
        sLifecycleRegistered = true;
        ((Application) context).registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                trackActivity(activity);
            }

            @Override
            public void onActivityStarted(Activity activity) {
                trackActivity(activity);
            }

            @Override
            public void onActivityResumed(Activity activity) {
                trackActivity(activity);
            }

            @Override
            public void onActivityPaused(Activity activity) {}

            @Override
            public void onActivityStopped(Activity activity) {}

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

            @Override
            public void onActivityDestroyed(Activity activity) {
                untrackActivity(activity);
            }
        });
    }

    private static void trackActivity(Activity activity) {
        if (activity == null) return;
        synchronized (sTrackedActivities) {
            Iterator<WeakReference<Activity>> it = sTrackedActivities.iterator();
            boolean exists = false;
            while (it.hasNext()) {
                Activity a = it.next().get();
                if (a == null || a.isFinishing()) {
                    it.remove();
                } else if (a == activity) {
                    exists = true;
                }
            }
            if (!exists && !activity.isFinishing()) {
                sTrackedActivities.add(new WeakReference<>(activity));
            }
        }
    }

    private static void untrackActivity(Activity activity) {
        if (activity == null) return;
        synchronized (sTrackedActivities) {
            Iterator<WeakReference<Activity>> it = sTrackedActivities.iterator();
            while (it.hasNext()) {
                Activity a = it.next().get();
                if (a == null || a == activity) {
                    it.remove();
                }
            }
        }
    }

    public static boolean isDynamicI18nEnabled() {
        if (!sCachedFlagsReady) {
            refreshCachedFlags();
            if (!sCachedFlagsReady) {
                return LibreConfig.isFeatureActive(LibreConfig.KEY_DYNAMIC_I18N);
            }
        }
        return sCachedDynamicI18n;
    }

    public static File getLanguageDirectory(Context context) {
        try {
            // 1. Primary: /sdcard/HuashijiePro/lang
            File sdcardDir = new File(Environment.getExternalStorageDirectory(), SDCARD_DIR_NAME + "/" + LANG_FOLDER_NAME);
            if (sdcardDir.exists() || sdcardDir.mkdirs()) {
                return sdcardDir;
            }
        } catch (Throwable ignored) {}

        // 2. Fallback: app external files dir
        if (context != null) {
            try {
                File extFiles = context.getExternalFilesDir(null);
                if (extFiles != null) {
                    File appLangDir = new File(extFiles, LANG_FOLDER_NAME);
                    if (appLangDir.exists() || appLangDir.mkdirs()) {
                        return appLangDir;
                    }
                }
            } catch (Throwable ignored) {}
        }
        return null;
    }

    public static List<File> getAvailableLanguageFiles(Context context) {
        List<File> list = new ArrayList<>();
        try {
            File langDir = getLanguageDirectory(context);
            if (langDir != null && langDir.isDirectory()) {
                File[] files = langDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".json") && !name.startsWith("_template"));
                if (files != null) {
                    for (File f : files) {
                        list.add(f);
                    }
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "Error listing language files", t);
        }
        return list;
    }

    public static String getEffectiveSelectedLanguageFile(File langDir) {
        try {
            if (langDir != null) {
                File overrideFile = new File(langDir, "selected_lang.txt");
                if (overrideFile.exists() && overrideFile.isFile()) {
                    long mod = overrideFile.lastModified();
                    boolean preferOverride = (sLastOverrideFileMod == 0 || mod != sLastOverrideFileMod
                            || LibreConfig.getInstance() == null);
                    if (preferOverride) {
                        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(overrideFile), StandardCharsets.UTF_8))) {
                            String line = br.readLine();
                            if (line != null) {
                                String trimmed = line.trim();
                                if (!trimmed.isEmpty()) {
                                    String clean = trimmed.endsWith(".json") ? trimmed : trimmed + ".json";
                                    sLastOverrideFileMod = mod;
                                    if (LibreConfig.getInstance() != null && !clean.equals(LibreConfig.getInstance().getSelectedLanguageFile())) {
                                        sInternalConfigUpdate = true;
                                        try {
                                            LibreConfig.getInstance().setSelectedLanguageFile(clean);
                                        } finally {
                                            sInternalConfigUpdate = false;
                                        }
                                    }
                                    return clean;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
        if (LibreConfig.getInstance() != null) {
            String cfg = LibreConfig.getInstance().getSelectedLanguageFile();
            if (cfg != null && !cfg.trim().isEmpty()) {
                return cfg.trim().endsWith(".json") ? cfg.trim() : cfg.trim() + ".json";
            }
        }
        return "vi.json";
    }

    private static void copyAssetIfMissing(Context context, File destFile, String assetPath) {
        if (context == null || destFile == null) return;
        if (destFile.exists() && destFile.isFile()) return;
        try {
            File parent = destFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try (InputStream is = context.getAssets().open(assetPath);
                 FileOutputStream fos = new FileOutputStream(destFile)) {
                byte[] buf = new byte[8192];
                int r;
                while ((r = is.read(buf)) != -1) {
                    fos.write(buf, 0, r);
                }
            }
        } catch (Throwable ignored) {}
    }

    private static byte[] readAllBytes(InputStream is) throws Exception {
        if (is == null) return null;
        int hint = 0;
        try {
            hint = is.available();
        } catch (Throwable ignored) {}
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream(Math.max(16384, hint));
        byte[] buf = new byte[16384];
        int r;
        while ((r = is.read(buf)) != -1) {
            baos.write(buf, 0, r);
        }
        return baos.toByteArray();
    }

    private static void writeBytesToFile(File destFile, byte[] data) {
        if (destFile == null || data == null) return;
        try {
            File parent = destFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try (FileOutputStream fos = new FileOutputStream(destFile)) {
                fos.write(data);
            }
        } catch (Throwable ignored) {}
    }

    private static void scheduleTemplateCopyInBackground(final Context ctx, final File templateFile) {
        if (sTemplateCopyScheduled || ctx == null || templateFile == null || templateFile.exists()) {
            return;
        }
        sTemplateCopyScheduled = true;
        Thread t = new Thread(() -> copyAssetIfMissing(ctx, templateFile, "lang/_template_zh.json"), "libre-lang-template");
        t.setDaemon(true);
        t.start();
    }

    private static void scheduleLanguageAssetCopyInBackground(final File destFile, final byte[] data) {
        if (destFile == null || data == null) return;
        Thread t = new Thread(() -> {
            writeBytesToFile(destFile, data);
            try {
                if (destFile.exists()) {
                    sLastLoadedFileMod = destFile.lastModified();
                }
            } catch (Throwable ignored) {}
        }, "libre-lang-export");
        t.setDaemon(true);
        t.start();
    }

    private static boolean applyLanguageJsonBytes(byte[] jsonBytes) {
        if (jsonBytes == null || jsonBytes.length == 0) return false;
        try (JsonReader reader = new JsonReader(new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(jsonBytes), StandardCharsets.UTF_8), 8192))) {
            reader.beginObject();
            while (reader.hasNext()) {
                String section = reader.nextName();
                if ("strings_by_id".equals(section)) {
                    if (reader.peek() == JsonToken.BEGIN_OBJECT) {
                        reader.beginObject();
                        while (reader.hasNext()) {
                            String hexKey = reader.nextName();
                            if (reader.peek() == JsonToken.STRING) {
                                String translated = reader.nextString();
                                if (!translated.isEmpty()) {
                                    try {
                                        int id = hexKey.startsWith("0x") || hexKey.startsWith("0X")
                                                ? (int) Long.parseLong(hexKey.substring(2), 16)
                                                : Integer.parseInt(hexKey);
                                        sIdMap.put(id, translated);
                                    } catch (Throwable ignoredKey) {}
                                }
                            } else {
                                reader.skipValue();
                            }
                        }
                        reader.endObject();
                    } else {
                        reader.skipValue();
                    }
                } else if ("strings_by_text".equals(section)
                        || "strings_by_value".equals(section)
                        || "strings".equals(section)) {
                    if (reader.peek() == JsonToken.BEGIN_OBJECT) {
                        reader.beginObject();
                        while (reader.hasNext()) {
                            String origText = reader.nextName();
                            if (reader.peek() == JsonToken.STRING) {
                                String translated = reader.nextString();
                                if (!translated.isEmpty()) {
                                    sTextMap.put(origText, translated);
                                }
                            } else {
                                reader.skipValue();
                            }
                        }
                        reader.endObject();
                    } else {
                        reader.skipValue();
                    }
                } else if ("__metadata__".equals(section)) {
                    if (reader.peek() == JsonToken.BEGIN_OBJECT) {
                        reader.beginObject();
                        while (reader.hasNext()) {
                            String metaKey = reader.nextName();
                            if ("locale".equals(metaKey) && reader.peek() == JsonToken.STRING) {
                                String loc = reader.nextString();
                                if (loc != null && !loc.trim().isEmpty()) {
                                    sLoadedMetadataLocale = loc.trim().toLowerCase(Locale.ROOT);
                                }
                            } else {
                                reader.skipValue();
                            }
                        }
                        reader.endObject();
                    } else {
                        reader.skipValue();
                    }
                } else if ("strings_by_name".equals(section)) {
                    reader.skipValue();
                } else if (reader.peek() == JsonToken.STRING) {
                    String val = reader.nextString();
                    if (!val.isEmpty()) {
                        sTextMap.put(section, val);
                    }
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "Failed to parse language pack JSON", t);
            return false;
        }
    }

    public static synchronized void loadSelectedLanguagePack() {
        try {
            sIdMap.clear();
            sTextMap.clear();
            sRuntimeTranslationCache.clear();
            sLoadedMetadataLocale = "";
            sPackGeneration++;

            if (!isDynamicI18nEnabled()) {
                refreshCachedLangKind();
                Log.i(TAG, "Dynamic i18n disabled by user config");
                return;
            }

            File langDir = getLanguageDirectory(sAppContext);
            String selectedFile = getEffectiveSelectedLanguageFile(langDir);
            sCurrentLoadedFileName = selectedFile;
            refreshCachedLangKind();

            byte[] assetBytes = null;
            if (sAppContext != null) {
                try (InputStream is = sAppContext.getAssets().open("lang/" + selectedFile)) {
                    assetBytes = readAllBytes(is);
                } catch (Throwable ignoredAsset) {}
            }

            byte[] extBytes = null;
            if (langDir != null) {
                File targetFile = new File(langDir, selectedFile);
                scheduleTemplateCopyInBackground(sAppContext, new File(langDir, "_template_zh.json"));
                if (!targetFile.exists() && assetBytes != null) {
                    scheduleLanguageAssetCopyInBackground(targetFile, assetBytes);
                } else if (targetFile.exists() && targetFile.isFile() && targetFile.canRead()) {
                    sLastLoadedFileMod = targetFile.lastModified();
                    try (FileInputStream fis = new FileInputStream(targetFile)) {
                        extBytes = readAllBytes(fis);
                    } catch (Throwable ignoredRead) {}
                }
            }

            boolean loadedAny = false;
            if (assetBytes != null && extBytes != null && java.util.Arrays.equals(assetBytes, extBytes)) {
                loadedAny = applyLanguageJsonBytes(assetBytes);
            } else {
                if (assetBytes != null) {
                    loadedAny |= applyLanguageJsonBytes(assetBytes);
                }
                if (extBytes != null) {
                    loadedAny |= applyLanguageJsonBytes(extBytes);
                }
            }

            refreshCachedLangKind();

            if (!loadedAny) {
                Log.i(TAG, "Language pack unavailable: " + selectedFile);
                return;
            }

            bridgeTemplateIdsToTextMap();
            Log.i(TAG, "Loaded language pack: " + selectedFile + " (" + sIdMap.size() + " ID keys, " + sTextMap.size() + " text keys)");
        } catch (Throwable t) {
            Log.e(TAG, "Failed to load language pack", t);
        }
    }

    private static void bridgeTemplateIdsToTextMap() {
        if (sAppContext == null || sIdMap.isEmpty()) return;
        try (InputStream is = sAppContext.getAssets().open("lang/_template_zh.json");
             JsonReader reader = new JsonReader(new BufferedReader(
                     new InputStreamReader(is, StandardCharsets.UTF_8), 8192))) {
            reader.beginObject();
            while (reader.hasNext()) {
                String section = reader.nextName();
                if ("strings_by_id".equals(section) && reader.peek() == JsonToken.BEGIN_OBJECT) {
                    reader.beginObject();
                    while (reader.hasNext()) {
                        String idKey = reader.nextName();
                        if (reader.peek() != JsonToken.STRING) {
                            reader.skipValue();
                            continue;
                        }
                        String zhText = reader.nextString();
                        if (zhText.isEmpty()) continue;
                        int id;
                        try {
                            id = (idKey.startsWith("0x") || idKey.startsWith("0X"))
                                    ? (int) Long.parseLong(idKey.substring(2), 16)
                                    : Integer.parseInt(idKey);
                        } catch (Throwable ignoredKey) {
                            continue;
                        }
                        String trans = sIdMap.get(id);
                        if (trans == null || trans.isEmpty()) continue;
                        sTextMap.putIfAbsent(zhText, trans);
                        String trimmed = zhText.trim();
                        if (!trimmed.isEmpty()) {
                            sTextMap.putIfAbsent(trimmed, trans);
                        }
                    }
                    reader.endObject();
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
        } catch (Throwable ignored) {}
    }

    public static synchronized void checkAndReloadIfChanged() {
        try {
            if (sAppContext == null) return;
            File langDir = getLanguageDirectory(sAppContext);
            if (langDir != null) {
                File overrideFile = new File(langDir, "selected_lang.txt");
                long overrideMod = overrideFile.exists() ? overrideFile.lastModified() : 0;
                String selectedFile = getEffectiveSelectedLanguageFile(langDir);
                File targetFile = new File(langDir, selectedFile);
                long targetMod = targetFile.exists() ? targetFile.lastModified() : 0;

                if (!selectedFile.equals(sCurrentLoadedFileName)
                        || targetMod != sLastLoadedFileMod
                        || overrideMod != sLastOverrideFileMod) {
                    sLastOverrideFileMod = overrideMod;
                    sCurrentLoadedFileName = selectedFile;
                    loadSelectedLanguagePack();
                    retranslateActiveViews(sAppContext);
                }
            }
        } catch (Throwable ignored) {}
    }

    private static boolean isProtectedFilesystemResId(int resId) {
        if (!sProtectedIdsResolved && sAppContext != null) {
            int sdId = LibreResourceResolver.resolveStringId(sAppContext, "sdcard_root_folder", 0x7f110803);
            int albId = LibreResourceResolver.resolveStringId(sAppContext, "album_folder", 0x7f110077);
            if (sdId != 0) sProtectedSdcardRootId = sdId;
            if (albId != 0) sProtectedAlbumFolderId = albId;
            sProtectedIdsResolved = true;
        }
        return resId == 0x7f110803
                || resId == 0x7f110077
                || resId == sProtectedSdcardRootId
                || resId == sProtectedAlbumFolderId;
    }

    /**
     * Primary hook point for AssetsUtil.getString(int) and Resources.getString(int).
     */
    public static String translateById(int resId, String original) {
        if (isProtectedFilesystemResId(resId)) {
            return original;
        }
        if (original == null || !isDynamicI18nEnabled()) {
            return original;
        }
        if (!sInitialized) {
            ensureInitialized();
        }
        try {
            String translated = sIdMap.get(resId);
            if (translated != null && !translated.isEmpty()) {
                return translated;
            }
            String textMatch = sTextMap.get(original);
            if (textMatch != null && !textMatch.isEmpty()) {
                return textMatch;
            }
            if (containsChinese(original)) {
                return translate(original);
            }
        } catch (Throwable ignored) {}
        return original;
    }

    public static String translateDynamic(int resId, String orig) {
        return translateById(resId, orig);
    }

    /**
     * Retrieves a localized string by key or original text, falling back to defaultVal.
     */
    public static String get(String key, String defaultVal) {
        if (key == null || !isDynamicI18nEnabled()) {
            return defaultVal;
        }
        if (!sInitialized) {
            ensureInitialized();
        }
        try {
            String translated = sTextMap.get(key);
            if (translated != null && !translated.isEmpty()) {
                return translated;
            }
        } catch (Throwable ignored) {}
        return defaultVal;
    }

    public static String getStats() {
        return sIdMap.size() + " ID keys, " + sTextMap.size() + " text keys";
    }

    public static String getLocalizedStats() {
        String fmt = get("libre_stats_format", "%1$d 项 ID，%2$d 项文本");
        try {
            return String.format(fmt, sIdMap.size(), sTextMap.size());
        } catch (Throwable ignored) {
            return sIdMap.size() + " / " + sTextMap.size();
        }
    }

    public static synchronized void loadLanguagePack(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) return;
        String clean = fileName.trim().endsWith(".json") ? fileName.trim() : fileName.trim() + ".json";
        sCurrentLoadedFileName = clean;
        try {
            File langDir = getLanguageDirectory(sAppContext);
            if (langDir != null) {
                File overrideFile = new File(langDir, "selected_lang.txt");
                try (FileOutputStream fos = new FileOutputStream(overrideFile)) {
                    fos.write(clean.getBytes(StandardCharsets.UTF_8));
                }
                sLastOverrideFileMod = overrideFile.lastModified();
            }
        } catch (Throwable ignored) {}
        if (LibreConfig.getInstance() != null) {
            sInternalConfigUpdate = true;
            try {
                LibreConfig.getInstance().setSelectedLanguageFile(clean);
            } finally {
                sInternalConfigUpdate = false;
            }
        }
        loadSelectedLanguagePack();
    }

    /**
     * Toggles dynamic i18n while suppressing ConfigChangeListener so JSON loading
     * and view retranslation execute exactly once.
     */
    public static void setDynamicI18nEnabled(Activity activity, boolean enabled) {
        LibreConfig cfg = activity != null ? LibreConfig.get(activity) : LibreConfig.getInstance();
        if (cfg != null) {
            sInternalConfigUpdate = true;
            try {
                cfg.setEnabled(LibreConfig.KEY_DYNAMIC_I18N, enabled);
            } finally {
                sInternalConfigUpdate = false;
            }
        }
        invalidateCachedFlags();
        loadSelectedLanguagePack();
        retranslateActiveViews(activity != null ? activity : sAppContext);
    }

    /**
     * Hook point for AssetsUtil.getStringArray(int) and Resources.getStringArray(int).
     */
    public static String[] translateArrayById(int resId, String[] original) {
        if (original == null || !isDynamicI18nEnabled()) {
            return original;
        }
        if (!sInitialized) {
            ensureInitialized();
        }
        try {
            String[] result = new String[original.length];
            for (int i = 0; i < original.length; i++) {
                result[i] = translate(original[i]);
            }
            return result;
        } catch (Throwable ignored) {}
        return original;
    }

    public static void ensureInitialized() {
        if (sInitialized) return;
        synchronized (TranslationEngine.class) {
            if (sInitialized) return;
            try {
                loadSelectedLanguagePack();
                sInitialized = true;
            } catch (Throwable ignored) {}
        }
    }

    private static boolean isInternalTechnicalLog(String s) {
        return s.indexOf('=') >= 0
                || s.startsWith("ACTION_")
                || s.startsWith("---")
                || s.startsWith("===")
                || s.contains("onAd")
                || s.contains("SplashAd")
                || s.contains("initView")
                || s.contains("beizi")
                || s.contains("MEISHU")
                || s.contains("LingYe");
    }

    /**
     * Translates strings decrypted at runtime by StringFog.
     */
    public static String translateStringFog(String str) {
        if (str == null || str.isEmpty() || !containsChinese(str) || isInternalTechnicalLog(str) || !isDynamicI18nEnabled()) {
            return str;
        }
        if (!sInitialized) {
            ensureInitialized();
        }
        try {
            return translate(str);
        } catch (Throwable ignored) {
            return str;
        }
    }

    /**
     * Hook point for TextView.setText(CharSequence).
     */
    public static CharSequence translateCharSequence(CharSequence cs) {
        if (cs == null || cs.length() == 0 || !isDynamicI18nEnabled()) {
            return cs;
        }
        if (!sInitialized) {
            ensureInitialized();
        }
        try {
            if (!containsChinese(cs)) {
                return cs;
            }
            return translate(cs.toString());
        } catch (Throwable ignored) {
            return cs;
        }
    }

    /**
     * Hook point for Toast.makeText(Context, CharSequence, int).
     */
    public static CharSequence onToastText(CharSequence text) {
        if (text == null || text.length() == 0 || !isDynamicI18nEnabled()) {
            return text;
        }
        if (!sInitialized) {
            ensureInitialized();
        }
        try {
            if (!containsChinese(text)) {
                return text;
            }
            return translate(text.toString());
        } catch (Throwable ignored) {
            return text;
        }
    }

    /**
     * Hook point replacing Toast.makeText(Context, int, int).
     */
    public static Toast makeTranslatedToast(Context ctx, int resId, int duration) {
        if (ctx == null) {
            return Toast.makeText(null, resId, duration);
        }
        try {
            String orig = ctx.getResources().getString(resId);
            String text = translateById(resId, orig);
            return Toast.makeText(ctx, text, duration);
        } catch (Throwable ignored) {
            return Toast.makeText(ctx, resId, duration);
        }
    }

    public static boolean isTextAutoFitActive() {
        if (!sCachedFlagsReady) {
            refreshCachedFlags();
            if (!sCachedFlagsReady) {
                return LibreConfig.isFeatureActive(LibreConfig.KEY_TEXT_AUTO_FIT);
            }
        }
        return sCachedTextAutoFit;
    }

    public static boolean isCleanSpeedpaintActive() {
        if (!sCachedFlagsReady) {
            refreshCachedFlags();
            if (!sCachedFlagsReady) {
                return LibreConfig.isFeatureActive(LibreConfig.KEY_CLEAN_SPEEDPAINT);
            }
        }
        return sCachedCleanSpeedpaint;
    }

    public static void attachTextAutoFitToActivity(Activity activity) {
        if (activity == null || activity.isFinishing()) return;
        try {
            if (!isTextAutoFitActive() || activity.getWindow() == null) return;
            View decor = activity.getWindow().getDecorView();
            if (decor != null) {
                if (decor.getTag(TAG_KEY_HOST_ACTIVITY_CLASS) == null) {
                    decor.setTag(TAG_KEY_HOST_ACTIVITY_CLASS, activity.getClass().getName());
                }
                attachAutoFitObserver(decor);
            }
        } catch (Throwable ignored) {}
    }

    public static void onDialogAutoFitPreShow(Dialog dialog) {
        if (dialog == null) return;
        try {
            if (!isTextAutoFitActive() || dialog.getWindow() == null) return;
            View decor = dialog.getWindow().getDecorView();
            if (decor != null) {
                decor.setTag(TAG_KEY_DIALOG_ROOT, Boolean.TRUE);
                attachAutoFitObserver(decor);
            }
        } catch (Throwable ignored) {}
    }

    public static void attachAutoFitObserver(final View rootView) {
        if (rootView == null) return;
        try {
            if (!isTextAutoFitActive()) return;
            attachUntrackedWindowRootsForAutoFit();
            // Unified observer already runs autofit and layout polish on predraw and trailing layout.
            if (rootView.getTag(TAG_KEY_OBSERVED) != null) {
                return;
            }
            applyTextAutoFitSubtree(rootView);
            if (rootView.getTag(TAG_KEY_AUTOFIT_OBSERVED) == null) {
                rootView.setTag(TAG_KEY_AUTOFIT_OBSERVED, Boolean.TRUE);
                final ViewTreeObserver.OnPreDrawListener preDrawFit = new ViewTreeObserver.OnPreDrawListener() {
                    private boolean mCanceledOnce = false;
                    @Override
                    public boolean onPreDraw() {
                        if (rootView.getTag(TAG_KEY_OBSERVED) == null && isTextAutoFitActive()) {
                            attachUntrackedWindowRootsForAutoFit();
                            boolean fitChanged = applyTextAutoFitSubtree(rootView);
                            if ((fitChanged || rootView.isLayoutRequested()) && !mCanceledOnce) {
                                mCanceledOnce = true;
                                rootView.requestLayout();
                                return false;
                            }
                        }
                        try {
                            rootView.getViewTreeObserver().removeOnPreDrawListener(this);
                        } catch (Throwable ignored) {}
                        return true;
                    }
                };
                rootView.getViewTreeObserver().addOnPreDrawListener(preDrawFit);
                rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    private long mLastFitRun = 0L;
                    @Override
                    public void onGlobalLayout() {
                        if (rootView.getTag(TAG_KEY_OBSERVED) != null || !isTextAutoFitActive()) return;
                        long now = System.currentTimeMillis();
                        if (now - mLastFitRun > 120L) {
                            mLastFitRun = now;
                            try {
                                attachUntrackedWindowRootsForAutoFit();
                                applyTextAutoFitSubtree(rootView);
                            } catch (Throwable ignored) {}
                        }
                    }
                });
            }
        } catch (Throwable ignored) {}
    }

    public static boolean ensureSafeVietnameseFontPadding(TextView tv) {
        if (tv == null) return false;
        CharSequence cs = tv.getText();
        if (cs == null || cs.length() == 0) return false;
        Context ctx = tv.getContext();
        Typeface activeTf = ctx != null ? UIFontManager.getActiveTypeface(ctx) : null;
        return TypographyNormalizer.prepareTextViewForPipeline(
                tv, cs, getActiveLocale(), activeTf, isDynamicI18nEnabled());
    }

    private static Activity unwrapActivity(Context ctx) {
        for (int i = 0; i < 8 && ctx instanceof android.content.ContextWrapper; i++) {
            if (ctx instanceof Activity) {
                return (Activity) ctx;
            }
            ctx = ((android.content.ContextWrapper) ctx).getBaseContext();
        }
        return (ctx instanceof Activity) ? (Activity) ctx : null;
    }

    public static String resolveHostActivityName(View view) {
        if (view == null) return "";
        Object tag = view.getTag(TAG_KEY_HOST_ACTIVITY_CLASS);
        if (tag instanceof String) {
            return (String) tag;
        }
        View root = view.getRootView();
        if (root != null) {
            Object rootTag = root.getTag(TAG_KEY_HOST_ACTIVITY_CLASS);
            if (rootTag instanceof String) {
                return (String) rootTag;
            }
        }
        Activity direct = unwrapActivity(view.getContext());
        if (direct != null) {
            return direct.getClass().getName();
        }
        View searchRoot = root != null ? root : view;
        View content = searchRoot.findViewById(android.R.id.content);
        if (content instanceof ViewGroup && ((ViewGroup) content).getChildCount() > 0) {
            Activity childAct = unwrapActivity(((ViewGroup) content).getChildAt(0).getContext());
            if (childAct != null) {
                return childAct.getClass().getName();
            }
        }
        return "";
    }

    private static boolean isPopupOrDialogRoot(View rootView) {
        if (rootView == null) return false;
        if (Boolean.TRUE.equals(rootView.getTag(TAG_KEY_DIALOG_ROOT))) {
            return true;
        }
        if (WorkspaceUiController.hasBoundPopupWindow(rootView)
                || WorkspaceUiController.isSettingPopupRoot(rootView)) {
            return true;
        }
        if (rootView.getTag(TAG_KEY_HOST_ACTIVITY_CLASS) != null) {
            return false;
        }
        View winRoot = rootView.getRootView();
        if (winRoot != null && winRoot != rootView) {
            if (Boolean.TRUE.equals(winRoot.getTag(TAG_KEY_DIALOG_ROOT))
                    || WorkspaceUiController.hasBoundPopupWindow(winRoot)
                    || WorkspaceUiController.isSettingPopupRoot(winRoot)) {
                return true;
            }
            if (winRoot.getTag(TAG_KEY_HOST_ACTIVITY_CLASS) != null
                    || winRoot.getClass().getName().endsWith("DecorView")) {
                return false;
            }
        }
        String hostAct = resolveHostActivityName(rootView);
        if (rootView.getClass().getName().endsWith("DecorView") && !hostAct.isEmpty()) {
            return false;
        }
        return hostAct.isEmpty()
                || (rootView.getParent() != null
                        && !rootView.getParent().getClass().getName().endsWith("ViewRootImpl"));
    }

    private static boolean isInsideMainActivityWindow(TextView tv) {
        if (tv == null) return false;
        if (WorkspaceUiController.hasBoundPopupWindow(tv)) {
            return false;
        }
        View root = tv.getRootView();
        if (root == null) root = tv;
        if (WorkspaceUiController.hasBoundPopupWindow(root)
                || Boolean.TRUE.equals(root.getTag(TAG_KEY_DIALOG_ROOT))) {
            return false;
        }
        String hostAct = resolveHostActivityName(tv);
        return !hostAct.isEmpty() && !hostAct.endsWith("PaintActivity");
    }

    private static boolean applyTextAutoFitSubtree(View rootView) {
        boolean fitActive = isTextAutoFitActive();
        boolean polishActive = isPopupOrDialogRoot(rootView)
                && WorkspaceUiController.isCanvasPopupPolishActive();
        if (polishActive) {
            WorkspaceUiController.normalizeThreeButtonCluster(rootView);
            WorkspaceUiController.normalizeExportFormatRows(rootView);
            WorkspaceUiController.optimizeBlendRow(rootView);
            WorkspaceUiController.centerAllIconButtonsRecursively(rootView);
            WorkspaceUiController.compactPaintSettingsScrollArea(rootView);
        }
        boolean changed = false;
        if (fitActive && isPopupOrDialogRoot(rootView)) {
            changed |= WorkspaceUiController.normalizeDialogTitles(rootView);
        }
        changed |= applyTextAutoFitHierarchyTracked(rootView);
        if (fitActive) {
            changed |= WorkspaceUiController.optimizeElasticPopupMenus(rootView);
            changed |= WorkspaceUiController.solveAllCanvasStripPeerGroups(rootView);
        } else {
            changed |= WorkspaceUiController.restoreAllSolverStates(rootView);
        }
        if (polishActive) {
            WorkspaceUiController.resizePaintSettingsPopup(rootView);
            WorkspaceUiController.expandLeftToolGridIfApplicable(rootView);
            WorkspaceUiController.optimizeFastKeyPopups(rootView);
        }
        return changed;
    }

    private static View resolveRootView(Object arg) {
        if (arg instanceof View) {
            return (View) arg;
        }
        if (arg instanceof PopupWindow) {
            PopupWindow pw = (PopupWindow) arg;
            View cv = pw.getContentView();
            if (cv != null) {
                WorkspaceUiController.bindHostPopupWindow(cv, pw);
            }
            return cv;
        }
        if (arg instanceof Dialog) {
            android.view.Window win = ((Dialog) arg).getWindow();
            return win != null ? win.getDecorView() : null;
        }
        if (arg != null) {
            try {
                Class<?> cls = arg.getClass();
                Field itemViewField = sItemViewFieldCache.get(cls);
                if (itemViewField == null) {
                    itemViewField = cls.getField("itemView");
                    sItemViewFieldCache.put(cls, itemViewField);
                }
                Object itemView = itemViewField.get(arg);
                if (itemView instanceof View) {
                    return (View) itemView;
                }
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private static boolean isRecyclerHolderOrItem(Object arg, View v) {
        if (arg != null
                && !(arg instanceof View)
                && !(arg instanceof Dialog)
                && !(arg instanceof PopupWindow)) {
            return true;
        }
        if (v != null && v.getParent() != null) {
            String pName = v.getParent().getClass().getName();
            if (pName.endsWith("RecyclerView") || pName.endsWith("ListView") || pName.endsWith("GridView")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isInsideHorizontalScrollView(View v) {
        ViewParent p = v != null ? v.getParent() : null;
        int depth = 0;
        while (p != null && depth < 8) {
            if (p instanceof android.widget.HorizontalScrollView) {
                return true;
            }
            p = p.getParent();
            depth++;
        }
        return false;
    }

    private static boolean isSingleLineView(TextView tv, int maxLines) {
        if (maxLines == 1) return true;
        try {
            if (tv.getTransformationMethod() instanceof android.text.method.SingleLineTransformationMethod) {
                return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private static boolean isVerticalIconWithText(TextView tv) {
        if (tv == null) return false;
        try {
            android.graphics.drawable.Drawable[] cd = tv.getCompoundDrawables();
            if (cd != null && cd.length > 1 && cd[1] != null) return true;
            if (Build.VERSION.SDK_INT >= 17) {
                android.graphics.drawable.Drawable[] cdr = tv.getCompoundDrawablesRelative();
                if (cdr != null && cdr.length > 1 && cdr[1] != null) return true;
            }
            ViewParent p = tv.getParent();
            if (p instanceof LinearLayout && ((LinearLayout) p).getOrientation() == LinearLayout.VERTICAL) {
                LinearLayout ll = (LinearLayout) p;
                for (int i = 0; i < ll.getChildCount(); i++) {
                    View ch = ll.getChildAt(i);
                    if (ch instanceof android.widget.ImageView && ch.getVisibility() != View.GONE) {
                        return ch.getBottom() <= tv.getTop() + 8 || ll.indexOfChild(ch) < ll.indexOfChild(tv);
                    }
                }
            } else if (p instanceof android.widget.RelativeLayout) {
                android.widget.RelativeLayout rl = (android.widget.RelativeLayout) p;
                for (int i = 0; i < rl.getChildCount(); i++) {
                    View ch = rl.getChildAt(i);
                    if (ch instanceof android.widget.ImageView && ch.getVisibility() != View.GONE) {
                        if (ch.getBottom() > 0 && tv.getTop() > 0 && ch.getBottom() <= tv.getTop() + 8) {
                            return true;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private static boolean isInsideListOrSettingsRow(View v) {
        ViewParent p = v != null ? v.getParent() : null;
        int depth = 0;
        while (p != null && depth < 6) {
            if (p instanceof android.widget.ListView || p instanceof android.widget.ScrollView) {
                return true;
            }
            if (p instanceof View && WorkspaceUiController.isSettingScrollOrInfoContainer((View) p)) {
                return true;
            }
            p = p.getParent();
            depth++;
        }
        return false;
    }

    private static boolean isDynamicNumericReadoutText(String str) {
        if (str == null) return false;
        String s = str.trim();
        if (s.isEmpty()) return false;
        if (s.startsWith("#") && s.length() <= 10) return true;
        if (s.startsWith("/") || s.contains("/HuashijiePro/")) return true;
        return PATTERN_DYNAMIC_NUMERIC.matcher(s).matches();
    }

    /**
     * Measures the widest whitespace-delimited token without allocating regex matchers or String arrays.
     */
    public static float computeMaxWordWidth(TextPaint paint, String str) {
        if (paint == null || str == null || str.isEmpty()) return 0f;
        float maxW = 0f;
        int n = str.length();
        int i = 0;
        while (i < n) {
            while (i < n && Character.isWhitespace(str.charAt(i))) {
                i++;
            }
            if (i >= n) break;
            int start = i;
            while (i < n && !Character.isWhitespace(str.charAt(i))) {
                i++;
            }
            float w = paint.measureText(str, start, i);
            if (w > maxW) maxW = w;
        }
        return maxW;
    }

    public static void applyTextAutoFitHierarchy(View view) {
        applyTextAutoFitHierarchyTracked(view);
    }

    public static boolean applyTextAutoFitHierarchyTracked(View view) {
        if (view == null || view.getVisibility() == View.GONE || !isTextAutoFitActive()) return false;
        boolean changed = false;
        try {
            if ("libre_settings_root".equals(view.getTag())) {
                return false;
            }
            if (view instanceof TextView) {
                TextView tv = (TextView) view;
                changed |= enforceCenterAlignmentIfApplicable(tv);
                changed |= fitNarrowTextView(tv);
            }
            if (view instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) view;
                int childCount = vg.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    changed |= applyTextAutoFitHierarchyTracked(vg.getChildAt(i));
                }
            }
        } catch (Throwable ignored) {}
        return changed;
    }

    private static volatile int sIdShapeRgbToOpacityLayout = -1;

    private static boolean fitConstrainedShapeButton(TextView tv) {
        if (tv == null) return false;
        ViewParent parent = tv.getParent();
        if (!(parent instanceof android.widget.RelativeLayout)) return false;
        ViewGroup vg = (ViewGroup) parent;
        if (sIdShapeRgbToOpacityLayout == -1) {
            sIdShapeRgbToOpacityLayout = LibreResourceResolver.resolveViewId(
                    tv.getContext(), "shape_rgb_to_opacity_layout", 0);
        }
        if (sIdShapeRgbToOpacityLayout == 0 || vg.getId() != sIdShapeRgbToOpacityLayout) {
            return false;
        }
        try {
            int width = tv.getWidth();
            android.text.Layout layout = tv.getLayout();
            if (width <= 0 || layout == null) return false;
            float density = tv.getResources().getDisplayMetrics().density;
            int minPadRight = Math.round(10 * density);
            boolean mutated = false;
            if (tv.getPaddingRight() < minPadRight) {
                tv.setPadding(tv.getPaddingLeft(), tv.getPaddingTop(), minPadRight, tv.getPaddingBottom());
                mutated = true;
            }
            Object origTag = tv.getTag(TAG_KEY_ORIG_TEXT_SIZE);
            float origPx;
            if (origTag instanceof Float) {
                origPx = (Float) origTag;
            } else {
                origPx = tv.getTextSize();
                tv.setTag(TAG_KEY_ORIG_TEXT_SIZE, origPx);
            }
            int avail = width - tv.getCompoundPaddingLeft() - tv.getCompoundPaddingRight();
            boolean overflows = layout.getEllipsisCount(0) > 0
                    || (layout.getLineCount() > 0 && layout.getLineWidth(0) > avail)
                    || layout.getLineCount() > 2;
            if (!overflows) {
                return mutated;
            }
            if (tv.getMaxLines() < 2) {
                tv.setSingleLine(false);
                tv.setMaxLines(2);
                tv.setEllipsize(TextUtils.TruncateAt.END);
                mutated = true;
            }
            int targetGrav = Gravity.START | Gravity.CENTER_VERTICAL;
            if (tv.getGravity() != targetGrav) {
                tv.setGravity(targetGrav);
                mutated = true;
            }
            if (tv.getLayoutParams() instanceof android.widget.RelativeLayout.LayoutParams) {
                android.widget.RelativeLayout.LayoutParams rlp =
                        (android.widget.RelativeLayout.LayoutParams) tv.getLayoutParams();
                if (rlp.height != ViewGroup.LayoutParams.WRAP_CONTENT) {
                    rlp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    rlp.addRule(android.widget.RelativeLayout.CENTER_VERTICAL);
                    tv.setLayoutParams(rlp);
                    mutated = true;
                }
            }
            float targetPx = Math.max(origPx * 0.85f, Math.min(tv.getTextSize(), origPx * 0.88f));
            if (Math.abs(tv.getTextSize() - targetPx) >= 0.5f) {
                tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, targetPx);
                mutated = true;
            }
            return mutated;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static StaticLayout buildStaticLayout(TextView tv, TextPaint paint, String str, int availWidth) {
        return buildStaticLayout(tv, paint, str, availWidth, 0, null);
    }

    private static StaticLayout buildStaticLayout(
            TextView tv,
            TextPaint paint,
            String str,
            int availWidth,
            int maxLines,
            TextUtils.TruncateAt ellipsize) {
        float spacingMult = tv != null ? tv.getLineSpacingMultiplier() : 1.0f;
        float spacingAdd = tv != null ? tv.getLineSpacingExtra() : 0f;
        boolean includePad = tv == null || tv.getIncludeFontPadding();
        boolean useAnyRtl = TypographyNormalizer.containsRtlChars(str);
        return TypographyNormalizer.buildStaticLayout(
                str, paint, availWidth, Layout.Alignment.ALIGN_NORMAL,
                spacingMult, spacingAdd, includePad, maxLines, ellipsize, useAnyRtl);
    }

    private static int computeVisibleChildMask(ViewGroup vg) {
        if (vg == null) return 0;
        int count = Math.min(vg.getChildCount(), 30);
        int mask = count;
        for (int i = 0; i < count; i++) {
            View c = vg.getChildAt(i);
            if (c != null && c.getVisibility() == View.VISIBLE) {
                mask ^= (1 << (i + 1));
            }
        }
        return mask;
    }

    private static int computeRightSiblingOverlap(TextView tv, float density) {
        if (tv == null) return 0;
        ViewParent p = tv.getParent();
        if (!(p instanceof ViewGroup) || p instanceof LinearLayout) return 0;
        ViewGroup vg = (ViewGroup) p;
        int count = vg.getChildCount();
        if (count < 2 || tv.getWidth() <= 0 || tv.getHeight() <= 0) return 0;
        int tvLeft = tv.getLeft();
        int tvRight = tv.getRight();
        int tvTop = tv.getTop();
        int tvBottom = tv.getBottom();
        int minRightEdge = tvRight;
        boolean foundOverlap = false;
        for (int i = 0; i < count; i++) {
            View sib = vg.getChildAt(i);
            if (sib == null || sib == tv || sib.getVisibility() != View.VISIBLE) continue;
            int sw = sib.getWidth();
            int sh = sib.getHeight();
            if (sw <= 0 || sh <= 0) continue;
            if (sib instanceof android.widget.ImageView && sw <= Math.round(tv.getWidth() * 0.45f)) {
                boolean yOverlap = sib.getBottom() > tvTop + 4 && sib.getTop() < tvBottom - 4;
                boolean xOnRight = sib.getLeft() >= tvLeft + (tv.getWidth() / 3) && sib.getLeft() < tvRight;
                if (yOverlap && xOnRight && sib.getLeft() < minRightEdge) {
                    minRightEdge = sib.getLeft();
                    foundOverlap = true;
                }
            }
        }
        if (!foundOverlap) return 0;
        int neededPad = (tvRight - minRightEdge) + Math.round(4 * density);
        return (neededPad > 0 && neededPad < Math.round(tv.getWidth() * 0.50f)) ? neededPad : 0;
    }

    private static void logAuditOverflowOnce(TextView tv, String str, float measuredW, int availW, boolean hasEllipsis, int lines) {
        try {
            if (sLoggedAuditKeys.size() > 512) {
                sLoggedAuditKeys.clear();
            }
            String resName = "no_id";
            int id = tv.getId();
            if (id != View.NO_ID && id != 0) {
                try {
                    resName = tv.getResources().getResourceEntryName(id);
                } catch (Throwable ignored) {
                    resName = "0x" + Integer.toHexString(id);
                }
            }
            String shortStr = str.length() > 36 ? str.substring(0, 36) : str;
            String key = resName + "|" + shortStr + "|" + availW + "|" + hasEllipsis;
            if (sLoggedAuditKeys.add(key)) {
                Log.w(AUDIT_TAG, "OVERFLOW id=" + resName
                        + " text=\"" + shortStr + "\""
                        + " measuredPx=" + Math.round(measuredW)
                        + " availPx=" + availW
                        + " ellipsis=" + hasEllipsis
                        + " lines=" + lines);
            }
        } catch (Throwable ignored) {}
    }

    private static void captureOrigTextViewState(TextView tv) {
        if (tv == null) return;
        TypographyNormalizer.captureBaseline(tv);
        if (tv.getTag(TAG_KEY_ORIG_TEXT_SIZE) == null) {
            tv.setTag(TAG_KEY_ORIG_TEXT_SIZE, Float.valueOf(tv.getTextSize()));
        }
        if (tv.getTag(TAG_KEY_ORIG_SCALE_X) == null) {
            tv.setTag(TAG_KEY_ORIG_SCALE_X, Float.valueOf(tv.getTextScaleX()));
        }
        if (tv.getTag(TAG_KEY_ORIG_PAD_LEFT) == null) {
            tv.setTag(TAG_KEY_ORIG_PAD_LEFT, Integer.valueOf(tv.getPaddingLeft()));
        }
        if (tv.getTag(TAG_KEY_ORIG_PAD_RIGHT) == null) {
            tv.setTag(TAG_KEY_ORIG_PAD_RIGHT, Integer.valueOf(tv.getPaddingRight()));
        }
        if (tv.getTag(TAG_KEY_ORIG_MAX_LINES) == null) {
            tv.setTag(TAG_KEY_ORIG_MAX_LINES, Integer.valueOf(tv.getMaxLines()));
        }
        if (tv.getTag(TAG_KEY_ORIG_SINGLE_LINE) == null) {
            tv.setTag(TAG_KEY_ORIG_SINGLE_LINE, Boolean.valueOf(isSingleLineView(tv, tv.getMaxLines())));
        }
        if (tv.getTag(TAG_KEY_ORIG_ELLIPSIZE) == null) {
            TextUtils.TruncateAt curEl = tv.getEllipsize();
            tv.setTag(TAG_KEY_ORIG_ELLIPSIZE, curEl != null ? curEl : SENTINEL_ELLIPSIZE_NONE);
        }
        if (tv.getTag(TAG_KEY_ORIG_MAX_WIDTH) == null) {
            tv.setTag(TAG_KEY_ORIG_MAX_WIDTH, Integer.valueOf(tv.getMaxWidth()));
        }
    }

    private static boolean restoreOrigTextViewState(TextView tv) {
        if (tv == null) return false;
        boolean mutated = TypographyNormalizer.restoreBaseline(tv);
        Object origSize = tv.getTag(TAG_KEY_ORIG_TEXT_SIZE);
        if (origSize instanceof Float && (Float) origSize > 0f) {
            float target = (Float) origSize;
            if (Math.abs(tv.getTextSize() - target) >= 0.5f) {
                tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, target);
                mutated = true;
            }
        }
        Object origScale = tv.getTag(TAG_KEY_ORIG_SCALE_X);
        if (origScale instanceof Float && Math.abs(tv.getTextScaleX() - (Float) origScale) > 0.005f) {
            tv.setTextScaleX((Float) origScale);
            mutated = true;
        }
        Object origPadL = tv.getTag(TAG_KEY_ORIG_PAD_LEFT);
        Object origPadR = tv.getTag(TAG_KEY_ORIG_PAD_RIGHT);
        int targetL = (origPadL instanceof Integer) ? (Integer) origPadL : tv.getPaddingLeft();
        int targetR = (origPadR instanceof Integer) ? (Integer) origPadR : tv.getPaddingRight();
        if (tv.getPaddingLeft() != targetL || tv.getPaddingRight() != targetR) {
            tv.setPadding(targetL, tv.getPaddingTop(), targetR, tv.getPaddingBottom());
            mutated = true;
        }
        Object origSingle = tv.getTag(TAG_KEY_ORIG_SINGLE_LINE);
        Object origLines = tv.getTag(TAG_KEY_ORIG_MAX_LINES);
        if (origSingle instanceof Boolean && isSingleLineView(tv, tv.getMaxLines()) != (Boolean) origSingle) {
            tv.setSingleLine((Boolean) origSingle);
            mutated = true;
        }
        if (origLines instanceof Integer && (Integer) origLines > 0 && tv.getMaxLines() != (Integer) origLines) {
            tv.setMaxLines((Integer) origLines);
            mutated = true;
        }
        Object origEl = tv.getTag(TAG_KEY_ORIG_ELLIPSIZE);
        if (origEl != null) {
            TextUtils.TruncateAt wantEl = (origEl == SENTINEL_ELLIPSIZE_NONE) ? null : (TextUtils.TruncateAt) origEl;
            if (tv.getEllipsize() != wantEl) {
                tv.setEllipsize(wantEl);
                mutated = true;
            }
        }
        Object origMaxW = tv.getTag(TAG_KEY_ORIG_MAX_WIDTH);
        if (origMaxW instanceof Integer && tv.getMaxWidth() != (Integer) origMaxW) {
            tv.setMaxWidth((Integer) origMaxW);
            mutated = true;
        }
        tv.setTag(TAG_KEY_LAST_FIT_QUICK, null);
        tv.setTag(TAG_KEY_LAST_FIT_SIG, null);
        return mutated;
    }

    private static boolean isInsideRecycledList(View v) {
        ViewParent p = v != null ? v.getParent() : null;
        int depth = 0;
        while (p != null && depth < 6) {
            String name = p.getClass().getName();
            if (p instanceof android.widget.AdapterView
                    || name.endsWith("RecyclerView")
                    || name.endsWith("ListView")
                    || name.endsWith("GridView")
                    || name.endsWith("Spinner")) {
                return true;
            }
            p = p.getParent();
            depth++;
        }
        return false;
    }

    private static int computeWrapChildAvailWidthInParent(ViewGroup parent, TextView tv, int rightObstaclePad, float density) {
        if (parent == null || parent.getWidth() <= 0) return tv.getWidth();
        int parentRight = parent.getWidth() - parent.getPaddingRight();
        int tvLeft = Math.max(parent.getPaddingLeft(), tv.getLeft());
        int minGap = Math.round(4f * density);
        if (parent instanceof android.widget.RelativeLayout || parent instanceof android.widget.FrameLayout) {
            int limitRight = parentRight;
            int tvTop = tv.getTop();
            int tvBot = tv.getBottom();
            for (int i = 0; i < parent.getChildCount(); i++) {
                View sib = parent.getChildAt(i);
                if (sib == null || sib == tv || sib.getVisibility() == View.GONE) continue;
                if (sib.getWidth() <= 0) continue;
                boolean yOverlaps = (tvBot <= tvTop) || (sib.getBottom() > tvTop + 2 && sib.getTop() < tvBot - 2);
                if (yOverlaps && sib.getLeft() >= tvLeft + Math.round(12f * density)) {
                    int sibEdge = sib.getLeft() - minGap;
                    if (sib.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                        sibEdge -= ((ViewGroup.MarginLayoutParams) sib.getLayoutParams()).leftMargin;
                    }
                    if (sibEdge < limitRight) {
                        limitRight = sibEdge;
                    }
                }
            }
            int avail = limitRight - tvLeft - rightObstaclePad;
            if (avail > 0) return avail;
        }
        if (parent instanceof LinearLayout && ((LinearLayout) parent).getOrientation() == LinearLayout.HORIZONTAL) {
            int avail = parent.getWidth() - parent.getPaddingLeft() - parent.getPaddingRight() - rightObstaclePad;
            if (tv.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams tmlp = (ViewGroup.MarginLayoutParams) tv.getLayoutParams();
                avail -= tmlp.leftMargin + tmlp.rightMargin;
            }
            for (int i = 0; i < parent.getChildCount(); i++) {
                View sib = parent.getChildAt(i);
                if (sib == null || sib == tv || sib.getVisibility() == View.GONE) continue;
                avail -= sib.getWidth();
                if (sib.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams smlp = (ViewGroup.MarginLayoutParams) sib.getLayoutParams();
                    avail -= smlp.leftMargin + smlp.rightMargin;
                }
            }
            if (avail > 0) return avail;
        }
        return Math.max(tv.getWidth(), parent.getWidth() - parent.getPaddingLeft() - parent.getPaddingRight() - rightObstaclePad);
    }

    private static View resolveOwningSlotView(TextView tv, float density) {
        if (tv == null) return null;
        int maxPillW = Math.round(220f * density);
        ViewParent p = tv.getParent();
        View fallbackCandidate = null;
        for (int depth = 0; depth < 2 && p instanceof ViewGroup; depth++) {
            ViewGroup vg = (ViewGroup) p;
            String pName = vg.getClass().getName();
            if (pName.endsWith("ScrollView")
                    || pName.endsWith("HorizontalScrollView")
                    || pName.endsWith("RecyclerView")
                    || pName.endsWith("ListView")
                    || pName.endsWith("GridView")
                    || vg.getId() == android.R.id.content) {
                break;
            }
            int vgW = vg.getWidth();
            if (vgW <= 0) {
                p = vg.getParent();
                continue;
            }
            int textChildren = 0;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View ch = vg.getChildAt(i);
                if (ch instanceof TextView && ch.getVisibility() != View.GONE) {
                    textChildren++;
                }
            }
            ViewGroup.LayoutParams vgLp = vg.getLayoutParams();
            boolean boundedOrStyled = TypographyNormalizer.hasVisibleBackground(vg)
                    || (vgLp != null && (vgLp.width > 0
                            || vgLp.width == ViewGroup.LayoutParams.MATCH_PARENT
                            || (vgLp instanceof LinearLayout.LayoutParams
                                    && ((LinearLayout.LayoutParams) vgLp).weight > 0f)));
            boolean isCompactSlot = vg.isClickable()
                    || boundedOrStyled
                    || (vg instanceof LinearLayout && textChildren <= 1 && vgW <= maxPillW);
            if (isCompactSlot && vgW <= maxPillW) {
                if (boundedOrStyled) {
                    return vg;
                }
                if (fallbackCandidate == null) {
                    fallbackCandidate = vg;
                }
            }
            p = vg.getParent();
        }
        return fallbackCandidate;
    }

    private static int computeSlotInnerWidthForChild(View owningSlot, TextView tv) {
        if (owningSlot == null) return tv.getWidth();
        int slotInner = owningSlot.getWidth() - owningSlot.getPaddingLeft() - owningSlot.getPaddingRight();
        if (slotInner <= 0) return tv.getWidth();
        // Walk from tv up to owningSlot subtracting horizontal siblings and child wrapper margins/paddings.
        View cur = tv;
        while (cur != null && cur != owningSlot && cur.getParent() instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) cur.getParent();
            slotInner -= parent.getPaddingLeft() + parent.getPaddingRight();
            ViewGroup.LayoutParams clp = cur.getLayoutParams();
            if (clp instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) clp;
                slotInner -= mlp.leftMargin + mlp.rightMargin;
            }
            if (parent instanceof LinearLayout && ((LinearLayout) parent).getOrientation() == LinearLayout.HORIZONTAL) {
                for (int i = 0; i < parent.getChildCount(); i++) {
                    View sib = parent.getChildAt(i);
                    if (sib == null || sib == cur || sib.getVisibility() == View.GONE) continue;
                    slotInner -= sib.getWidth();
                    ViewGroup.LayoutParams slp = sib.getLayoutParams();
                    if (slp instanceof ViewGroup.MarginLayoutParams) {
                        ViewGroup.MarginLayoutParams smlp = (ViewGroup.MarginLayoutParams) slp;
                        slotInner -= smlp.leftMargin + smlp.rightMargin;
                    }
                }
            }
            cur = parent;
        }
        return Math.max(1, slotInner);
    }

    private static int computeSlotAvailableHeightForChild(View owningSlot, TextView tv) {
        ViewGroup.LayoutParams tvLp = tv.getLayoutParams();
        if (tvLp != null && tvLp.height > 0 && tv.getHeight() > 0) {
            return Math.max(1, tv.getHeight() - tv.getCompoundPaddingTop() - tv.getCompoundPaddingBottom());
        }
        if (owningSlot == null || owningSlot.getHeight() <= 0) {
            return 0;
        }
        ViewGroup.LayoutParams slotLp = owningSlot.getLayoutParams();
        boolean fixedSelfH = slotLp != null && slotLp.height > 0;
        boolean fixedParentH = false;
        if (!fixedSelfH && owningSlot.getParent() instanceof View) {
            View p = (View) owningSlot.getParent();
            ViewGroup.LayoutParams pLp = p.getLayoutParams();
            if (pLp != null && pLp.height > 0 && (slotLp == null || slotLp.height == ViewGroup.LayoutParams.MATCH_PARENT
                    || (slotLp instanceof LinearLayout.LayoutParams && ((LinearLayout.LayoutParams) slotLp).weight > 0f))) {
                fixedParentH = true;
            }
        }
        if (!fixedSelfH && !fixedParentH) return 0;
        int availH = owningSlot.getHeight() - owningSlot.getPaddingTop() - owningSlot.getPaddingBottom()
                - tv.getCompoundPaddingTop() - tv.getCompoundPaddingBottom();
        View cur = tv;
        while (cur != null && cur != owningSlot && cur.getParent() instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) cur.getParent();
            if (parent != owningSlot) {
                availH -= parent.getPaddingTop() + parent.getPaddingBottom();
            }
            ViewGroup.LayoutParams clp = cur.getLayoutParams();
            if (clp instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) clp;
                availH -= mlp.topMargin + mlp.bottomMargin;
            }
            if (parent instanceof LinearLayout && ((LinearLayout) parent).getOrientation() == LinearLayout.VERTICAL) {
                for (int i = 0; i < parent.getChildCount(); i++) {
                    View sib = parent.getChildAt(i);
                    if (sib == null || sib == cur || sib.getVisibility() == View.GONE) continue;
                    availH -= sib.getHeight();
                    ViewGroup.LayoutParams slp = sib.getLayoutParams();
                    if (slp instanceof ViewGroup.MarginLayoutParams) {
                        ViewGroup.MarginLayoutParams smlp = (ViewGroup.MarginLayoutParams) slp;
                        availH -= smlp.topMargin + smlp.bottomMargin;
                    }
                }
            }
            cur = parent;
        }
        return Math.max(0, availH);
    }

    private static boolean isInsideFreeHorizontalScroller(View v) {
        ViewParent p = v != null ? v.getParent() : null;
        int depth = 0;
        View prev = v;
        while (p != null && depth < 8) {
            if (p instanceof android.widget.HorizontalScrollView) {
                android.widget.HorizontalScrollView hsv = (android.widget.HorizontalScrollView) p;
                if (hsv.getChildCount() > 0 && hsv.getWidth() > 0) {
                    View strip = hsv.getChildAt(0);
                    // In TabLayout fixed mode or fitted strip, children cannot scroll freely past viewport.
                    if (strip.getWidth() <= hsv.getWidth() + 2 && prev != hsv) {
                        return false;
                    }
                }
                return true;
            }
            if (p instanceof View) prev = (View) p;
            p = p.getParent();
            depth++;
        }
        return false;
    }

    private static int computeHorizontalRowSlack(View pillTarget) {
        ViewParent p = pillTarget != null ? pillTarget.getParent() : null;
        if (!(p instanceof ViewGroup)) return 0;
        ViewGroup parent = (ViewGroup) p;
        if (parent instanceof android.widget.HorizontalScrollView) return Integer.MAX_VALUE / 4;
        if (parent.getParent() instanceof android.widget.HorizontalScrollView) return Integer.MAX_VALUE / 4;
        int pw = parent.getWidth() - parent.getPaddingLeft() - parent.getPaddingRight();
        if (pw <= 0) return 0;
        if (parent instanceof LinearLayout && ((LinearLayout) parent).getOrientation() == LinearLayout.HORIZONTAL) {
            int used = 0;
            for (int i = 0; i < parent.getChildCount(); i++) {
                View ch = parent.getChildAt(i);
                if (ch == null || ch.getVisibility() == View.GONE) continue;
                used += ch.getWidth();
                if (ch.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) ch.getLayoutParams();
                    used += mlp.leftMargin + mlp.rightMargin;
                }
            }
            return Math.max(0, pw - used);
        }
        return Math.max(0, pw - pillTarget.getWidth());
    }

    private static boolean isButtonRowOrTabChild(TextView tv) {
        if (tv == null) return false;
        ViewParent p = tv.getParent();
        if (!(p instanceof ViewGroup)) return false;
        ViewGroup vg = (ViewGroup) p;
        String pName = vg.getClass().getName();
        return pName.contains("TabView")
                || pName.contains("SlidingTabStrip")
                || (tv.getLayoutParams() instanceof LinearLayout.LayoutParams
                        && ((LinearLayout.LayoutParams) tv.getLayoutParams()).weight > 0f
                        && vg.getChildCount() >= 2);
    }

    private static boolean isSingleLineToolbarForced(TextView tv) {
        return tv != null && tv.getTag(TAG_KEY_HOST_ACTIVITY_CLASS) == null
                && (tv.getParent() instanceof View && ((View) tv.getParent()).getId() != View.NO_ID)
                && WorkspaceUiController.isBlendOrPaperLabel(tv);
    }

    private static boolean isRigidHeaderWithRightIcons(TextView tv, float density) {
        if (tv == null || !(tv.getParent() instanceof ViewGroup)) return false;
        ViewGroup parent = (ViewGroup) tv.getParent();
        int tvLeft = tv.getLeft();
        int tvTop = tv.getTop();
        int tvBot = tv.getBottom();
        int rightIcons = 0;
        for (int i = 0; i < parent.getChildCount(); i++) {
            View sib = parent.getChildAt(i);
            if (sib == null || sib == tv || sib.getVisibility() != View.VISIBLE || sib.getWidth() <= 0) continue;
            boolean yOverlap = (tvBot <= tvTop) || (sib.getBottom() > tvTop + 2 && sib.getTop() < tvBot - 2);
            if (yOverlap && sib.getLeft() >= tvLeft + Math.round(12f * density)
                    && (sib instanceof android.widget.ImageView || sib.isClickable())) {
                rightIcons++;
            }
        }
        return rightIcons >= 2;
    }

    public static boolean fitNarrowTextView(TextView tv) {
        if (tv == null || tv instanceof android.widget.EditText) return false;
        try {
            CharSequence cs = tv.getText();
            if (cs == null || cs.length() == 0) return false;
            String str = cs.toString();

            if (WorkspaceUiController.isPaperNameView(tv)
                    || WorkspaceUiController.isCanvasPolishToolGridLabel(tv)) {
                return false;
            }

            // Layer C: Capture original typography baselines once before any mutation.
            captureOrigTextViewState(tv);
            if (!isTextAutoFitActive()) {
                return restoreOrigTextViewState(tv);
            }

            Context ctx = tv.getContext();
            Locale activeLocale = getActiveLocale();
            Typeface activeTf = ctx != null ? UIFontManager.getActiveTypeface(ctx) : null;
            boolean dynamicI18n = isDynamicI18nEnabled();
            boolean mutated = TypographyNormalizer.prepareTextViewForPipeline(
                    tv, str, activeLocale, activeTf, dynamicI18n);

            if (WorkspaceUiController.isPhase2ManagedStripView(tv)) {
                return mutated;
            }

            if (str.length() > 64 || str.contains("\n\n") || str.contains(". ") || isDynamicNumericReadoutText(str)) {
                return mutated;
            }
            if (tv.isLayoutRequested()) {
                return mutated;
            }
            int width = tv.getWidth();
            android.text.Layout layout = tv.getLayout();
            if (width <= 0 || layout == null) return mutated;

            android.util.DisplayMetrics dm = tv.getResources().getDisplayMetrics();
            float density = dm.density;
            if (density <= 0f) return mutated;

            View owningSlot = resolveOwningSlotView(tv, density);
            int slotOuterW = (owningSlot != null && owningSlot.getWidth() > 0) ? owningSlot.getWidth() : width;
            if (slotOuterW < Math.round(32f * density) || TypographyNormalizer.isShortAsciiOrDigitBadge(str)) {
                mutated |= restoreOrigTextViewState(tv);
                return mutated;
            }

            ViewParent vp = tv.getParent();
            int parentW = (vp instanceof View) ? ((View) vp).getWidth() : 0;
            int sibMask = (vp instanceof LinearLayout || !(vp instanceof ViewGroup))
                    ? 0
                    : computeVisibleChildMask((ViewGroup) vp);
            int slotW = owningSlot != null ? owningSlot.getWidth() : 0;
            int slotH = owningSlot != null ? owningSlot.getHeight() : 0;
            int tfId = UIFontManager.getActiveTypefaceIdentity(ctx);
            String locTag = getActiveLocaleTag();

            int quickHash = sPackGeneration;
            quickHash = quickHash * 31 + (locTag != null ? locTag.hashCode() : 0);
            quickHash = quickHash * 31 + tfId;
            quickHash = quickHash * 31 + str.hashCode();
            quickHash = quickHash * 31 + width;
            quickHash = quickHash * 31 + tv.getHeight();
            quickHash = quickHash * 31 + slotW;
            quickHash = quickHash * 31 + slotH;
            quickHash = quickHash * 31 + parentW;
            quickHash = quickHash * 31 + sibMask;
            quickHash = quickHash * 31 + Float.floatToRawIntBits(tv.getTextSize());
            quickHash = quickHash * 31 + Float.floatToRawIntBits(tv.getTextScaleX());
            quickHash = quickHash * 31 + tv.getMaxLines();
            quickHash = quickHash * 31 + tv.getPaddingLeft();
            quickHash = quickHash * 31 + tv.getPaddingRight();
            quickHash = quickHash * 31 + (Build.VERSION.SDK_INT >= 17 ? tv.getTextDirection() : 0);
            Object prevQuick = tv.getTag(TAG_KEY_LAST_FIT_QUICK);
            if (prevQuick instanceof Integer && (Integer) prevQuick == quickHash) {
                return mutated;
            }

            boolean inMainActivity = isInsideMainActivityWindow(tv);
            if (!inMainActivity && fitConstrainedShapeButton(tv)) {
                return true;
            }

            int origPadL = (Integer) tv.getTag(TAG_KEY_ORIG_PAD_LEFT);
            int origPadR = (Integer) tv.getTag(TAG_KEY_ORIG_PAD_RIGHT);
            float origPx = (Float) tv.getTag(TAG_KEY_ORIG_TEXT_SIZE);
            float origScaleX = (Float) tv.getTag(TAG_KEY_ORIG_SCALE_X);
            int origMaxLines = (Integer) tv.getTag(TAG_KEY_ORIG_MAX_LINES);
            boolean origSingleLine = (Boolean) tv.getTag(TAG_KEY_ORIG_SINGLE_LINE);
            Object origEllipsizeObj = tv.getTag(TAG_KEY_ORIG_ELLIPSIZE);
            TextUtils.TruncateAt origEllipsize =
                    (origEllipsizeObj == SENTINEL_ELLIPSIZE_NONE) ? null : (TextUtils.TruncateAt) origEllipsizeObj;
            int origMaxWidth = (Integer) tv.getTag(TAG_KEY_ORIG_MAX_WIDTH);
            if (origPx <= 0f) return mutated;

            int scriptGroup = TypographyNormalizer.classifyScript(str);
            float baseScaleX = (scriptGroup == TypographyNormalizer.SCRIPT_LATIN_CYRILLIC_GREEK) ? origScaleX : 1.0f;

            // Layer B: WRAP_CONTENT chips/pills/tabs with visible background inside HorizontalScrollView
            // or unconstrained rows get minimum 6dp horizontal breathing room rather than useless downscaling.
            ViewGroup.LayoutParams tvLp = tv.getLayoutParams();
            boolean tvIsWrap = tvLp == null || tvLp.width == ViewGroup.LayoutParams.WRAP_CONTENT;
            boolean insideFreeHScroll = isInsideFreeHorizontalScroller(tv);
            boolean isFreePill = insideFreeHScroll
                    || (tvIsWrap && owningSlot == null
                            && TypographyNormalizer.hasVisibleBackground(tv)
                            && !isInsideListOrSettingsRow(tv)
                            && computeRightSiblingOverlap(tv, density) == 0);
            if (isFreePill) {
                View pillBox = TypographyNormalizer.hasVisibleBackground(tv)
                        ? tv
                        : (owningSlot != null && TypographyNormalizer.hasVisibleBackground(owningSlot) ? owningSlot : null);
                if (pillBox == tv && !isCompoundSwitchOrCheck(tv)) {
                    int minPad5 = Math.round(5f * density);
                    int targetMin6 = Math.round(6f * density);
                    int curL = origPadL;
                    int curR = origPadR;
                    int wantL = curL < minPad5 ? targetMin6 : curL;
                    int wantR = curR < minPad5 ? targetMin6 : curR;
                    int neededExtra = Math.max(0, wantL - curL) + Math.max(0, wantR - curR);
                    if (neededExtra > 0) {
                        int slack = computeHorizontalRowSlack(tv);
                        if (slack < neededExtra) {
                            int half = slack / 2;
                            wantL = Math.max(curL, Math.min(wantL, curL + half));
                            wantR = Math.max(curR, Math.min(wantR, curR + (slack - half)));
                        }
                        if (tv.getPaddingLeft() != wantL || tv.getPaddingRight() != wantR) {
                            tv.setPadding(wantL, tv.getPaddingTop(), wantR, tv.getPaddingBottom());
                            mutated = true;
                        }
                    }
                } else if (pillBox != null && pillBox != tv && origMaxLines == 1 && insideFreeHScroll) {
                    int minPad5 = Math.round(5f * density);
                    int targetMin6 = Math.round(6f * density);
                    if (pillBox.getPaddingLeft() < minPad5 || pillBox.getPaddingRight() < minPad5) {
                        int nL = Math.max(pillBox.getPaddingLeft(), pillBox.getPaddingLeft() < minPad5 ? targetMin6 : 0);
                        int nR = Math.max(pillBox.getPaddingRight(), pillBox.getPaddingRight() < minPad5 ? targetMin6 : 0);
                        pillBox.setPadding(nL, pillBox.getPaddingTop(), nR, pillBox.getPaddingBottom());
                        mutated = true;
                    }
                }
                if (Math.abs(tv.getTextSize() - origPx) >= 0.5f) {
                    tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, origPx);
                    mutated = true;
                }
                if (Math.abs(tv.getTextScaleX() - baseScaleX) > 0.005f) {
                    tv.setTextScaleX(baseScaleX);
                    mutated = true;
                }
                if (!mutated) {
                    tv.setTag(TAG_KEY_LAST_FIT_QUICK, Integer.valueOf(quickHash));
                }
                return mutated;
            }
            if (isInsideHorizontalScrollView(tv) && insideFreeHScroll) {
                if (!mutated) {
                    tv.setTag(TAG_KEY_LAST_FIT_QUICK, Integer.valueOf(quickHash));
                }
                return mutated;
            }

            // Determine tighter slot inner width and optical safe zone.
            int compoundL = tv.getCompoundPaddingLeft() - tv.getPaddingLeft();
            int compoundR = tv.getCompoundPaddingRight() - tv.getPaddingRight();
            int rightObstaclePad = !inMainActivity ? computeRightSiblingOverlap(tv, density) : 0;

            boolean inRecycledList = isInsideRecycledList(tv);
            boolean tvFillsContainer = (tvLp != null
                    && (tvLp.width > 0 || tvLp.width == ViewGroup.LayoutParams.MATCH_PARENT
                            || (tvLp instanceof LinearLayout.LayoutParams && ((LinearLayout.LayoutParams) tvLp).weight > 0f)));
            int directParentInnerW = (vp instanceof ViewGroup)
                    ? ((ViewGroup) vp).getWidth() - ((ViewGroup) vp).getPaddingLeft() - ((ViewGroup) vp).getPaddingRight()
                    : width;
            int slotInnerW;
            if (owningSlot != null && owningSlot != tv) {
                int ownerAvail = computeSlotInnerWidthForChild(owningSlot, tv) - rightObstaclePad;
                slotInnerW = tvFillsContainer ? Math.min(width, ownerAvail) : ownerAvail;
            } else if (tvIsWrap && vp instanceof ViewGroup && ((ViewGroup) vp).getWidth() > 0) {
                slotInnerW = computeWrapChildAvailWidthInParent((ViewGroup) vp, tv, rightObstaclePad, density);
            } else {
                slotInnerW = width - rightObstaclePad;
            }
            slotInnerW = Math.max(1, slotInnerW);

            boolean hasRightIconsHeader = !inMainActivity && (origSingleLine || origMaxLines == 1)
                    && isRigidHeaderWithRightIcons(tv, density);
            boolean isRigid1DStrip = hasRightIconsHeader
                    || (WorkspaceUiController.isBlendOrPaperLabel(tv) && !WorkspaceUiController.isPaperNameView(tv))
                    || (isButtonRowOrTabChild(tv) && origMaxLines == 1);

            boolean isButtonLike = tv instanceof android.widget.Button
                    || tv.isClickable()
                    || (vp instanceof View && ((View) vp).isClickable());
            boolean hasVertIcon = isVerticalIconWithText(tv);
            boolean isPillSlotOrTab = (owningSlot != null)
                    || isButtonLike
                    || hasVertIcon
                    || TypographyNormalizer.hasVisibleBackground(tv)
                    || isButtonRowOrTabChild(tv);
            boolean useSafeZone = !inMainActivity && isPillSlotOrTab && slotInnerW <= Math.round(240f * density);
            int safeInset = useSafeZone
                    ? TypographyNormalizer.clamp(Math.round(slotInnerW * 0.08f), Math.round(4f * density), Math.round(10f * density))
                    : (hasRightIconsHeader ? Math.round(4f * density) : 0);

            int desiredPadL = origPadL;
            int desiredPadR = Math.max(origPadR, rightObstaclePad);
            if (hasRightIconsHeader) {
                desiredPadR = Math.max(desiredPadR, Math.round(4f * density));
            }
            if (useSafeZone && tvFillsContainer && (origSingleLine || origMaxLines <= 2)) {
                desiredPadL = Math.max(desiredPadL, safeInset);
                desiredPadR = Math.max(desiredPadR, safeInset);
            }

            int safeTextAvailW;
            if (useSafeZone) {
                safeTextAvailW = Math.max(1, slotInnerW - 2 * safeInset - compoundL - compoundR);
            } else {
                safeTextAvailW = Math.max(1, slotInnerW - desiredPadL - desiredPadR - compoundL - compoundR);
            }

            TextPaint measurePaint = TypographyNormalizer.getScratchTextPaint(tv.getPaint());
            TypographyNormalizer.preparePaintForScript(measurePaint, str, activeLocale, activeTf);
            measurePaint.setTextSize(origPx);
            measurePaint.setTextScaleX(baseScaleX);
            float origMeasuredW = TypographyNormalizer.measureHarfBuzzWidth(str, measurePaint);

            float maxRenderedW = 0f;
            boolean hasEllipsis = false;
            int lc = layout.getLineCount();
            for (int li = 0; li < lc; li++) {
                float lw = layout.getLineWidth(li);
                if (lw > maxRenderedW) maxRenderedW = lw;
                if (layout.getEllipsisCount(li) > 0) hasEllipsis = true;
            }

            if (inMainActivity) {
                boolean overflowsNow = hasEllipsis
                        || maxRenderedW > safeTextAvailW + 1.0f
                        || (origSingleLine && origMeasuredW > safeTextAvailW + 1.0f);
                if (!origSingleLine || !overflowsNow) {
                    if (hasEllipsis) {
                        logAuditOverflowOnce(tv, str, maxRenderedW, safeTextAvailW, true, lc);
                    }
                    if (!mutated) {
                        tv.setTag(TAG_KEY_LAST_FIT_QUICK, Integer.valueOf(quickHash));
                    }
                    return mutated;
                }
                if (tv.getEllipsize() != TextUtils.TruncateAt.END) {
                    tv.setEllipsize(TextUtils.TruncateAt.END);
                    mutated = true;
                }
                int maxTrim = Math.round(4f * density);
                if (maxTrim > 0 && (origPadL > 0 || origPadR > 0)) {
                    int nL = Math.max(0, origPadL - Math.min(origPadL, maxTrim));
                    int nR = Math.max(0, origPadR - Math.min(origPadR, maxTrim));
                    if (tv.getPaddingLeft() != nL || tv.getPaddingRight() != nR) {
                        tv.setPadding(nL, tv.getPaddingTop(), nR, tv.getPaddingBottom());
                        mutated = true;
                    }
                }
            }

            int availHeight = computeSlotAvailableHeightForChild(owningSlot, tv);
            float origFmHeight = TypographyNormalizer.measureFontMetricsLineHeight(measurePaint);
            boolean vertOverflowOrig = availHeight > 0 && origSingleLine && origFmHeight > availHeight + 2f;
            float strictSingleLimit = Math.max(1f, safeTextAvailW - 0.5f);
            boolean origFitsSingle = !hasEllipsis
                    && origMeasuredW <= strictSingleLimit
                    && !vertOverflowOrig
                    && lc <= 1;

            boolean narrowEligible = hasVertIcon || rightObstaclePad > 0 || hasRightIconsHeader
                    || (isPillSlotOrTab && !isInsideListOrSettingsRow(tv));
            if (origFitsSingle && !(useSafeZone && (desiredPadL != tv.getPaddingLeft() || desiredPadR != tv.getPaddingRight()))) {
                if (Math.abs(tv.getTextSize() - origPx) >= 0.5f) {
                    tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, origPx);
                    mutated = true;
                }
                if (Math.abs(tv.getTextScaleX() - baseScaleX) > 0.005f) {
                    tv.setTextScaleX(baseScaleX);
                    mutated = true;
                }
                if (tv.getPaddingLeft() != desiredPadL || tv.getPaddingRight() != desiredPadR) {
                    tv.setPadding(desiredPadL, tv.getPaddingTop(), desiredPadR, tv.getPaddingBottom());
                    mutated = true;
                }
                if (isSingleLineView(tv, tv.getMaxLines()) != origSingleLine) {
                    tv.setSingleLine(origSingleLine);
                    mutated = true;
                }
                if (origMaxLines > 0 && tv.getMaxLines() != origMaxLines) {
                    tv.setMaxLines(origMaxLines);
                    mutated = true;
                }
                if (tv.getEllipsize() != origEllipsize) {
                    tv.setEllipsize(origEllipsize);
                    mutated = true;
                }
                if (tv.getMaxWidth() != origMaxWidth) {
                    tv.setMaxWidth(origMaxWidth);
                    mutated = true;
                }
                if (!mutated) {
                    tv.setTag(TAG_KEY_LAST_FIT_QUICK, Integer.valueOf(quickHash));
                }
                return mutated;
            }

            boolean overflowsOrig = origMeasuredW > strictSingleLimit
                    || hasEllipsis
                    || vertOverflowOrig
                    || (!origSingleLine && lc > Math.max(1, origMaxLines));
            if (WorkspaceUiController.isBlendOrPaperLabel(tv) && !overflowsOrig) {
                if (!mutated) {
                    tv.setTag(TAG_KEY_LAST_FIT_QUICK, Integer.valueOf(quickHash));
                }
                return mutated;
            }
            if (!narrowEligible && !overflowsOrig) {
                if (!mutated) {
                    tv.setTag(TAG_KEY_LAST_FIT_QUICK, Integer.valueOf(quickHash));
                }
                return mutated;
            }
            if (safeTextAvailW > Math.round(160f * density) && !overflowsOrig && rightObstaclePad == 0 && !hasRightIconsHeader) {
                if (!mutated) {
                    tv.setTag(TAG_KEY_LAST_FIT_QUICK, Integer.valueOf(quickHash));
                }
                return mutated;
            }

            int archetype = isRigid1DStrip
                    ? TypographyNormalizer.ARCHETYPE_RIGID_1D_STRIP
                    : (hasVertIcon
                            ? TypographyNormalizer.ARCHETYPE_VERTICAL_ELASTIC_STRIP
                            : TypographyNormalizer.ARCHETYPE_GENERIC_BOUNDED);
            boolean allowMultiLine = !isRigid1DStrip
                    && !inMainActivity
                    && !isSingleLineToolbarForced(tv)
                    && !isInsideListOrSettingsRow(tv)
                    && (hasVertIcon || !origSingleLine || origMaxLines > 1);
            int maxGrowthPx = hasVertIcon ? Math.round(18f * density) : 0;

            TypographyNormalizer.CandidatePlan plan = TypographyNormalizer.evaluateCandidates(
                    str,
                    origPx,
                    baseScaleX,
                    scriptGroup,
                    archetype,
                    allowMultiLine,
                    safeTextAvailW,
                    availHeight,
                    maxGrowthPx,
                    tv.getLineSpacingMultiplier(),
                    tv.getLineSpacingExtra(),
                    true,
                    tv.getPaint(),
                    activeLocale,
                    activeTf,
                    dm,
                    useSafeZone ? safeInset : 0,
                    useSafeZone ? safeInset : 0);

            float targetPx = plan.targetPx;
            float targetScaleX = plan.targetScaleX;
            int targetLines = plan.targetLines;
            boolean targetSingleLine = targetLines <= 1;
            TextUtils.TruncateAt targetEllipsize = plan.useEllipsis ? TextUtils.TruncateAt.END : origEllipsize;
            int targetMaxWidth = origMaxWidth;

            if (plan.resolvedSafeInsetPx > 0 && useSafeZone) {
                desiredPadL = Math.max(origPadL, plan.resolvedSafeInsetPx);
                desiredPadR = Math.max(Math.max(origPadR, rightObstaclePad), plan.resolvedSafeInsetPx);
                if (!tvFillsContainer && owningSlot != null && !inRecycledList) {
                    targetMaxWidth = safeTextAvailW + 2 * safeInset + compoundL + compoundR;
                }
            } else if (plan.state == 5) {
                if (useSafeZone) {
                    if (tvFillsContainer) {
                        desiredPadL = Math.max(desiredPadL, safeInset);
                        desiredPadR = Math.max(desiredPadR, safeInset);
                    } else if (owningSlot != null && !inRecycledList) {
                        targetMaxWidth = safeTextAvailW + desiredPadL + desiredPadR + compoundL + compoundR;
                    }
                }
            }

            if (tv.getPaddingLeft() != desiredPadL || tv.getPaddingRight() != desiredPadR) {
                tv.setPadding(desiredPadL, tv.getPaddingTop(), desiredPadR, tv.getPaddingBottom());
                mutated = true;
            }
            if (isSingleLineView(tv, tv.getMaxLines()) != targetSingleLine) {
                tv.setSingleLine(targetSingleLine);
                mutated = true;
            }
            if (targetLines > 0 && tv.getMaxLines() != targetLines) {
                tv.setMaxLines(targetLines);
                mutated = true;
            }
            if (Build.VERSION.SDK_INT >= 23 && targetLines >= 2) {
                if (tv.getBreakStrategy() != Layout.BREAK_STRATEGY_BALANCED) {
                    tv.setBreakStrategy(Layout.BREAK_STRATEGY_BALANCED);
                    mutated = true;
                }
                if (tv.getHyphenationFrequency() != Layout.HYPHENATION_FREQUENCY_NONE) {
                    tv.setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE);
                    mutated = true;
                }
            }
            if (tv.getEllipsize() != targetEllipsize) {
                tv.setEllipsize(targetEllipsize);
                mutated = true;
            }
            if (tv.getMaxWidth() != targetMaxWidth) {
                tv.setMaxWidth(targetMaxWidth);
                mutated = true;
            }
            if (Math.abs(tv.getTextScaleX() - targetScaleX) > 0.005f) {
                tv.setTextScaleX(targetScaleX);
                mutated = true;
            }
            if (Math.abs(tv.getTextSize() - targetPx) >= 0.4f) {
                tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, targetPx);
                mutated = true;
            }

            if (plan.state == 5) {
                measurePaint.setTextSize(targetPx);
                measurePaint.setTextScaleX(targetScaleX);
                logAuditOverflowOnce(tv, str,
                        TypographyNormalizer.measureHarfBuzzWidth(str, measurePaint),
                        safeTextAvailW,
                        hasEllipsis || targetEllipsize == TextUtils.TruncateAt.END,
                        targetLines);
            }

            if (!mutated) {
                tv.setTag(TAG_KEY_LAST_FIT_QUICK, Integer.valueOf(quickHash));
            }
            return mutated;
        } catch (Throwable ignored) {}
        return false;
    }

    private static boolean isCompoundSwitchOrCheck(TextView tv) {
        return tv instanceof android.widget.CompoundButton || isSwitchCompat(tv);
    }

    /**
     * Enforces internal horizontal centering on symmetric elements (buttons, tabs, grid labels, popup headers)
     * while preserving left aligned export rows, draft metadata readouts, and numeric readouts.
     */
    public static boolean enforceCenterAlignmentIfApplicable(TextView tv) {
        if (tv == null || !isTextAutoFitActive()) return false;
        if (isInsideMainActivityWindow(tv)) {
            return false;
        }
        if (WorkspaceUiController.isPaperNameView(tv)) {
            return WorkspaceUiController.centerPaperTextureLabelIfApplicable(tv);
        }
        if (isInsideHorizontalScrollView(tv)) return false;
        boolean mutated = false;
        try {
            if (tv.getTag(TAG_KEY_ALIGNED) != null) return false;

            CharSequence text = tv.getText();
            if (text == null || text.length() == 0) return false;
            String str = text.toString();
            if (str.length() > 60 || str.contains("\n\n") || str.contains(". ") || isDynamicNumericReadoutText(str)) {
                tv.setTag(TAG_KEY_ALIGNED, Boolean.TRUE);
                return false;
            }

            if (WorkspaceUiController.isExportRowView(tv)) {
                tv.setTag(TAG_KEY_ALIGNED, Boolean.TRUE);
                return false;
            }

            if (WorkspaceUiController.isDraftInfoRowChild(tv)) {
                tv.setTag(TAG_KEY_ALIGNED, Boolean.TRUE);
                return false;
            }

            boolean isVerticalIconCard = false;
            ViewParent parent = tv.getParent();
            if (parent instanceof android.widget.RelativeLayout) {
                android.widget.RelativeLayout rl = (android.widget.RelativeLayout) parent;
                for (int i = 0; i < rl.getChildCount(); i++) {
                    View ch = rl.getChildAt(i);
                    if (ch instanceof android.widget.ImageView && ch.getVisibility() != View.GONE) {
                        boolean stackedBelow = ch.getBottom() > 0 && tv.getTop() > 0
                                && ch.getBottom() <= tv.getTop() + 8;
                        if (stackedBelow) {
                            isVerticalIconCard = true;
                        } else {
                            int g = tv.getGravity() & Gravity.HORIZONTAL_GRAVITY_MASK;
                            if (g != Gravity.CENTER_HORIZONTAL) {
                                return false;
                            }
                        }
                        break;
                    }
                }
            }

            int gravity = tv.getGravity();
            if (computeRightSiblingOverlap(tv, tv.getResources().getDisplayMetrics().density) > 0) {
                return false;
            }
            boolean isInsideClickable = parent instanceof View && ((View) parent).isClickable();
            boolean isButton = isVerticalIconCard || tv instanceof android.widget.Button || tv.isClickable() || isInsideClickable;
            boolean isCenteredHori = (gravity & Gravity.HORIZONTAL_GRAVITY_MASK) == Gravity.CENTER_HORIZONTAL;

            boolean inMainActivity = isInsideMainActivityWindow(tv);
            if (isCenteredHori || isButton) {
                int newGravity = (gravity & ~Gravity.HORIZONTAL_GRAVITY_MASK) | Gravity.CENTER_HORIZONTAL;
                if (gravity != newGravity) {
                    tv.setGravity(newGravity);
                    mutated = true;
                }
                if (Build.VERSION.SDK_INT >= 17 && tv.getTextAlignment() != View.TEXT_ALIGNMENT_CENTER) {
                    tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    mutated = true;
                }

                if (!inMainActivity && parent instanceof LinearLayout) {
                    ViewGroup.LayoutParams lp = tv.getLayoutParams();
                    if (lp instanceof LinearLayout.LayoutParams) {
                        LinearLayout.LayoutParams llp = (LinearLayout.LayoutParams) lp;
                        int targetLlpGrav = ((LinearLayout) parent).getOrientation() == LinearLayout.VERTICAL
                                ? Gravity.CENTER_HORIZONTAL
                                : ((llp.gravity & Gravity.VERTICAL_GRAVITY_MASK) | Gravity.CENTER_HORIZONTAL);
                        if (llp.gravity != targetLlpGrav) {
                            llp.gravity = targetLlpGrav;
                            tv.setLayoutParams(llp);
                            mutated = true;
                        }
                    }
                }
                if (isVerticalIconCard || (tv.getWidth() > 0 && tv.getTop() > 0)) {
                    tv.setTag(TAG_KEY_ALIGNED, Boolean.TRUE);
                }
            } else {
                tv.setTag(TAG_KEY_ALIGNED, Boolean.TRUE);
            }
        } catch (Throwable ignored) {}
        return mutated;
    }

    /**
     * Fast check if a CharSequence contains any Chinese character (CJK Unified Ideographs).
     */
    public static boolean containsChinese(CharSequence s) {
        if (s == null) return false;
        int len = s.length();
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if (c >= '\u4e00' && c <= '\u9fff') {
                return true;
            }
        }
        return false;
    }

    /**
     * Translates official swatch palette titles seeded in English when switching to a non-English pack.
     */
    private static String translateSeededEnglishLabel(String s) {
        if (s == null || s.isEmpty() || !isDynamicI18nEnabled() || isEnglish() || isChinesePack()) {
            return null;
        }
        String defLocal = sTextMap.get("(\u9ed8\u8ba4)");
        if (defLocal == null || defLocal.isEmpty()) {
            defLocal = isVietnamese() ? "(M\u1eb7c \u0111\u1ecbnh)" : "(Default)";
        }
        String trimmedDef = defLocal.trim();
        String base = s;
        String suffix = "";
        if (base.endsWith("(Default)")) {
            base = base.substring(0, base.length() - 9).trim();
            suffix = " " + trimmedDef;
        } else if (!trimmedDef.isEmpty() && base.endsWith(trimmedDef)) {
            base = base.substring(0, base.length() - trimmedDef.length()).trim();
            suffix = " " + trimmedDef;
        }
        String zhKey = null;
        if ("Basic Colors".equals(base)) {
            zhKey = "\u57fa\u672c\u8272";
        } else if ("Traditional Chinese Colors".equals(base)) {
            zhKey = "\u4e2d\u56fd\u4f20\u7edf\u8272";
        } else if ("Skin Tones".equals(base)) {
            zhKey = "\u80a4\u8272";
        } else if ("Retro Nostalgia".equals(base)) {
            zhKey = "\u590d\u53e4\u60c5\u6000";
        } else if ("Neon Starry Sky".equals(base)) {
            zhKey = "\u9713\u8679\u661f\u7a7a";
        }
        if (zhKey != null) {
            String mapped = sTextMap.get(zhKey);
            if (mapped != null && !mapped.isEmpty()) {
                return mapped + suffix;
            }
        }
        String basicLocal = sTextMap.get("\u57fa\u672c\u8272");
        if (!suffix.isEmpty() && basicLocal != null && basicLocal.equals(base)
                && !s.equals(basicLocal + suffix)) {
            return basicLocal + suffix;
        }
        return null;
    }

    private static boolean hasUnitSuffix(String s, int end, String unit) {
        int uLen = unit.length();
        if (end <= uLen) return false;
        int start = end - uLen;
        char before = s.charAt(start - 1);
        if ((before < '0' || before > '9') && before != ' ') {
            return false;
        }
        return s.regionMatches(true, start, unit, 0, uLen);
    }

    private static boolean isPureNumericOrSymbol(String s) {
        int len = s.length();
        if (len == 0) return true;
        int end = len;
        while (end > 0 && s.charAt(end - 1) == ' ') {
            end--;
        }
        if (end == 0) return true;
        if (hasUnitSuffix(s, end, "fps")) {
            end -= 3;
        } else if (hasUnitSuffix(s, end, "px")
                || hasUnitSuffix(s, end, "dp")
                || hasUnitSuffix(s, end, "sp")
                || hasUnitSuffix(s, end, "pt")
                || hasUnitSuffix(s, end, "mm")
                || hasUnitSuffix(s, end, "cm")) {
            end -= 2;
        }
        boolean hasDigit = false;
        for (int i = 0; i < end; i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') {
                hasDigit = true;
            } else if (c == '.' || c == ',' || c == '-' || c == '+' || c == '%' || c == ' '
                    || c == ':' || c == '/' || c == 'x' || c == 'X' || c == '#' || c == '\u00b0' || c == '\u00d7') {
                continue;
            } else {
                return false;
            }
        }
        return hasDigit;
    }

    /**
     * General string translator by text content.
     */
    public static String translate(String original) {
        if (original == null || original.isEmpty() || !isDynamicI18nEnabled()) {
            return original;
        }
        if (isPureNumericOrSymbol(original)) {
            return original;
        }
        if (!sInitialized) {
            ensureInitialized();
        }
        String cached = sRuntimeTranslationCache.get(original);
        if (cached != null) {
            return cached;
        }
        String result = translateUncached(original);
        if (result != null) {
            if (sRuntimeTranslationCache.size() >= RUNTIME_CACHE_MAX_SIZE) {
                sRuntimeTranslationCache.clear();
            }
            sRuntimeTranslationCache.put(original, result);
        }
        return result;
    }

    private static String translateUncached(String original) {
        try {
            // 1. Direct exact match
            String translated = sTextMap.get(original);
            if (translated != null && !translated.isEmpty()) {
                return translated;
            }

            String seededPalette = translateSeededEnglishLabel(original);
            if (seededPalette != null) {
                return seededPalette;
            }

            if (!containsChinese(original)) {
                return original;
            }

            // 2. Trimmed match (handling surrounding spaces or newlines)
            String trimmed = original.trim();
            if (!trimmed.equals(original)) {
                String match = sTextMap.get(trimmed);
                if (match != null && !match.isEmpty()) {
                    return original.replace(trimmed, match);
                }
            }

            if (isChinesePack()) {
                return original;
            }

            // 3. Dynamic numeric layer suffix pattern
            if (original.contains("图层")) {
                String res = PATTERN_INLINE_LAYER.matcher(original)
                        .replaceAll(isVietnamese() ? "$1 lớp" : "$1 Layer");
                if (!containsChinese(res)) {
                    return res;
                }
            }

            // 4. Dynamic numeric day suffix pattern
            if (original.contains("天")) {
                String res = PATTERN_INLINE_DAY.matcher(original)
                        .replaceAll(isVietnamese() ? "$1 ngày" : "$1 Days");
                if (!containsChinese(res)) {
                    return res;
                }
            }

            // 5. Dynamic expiration date pattern
            if (original.contains("到期") && original.contains("年") && original.contains("月")) {
                String res = PATTERN_INLINE_EXPIRE_DATE.matcher(original)
                        .replaceAll(isVietnamese() ? "Hết hạn ngày $3/$2/$1" : "Expires on $1-$2-$3");
                if (!containsChinese(res)) {
                    return res;
                }
            }

            // 6. Generic Chinese date pattern
            if (original.contains("年") && original.contains("月") && original.contains("日")) {
                String res = PATTERN_INLINE_DATE.matcher(original)
                        .replaceAll(isVietnamese() ? "$3/$2/$1" : "$1-$2-$3");
                if (!containsChinese(res)) {
                    return res;
                }
            }

            // 7. Dynamic range pattern
            if (original.startsWith("范围（") && original.endsWith("）")) {
                String inner = original.substring(3, original.length() - 1);
                return isVietnamese() ? ("Giới hạn (" + inner + ")") : ("Range (" + inner + ")");
            }
            if (original.startsWith("范围(") && original.endsWith(")")) {
                String inner = original.substring(3, original.length() - 1);
                return isVietnamese() ? ("Gi\u1edbi h\u1ea1n (" + inner + ")") : ("Range (" + inner + ")");
            }
            if (original.startsWith("导出草稿到")) {
                String rawTarget = original.substring(5).trim();
                String mappedTarget = rawTarget.isEmpty() ? "" : translate(rawTarget);
                return isVietnamese()
                        ? ("Xu\u1ea5t b\u1ea3n nh\u00e1p t\u1edbi " + mappedTarget)
                        : ("Export draft to " + mappedTarget);
            }
            if (trimmed.startsWith("\u4e00\u5171\u753b\u4e86") && trimmed.endsWith("\u7b14")) {
                String countPart = trimmed.substring(4, trimmed.length() - 1).trim();
                if (!countPart.isEmpty() && !containsChinese(countPart)) {
                    String prefix = get("\u4e00\u5171\u753b\u4e86", isVietnamese() ? "\u0110\u00e3 v\u1ebd" : "Drew");
                    String unit = get("\u7b14", isVietnamese() ? "n\u00e9t" : "strokes");
                    return prefix + " " + countPart + " " + unit;
                }
            }
            if (trimmed.startsWith("\u4f5c\u753b\u8017\u65f6")) {
                String restPart = trimmed.substring(4).trim();
                String transRest = restPart.isEmpty() ? "" : translate(restPart);
                String prefix = get("\u4f5c\u753b\u8017\u65f6", isVietnamese() ? "Th\u1eddi gian v\u1ebd" : "Drawing time");
                return transRest.isEmpty() ? prefix : (prefix + ": " + transRest);
            }

            // 8. Dynamic numbered prefix and unit patterns for FastKey, brushes, layers, frames, and presets.
            String dynamicNum = translateDynamicNumberedPattern(trimmed);
            if (dynamicNum != null) {
                return original.equals(trimmed) ? dynamicNum : original.replace(trimmed, dynamicNum);
            }

            // 9. Suffix aware lookup for auto numbered brushes and folders.
            String suffixTranslated = translateWithNumericSuffix(trimmed);
            if (suffixTranslated != null) {
                return original.equals(trimmed) ? suffixTranslated : original.replace(trimmed, suffixTranslated);
            }
        } catch (Throwable ignored) {}
        return original;
    }

    private static String translateDynamicNumberedPattern(String s) {
        if (s == null || s.isEmpty() || !containsChinese(s)) return null;
        boolean vi = isVietnamese();
        Matcher m;

        m = PATTERN_ACTION_NUM.matcher(s);
        if (m.matches()) {
            return (vi ? "Thao t\u00e1c " : "Action ") + m.group(1);
        }
        m = PATTERN_BRUSH_NUM.matcher(s);
        if (m.matches()) {
            return (vi ? "C\u1ecd " : "Brush ") + m.group(1);
        }
        m = PATTERN_COLOR_NUM.matcher(s);
        if (m.matches()) {
            return (vi ? "B\u1ea3ng m\u00e0u " : "Palette ") + m.group(1);
        }
        m = PATTERN_PRESET_NUM.matcher(s);
        if (m.matches()) {
            return (vi ? "M\u1eabu " : "Preset ") + m.group(1);
        }
        m = PATTERN_GROUP_NUM.matcher(s);
        if (m.matches()) {
            return (vi ? "Nh\u00f3m " : "Group ") + m.group(1);
        }
        m = PATTERN_FRAME_ORDINAL.matcher(s);
        if (m.matches()) {
            return (vi ? "Khung " : "Frame ") + m.group(1);
        }
        m = PATTERN_LAYER_ORDINAL.matcher(s);
        if (m.matches()) {
            return "Layer " + m.group(1);
        }
        m = PATTERN_PAGE_ORDINAL.matcher(s);
        if (m.matches()) {
            return (vi ? "Trang " : "Page ") + m.group(1);
        }

        String candidate = s;
        if (candidate.contains("\u50cf\u7d20")) {
            candidate = PATTERN_UNIT_PX.matcher(candidate).replaceAll("$1px");
        }
        if (candidate.contains("\u5f20") || candidate.contains("\u5e27")) {
            candidate = PATTERN_UNIT_FRAMES.matcher(candidate).replaceAll(vi ? "$1 khung" : "$1 frames");
        }
        if (candidate.contains("\u5c42")) {
            candidate = PATTERN_UNIT_LAYERS.matcher(candidate).replaceAll("$1 Layer");
        }
        if (candidate.contains("\u79d2")) {
            candidate = PATTERN_UNIT_SEC.matcher(candidate).replaceAll("$1s");
        }
        if (!candidate.equals(s) && !containsChinese(candidate)) {
            return candidate;
        }
        return null;
    }

    private static String translateWithNumericSuffix(String s) {
        if (s == null || s.length() < 3) return null;
        int len = s.length();
        char last = s.charAt(len - 1);
        // Case A: "<base>_<digits>" or "<base>-<digits>"
        if (last >= '0' && last <= '9') {
            int pos = len - 1;
            while (pos >= 0 && s.charAt(pos) >= '0' && s.charAt(pos) <= '9') {
                pos--;
            }
            if (pos > 0 && (s.charAt(pos) == '_' || s.charAt(pos) == '-')) {
                String base = s.substring(0, pos).trim();
                if (!base.isEmpty() && containsChinese(base)) {
                    String baseTr = sTextMap.get(base);
                    if (baseTr != null && !baseTr.isEmpty()) {
                        return baseTr + s.substring(pos);
                    }
                }
            }
            return null;
        }
        // Case B: "<base>(<digits>)" or "<base> (<digits>)" with ASCII or fullwidth parens
        if (last == ')' || last == '\uff09') {
            int openIdx = s.lastIndexOf(last == ')' ? '(' : '\uff08');
            if (openIdx > 0 && openIdx < len - 2) {
                boolean allDigits = true;
                for (int i = openIdx + 1; i < len - 1; i++) {
                    char ch = s.charAt(i);
                    if (ch < '0' || ch > '9') {
                        allDigits = false;
                        break;
                    }
                }
                if (allDigits) {
                    String digits = s.substring(openIdx + 1, len - 1);
                    String base = s.substring(0, openIdx).trim();
                    if (!base.isEmpty() && containsChinese(base)) {
                        String baseTr = sTextMap.get(base);
                        if (baseTr != null && !baseTr.isEmpty()) {
                            return baseTr + " (" + digits + ")";
                        }
                    }
                }
            }
        }
        return null;
    }

    private static String normalizeNumberUnitSpacing(String s) {
        if (s == null || s.length() < 5) return s;
        int i = 0;
        int n = s.length();
        while (i < n && s.charAt(i) >= '0' && s.charAt(i) <= '9') {
            i++;
        }
        if (i == 0 || i >= n) return s;
        String tail = s.substring(i);
        if ("Layer".equals(tail) || "Layers".equals(tail) || "L\u1edbp".equals(tail)) {
            return s.substring(0, i) + " " + tail;
        }
        return s;
    }

    /**
     * Recursively traverses any view hierarchy and translates TextViews, Hints,
     * Switch/SwitchCompat texts, and ContentDescriptions.
     * Caches original untranslated strings in view tags so dynamic language switching
     * and toggling dynamic i18n off restore source strings cleanly.
     */
    public static void translateViewHierarchy(View view) {
        translateViewHierarchyTracked(view);
    }

    public static boolean translateViewHierarchyTracked(View view) {
        if (view == null || view.getVisibility() == View.GONE) return false;
        if ("libre_settings_root".equals(view.getTag())) {
            return false;
        }
        if (!sInitialized) {
            ensureInitialized();
        }
        boolean changed = false;
        try {
            if (view instanceof TextView) {
                changed |= translateTextView((TextView) view);
            } else if (view instanceof ImageView) {
                Drawable d = ((ImageView) view).getDrawable();
                if (d instanceof LocalizedDraftPlaceholderDrawable) {
                    if (((LocalizedDraftPlaceholderDrawable) d).syncWithCurrentPack()) {
                        view.invalidate();
                        changed = true;
                    }
                }
            }

            changed |= translateSwitchTexts(view);

            CharSequence desc = view.getContentDescription();
            Object tagDesc = view.getTag(TAG_KEY_ORIG_DESC);
            String origDesc = null;
            if (tagDesc instanceof String) {
                origDesc = (String) tagDesc;
            } else if (desc != null && desc.length() > 0 && containsChinese(desc)) {
                origDesc = desc.toString();
                view.setTag(TAG_KEY_ORIG_DESC, origDesc);
            }
            if (origDesc != null) {
                String transDesc = translate(origDesc);
                if (desc == null || !desc.toString().equals(transDesc)) {
                    view.setContentDescription(transDesc);
                    changed = true;
                }
            }

            if (view instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) view;
                int childCount = vg.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    changed |= translateViewHierarchyTracked(vg.getChildAt(i));
                }
            }
        } catch (Throwable ignored) {}
        return changed;
    }

    private static void attachTextWatcherIfNeeded(final TextView tv) {
        if (tv == null || tv instanceof android.widget.EditText) return;
        if (tv.getTag(TAG_KEY_TEXT_WATCHER) != null) return;
        tv.setTag(TAG_KEY_TEXT_WATCHER, Boolean.TRUE);
        tv.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (tv.getTag(TAG_KEY_WATCHER_GUARD) != null) return;
                if (s == null || s.length() == 0) return;
                if (isDynamicI18nEnabled() && (containsChinese(s) || translateSeededEnglishLabel(s.toString()) != null)) {
                    try {
                        tv.setTag(TAG_KEY_WATCHER_GUARD, Boolean.TRUE);
                        translateTextView(tv);
                    } finally {
                        tv.setTag(TAG_KEY_WATCHER_GUARD, null);
                    }
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    public static boolean translateSingleTextView(TextView tv) {
        if (tv == null || !isDynamicI18nEnabled()) return false;
        if (!sInitialized) {
            ensureInitialized();
        }
        try {
            return translateTextView(tv);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean translateTextView(TextView tv) {
        attachTextWatcherIfNeeded(tv);
        WorkspaceUiController.centerShareLabelIfApplicable(tv);
        boolean changed = false;
        int flipKind = WorkspaceUiController.getFlipButtonKind(tv);
        boolean isFlipButton = flipKind != 0;

        if (isFlipButton) {
            String canonicalOrig = (flipKind == 1) ? "水平翻转" : "垂直翻转";
            String key = (flipKind == 1) ? "libre_canvas_flip_hori" : "libre_canvas_flip_verti";
            tv.setTag(TAG_KEY_ORIG_TEXT, canonicalOrig);
            String targetText;
            if (!isDynamicI18nEnabled()) {
                targetText = canonicalOrig;
            } else {
                String byKey = sTextMap.get(key);
                if (byKey != null && !byKey.isEmpty()) {
                    targetText = byKey;
                } else {
                    String mapped = translate(canonicalOrig);
                    if (!mapped.equals(canonicalOrig)) {
                        targetText = mapped;
                    } else if (isVietnamese()) {
                        targetText = (flipKind == 1) ? "Lật ngang" : "Lật dọc";
                    } else if (isEnglish()) {
                        targetText = (flipKind == 1) ? "Flip Horizontal" : "Flip Vertical";
                    } else {
                        targetText = canonicalOrig;
                    }
                }
            }
            CharSequence curText = tv.getText();
            if (curText == null || !curText.toString().equals(targetText)) {
                tv.setText(targetText);
                changed = true;
            }
            if (isTextAutoFitActive()) {
                changed |= enforceCenterAlignmentIfApplicable(tv);
                changed |= fitNarrowTextView(tv);
            } else {
                changed |= fitNarrowTextView(tv);
            }
        } else {
            CharSequence text = tv.getText();
            Object tagText = tv.getTag(TAG_KEY_ORIG_TEXT);
            Object lastTrans = tv.getTag(TAG_KEY_LAST_TRANSLATED);
            String orig = null;
            if (text != null && text.length() > 0 && (containsChinese(text)
                    || (!(tv instanceof android.widget.EditText) && translateSeededEnglishLabel(text.toString()) != null))) {
                orig = text.toString();
                tv.setTag(TAG_KEY_ORIG_TEXT, orig);
                tv.setTag(TAG_KEY_LAST_TRANSLATED, null);
            } else if (tagText instanceof String
                    && text != null
                    && lastTrans instanceof String
                    && text.toString().equals(lastTrans)) {
                orig = (String) tagText;
            } else if (tagText != null || lastTrans != null) {
                tv.setTag(TAG_KEY_ORIG_TEXT, null);
                tv.setTag(TAG_KEY_LAST_TRANSLATED, null);
            }
            boolean translatedHere = false;
            if (orig != null) {
                String trans = translate(orig);
                translatedHere = true;
                if (text == null || !text.toString().equals(trans)) {
                    tv.setText(trans);
                    text = trans;
                    changed = true;
                }
            }
            if (text != null && isDynamicI18nEnabled()) {
                String spaced = normalizeNumberUnitSpacing(text.toString());
                if (!spaced.equals(text.toString())) {
                    tv.setText(spaced);
                    text = spaced;
                    changed = true;
                }
            }
            if (changed && tv.getTag(TAG_KEY_ORIG_TEXT_SIZE) != null && isInsideRecycledList(tv)) {
                restoreOrigTextViewState(tv);
            }
            if (translatedHere && text != null) {
                tv.setTag(TAG_KEY_LAST_TRANSLATED, text.toString());
            }
            if (text != null && text.length() > 0) {
                if (isTextAutoFitActive()) {
                    changed |= WorkspaceUiController.centerPaperTextureLabelIfApplicable(tv);
                    changed |= enforceCenterAlignmentIfApplicable(tv);
                }
                changed |= fitNarrowTextView(tv);
            }
        }

        CharSequence hint = tv.getHint();
        Object tagHint = tv.getTag(TAG_KEY_ORIG_HINT);
        String origHint = null;
        if (tagHint instanceof String) {
            origHint = (String) tagHint;
        } else if (hint != null && hint.length() > 0 && containsChinese(hint)) {
            origHint = hint.toString();
            tv.setTag(TAG_KEY_ORIG_HINT, origHint);
        }
        if (origHint != null) {
            String transHint = translate(origHint);
            if (hint == null || !hint.toString().equals(transHint)) {
                tv.setHint(transHint);
                changed = true;
            }
        }
        return changed;
    }

    private static boolean isSwitchCompat(View view) {
        if (!(view instanceof android.widget.CompoundButton)) {
            return false;
        }
        Class<?> cls = view.getClass();
        while (cls != null && cls != Object.class) {
            String name = cls.getName();
            if ("androidx.appcompat.widget.SwitchCompat".equals(name)
                    || "android.support.v7.widget.SwitchCompat".equals(name)) {
                return true;
            }
            cls = cls.getSuperclass();
        }
        return false;
    }

    private static void resolveSwitchCompatMethods(Class<?> cls) {
        if (sSwitchCompatMethodsResolved) return;
        try {
            sSwitchCompatGetOn = cls.getMethod("getTextOn");
            sSwitchCompatSetOn = cls.getMethod("setTextOn", CharSequence.class);
            sSwitchCompatGetOff = cls.getMethod("getTextOff");
            sSwitchCompatSetOff = cls.getMethod("setTextOff", CharSequence.class);
            sSwitchCompatMethodsResolved = true;
        } catch (Throwable ignored) {}
    }

    private static String computeSwitchTargetText(View view, int tagKey, CharSequence current,
                                                  String defaultOrigZh, boolean isOn) {
        Object cached = view.getTag(tagKey);
        String orig = null;
        if (cached instanceof String) {
            orig = (String) cached;
        } else if (current != null && current.length() > 0) {
            String s = current.toString();
            if (isOn && ("开启".equals(s) || "开".equals(s) || "BẬT".equalsIgnoreCase(s) || "ON".equalsIgnoreCase(s))) {
                orig = ("ON".equalsIgnoreCase(s) || "BẬT".equalsIgnoreCase(s)) ? defaultOrigZh : s;
            } else if (!isOn && ("关闭".equals(s) || "关".equals(s)
                    || "TẮT".equalsIgnoreCase(s) || "OFF".equalsIgnoreCase(s) || "Đóng".equals(s))) {
                orig = ("OFF".equalsIgnoreCase(s) || "TẮT".equalsIgnoreCase(s) || "Đóng".equals(s)) ? defaultOrigZh : s;
            } else if (containsChinese(s)) {
                orig = s;
            }
            if (orig != null) {
                view.setTag(tagKey, orig);
            }
        }

        if (orig == null) {
            return null;
        }
        if (!isDynamicI18nEnabled()) {
            return orig;
        }
        if (isOn && ("开启".equals(orig) || "开".equals(orig))) {
            if (isVietnamese()) return "BẬT";
            if (isEnglish()) return "ON";
        } else if (!isOn && ("关闭".equals(orig) || "关".equals(orig))) {
            if (isVietnamese()) return "TẮT";
            if (isEnglish()) return "OFF";
        }
        return translate(orig);
    }

    private static boolean translateSwitchTexts(View view) {
        if (!(view instanceof android.widget.CompoundButton)) {
            return false;
        }
        if (view instanceof android.widget.Switch) {
            android.widget.Switch sw = (android.widget.Switch) view;
            boolean changed = false;
            CharSequence curOn = sw.getTextOn();
            String targetOn = computeSwitchTargetText(sw, TAG_KEY_ORIG_SWITCH_ON, curOn, "开启", true);
            if (targetOn != null && (curOn == null || !curOn.toString().equals(targetOn))) {
                sw.setTextOn(targetOn);
                changed = true;
            }
            CharSequence curOff = sw.getTextOff();
            String targetOff = computeSwitchTargetText(sw, TAG_KEY_ORIG_SWITCH_OFF, curOff, "关闭", false);
            if (targetOff != null && (curOff == null || !curOff.toString().equals(targetOff))) {
                sw.setTextOff(targetOff);
                changed = true;
            }
            if (changed) {
                sw.requestLayout();
                sw.invalidate();
            }
            return changed;
        }

        if (isSwitchCompat(view)) {
            try {
                resolveSwitchCompatMethods(view.getClass());
                if (!sSwitchCompatMethodsResolved) return false;
                boolean changed = false;
                CharSequence curOn = (CharSequence) sSwitchCompatGetOn.invoke(view);
                String targetOn = computeSwitchTargetText(view, TAG_KEY_ORIG_SWITCH_ON, curOn, "开启", true);
                if (targetOn != null && (curOn == null || !curOn.toString().equals(targetOn))) {
                    sSwitchCompatSetOn.invoke(view, targetOn);
                    changed = true;
                }
                CharSequence curOff = (CharSequence) sSwitchCompatGetOff.invoke(view);
                String targetOff = computeSwitchTargetText(view, TAG_KEY_ORIG_SWITCH_OFF, curOff, "关闭", false);
                if (targetOff != null && (curOff == null || !curOff.toString().equals(targetOff))) {
                    sSwitchCompatSetOff.invoke(view, targetOff);
                    changed = true;
                }
                if (changed) {
                    view.requestLayout();
                    view.invalidate();
                }
                return changed;
            } catch (Throwable ignored) {}
        }
        return false;
    }

    public static void preTranslateBubble(PopupWindow pw) {
        if (pw == null) return;
        try {
            View cv = pw.getContentView();
            if (cv != null) {
                onBubblePopupPreShow(cv);
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Pre show dialog interception hook.
     * Synchronously applies enabled feature rules and translation before the first rasterized frame.
     */
    public static void onDialogPreShow(Dialog dialog) {
        if (dialog == null) return;
        try {
            android.view.Window win = dialog.getWindow();
            if (win != null) {
                View decor = win.getDecorView();
                if (decor != null) {
                    if (LibreConfig.isFeatureActive(LibreConfig.KEY_HIDE_TOPBAR_CLOUD)
                            || LibreConfig.isFeatureActive(LibreConfig.KEY_HIDE_CANVAS_POPUP_CLOUD)) {
                        WorkspaceUiController.optimizePaintCloudViews(decor);
                    }
                    if (LibreConfig.isFeatureActive(LibreConfig.KEY_HIDE_CANVAS_EXIT_CLOUD)
                            || isTextAutoFitActive()) {
                        WorkspaceUiController.optimizeCanvasExitDialog(decor);
                    }
                    if (GlobalCompatibilityController.isHideExportSocialIconsActive()
                            || GlobalCompatibilityController.isHideTimelapseExportSocialActive()
                            || OfflineAccountController.isOfflineProfileActive()
                            || WorkspaceUiController.isCanvasPopupPolishActive()) {
                        WorkspaceUiController.optimizeExportDialog(decor);
                    }
                    if (LibreConfig.isFeatureActive(LibreConfig.KEY_HIDE_DRAWER_VIP)) {
                        WorkspaceUiController.hideDrawerVipIfEnabled(decor);
                    }
                    WorkspaceUiController.optimizeAddBrushDialog(decor);
                    decor.setTag(TAG_KEY_DIALOG_ROOT, Boolean.TRUE);
                    attachViewTreeObserver(decor);
                }
            }
        } catch (Throwable ignored) {}
    }

    public static void onBubblePopupPreShow(View contentView) {
        if (contentView == null) return;
        try {
            if (LibreConfig.isFeatureActive(LibreConfig.KEY_HIDE_TOPBAR_CLOUD)
                    || LibreConfig.isFeatureActive(LibreConfig.KEY_HIDE_CANVAS_POPUP_CLOUD)) {
                WorkspaceUiController.optimizePaintCloudViews(contentView);
            }
            if (GlobalCompatibilityController.isHideExportSocialIconsActive()
                    || GlobalCompatibilityController.isHideTimelapseExportSocialActive()
                    || OfflineAccountController.isOfflineProfileActive()
                    || WorkspaceUiController.isCanvasPopupPolishActive()) {
                WorkspaceUiController.optimizeExportDialog(contentView);
            }
            attachViewTreeObserver(contentView);
        } catch (Throwable ignored) {}
    }

    @SuppressWarnings("rawtypes")
    public static void sanitizeExportLists(Object exportPictureView, ArrayList list0, ArrayList list4) {
        GlobalCompatibilityController.sanitizeExportLists(exportPictureView, list0, list4);
    }

    private static String getEffectiveFileNameLower() {
        if (sCurrentLoadedFileName != null && !sCurrentLoadedFileName.isEmpty()) {
            return sCurrentLoadedFileName.toLowerCase();
        }
        if (LibreConfig.getInstance() != null) {
            String f = LibreConfig.getInstance().getSelectedLanguageFile();
            if (f != null) {
                return f.toLowerCase();
            }
        }
        return "";
    }

    public static boolean isVietnamese() {
        if (!sCachedFlagsReady) refreshCachedLangKind();
        return sCachedLangKind == 1;
    }

    public static boolean isEnglish() {
        if (!sCachedFlagsReady) refreshCachedLangKind();
        return sCachedLangKind == 2;
    }

    private static boolean isChinesePack() {
        if (!sCachedFlagsReady) refreshCachedLangKind();
        return sCachedLangKind == 3;
    }

    public static List<View> getWindowManagerViews() {
        List<View> result = new ArrayList<>();
        try {
            if (!sWmgResolved) {
                sWmgResolved = true;
                Class<?> wmgClass = Class.forName("android.view.WindowManagerGlobal");
                Method getInst = wmgClass.getMethod("getInstance");
                sWmgInstance = getInst.invoke(null);
                sWmgViewsField = wmgClass.getDeclaredField("mViews");
                sWmgViewsField.setAccessible(true);
            }
            if (sWmgInstance != null && sWmgViewsField != null) {
                List<?> views = (List<?>) sWmgViewsField.get(sWmgInstance);
                if (views != null) {
                    Object[] snapshot = views.toArray();
                    for (Object item : snapshot) {
                        if (item instanceof View) {
                            result.add((View) item);
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
        return result;
    }

    /**
     * Retranslates all currently open Activities and active Windows on the Main Looper
     * so language pack switches or toggling dynamic i18n take effect immediately.
     */
    public static void retranslateActiveViews(final Context context) {
        Runnable task = () -> {
            try {
                if (context instanceof Activity) {
                    trackActivity((Activity) context);
                }
                Set<View> roots = Collections.newSetFromMap(new IdentityHashMap<>());
                if (context instanceof Activity) {
                    Activity act = (Activity) context;
                    if (!act.isFinishing() && act.getWindow() != null && act.getWindow().getDecorView() != null) {
                        roots.add(act.getWindow().getDecorView());
                    }
                }
                List<Activity> snapshot = new ArrayList<>();
                synchronized (sTrackedActivities) {
                    Iterator<WeakReference<Activity>> it = sTrackedActivities.iterator();
                    while (it.hasNext()) {
                        Activity a = it.next().get();
                        if (a == null || a.isFinishing()) {
                            it.remove();
                        } else {
                            snapshot.add(a);
                        }
                    }
                }
                for (Activity a : snapshot) {
                    if (Build.VERSION.SDK_INT >= 17 && a.isDestroyed()) {
                        continue;
                    }
                    if (a.getWindow() != null && a.getWindow().getDecorView() != null) {
                        roots.add(a.getWindow().getDecorView());
                    }
                }
                for (View v : getWindowManagerViews()) {
                    roots.add(v);
                }
                for (View root : roots) {
                    translateViewHierarchy(root);
                }
            } catch (Throwable ignored) {}
        };
        if (Looper.myLooper() == Looper.getMainLooper()) {
            task.run();
        } else {
            sUiHandler.post(task);
        }
    }

    /**
     * Attaches translation observer to an Activity to translate views upon creation and layout changes.
     */
    public static void attachToActivity(Activity activity) {
        if (activity == null || activity.isFinishing()) return;
        try {
            init(activity);
            if (activity.getWindow() == null) return;
            View decor = activity.getWindow().getDecorView();
            if (decor != null) {
                decor.setTag(TAG_KEY_HOST_ACTIVITY_CLASS, activity.getClass().getName());
                attachViewTreeObserver(decor);
            }
        } catch (Throwable ignored) {}
    }

    private static boolean hasUntrackedVisibleWindowRoot() {
        try {
            if (!sWmgResolved) {
                getWindowManagerViews();
            }
            if (sWmgInstance != null && sWmgViewsField != null) {
                List<?> views = (List<?>) sWmgViewsField.get(sWmgInstance);
                if (views != null) {
                    int count = views.size();
                    for (int i = 0; i < count; i++) {
                        Object item = views.get(i);
                        if (item instanceof View) {
                            View wRoot = (View) item;
                            if (wRoot.getVisibility() == View.VISIBLE
                                    && wRoot.getTag(TAG_KEY_OBSERVED) == null) {
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private static int computeDecorStructuralHash(View root) {
        return computeDecorStructuralHashNode(root, 0);
    }

    private static int computeDecorStructuralHashNode(View v, int depth) {
        if (v == null || depth > 20) return 0;
        int vis = v.getVisibility();
        if (vis == View.GONE) return 8;
        if ("libre_settings_root".equals(v.getTag())) return 0;
        int h = ((vis + 1) * 31) + v.getId();
        if (v instanceof ViewGroup) {
            if (v instanceof android.widget.AbsListView
                    || v.getClass().getName().endsWith("RecyclerView")) {
                return h * 31 + 1;
            }
            ViewGroup vg = (ViewGroup) v;
            int childCount = vg.getChildCount();
            h = h * 31 + childCount;
            for (int i = 0; i < childCount; i++) {
                h = h * 31 + computeDecorStructuralHashNode(vg.getChildAt(i), depth + 1);
            }
        }
        return h;
    }

    private static void attachUntrackedWindowRoots() {
        if (sSyncingWindows) return;
        sSyncingWindows = true;
        try {
            List<View> windows = getWindowManagerViews();
            for (int i = 0; i < windows.size(); i++) {
                View wRoot = windows.get(i);
                if (wRoot != null
                        && wRoot.getVisibility() == View.VISIBLE
                        && wRoot.getTag(TAG_KEY_OBSERVED) == null) {
                    if (wRoot.getTag(TAG_KEY_HOST_ACTIVITY_CLASS) == null
                            && !isKnownActivityDecorView(wRoot)) {
                        wRoot.setTag(TAG_KEY_DIALOG_ROOT, Boolean.TRUE);
                    }
                    attachViewTreeObserver(wRoot);
                }
            }
        } catch (Throwable ignored) {
        } finally {
            sSyncingWindows = false;
        }
    }

    private static void attachUntrackedWindowRootsForAutoFit() {
        if (sSyncingWindows) return;
        sSyncingWindows = true;
        try {
            List<View> windows = getWindowManagerViews();
            for (int i = 0; i < windows.size(); i++) {
                View wRoot = windows.get(i);
                if (wRoot != null
                        && wRoot.getVisibility() == View.VISIBLE
                        && wRoot.getTag(TAG_KEY_OBSERVED) == null
                        && wRoot.getTag(TAG_KEY_AUTOFIT_OBSERVED) == null) {
                    if (wRoot.getTag(TAG_KEY_HOST_ACTIVITY_CLASS) == null
                            && !isKnownActivityDecorView(wRoot)) {
                        wRoot.setTag(TAG_KEY_DIALOG_ROOT, Boolean.TRUE);
                    }
                    attachAutoFitObserver(wRoot);
                }
            }
        } catch (Throwable ignored) {
        } finally {
            sSyncingWindows = false;
        }
    }

    private static boolean isKnownActivityDecorView(View root) {
        if (root == null) return false;
        synchronized (sTrackedActivities) {
            for (WeakReference<Activity> ref : sTrackedActivities) {
                Activity a = ref != null ? ref.get() : null;
                if (a != null && a.getWindow() != null && a.getWindow().getDecorView() == root) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean applyFullLayoutPass(View rootView) {
        if (rootView == null) return false;
        boolean changed = false;
        boolean fitActive = isTextAutoFitActive();
        boolean canvasPolish = WorkspaceUiController.isCanvasPopupPolishActive();
        boolean popupOrDialog = isPopupOrDialogRoot(rootView);
        boolean polishActive = popupOrDialog && canvasPolish;
        if (!popupOrDialog && rootView.getTag(TAG_KEY_HOST_ACTIVITY_CLASS) != null) {
            attachUntrackedWindowRoots();
        }
        if (fitActive || LibreConfig.isFeatureActive(LibreConfig.KEY_OPTIMIZE_CREATE_CANVAS)) {
            WorkspaceUiController.fixCreateCanvasTopBar(rootView);
        }
        if (canvasPolish) {
            changed |= WorkspaceUiController.optimizeReferenceFloatingWindow(rootView);
            WorkspaceUiController.polishBrushInfoRow(rootView);
            WorkspaceUiController.polishFastKeyHostCardHeader(rootView);
        }
        if (popupOrDialog) {
            WorkspaceUiController.optimizeAddBrushDialog(rootView);
            if (fitActive) {
                changed |= WorkspaceUiController.normalizeDialogTitles(rootView);
            }
        }
        if (popupOrDialog || WorkspaceUiController.isSettingPopupRoot(rootView)) {
            WorkspaceUiController.applyCanvasSettingPopupLayout(rootView);
        }
        if (polishActive) {
            WorkspaceUiController.normalizeThreeButtonCluster(rootView);
            WorkspaceUiController.normalizeExportFormatRows(rootView);
            WorkspaceUiController.optimizeBlendRow(rootView);
            WorkspaceUiController.centerAllIconButtonsRecursively(rootView);
        }
        changed |= translateViewHierarchyTracked(rootView);
        if (!fitActive) {
            changed |= WorkspaceUiController.restoreAllSolverStates(rootView);
        }
        if (canvasPolish || fitActive) {
            changed |= WorkspaceUiController.optimizeCanvasToolBars(rootView);
        }
        if (fitActive) {
            changed |= WorkspaceUiController.optimizeElasticPopupMenus(rootView);
            changed |= WorkspaceUiController.solveAllCanvasStripPeerGroups(rootView);
        }
        if (polishActive) {
            WorkspaceUiController.compactPaintSettingsScrollArea(rootView);
            WorkspaceUiController.resizePaintSettingsPopup(rootView);
            WorkspaceUiController.expandLeftToolGridIfApplicable(rootView);
            WorkspaceUiController.optimizeFastKeyPopups(rootView);
        }
        return changed;
    }

    /**
     * Translates RecyclerView / Adapter item views with zero permanent ViewTreeObserver retention.
     * For detached holders (initial onBindViewHolder), translates once onViewAttachedToWindow right
     * before measure/layout/draw. For already-attached in-place rebinds, schedules a single
     * self-removing OnPreDrawListener (with detach cleanup) that expires on Frame 0.
     */
    private static void attachOneShotItemObserver(final View itemView) {
        if (itemView == null) return;
        try {
            translateViewHierarchyTracked(itemView);
            if (Boolean.TRUE.equals(itemView.getTag(TAG_KEY_ITEM_ONE_SHOT_PENDING))) {
                return;
            }
            itemView.setTag(TAG_KEY_ITEM_ONE_SHOT_PENDING, Boolean.TRUE);
            if (Build.VERSION.SDK_INT >= 19 && !itemView.isAttachedToWindow()) {
                itemView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                    @Override
                    public void onViewAttachedToWindow(View v) {
                        v.removeOnAttachStateChangeListener(this);
                        v.setTag(TAG_KEY_ITEM_ONE_SHOT_PENDING, null);
                        translateViewHierarchyTracked(v);
                    }

                    @Override
                    public void onViewDetachedFromWindow(View v) {
                        v.removeOnAttachStateChangeListener(this);
                        v.setTag(TAG_KEY_ITEM_ONE_SHOT_PENDING, null);
                    }
                });
                return;
            }
            final ViewTreeObserver vto = itemView.getViewTreeObserver();
            if (vto == null || !vto.isAlive()) {
                itemView.setTag(TAG_KEY_ITEM_ONE_SHOT_PENDING, null);
                return;
            }
            final View.OnAttachStateChangeListener[] detachHolder = new View.OnAttachStateChangeListener[1];
            final ViewTreeObserver.OnPreDrawListener oneShot = new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    itemView.setTag(TAG_KEY_ITEM_ONE_SHOT_PENDING, null);
                    if (detachHolder[0] != null) {
                        itemView.removeOnAttachStateChangeListener(detachHolder[0]);
                        detachHolder[0] = null;
                    }
                    ViewTreeObserver liveVto = itemView.getViewTreeObserver();
                    if (liveVto != null && liveVto.isAlive()) {
                        liveVto.removeOnPreDrawListener(this);
                    }
                    boolean ch = translateViewHierarchyTracked(itemView);
                    return !(ch && itemView.isLayoutRequested());
                }
            };
            vto.addOnPreDrawListener(oneShot);
            if (Build.VERSION.SDK_INT >= 19) {
                detachHolder[0] = new View.OnAttachStateChangeListener() {
                    @Override
                    public void onViewAttachedToWindow(View v) {}

                    @Override
                    public void onViewDetachedFromWindow(View v) {
                        v.removeOnAttachStateChangeListener(this);
                        detachHolder[0] = null;
                        v.setTag(TAG_KEY_ITEM_ONE_SHOT_PENDING, null);
                        ViewTreeObserver liveVto = v.getViewTreeObserver();
                        if (liveVto != null && liveVto.isAlive()) {
                            liveVto.removeOnPreDrawListener(oneShot);
                        }
                    }
                };
                itemView.addOnAttachStateChangeListener(detachHolder[0]);
            }
        } catch (Throwable ignored) {
            itemView.setTag(TAG_KEY_ITEM_ONE_SHOT_PENDING, null);
        }
    }

    /**
     * Attaches frame zero observer and loop safe coalesced layout listener to any root view
     * so initial inflation, tab switches, and post tap panel changes render on Frame 0 without idle redraw loops.
     */
    public static void attachViewTreeObserver(final View rootView) {
        if (rootView == null) return;
        try {
            applyFullLayoutPass(rootView);

            if (Build.VERSION.SDK_INT >= 19 && !rootView.isAttachedToWindow()) {
                if (rootView.getTag(TAG_KEY_ATTACH_LISTENER) == null) {
                    rootView.setTag(TAG_KEY_ATTACH_LISTENER, Boolean.TRUE);
                    rootView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                        @Override
                        public void onViewAttachedToWindow(View v) {
                            v.removeOnAttachStateChangeListener(this);
                            v.setTag(TAG_KEY_ATTACH_LISTENER, null);
                            applyFullLayoutPass(v);
                            markLayoutDirty();
                            installLiveViewTreeObserver(v);
                        }

                        @Override
                        public void onViewDetachedFromWindow(View v) {}
                    });
                }
                return;
            }
            installLiveViewTreeObserver(rootView);
        } catch (Throwable ignored) {}
    }

    private static void installLiveViewTreeObserver(final View rootView) {
        if (rootView == null) return;
        try {
            View winRoot = rootView.getRootView();
            if (winRoot != null
                    && winRoot != rootView
                    && Boolean.TRUE.equals(winRoot.getTag(TAG_KEY_OBSERVED))
                    && !isPopupOrDialogRoot(rootView)) {
                markLayoutDirty();
                return;
            }
            if (rootView.getTag(TAG_KEY_OBSERVED) == null) {
                rootView.setTag(TAG_KEY_OBSERVED, Boolean.TRUE);
                final boolean isActivityDecor = !isPopupOrDialogRoot(rootView);
                // [0] = lastSeenDirtyEpoch, [1] = lastDecorStructHash, [2] = pendingFullPass
                final int[] decorState = new int[]{0, 0, 1};
                // [0] = stateHash, [1] = retryCount, [2] = lastSizeHash
                final int[] stateTracker = new int[]{0, 0, 0};
                final ViewTreeObserver.OnPreDrawListener preDrawListener = new ViewTreeObserver.OnPreDrawListener() {
                    private boolean mCanceledInitial = false;
                    private boolean mRunning = false;
                    private int mPreDrawRetries = 0;
                    private long mLastSizePreDrawMs = 0L;
                    @Override
                    public boolean onPreDraw() {
                        if (mRunning) return true;
                        if (isActivityDecor) {
                            if (decorState[2] == 0 && decorState[0] == sLayoutDirtyEpoch) {
                                return true;
                            }
                            mRunning = true;
                            decorState[2] = 0;
                            decorState[0] = sLayoutDirtyEpoch;
                            try {
                                boolean changed = applyFullLayoutPass(rootView);
                                decorState[1] = computeDecorStructuralHash(rootView);
                                if (changed && mPreDrawRetries < 2) {
                                    mPreDrawRetries++;
                                    decorState[2] = 1;
                                    rootView.requestLayout();
                                    return false;
                                }
                                mPreDrawRetries = 0;
                            } catch (Throwable ignored) {}
                            finally {
                                mRunning = false;
                            }
                            return true;
                        }
                        int stateHash = WorkspaceUiController.computePopupPreDrawStateHash(rootView);
                        int sizeHash = WorkspaceUiController.computePopupSizeHash(rootView);
                        boolean stateChanged = (stateHash != stateTracker[0]);
                        boolean sizeChanged = (sizeHash != stateTracker[2]);
                        if (mCanceledInitial && !stateChanged && !sizeChanged && !rootView.isLayoutRequested()) {
                            return true;
                        }
                        if (stateChanged) {
                            stateTracker[0] = stateHash;
                            stateTracker[1] = 0;
                            stateTracker[2] = sizeHash;
                        } else if (sizeChanged) {
                            stateTracker[2] = sizeHash;
                            long now = System.currentTimeMillis();
                            if (!mCanceledInitial || (now - mLastSizePreDrawMs >= 160L && stateTracker[1] < 2)) {
                                mLastSizePreDrawMs = now;
                            } else {
                                return true;
                            }
                        }
                        if (stateTracker[1] >= 2) {
                            return true;
                        }
                        mRunning = true;
                        try {
                            boolean changed = applyFullLayoutPass(rootView);
                            mCanceledInitial = true;
                            if (changed || rootView.isLayoutRequested()) {
                                stateTracker[1]++;
                                rootView.requestLayout();
                                return false;
                            }
                        } catch (Throwable ignored) {}
                        finally {
                            mRunning = false;
                        }
                        return true;
                    }
                };
                rootView.getViewTreeObserver().addOnPreDrawListener(preDrawListener);
                if (isActivityDecor && Build.VERSION.SDK_INT >= 18) {
                    rootView.getViewTreeObserver().addOnWindowFocusChangeListener(new ViewTreeObserver.OnWindowFocusChangeListener() {
                        @Override
                        public void onWindowFocusChanged(boolean hasFocus) {
                            attachUntrackedWindowRoots();
                        }
                    });
                }
                final class LiveGlobalLayoutObserver implements ViewTreeObserver.OnGlobalLayoutListener, View.OnAttachStateChangeListener {
                    private long mLastRun = 0;
                    private int mLastIdleHash = 0;
                    private int mLastIdleSizeHash = 0;
                    private long mLastSizeRun = 0L;
                    private int mTrailingBurst = 0;
                    private boolean mTrailingScheduled = false;
                    private boolean mInPass = false;
                    private final Runnable mTrailingRefPass = new Runnable() {
                        @Override
                        public void run() {
                            if (Build.VERSION.SDK_INT >= 19 && !rootView.isAttachedToWindow()) return;
                            if (mInPass || rootView.getVisibility() != View.VISIBLE) return;
                            if (!WorkspaceUiController.isCanvasPopupPolishActive()) return;
                            mInPass = true;
                            mLastRun = System.currentTimeMillis();
                            try {
                                WorkspaceUiController.optimizeReferenceFloatingWindow(rootView);
                            } catch (Throwable ignored) {}
                            finally {
                                mInPass = false;
                            }
                        }
                    };
                    private final Runnable mTrailingPass = new Runnable() {
                        @Override
                        public void run() {
                            mTrailingScheduled = false;
                            if (Build.VERSION.SDK_INT >= 19 && !rootView.isAttachedToWindow()) return;
                            if (mInPass || rootView.getVisibility() != View.VISIBLE) return;
                            mInPass = true;
                            mLastRun = System.currentTimeMillis();
                            try {
                                applyFullLayoutPass(rootView);
                            } catch (Throwable ignored) {}
                            finally {
                                mInPass = false;
                            }
                        }
                    };
                    @Override
                    public void onViewAttachedToWindow(View v) {}
                    @Override
                    public void onViewDetachedFromWindow(View v) {
                        mTrailingScheduled = false;
                        rootView.removeCallbacks(mTrailingRefPass);
                        rootView.removeCallbacks(mTrailingPass);
                    }
                    @Override
                    public void onGlobalLayout() {
                        if (mInPass || rootView.getVisibility() != View.VISIBLE) return;
                        if (isActivityDecor) {
                            if (hasUntrackedVisibleWindowRoot()) {
                                attachUntrackedWindowRoots();
                            }
                            int structHash = computeDecorStructuralHash(rootView);
                            if (structHash != decorState[1]) {
                                decorState[1] = structHash;
                                decorState[2] = 1;
                                rootView.removeCallbacks(mTrailingRefPass);
                                return;
                            }
                            if (WorkspaceUiController.isCanvasPopupPolishActive()) {
                                long now = System.currentTimeMillis();
                                if (now - mLastRun >= 100L) {
                                    mLastRun = now;
                                    rootView.removeCallbacks(mTrailingRefPass);
                                    mInPass = true;
                                    try {
                                        WorkspaceUiController.optimizeReferenceFloatingWindow(rootView);
                                    } catch (Throwable ignored) {}
                                    finally {
                                        mInPass = false;
                                    }
                                } else {
                                    rootView.removeCallbacks(mTrailingRefPass);
                                    rootView.postDelayed(mTrailingRefPass, 80L);
                                }
                            }
                            return;
                        }
                        int stateHash = WorkspaceUiController.computePopupPreDrawStateHash(rootView);
                        int sizeHash = WorkspaceUiController.computePopupSizeHash(rootView);
                        boolean stateChanged = (stateHash != mLastIdleHash);
                        boolean sizeChanged = (sizeHash != mLastIdleSizeHash);
                        if (stateChanged) {
                            mLastIdleHash = stateHash;
                            mLastIdleSizeHash = sizeHash;
                            mTrailingBurst = 0;
                        } else if (sizeChanged) {
                            mLastIdleSizeHash = sizeHash;
                            long now = System.currentTimeMillis();
                            if (now - mLastSizeRun >= 160L && mTrailingBurst < 2) {
                                mLastSizeRun = now;
                                if (mTrailingScheduled) {
                                    mTrailingScheduled = false;
                                    rootView.removeCallbacks(mTrailingPass);
                                }
                                mInPass = true;
                                try {
                                    boolean changed = applyFullLayoutPass(rootView);
                                    if (!changed) {
                                        mTrailingBurst++;
                                    }
                                } catch (Throwable ignored) {}
                                finally {
                                    mInPass = false;
                                }
                            } else {
                                if (mTrailingScheduled) {
                                    rootView.removeCallbacks(mTrailingPass);
                                }
                                mTrailingScheduled = true;
                                rootView.postDelayed(mTrailingPass, 80L);
                            }
                            return;
                        } else if (mTrailingBurst >= 2) {
                            return;
                        }
                        long now = System.currentTimeMillis();
                        if (now - mLastRun > 40) {
                            mLastRun = now;
                            if (mTrailingScheduled) {
                                mTrailingScheduled = false;
                                rootView.removeCallbacks(mTrailingPass);
                            }
                            mInPass = true;
                            try {
                                boolean changed = applyFullLayoutPass(rootView);
                                if (!changed) {
                                    mTrailingBurst++;
                                }
                            } catch (Throwable ignored) {}
                            finally {
                                mInPass = false;
                            }
                        } else if (!mTrailingScheduled && mTrailingBurst < 2) {
                            mTrailingBurst++;
                            mTrailingScheduled = true;
                            rootView.postDelayed(mTrailingPass, 55L);
                        }
                    }
                }
                LiveGlobalLayoutObserver globalObserver = new LiveGlobalLayoutObserver();
                rootView.getViewTreeObserver().addOnGlobalLayoutListener(globalObserver);
                rootView.addOnAttachStateChangeListener(globalObserver);
            }
        } catch (Throwable ignored) {}
    }

    private static void ensureBitmapResourceIdsResolved() {
        if (sBitmapResIdsResolved || sAppContext == null) {
            return;
        }
        sBitmapResIdsResolved = true;
        try {
            int fb = LibreResourceResolver.resolveId(sAppContext, "drawable", "d3_fb", 0);
            if (fb != 0) sD3FbResId = fb;
            int lr = LibreResourceResolver.resolveId(sAppContext, "drawable", "d3_lr", 0);
            if (lr != 0) sD3LrResId = lr;
            int ud = LibreResourceResolver.resolveId(sAppContext, "drawable", "d3_ud", 0);
            if (ud != 0) sD3UdResId = ud;
            int rlr = LibreResourceResolver.resolveId(sAppContext, "drawable", "d3_rotate_lr", 0);
            if (rlr != 0) sD3RotateLrResId = rlr;
            int rud = LibreResourceResolver.resolveId(sAppContext, "drawable", "d3_rotate_ud", 0);
            if (rud != 0) sD3RotateUdResId = rud;
            int autoSave = LibreResourceResolver.resolveId(sAppContext, "mipmap", "ic_auto_save", 0);
            if (autoSave != 0) sIcAutoSaveResId = autoSave;
            int coverLost = LibreResourceResolver.resolveId(sAppContext, "mipmap", "ic_cover_lost", 0);
            if (coverLost != 0) sIcCoverLostResId = coverLost;
            int autoHint = LibreResourceResolver.resolveId(sAppContext, "drawable", "auto_save_hint", 0);
            if (autoHint != 0) sAutoSaveHintResId = autoHint;
            int draftLost = LibreResourceResolver.resolveId(sAppContext, "drawable", "draft_cover_lost", 0);
            if (draftLost != 0) sDraftCoverLostResId = draftLost;
        } catch (Throwable ignored) {}
    }

    private static boolean isTargetD3ArrowResId(int resId) {
        if (resId == 0) return false;
        ensureBitmapResourceIdsResolved();
        return resId == sD3FbResId
                || resId == sD3LrResId
                || resId == sD3UdResId
                || resId == sD3RotateLrResId
                || resId == sD3RotateUdResId;
    }

    private static int matchDraftPlaceholderKind(int resId) {
        if (resId == 0) return 0;
        ensureBitmapResourceIdsResolved();
        if (resId == sIcAutoSaveResId) return 1;
        if (resId == sIcCoverLostResId) return 2;
        if (resId == sAutoSaveHintResId) return 3;
        if (resId == sDraftCoverLostResId) return 4;
        return 0;
    }

    /**
     * Removes baked Chinese axis labels from 3D arrow bitmaps when a non-Chinese pack is active.
     */
    public static Bitmap translateBitmapById(int resId, Bitmap orig) {
        if (orig == null || orig.isRecycled()) {
            return orig;
        }
        if (!isTargetD3ArrowResId(resId)) {
            return orig;
        }
        if (!sInitialized) {
            ensureInitialized();
        }
        if (!isDynamicI18nEnabled() || isChinesePack()) {
            return orig;
        }
        Bitmap cached = sCleanArrowBitmapCache.get(resId);
        if (cached != null && !cached.isRecycled()) {
            return cached;
        }
        Bitmap cleaned = stripInteriorTextFromWhiteShape(orig);
        if (cleaned != null && cleaned != orig) {
            sCleanArrowBitmapCache.put(resId, cleaned);
            return cleaned;
        }
        return orig;
    }

    /**
     * Wraps draft placeholder/missing-cover Drawables so their caption tracks live language state.
     */
    public static Drawable translateDrawableById(int resId, Drawable orig) {
        if (orig == null) {
            return null;
        }
        int kind = matchDraftPlaceholderKind(resId);
        if (kind == 0) {
            return orig;
        }
        if (orig instanceof LocalizedDraftPlaceholderDrawable) {
            return orig;
        }
        if (!sInitialized) {
            ensureInitialized();
        }
        try {
            Bitmap origBitmap = extractBitmapFromDrawable(orig);
            if (origBitmap == null || origBitmap.isRecycled()) {
                return orig;
            }
            android.content.res.Resources res = sAppContext != null ? sAppContext.getResources() : null;
            if (res == null) {
                return orig;
            }
            boolean isAutoSave = (kind == 1 || kind == 3);
            boolean isNineHint = (kind == 3 || kind == 4);
            return new LocalizedDraftPlaceholderDrawable(res, resId, isAutoSave, isNineHint, origBitmap);
        } catch (Throwable ignored) {
            return orig;
        }
    }

    /**
     * Intercepts android.resource:// image URIs passed to Glide in DraftAdapter/DraftAdapter2.
     */
    public static Object resolveLocalizedDraftModel(Object model) {
        if (!(model instanceof String)) {
            return model;
        }
        String str = (String) model;
        if (!str.startsWith("android.resource://")) {
            return model;
        }
        int slashIdx = str.lastIndexOf('/');
        if (slashIdx < 0 || slashIdx + 1 >= str.length()) {
            return model;
        }
        int resId;
        try {
            resId = Integer.parseInt(str.substring(slashIdx + 1));
        } catch (NumberFormatException ignored) {
            return model;
        }
        int kind = matchDraftPlaceholderKind(resId);
        if (kind == 0) {
            return model;
        }
        if (!sInitialized) {
            ensureInitialized();
        }
        try {
            Context ctx = sAppContext;
            if (ctx == null) {
                return model;
            }
            Drawable baseDrawable = null;
            try {
                baseDrawable = ctx.getDrawable(resId);
            } catch (Throwable ignored) {}
            if (baseDrawable != null) {
                Drawable wrapped = translateDrawableById(resId, baseDrawable);
                if (wrapped instanceof LocalizedDraftPlaceholderDrawable) {
                    return wrapped;
                }
            }
        } catch (Throwable ignored) {}
        return model;
    }

    private static Bitmap extractBitmapFromDrawable(Drawable d) {
        if (d instanceof BitmapDrawable) {
            Bitmap b = ((BitmapDrawable) d).getBitmap();
            if (b != null && !b.isRecycled()) {
                return b;
            }
        }
        int w = d.getIntrinsicWidth();
        int h = d.getIntrinsicHeight();
        if (w <= 0 || h <= 0) {
            return null;
        }
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        d.setBounds(0, 0, w, h);
        d.draw(c);
        return out;
    }

    private static Bitmap stripInteriorTextFromWhiteShape(Bitmap src) {
        try {
            int w = src.getWidth();
            int h = src.getHeight();
            if (w <= 0 || h <= 0) {
                return src;
            }
            int[] pixels = new int[w * h];
            src.getPixels(pixels, 0, w, 0, 0, w, h);
            int total = pixels.length;
            boolean[] visited = new boolean[total];
            int[] queue = new int[total];
            int head = 0;
            int tail = 0;

            for (int x = 0; x < w; x++) {
                int topIdx = x;
                if (!isWhiteBarrierPixel(pixels[topIdx]) && !visited[topIdx]) {
                    visited[topIdx] = true;
                    queue[tail++] = topIdx;
                }
                int botIdx = (h - 1) * w + x;
                if (!isWhiteBarrierPixel(pixels[botIdx]) && !visited[botIdx]) {
                    visited[botIdx] = true;
                    queue[tail++] = botIdx;
                }
            }
            for (int y = 0; y < h; y++) {
                int leftIdx = y * w;
                if (!isWhiteBarrierPixel(pixels[leftIdx]) && !visited[leftIdx]) {
                    visited[leftIdx] = true;
                    queue[tail++] = leftIdx;
                }
                int rightIdx = y * w + (w - 1);
                if (!isWhiteBarrierPixel(pixels[rightIdx]) && !visited[rightIdx]) {
                    visited[rightIdx] = true;
                    queue[tail++] = rightIdx;
                }
            }

            while (head < tail) {
                int idx = queue[head++];
                int x = idx % w;
                int y = idx / w;
                if (x > 0) {
                    int n = idx - 1;
                    if (!visited[n] && !isWhiteBarrierPixel(pixels[n])) {
                        visited[n] = true;
                        queue[tail++] = n;
                    }
                }
                if (x + 1 < w) {
                    int n = idx + 1;
                    if (!visited[n] && !isWhiteBarrierPixel(pixels[n])) {
                        visited[n] = true;
                        queue[tail++] = n;
                    }
                }
                if (y > 0) {
                    int n = idx - w;
                    if (!visited[n] && !isWhiteBarrierPixel(pixels[n])) {
                        visited[n] = true;
                        queue[tail++] = n;
                    }
                }
                if (y + 1 < h) {
                    int n = idx + w;
                    if (!visited[n] && !isWhiteBarrierPixel(pixels[n])) {
                        visited[n] = true;
                        queue[tail++] = n;
                    }
                }
            }

            boolean changed = false;
            for (int i = 0; i < total; i++) {
                if (!visited[i] && pixels[i] != 0xFFFFFFFF) {
                    pixels[i] = 0xFFFFFFFF;
                    changed = true;
                }
            }
            if (!changed) {
                return src;
            }
            Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            out.setDensity(src.getDensity());
            out.setPixels(pixels, 0, w, 0, 0, w, h);
            return out;
        } catch (Throwable ignored) {
            return src;
        }
    }

    private static boolean isWhiteBarrierPixel(int argb) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        return a > 240 && r > 250 && g > 250 && b > 250;
    }

    private static final class LocalizedDraftPlaceholderDrawable extends Drawable {
        private final android.content.res.Resources mResources;
        private final int mResId;
        private final boolean mIsAutoSave;
        private final boolean mIsNineHint;
        private final Bitmap mOrigBitmap;
        private final Paint mBitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        private int mLastGen = -1;
        private boolean mLastDynamicEnabled = false;
        private String mLastLocaleTag = "";
        private int mLastTypefaceId = 0;
        private Bitmap mCachedLocalizedBitmap;
        private final Drawable.ConstantState mConstantState;

        LocalizedDraftPlaceholderDrawable(
                android.content.res.Resources res,
                int resId,
                boolean isAutoSave,
                boolean isNineHint,
                Bitmap origBitmap) {
            this.mResources = res;
            this.mResId = resId;
            this.mIsAutoSave = isAutoSave;
            this.mIsNineHint = isNineHint;
            this.mOrigBitmap = origBitmap;
            this.mConstantState = new PlaceholderConstantState(res, resId, isAutoSave, isNineHint, origBitmap);
        }

        boolean syncWithCurrentPack() {
            boolean active = isDynamicI18nEnabled() && !isChinesePack();
            int gen = sPackGeneration;
            String locTag = getActiveLocaleTag();
            int tfId = UIFontManager.getActiveTypefaceIdentity(sAppContext);
            if (active != mLastDynamicEnabled
                    || (active && (gen != mLastGen || !locTag.equals(mLastLocaleTag) || tfId != mLastTypefaceId))) {
                invalidateSelf();
                return true;
            }
            return false;
        }

        private Bitmap resolveActiveBitmap() {
            boolean active = isDynamicI18nEnabled() && !isChinesePack();
            int gen = sPackGeneration;
            String locTag = getActiveLocaleTag();
            int tfId = UIFontManager.getActiveTypefaceIdentity(sAppContext);
            if (!active) {
                mLastDynamicEnabled = false;
                mLastGen = gen;
                mLastLocaleTag = locTag;
                mLastTypefaceId = tfId;
                return mOrigBitmap;
            }
            if (mCachedLocalizedBitmap != null && !mCachedLocalizedBitmap.isRecycled()
                    && mLastDynamicEnabled
                    && mLastGen == gen
                    && locTag.equals(mLastLocaleTag)
                    && tfId == mLastTypefaceId) {
                return mCachedLocalizedBitmap;
            }
            mCachedLocalizedBitmap = renderLocalizedPlaceholderBitmap();
            mLastDynamicEnabled = true;
            mLastGen = gen;
            mLastLocaleTag = locTag;
            mLastTypefaceId = tfId;
            return mCachedLocalizedBitmap != null ? mCachedLocalizedBitmap : mOrigBitmap;
        }

        private Bitmap renderLocalizedPlaceholderBitmap() {
            try {
                int w = mOrigBitmap.getWidth();
                int h = mOrigBitmap.getHeight();
                if (w <= 0 || h <= 0) {
                    return mOrigBitmap;
                }
                Bitmap out = mOrigBitmap.copy(Bitmap.Config.ARGB_8888, true);
                if (out == null) {
                    return mOrigBitmap;
                }
                out.setDensity(mOrigBitmap.getDensity());
                int cutY = Math.round(h * (mIsNineHint ? 0.70f : 0.58f));
                if (cutY < 0) cutY = 0;
                if (cutY >= h) cutY = h - 1;
                int clearH = h - cutY;
                if (clearH > 0) {
                    int[] transparent = new int[w * clearH];
                    out.setPixels(transparent, 0, w, 0, cutY, w, clearH);
                }
                String key = mIsAutoSave ? "\u81ea\u52a8\u4fdd\u5b58" : "\u5c01\u9762\u4e22\u5931";
                String label = get(key, key);
                if (label != null && !label.isEmpty()) {
                    Locale activeLocale = getActiveLocale();
                    Typeface activeTf = sAppContext != null ? UIFontManager.getActiveTypeface(sAppContext) : null;
                    String wrapped = TypographyNormalizer.wrapForCanvas(label, activeLocale);
                    boolean useRtl = TypographyNormalizer.containsRtlChars(wrapped);

                    TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
                    tp.setColor(mIsNineHint ? 0xFFE6E6E6 : 0xFFC2C2C2);
                    TypographyNormalizer.preparePaintForScript(tp, wrapped, activeLocale, activeTf);

                    // Enforce >= 10% horizontal breathing margin on both left and right sides.
                    int availW = Math.max(1, Math.round(w * 0.80f));
                    int bandH = Math.max(1, clearH - Math.max(2, Math.round(h * 0.02f)));
                    float startSize = h * (mIsNineHint ? 0.165f : 0.205f);
                    float oneLineFloor = Math.max(11f, h * 0.115f);
                    float twoLineFloor = Math.max(8f, h * 0.075f);

                    StaticLayout chosen = null;
                    float sz = startSize;
                    for (int step = 0; step < 8; step++) {
                        tp.setTextSize(sz);
                        StaticLayout sl = TypographyNormalizer.buildStaticLayout(
                                wrapped, tp, availW, Layout.Alignment.ALIGN_CENTER,
                                1.0f, 0f, true, 0, null, useRtl);
                        float fmH = TypographyNormalizer.measureFontMetricsLineHeight(tp);
                        if (sl.getLineCount() == 1
                                && TypographyNormalizer.getMaxLineWidth(sl) <= availW + 0.5f
                                && sl.getHeight() <= bandH
                                && fmH <= bandH + 2f) {
                            chosen = sl;
                            break;
                        }
                        if (sz <= oneLineFloor) break;
                        sz = Math.max(oneLineFloor, sz * 0.92f);
                    }

                    if (chosen == null) {
                        sz = Math.min(startSize * 0.88f, oneLineFloor * 1.15f);
                        for (int step = 0; step < 8; step++) {
                            tp.setTextSize(sz);
                            StaticLayout sl = TypographyNormalizer.buildStaticLayout(
                                    wrapped, tp, availW, Layout.Alignment.ALIGN_CENTER,
                                    0.96f, 0f, true, 0, null, useRtl);
                            float fm2H = 2f * TypographyNormalizer.measureFontMetricsLineHeight(tp);
                            if (sl.getLineCount() <= 2
                                    && TypographyNormalizer.getMaxLineWidth(sl) <= availW + 0.5f
                                    && sl.getHeight() <= bandH
                                    && fm2H <= bandH + 2f) {
                                chosen = sl;
                                break;
                            }
                            if (sz <= twoLineFloor) break;
                            sz = Math.max(twoLineFloor, sz * 0.92f);
                        }
                    }

                    if (chosen == null) {
                        tp.setTextSize(twoLineFloor);
                        boolean allow2 = 2f * TypographyNormalizer.measureFontMetricsLineHeight(tp) <= bandH + 2f;
                        chosen = TypographyNormalizer.buildStaticLayout(
                                wrapped, tp, availW, Layout.Alignment.ALIGN_CENTER,
                                0.96f, 0f, true, allow2 ? 2 : 1, TextUtils.TruncateAt.END, useRtl);
                    }

                    Canvas canvas = new Canvas(out);
                    float dx = (w - availW) * 0.5f;
                    float dy = cutY + Math.max(0f, (clearH - chosen.getHeight()) * 0.44f);
                    canvas.save();
                    canvas.translate(dx, dy);
                    chosen.draw(canvas);
                    canvas.restore();
                }
                return out;
            } catch (Throwable ignored) {
                return mOrigBitmap;
            }
        }

        @Override
        public void draw(Canvas canvas) {
            Bitmap active = resolveActiveBitmap();
            if (active != null && !active.isRecycled()) {
                canvas.drawBitmap(active, null, getBounds(), mBitmapPaint);
            }
        }

        @Override
        public int getIntrinsicWidth() {
            if (mOrigBitmap == null || mOrigBitmap.isRecycled()) return -1;
            return mResources != null
                    ? mOrigBitmap.getScaledWidth(mResources.getDisplayMetrics())
                    : mOrigBitmap.getWidth();
        }

        @Override
        public int getIntrinsicHeight() {
            if (mOrigBitmap == null || mOrigBitmap.isRecycled()) return -1;
            return mResources != null
                    ? mOrigBitmap.getScaledHeight(mResources.getDisplayMetrics())
                    : mOrigBitmap.getHeight();
        }

        @Override
        public void setAlpha(int alpha) {
            if (mBitmapPaint.getAlpha() != alpha) {
                mBitmapPaint.setAlpha(alpha);
                invalidateSelf();
            }
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            mBitmapPaint.setColorFilter(colorFilter);
            invalidateSelf();
        }

        @Override
        @SuppressWarnings("deprecation")
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }

        @Override
        public Drawable.ConstantState getConstantState() {
            return mConstantState;
        }

        private static final class PlaceholderConstantState extends Drawable.ConstantState {
            private final android.content.res.Resources mRes;
            private final int mId;
            private final boolean mAutoSave;
            private final boolean mNineHint;
            private final Bitmap mOrig;

            PlaceholderConstantState(
                    android.content.res.Resources res,
                    int resId,
                    boolean isAutoSave,
                    boolean isNineHint,
                    Bitmap origBitmap) {
                this.mRes = res;
                this.mId = resId;
                this.mAutoSave = isAutoSave;
                this.mNineHint = isNineHint;
                this.mOrig = origBitmap;
            }

            @Override
            public Drawable newDrawable() {
                return new LocalizedDraftPlaceholderDrawable(mRes, mId, mAutoSave, mNineHint, mOrig);
            }

            @Override
            public Drawable newDrawable(android.content.res.Resources res) {
                return new LocalizedDraftPlaceholderDrawable(
                        res != null ? res : mRes, mId, mAutoSave, mNineHint, mOrig);
            }

            @Override
            public int getChangingConfigurations() {
                return 0;
            }
        }
    }
}

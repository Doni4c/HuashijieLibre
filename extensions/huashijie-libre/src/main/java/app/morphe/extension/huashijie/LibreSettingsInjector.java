package app.morphe.extension.huashijie;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.HttpsURLConnection;

/**
 * Huashijie Libre: Native Settings Architecture.
 * Renders a compact, grouped settings screen matching Huashijie Pro's light/dark theme.
 * Supports dynamic patch registration so unchecked patches leave no dead toggles,
 * with Simplified Chinese defaults matching lang/_template_zh.json.
 */
public final class LibreSettingsInjector {
    private static final String NAME_SUB_CONTENT_VIEW = "sub_contentView";
    private static final int FALLBACK_SUB_CONTENT_VIEW = 0x7f090c96;

    private static final String TAG_ENTRY_ROW = "libre_settings_entry";
    private static final String TAG_ENTRY_TITLE = "libre_settings_title_tv";
    private static final String TAG_ENTRY_DESC = "libre_settings_desc";

    private static final String LIBRE_VERSION = "1.0.0-beta.1";
    private static final String BASE_APP_VERSION = "3.4.6";
    private static final String GITHUB_REPO_URL = "https://github.com/Doni4c/HuashijieLibre";
    private static final String GITHUB_RELEASES_URL = GITHUB_REPO_URL + "/releases";
    private static final String GITHUB_LATEST_API_URL =
            "https://api.github.com/repos/Doni4c/HuashijieLibre/releases/latest";
    private static final String GITHUB_RELEASES_API_URL =
            "https://api.github.com/repos/Doni4c/HuashijieLibre/releases?per_page=1";
    private static final String DOCS_README_URL = GITHUB_REPO_URL + "/blob/main/README.md";
    private static final String DOCS_TRANSLATING_URL = GITHUB_REPO_URL + "/blob/main/TRANSLATING.md";

    private static final String DEFAULT_SUBTITLE_ZH = "纯净本地画室，多语言与操作优化";
    private static final String DEFAULT_SYSTEM_FONT_ZH = "系统默认字体";
    private static final String DEFAULT_CUSTOM_FONT_DESC_ZH =
            "读取 HuashijiePro/fonts/ 内的 .ttf 或 .otf 字体文件并即时应用到界面。选回系统默认即可恢复。";

    private static final AtomicBoolean sUpdateCheckInFlight = new AtomicBoolean(false);
    private static final Set<String> sRegisteredToggles = java.util.concurrent.ConcurrentHashMap.newKeySet();

    static {
        registerInstalledToggles();
    }

    private LibreSettingsInjector() {}

    /**
     * Populated at patch time by LibreSettingsPatch via registerToggle calls
     * so build time selections stay synchronized without mutating app lifecycle methods.
     */
    public static void registerInstalledToggles() {}

    /**
     * Registers a LibreConfig toggle key so the settings UI only displays controls
     * for patches actually compiled into the APK.
     */
    public static void registerToggle(String key) {
        if (key != null && !key.trim().isEmpty()) {
            sRegisteredToggles.add(key.trim());
        }
    }

    public static boolean isToggleRegistered(String key) {
        return key != null && sRegisteredToggles.contains(key);
    }

    private static boolean isAnyRegistered(String... keys) {
        for (String k : keys) {
            if (isToggleRegistered(k)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Trampoline hook called from GeneralSettingActivity.initView()V.
     */
    public static void injectGeneralSettings(Activity activity) {
        if (activity == null || activity.isFinishing()) return;

        try {
            LibreConfig.init(activity);
            TranslationEngine.init(activity);

            View found = LibreResourceResolver.findViewByName(
                    activity, NAME_SUB_CONTENT_VIEW, FALLBACK_SUB_CONTENT_VIEW);
            if (!(found instanceof ViewGroup)) return;
            ViewGroup subContent = (ViewGroup) found;

            if (subContent.findViewWithTag(TAG_ENTRY_ROW) != null) {
                refreshEntryRow(activity);
                return;
            }

            LinearLayout row = new LinearLayout(activity);
            row.setTag(TAG_ENTRY_ROW);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            int padV = dpToPx(activity, 15);
            int padH = dpToPx(activity, 16);
            row.setPadding(padH, padV, padH, padV);
            row.setClickable(true);
            row.setFocusable(true);

            LinearLayout textBlock = new LinearLayout(activity);
            textBlock.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams textParams =
                    new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            textBlock.setLayoutParams(textParams);

            TextView titleView = new TextView(activity);
            titleView.setTag(TAG_ENTRY_TITLE);
            titleView.setText(TranslationEngine.get("libre_settings_title", "HuashijieLibre"));
            titleView.setTextSize(16.0f);
            titleView.setTypeface(Typeface.DEFAULT_BOLD);
            titleView.setTextColor(Color.parseColor("#1A73E8"));

            boolean isNight = isNightMode(activity);
            int dividerColor = isNight ? Color.parseColor("#2C2C2E") : Color.parseColor("#EEEEEE");

            TextView descView = new TextView(activity);
            descView.setTag(TAG_ENTRY_DESC);
            descView.setText(TranslationEngine.get("libre_settings_subtitle", DEFAULT_SUBTITLE_ZH));
            descView.setTextSize(12.0f);
            descView.setTextColor(isNight ? Color.parseColor("#8E8E93") : Color.parseColor("#757575"));

            textBlock.addView(titleView);
            textBlock.addView(descView);

            TextView arrowView = new TextView(activity);
            arrowView.setText("›");
            arrowView.setTextSize(22.0f);
            arrowView.setTextColor(isNight ? Color.parseColor("#8E8E93") : Color.parseColor("#9E9E9E"));
            arrowView.setPadding(dpToPx(activity, 8), 0, 0, 0);

            row.addView(textBlock);
            row.addView(arrowView);

            row.setOnClickListener(v -> showLibreNativeSettings(activity));

            subContent.addView(row, 0);

            View divider = new View(activity);
            LinearLayout.LayoutParams divLp =
                    new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(activity, 1));
            divLp.setMargins(dpToPx(activity, 16), 0, dpToPx(activity, 16), 0);
            divider.setLayoutParams(divLp);
            divider.setBackgroundColor(dividerColor);
            subContent.addView(divider, 1);

            if (isToggleRegistered(LibreConfig.KEY_UI_FONT_FILE)) {
                UIFontManager.applyCustomTypeface(row);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static void refreshEntryRow(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.getWindow() == null) return;
        try {
            View decor = activity.getWindow().getDecorView();
            if (decor == null) return;
            View entry = decor.findViewWithTag(TAG_ENTRY_ROW);
            if (entry == null) return;
            View titleView = entry.findViewWithTag(TAG_ENTRY_TITLE);
            if (titleView instanceof TextView) {
                ((TextView) titleView).setText(TranslationEngine.get("libre_settings_title", "HuashijieLibre"));
            }
            View descView = entry.findViewWithTag(TAG_ENTRY_DESC);
            if (descView instanceof TextView) {
                ((TextView) descView).setText(TranslationEngine.get("libre_settings_subtitle", DEFAULT_SUBTITLE_ZH));
            }
        } catch (Throwable ignored) {}
    }

    private static boolean shouldAnimateTransitions(Activity activity, LibreConfig config) {
        if (activity == null || config == null || !config.isEnabled(LibreConfig.KEY_ENABLE_ANIMATIONS, true)) {
            return false;
        }
        try {
            if (Build.VERSION.SDK_INT >= 17) {
                float scale = Settings.Global.getFloat(
                        activity.getContentResolver(),
                        Settings.Global.TRANSITION_ANIMATION_SCALE,
                        1.0f);
                return scale > 0.0f;
            }
        } catch (Throwable ignored) {}
        return true;
    }

    private static int[] resolveActivityTransitionAnimIds(Activity activity) {
        int[] result = new int[]{0, 0, 0, 0};
        if (activity == null) return result;
        try {
            TypedValue tv = new TypedValue();
            if (activity.getTheme().resolveAttribute(android.R.attr.windowAnimationStyle, tv, true)
                    && tv.resourceId != 0) {
                int[] attrs = new int[]{
                    android.R.attr.activityOpenEnterAnimation,
                    android.R.attr.activityOpenExitAnimation,
                    android.R.attr.activityCloseEnterAnimation,
                    android.R.attr.activityCloseExitAnimation
                };
                TypedArray ta = activity.obtainStyledAttributes(tv.resourceId, attrs);
                try {
                    for (int i = 0; i < 4; i++) {
                        result[i] = ta.getResourceId(i, 0);
                    }
                } finally {
                    ta.recycle();
                }
            }
        } catch (Throwable ignored) {}
        return result;
    }

    private static Animation loadTransitionAnim(Activity activity, int resId, boolean isEnter, boolean isOpen) {
        if (activity != null && resId != 0) {
            try {
                Animation anim = AnimationUtils.loadAnimation(activity, resId);
                if (anim != null) {
                    return anim;
                }
            } catch (Throwable ignored) {}
        }
        float fromX;
        float toX;
        if (isOpen) {
            fromX = isEnter ? 1.0f : 0.0f;
            toX = isEnter ? 0.0f : -0.25f;
        } else {
            fromX = isEnter ? -0.25f : 0.0f;
            toX = isEnter ? 0.0f : 1.0f;
        }
        TranslateAnimation anim = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, fromX,
                Animation.RELATIVE_TO_SELF, toX,
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f);
        anim.setDuration(250L);
        anim.setInterpolator(new DecelerateInterpolator(1.6f));
        return anim;
    }

    private static void syncHostSystemBarAppearance(Activity activity, Window dialogWindow,
                                                    boolean isNight, int colorCard, int colorBg) {
        if (activity == null || dialogWindow == null) return;
        try {
            if (Build.VERSION.SDK_INT >= 21) {
                dialogWindow.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                dialogWindow.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                        | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
                        | WindowManager.LayoutParams.FLAG_FULLSCREEN);
                dialogWindow.setStatusBarColor(colorCard);
                dialogWindow.setNavigationBarColor(colorBg);
            }
            View dlgDecor = dialogWindow.getDecorView();
            if (dlgDecor != null) {
                if (Build.VERSION.SDK_INT >= 30) {
                    try {
                        Object dlgCtrl = dlgDecor.getWindowInsetsController();
                        if (dlgCtrl != null) {
                            java.lang.reflect.Method setApp =
                                    dlgCtrl.getClass().getMethod("setSystemBarsAppearance", int.class, int.class);
                            int mask = 8 | 16;
                            setApp.invoke(dlgCtrl, isNight ? 0 : mask, mask);
                        }
                    } catch (Throwable ignored) {}
                }
                int sysUiFlags = dlgDecor.getSystemUiVisibility();
                sysUiFlags &= ~(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
                int lightFlags = 0;
                if (Build.VERSION.SDK_INT >= 23) {
                    lightFlags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                }
                if (Build.VERSION.SDK_INT >= 26) {
                    lightFlags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                }
                if (lightFlags != 0) {
                    sysUiFlags = isNight ? (sysUiFlags & ~lightFlags) : (sysUiFlags | lightFlags);
                }
                dlgDecor.setSystemUiVisibility(sysUiFlags);
            }
        } catch (Throwable ignored) {}
    }

    private static final class AnimatedLibreDialog extends Dialog {
        private final Activity mActivity;
        private final View mRootContainer;
        private final View mHostContent;
        private final boolean mAnimateClose;
        private final int mCloseEnterResId;
        private final int mCloseExitResId;
        private boolean mClosing = false;
        private boolean mImmediate = false;

        AnimatedLibreDialog(Activity activity, View rootContainer, View hostContent,
                            boolean isNight, boolean animateClose, int closeEnterResId, int closeExitResId) {
            super(activity, isNight
                    ? android.R.style.Theme_DeviceDefault_NoActionBar
                    : android.R.style.Theme_DeviceDefault_Light_NoActionBar);
            mActivity = activity;
            mRootContainer = rootContainer;
            mHostContent = hostContent;
            mAnimateClose = animateClose;
            mCloseEnterResId = closeEnterResId;
            mCloseExitResId = closeExitResId;
        }

        void immediateDismiss() {
            mImmediate = true;
            if (mHostContent != null) {
                mHostContent.clearAnimation();
            }
            super.dismiss();
        }

        @Override
        public void onBackPressed() {
            dismiss();
        }

        @Override
        public void dismiss() {
            if (mImmediate || !mAnimateClose || !isShowing() || mActivity == null || mActivity.isFinishing()) {
                mClosing = true;
                if (mHostContent != null) {
                    mHostContent.clearAnimation();
                }
                try {
                    super.dismiss();
                } catch (Throwable ignored) {}
                return;
            }
            if (mClosing) {
                return;
            }
            mClosing = true;

            try {
                Animation closeExit = loadTransitionAnim(mActivity, mCloseExitResId, false, false);
                Animation closeEnter = loadTransitionAnim(mActivity, mCloseEnterResId, true, false);
                closeExit.setFillAfter(true);

                final Runnable finishDismiss = new Runnable() {
                    private boolean mDone = false;
                    @Override
                    public void run() {
                        if (mDone) return;
                        mDone = true;
                        if (mHostContent != null) {
                            mHostContent.clearAnimation();
                        }
                        try {
                            AnimatedLibreDialog.super.dismiss();
                        } catch (Throwable ignored) {}
                    }
                };

                closeExit.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {}
                    @Override
                    public void onAnimationRepeat(Animation animation) {}
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        mRootContainer.post(finishDismiss);
                    }
                });

                if (mHostContent != null && closeEnter != null) {
                    mHostContent.clearAnimation();
                    mHostContent.startAnimation(closeEnter);
                }
                mRootContainer.clearAnimation();
                mRootContainer.startAnimation(closeExit);

                long fallbackDelay = Math.max(120L, Math.min(closeExit.getDuration(), 600L) + 45L);
                mRootContainer.postDelayed(finishDismiss, fallbackDelay);
            } catch (Throwable ignored) {
                if (mHostContent != null) {
                    mHostContent.clearAnimation();
                }
                try {
                    super.dismiss();
                } catch (Throwable ignored2) {}
            }
        }
    }

    /**
     * Displays a native-styled settings screen organized by studio workflow.
     */
    public static void showLibreNativeSettings(Activity activity) {
        showLibreNativeSettings(activity, true, false);
    }

    private static void showLibreNativeSettings(Activity activity, boolean animate, boolean showRestartBanner) {
        if (activity == null || activity.isFinishing()) return;

        try {
            LibreConfig.init(activity);
            TranslationEngine.init(activity);
            final LibreConfig config = LibreConfig.getInstance();
            final boolean isNight = isNightMode(activity);
            final boolean runAnimations = animate && shouldAnimateTransitions(activity, config);
            final int[] animIds = resolveActivityTransitionAnimIds(activity);
            final View hostContent = activity.findViewById(android.R.id.content);

            final int colorBg = isNight ? Color.parseColor("#121212") : Color.parseColor("#F4F5F7");
            final int colorCard = isNight ? Color.parseColor("#1C1C1E") : Color.parseColor("#FFFFFF");
            final int colorCardBorder = isNight ? Color.parseColor("#2A2A2D") : Color.parseColor("#EAECEF");
            final int colorTextPrimary = isNight ? Color.parseColor("#ECECEF") : Color.parseColor("#1C1E21");
            final int colorTextSecondary = isNight ? Color.parseColor("#9A9AA0") : Color.parseColor("#6E7178");
            final int colorHeader = isNight ? Color.parseColor("#64B5F6") : Color.parseColor("#1976D2");
            final int colorDivider = isNight ? Color.parseColor("#2C2C2E") : Color.parseColor("#EFEFF4");
            final int colorAccent = isNight ? Color.parseColor("#4DA3FF") : Color.parseColor("#1A73E8");

            final FrameLayout rootContainer = new FrameLayout(activity);
            rootContainer.setTag("libre_settings_root");
            rootContainer.setBackgroundColor(colorBg);
            rootContainer.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            if (runAnimations) {
                rootContainer.setVisibility(View.INVISIBLE);
            }

            final AnimatedLibreDialog dialog = new AnimatedLibreDialog(
                    activity,
                    rootContainer,
                    hostContent,
                    isNight,
                    shouldAnimateTransitions(activity, config),
                    animIds[2],
                    animIds[3]);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

            final LinearLayout mainLayout = new LinearLayout(activity);
            mainLayout.setOrientation(LinearLayout.VERTICAL);
            mainLayout.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            final LinearLayout topBar = buildTopBar(activity, dialog, isNight, colorCard, colorTextPrimary, colorAccent);
            mainLayout.addView(topBar);

            View topDivider = new View(activity);
            topDivider.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(activity, 1)));
            topDivider.setBackgroundColor(colorDivider);
            mainLayout.addView(topDivider);

            ScrollView scrollView = new ScrollView(activity);
            scrollView.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));
            scrollView.setVerticalScrollBarEnabled(false);

            final LinearLayout contentList = new LinearLayout(activity);
            contentList.setOrientation(LinearLayout.VERTICAL);
            final int padH = dpToPx(activity, 14);
            final int contentPadTop = dpToPx(activity, 8);
            final int contentPadBottom = dpToPx(activity, 72);
            contentList.setPadding(padH, contentPadTop, padH, contentPadBottom);

            final LinearLayout restartBanner = createRestartBanner(activity, isNight);
            if (showRestartBanner) {
                restartBanner.setVisibility(View.VISIBLE);
            }
            final Runnable markRestartNeeded = () -> {
                if (restartBanner.getVisibility() != View.VISIBLE) {
                    restartBanner.setVisibility(View.VISIBLE);
                    restartBanner.animate().alpha(1.0f).setDuration(250).start();
                }
            };

            buildAppearanceSection(activity, contentList, config, dialog, restartBanner, isNight,
                    colorHeader, colorCard, colorCardBorder, colorDivider, colorTextPrimary, colorTextSecondary);

            buildStudioFocusSection(activity, contentList, config, markRestartNeeded,
                    colorHeader, colorCard, colorCardBorder, colorDivider, colorTextPrimary, colorTextSecondary);

            buildCanvasWorkflowSection(activity, contentList, config, markRestartNeeded,
                    colorHeader, colorCard, colorCardBorder, colorDivider, colorTextPrimary, colorTextSecondary);

            buildExportStorageSection(activity, contentList, config,
                    colorHeader, colorCard, colorCardBorder, colorDivider, colorTextPrimary, colorTextSecondary);

            buildPrivacySection(activity, contentList, config, markRestartNeeded,
                    colorHeader, colorCard, colorCardBorder, colorDivider, colorTextPrimary, colorTextSecondary);

            buildProjectSection(activity, contentList, isNight,
                    colorHeader, colorCard, colorCardBorder, colorDivider, colorTextPrimary, colorTextSecondary, colorAccent);

            scrollView.addView(contentList);
            mainLayout.addView(scrollView);

            rootContainer.addView(mainLayout);
            rootContainer.addView(restartBanner);

            if (Build.VERSION.SDK_INT >= 20) {
                final int topBarPadH = dpToPx(activity, 16);
                final int topBarMinH = dpToPx(activity, 54);
                final int bannerPadH = dpToPx(activity, 16);
                final int bannerPadV = dpToPx(activity, 12);
                rootContainer.setOnApplyWindowInsetsListener((v, insets) -> {
                    int insetLeft;
                    int insetTop;
                    int insetRight;
                    int insetBottom;
                    if (Build.VERSION.SDK_INT >= 30) {
                        Insets sys = insets.getInsets(
                                WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                        insetLeft = sys.left;
                        insetTop = sys.top;
                        insetRight = sys.right;
                        insetBottom = sys.bottom;
                    } else {
                        insetLeft = insets.getSystemWindowInsetLeft();
                        insetTop = insets.getSystemWindowInsetTop();
                        insetRight = insets.getSystemWindowInsetRight();
                        insetBottom = insets.getSystemWindowInsetBottom();
                    }
                    topBar.setPadding(topBarPadH, insetTop, topBarPadH, 0);
                    topBar.setMinimumHeight(topBarMinH + insetTop);
                    mainLayout.setPadding(insetLeft, 0, insetRight, 0);
                    contentList.setPadding(padH, contentPadTop, padH, contentPadBottom + insetBottom);
                    restartBanner.setPadding(
                            bannerPadH + insetLeft,
                            bannerPadV,
                            bannerPadH + insetRight,
                            bannerPadV + insetBottom);
                    return insets;
                });
            }

            dialog.setContentView(rootContainer);
            if (dialog.getWindow() != null) {
                Window dlgWin = dialog.getWindow();
                dlgWin.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                if (runAnimations) {
                    dlgWin.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    dlgWin.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                    dlgWin.setDimAmount(0f);
                } else {
                    dlgWin.setBackgroundDrawable(new ColorDrawable(colorBg));
                }
                dlgWin.setWindowAnimations(0);
                syncHostSystemBarAppearance(activity, dlgWin, isNight, colorCard, colorBg);
            }

            if (runAnimations) {
                rootContainer.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        try {
                            ViewTreeObserver vto = rootContainer.getViewTreeObserver();
                            if (vto.isAlive()) {
                                vto.removeOnPreDrawListener(this);
                            }
                            rootContainer.setVisibility(View.VISIBLE);
                            Animation openEnter = loadTransitionAnim(activity, animIds[0], true, true);
                            Animation openExit = loadTransitionAnim(activity, animIds[1], false, true);
                            if (hostContent != null && openExit != null) {
                                openExit.setFillBefore(true);
                                openExit.setAnimationListener(new Animation.AnimationListener() {
                                    @Override
                                    public void onAnimationStart(Animation animation) {}
                                    @Override
                                    public void onAnimationRepeat(Animation animation) {}
                                    @Override
                                    public void onAnimationEnd(Animation animation) {
                                        hostContent.clearAnimation();
                                    }
                                });
                                hostContent.clearAnimation();
                                hostContent.startAnimation(openExit);
                            }
                            if (openEnter != null) {
                                openEnter.setFillBefore(true);
                                rootContainer.clearAnimation();
                                rootContainer.startAnimation(openEnter);
                                return false;
                            }
                        } catch (Throwable ignored) {
                            rootContainer.setVisibility(View.VISIBLE);
                        }
                        return true;
                    }
                });
            }

            dialog.setOnShowListener(d -> {
                if (dialog.getWindow() != null) {
                    syncHostSystemBarAppearance(activity, dialog.getWindow(), isNight, colorCard, colorBg);
                }
                if (Build.VERSION.SDK_INT >= 20) {
                    rootContainer.requestApplyInsets();
                }
                if (!runAnimations && rootContainer.getVisibility() != View.VISIBLE) {
                    rootContainer.setVisibility(View.VISIBLE);
                }
            });
            dialog.setOnDismissListener(d -> {
                if (hostContent != null) {
                    hostContent.clearAnimation();
                }
            });
            dialog.show();

            if (isToggleRegistered(LibreConfig.KEY_UI_FONT_FILE)
                    && dialog.getWindow() != null
                    && dialog.getWindow().getDecorView() != null) {
                UIFontManager.applyCustomTypeface(dialog.getWindow().getDecorView());
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static LinearLayout buildTopBar(Activity activity, Dialog dialog, boolean isNight,
                                            int colorCard, int colorTextPrimary, int colorAccent) {
        LinearLayout topBar = new LinearLayout(activity);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        topBar.setMinimumHeight(dpToPx(activity, 54));
        topBar.setBackgroundColor(colorCard);
        topBar.setPadding(dpToPx(activity, 16), 0, dpToPx(activity, 16), 0);

        TextView btnBack = new TextView(activity);
        btnBack.setText("❮");
        btnBack.setTextSize(18.0f);
        btnBack.setTextColor(colorAccent);
        btnBack.setGravity(Gravity.CENTER);
        btnBack.setPadding(0, dpToPx(activity, 4), dpToPx(activity, 14), dpToPx(activity, 4));
        btnBack.setClickable(true);
        btnBack.setFocusable(true);
        btnBack.setOnClickListener(v -> dialog.dismiss());
        topBar.addView(btnBack);

        TextView tvTitle = new TextView(activity);
        tvTitle.setText(TranslationEngine.get("libre_settings_title", "HuashijieLibre"));
        tvTitle.setTextSize(17.5f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(colorTextPrimary);
        tvTitle.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        topBar.addView(tvTitle);

        TextView tvVerBadge = new TextView(activity);
        tvVerBadge.setText("v" + LIBRE_VERSION);
        tvVerBadge.setTextSize(11.0f);
        tvVerBadge.setTypeface(Typeface.DEFAULT_BOLD);
        tvVerBadge.setTextColor(colorAccent);
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setColor(isNight ? Color.parseColor("#1D334D") : Color.parseColor("#E8F1FD"));
        badgeBg.setCornerRadius(dpToPx(activity, 12));
        tvVerBadge.setBackground(badgeBg);
        tvVerBadge.setPadding(dpToPx(activity, 10), dpToPx(activity, 3), dpToPx(activity, 10), dpToPx(activity, 3));
        topBar.addView(tvVerBadge);

        return topBar;
    }

    private static void reopenSettingsDialog(Activity activity, Dialog dialog, boolean restartVisible) {
        if (activity == null || activity.isFinishing()) return;
        if (dialog != null) {
            if (dialog instanceof AnimatedLibreDialog) {
                if (dialog.isShowing()) {
                    ((AnimatedLibreDialog) dialog).immediateDismiss();
                }
            } else {
                if (dialog.getWindow() != null) {
                    dialog.getWindow().setWindowAnimations(0);
                }
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
            }
        }
        showLibreNativeSettings(activity, false, restartVisible);
    }

    private static void buildAppearanceSection(Activity activity, LinearLayout contentList, LibreConfig config,
                                               Dialog dialog, LinearLayout restartBanner, boolean isNight,
                                               int colorHeader, int colorCard, int colorBorder, int colorDivider,
                                               int colorTextPrimary, int colorTextSecondary) {
        if (!isAnyRegistered(
                LibreConfig.KEY_DYNAMIC_I18N,
                LibreConfig.KEY_LANG_FILE,
                LibreConfig.KEY_UI_FONT_FILE,
                LibreConfig.KEY_TEXT_AUTO_FIT,
                LibreConfig.KEY_CANVAS_POPUP_POLISH,
                LibreConfig.KEY_ENABLE_ANIMATIONS)) {
            return;
        }
        addCategoryHeader(activity, contentList,
                TranslationEngine.get("libre_cat_appearance", "外观与语言"), colorHeader);
        LinearLayout card = createCard(activity, contentList, colorCard, colorBorder);

        if (isToggleRegistered(LibreConfig.KEY_DYNAMIC_I18N)) {
            addSwitchWithDivider(activity, card, colorDivider,
                    TranslationEngine.get("libre_opt_dynamic_i18n", "动态界面翻译"),
                    TranslationEngine.get("libre_desc_dynamic_i18n",
                            "将菜单与工具即时切换为选定语言，并支持加载外置语言包。关闭后恢复原版中文。"),
                    config.isEnabled(LibreConfig.KEY_DYNAMIC_I18N, true),
                    colorTextPrimary, colorTextSecondary,
                    (btn, isChecked) -> {
                        TranslationEngine.setDynamicI18nEnabled(activity, isChecked);
                        refreshEntryRow(activity);
                        reopenSettingsDialog(activity, dialog, restartBanner.getVisibility() == View.VISIBLE);
                    });
        }

        if (isToggleRegistered(LibreConfig.KEY_LANG_FILE)) {
            addSpinnerWithDivider(activity, card, colorDivider, isNight,
                    TranslationEngine.get("libre_opt_active_language", "当前语言包"),
                    null,
                    getAvailableLanguageFiles(activity),
                    config.getSelectedLanguageFile(),
                    colorTextPrimary, colorTextSecondary,
                    selected -> {
                        if (!selected.equals(config.getSelectedLanguageFile())) {
                            TranslationEngine.loadLanguagePack(selected);
                            TranslationEngine.retranslateActiveViews(activity);
                            refreshEntryRow(activity);
                            reopenSettingsDialog(activity, dialog, restartBanner.getVisibility() == View.VISIBLE);
                        }
                    });
        }

        if (isToggleRegistered(LibreConfig.KEY_UI_FONT_FILE)) {
            final String systemDefaultLabel =
                    TranslationEngine.get("libre_font_system_default", DEFAULT_SYSTEM_FONT_ZH);
            String savedFont = config.getSelectedFontFile();
            String displayFont = isSystemDefaultFontValue(savedFont, systemDefaultLabel)
                    ? systemDefaultLabel : savedFont;

            addSpinnerWithDivider(activity, card, colorDivider, isNight,
                    TranslationEngine.get("libre_opt_custom_font", "自定义显示字体"),
                    TranslationEngine.get("libre_desc_custom_font", DEFAULT_CUSTOM_FONT_DESC_ZH),
                    getAvailableFontFiles(activity, systemDefaultLabel),
                    displayFont,
                    colorTextPrimary, colorTextSecondary,
                    selected -> {
                        String targetFont = isSystemDefaultFontValue(selected, systemDefaultLabel) ? "" : selected;
                        if (!targetFont.equals(config.getSelectedFontFile())) {
                            config.setSelectedFontFile(targetFont);
                            applyOrResetFont(activity, dialog, targetFont);
                            if (targetFont.isEmpty() && restartBanner.getVisibility() != View.VISIBLE) {
                                restartBanner.setVisibility(View.VISIBLE);
                                restartBanner.animate().alpha(1.0f).setDuration(250).start();
                            }
                        }
                    });
        }

        if (isToggleRegistered(LibreConfig.KEY_TEXT_AUTO_FIT)) {
            addSwitchWithDivider(activity, card, colorDivider,
                    TranslationEngine.get("libre_opt_text_auto_fit", "窄按钮文字自动适配"),
                    TranslationEngine.get("libre_desc_text_auto_fit",
                            "在较长语言下自动缩放窄按钮文字并对齐排版，避免截断。关闭后恢复原始大小。"),
                    config.isEnabled(LibreConfig.KEY_TEXT_AUTO_FIT, true),
                    colorTextPrimary, colorTextSecondary,
                    (btn, isChecked) -> config.setEnabled(LibreConfig.KEY_TEXT_AUTO_FIT, isChecked));
        }

        if (isToggleRegistered(LibreConfig.KEY_CANVAS_POPUP_POLISH)) {
            addSwitchWithDivider(activity, card, colorDivider,
                    TranslationEngine.get("libre_opt_canvas_popup_polish", "弹窗与工具条几何精修"),
                    TranslationEngine.get("libre_desc_canvas_popup_polish",
                            "微调画布弹窗、参考图窗口与工具条比例，让隐藏项目后排版依然对称。关闭则沿用原布局。"),
                    config.isEnabled(LibreConfig.KEY_CANVAS_POPUP_POLISH, true),
                    colorTextPrimary, colorTextSecondary,
                    (btn, isChecked) -> config.setEnabled(LibreConfig.KEY_CANVAS_POPUP_POLISH, isChecked));
        }

        if (isToggleRegistered(LibreConfig.KEY_ENABLE_ANIMATIONS)) {
            addSwitchWithDivider(activity, card, colorDivider,
                    TranslationEngine.get("libre_opt_enable_animations", "平滑过渡动画"),
                    TranslationEngine.get("libre_desc_enable_animations",
                            "打开设置页面时播放柔和滑动过渡效果。关闭后改为立即响应以追求干脆切换。"),
                    config.isEnabled(LibreConfig.KEY_ENABLE_ANIMATIONS, true),
                    colorTextPrimary, colorTextSecondary,
                    (btn, isChecked) -> config.setEnabled(LibreConfig.KEY_ENABLE_ANIMATIONS, isChecked));
        }
    }

    private static void buildStudioFocusSection(Activity activity, LinearLayout contentList, LibreConfig config,
                                                Runnable markRestartNeeded,
                                                int colorHeader, int colorCard, int colorBorder, int colorDivider,
                                                int colorTextPrimary, int colorTextSecondary) {
        if (!isAnyRegistered(
                LibreConfig.KEY_INSTANT_LAUNCH,
                LibreConfig.KEY_SUPPRESS_SPLASH_AD,
                LibreConfig.KEY_HIDE_COMMUNITY_TABS,
                LibreConfig.KEY_HIDE_DRAWER_VIP,
                LibreConfig.KEY_HIDE_PERSONALIZED_ADS,
                LibreConfig.KEY_HIDE_TOPBAR_CLOUD,
                LibreConfig.KEY_HIDE_DRAFT_CLOUD_TICK,
                LibreConfig.KEY_HIDE_CANVAS_POPUP_CLOUD,
                LibreConfig.KEY_HIDE_CANVAS_EXIT_CLOUD,
                LibreConfig.KEY_HIDE_BAIDU_BACKUP,
                LibreConfig.KEY_OPTIMIZE_CREATE_CANVAS,
                LibreConfig.KEY_HIDE_BRUSH_SHOP,
                LibreConfig.KEY_HIDE_BRUSH_LIBRARY_SHOP,
                LibreConfig.KEY_BLOCK_BRUSH_UPGRADE_TIP)) {
            return;
        }
        addCategoryHeader(activity, contentList,
                TranslationEngine.get("libre_cat_studio", "纯净创作空间"), colorHeader);
        LinearLayout card = createCard(activity, contentList, colorCard, colorBorder);

        if (isToggleRegistered(LibreConfig.KEY_INSTANT_LAUNCH)) {
            addSwitchWithDivider(activity, card, colorDivider,
                    TranslationEngine.get("libre_opt_instant_launch", "极速直达草稿画廊"),
                    TranslationEngine.get("libre_desc_instant_launch",
                            "跳过启动等待，打开应用直接进入草稿画廊。关闭后恢复原版启动画面。"),
                    config.isEnabled(LibreConfig.KEY_INSTANT_LAUNCH, true),
                    colorTextPrimary, colorTextSecondary,
                    (btn, isChecked) -> config.setEnabled(LibreConfig.KEY_INSTANT_LAUNCH, isChecked));
        }

        if (isToggleRegistered(LibreConfig.KEY_SUPPRESS_SPLASH_AD)) {
            addSwitchWithDivider(activity, card, colorDivider,
                    TranslationEngine.get("libre_opt_suppress_splash_ad", "屏蔽开屏广告加载"),
                    TranslationEngine.get("libre_desc_suppress_splash_ad",
                            "阻止启动时请求和展示开屏推广画面。关闭后恢复原版开屏广告逻辑。"),
                    config.isEnabled(LibreConfig.KEY_SUPPRESS_SPLASH_AD, true),
                    colorTextPrimary, colorTextSecondary,
                    (btn, isChecked) -> config.setEnabled(LibreConfig.KEY_SUPPRESS_SPLASH_AD, isChecked));
        }

        if (isToggleRegistered(LibreConfig.KEY_HIDE_COMMUNITY_TABS)) {
            addSwitchWithDivider(activity, card, colorDivider,
                    TranslationEngine.get("libre_opt_hide_community", "隐藏社区与发现页"),
                    TranslationEngine.get("libre_desc_hide_community",
                            "隐藏主界面中的社区和推荐分页，保留纯净本地画室。关闭并重启可恢复。"),
                    config.isEnabled(LibreConfig.KEY_HIDE_COMMUNITY_TABS, true),
                    colorTextPrimary, colorTextSecondary,
                    (btn, isChecked) -> {
                        config.setEnabled(LibreConfig.KEY_HIDE_COMMUNITY_TABS, isChecked);
                        markRestartNeeded.run();
                    });
        }

        if (isToggleRegistered(LibreConfig.KEY_HIDE_DRAWER_VIP)) {
            addSwitchWithDivider(activity, card, colorDivider,
                    TranslationEngine.get("libre_opt_hide_drawer_vip", "隐藏侧边栏会员横幅"),
                    TranslationEngine.get("libre_desc_hide_drawer_vip",
                            "收起首页抽屉里的会员推广卡片，减少误点。关闭可恢复显示。"),
                    config.isEnabled(LibreConfig.KEY_HIDE_DRAWER_VIP, true),
                    colorTextPrimary, colorTextSecondary,
                    (btn, isChecked) -> config.setEnabled(LibreConfig.KEY_HIDE_DRAWER_VIP, isChecked));
        }

        if (isToggleRegistered(LibreConfig.KEY_HIDE_PERSONALIZED_ADS)) {
            addSwitchWithDivider(activity, card, colorDivider,
                    TranslationEngine.get("libre_opt_hide_personalized_ads", "隐藏个性化广告选项"),
                    TranslationEngine.get("libre_desc_hide_personalized_ads",
                            "隐藏常规设置中的广告推荐开关入口。关闭后恢复原本菜单条目。"),
                    config.isEnabled(LibreConfig.KEY_HIDE_PERSONALIZED_ADS, true),
                    colorTextPrimary, colorTextSecondary,
                    (btn, isChecked) -> config.setEnabled(LibreConfig.KEY_HIDE_PERSONALIZED_ADS, isChecked));
        }

        if (isToggleRegistered(LibreConfig.KEY_HIDE_TOPBAR_CLOUD)) {
            addSwitchWithDivider(activity, card, colorDivider,
                    TranslationEngine.get("libre_opt_hide_topbar_cloud", "隐藏画廊顶部云端按钮"),
                    TranslationEngine.get("libre_desc_hide_topbar_cloud",
                            "隐藏画廊顶栏的云同步图标，防止误点触发登录提示。关闭即可恢复。"),
                    config.isEnabled(LibreConfig.KEY_HIDE_TOPBAR_CLOUD, true),
                    colorTextPrimary, colorTextSecondary,
                    (btn, isChecked) -> config.setEnabled(LibreConfig.KEY_HIDE_TOPBAR_CLOUD, isChecked));
        }

        if (isToggleRegistered(LibreConfig.KEY_HIDE_DRAFT_CLOUD_TICK)) {
            addSwitchWithDivider(activity, card, colorDivider,
                    TranslationEngine.get("libre_opt_hide_draft_cloud_tick", "隐藏草稿云同步角标"),
                    TranslationEngine.get("libre_desc_hide_draft_cloud_tick",
                            "去掉草稿封面上的云状态小图标，完整展示缩略图内容。关闭可重新显示。"),
                    config.isEnabled(LibreConfig.KEY_HIDE_DRAFT_CLOUD_TICK, true),
                    colorTextPrimary, colorTextSecondary,
                    (btn, isChecked) -> config.setEnabled(LibreConfig.KEY_HIDE_DRAFT_CLOUD_TICK, isChecked));
        }

        if (isToggleRegistered(LibreConfig.KEY_HIDE_CANVAS_POPUP_CLOUD)) {
            addSwitchWithDivider(activity, card, colorDivider,
                    TranslationEngine.get("libre_opt_hide_canvas_popup_cloud", "隐藏画布设置云同步页"),
                    TranslationEngine.get("libre_desc_hide_canvas_popup_cloud",
                            "在画布右上角设置弹窗中隐藏云存档按钮和云图分页。关闭后恢复显示。"),
                    config.isEnabled(LibreConfig.KEY_HIDE_CANVAS_POPUP_CLOUD, true),
                    colorTextPrimary, colorTextSecondary,
                    (btn, isChecked) -> config.setEnabled(LibreConfig.KEY_HIDE_CANVAS_POPUP_CLOUD, isChecked));
        }

        if (isToggleRegistered(LibreConfig.KEY_HIDE_CANVAS_EXIT_CLOUD)) {
            addSwitchWithDivider(activity, card, colorDivider,
                    TranslationEngine.get("libre_opt_hide_canvas_exit_cloud", "隐藏退出画布云备份项"),
                    TranslationEngine.get("libre_desc_hide_canvas_exit_cloud",
                            "退出画布时隐藏保存到云端的选项，只保留本地保存与丢弃。关闭恢复原状。"),
                    config.isEnabled(LibreConfig.KEY_HIDE_CANVAS_EXIT_CLOUD, true),
                    colorTextPrimary, colorTextSecondary,
                    (btn, isChecked) -> config.setEnabled(LibreConfig.KEY_HIDE_CANVAS_EXIT_CLOUD, isChecked));
        }

        if (isToggleRegistered(LibreConfig.KEY_HIDE_BAIDU_BACKUP)) {
            addSwitchWithDivider(activity, card, colorDivider,
                    TranslationEngine.get("libre_opt_hide_baidu_backup", "隐藏百度网盘备份入口"),
                    TranslationEngine.get("libre_desc_hide_baidu_backup",
                            "隐藏首页侧边栏里的百度网盘备份入口，让抽屉菜单更清爽。关闭并重启可重新显示。"),
                    config.isEnabled(LibreConfig.KEY_HIDE_BAIDU_BACKUP, true),
                    colorTextPrimary, colorTextSecondary,
                    (btn, isChecked) -> {
                        config.setEnabled(LibreConfig.KEY_HIDE_BAIDU_BACKUP, isChecked);
                        markRestartNeeded.run();
                    });
        }

        if (isToggleRegistered(LibreConfig.KEY_OPTIMIZE_CREATE_CANVAS)) {
            addSwitchWithDivider(activity, card, colorDivider,
                    TranslationEngine.get("libre_opt_optimize_create_canvas", "精简新建画布工具栏"),
                    TranslationEngine.get("libre_desc_optimize_create_canvas",
                            "隐藏在线模板入口并将自定义画布、像素与导入图片三项居中排布。关闭可恢复。"),
                    config.isEnabled(LibreConfig.KEY_OPTIMIZE_CREATE_CANVAS, true),
                    colorTextPrimary, colorTextSecondary,
                    (btn, isChecked) -> config.setEnabled(LibreConfig.KEY_OPTIMIZE_CREATE_CANVAS, isChecked));
        }

        if (isToggleRegistered(LibreConfig.KEY_HIDE_BRUSH_SHOP)) {
            addSwitchWithDivider(activity, card, colorDivider,
                    TranslationEngine.get("libre_opt_hide_brush_shop", "隐藏画笔面板商店按钮"),
                    TranslationEngine.get("libre_desc_hide_brush_shop",
                            "隐藏选笔浮窗中的在线商店按钮，保留全部内置与本地画笔。关闭恢复按钮。"),
                    config.isEnabled(LibreConfig.KEY_HIDE_BRUSH_SHOP, true),
                    colorTextPrimary, colorTextSecondary,
                    (btn, isChecked) -> config.setEnabled(LibreConfig.KEY_HIDE_BRUSH_SHOP, isChecked));
        }

        if (isToggleRegistered(LibreConfig.KEY_HIDE_BRUSH_LIBRARY_SHOP)) {
            addSwitchWithDivider(activity, card, colorDivider,
                    TranslationEngine.get("libre_opt_hide_brush_library_shop", "隐藏笔刷库在线商店"),
                    TranslationEngine.get("libre_desc_hide_brush_library_shop",
                            "仅在全屏笔刷库管理页隐藏商店入口。随时关闭即可重新显示。"),
                    config.isEnabled(LibreConfig.KEY_HIDE_BRUSH_LIBRARY_SHOP, true),
                    colorTextPrimary, colorTextSecondary,
                    (btn, isChecked) -> config.setEnabled(LibreConfig.KEY_HIDE_BRUSH_LIBRARY_SHOP, isChecked));
        }

        if (isToggleRegistered(LibreConfig.KEY_BLOCK_BRUSH_UPGRADE_TIP)) {
            addSwitchWithDivider(activity, card, colorDivider,
                    TranslationEngine.get("libre_opt_block_brush_upgrade_tip", "屏蔽画笔升级气泡"),
                    TranslationEngine.get("libre_desc_block_brush_upgrade_tip",
                            "不再在选笔面板顶部弹出笔刷升级横幅。关闭可恢复显示推荐提醒。"),
                    config.isEnabled(LibreConfig.KEY_BLOCK_BRUSH_UPGRADE_TIP, true),
                    colorTextPrimary, colorTextSecondary,
                    (btn, isChecked) -> config.setEnabled(LibreConfig.KEY_BLOCK_BRUSH_UPGRADE_TIP, isChecked));
        }
    }

    private static void buildCanvasWorkflowSection(Activity activity, LinearLayout contentList, LibreConfig config,
                                                   Runnable markRestartNeeded,
                                                   int colorHeader, int colorCard, int colorBorder, int colorDivider,
                                                   int colorTextPrimary, int colorTextSecondary) {
        if (!isAnyRegistered(
                LibreConfig.KEY_TOUCH_OUTSIDE_DISMISS,
                LibreConfig.KEY_FREE_REF_ROTATION,
                LibreConfig.KEY_FLOATING_BRUSH_SLIDERS,
                LibreConfig.KEY_LOCAL_BRUSH_MANAGER,
                LibreConfig.KEY_EXTERNAL_BRUSH_IMPORT,
                LibreConfig.KEY_OFFLINE_USER_STUB)) {
            return;
        }
        addCategoryHeader(activity, contentList,
                TranslationEngine.get("libre_cat_canvas", "画布与笔刷操控"), colorHeader);
        LinearLayout card = createCard(activity, contentList, colorCard, colorBorder);

        if (isToggleRegistered(LibreConfig.KEY_TOUCH_OUTSIDE_DISMISS)) {
            addSwitchWithDivider(activity, card, colorDivider,
                    TranslationEngine.get("libre_opt_touch_outside", "点选区外收起套索"),
                    TranslationEngine.get("libre_desc_touch_outside",
                            "套索框选后轻点空白画布即可快速结束当前工具，长按与双指缩放不受影响。关闭恢复仅点按钮结束。"),
                    config.isEnabled(LibreConfig.KEY_TOUCH_OUTSIDE_DISMISS, true),
                    colorTextPrimary, colorTextSecondary,
                    (btn, isChecked) -> config.setEnabled(LibreConfig.KEY_TOUCH_OUTSIDE_DISMISS, isChecked));
        }

        if (isToggleRegistered(LibreConfig.KEY_FREE_REF_ROTATION)) {
            addSwitchWithDivider(activity, card, colorDivider,
                    TranslationEngine.get("libre_opt_free_ref_rotation", "参考图双指自由旋转"),
                    TranslationEngine.get("libre_desc_free_ref_rotation",
                            "直接在参考图悬浮窗内使用双指自由旋转构图，关闭恢复仅允许平移缩放。"),
                    config.isEnabled(LibreConfig.KEY_FREE_REF_ROTATION, true),
                    colorTextPrimary, colorTextSecondary,
                    (btn, isChecked) -> config.setEnabled(LibreConfig.KEY_FREE_REF_ROTATION, isChecked));
        }

        if (isToggleRegistered(LibreConfig.KEY_FLOATING_BRUSH_SLIDERS)) {
            addSwitchWithDivider(activity, card, colorDivider,
                    TranslationEngine.get("libre_opt_floating_sliders", "悬浮笔刷调节滑块"),
                    TranslationEngine.get("libre_desc_floating_sliders",
                            "通过顶部把手将侧边粗细与透明度滑块拖到画布任意习惯位置。关闭则固定回侧边。"),
                    config.isEnabled(LibreConfig.KEY_FLOATING_BRUSH_SLIDERS, false),
                    colorTextPrimary, colorTextSecondary,
                    (btn, isChecked) -> config.setEnabled(LibreConfig.KEY_FLOATING_BRUSH_SLIDERS, isChecked));
        }

        if (isToggleRegistered(LibreConfig.KEY_LOCAL_BRUSH_MANAGER)) {
            addSwitchWithDivider(activity, card, colorDivider,
                    TranslationEngine.get("libre_opt_local_brush", "本地画笔自由管理"),
                    TranslationEngine.get("libre_desc_local_brush",
                            "免登录自由编辑、重命名和复制自制或导入的笔刷参数。关闭恢复原版归属限制。"),
                    config.isEnabled(LibreConfig.KEY_LOCAL_BRUSH_MANAGER, true),
                    colorTextPrimary, colorTextSecondary,
                    (btn, isChecked) -> config.setEnabled(LibreConfig.KEY_LOCAL_BRUSH_MANAGER, isChecked));
        }

        if (isToggleRegistered(LibreConfig.KEY_EXTERNAL_BRUSH_IMPORT)) {
            addSwitchWithDivider(activity, card, colorDivider,
                    TranslationEngine.get("libre_opt_external_brush_import", "扫描导入本地外部笔刷"),
                    TranslationEngine.get("libre_desc_external_brush_import",
                            "自动读取 HuashijiePro/ASBrush 中的外部笔刷包并保持列表完整。关闭则停止自动扫描。"),
                    config.isEnabled(LibreConfig.KEY_EXTERNAL_BRUSH_IMPORT, true),
                    colorTextPrimary, colorTextSecondary,
                    (btn, isChecked) -> config.setEnabled(LibreConfig.KEY_EXTERNAL_BRUSH_IMPORT, isChecked));
        }

        if (isToggleRegistered(LibreConfig.KEY_OFFLINE_USER_STUB)) {
            addSwitchWithDivider(activity, card, colorDivider,
                    TranslationEngine.get("libre_opt_offline_user", "离线创作者档案"),
                    TranslationEngine.get("libre_desc_offline_user",
                            "在本机建立零联网创作者档案，免登录使用导出 PDF 等本地工具。关闭并重启还原未登录。"),
                    config.isEnabled(LibreConfig.KEY_OFFLINE_USER_STUB, true),
                    colorTextPrimary, colorTextSecondary,
                    (btn, isChecked) -> {
                        config.setEnabled(LibreConfig.KEY_OFFLINE_USER_STUB, isChecked);
                        if (isChecked) {
                            OfflineAccountController.setupOfflineUserStub();
                        } else {
                            markRestartNeeded.run();
                        }
                    });
        }
    }

    private static void buildExportStorageSection(Activity activity, LinearLayout contentList, LibreConfig config,
                                                  int colorHeader, int colorCard, int colorBorder, int colorDivider,
                                                  int colorTextPrimary, int colorTextSecondary) {
        if (!isAnyRegistered(
                LibreConfig.KEY_CLEAN_SPEEDPAINT,
                LibreConfig.KEY_SYSTEM_SHARE,
                LibreConfig.KEY_REPLAY_SYSTEM_SHARE,
                LibreConfig.KEY_EXPORT_SHARE_BUTTON,
                LibreConfig.KEY_HIDE_EXPORT_SOCIAL_ICONS,
                LibreConfig.KEY_HIDE_TIMELAPSE_EXPORT_SOCIAL,
                LibreConfig.KEY_PUBLIC_STORAGE,
                LibreConfig.KEY_CANVAS_HSJ_PATH_ROW)) {
            return;
        }
        addCategoryHeader(activity, contentList,
                TranslationEngine.get("libre_cat_export_files", "导出与本地文件"), colorHeader);
        LinearLayout card = createCard(activity, contentList, colorCard, colorBorder);

        if (isToggleRegistered(LibreConfig.KEY_CLEAN_SPEEDPAINT)) {
            addSwitchWithDivider(activity, card, colorDivider,
                    TranslationEngine.get("libre_opt_clean_speedpaint", "无水印导出延时回放"),
                    TranslationEngine.get("libre_desc_clean_speedpaint",
                            "生成纯净无角标水印的绘画过程视频，方便直接发布与归档。关闭保留右下角水印。"),
                    config.isEnabled(LibreConfig.KEY_CLEAN_SPEEDPAINT, true),
                    colorTextPrimary, colorTextSecondary,
                    (btn, isChecked) -> config.setEnabled(LibreConfig.KEY_CLEAN_SPEEDPAINT, isChecked));
        }

        if (isToggleRegistered(LibreConfig.KEY_SYSTEM_SHARE)) {
            addSwitchWithDivider(activity, card, colorDivider,
                    TranslationEngine.get("libre_opt_system_share", "调用系统原生分享面板"),
                    TranslationEngine.get("libre_desc_system_share",
                            "分享作品成图或 .hsj 工程时改用 Android 系统分享列表。关闭恢复内置社交分享菜单。"),
                    config.isEnabled(LibreConfig.KEY_SYSTEM_SHARE, true),
                    colorTextPrimary, colorTextSecondary,
                    (btn, isChecked) -> config.setEnabled(LibreConfig.KEY_SYSTEM_SHARE, isChecked));
        }

        if (isToggleRegistered(LibreConfig.KEY_REPLAY_SYSTEM_SHARE)) {
            addSwitchWithDivider(activity, card, colorDivider,
                    TranslationEngine.get("libre_opt_replay_system_share", "回放页启用系统分享"),
                    TranslationEngine.get("libre_desc_replay_system_share",
                            "在绘画回放界面直接调用系统分享传出延时视频。关闭恢复内置分享。"),
                    config.isEnabled(LibreConfig.KEY_REPLAY_SYSTEM_SHARE, true),
                    colorTextPrimary, colorTextSecondary,
                    (btn, isChecked) -> config.setEnabled(LibreConfig.KEY_REPLAY_SYSTEM_SHARE, isChecked));
        }

        if (isToggleRegistered(LibreConfig.KEY_EXPORT_SHARE_BUTTON)) {
            addSwitchWithDivider(activity, card, colorDivider,
                    TranslationEngine.get("libre_opt_export_share_button", "在导出面板显示系统分享按钮"),
                    TranslationEngine.get("libre_desc_export_share_button",
                            "在导出弹窗底部的相册和文件旁添加系统分享入口，一次点击即可把作品或延时视频发往外部应用。关闭后仅保留本地保存选项。"),
                    config.isEnabled(LibreConfig.KEY_EXPORT_SHARE_BUTTON, true),
                    colorTextPrimary, colorTextSecondary,
                    (btn, isChecked) -> config.setEnabled(LibreConfig.KEY_EXPORT_SHARE_BUTTON, isChecked));
        }

        if (isToggleRegistered(LibreConfig.KEY_HIDE_EXPORT_SOCIAL_ICONS)) {
            addSwitchWithDivider(activity, card, colorDivider,
                    TranslationEngine.get("libre_opt_hide_domestic_export", "精简导出界面社交图标"),
                    TranslationEngine.get("libre_desc_hide_domestic_export",
                            "隐藏导出弹窗底部 QQ、微信、微博等社交图标，保留相册与文件导出。关闭恢复。"),
                    config.isEnabled(LibreConfig.KEY_HIDE_EXPORT_SOCIAL_ICONS, true),
                    colorTextPrimary, colorTextSecondary,
                    (btn, isChecked) -> config.setEnabled(LibreConfig.KEY_HIDE_EXPORT_SOCIAL_ICONS, isChecked));
        }

        if (isToggleRegistered(LibreConfig.KEY_HIDE_TIMELAPSE_EXPORT_SOCIAL)) {
            addSwitchWithDivider(activity, card, colorDivider,
                    TranslationEngine.get("libre_opt_hide_timelapse_social", "精简延时视频分享入口"),
                    TranslationEngine.get("libre_desc_hide_timelapse_social",
                            "仅隐藏延时视频导出弹窗中的社交图标，保留画质与本地保存。关闭即可还原。"),
                    config.isEnabled(LibreConfig.KEY_HIDE_TIMELAPSE_EXPORT_SOCIAL, true),
                    colorTextPrimary, colorTextSecondary,
                    (btn, isChecked) -> config.setEnabled(LibreConfig.KEY_HIDE_TIMELAPSE_EXPORT_SOCIAL, isChecked));
        }

        if (isToggleRegistered(LibreConfig.KEY_PUBLIC_STORAGE)) {
            addSwitchWithDivider(activity, card, colorDivider,
                    TranslationEngine.get("libre_opt_storage_transparency", "明确展示本地存储路径"),
                    TranslationEngine.get("libre_desc_storage_transparency",
                            "直接提示工程草稿与导出文件的真实公开目录，方便手动备份。关闭恢复简化提示。"),
                    config.isEnabled(LibreConfig.KEY_PUBLIC_STORAGE, true),
                    colorTextPrimary, colorTextSecondary,
                    (btn, isChecked) -> config.setEnabled(LibreConfig.KEY_PUBLIC_STORAGE, isChecked));
        }

        if (isToggleRegistered(LibreConfig.KEY_CANVAS_HSJ_PATH_ROW)) {
            addSwitchWithDivider(activity, card, colorDivider,
                    TranslationEngine.get("libre_opt_canvas_hsj_path", "在画布信息显示 .hsj 目录"),
                    TranslationEngine.get("libre_desc_canvas_hsj_path",
                            "在画布设置的图纸信息区新增一行展示工程存放目录。关闭则隐藏该行。"),
                    config.isEnabled(LibreConfig.KEY_CANVAS_HSJ_PATH_ROW, true),
                    colorTextPrimary, colorTextSecondary,
                    (btn, isChecked) -> config.setEnabled(LibreConfig.KEY_CANVAS_HSJ_PATH_ROW, isChecked));
        }
    }

    private static void buildPrivacySection(Activity activity, LinearLayout contentList, LibreConfig config,
                                            Runnable markRestartNeeded,
                                            int colorHeader, int colorCard, int colorBorder, int colorDivider,
                                            int colorTextPrimary, int colorTextSecondary) {
        if (!isAnyRegistered(
                LibreConfig.KEY_BLOCK_TELEMETRY,
                LibreConfig.KEY_BLOCK_UMENG_SDK,
                LibreConfig.KEY_BLOCK_BUGLY_SDK)) {
            return;
        }
        addCategoryHeader(activity, contentList,
                TranslationEngine.get("libre_cat_privacy", "隐私与静默运行"), colorHeader);
        LinearLayout card = createCard(activity, contentList, colorCard, colorBorder);

        if (isToggleRegistered(LibreConfig.KEY_BLOCK_TELEMETRY)) {
            addSwitchWithDivider(activity, card, colorDivider,
                    TranslationEngine.get("libre_opt_block_telemetry", "拦截内置统计与推广服务"),
                    TranslationEngine.get("libre_desc_block_telemetry",
                            "跳过启动时附带的设备统计与广告唤醒服务，让软件安静离线运行。关闭并重启还原。"),
                    config.isEnabled(LibreConfig.KEY_BLOCK_TELEMETRY, true),
                    colorTextPrimary, colorTextSecondary,
                    (btn, isChecked) -> {
                        config.setEnabled(LibreConfig.KEY_BLOCK_TELEMETRY, isChecked);
                        markRestartNeeded.run();
                    });
        }

        if (isToggleRegistered(LibreConfig.KEY_BLOCK_UMENG_SDK)) {
            addSwitchWithDivider(activity, card, colorDivider,
                    TranslationEngine.get("libre_opt_block_umeng_sdk", "停用友盟分析组件"),
                    TranslationEngine.get("libre_desc_block_umeng_sdk",
                            "阻止友盟后台收集界面点击与操作统计。如需启用原版统计请关闭并重启。"),
                    config.isEnabled(LibreConfig.KEY_BLOCK_UMENG_SDK, true),
                    colorTextPrimary, colorTextSecondary,
                    (btn, isChecked) -> {
                        config.setEnabled(LibreConfig.KEY_BLOCK_UMENG_SDK, isChecked);
                        markRestartNeeded.run();
                    });
        }

        if (isToggleRegistered(LibreConfig.KEY_BLOCK_BUGLY_SDK)) {
            addSwitchWithDivider(activity, card, colorDivider,
                    TranslationEngine.get("libre_opt_block_bugly_sdk", "停用 Bugly 异常上报"),
                    TranslationEngine.get("libre_desc_block_bugly_sdk",
                            "跳过崩溃日志自动联网上传以减少后台连接。关闭并重启可重新启用。"),
                    config.isEnabled(LibreConfig.KEY_BLOCK_BUGLY_SDK, true),
                    colorTextPrimary, colorTextSecondary,
                    (btn, isChecked) -> {
                        config.setEnabled(LibreConfig.KEY_BLOCK_BUGLY_SDK, isChecked);
                        markRestartNeeded.run();
                    });
        }
    }

    private static void buildProjectSection(Activity activity, LinearLayout contentList, boolean isNight,
                                            int colorHeader, int colorCard, int colorBorder, int colorDivider,
                                            int colorTextPrimary, int colorTextSecondary, int colorAccent) {
        addCategoryHeader(activity, contentList,
                TranslationEngine.get("libre_cat_project", "项目、更新与关于"), colorHeader);
        LinearLayout card = createCard(activity, contentList, colorCard, colorBorder);

        String versionFmt = TranslationEngine.get("libre_desc_check_updates",
                "当前版本 %1$s（原版 %2$s），从 GitHub 检查更新。");
        String updateSubtitle;
        try {
            updateSubtitle = String.format(versionFmt, LIBRE_VERSION, BASE_APP_VERSION);
        } catch (Throwable ignored) {
            updateSubtitle = LIBRE_VERSION + " / " + BASE_APP_VERSION;
        }
        addActionRowWithDivider(activity, card, colorDivider,
                TranslationEngine.get("libre_opt_check_updates", "检查更新"),
                updateSubtitle,
                colorTextPrimary, colorTextSecondary, colorAccent,
                () -> checkForUpdatesAsync(activity, isNight));

        addActionRowWithDivider(activity, card, colorDivider,
                TranslationEngine.get("libre_opt_credits", "致谢"),
                TranslationEngine.get("libre_desc_credits",
                        "Doni4c、Morphe 与社区贡献者"),
                colorTextPrimary, colorTextSecondary, colorTextSecondary,
                () -> showCreditsDialog(activity, isNight));

        addActionRowWithDivider(activity, card, colorDivider,
                TranslationEngine.get("libre_opt_docs_license", "使用文档与开源协议"),
                TranslationEngine.get("libre_desc_docs_license",
                        "GPL-3.0 开源协议与使用及翻译说明"),
                colorTextPrimary, colorTextSecondary, colorTextSecondary,
                () -> showDocsAndLicenseDialog(activity, isNight));

        String runtimeSummary = getPrimaryAbi() + " · " + TranslationEngine.getLocalizedStats();
        addActionRowWithDivider(activity, card, colorDivider,
                TranslationEngine.get("libre_opt_copy_diagnostics", "复制系统诊断信息"),
                runtimeSummary,
                colorTextPrimary, colorTextSecondary, colorTextSecondary,
                () -> {
                    ClipboardManager cm = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cm != null) {
                        ClipData clip = ClipData.newPlainText(
                                "HuashijieLibre Diagnostics", buildDiagnosticsReport(activity));
                        cm.setPrimaryClip(clip);
                        Toast.makeText(activity,
                                TranslationEngine.get("libre_toast_copied", "诊断报告已复制到剪贴板"),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private static void checkForUpdatesAsync(final Activity activity, final boolean isNight) {
        if (activity == null || activity.isFinishing()) return;
        if (!sUpdateCheckInFlight.compareAndSet(false, true)) {
            Toast.makeText(activity,
                    TranslationEngine.get("libre_update_checking", "正在检查更新，请稍候..."),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(activity,
                TranslationEngine.get("libre_update_checking", "正在检查更新，请稍候..."),
                Toast.LENGTH_SHORT).show();

        final Handler mainHandler = new Handler(Looper.getMainLooper());
        Thread worker = new Thread(() -> {
            String remoteTag = null;
            String releaseName = null;
            String releaseNotes = null;
            String releaseUrl = GITHUB_RELEASES_URL;
            int httpCode = -1;
            String errorMsg = null;

            ReleaseFetchResult res = null;
            try {
                boolean preferPreReleases = LIBRE_VERSION.contains("-");
                if (preferPreReleases) {
                    res = fetchReleaseJson(GITHUB_RELEASES_API_URL, true);
                } else {
                    res = fetchReleaseJson(GITHUB_LATEST_API_URL, false);
                    if (res.httpCode == 404) {
                        res = fetchReleaseJson(GITHUB_RELEASES_API_URL, true);
                    }
                }
                if (res.json != null) {
                    remoteTag = res.json.optString("tag_name", "").trim();
                    releaseName = res.json.optString("name", "").trim();
                    releaseNotes = res.json.optString("body", "").trim();
                    String html = res.json.optString("html_url", "").trim();
                    if (!html.isEmpty()) {
                        releaseUrl = html;
                    }
                } else {
                    httpCode = res.httpCode;
                    errorMsg = res.errorMsg != null ? res.errorMsg : ("HTTP " + res.httpCode);
                }
            } catch (Throwable t) {
                String m = t.getMessage();
                errorMsg = (m != null && !m.trim().isEmpty()) ? m.trim() : t.getClass().getSimpleName();
            } finally {
                sUpdateCheckInFlight.set(false);
            }

            final String fTag = remoteTag;
            final String fName = releaseName;
            final String fNotes = releaseNotes;
            final String fUrl = releaseUrl;
            final int fHttp = httpCode;
            final String fErr = errorMsg;

            mainHandler.post(() -> {
                if (isActivityDead(activity)) return;
                showUpdateResultDialog(activity, isNight, fTag, fName, fNotes, fUrl, fHttp, fErr);
            });
        }, "libre-update-check");
        worker.setDaemon(true);
        worker.start();
    }

    private static void showUpdateResultDialog(Activity activity, boolean isNight,
                                               String remoteTag, String releaseName, String releaseNotes,
                                               String releaseUrl, int httpCode, String errorMsg) {
        try {
            if (errorMsg != null || remoteTag == null || remoteTag.isEmpty()) {
                String title = TranslationEngine.get("libre_update_error_title", "无法检查更新");
                String detail;
                if (httpCode == 404) {
                    detail = TranslationEngine.get("libre_update_no_release",
                            "仓库中暂未发布公开版本，可前往发布页面查看最新进展。");
                } else {
                    String fmt = TranslationEngine.get("libre_update_error_body",
                            "无法连接到 GitHub（%1$s），请检查网络后重试或直接在浏览器查看。");
                    try {
                        String fallbackErr = TranslationEngine.get("libre_update_network_fallback", "网络异常");
                        detail = String.format(fmt, errorMsg != null ? errorMsg : fallbackErr);
                    } catch (Throwable ignored) {
                        detail = fmt;
                    }
                }
                AlertDialog dlg = newThemedAlertBuilder(activity, isNight)
                        .setTitle(title)
                        .setMessage(detail)
                        .setPositiveButton(TranslationEngine.get("libre_btn_open_releases", "打开发布页面"),
                                (d, w) -> openExternalUrl(activity, GITHUB_RELEASES_URL))
                        .setNegativeButton(TranslationEngine.get("libre_btn_close", "关闭"), null)
                        .show();
                applyFontToDialogIfEnabled(dlg);
                return;
            }

            String normalizedRemote = normalizeVersionTag(remoteTag);
            boolean hasNewer = compareSemVer(normalizedRemote, LIBRE_VERSION) > 0;
            String headerTitle = hasNewer
                    ? TranslationEngine.get("libre_update_available_title", "发现新版本")
                    : TranslationEngine.get("libre_update_latest_title", "已是最新版本");

            StringBuilder msg = new StringBuilder();
            String statusFmt = hasNewer
                    ? TranslationEngine.get("libre_update_available_summary", "当前版本：%1$s\n最新版本：%2$s")
                    : TranslationEngine.get("libre_update_latest_summary", "当前已是最新版本 %1$s（GitHub：%2$s）。");
            try {
                msg.append(String.format(statusFmt, LIBRE_VERSION, remoteTag));
            } catch (Throwable ignored) {
                msg.append(LIBRE_VERSION).append(" / ").append(remoteTag);
            }

            if (releaseName != null && !releaseName.isEmpty() && !releaseName.equalsIgnoreCase(remoteTag)) {
                msg.append("\n\n").append(releaseName);
            }
            if (releaseNotes != null && !releaseNotes.isEmpty()) {
                String cleanNotes = sanitizeDialogMarkdown(releaseNotes);
                if (cleanNotes.length() > 500) {
                    cleanNotes = cleanNotes.substring(0, 497) + "...";
                }
                if (!cleanNotes.isEmpty()) {
                    msg.append("\n\n").append(cleanNotes);
                }
            }

            AlertDialog.Builder builder = newThemedAlertBuilder(activity, isNight)
                    .setTitle(headerTitle)
                    .setMessage(msg.toString());
            if (hasNewer) {
                builder.setPositiveButton(TranslationEngine.get("libre_btn_download_update", "前往下载"),
                        (d, w) -> openExternalUrl(activity, releaseUrl));
                builder.setNegativeButton(TranslationEngine.get("libre_btn_later", "稍后"), null);
            } else {
                builder.setPositiveButton(TranslationEngine.get("libre_btn_close", "关闭"), null);
                builder.setNeutralButton(TranslationEngine.get("libre_btn_open_releases", "打开发布页面"),
                        (d, w) -> openExternalUrl(activity, releaseUrl));
            }
            AlertDialog dlg = builder.show();
            applyFontToDialogIfEnabled(dlg);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static void showCreditsDialog(Activity activity, boolean isNight) {
        if (isActivityDead(activity)) return;
        try {
            String title = TranslationEngine.get("libre_credits_title", "致谢");
            String body = TranslationEngine.get("libre_credits_body",
                    "1. 项目作者与维护：Doni4c（HuashijieLibre）\n"
                            + "2. 应用内修补框架：Morphe\n"
                            + "3. 参与多语言翻译与实机体验测试的社区画师与贡献者");
            AlertDialog dlg = newThemedAlertBuilder(activity, isNight)
                    .setTitle(title)
                    .setMessage(body)
                    .setPositiveButton(TranslationEngine.get("libre_btn_open_github", "访问 GitHub"),
                            (d, w) -> openExternalUrl(activity, GITHUB_REPO_URL))
                    .setNegativeButton(TranslationEngine.get("libre_btn_close", "关闭"), null)
                    .show();
            applyFontToDialogIfEnabled(dlg);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static void showDocsAndLicenseDialog(Activity activity, boolean isNight) {
        if (isActivityDead(activity)) return;
        try {
            String title = TranslationEngine.get("libre_docs_license_title", "使用文档与开源协议");
            String body = TranslationEngine.get("libre_docs_license_body",
                    "开源许可：GNU General Public License v3.0（GPL-3.0）\n"
                            + "你可以自由查阅项目源代码、自行编译修补包，并在同等开源协议下分享修改。\n\n"
                            + "应用内使用的开源组件：\n"
                            + "1. Morphe（应用内扩展与修补框架）\n\n"
                            + "使用文档与本地目录：\n"
                            + "2. 项目主页提供完整使用说明与自制语言包指南\n"
                            + "3. 语言包目录：/sdcard/HuashijiePro/lang/\n"
                            + "4. 字体目录：/sdcard/HuashijiePro/fonts/");
            AlertDialog dlg = newThemedAlertBuilder(activity, isNight)
                    .setTitle(title)
                    .setMessage(body)
                    .setPositiveButton(TranslationEngine.get("libre_btn_open_readme", "使用说明"),
                            (d, w) -> openExternalUrl(activity, DOCS_README_URL))
                    .setNeutralButton(TranslationEngine.get("libre_btn_open_translating", "翻译指南"),
                            (d, w) -> openExternalUrl(activity, DOCS_TRANSLATING_URL))
                    .setNegativeButton(TranslationEngine.get("libre_btn_close", "关闭"), null)
                    .show();
            applyFontToDialogIfEnabled(dlg);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static AlertDialog.Builder newThemedAlertBuilder(Context ctx, boolean isNight) {
        if (Build.VERSION.SDK_INT >= 21) {
            return new AlertDialog.Builder(ctx, isNight
                    ? android.R.style.Theme_DeviceDefault_Dialog_Alert
                    : android.R.style.Theme_DeviceDefault_Light_Dialog_Alert);
        }
        return new AlertDialog.Builder(ctx);
    }

    private static void applyFontToDialogIfEnabled(AlertDialog dlg) {
        if (isToggleRegistered(LibreConfig.KEY_UI_FONT_FILE)
                && dlg != null
                && dlg.getWindow() != null
                && dlg.getWindow().getDecorView() != null) {
            UIFontManager.applyCustomTypeface(dlg.getWindow().getDecorView());
        }
    }

    private static void openExternalUrl(Activity activity, String url) {
        if (activity == null || url == null || url.isEmpty()) return;
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
        } catch (Throwable t) {
            try {
                ClipboardManager cm = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(ClipData.newPlainText("URL", url));
                }
                Toast.makeText(activity, url, Toast.LENGTH_LONG).show();
            } catch (Throwable ignored) {}
        }
    }

    private static boolean isActivityDead(Activity activity) {
        if (activity == null || activity.isFinishing()) return true;
        return Build.VERSION.SDK_INT >= 17 && activity.isDestroyed();
    }

    private static String normalizeVersionTag(String raw) {
        if (raw == null) return "0.0.0";
        String s = raw.trim();
        if (s.startsWith("v") || s.startsWith("V")) {
            s = s.substring(1).trim();
        }
        return s;
    }

    private static final class ReleaseFetchResult {
        final JSONObject json;
        final int httpCode;
        final String errorMsg;

        ReleaseFetchResult(JSONObject json, int httpCode, String errorMsg) {
            this.json = json;
            this.httpCode = httpCode;
            this.errorMsg = errorMsg;
        }
    }

    private static ReleaseFetchResult fetchReleaseJson(String apiUrl, boolean expectArray) {
        HttpsURLConnection conn = null;
        try {
            URL url = new URL(apiUrl);
            conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("Accept", "application/vnd.github+json");
            conn.setRequestProperty("User-Agent", "HuashijieLibre/" + LIBRE_VERSION);

            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                StringBuilder bodySb = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        bodySb.append(line).append('\n');
                    }
                }
                String raw = bodySb.toString().trim();
                if (expectArray) {
                    JSONArray arr = new JSONArray(raw);
                    if (arr.length() == 0) {
                        return new ReleaseFetchResult(null, 404, "HTTP 404");
                    }
                    return new ReleaseFetchResult(arr.getJSONObject(0), code, null);
                }
                return new ReleaseFetchResult(new JSONObject(raw), code, null);
            }
            return new ReleaseFetchResult(null, code, "HTTP " + code);
        } catch (Throwable t) {
            String m = t.getMessage();
            return new ReleaseFetchResult(
                    null,
                    -1,
                    (m != null && !m.trim().isEmpty()) ? m.trim() : t.getClass().getSimpleName());
        } finally {
            if (conn != null) {
                try {
                    conn.disconnect();
                } catch (Throwable ignored) {}
            }
        }
    }

    private static int compareSemVer(String a, String b) {
        String[] splitA = a.split("[-+]", 2);
        String[] splitB = b.split("[-+]", 2);
        String[] partsA = splitA[0].split("\\.");
        String[] partsB = splitB[0].split("\\.");
        int maxLen = Math.max(partsA.length, partsB.length);
        for (int i = 0; i < maxLen; i++) {
            int va = i < partsA.length ? parseIntSafe(partsA[i]) : 0;
            int vb = i < partsB.length ? parseIntSafe(partsB[i]) : 0;
            if (va != vb) {
                return va > vb ? 1 : -1;
            }
        }
        boolean aPre = splitA.length > 1 && !splitA[1].isEmpty();
        boolean bPre = splitB.length > 1 && !splitB[1].isEmpty();
        if (aPre != bPre) {
            return aPre ? -1 : 1;
        }
        if (aPre) {
            String[] preA = splitA[1].split("\\.");
            String[] preB = splitB[1].split("\\.");
            int maxPre = Math.max(preA.length, preB.length);
            for (int i = 0; i < maxPre; i++) {
                if (i >= preA.length) return -1;
                if (i >= preB.length) return 1;
                boolean aNum = isAllDigits(preA[i]);
                boolean bNum = isAllDigits(preB[i]);
                if (aNum && bNum) {
                    int na = parseIntSafe(preA[i]);
                    int nb = parseIntSafe(preB[i]);
                    if (na != nb) return na > nb ? 1 : -1;
                } else if (aNum != bNum) {
                    return aNum ? -1 : 1;
                } else {
                    int cmp = preA[i].compareToIgnoreCase(preB[i]);
                    if (cmp != 0) return cmp > 0 ? 1 : -1;
                }
            }
        }
        return 0;
    }

    private static boolean isAllDigits(String s) {
        if (s == null || s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') return false;
        }
        return true;
    }

    private static int parseIntSafe(String s) {
        if (s == null || s.isEmpty()) return 0;
        int val = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') {
                val = val * 10 + (c - '0');
            } else {
                break;
            }
        }
        return val;
    }

    private static String sanitizeDialogMarkdown(String raw) {
        if (raw == null) return "";
        String normalized = raw.replace("\r\n", "\n").replace('\u2014', ',').replace('\u2013', ',');
        StringBuilder sb = new StringBuilder();
        String[] lines = normalized.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            while (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("#")) {
                if (trimmed.startsWith("#")) {
                    trimmed = trimmed.substring(1).trim();
                } else {
                    trimmed = trimmed.substring(2).trim();
                }
            }
            if (sb.length() > 0) sb.append('\n');
            sb.append(trimmed);
        }
        return sb.toString().trim();
    }

    private static void addSwitchWithDivider(Context ctx, LinearLayout card, int dividerColor,
                                             String title, String desc, boolean checked,
                                             int titleColor, int descColor,
                                             CompoundButton.OnCheckedChangeListener listener) {
        if (card.getChildCount() > 0) {
            addCardDivider(ctx, card, dividerColor);
        }
        addNativeSwitchRow(ctx, card, title, desc, checked, titleColor, descColor, listener);
    }

    private static void addSpinnerWithDivider(Context ctx, LinearLayout card, int dividerColor, boolean isNight,
                                              String title, String desc, List<String> items,
                                              String currentSelection, int titleColor, int descColor,
                                              ValueConsumer<String> onSelected) {
        if (card.getChildCount() > 0) {
            addCardDivider(ctx, card, dividerColor);
        }
        addSpinnerRow(ctx, card, isNight, title, desc, items, currentSelection, titleColor, descColor, onSelected);
    }

    private static void addActionRowWithDivider(Context ctx, LinearLayout card, int dividerColor,
                                                String title, String subtitle,
                                                int titleColor, int subtitleColor, int subtitleAccentOrMuted,
                                                Runnable onClick) {
        if (card.getChildCount() > 0) {
            addCardDivider(ctx, card, dividerColor);
        }
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dpToPx(ctx, 8), 0, dpToPx(ctx, 8));
        row.setClickable(true);
        row.setFocusable(true);

        LinearLayout textLayout = new LinearLayout(ctx);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tLp =
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        tLp.setMarginEnd(dpToPx(ctx, 12));
        textLayout.setLayoutParams(tLp);

        TextView tvTitle = new TextView(ctx);
        tvTitle.setText(title);
        tvTitle.setTextSize(14.5f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(titleColor);
        textLayout.addView(tvTitle);

        if (subtitle != null && !subtitle.isEmpty()) {
            TextView tvSub = new TextView(ctx);
            tvSub.setText(subtitle);
            tvSub.setTextSize(12.0f);
            tvSub.setTextColor(subtitleAccentOrMuted != 0 ? subtitleAccentOrMuted : subtitleColor);
            tvSub.setPadding(0, dpToPx(ctx, 2), 0, 0);
            tvSub.setMaxLines(2);
            tvSub.setSingleLine(false);
            tvSub.setEllipsize(TextUtils.TruncateAt.END);
            textLayout.addView(tvSub);
        }

        row.addView(textLayout);

        TextView arrow = new TextView(ctx);
        arrow.setText("›");
        arrow.setTextSize(18.0f);
        arrow.setTextColor(subtitleColor);
        arrow.setPadding(dpToPx(ctx, 8), 0, dpToPx(ctx, 2), 0);
        row.addView(arrow);

        row.setOnClickListener(v -> {
            if (onClick != null) {
                onClick.run();
            }
        });
        card.addView(row);
    }

    private static void addCategoryHeader(Context ctx, LinearLayout parent, String title, int textColor) {
        TextView tv = new TextView(ctx);
        tv.setText(title);
        tv.setTextSize(12.5f);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextColor(textColor);
        if (Build.VERSION.SDK_INT >= 21) {
            tv.setLetterSpacing(0.02f);
        }
        int padH = dpToPx(ctx, 16);
        tv.setPadding(padH, dpToPx(ctx, 14), padH, dpToPx(ctx, 6));
        parent.addView(tv);
    }

    private static LinearLayout createCard(Context ctx, LinearLayout parent, int cardBgColor, int borderColor) {
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(cardBgColor);
        gd.setCornerRadius(dpToPx(ctx, 14));
        gd.setStroke(dpToPx(ctx, 1), borderColor);
        card.setBackground(gd);
        int padH = dpToPx(ctx, 16);
        int padV = dpToPx(ctx, 6);
        card.setPadding(padH, padV, padH, padV);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dpToPx(ctx, 4), 0, dpToPx(ctx, 4), dpToPx(ctx, 4));
        card.setLayoutParams(lp);

        parent.addView(card);
        return card;
    }

    private static void addCardDivider(Context ctx, LinearLayout card, int dividerColor) {
        View divider = new View(ctx);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dpToPx(ctx, 1)));
        lp.setMargins(0, dpToPx(ctx, 4), 0, dpToPx(ctx, 4));
        divider.setLayoutParams(lp);
        divider.setBackgroundColor(dividerColor);
        card.addView(divider);
    }

    private static void addNativeSwitchRow(Context ctx, LinearLayout card, String title, String desc,
                                           boolean checked, int titleColor, int descColor,
                                           CompoundButton.OnCheckedChangeListener listener) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dpToPx(ctx, 8), 0, dpToPx(ctx, 8));

        LinearLayout textLayout = new LinearLayout(ctx);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams =
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        textParams.setMarginEnd(dpToPx(ctx, 16));
        textLayout.setLayoutParams(textParams);

        TextView tvTitle = new TextView(ctx);
        tvTitle.setText(title);
        tvTitle.setTextSize(14.5f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(titleColor);
        tvTitle.setMaxLines(2);
        tvTitle.setSingleLine(false);
        tvTitle.setEllipsize(TextUtils.TruncateAt.END);

        TextView tvDesc = new TextView(ctx);
        tvDesc.setText(desc);
        tvDesc.setTextSize(12.0f);
        tvDesc.setTextColor(descColor);
        tvDesc.setPadding(0, dpToPx(ctx, 2), 0, 0);
        tvDesc.setLineSpacing(dpToPx(ctx, 2), 1.04f);
        tvDesc.setMaxLines(4);
        tvDesc.setSingleLine(false);
        tvDesc.setEllipsize(TextUtils.TruncateAt.END);

        textLayout.addView(tvTitle);
        textLayout.addView(tvDesc);

        CompoundButton sw;
        try {
            Class<?> clazz = Class.forName("androidx.appcompat.widget.SwitchCompat");
            sw = (CompoundButton) clazz.getConstructor(Context.class).newInstance(ctx);
        } catch (Throwable t) {
            sw = new android.widget.Switch(ctx);
        }
        sw.setChecked(checked);
        sw.setOnCheckedChangeListener(listener);

        row.addView(textLayout);
        row.addView(sw);

        final CompoundButton swFinal = sw;
        row.setClickable(true);
        row.setOnClickListener(v -> swFinal.toggle());

        card.addView(row);
    }

    private static void addSpinnerRow(Context ctx, LinearLayout card, boolean isNight, String title, String desc,
                                      List<String> items, String currentSelection,
                                      int titleColor, int descColor,
                                      ValueConsumer<String> onSelected) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dpToPx(ctx, 8), 0, dpToPx(ctx, 8));
        row.setClickable(true);
        row.setFocusable(true);

        LinearLayout textLayout = new LinearLayout(ctx);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tLp =
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        tLp.setMarginEnd(dpToPx(ctx, 12));
        textLayout.setLayoutParams(tLp);

        TextView tvTitle = new TextView(ctx);
        tvTitle.setText(title);
        tvTitle.setTextSize(14.5f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(titleColor);
        textLayout.addView(tvTitle);

        boolean hasDesc = desc != null && !desc.isEmpty();
        if (hasDesc) {
            TextView tvDesc = new TextView(ctx);
            tvDesc.setText(desc);
            tvDesc.setTextSize(12.0f);
            tvDesc.setTextColor(descColor);
            tvDesc.setPadding(0, dpToPx(ctx, 2), 0, 0);
            tvDesc.setLineSpacing(dpToPx(ctx, 2), 1.04f);
            tvDesc.setMaxLines(4);
            tvDesc.setSingleLine(false);
            tvDesc.setEllipsize(TextUtils.TruncateAt.END);
            textLayout.addView(tvDesc);
        }

        final TextView tvVal = new TextView(ctx);
        tvVal.setText(currentSelection);
        tvVal.setTextSize(12.0f);
        tvVal.setTextColor(isNight ? Color.parseColor("#4DA3FF") : Color.parseColor("#1A73E8"));
        tvVal.setPadding(0, dpToPx(ctx, hasDesc ? 4 : 2), 0, 0);
        textLayout.addView(tvVal);

        row.addView(textLayout);

        TextView arrow = new TextView(ctx);
        arrow.setText("›");
        arrow.setTextSize(18.0f);
        arrow.setTextColor(descColor);
        arrow.setPadding(dpToPx(ctx, 8), 0, dpToPx(ctx, 2), 0);
        row.addView(arrow);

        row.setOnClickListener(v -> {
            try {
                final String[] itemArray = items.toArray(new String[0]);
                int curPos = items.indexOf(tvVal.getText().toString());
                AlertDialog alert = newThemedAlertBuilder(ctx, isNight)
                        .setTitle(title)
                        .setSingleChoiceItems(itemArray, curPos, (dialogInterface, which) -> {
                            dialogInterface.dismiss();
                            if (which >= 0 && which < items.size()) {
                                String selected = items.get(which);
                                tvVal.setText(selected);
                                onSelected.accept(selected);
                            }
                        })
                        .setNegativeButton(TranslationEngine.get("dialog_cancel", "取消"), null)
                        .show();
                applyFontToDialogIfEnabled(alert);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        });

        card.addView(row);
    }

    private static boolean isSystemDefaultFontValue(String val, String systemDefaultLabel) {
        if (val == null || val.trim().isEmpty()) {
            return true;
        }
        String trimmed = val.trim();
        return trimmed.equals(systemDefaultLabel)
                || DEFAULT_SYSTEM_FONT_ZH.equals(trimmed)
                || "系统默认".equals(trimmed)
                || "System Default".equals(trimmed)
                || trimmed.endsWith("(System Default)");
    }

    private static void applyOrResetFont(Activity activity, Dialog dialog, String targetFont) {
        View activityDecor = (activity != null && activity.getWindow() != null)
                ? activity.getWindow().getDecorView()
                : null;
        View dialogDecor = (dialog != null && dialog.getWindow() != null)
                ? dialog.getWindow().getDecorView()
                : null;

        if (targetFont.isEmpty()) {
            if (activityDecor != null) {
                UIFontManager.resetDefaultTypeface(activityDecor);
            }
            if (dialogDecor != null) {
                UIFontManager.resetDefaultTypeface(dialogDecor);
            }
        } else {
            if (activityDecor != null) {
                UIFontManager.applyCustomTypeface(activityDecor);
            }
            if (dialogDecor != null) {
                UIFontManager.applyCustomTypeface(dialogDecor);
            }
        }
    }

    private static LinearLayout createRestartBanner(Activity activity, boolean isNight) {
        LinearLayout banner = new LinearLayout(activity);
        banner.setOrientation(LinearLayout.HORIZONTAL);
        banner.setGravity(Gravity.CENTER_VERTICAL);
        banner.setBackgroundColor(isNight ? Color.parseColor("#2D1F00") : Color.parseColor("#FEF7E0"));
        banner.setPadding(dpToPx(activity, 16), dpToPx(activity, 12), dpToPx(activity, 16), dpToPx(activity, 12));
        banner.setVisibility(View.GONE);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
        );
        banner.setLayoutParams(params);

        TextView tvMsg = new TextView(activity);
        tvMsg.setText(TranslationEngine.get("libre_banner_restart_needed", "部分修改将在重启应用后完全生效"));
        tvMsg.setTextColor(isNight ? Color.parseColor("#FFD54F") : Color.parseColor("#B06000"));
        tvMsg.setTextSize(12.0f);
        LinearLayout.LayoutParams textParams =
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        tvMsg.setLayoutParams(textParams);
        banner.addView(tvMsg);

        Button btnRestart = new Button(activity);
        btnRestart.setText(TranslationEngine.get("libre_btn_restart_now", "立即重启"));
        btnRestart.setTextSize(11.0f);
        btnRestart.setTextColor(Color.WHITE);
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor("#E65100"));
        gd.setCornerRadius(dpToPx(activity, 6));
        btnRestart.setBackground(gd);
        btnRestart.setPadding(dpToPx(activity, 10), 0, dpToPx(activity, 10), 0);
        btnRestart.setOnClickListener(v -> restartApplication(activity));
        banner.addView(btnRestart);

        return banner;
    }

    private static void restartApplication(Activity activity) {
        try {
            LibreConfig.flushSync();
            Intent intent = activity.getPackageManager().getLaunchIntentForPackage(activity.getPackageName());
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                activity.startActivity(intent);
                Runtime.getRuntime().exit(0);
            }
        } catch (Throwable t) {
            activity.finishAffinity();
        }
    }

    private static List<String> getAvailableLanguageFiles(Context ctx) {
        List<String> files = new ArrayList<>();
        files.add("vi.json");
        files.add("en.json");
        try {
            List<File> found = TranslationEngine.getAvailableLanguageFiles(ctx);
            if (found != null) {
                for (File f : found) {
                    if (!files.contains(f.getName())) {
                        files.add(f.getName());
                    }
                }
            }
        } catch (Throwable ignored) {}
        return files;
    }

    private static List<String> getAvailableFontFiles(Context ctx, String systemDefaultLabel) {
        List<String> files = new ArrayList<>();
        files.add(systemDefaultLabel);
        try {
            List<File> found = UIFontManager.getAvailableFontFiles(ctx);
            if (found != null) {
                for (File f : found) {
                    if (!files.contains(f.getName())) {
                        files.add(f.getName());
                    }
                }
            }
        } catch (Throwable ignored) {}
        return files;
    }

    private static String getPrimaryAbi() {
        return (Build.SUPPORTED_ABIS != null && Build.SUPPORTED_ABIS.length > 0)
                ? Build.SUPPORTED_ABIS[0]
                : "unknown";
    }

    private static void appendToggleDiag(StringBuilder sb, Set<String> reported, String key, String label, String value) {
        if (isToggleRegistered(key)) {
            sb.append(label).append(": ").append(value).append('\n');
            reported.add(key);
        }
    }

    private static String buildDiagnosticsReport(Context ctx) {
        LibreConfig cfg = LibreConfig.getInstance();
        StringBuilder sb = new StringBuilder();
        sb.append("=== HUASHIJIE LIBRE DIAGNOSTIC REPORT ===\n");
        sb.append("Package: ").append(ctx != null ? ctx.getPackageName() : "net.huanci.hsjpro").append('\n');
        sb.append("Libre Version: ").append(LIBRE_VERSION).append(" (Base ")
                .append(BASE_APP_VERSION).append(" Code 313)\n");
        sb.append("Device ABI: ").append(getPrimaryAbi()).append('\n');
        sb.append("Android OS: ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");

        List<String> regSnapshot;
        synchronized (sRegisteredToggles) {
            regSnapshot = new ArrayList<>(sRegisteredToggles);
        }
        sb.append("Registered Modules: ").append(regSnapshot.size()).append('\n');

        Set<String> reported = new LinkedHashSet<>();
        if (cfg != null) {
            appendToggleDiag(sb, reported, LibreConfig.KEY_DYNAMIC_I18N, "Dynamic i18n", String.valueOf(cfg.isEnabled(LibreConfig.KEY_DYNAMIC_I18N, true)));
            appendToggleDiag(sb, reported, LibreConfig.KEY_LANG_FILE, "Active Language", cfg.getSelectedLanguageFile());
            String fontVal = cfg.getSelectedFontFile();
            appendToggleDiag(sb, reported, LibreConfig.KEY_UI_FONT_FILE, "Custom UI Font",
                    (fontVal == null || fontVal.trim().isEmpty()) ? "System Default" : fontVal);
            appendToggleDiag(sb, reported, LibreConfig.KEY_INSTANT_LAUNCH, "Instant Launch", String.valueOf(cfg.isEnabled(LibreConfig.KEY_INSTANT_LAUNCH, true)));
            appendToggleDiag(sb, reported, LibreConfig.KEY_SUPPRESS_SPLASH_AD, "Suppress Splash Ad", String.valueOf(cfg.isEnabled(LibreConfig.KEY_SUPPRESS_SPLASH_AD, true)));
            appendToggleDiag(sb, reported, LibreConfig.KEY_HIDE_COMMUNITY_TABS, "Hide Community", String.valueOf(cfg.isEnabled(LibreConfig.KEY_HIDE_COMMUNITY_TABS, true)));
            appendToggleDiag(sb, reported, LibreConfig.KEY_HIDE_TOPBAR_CLOUD, "Hide TopBar Cloud", String.valueOf(cfg.isEnabled(LibreConfig.KEY_HIDE_TOPBAR_CLOUD, true)));
            appendToggleDiag(sb, reported, LibreConfig.KEY_HIDE_CANVAS_POPUP_CLOUD, "Hide Canvas Popup Cloud", String.valueOf(cfg.isEnabled(LibreConfig.KEY_HIDE_CANVAS_POPUP_CLOUD, true)));
            appendToggleDiag(sb, reported, LibreConfig.KEY_HIDE_DRAFT_CLOUD_TICK, "Hide Draft Cloud Tick", String.valueOf(cfg.isEnabled(LibreConfig.KEY_HIDE_DRAFT_CLOUD_TICK, true)));
            appendToggleDiag(sb, reported, LibreConfig.KEY_OPTIMIZE_CREATE_CANVAS, "Optimize Create Canvas", String.valueOf(cfg.isEnabled(LibreConfig.KEY_OPTIMIZE_CREATE_CANVAS, true)));
            appendToggleDiag(sb, reported, LibreConfig.KEY_HIDE_BRUSH_SHOP, "Hide Brush Shop", String.valueOf(cfg.isEnabled(LibreConfig.KEY_HIDE_BRUSH_SHOP, true)));
            appendToggleDiag(sb, reported, LibreConfig.KEY_HIDE_BRUSH_LIBRARY_SHOP, "Hide Brush Library Shop", String.valueOf(cfg.isEnabled(LibreConfig.KEY_HIDE_BRUSH_LIBRARY_SHOP, true)));
            appendToggleDiag(sb, reported, LibreConfig.KEY_BLOCK_BRUSH_UPGRADE_TIP, "Block Brush Upgrade Tip", String.valueOf(cfg.isEnabled(LibreConfig.KEY_BLOCK_BRUSH_UPGRADE_TIP, true)));
            appendToggleDiag(sb, reported, LibreConfig.KEY_HIDE_BAIDU_BACKUP, "Hide Baidu Backup", String.valueOf(cfg.isEnabled(LibreConfig.KEY_HIDE_BAIDU_BACKUP, true)));
            appendToggleDiag(sb, reported, LibreConfig.KEY_HIDE_PERSONALIZED_ADS, "Hide Personalized Ads", String.valueOf(cfg.isEnabled(LibreConfig.KEY_HIDE_PERSONALIZED_ADS, true)));
            appendToggleDiag(sb, reported, LibreConfig.KEY_HIDE_DRAWER_VIP, "Hide Drawer VIP Banner", String.valueOf(cfg.isEnabled(LibreConfig.KEY_HIDE_DRAWER_VIP, true)));
            appendToggleDiag(sb, reported, LibreConfig.KEY_TEXT_AUTO_FIT, "Text Auto Fit", String.valueOf(cfg.isEnabled(LibreConfig.KEY_TEXT_AUTO_FIT, true)));
            appendToggleDiag(sb, reported, LibreConfig.KEY_CANVAS_POPUP_POLISH, "Canvas Popup Polish", String.valueOf(cfg.isEnabled(LibreConfig.KEY_CANVAS_POPUP_POLISH, true)));
            appendToggleDiag(sb, reported, LibreConfig.KEY_ENABLE_ANIMATIONS, "Smooth Animations", String.valueOf(cfg.isEnabled(LibreConfig.KEY_ENABLE_ANIMATIONS, true)));
            appendToggleDiag(sb, reported, LibreConfig.KEY_TOUCH_OUTSIDE_DISMISS, "Touch Outside Dismiss", String.valueOf(cfg.isEnabled(LibreConfig.KEY_TOUCH_OUTSIDE_DISMISS, true)));
            appendToggleDiag(sb, reported, LibreConfig.KEY_FREE_REF_ROTATION, "Free Reference Rotation", String.valueOf(cfg.isEnabled(LibreConfig.KEY_FREE_REF_ROTATION, true)));
            appendToggleDiag(sb, reported, LibreConfig.KEY_FLOATING_BRUSH_SLIDERS, "Floating Brush Sliders", String.valueOf(cfg.isEnabled(LibreConfig.KEY_FLOATING_BRUSH_SLIDERS, false)));
            appendToggleDiag(sb, reported, LibreConfig.KEY_HIDE_CANVAS_EXIT_CLOUD, "Hide Canvas Exit Cloud", String.valueOf(cfg.isEnabled(LibreConfig.KEY_HIDE_CANVAS_EXIT_CLOUD, true)));
            appendToggleDiag(sb, reported, LibreConfig.KEY_CLEAN_SPEEDPAINT, "Clean Speedpaint", String.valueOf(cfg.isEnabled(LibreConfig.KEY_CLEAN_SPEEDPAINT, true)));
            appendToggleDiag(sb, reported, LibreConfig.KEY_LOCAL_BRUSH_MANAGER, "Local Brush Studio", String.valueOf(cfg.isEnabled(LibreConfig.KEY_LOCAL_BRUSH_MANAGER, true)));
            appendToggleDiag(sb, reported, LibreConfig.KEY_EXTERNAL_BRUSH_IMPORT, "External Brush Import", String.valueOf(cfg.isEnabled(LibreConfig.KEY_EXTERNAL_BRUSH_IMPORT, true)));
            appendToggleDiag(sb, reported, LibreConfig.KEY_HIDE_EXPORT_SOCIAL_ICONS, "Hide Export Social Icons", String.valueOf(cfg.isEnabled(LibreConfig.KEY_HIDE_EXPORT_SOCIAL_ICONS, true)));
            appendToggleDiag(sb, reported, LibreConfig.KEY_HIDE_TIMELAPSE_EXPORT_SOCIAL, "Hide Timelapse Export Social", String.valueOf(cfg.isEnabled(LibreConfig.KEY_HIDE_TIMELAPSE_EXPORT_SOCIAL, true)));
            appendToggleDiag(sb, reported, LibreConfig.KEY_SYSTEM_SHARE, "System Share Sheet", String.valueOf(cfg.isEnabled(LibreConfig.KEY_SYSTEM_SHARE, true)));
            appendToggleDiag(sb, reported, LibreConfig.KEY_REPLAY_SYSTEM_SHARE, "Replay System Share Sheet", String.valueOf(cfg.isEnabled(LibreConfig.KEY_REPLAY_SYSTEM_SHARE, true)));
            appendToggleDiag(sb, reported, LibreConfig.KEY_EXPORT_SHARE_BUTTON, "Export Share Button", String.valueOf(cfg.isEnabled(LibreConfig.KEY_EXPORT_SHARE_BUTTON, true)));
            appendToggleDiag(sb, reported, LibreConfig.KEY_PUBLIC_STORAGE, "Storage Transparency", String.valueOf(cfg.isEnabled(LibreConfig.KEY_PUBLIC_STORAGE, true)));
            appendToggleDiag(sb, reported, LibreConfig.KEY_CANVAS_HSJ_PATH_ROW, "Canvas Popup HSJ Path Row", String.valueOf(cfg.isEnabled(LibreConfig.KEY_CANVAS_HSJ_PATH_ROW, true)));
            appendToggleDiag(sb, reported, LibreConfig.KEY_BLOCK_TELEMETRY, "Block Native Telemetry", String.valueOf(cfg.isEnabled(LibreConfig.KEY_BLOCK_TELEMETRY, true)));
            appendToggleDiag(sb, reported, LibreConfig.KEY_BLOCK_UMENG_SDK, "Block Umeng SDK", String.valueOf(cfg.isEnabled(LibreConfig.KEY_BLOCK_UMENG_SDK, true)));
            appendToggleDiag(sb, reported, LibreConfig.KEY_BLOCK_BUGLY_SDK, "Block Bugly SDK", String.valueOf(cfg.isEnabled(LibreConfig.KEY_BLOCK_BUGLY_SDK, true)));
            appendToggleDiag(sb, reported, LibreConfig.KEY_OFFLINE_USER_STUB, "Offline Artist Profile", String.valueOf(cfg.isEnabled(LibreConfig.KEY_OFFLINE_USER_STUB, true)));
        }
        Collections.sort(regSnapshot);
        for (String extraKey : regSnapshot) {
            if (!reported.contains(extraKey)) {
                sb.append("Module ").append(extraKey).append(": registered\n");
            }
        }

        sb.append("Dictionary Stats: ").append(TranslationEngine.getStats()).append('\n');
        sb.append("Timestamp: ").append(System.currentTimeMillis()).append('\n');
        return sb.toString();
    }

    private static boolean isNightMode(Context ctx) {
        return (ctx.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    private static int dpToPx(Context ctx, int dp) {
        return Math.round(dp * ctx.getResources().getDisplayMetrics().density);
    }

    public interface ValueConsumer<T> {
        void accept(T value);
    }
}

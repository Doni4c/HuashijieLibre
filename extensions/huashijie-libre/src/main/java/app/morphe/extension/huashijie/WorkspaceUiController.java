package app.morphe.extension.huashijie;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Environment;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Huashijie Libre: Workspace and Studio UI Controller
 * Manages decluttering of social feeds, clean top bar,
 * cloud tick elimination on drafts, and balanced canvas creation bar.
 */
public final class WorkspaceUiController {
    private static final int SYNTHETIC_HSJ_VAL_BASE = 0x7e098500;
    private static final int SYNTHETIC_HSJ_PATH_VAL_ID = 0x7e098599;

    private static final int TAG_EXIT_DIALOG_HOOKED = 0x7e098601;
    private static final int TAG_SETTING_POPUP_HOOKED = 0x7e098602;
    private static final int TAG_ICON_CENTERED = 0x7e098603;
    private static final int TAG_ICON_ROW_UNIFORMED = 0x7e098604;
    private static final int TAG_EXPORT_ROW_NORMALIZED = 0x7e098605;
    private static final int TAG_THREE_BTN_NORMALIZED = 0x7e098606;
    private static final int TAG_BLEND_ORIG_TEXT_SIZE = 0x7e098618;
    private static final int TAG_BLEND_LAST_FIT_SIG = 0x7e098619;
    private static final int TAG_HOST_POPUP_WINDOW = 0x7e098630;
    private static final int TAG_LAST_POPUP_RESIZE_SIG = 0x7e098631;
    private static final int TAG_CREATE_CANVAS_ORIG_PX = 0x7e098632;
    private static final int TAG_REF_LABEL_ORIG_PX = 0x7e098633;
    private static final int TAG_TOOL_GRID_ORIG_PX = 0x7e098634;
    private static final int TAG_PAPER_NAME_CENTERED = 0x7e098635;
    private static final int TAG_LAST_POPUP_ROOT_H = 0x7e098636;
    private static final int TAG_LAST_TOOL_GRID_SIG = 0x7e098637;
    private static final int TAG_FASTKEY_STYLED = 0x7e098638;
    private static final int TAG_LAST_POPUP_PRE_SIG = 0x7e098639;
    private static final int TAG_LAST_POPUP_DESIRED_H = 0x7e09863a;
    private static final int TAG_SCROLL_RIGID_LOCKED = 0x7e09863b;
    private static final int TAG_SCROLL_CONTENT_FITS = 0x7e09863c;
    private static final int TAG_LAST_AUDIT_HASH = 0x7e09863d;
    private static final int TAG_TOOLBAR_POLISHED = 0x7e09863e;
    private static final int TAG_EXIT_ORIG_PAD_TOP = 0x7e09863f;
    private static final int TAG_ORIG_TOOL_GRID_W = 0x7e098641;
    private static final int TAG_ORIG_TOOL_GRID_RV_PAD_R = 0x7e098650;
    private static final int TAG_REF_LABEL_MEASURE_KEY = 0x7e098651;
    private static final int TAG_REF_LABEL_MEASURE_VAL = 0x7e098652;
    private static final int TAG_REF_LABEL_LAST_PW = 0x7e098653;
    private static final int TAG_REF_LABEL_LAST_KEY = 0x7e098654;
    private static final int TAG_TOOLBAR_ORIG_PX = 0x7e098655;
    private static final int TAG_ORIG_SHAPE_W = 0x7e098656;
    private static final int TAG_ORIG_SHAPE_CONTENT_W = 0x7e098657;
    private static final int TAG_ORIG_SHAPE_STD_W = 0x7e098658;
    private static final int TAG_ORIG_LIUYEBI_W = 0x7e098659;
    private static final int TAG_ORIG_XUANQU_V1_W = 0x7e09865a;
    private static final int TAG_ORIG_XUANQU_V2_W = 0x7e09865b;
    private static final int TAG_TOGGLE_ORIG_FONT_PX = 0x7e09865c;
    private static final int TAG_TOGGLE_PLAN_KEY = 0x7e09865d;
    private static final int TAG_TOGGLE_PLAN = 0x7e09865e;
    private static final int TAG_HSV_PLAN_KEY = 0x7e09865f;
    private static final int TAG_HSV_PLAN = 0x7e098660;
    private static final int TAG_REPLAY_ORIG_TOP_DRAWABLE_BOUNDS = 0x7e098661;
    private static final int SYNTHETIC_SWITCH_ID_BASE = 0x7e098540;

    private static final Pattern PATTERN_DYNAMIC_NUMERIC =
            Pattern.compile("^[+-]?\\d+(?:[.,]\\d+)?\\s*(?:%|px|dp|sp|fps|pt|mm|cm|x|X)?$");

    private static volatile boolean sConstraintFieldsResolved = false;
    private static Class<?> sConstraintLpClass;
    private static Field fStartToStart;
    private static Field fStartToEnd;
    private static Field fEndToStart;
    private static Field fEndToEnd;
    private static Field fLeftToLeft;
    private static Field fLeftToRight;
    private static Field fRightToLeft;
    private static Field fRightToRight;
    private static Field fTopToTop;
    private static Field fTopToBottom;
    private static Field fBottomToTop;
    private static Field fBottomToBottom;
    private static Field fHoriChainStyle;
    private static Field fHoriWeight;

    private static final String[] EXPORT_ROW_TEXT_NAMES = {
        "tv_png", "tv_jpg", "tv_pdf", "tv_psd", "tv_tiff",
        "tv_hsj", "tv_gif", "tv_mp4", "tv_image_sequence_frames"
    };
    private static final int[] EXPORT_ROW_TEXT_FALLBACKS = {
        0x7f090ee1, 0x7f090e85, 0x7f090ed6, 0x7f090eed, 0x7f090f5a,
        0x7f090e79, 0x7f090e68, 0x7f090eb6, 0x7f090e7b
    };

    private static final String[] TOPBAR_CLOUD_NAMES = {
        "iv_cloud", "bg_sync_draft", "iv_sync_draft", "tv_sync_draft", "upload_draft"
    };
    private static final int[] TOPBAR_CLOUD_FALLBACKS = {
        0x7f090689, 0x7f090213, 0x7f09071b, 0x7f090f4b, 0x7f090fa0
    };

    private static final String[] DRAFT_ITEM_CLOUD_NAMES = {
        "draft_cloud_for_pad", "draft_cloud_iv", "draft_cloud_iv2",
        "draft_cloud_pb", "draft_cloud_pb2", "pb_draft_cloud_for_pad",
        "iv_cloud_draft", "tv_sync_draft_state", "tv_sync_draft_state_error"
    };
    private static final int[] DRAFT_ITEM_CLOUD_FALLBACKS = {
        0x7f09047e, 0x7f09047f, 0x7f090480,
        0x7f090481, 0x7f090482, 0x7f090a6d,
        0x7f09068a, 0x7f090f4c, 0x7f090f4d
    };

    private static final String[] DRAFT_INFO_VALUE_NAMES = {
        "create_time", "canvas_size", "total_line", "use_time",
        "tv_color_model", "tv_frame_rate", "tv_time",
        "file_size_tv", "template_author_tv"
    };
    private static final int[] DRAFT_INFO_VALUE_FALLBACKS = {
        0x7f090407, 0x7f090320, 0x7f090d92, 0x7f090faa,
        0x7f090e06, 0x7f090e60, 0x7f090f5c,
        0x7f0904f5, 0x7f090cf8
    };

    private static final String[] DRAFT_INFO_LABEL_NAMES = {
        "tv_color_model_name", "tv_frame_rate_name", "tv_time_name"
    };
    private static final int[] DRAFT_INFO_LABEL_FALLBACKS = {
        0x7f090e07, 0x7f090e61, 0x7f090f5e
    };

    private WorkspaceUiController() {}

    public static boolean hasBoundPopupWindow(View root) {
        return getBoundPopupWindow(root) != null;
    }

    public static int computePopupPreDrawStateHash(View root) {
        if (root == null) return 0;
        int visMask = 0;
        View nsvCanvas = findView(root, "nsv_canvas", 0x7f090a13);
        if (nsvCanvas != null && nsvCanvas.getVisibility() == View.VISIBLE) visMask |= 1;
        View nsvImport = findView(root, "nsv_import", 0x7f090a15);
        if (nsvImport != null && nsvImport.getVisibility() == View.VISIBLE) visMask |= 2;
        View nsvSetting = findView(root, "nsv_setting", 0x7f090a17);
        if (nsvSetting != null && nsvSetting.getVisibility() == View.VISIBLE) visMask |= 4;
        View bgPaper = findView(root, "tv_paper_name", 0x7f090ed3);
        if (bgPaper != null && bgPaper.isShown()) visMask |= 8;
        int w = root.getWidth();
        int hPx = root.getHeight();
        int sizeBucket = (w > 0 && hPx > 0) ? (w > hPx ? 2 : 1) : 0;
        int h = visMask * 31 + sizeBucket;
        if (root instanceof ViewGroup) {
            h = h * 31 + ((ViewGroup) root).getChildCount();
        }
        return h;
    }

    public static int computePopupSizeHash(View root) {
        if (root == null) return 0;
        return root.getWidth() * 31 + root.getHeight();
    }

    private static boolean setSingleLineIfChanged(TextView tv, boolean singleLine) {
        if (tv == null) return false;
        boolean curSingle = tv.getMaxLines() == 1
                && (tv.getTransformationMethod() instanceof android.text.method.SingleLineTransformationMethod);
        if (curSingle != singleLine) {
            tv.setSingleLine(singleLine);
            return true;
        }
        return false;
    }

    private static boolean setMaxLinesIfChanged(TextView tv, int maxLines) {
        if (tv != null && tv.getMaxLines() != maxLines) {
            tv.setMaxLines(maxLines);
            return true;
        }
        return false;
    }

    private static boolean setEllipsizeIfChanged(TextView tv, TextUtils.TruncateAt where) {
        if (tv != null && tv.getEllipsize() != where) {
            tv.setEllipsize(where);
            return true;
        }
        return false;
    }

    private static boolean setGravityIfChanged(TextView tv, int gravity) {
        if (tv == null) return false;
        int normalized = gravity;
        if ((normalized & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) == 0) {
            normalized |= Gravity.START;
        }
        if ((normalized & Gravity.VERTICAL_GRAVITY_MASK) == 0) {
            normalized |= Gravity.TOP;
        }
        if (tv.getGravity() != normalized) {
            tv.setGravity(normalized);
            return true;
        }
        return false;
    }

    private static boolean setTextAlignmentIfChanged(TextView tv, int alignment) {
        if (tv != null && Build.VERSION.SDK_INT >= 17 && tv.getTextAlignment() != alignment) {
            tv.setTextAlignment(alignment);
            return true;
        }
        return false;
    }

    private static int id(View root, String name, int fallback) {
        return LibreResourceResolver.resolveViewId(root != null ? root.getContext() : null, name, fallback);
    }

    private static int id(Context ctx, String name, int fallback) {
        return LibreResourceResolver.resolveViewId(ctx, name, fallback);
    }

    private static View findView(View root, String name, int fallback) {
        return LibreResourceResolver.findViewByName(root, name, fallback);
    }

    private static View findView(Activity activity, String name, int fallback) {
        return LibreResourceResolver.findViewByName(activity, name, fallback);
    }

    private static boolean matchesId(View v, String name, int fallback) {
        if (v == null) return false;
        int resolved = id(v.getContext(), name, fallback);
        return resolved != 0 && v.getId() == resolved;
    }

    private static boolean matchesAnyId(View v, String[] names, int[] fallbacks) {
        if (v == null) return false;
        int vid = v.getId();
        if (vid == View.NO_ID) return false;
        Context ctx = v.getContext();
        for (int i = 0; i < names.length; i++) {
            int resolved = id(ctx, names[i], fallbacks[i]);
            if (resolved != 0 && vid == resolved) {
                return true;
            }
        }
        return false;
    }

    public static boolean isDraftInfoValueView(View v) {
        if (v == null) return false;
        if (v.getId() == SYNTHETIC_HSJ_PATH_VAL_ID) return true;
        return matchesAnyId(v, DRAFT_INFO_VALUE_NAMES, DRAFT_INFO_VALUE_FALLBACKS);
    }

    public static boolean isDraftInfoLabelView(View v) {
        return v != null && matchesAnyId(v, DRAFT_INFO_LABEL_NAMES, DRAFT_INFO_LABEL_FALLBACKS);
    }

    public static boolean isDraftInfoRowChild(View v) {
        if (v == null) return false;
        if (isDraftInfoValueView(v) || isDraftInfoLabelView(v)) return true;
        ViewParent row = v.getParent();
        if (row instanceof View) {
            ViewParent container = ((View) row).getParent();
            if (container instanceof View && matchesId((View) container, "ll_draft_info", 0x7f090828)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSettingPopupRoot(View v) {
        return v != null && Boolean.TRUE.equals(v.getTag(TAG_SETTING_POPUP_HOOKED));
    }

    public static boolean isSettingScrollOrInfoContainer(View v) {
        return matchesId(v, "contentView", 0x7f0903cd)
                || matchesId(v, "scroll_view", 0x7f090b87)
                || matchesId(v, "ll_draft_info", 0x7f090828);
    }

    public static boolean isBlendOrPaperLabel(TextView tv) {
        return matchesId(tv, "tv_blend_mode", 0x7f090dd4)
                || matchesId(tv, "tv_blend", 0x7f090dd3)
                || matchesId(tv, "tv_paper_name", 0x7f090ed3);
    }

    public static boolean isPaperNameView(TextView tv) {
        return matchesId(tv, "tv_paper_name", 0x7f090ed3);
    }

    public static int getFlipButtonKind(View v) {
        if (matchesId(v, "canvas_fan_tv_hori", 0x7f09031c)) return 1;
        if (matchesId(v, "canvas_fan_tv_verti", 0x7f09031d)) return 2;
        return 0;
    }

    /**
     * Checks if hiding Top Bar cloud button is enabled.
     */
    public static boolean isHideTopBarCloudEnabled() {
        return LibreConfig.isFeatureActive(LibreConfig.KEY_HIDE_TOPBAR_CLOUD);
    }

    public static boolean shouldBlockBrushUpgradeTip() {
        return LibreConfig.isFeatureActive(LibreConfig.KEY_BLOCK_BRUSH_UPGRADE_TIP);
    }

    public static void applyBaiduBackupVisibility(Activity activity) {
        if (activity == null || activity.isFinishing()) return;
        try {
            if (!LibreConfig.isFeatureActive(LibreConfig.KEY_HIDE_BAIDU_BACKUP)) {
                return;
            }
            View backupView = findView(activity, "backup_view", 0x7f0900fc);
            if (backupView != null) {
                hideViewCleanly(backupView);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static void applyPersonalizedAdsVisibility(Activity activity) {
        if (activity == null || activity.isFinishing()) return;
        try {
            if (!LibreConfig.isFeatureActive(LibreConfig.KEY_HIDE_PERSONALIZED_ADS)) {
                return;
            }
            View adSettingView = findView(activity, "ad_setting_view", 0x7f090067);
            if (adSettingView != null) {
                hideViewCleanly(adSettingView);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Checks if hiding draft item cloud badge/tick is enabled.
     */
    public static boolean isHideDraftCloudTickEnabled() {
        return LibreConfig.isFeatureActive(LibreConfig.KEY_HIDE_DRAFT_CLOUD_TICK);
    }

    /**
     * Custom visibility setter for cloud button to enforce GONE when enabled.
     */
    public static void hideCloudView(View view, int originalVisibility) {
        if (view == null) return;
        try {
            if (isHideTopBarCloudEnabled()) {
                view.setVisibility(View.GONE);
                view.setEnabled(false);
                view.setClickable(false);
                return;
            }
        } catch (Throwable ignored) {}
        view.setVisibility(originalVisibility);
        view.setEnabled(true);
        view.setClickable(true);
    }

    public static void hideCloudView(View view) {
        hideCloudView(view, View.GONE);
    }

    /**
     * Hides Top Bar cloud sync elements systematically inside a container.
     */
    public static void hideCloudSyncEntry(View rootView) {
        if (rootView == null) return;
        try {
            if (!isHideTopBarCloudEnabled()) return;
            for (int i = 0; i < TOPBAR_CLOUD_NAMES.length; i++) {
                View v = findView(rootView, TOPBAR_CLOUD_NAMES[i], TOPBAR_CLOUD_FALLBACKS[i]);
                if (v != null) {
                    v.setVisibility(View.GONE);
                    v.setEnabled(false);
                    v.setClickable(false);
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static void hideCloudSyncEntry(Activity activity) {
        if (activity == null || activity.isFinishing()) return;
        try {
            if (activity.getWindow() != null && activity.getWindow().getDecorView() != null) {
                hideCloudSyncEntry(activity.getWindow().getDecorView());
            }
        } catch (Throwable ignored) {}
    }

    public static void applyCommunityTabsVisibility(Activity activity) {
        if (activity == null || activity.isFinishing()) return;
        try {
            if (!LibreConfig.isFeatureActive(LibreConfig.KEY_HIDE_COMMUNITY_TABS)) {
                return;
            }
            View foundParent = findView(activity, "found_view_parent", 0x7f09055e);
            if (foundParent != null) {
                foundParent.setVisibility(View.GONE);
            }
            View foundView = findView(activity, "found_view", 0x7f09055d);
            if (foundView != null) {
                foundView.setVisibility(View.GONE);
            }

            View draftParent = findView(activity, "draft_view_parent", 0x7f090492);
            if (draftParent != null) {
                draftParent.setVisibility(View.VISIBLE);
            }
            View createDraft = findView(activity, "create_draft_parent", 0x7f0903fa);
            if (createDraft != null) {
                createDraft.setVisibility(View.VISIBLE);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Hook called at the end of MainActivity.initView()V
     */
    public static void onMainActivityInitView(Activity activity) {
        if (activity == null || activity.isFinishing()) return;

        try {
            LibreConfig.init(activity);
            TranslationEngine.init(activity);
            OfflineAccountController.setupOfflineUserStub();

            View decor = activity.getWindow() != null ? activity.getWindow().getDecorView() : null;

            // 1. Hide community tabs if registered and enabled
            applyCommunityTabsVisibility(activity);

            // 2. Hide Top Bar cloud button if enabled
            if (isHideTopBarCloudEnabled() && decor != null) {
                hideCloudSyncEntry(decor);
            }

            // 3. Hide Baidu NetDisk backup entry and VIP promo card in drawer
            applyBaiduBackupVisibility(activity);
            if (decor != null) {
                hideDrawerVipIfEnabled(decor);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static boolean isHidePersonalizedAdsActive() {
        return LibreConfig.isFeatureActive(LibreConfig.KEY_HIDE_PERSONALIZED_ADS);
    }

    public static boolean isHideDrawerVipActive() {
        return LibreConfig.isFeatureActive(LibreConfig.KEY_HIDE_DRAWER_VIP);
    }

    public static boolean isHideCanvasExitCloudActive() {
        return LibreConfig.isFeatureActive(LibreConfig.KEY_HIDE_CANVAS_EXIT_CLOUD);
    }

    public static boolean isHideCanvasPopupCloudActive() {
        return LibreConfig.isFeatureActive(LibreConfig.KEY_HIDE_CANVAS_POPUP_CLOUD);
    }

    public static void hideDrawerVipIfEnabled(View root) {
        if (root == null) return;
        try {
            if (!isHideDrawerVipActive()) return;
            View vipView = findView(root, "vip_view", 0x7f090ff0);
            if (vipView != null) {
                hideViewCleanly(vipView);
            }
        } catch (Throwable ignored) {}
    }

    public static void hideViewCleanly(View view) {
        if (view == null) return;
        try {
            boolean wasVisible = view.getVisibility() != View.GONE;
            if (wasVisible) {
                view.setVisibility(View.GONE);
            }
            if (view.isEnabled()) {
                view.setEnabled(false);
            }
            if (view.isClickable()) {
                view.setClickable(false);
            }
            if (wasVisible) {
                reflowParentContainer(view);
            }
        } catch (Throwable ignored) {}
    }

    private static void resolveConstraintFields(Class<?> lpClass) {
        if (sConstraintFieldsResolved || lpClass == null) return;
        synchronized (WorkspaceUiController.class) {
            if (sConstraintFieldsResolved) return;
            try {
                Class<?> cur = lpClass;
                while (cur != null && !"androidx.constraintlayout.widget.ConstraintLayout$LayoutParams".equals(cur.getName())) {
                    cur = cur.getSuperclass();
                }
                if (cur != null) {
                    sConstraintLpClass = cur;
                    fStartToStart = cur.getField("startToStart");
                    fStartToEnd = cur.getField("startToEnd");
                    fEndToStart = cur.getField("endToStart");
                    fEndToEnd = cur.getField("endToEnd");
                    fLeftToLeft = cur.getField("leftToLeft");
                    fLeftToRight = cur.getField("leftToRight");
                    fRightToLeft = cur.getField("rightToLeft");
                    fRightToRight = cur.getField("rightToRight");
                    fTopToTop = cur.getField("topToTop");
                    fTopToBottom = cur.getField("topToBottom");
                    fBottomToTop = cur.getField("bottomToTop");
                    fBottomToBottom = cur.getField("bottomToBottom");
                    fHoriChainStyle = cur.getField("horizontalChainStyle");
                    fHoriWeight = cur.getField("horizontalWeight");
                }
            } catch (Throwable ignored) {
            } finally {
                sConstraintFieldsResolved = true;
            }
        }
    }

    private static int getIntField(Field f, Object obj, int fallback) {
        if (f == null || obj == null) return fallback;
        try {
            return f.getInt(obj);
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static boolean setIntField(Field f, Object obj, int val) {
        if (f == null || obj == null) return false;
        try {
            if (f.getInt(obj) != val) {
                f.setInt(obj, val);
                return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public static void reflowParentContainer(View hiddenView) {
        if (hiddenView == null) return;
        ViewParent parent = hiddenView.getParent();
        if (!(parent instanceof ViewGroup)) return;
        ViewGroup vg = (ViewGroup) parent;
        int hiddenId = hiddenView.getId();

        try {
            if (vg instanceof LinearLayout) {
                LinearLayout ll = (LinearLayout) vg;
                if (ll.getOrientation() == LinearLayout.HORIZONTAL) {
                    float totalWeight = 0f;
                    int weightedVisible = 0;
                    int count = ll.getChildCount();
                    for (int i = 0; i < count; i++) {
                        View ch = ll.getChildAt(i);
                        if (ch.getVisibility() != View.GONE && ch.getLayoutParams() instanceof LinearLayout.LayoutParams) {
                            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) ch.getLayoutParams();
                            if (lp.weight > 0f || (lp.width == 0 && ll.getWeightSum() > 0f)) {
                                float w = lp.weight > 0f ? lp.weight : 1.0f;
                                if (lp.weight != w) {
                                    lp.weight = w;
                                    ch.setLayoutParams(lp);
                                }
                                totalWeight += w;
                                weightedVisible++;
                            }
                        }
                    }
                    if (weightedVisible > 0 && Math.abs(ll.getWeightSum() - totalWeight) > 0.01f) {
                        ll.setWeightSum(totalWeight);
                    }
                }
            } else if ("androidx.constraintlayout.widget.ConstraintLayout".equals(vg.getClass().getName()) && hiddenId != View.NO_ID) {
                ViewGroup.LayoutParams rawHiddenLp = hiddenView.getLayoutParams();
                if (rawHiddenLp != null) {
                    resolveConstraintFields(rawHiddenLp.getClass());
                }
                if (sConstraintLpClass != null && sConstraintLpClass.isInstance(rawHiddenLp)) {
                    int hStartToStart = getIntField(fStartToStart, rawHiddenLp, -1);
                    int hStartToEnd = getIntField(fStartToEnd, rawHiddenLp, -1);
                    int hEndToStart = getIntField(fEndToStart, rawHiddenLp, -1);
                    int hEndToEnd = getIntField(fEndToEnd, rawHiddenLp, -1);
                    int hTopToBottom = getIntField(fTopToBottom, rawHiddenLp, -1);
                    int hTopToTop = getIntField(fTopToTop, rawHiddenLp, -1);
                    int hBottomToTop = getIntField(fBottomToTop, rawHiddenLp, -1);
                    int hBottomToBottom = getIntField(fBottomToBottom, rawHiddenLp, -1);

                    int count = vg.getChildCount();
                    for (int i = 0; i < count; i++) {
                        View sibling = vg.getChildAt(i);
                        if (sibling == hiddenView || sibling.getVisibility() == View.GONE) continue;
                        ViewGroup.LayoutParams sLp = sibling.getLayoutParams();
                        if (!sConstraintLpClass.isInstance(sLp)) continue;
                        boolean changed = false;

                        if (getIntField(fEndToStart, sLp, -1) == hiddenId) {
                            changed |= setIntField(fEndToStart, sLp, hEndToStart != hiddenId ? hEndToStart : -1);
                            if (hEndToEnd >= 0) changed |= setIntField(fEndToEnd, sLp, hEndToEnd);
                        }
                        if (getIntField(fRightToLeft, sLp, -1) == hiddenId) {
                            changed |= setIntField(fRightToLeft, sLp, -1);
                            if (hEndToEnd == 0) changed |= setIntField(fRightToRight, sLp, 0);
                        }
                        if (getIntField(fStartToEnd, sLp, -1) == hiddenId) {
                            changed |= setIntField(fStartToEnd, sLp, hStartToEnd != hiddenId ? hStartToEnd : -1);
                            if (hStartToStart >= 0) changed |= setIntField(fStartToStart, sLp, hStartToStart);
                        }
                        if (getIntField(fLeftToRight, sLp, -1) == hiddenId) {
                            changed |= setIntField(fLeftToRight, sLp, -1);
                            if (hStartToStart == 0) changed |= setIntField(fLeftToLeft, sLp, 0);
                        }
                        if (getIntField(fTopToBottom, sLp, -1) == hiddenId) {
                            changed |= setIntField(fTopToBottom, sLp, hTopToBottom != hiddenId ? hTopToBottom : -1);
                            if (hTopToTop >= 0) changed |= setIntField(fTopToTop, sLp, hTopToTop);
                        }
                        if (getIntField(fBottomToTop, sLp, -1) == hiddenId) {
                            changed |= setIntField(fBottomToTop, sLp, hBottomToTop != hiddenId ? hBottomToTop : -1);
                            if (hBottomToBottom >= 0) changed |= setIntField(fBottomToBottom, sLp, hBottomToBottom);
                        }
                        if (changed) {
                            sibling.setLayoutParams(sLp);
                        }
                    }
                }
            } else if (vg instanceof RelativeLayout && hiddenId != View.NO_ID && Build.VERSION.SDK_INT >= 17) {
                RelativeLayout.LayoutParams hLp = hiddenView.getLayoutParams() instanceof RelativeLayout.LayoutParams
                        ? (RelativeLayout.LayoutParams) hiddenView.getLayoutParams() : null;
                int[] hRules = hLp != null ? hLp.getRules() : null;
                int count = vg.getChildCount();
                for (int i = 0; i < count; i++) {
                    View sibling = vg.getChildAt(i);
                    if (sibling == hiddenView || sibling.getVisibility() == View.GONE) continue;
                    if (!(sibling.getLayoutParams() instanceof RelativeLayout.LayoutParams)) continue;
                    RelativeLayout.LayoutParams sLp = (RelativeLayout.LayoutParams) sibling.getLayoutParams();
                    int[] sRules = sLp.getRules();
                    boolean changed = false;
                    int[] relVerbs = new int[]{
                        RelativeLayout.LEFT_OF, RelativeLayout.RIGHT_OF,
                        RelativeLayout.START_OF, RelativeLayout.END_OF,
                        RelativeLayout.ABOVE, RelativeLayout.BELOW
                    };
                    for (int verb : relVerbs) {
                        if (sRules[verb] == hiddenId) {
                            sLp.removeRule(verb);
                            if (hRules != null && hRules[verb] != 0) {
                                sLp.addRule(verb, hRules[verb]);
                            }
                            changed = true;
                        }
                    }
                    if (changed) {
                        sibling.setLayoutParams(sLp);
                    }
                }
            }

            if (!(vg instanceof ScrollView) && !(vg instanceof HorizontalScrollView)
                    && vg.getId() != android.R.id.content
                    && !vg.getClass().getName().contains("RecyclerView")
                    && !vg.getClass().getName().contains("NestedScrollView")
                    && !vg.getClass().getName().contains("FrameLayout")) {
                boolean anyVisible = false;
                for (int i = 0; i < vg.getChildCount(); i++) {
                    if (vg.getChildAt(i).getVisibility() != View.GONE) {
                        anyVisible = true;
                        break;
                    }
                }
                if (!anyVisible && vg.getVisibility() != View.GONE) {
                    vg.setVisibility(View.GONE);
                }
            }
        } catch (Throwable ignored) {}
    }

    private static volatile Field sItemViewField = null;

    /**
     * Hook called at the end of DraftAdapter.onBindViewHolder or DraftAdapter2.onBindViewHolder.
     * Enforces GONE on cloud status icons/progress indicators on artwork cards.
     *
     * @param holder RecyclerView ViewHolder instance (or View)
     */
    public static void handleDraftItemBinding(Object holder) {
        if (holder == null) return;
        try {
            if (!isHideDraftCloudTickEnabled()) return;

            View item = null;
            if (holder instanceof View) {
                item = (View) holder;
            } else {
                if (sItemViewField == null) {
                    try {
                        sItemViewField = holder.getClass().getField("itemView");
                    } catch (NoSuchFieldException ignored) {
                        sItemViewField = holder.getClass().getDeclaredField("itemView");
                        sItemViewField.setAccessible(true);
                    }
                }
                Object itemViewObj = sItemViewField.get(holder);
                if (itemViewObj instanceof View) {
                    item = (View) itemViewObj;
                }
            }

            if (item == null) return;

            for (int i = 0; i < DRAFT_ITEM_CLOUD_NAMES.length; i++) {
                View v = findView(item, DRAFT_ITEM_CLOUD_NAMES[i], DRAFT_ITEM_CLOUD_FALLBACKS[i]);
                if (v != null && v.getVisibility() != View.GONE) {
                    v.setVisibility(View.GONE);
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Hook called at the end of CreateCanvasActivity.initView()V.
     * If pure_studio_optimize_create_canvas is active:
     * (a) Hides unavailable online Templates button.
     * (b) Dynamically balances the 3 remaining tools (Custom, Animation, Open Picture)
     *     with equal 1/3 weight and center alignment across the entire display.
     *
     * @param activity CreateCanvasActivity instance
     */
    private static void fixCreateCanvasTopBar(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.getWindow() == null) return;
        fixCreateCanvasTopBar(activity.getWindow().getDecorView());
    }

    public static void fixCreateCanvasTopBar(View root) {
        if (root == null) return;
        try {
            android.content.Context ctx = root.getContext();
            if (ctx == null) return;
            if (!TranslationEngine.resolveHostActivityName(root).endsWith("CreateCanvasActivity")) {
                return;
            }
            android.content.res.Resources res = root.getResources();
            float density = res.getDisplayMetrics().density;
            int idTitle = LibreResourceResolver.resolveViewId(ctx, "tv_title", 0);
            int idLabLayout = LibreResourceResolver.resolveViewId(ctx, "laborat_layout", 0);
            int idLabIv = LibreResourceResolver.resolveViewId(ctx, "laborat_iv", 0);
            int idLabTv = LibreResourceResolver.resolveViewId(ctx, "laborat_tv", 0);
            if (idLabLayout == 0 && idLabTv == 0) return;

            View labLayout = idLabLayout != 0 ? root.findViewById(idLabLayout) : null;
            if (labLayout == null) return;
            ImageView labIv = idLabIv != 0 ? root.findViewById(idLabIv) : null;
            TextView labTv = idLabTv != 0 ? root.findViewById(idLabTv) : null;
            TextView tvTitle = idTitle != 0 ? root.findViewById(idTitle) : null;

            if (labLayout != null) {
                int padH = Math.round(4 * density);
                if (labLayout.getPaddingLeft() != padH || labLayout.getPaddingRight() != padH
                        || labLayout.getPaddingTop() != 0 || labLayout.getPaddingBottom() != 0) {
                    labLayout.setPadding(padH, 0, padH, 0);
                }
                if (labLayout instanceof LinearLayout) {
                    LinearLayout ll = (LinearLayout) labLayout;
                    ll.setOrientation(LinearLayout.HORIZONTAL);
                    ll.setBaselineAligned(false);
                    ll.setGravity(Gravity.CENTER_VERTICAL);
                }
                if (labLayout.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) labLayout.getLayoutParams();
                    int endMargin = Math.round(8 * density);
                    boolean changed = false;
                    if (mlp.rightMargin > endMargin) {
                        mlp.rightMargin = endMargin;
                        changed = true;
                    }
                    if (Build.VERSION.SDK_INT >= 17 && mlp.getMarginEnd() > endMargin) {
                        mlp.setMarginEnd(endMargin);
                        changed = true;
                    }
                    if (changed) labLayout.setLayoutParams(mlp);
                }
            }
            if (labIv != null) {
                int iconPx = Math.round(17 * density);
                ViewGroup.LayoutParams rawIvLp = labIv.getLayoutParams();
                if (rawIvLp != null) {
                    boolean changed = false;
                    if (rawIvLp.width > iconPx || rawIvLp.width <= 0) {
                        rawIvLp.width = iconPx;
                        changed = true;
                    }
                    if (rawIvLp.height > iconPx || rawIvLp.height <= 0) {
                        rawIvLp.height = iconPx;
                        changed = true;
                    }
                    if (rawIvLp instanceof LinearLayout.LayoutParams) {
                        LinearLayout.LayoutParams llp = (LinearLayout.LayoutParams) rawIvLp;
                        if (llp.gravity != Gravity.CENTER_VERTICAL) {
                            llp.gravity = Gravity.CENTER_VERTICAL;
                            changed = true;
                        }
                        int gapPx = Math.round(4 * density);
                        if (llp.rightMargin != gapPx) {
                            llp.rightMargin = gapPx;
                            changed = true;
                        }
                        if (Build.VERSION.SDK_INT >= 17 && llp.getMarginEnd() != gapPx) {
                            llp.setMarginEnd(gapPx);
                            changed = true;
                        }
                    } else if (rawIvLp instanceof RelativeLayout.LayoutParams) {
                        ((RelativeLayout.LayoutParams) rawIvLp).addRule(RelativeLayout.CENTER_VERTICAL);
                        changed = true;
                    }
                    if (changed) labIv.setLayoutParams(rawIvLp);
                }
                labIv.setScaleType(ImageView.ScaleType.FIT_CENTER);
                labIv.setBaselineAlignBottom(false);
            }
            if (labTv != null) {
                Object origLabObj = labTv.getTag(TAG_CREATE_CANVAS_ORIG_PX);
                float origLabPx;
                if (origLabObj instanceof Float) {
                    origLabPx = (Float) origLabObj;
                } else {
                    origLabPx = labTv.getTextSize();
                    labTv.setTag(TAG_CREATE_CANVAS_ORIG_PX, origLabPx);
                }
                int maxLabW = Math.round(104 * density);
                setSingleLineIfChanged(labTv, true);
                setMaxLinesIfChanged(labTv, 1);
                setEllipsizeIfChanged(labTv, TextUtils.TruncateAt.END);
                if (labTv.getMaxWidth() != maxLabW) {
                    labTv.setMaxWidth(maxLabW);
                }
                setGravityIfChanged(labTv, Gravity.CENTER_VERTICAL);
                TranslationEngine.ensureSafeVietnameseFontPadding(labTv);
                labTv.clearAnimation();
                labTv.animate().cancel();
                labTv.setTranslationY(0f);
                if (origLabPx > 0f) {
                    float targetLabPx = origLabPx;
                    CharSequence labText = labTv.getText();
                    if (labText != null && labText.length() > 0) {
                        TextPaint labPaint = new TextPaint(labTv.getPaint());
                        labPaint.setTextSize(origLabPx);
                        float measuredLabW = labPaint.measureText(labText.toString());
                        if (measuredLabW > maxLabW) {
                            float minLabPx = Math.max(origLabPx * 0.85f, 11.5f * density);
                            targetLabPx = Math.max(minLabPx, origLabPx * ((float) maxLabW / Math.max(1f, measuredLabW)));
                        }
                    }
                    if (Math.abs(labTv.getTextSize() - targetLabPx) >= 0.5f) {
                        labTv.setTextSize(TypedValue.COMPLEX_UNIT_PX, targetLabPx);
                    }
                }
                if (labTv.getPaddingTop() != 0 || labTv.getPaddingBottom() != 0) {
                    labTv.setPadding(labTv.getPaddingLeft(), 0, labTv.getPaddingRight(), 0);
                }
                ViewGroup.LayoutParams rawTvLp = labTv.getLayoutParams();
                if (rawTvLp != null) {
                    boolean changed = false;
                    if (rawTvLp.height != ViewGroup.LayoutParams.WRAP_CONTENT) {
                        rawTvLp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                        changed = true;
                    }
                    if (rawTvLp instanceof ViewGroup.MarginLayoutParams) {
                        ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) rawTvLp;
                        if (mlp.topMargin != 0 || mlp.bottomMargin != 0) {
                            mlp.topMargin = 0;
                            mlp.bottomMargin = 0;
                            changed = true;
                        }
                        if (mlp.leftMargin != 0) {
                            mlp.leftMargin = 0;
                            changed = true;
                        }
                        if (Build.VERSION.SDK_INT >= 17 && mlp.getMarginStart() != 0) {
                            mlp.setMarginStart(0);
                            changed = true;
                        }
                    }
                    if (rawTvLp instanceof LinearLayout.LayoutParams) {
                        LinearLayout.LayoutParams llp = (LinearLayout.LayoutParams) rawTvLp;
                        if (llp.gravity != Gravity.CENTER_VERTICAL) {
                            llp.gravity = Gravity.CENTER_VERTICAL;
                            changed = true;
                        }
                    } else if (rawTvLp instanceof RelativeLayout.LayoutParams) {
                        RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) rawTvLp;
                        rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
                        rlp.addRule(RelativeLayout.CENTER_VERTICAL);
                        changed = true;
                    }
                    if (changed) labTv.setLayoutParams(rawTvLp);
                }
            }
            if (tvTitle != null) {
                tvTitle.setSingleLine(true);
                tvTitle.setMaxLines(1);
                tvTitle.setEllipsize(TextUtils.TruncateAt.END);
            }
        } catch (Throwable ignored) {}
    }

    public static void optimizeCreateCanvasLayout(Activity activity) {
        if (activity == null || activity.isFinishing()) return;
        boolean hideTemplates = LibreConfig.isFeatureActive(LibreConfig.KEY_OPTIMIZE_CREATE_CANVAS);
        if (hideTemplates || TranslationEngine.isTextAutoFitActive()) {
            fixCreateCanvasTopBar(activity);
        }

        try {
            if (!hideTemplates) {
                return;
            }

            View templateBtn = findView(activity, "create_canvas_draft", 0x7f0903f8);
            if (templateBtn != null) {
                templateBtn.setVisibility(View.GONE);
                templateBtn.setEnabled(false);
            }

            View customCanvas = findView(activity, "custom_canvas", 0x7f09041f);
            View pxCanvas = findView(activity, "create_px_canvas", 0x7f090404);
            View picCanvas = findView(activity, "create_canvas_by_pic", 0x7f0903f7);

            // Re-align parent horizontal LinearLayout to weightSum = 3.0f
            if (templateBtn != null && templateBtn.getParent() instanceof LinearLayout) {
                LinearLayout parentLayout = (LinearLayout) templateBtn.getParent();
                parentLayout.setWeightSum(3.0f);

                View[] activeButtons = new View[]{customCanvas, pxCanvas, picCanvas};
                for (View btn : activeButtons) {
                    if (btn != null) {
                        ViewGroup.LayoutParams rawLp = btn.getLayoutParams();
                        if (rawLp instanceof LinearLayout.LayoutParams) {
                            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) rawLp;
                            lp.width = 0;
                            lp.weight = 1.0f;
                            lp.leftMargin = 0;
                            lp.rightMargin = 0;
                            btn.setLayoutParams(lp);
                        }
                        if (btn instanceof LinearLayout) {
                            ((LinearLayout) btn).setGravity(Gravity.CENTER);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static boolean isPublicStorageActive() {
        return LibreConfig.isFeatureActive(LibreConfig.KEY_PUBLIC_STORAGE);
    }

    public static boolean isCanvasHsjPathRowActive() {
        return LibreConfig.isFeatureActive(LibreConfig.KEY_CANVAS_HSJ_PATH_ROW);
    }

    public static boolean isCanvasPopupPolishActive() {
        return LibreConfig.isFeatureActive(LibreConfig.KEY_CANVAS_POPUP_POLISH);
    }

    public static boolean isHideBrushLibraryShopActive() {
        return LibreConfig.isFeatureActive(LibreConfig.KEY_HIDE_BRUSH_LIBRARY_SHOP);
    }

    public static String resolveActiveDraftDirectory(Context ctx) {
        try {
            Class<?> clz = LibreResourceResolver.loadRegisteredClass("FileHelper.class", null);
            Method m = LibreResourceResolver.findRegisteredMethod(
                    clz, "FileHelper.getDraftDir", null, String.class, true, boolean.class, String.class);
            if (m != null) {
                Object res = m.invoke(null, true, "");
                if (res instanceof String && !((String) res).trim().isEmpty()) {
                    String trimmed = ((String) res).trim();
                    while (trimmed.endsWith("/") && trimmed.length() > 1) {
                        trimmed = trimmed.substring(0, trimmed.length() - 1);
                    }
                    if (!trimmed.endsWith("/Draft")) {
                        File draftSub = new File(trimmed, "Draft");
                        if (draftSub.exists()
                                || trimmed.endsWith("/HuashijiePro")
                                || trimmed.endsWith("/files")) {
                            trimmed = draftSub.getAbsolutePath();
                        }
                    }
                    if (!trimmed.endsWith("/")) {
                        trimmed = trimmed + "/";
                    }
                    return trimmed;
                }
            }
        } catch (Throwable ignored) {}
        return new File(Environment.getExternalStorageDirectory(), "HuashijiePro/Draft").getAbsolutePath() + "/";
    }

    public static String resolveActiveExportDraftDirectory(Context ctx) {
        try {
            Class<?> clz = LibreResourceResolver.loadRegisteredClass("FileHelper.class", null);
            Method m = LibreResourceResolver.findRegisteredMethod(
                    clz, "FileHelper.getExportDraftsDir", null, String.class, true);
            if (m != null) {
                Object res = m.invoke(null);
                if (res instanceof String && !((String) res).trim().isEmpty()) {
                    String trimmed = ((String) res).trim();
                    if (!trimmed.endsWith("/")) {
                        trimmed = trimmed + "/";
                    }
                    return trimmed;
                }
            }
        } catch (Throwable ignored) {}
        return "/storage/emulated/0/HuashijiePro/exportDrafts/";
    }

    private static String formatShortDraftPath(String fullPath) {
        if (fullPath == null || fullPath.trim().isEmpty()) {
            return "/HuashijiePro/Draft/";
        }
        String p = fullPath.trim();
        if (!p.endsWith("/")) {
            p = p + "/";
        }
        if (p.contains("/Android/data/")) {
            return "Android/data/.../Draft/";
        }
        if (p.contains("/HuashijiePro/Draft")) {
            return "/HuashijiePro/Draft/";
        }
        String[] parts = p.split("/");
        ArrayList<String> nonEmpty = new ArrayList<>();
        for (String part : parts) {
            if (part != null && !part.isEmpty()) {
                nonEmpty.add(part);
            }
        }
        if (nonEmpty.size() >= 2) {
            return "/" + nonEmpty.get(nonEmpty.size() - 2) + "/" + nonEmpty.get(nonEmpty.size() - 1) + "/";
        }
        return p;
    }

    private static String extractLabelPrefix(String text) {
        if (text == null || text.isEmpty()) return "";
        int halfIdx = text.indexOf(':');
        int fullIdx = text.indexOf('\uff1a');
        int idx = -1;
        if (halfIdx >= 0 && fullIdx >= 0) {
            idx = Math.min(halfIdx, fullIdx);
        } else if (halfIdx >= 0) {
            idx = halfIdx;
        } else if (fullIdx >= 0) {
            idx = fullIdx;
        }
        if (idx >= 0) {
            char colon = text.charAt(idx);
            return text.substring(0, idx + 1) + (colon == '\uff1a' ? "" : " ");
        }
        return text + ": ";
    }

    /**
     * Hook called at the end of DraftStorageSettingActivity.initView()V.
     * Renders native dark matched storage paths and prevents text clipping next to switches.
     */
    public static void onDraftStorageInitView(Activity activity) {
        if (activity == null || activity.isFinishing()) return;

        try {
            boolean isDark = true;
            try {
                isDark = (activity.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
            } catch (Throwable ignored) {}

            float density = activity.getResources().getDisplayMetrics().density;
            int pad = Math.round(14 * density);

            // 1. Storage paths info card
            ViewGroup container1 = (ViewGroup) findView(activity, "sub_contentView1", 0x7f090c97);
            if (isPublicStorageActive() && container1 != null && container1.findViewWithTag("libre_storage_info") == null) {
                LinearLayout infoBox = new LinearLayout(activity);
                infoBox.setTag("libre_storage_info");
                infoBox.setOrientation(LinearLayout.VERTICAL);
                infoBox.setPadding(pad, pad, pad, Math.round(8 * density));

                TextView tvHeader = new TextView(activity);
                tvHeader.setText(TranslationEngine.get("libre_storage_box_title", "HuashijieLibre: \u5de5\u7a0b\u5b58\u50a8\u8def\u5f84 (.hsj)"));
                tvHeader.setTextColor(isDark ? Color.parseColor("#E0E0E0") : Color.parseColor("#1A1A1A"));
                tvHeader.setTextSize(14.0f);
                tvHeader.setTypeface(null, android.graphics.Typeface.BOLD);

                String draftPrefix = extractLabelPrefix(TranslationEngine.get(
                        "libre_storage_box_draft", "\u672c\u5730\u8349\u7a3f\u76ee\u5f55: /storage/emulated/0/HuashijiePro/Draft/"));
                String exportPrefix = extractLabelPrefix(TranslationEngine.get(
                        "libre_storage_box_export", "\u5de5\u7a0b\u5bfc\u51fa\u76ee\u5f55: /storage/emulated/0/HuashijiePro/exportDrafts/"));
                String realDraftDir = resolveActiveDraftDirectory(activity);
                String realExportDir = resolveActiveExportDraftDirectory(activity);

                TextView tvDraft = new TextView(activity);
                tvDraft.setText(draftPrefix + realDraftDir);
                tvDraft.setTextColor(isDark ? Color.parseColor("#A0A0A0") : Color.parseColor("#555555"));
                tvDraft.setTextSize(12.0f);
                tvDraft.setPadding(0, Math.round(6 * density), 0, 0);

                TextView tvExport = new TextView(activity);
                tvExport.setText(exportPrefix + realExportDir);
                tvExport.setTextColor(isDark ? Color.parseColor("#A0A0A0") : Color.parseColor("#555555"));
                tvExport.setTextSize(12.0f);
                tvExport.setPadding(0, Math.round(3 * density), 0, 0);

                View divider = new View(activity);
                divider.setBackgroundColor(isDark ? Color.parseColor("#2C2C2E") : Color.parseColor("#EEEEEE"));
                LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
                divLp.setMargins(0, Math.round(12 * density), 0, 0);
                divider.setLayoutParams(divLp);

                infoBox.addView(tvHeader);
                infoBox.addView(tvDraft);
                infoBox.addView(tvExport);
                infoBox.addView(divider);

                container1.addView(infoBox, 0);
            }

            // 2. Prevent switch clipping across all cards in DraftStorageSettingActivity
            fixSwitchRowClipping(container1, density);

            ViewGroup container2 = (ViewGroup) findView(activity, "sub_contentView2", 0x7f090c98);
            if (container2 != null) {
                fixSwitchRowClipping(container2, density);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static void fixSwitchRowClipping(ViewGroup container, float density) {
        if (container == null) return;
        try {
            int safeEndMargin = Math.round(60 * density);
            int relGapPx = Math.round(12 * density);
            int count = container.getChildCount();
            for (int i = 0; i < count; i++) {
                View child = container.getChildAt(i);
                if (child instanceof ViewGroup) {
                    ViewGroup vg = (ViewGroup) child;
                    TextView label = null;
                    View toggle = null;
                    int textCount = 0;
                    for (int j = 0; j < vg.getChildCount(); j++) {
                        View sub = vg.getChildAt(j);
                        if (sub instanceof TextView && !(sub instanceof android.widget.CompoundButton)) {
                            label = (TextView) sub;
                            textCount++;
                        } else if (sub instanceof android.widget.CompoundButton) {
                            toggle = sub;
                        }
                    }
                    if (label != null && toggle != null && textCount == 1) {
                        ViewGroup.LayoutParams lp = label.getLayoutParams();
                        if (vg instanceof RelativeLayout && lp instanceof RelativeLayout.LayoutParams) {
                            int toggleId = toggle.getId();
                            if (toggleId == View.NO_ID || toggleId <= 0) {
                                toggleId = SYNTHETIC_SWITCH_ID_BASE + i;
                                toggle.setId(toggleId);
                            }
                            RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) lp;
                            int[] rules = rlp.getRules();
                            boolean changed = rlp.rightMargin != relGapPx
                                    || rules[RelativeLayout.LEFT_OF] != toggleId
                                    || rules[RelativeLayout.ALIGN_PARENT_LEFT] == 0
                                    || (Build.VERSION.SDK_INT >= 17 && (rlp.getMarginEnd() != relGapPx
                                            || rules[RelativeLayout.START_OF] != toggleId
                                            || rules[RelativeLayout.ALIGN_PARENT_START] == 0));
                            if (changed) {
                                rlp.rightMargin = relGapPx;
                                if (Build.VERSION.SDK_INT >= 17) {
                                    rlp.setMarginEnd(relGapPx);
                                    rlp.removeRule(RelativeLayout.ALIGN_PARENT_END);
                                    rlp.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                                    rlp.addRule(RelativeLayout.ALIGN_PARENT_START);
                                    rlp.addRule(RelativeLayout.START_OF, toggleId);
                                }
                                rlp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                                rlp.addRule(RelativeLayout.LEFT_OF, toggleId);
                                label.setLayoutParams(rlp);
                            }
                        } else if (lp instanceof ViewGroup.MarginLayoutParams) {
                            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
                            if (mlp.rightMargin != safeEndMargin
                                    || (Build.VERSION.SDK_INT >= 17 && mlp.getMarginEnd() != safeEndMargin)) {
                                mlp.rightMargin = safeEndMargin;
                                if (Build.VERSION.SDK_INT >= 17) {
                                    mlp.setMarginEnd(safeEndMargin);
                                }
                                label.setLayoutParams(mlp);
                            }
                        }
                        setMaxLinesIfChanged(label, 2);
                        setSingleLineIfChanged(label, false);
                        setEllipsizeIfChanged(label, TextUtils.TruncateAt.END);
                        TranslationEngine.ensureSafeVietnameseFontPadding(label);
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Hook called on the inflated layout_exit_draft_cloud_sync (0x7f0c01d5) or its root.
     * 1. Hides domestic cloud sync switch container and sync message.
     * 2. Replaces fixed 32dp header row height with WRAP_CONTENT to eliminate vertical text clipping.
     * 3. Configures title TextView to render crisp single line at 17sp without vertical or horizontal clipping.
     * 4. Suppresses unwanted cloud guide popup.
     */
    public static void optimizeCanvasExitDialog(final View dialogView) {
        if (dialogView == null) return;
        try {
            final boolean hideCloud = isHideCanvasExitCloudActive();
            final boolean autoFit = TranslationEngine.isTextAutoFitActive();
            final boolean i18nActive = TranslationEngine.isDynamicI18nEnabled();
            if (!hideCloud && !autoFit && !i18nActive) {
                return;
            }

            final Runnable enforceExitDialog = new Runnable() {
                @Override
                public void run() {
                    try {
                        if (hideCloud) {
                            View cloudContainer = findView(dialogView, "cloud_sync_switch_container", 0x7f09037a);
                            if (cloudContainer != null && cloudContainer.getVisibility() != View.GONE) {
                                cloudContainer.setVisibility(View.GONE);
                                cloudContainer.setEnabled(false);
                                cloudContainer.setClickable(false);
                            }
                            View cloudSwitch = findView(dialogView, "cloud_sync_switch", 0x7f090379);
                            if (cloudSwitch != null && cloudSwitch.getVisibility() != View.GONE) {
                                cloudSwitch.setVisibility(View.GONE);
                                cloudSwitch.setEnabled(false);
                                cloudSwitch.setClickable(false);
                            }
                            View cloudMsg = findView(dialogView, "tv_cloud_sync_message", 0x7f090dfd);
                            if (cloudMsg != null && cloudMsg.getVisibility() != View.GONE) {
                                cloudMsg.setVisibility(View.GONE);
                                cloudMsg.setEnabled(false);
                                cloudMsg.setClickable(false);
                            }
                        }

                        if (!hideCloud && !autoFit) {
                            return;
                        }

                        ViewGroup headerRow = null;
                        View cloudContainer = findView(dialogView, "cloud_sync_switch_container", 0x7f09037a);
                        if (cloudContainer != null && cloudContainer.getParent() instanceof ViewGroup) {
                            headerRow = (ViewGroup) cloudContainer.getParent();
                        } else if (dialogView instanceof ViewGroup
                                && (findView(dialogView, "cloud_sync_switch", 0x7f090379) != null
                                || findView(dialogView, "tv_cloud_sync_message", 0x7f090dfd) != null)) {
                            ViewGroup vg = (ViewGroup) dialogView;
                            if (vg.getChildCount() > 0 && vg.getChildAt(0) instanceof ViewGroup) {
                                headerRow = (ViewGroup) vg.getChildAt(0);
                            }
                        }

                        if (headerRow != null) {
                            ViewGroup.LayoutParams lp = headerRow.getLayoutParams();
                            if (lp != null && lp.height != ViewGroup.LayoutParams.WRAP_CONTENT) {
                                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                                headerRow.setLayoutParams(lp);
                            }
                            float density = headerRow.getResources().getDisplayMetrics().density;
                            if (hideCloud) {
                                if (headerRow.getMinimumHeight() != 0) {
                                    headerRow.setMinimumHeight(0);
                                }
                                Object origPadObj = headerRow.getTag(TAG_EXIT_ORIG_PAD_TOP);
                                int origPadTop = (origPadObj instanceof Integer)
                                        ? (Integer) origPadObj
                                        : headerRow.getPaddingTop();
                                if (origPadObj == null) {
                                    headerRow.setTag(TAG_EXIT_ORIG_PAD_TOP, origPadTop);
                                }
                                int balancedTop = origPadTop;
                                if (headerRow.getPaddingTop() != balancedTop || headerRow.getPaddingBottom() != 0) {
                                    headerRow.setPadding(
                                            headerRow.getPaddingLeft(),
                                            balancedTop,
                                            headerRow.getPaddingRight(),
                                            0);
                                }
                                if (lp instanceof ViewGroup.MarginLayoutParams) {
                                    ViewGroup.MarginLayoutParams hMlp = (ViewGroup.MarginLayoutParams) lp;
                                    if (hMlp.bottomMargin != 0) {
                                        hMlp.bottomMargin = 0;
                                        headerRow.setLayoutParams(hMlp);
                                    }
                                }
                                if (headerRow.getParent() instanceof ViewGroup) {
                                    ViewGroup dialogContent = (ViewGroup) headerRow.getParent();
                                    if (dialogContent.getMinimumHeight() != 0) {
                                        dialogContent.setMinimumHeight(0);
                                    }
                                    if (dialogContent.getPaddingBottom() != 0) {
                                        dialogContent.setPadding(
                                                dialogContent.getPaddingLeft(),
                                                dialogContent.getPaddingTop(),
                                                dialogContent.getPaddingRight(),
                                                0);
                                    }
                                }
                            } else {
                                int minH = Math.round(56 * density);
                                if (headerRow.getMinimumHeight() < minH) {
                                    headerRow.setMinimumHeight(minH);
                                }
                            }
                            headerRow.setClipChildren(false);
                            headerRow.setClipToPadding(false);

                            int msgId = id(dialogView, "tv_cloud_sync_message", 0x7f090dfd);
                            int switchContId = id(dialogView, "cloud_sync_switch_container", 0x7f09037a);
                            int exitTitleStrId = LibreResourceResolver.resolveStringId(
                                    dialogView.getContext(), "exit_canvas_title", 0x7f1102e6);

                            TextView foundTitle = null;
                            for (int i = 0; i < headerRow.getChildCount(); i++) {
                                View child = headerRow.getChildAt(i);
                                if (child instanceof TextView
                                        && child.getId() != msgId
                                        && child.getId() != switchContId) {
                                    TextView tvTitle = (TextView) child;
                                    foundTitle = tvTitle;
                                    ViewGroup.LayoutParams tvLp = tvTitle.getLayoutParams();
                                    if (tvLp instanceof LinearLayout.LayoutParams) {
                                        LinearLayout.LayoutParams llp = (LinearLayout.LayoutParams) tvLp;
                                        boolean changed = false;
                                        if (llp.width != ViewGroup.LayoutParams.MATCH_PARENT && llp.weight != 1.0f) {
                                            llp.width = 0;
                                            llp.weight = 1.0f;
                                            llp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                                            changed = true;
                                        }
                                        if (hideCloud && (llp.topMargin != 0 || llp.bottomMargin != 0)) {
                                            llp.topMargin = 0;
                                            llp.bottomMargin = 0;
                                            changed = true;
                                        }
                                        if (changed) {
                                            tvTitle.setLayoutParams(llp);
                                        }
                                    } else if (hideCloud && tvLp instanceof ViewGroup.MarginLayoutParams) {
                                        ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) tvLp;
                                        if (mlp.topMargin != 0 || mlp.bottomMargin != 0) {
                                            mlp.topMargin = 0;
                                            mlp.bottomMargin = 0;
                                            tvTitle.setLayoutParams(mlp);
                                        }
                                    }
                                    if (hideCloud && (tvTitle.getPaddingTop() != 0 || tvTitle.getPaddingBottom() != 0)) {
                                        tvTitle.setPadding(
                                                tvTitle.getPaddingLeft(),
                                                0,
                                                tvTitle.getPaddingRight(),
                                                0);
                                    }
                                    TranslationEngine.ensureSafeVietnameseFontPadding(tvTitle);
                                    if (Math.abs(tvTitle.getLineSpacingMultiplier() - 1.15f) > 0.01f) {
                                        tvTitle.setLineSpacing(0f, 1.15f);
                                    }
                                    setSingleLineIfChanged(tvTitle, true);
                                    setMaxLinesIfChanged(tvTitle, 1);
                                    if (Math.abs(tvTitle.getTextSize() - TypedValue.applyDimension(
                                            TypedValue.COMPLEX_UNIT_SP, 17.0f,
                                            tvTitle.getResources().getDisplayMetrics())) >= 0.5f) {
                                        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17.0f);
                                    }
                                    setEllipsizeIfChanged(tvTitle, TextUtils.TruncateAt.END);
                                    if (i18nActive && exitTitleStrId != 0) {
                                        String expectedTitle = TranslationEngine.translateById(exitTitleStrId, "\u9000\u51fa\u753b\u5e03\uff1f");
                                        CharSequence currText = tvTitle.getText();
                                        if (currText == null || !currText.toString().equals(expectedTitle)) {
                                            tvTitle.setText(expectedTitle);
                                        }
                                    }
                                    break;
                                }
                            }
                            if (hideCloud && foundTitle != null) {
                                collapseExitTitleContainersBottom(dialogView, foundTitle, headerRow);
                            }
                        }

                        if (dialogView instanceof ViewGroup) {
                            unclipContainerSafely((ViewGroup) dialogView);
                        }
                    } catch (Throwable ignored) {}
                }
            };

            enforceExitDialog.run();

            if (dialogView.getTag(TAG_EXIT_DIALOG_HOOKED) == null) {
                dialogView.setTag(TAG_EXIT_DIALOG_HOOKED, Boolean.TRUE);
                final ViewTreeObserver.OnPreDrawListener listener = new ViewTreeObserver.OnPreDrawListener() {
                    private int mAdjustPasses = 0;
                    @Override
                    public boolean onPreDraw() {
                        if (mAdjustPasses < 2 && hideCloud && balanceExitDialogVerticalSpacingOnce(dialogView)) {
                            mAdjustPasses++;
                            return false;
                        }
                        try {
                            dialogView.getViewTreeObserver().removeOnPreDrawListener(this);
                        } catch (Throwable ignored) {}
                        return true;
                    }
                };
                dialogView.getViewTreeObserver().addOnPreDrawListener(listener);
            }

            TranslationEngine.translateViewHierarchy(dialogView);

            if (hideCloud) {
                try {
                    Class<?> utilClass = LibreResourceResolver.loadRegisteredClass("PrefUtil.class", null);
                    Method setMethod = LibreResourceResolver.findRegisteredMethod(
                            utilClass, "PrefUtil.putBoolean", null, void.class, true, String.class, boolean.class);
                    if (setMethod != null) {
                        setMethod.invoke(null, "quit_draft_cloud_sync_hint", true);
                    }
                } catch (Throwable ignored) {}
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static void collapseExitTitleContainersBottom(View dialogView, View tvTitle, ViewGroup headerRow) {
        if (dialogView == null || tvTitle == null) return;
        int msgId = id(dialogView, "tv_cloud_sync_message", 0x7f090dfd);
        int switchContId = id(dialogView, "cloud_sync_switch_container", 0x7f09037a);
        int switchId = id(dialogView, "cloud_sync_switch", 0x7f090379);
        ViewParent p = tvTitle.getParent();
        while (p instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) p;
            if (vg == dialogView) break;
            if (vg.getMinimumHeight() != 0) {
                vg.setMinimumHeight(0);
            }
            if (vg.getPaddingBottom() != 0) {
                vg.setPadding(vg.getPaddingLeft(), vg.getPaddingTop(), vg.getPaddingRight(), 0);
            }
            ViewGroup.LayoutParams lp = vg.getLayoutParams();
            if (lp != null) {
                boolean lpChanged = false;
                if (lp.height != ViewGroup.LayoutParams.WRAP_CONTENT && lp.height > 0) {
                    lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    lpChanged = true;
                }
                if (lp instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
                    if (mlp.bottomMargin != 0) {
                        mlp.bottomMargin = 0;
                        lpChanged = true;
                    }
                }
                if (lpChanged) {
                    vg.setLayoutParams(lp);
                }
            }
            if (vg == headerRow) {
                for (int i = 0; i < vg.getChildCount(); i++) {
                    View ch = vg.getChildAt(i);
                    if (ch != null && ch != tvTitle && (ch.getId() == msgId || ch.getId() == switchContId || ch.getId() == switchId)) {
                        if (ch.getVisibility() != View.GONE) ch.setVisibility(View.GONE);
                    }
                }
            }
            p = vg.getParent();
        }
    }

    private static View findExitDialogTitleView(View root) {
        if (root == null) return null;
        int msgId = id(root, "tv_cloud_sync_message", 0x7f090dfd);
        int switchContId = id(root, "cloud_sync_switch_container", 0x7f09037a);
        View msgLayout = findView(root, "msg_layout", 0);
        View search = msgLayout != null ? msgLayout : findView(root, "dialog_content", 0);
        if (search == null) search = root;
        if (search instanceof ViewGroup) {
            java.util.ArrayDeque<View> q = new java.util.ArrayDeque<>();
            q.add(search);
            while (!q.isEmpty()) {
                View v = q.poll();
                if (v instanceof TextView && v.getVisibility() == View.VISIBLE
                        && v.getId() != msgId && v.getId() != switchContId) {
                    return v;
                }
                if (v instanceof ViewGroup && v.getId() != switchContId) {
                    ViewGroup vg = (ViewGroup) v;
                    for (int i = 0; i < vg.getChildCount(); i++) {
                        View ch = vg.getChildAt(i);
                        if (ch != null) q.add(ch);
                    }
                }
            }
        }
        return null;
    }

    private static int getYRelativeTo(View target, View ancestor) {
        int y = 0;
        View cur = target;
        while (cur != null && cur != ancestor) {
            y += cur.getTop();
            ViewParent p = cur.getParent();
            if (!(p instanceof View)) break;
            cur = (View) p;
        }
        return y;
    }

    private static boolean balanceExitDialogVerticalSpacingOnce(View dialogView) {
        if (dialogView == null) return false;
        View searchRoot = dialogView;
        View btnLayout = findView(searchRoot, "btn_layout", 0);
        while (btnLayout == null && searchRoot.getParent() instanceof ViewGroup) {
            searchRoot = (View) searchRoot.getParent();
            btnLayout = findView(searchRoot, "btn_layout", 0);
        }
        if (btnLayout == null || btnLayout.getHeight() <= 0) return false;

        View cardRoot = searchRoot;
        ViewParent walk = cardRoot.getParent();
        for (int hops = 0; hops < 6 && walk instanceof ViewGroup; hops++) {
            ViewGroup parent = (ViewGroup) walk;
            cardRoot = parent;
            if (parent.getId() == android.R.id.content) {
                break;
            }
            walk = parent.getParent();
        }

        View tvTitle = findExitDialogTitleView(cardRoot);
        if (tvTitle == null || tvTitle.getHeight() <= 0 || cardRoot.getHeight() <= 0) return false;

        int cardH = cardRoot.getHeight();
        int titleTop = getYRelativeTo(tvTitle, cardRoot);
        int titleBot = titleTop + tvTitle.getHeight();
        int btnTop = getYRelativeTo(btnLayout, cardRoot);
        int btnBot = btnTop + btnLayout.getHeight();

        int topGap = titleTop;
        int midGap = btnTop - titleBot;
        int botGap = cardH - btnBot;
        if (botGap <= 0) return false;

        int targetGap = botGap;
        boolean changed = false;

        int deltaTop = targetGap - topGap;
        if (Math.abs(deltaTop) > 1) {
            View msgLayout = findView(cardRoot, "msg_layout", 0);
            View dialogContent = findView(cardRoot, "dialog_content", 0);
            View padTarget = msgLayout != null ? msgLayout : (dialogContent != null ? dialogContent : tvTitle);
            int curPadTop = padTarget.getPaddingTop();
            int newPadTop = curPadTop + deltaTop;
            if (newPadTop >= 0) {
                padTarget.setPadding(padTarget.getPaddingLeft(), newPadTop, padTarget.getPaddingRight(), padTarget.getPaddingBottom());
                changed = true;
            } else if (curPadTop > 0) {
                padTarget.setPadding(padTarget.getPaddingLeft(), 0, padTarget.getPaddingRight(), padTarget.getPaddingBottom());
                changed = true;
            }
        }

        int deltaMid = targetGap - midGap;
        if (Math.abs(deltaMid) > 1) {
            int remainingMid = deltaMid;
            ViewGroup.LayoutParams btnLp = btnLayout.getLayoutParams();
            if (btnLp instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) btnLp;
                int newMarginTop = Math.max(0, mlp.topMargin + remainingMid);
                remainingMid -= (newMarginTop - mlp.topMargin);
                if (newMarginTop != mlp.topMargin) {
                    mlp.topMargin = newMarginTop;
                    btnLayout.setLayoutParams(mlp);
                    changed = true;
                }
            }
            if (remainingMid < -1) {
                View dialogContent = findView(cardRoot, "dialog_content", 0);
                if (dialogContent != null && dialogContent.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams dMlp = (ViewGroup.MarginLayoutParams) dialogContent.getLayoutParams();
                    int newBotMargin = Math.max(0, dMlp.bottomMargin + remainingMid);
                    if (newBotMargin != dMlp.bottomMargin) {
                        dMlp.bottomMargin = newBotMargin;
                        dialogContent.setLayoutParams(dMlp);
                        changed = true;
                    }
                }
            }
        }

        if (changed) {
            cardRoot.requestLayout();
        }
        return changed;
    }

    private static boolean isDynamicNumericReadout(TextView tv) {
        if (tv == null) return false;
        CharSequence cs = tv.getText();
        if (cs == null) return false;
        String s = cs.toString().trim();
        if (s.isEmpty()) return false;
        if (s.startsWith("#") && s.length() <= 10) return true;
        if (s.startsWith("/") || s.contains("/HuashijiePro/")) return true;
        if (PATTERN_DYNAMIC_NUMERIC.matcher(s).matches()) {
            return true;
        }
        return isDraftInfoValueView(tv);
    }

    public static boolean isExportRowView(View v) {
        if (v == null) return false;
        if (matchesId(v, "bg_pdf", 0x7f0901fb)
                || matchesId(v, "tv_pdf", 0x7f090ed6)
                || matchesId(v, "iv_pdf_vip", 0x7f0906e8)) {
            return true;
        }
        if (matchesAnyId(v, EXPORT_ROW_TEXT_NAMES, EXPORT_ROW_TEXT_FALLBACKS)) {
            return true;
        }
        if (Boolean.TRUE.equals(v.getTag(TAG_EXPORT_ROW_NORMALIZED))) return true;
        ViewParent p = v.getParent();
        if (p instanceof View) {
            View pv = (View) p;
            if (matchesId(pv, "bg_pdf", 0x7f0901fb)) return true;
            if (Boolean.TRUE.equals(pv.getTag(TAG_EXPORT_ROW_NORMALIZED))) return true;
        }
        return false;
    }

    private static void unclipContainerSafely(ViewGroup vg) {
        if (vg == null) return;
        String cls = vg.getClass().getName();
        if (vg instanceof ScrollView
                || vg instanceof HorizontalScrollView
                || cls.contains("ScrollView")
                || cls.contains("RecyclerView")
                || cls.contains("WebView")) {
            return;
        }
        if (vg.getClipChildren()) {
            vg.setClipChildren(false);
        }
        if (vg.getClipToPadding()) {
            vg.setClipToPadding(false);
        }
    }

    private static boolean isIconAboveTextButton(View v, float density) {
        if (!(v instanceof LinearLayout)) return false;
        LinearLayout ll = (LinearLayout) v;
        if (ll.getOrientation() != LinearLayout.VERTICAL) return false;
        if (isExportRowView(ll)
                || "libre_settings_root".equals(ll.getTag())
                || Boolean.TRUE.equals(ll.getTag(TAG_THREE_BTN_NORMALIZED))) {
            return false;
        }

        int maxButtonH = Math.round(96 * density);
        int maxIconDim = Math.round(48 * density);
        if (ll.getHeight() > maxButtonH) return false;
        ViewGroup.LayoutParams rootLp = ll.getLayoutParams();
        if (rootLp != null && rootLp.height > maxButtonH) return false;

        ImageView foundIv = null;
        TextView foundTv = null;
        int count = ll.getChildCount();
        for (int i = 0; i < count; i++) {
            View ch = ll.getChildAt(i);
            if (ch.getVisibility() == View.GONE) continue;
            if (ch instanceof ImageView) {
                if (foundIv != null) return false;
                ImageView iv = (ImageView) ch;
                if (iv.getWidth() > maxIconDim || iv.getHeight() > maxIconDim) return false;
                ViewGroup.LayoutParams ivLp = iv.getLayoutParams();
                if (ivLp != null && (ivLp.width > maxIconDim || ivLp.height > maxIconDim)) return false;
                foundIv = iv;
            } else if (ch instanceof TextView) {
                if (foundTv != null) return false;
                TextView tv = (TextView) ch;
                if (isDynamicNumericReadout(tv)) return false;
                foundTv = tv;
            } else {
                return false;
            }
        }
        return foundIv != null && foundTv != null;
    }

    private static void applyIconAboveTextButtonLock(
            LinearLayout item, int uniformIconH, int uniformTopPad, float density,
            boolean compactSpacing, boolean unlockItemHeight) {
        if (item == null) return;
        ImageView iv = null;
        TextView tv = null;
        for (int i = 0; i < item.getChildCount(); i++) {
            View ch = item.getChildAt(i);
            if (ch.getVisibility() == View.GONE) continue;
            if (ch instanceof ImageView) iv = (ImageView) ch;
            else if (ch instanceof TextView) tv = (TextView) ch;
        }
        if (iv == null || tv == null) return;

        int targetGravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        if (item.getGravity() != targetGravity) {
            item.setGravity(targetGravity);
        }
        int bottomPad = item.getPaddingBottom();
        if (compactSpacing) {
            int minBot = Math.round(5 * density);
            if (bottomPad < minBot) bottomPad = minBot;
        }
        if ((uniformTopPad >= 0 && item.getPaddingTop() != uniformTopPad)
                || bottomPad != item.getPaddingBottom()) {
            int topPad = uniformTopPad >= 0 ? uniformTopPad : item.getPaddingTop();
            item.setPadding(item.getPaddingLeft(), topPad, item.getPaddingRight(), bottomPad);
        }

        if (unlockItemHeight) {
            ViewGroup.LayoutParams rawItemLp = item.getLayoutParams();
            int minItemH = Math.round(60 * density);
            if (item.getMinimumHeight() < minItemH) {
                item.setMinimumHeight(minItemH);
            }
            if (rawItemLp != null && rawItemLp.height != ViewGroup.LayoutParams.WRAP_CONTENT) {
                rawItemLp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                item.setLayoutParams(rawItemLp);
            }
        }

        ViewGroup.LayoutParams rawIvLp = iv.getLayoutParams();
        if (rawIvLp instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams ivLp = (LinearLayout.LayoutParams) rawIvLp;
            boolean ivChanged = false;
            if (ivLp.gravity != Gravity.CENTER_HORIZONTAL) {
                ivLp.gravity = Gravity.CENTER_HORIZONTAL;
                ivChanged = true;
            }
            if (uniformIconH > 0 && ivLp.height != uniformIconH) {
                ivLp.height = uniformIconH;
                ivChanged = true;
            }
            if (ivChanged) {
                iv.setLayoutParams(ivLp);
            }
        }
        if (uniformIconH > 0 && iv.getScaleType() != ImageView.ScaleType.CENTER_INSIDE) {
            iv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        }

        TranslationEngine.ensureSafeVietnameseFontPadding(tv);
        setEllipsizeIfChanged(tv, null);
        int targetMaxLines = unlockItemHeight ? 3 : 2;
        if (tv.getMaxLines() < targetMaxLines) {
            setSingleLineIfChanged(tv, false);
            setMaxLinesIfChanged(tv, targetMaxLines);
        }
        int targetTvGravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        if (tv.getGravity() != targetTvGravity) {
            tv.setGravity(targetTvGravity);
        }
        if (Build.VERSION.SDK_INT >= 17 && tv.getTextAlignment() != View.TEXT_ALIGNMENT_CENTER) {
            tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        }
        ViewGroup.LayoutParams rawTvLp = tv.getLayoutParams();
        if (rawTvLp instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams tvLp = (LinearLayout.LayoutParams) rawTvLp;
            boolean tvChanged = false;
            if (tvLp.height > 0) {
                tvLp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                tvChanged = true;
            }
            if (compactSpacing) {
                int maxTopGap = Math.round(3 * density);
                if (tvLp.topMargin > maxTopGap) {
                    tvLp.topMargin = maxTopGap;
                    tvChanged = true;
                }
            }
            if (tvLp.gravity != Gravity.CENTER_HORIZONTAL) {
                tvLp.gravity = Gravity.CENTER_HORIZONTAL;
                tvChanged = true;
            }
            if (tvChanged) {
                tv.setLayoutParams(tvLp);
            }
        } else if (rawTvLp != null && rawTvLp.height > 0) {
            rawTvLp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            tv.setLayoutParams(rawTvLp);
        }
    }

    /**
     * Repairs Layer Settings blend_bg_view overlap by chaining tv_blend between tv_blend_mode
     * and iv_arrow_down and downscaling text uniformly when localized labels exceed row width.
     */
    public static void optimizeBlendRow(View root) {
        if (root == null) return;
        try {
            View blendBg = findView(root, "blend_bg_view", 0x7f090223);
            if (blendBg == null) return;
            TextView tvMode = (TextView) findView(blendBg, "tv_blend_mode", 0x7f090dd4);
            TextView tvBlend = (TextView) findView(blendBg, "tv_blend", 0x7f090dd3);
            View ivArrow = findView(blendBg, "iv_arrow_down", 0x7f090665);
            if (tvMode == null || tvBlend == null || ivArrow == null) return;
            int idTvBlendMode = tvMode.getId();
            int idIvArrowDown = ivArrow.getId();

            float density = blendBg.getResources().getDisplayMetrics().density;

            ViewGroup.LayoutParams rawModeLp = tvMode.getLayoutParams();
            if (rawModeLp instanceof RelativeLayout.LayoutParams) {
                RelativeLayout.LayoutParams lpMode = (RelativeLayout.LayoutParams) rawModeLp;
                boolean changedMode = false;
                int maxLeft = Math.round(6 * density);
                if (lpMode.leftMargin > maxLeft) {
                    lpMode.leftMargin = maxLeft;
                    changedMode = true;
                }
                if (lpMode.getRules()[RelativeLayout.ALIGN_PARENT_LEFT] == 0) {
                    lpMode.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                    changedMode = true;
                }
                if (Build.VERSION.SDK_INT >= 17 && lpMode.getRules()[RelativeLayout.ALIGN_PARENT_START] == 0) {
                    lpMode.addRule(RelativeLayout.ALIGN_PARENT_START);
                    changedMode = true;
                }
                if (changedMode) {
                    tvMode.setLayoutParams(lpMode);
                }
            }
            setSingleLineIfChanged(tvMode, true);
            setMaxLinesIfChanged(tvMode, 1);
            setEllipsizeIfChanged(tvMode, TextUtils.TruncateAt.END);
            TranslationEngine.ensureSafeVietnameseFontPadding(tvMode);

            ViewGroup.LayoutParams rawArrowLp = ivArrow.getLayoutParams();
            int arrowRightMargin = Math.round(6 * density);
            if (rawArrowLp instanceof RelativeLayout.LayoutParams) {
                RelativeLayout.LayoutParams lpArrow = (RelativeLayout.LayoutParams) rawArrowLp;
                if (lpArrow.rightMargin > arrowRightMargin) {
                    lpArrow.rightMargin = arrowRightMargin;
                    ivArrow.setLayoutParams(lpArrow);
                }
                arrowRightMargin = lpArrow.rightMargin;
            }

            ViewGroup.LayoutParams rawBlendLp = tvBlend.getLayoutParams();
            int blendLeftGap = Math.round(4 * density);
            int blendRightGap = Math.round(2 * density);
            if (rawBlendLp instanceof RelativeLayout.LayoutParams) {
                RelativeLayout.LayoutParams lpBlend = (RelativeLayout.LayoutParams) rawBlendLp;
                boolean changedBlend = false;
                if (idTvBlendMode != View.NO_ID && lpBlend.getRules()[RelativeLayout.RIGHT_OF] != idTvBlendMode) {
                    lpBlend.addRule(RelativeLayout.RIGHT_OF, idTvBlendMode);
                    changedBlend = true;
                }
                if (idIvArrowDown != View.NO_ID && lpBlend.getRules()[RelativeLayout.LEFT_OF] != idIvArrowDown) {
                    lpBlend.addRule(RelativeLayout.LEFT_OF, idIvArrowDown);
                    changedBlend = true;
                }
                if (Build.VERSION.SDK_INT >= 17) {
                    if (idTvBlendMode != View.NO_ID && lpBlend.getRules()[RelativeLayout.END_OF] != idTvBlendMode) {
                        lpBlend.addRule(RelativeLayout.END_OF, idTvBlendMode);
                        changedBlend = true;
                    }
                    if (idIvArrowDown != View.NO_ID && lpBlend.getRules()[RelativeLayout.START_OF] != idIvArrowDown) {
                        lpBlend.addRule(RelativeLayout.START_OF, idIvArrowDown);
                        changedBlend = true;
                    }
                }
                if (lpBlend.width != ViewGroup.LayoutParams.MATCH_PARENT) {
                    lpBlend.width = ViewGroup.LayoutParams.MATCH_PARENT;
                    changedBlend = true;
                }
                if (lpBlend.leftMargin != blendLeftGap) {
                    lpBlend.leftMargin = blendLeftGap;
                    changedBlend = true;
                }
                if (lpBlend.rightMargin != blendRightGap) {
                    lpBlend.rightMargin = blendRightGap;
                    changedBlend = true;
                }
                if (changedBlend) {
                    tvBlend.setLayoutParams(lpBlend);
                }
            }
            if (tvBlend.getPaddingLeft() != 0 || tvBlend.getPaddingRight() != 0) {
                tvBlend.setPadding(0, tvBlend.getPaddingTop(), 0, tvBlend.getPaddingBottom());
            }
            int endGravity = Gravity.END | Gravity.CENTER_VERTICAL;
            if (tvBlend.getGravity() != endGravity) {
                tvBlend.setGravity(endGravity);
            }
            if (Build.VERSION.SDK_INT >= 17 && tvBlend.getTextAlignment() != View.TEXT_ALIGNMENT_VIEW_END) {
                tvBlend.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
            }
            setSingleLineIfChanged(tvBlend, true);
            setMaxLinesIfChanged(tvBlend, 1);
            setEllipsizeIfChanged(tvBlend, TextUtils.TruncateAt.END);
            TranslationEngine.ensureSafeVietnameseFontPadding(tvBlend);

            int rowW = blendBg.getWidth();
            if (rowW <= 0) return;

            String modeTxt = tvMode.getText() != null ? tvMode.getText().toString() : "";
            String blendTxt = tvBlend.getText() != null ? tvBlend.getText().toString() : "";
            String sig = modeTxt + "|" + blendTxt + "@" + rowW;
            if (sig.equals(blendBg.getTag(TAG_BLEND_LAST_FIT_SIG))) {
                return;
            }
            blendBg.setTag(TAG_BLEND_LAST_FIT_SIG, sig);

            Float origModePxObj = (Float) tvMode.getTag(TAG_BLEND_ORIG_TEXT_SIZE);
            if (origModePxObj == null) {
                origModePxObj = tvMode.getTextSize();
                tvMode.setTag(TAG_BLEND_ORIG_TEXT_SIZE, origModePxObj);
            }
            Float origBlendPxObj = (Float) tvBlend.getTag(TAG_BLEND_ORIG_TEXT_SIZE);
            if (origBlendPxObj == null) {
                origBlendPxObj = tvBlend.getTextSize();
                tvBlend.setTag(TAG_BLEND_ORIG_TEXT_SIZE, origBlendPxObj);
            }

            float origModePx = origModePxObj;
            float origBlendPx = origBlendPxObj;
            int modeLeftMargin = (tvMode.getLayoutParams() instanceof ViewGroup.MarginLayoutParams)
                    ? ((ViewGroup.MarginLayoutParams) tvMode.getLayoutParams()).leftMargin
                    : Math.round(6 * density);
            int arrowW = ivArrow.getWidth() > 0 ? ivArrow.getWidth() : Math.round(18 * density);
            int availW = rowW - blendBg.getPaddingLeft() - blendBg.getPaddingRight()
                    - modeLeftMargin - blendLeftGap - blendRightGap - arrowW - arrowRightMargin - Math.round(4 * density);
            if (availW <= Math.round(48 * density)) return;

            float minPx = 10.0f * density;
            float stepPx = 0.5f * density;
            float curModePx = origModePx;
            float curBlendPx = origBlendPx;
            TextPaint modePaint = new TextPaint(tvMode.getPaint());
            TextPaint blendPaint = new TextPaint(tvBlend.getPaint());

            while (curModePx > minPx || curBlendPx > minPx) {
                modePaint.setTextSize(curModePx);
                blendPaint.setTextSize(curBlendPx);
                float totalW = modePaint.measureText(modeTxt) + blendPaint.measureText(blendTxt);
                if (totalW <= availW) break;
                if (curModePx > minPx) curModePx = Math.max(minPx, curModePx - stepPx);
                if (curBlendPx > minPx) curBlendPx = Math.max(minPx, curBlendPx - stepPx);
            }

            tvMode.setTextSize(TypedValue.COMPLEX_UNIT_PX, curModePx);
            tvBlend.setTextSize(TypedValue.COMPLEX_UNIT_PX, curBlendPx);

            blendPaint.setTextSize(curBlendPx);
            int minBlendNeed = Math.min((int) Math.ceil(blendPaint.measureText(blendTxt)) + Math.round(4 * density),
                    Math.round(availW * 0.45f));
            int maxModeW = Math.max(Math.round(availW * 0.54f), availW - minBlendNeed);
            tvMode.setMaxWidth(maxModeW);
        } catch (Throwable ignored) {}
    }

    /**
     * Scoped geometric lock for icon-above-text buttons (icons <= 48dp, items <= 96dp).
     * Locks horizontal sibling rows onto equal icon container height and top baseline
     * so single-line and two-line wrapped labels never desynchronize vertical rhythm.
     */
    private static boolean isScrollContainer(View v) {
        if (v instanceof ScrollView) return true;
        return v.getClass().getName().endsWith("NestedScrollView");
    }

    private static boolean hasWeightedChildRow(LinearLayout parent) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child.getVisibility() == View.GONE) continue;
            ViewGroup.LayoutParams lp = child.getLayoutParams();
            if (lp instanceof LinearLayout.LayoutParams && ((LinearLayout.LayoutParams) lp).weight > 0f) {
                return true;
            }
        }
        return false;
    }

    private static boolean isInsideCanvasPopupGrid(View v) {
        if (v == null) return false;
        if (matchesId(v, "nsv_canvas", 0x7f090a13) || matchesId(v, "bg_more_funcation", 0x7f0901f4)) return true;
        ViewParent cur = v.getParent();
        for (int depth = 0; cur instanceof View && depth < 8; depth++) {
            View pv = (View) cur;
            if (matchesId(pv, "nsv_canvas", 0x7f090a13) || matchesId(pv, "bg_more_funcation", 0x7f0901f4)) {
                return true;
            }
            cur = cur.getParent();
        }
        return false;
    }

    private static boolean ensureScrollDescendantCanGrow(View start) {
        List<ViewGroup> intermediates = new ArrayList<>();
        boolean inCanvasGrid = isInsideCanvasPopupGrid(start);
        ViewParent cur = start.getParent();
        for (int depth = 0; cur instanceof ViewGroup && depth < 12; depth++) {
            ViewGroup vg = (ViewGroup) cur;
            if (isScrollContainer(vg)) {
                for (int i = 0; i < intermediates.size(); i++) {
                    ViewGroup inter = intermediates.get(i);
                    ViewGroup.LayoutParams lp = inter.getLayoutParams();
                    if (lp != null && lp.height > 0) {
                        if (!inCanvasGrid) {
                            inter.setMinimumHeight(Math.max(inter.getMinimumHeight(), lp.height));
                        } else if (inter.getMinimumHeight() > 0) {
                            inter.setMinimumHeight(0);
                        }
                        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                        inter.setLayoutParams(lp);
                    }
                }
                return true;
            }
            intermediates.add(vg);
            cur = vg.getParent();
        }
        return false;
    }

    public static void centerAllIconButtonsRecursively(View v) {
        if (v == null || !isCanvasPopupPolishActive()) return;
        try {
            if ("libre_settings_root".equals(v.getTag()) || isExportRowView(v)) {
                return;
            }
            float density = v.getResources().getDisplayMetrics().density;

            if (v instanceof LinearLayout) {
                LinearLayout row = (LinearLayout) v;
                if (row.getOrientation() == LinearLayout.HORIZONTAL) {
                    alignFrameSlotDrawableTopRow(row);
                    int visibleCount = 0;
                    List<LinearLayout> iconSiblings = new ArrayList<>();
                    for (int i = 0; i < row.getChildCount(); i++) {
                        View ch = row.getChildAt(i);
                        if (ch.getVisibility() != View.GONE) {
                            visibleCount++;
                            if (isIconAboveTextButton(ch, density)) {
                                iconSiblings.add((LinearLayout) ch);
                            }
                        }
                    }
                    if (iconSiblings.size() >= 2) {
                        if (row.isBaselineAligned()) {
                            row.setBaselineAligned(false);
                        }
                        int maxIconH = Math.round(24 * density);
                        int capIconH = Math.round(48 * density);
                        int maxOrigTopPad = 0;
                        boolean anyNeedsCenterCompensation = false;
                        boolean anyTwoLine = false;
                        int maxItemH = 0;
                        int rowW = row.getWidth();
                        int approxSlotW = (rowW > 0 && visibleCount > 0) ? (rowW / visibleCount) : 0;

                        for (LinearLayout item : iconSiblings) {
                            if (item.getPaddingTop() > maxOrigTopPad) {
                                maxOrigTopPad = item.getPaddingTop();
                            }
                            if ((item.getGravity() & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.CENTER_VERTICAL
                                    && item.getPaddingTop() == 0) {
                                anyNeedsCenterCompensation = true;
                            }
                            int itemH = item.getHeight();
                            if (itemH <= 0 && item.getLayoutParams() != null && item.getLayoutParams().height > 0) {
                                itemH = item.getLayoutParams().height;
                            }
                            if (itemH > maxItemH) maxItemH = itemH;

                            for (int j = 0; j < item.getChildCount(); j++) {
                                View ch = item.getChildAt(j);
                                if (ch instanceof ImageView && ch.getVisibility() != View.GONE) {
                                    ImageView iv = (ImageView) ch;
                                    int h = iv.getHeight();
                                    if (h <= 0 && iv.getLayoutParams() != null && iv.getLayoutParams().height > 0) {
                                        h = iv.getLayoutParams().height;
                                    }
                                    if (h <= 0 && iv.getDrawable() != null) {
                                        h = iv.getDrawable().getIntrinsicHeight();
                                    }
                                    if (h > maxIconH && h <= capIconH) {
                                        maxIconH = h;
                                    }
                                } else if (ch instanceof TextView && ch.getVisibility() != View.GONE) {
                                    TextView tv = (TextView) ch;
                                    if (tv.getLineCount() > 1
                                            || (tv.getLayout() != null && tv.getLayout().getLineCount() > 1)) {
                                        anyTwoLine = true;
                                    } else if (approxSlotW > 0 && tv.getText() != null) {
                                        float txtW = tv.getPaint().measureText(tv.getText().toString());
                                        if (txtW > (approxSlotW - Math.round(8 * density))) {
                                            anyTwoLine = true;
                                        }
                                    }
                                }
                            }
                        }

                        boolean unlockRowHeight = false;
                        boolean compactOversizedRow = false;
                        boolean inPopupContext = isInsideCanvasPopupGrid(row)
                                || hasBoundPopupWindow(row)
                                || hasBoundPopupWindow(v.getRootView());
                        if (inPopupContext && row.getLayoutParams() instanceof LinearLayout.LayoutParams) {
                            LinearLayout.LayoutParams rowLp = (LinearLayout.LayoutParams) row.getLayoutParams();
                            ViewParent p = row.getParent();
                            if (p instanceof LinearLayout && ((LinearLayout) p).getOrientation() == LinearLayout.VERTICAL) {
                                LinearLayout parentLl = (LinearLayout) p;
                                ViewGroup.LayoutParams parentLp = parentLl.getLayoutParams();
                                boolean constrainedRow = (rowLp.height == 0 && rowLp.weight > 0f)
                                        || (rowLp.height > 0 && rowLp.height < Math.round(74 * density));
                                boolean oversizedSingleLineRow = !anyTwoLine
                                        && rowLp.height > Math.round(68 * density)
                                        && (hasWeightedChildRow(parentLl)
                                                || Boolean.TRUE.equals(parentLl.getTag(TAG_ICON_ROW_UNIFORMED)));
                                if (constrainedRow) {
                                    boolean inScroll = ensureScrollDescendantCanGrow(parentLl);
                                    ViewParent gp = parentLl.getParent();
                                    boolean grandparentAllowsGrowth = inScroll
                                            || !(gp instanceof ViewGroup)
                                            || ((ViewGroup) gp).getLayoutParams() == null
                                            || ((ViewGroup) gp).getLayoutParams().height <= 0;
                                    boolean shouldUnlock = anyTwoLine
                                            || inScroll
                                            || (parentLp != null && parentLp.height == ViewGroup.LayoutParams.WRAP_CONTENT);
                                    if (shouldUnlock && parentLp != null && grandparentAllowsGrowth) {
                                        boolean inCanvasGrid = isInsideCanvasPopupGrid(parentLl);
                                        if (parentLp.height > 0) {
                                            if (!inCanvasGrid) {
                                                parentLl.setMinimumHeight(Math.max(parentLl.getMinimumHeight(), parentLp.height));
                                            } else if (parentLl.getMinimumHeight() > 0) {
                                                parentLl.setMinimumHeight(0);
                                            }
                                            parentLp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                                            parentLl.setLayoutParams(parentLp);
                                        }
                                        parentLl.setTag(TAG_ICON_ROW_UNIFORMED, Boolean.TRUE);
                                        int minRowH = Math.round((inCanvasGrid ? 44 : 62) * density);
                                        if (!inCanvasGrid && row.getMinimumHeight() < minRowH) {
                                            row.setMinimumHeight(minRowH);
                                        } else if (inCanvasGrid && row.getMinimumHeight() > minRowH) {
                                            row.setMinimumHeight(0);
                                        }
                                        rowLp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                                        rowLp.weight = 0f;
                                        row.setLayoutParams(rowLp);
                                        unlockRowHeight = true;
                                    }
                                } else if (oversizedSingleLineRow && ensureScrollDescendantCanGrow(parentLl)) {
                                    boolean inCanvasGrid = isInsideCanvasPopupGrid(parentLl);
                                    if (parentLp != null && parentLp.height > 0) {
                                        if (!inCanvasGrid) {
                                            parentLl.setMinimumHeight(Math.max(parentLl.getMinimumHeight(), parentLp.height));
                                        } else if (parentLl.getMinimumHeight() > 0) {
                                            parentLl.setMinimumHeight(0);
                                        }
                                        parentLp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                                        parentLl.setLayoutParams(parentLp);
                                    }
                                    parentLl.setTag(TAG_ICON_ROW_UNIFORMED, Boolean.TRUE);
                                    int minRowH = Math.round((inCanvasGrid ? 44 : 58) * density);
                                    if (!inCanvasGrid && row.getMinimumHeight() < minRowH) {
                                        row.setMinimumHeight(minRowH);
                                    } else if (inCanvasGrid && row.getMinimumHeight() > minRowH) {
                                        row.setMinimumHeight(0);
                                    }
                                    rowLp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                                    rowLp.weight = 0f;
                                    row.setLayoutParams(rowLp);
                                    unlockRowHeight = true;
                                    compactOversizedRow = true;
                                }
                            }
                        }

                        int uniformTopPad = maxOrigTopPad;
                        if (anyTwoLine) {
                            int compactPad = Math.round(5 * density);
                            uniformTopPad = uniformTopPad > 0 ? Math.min(uniformTopPad, compactPad) : compactPad;
                        } else if (compactOversizedRow) {
                            uniformTopPad = Math.round(8 * density);
                        } else if (anyNeedsCenterCompensation) {
                            int minPad = Math.round(8 * density);
                            if (maxItemH > maxIconH + Math.round(28 * density)) {
                                int calc = (maxItemH - maxIconH - Math.round(26 * density)) / 2;
                                uniformTopPad = Math.max(minPad, Math.min(calc, Math.round(14 * density)));
                            } else {
                                uniformTopPad = Math.max(uniformTopPad, minPad);
                            }
                        }

                        for (LinearLayout item : iconSiblings) {
                            item.setTag(TAG_ICON_ROW_UNIFORMED, Boolean.TRUE);
                            applyIconAboveTextButtonLock(
                                    item, maxIconH, uniformTopPad, density, anyTwoLine, unlockRowHeight);
                        }
                    }
                } else if (isIconAboveTextButton(row, density)
                        && !Boolean.TRUE.equals(row.getTag(TAG_ICON_ROW_UNIFORMED))) {
                    int iconH = 0;
                    boolean twoLineSingle = false;
                    for (int j = 0; j < row.getChildCount(); j++) {
                        View ch = row.getChildAt(j);
                        if (ch instanceof ImageView && ch.getVisibility() != View.GONE) {
                            ImageView iv = (ImageView) ch;
                            iconH = iv.getHeight();
                            if (iconH <= 0 && iv.getLayoutParams() != null && iv.getLayoutParams().height > 0) {
                                iconH = iv.getLayoutParams().height;
                            }
                            break;
                        }
                    }
                    for (int j = 0; j < row.getChildCount(); j++) {
                        View ch = row.getChildAt(j);
                        if (ch instanceof TextView && ch.getVisibility() != View.GONE) {
                            TextView tv = (TextView) ch;
                            if (tv.getLineCount() > 1
                                    || (tv.getLayout() != null && tv.getLayout().getLineCount() > 1)) {
                                twoLineSingle = true;
                            }
                        }
                    }
                    int topPad = row.getPaddingTop();
                    if (topPad == 0 && (row.getGravity() & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.CENTER_VERTICAL) {
                        topPad = Math.round((twoLineSingle ? 5 : 8) * density);
                    }
                    applyIconAboveTextButtonLock(row, iconH, topPad, density, twoLineSingle, false);
                }
            } else if (v instanceof RelativeLayout) {
                centerRelativeVerticalIconCard((RelativeLayout) v, density);
            } else if (v instanceof TextView && isPaperNameView((TextView) v)) {
                centerPaperTextureLabelIfApplicable((TextView) v);
            }

            if (v instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) v;
                int childCount = vg.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    centerAllIconButtonsRecursively(vg.getChildAt(i));
                }
            }
        } catch (Throwable ignored) {}
    }

    private static void alignFrameSlotDrawableTopRow(LinearLayout row) {
        try {
            int childCount = row.getChildCount();
            if (childCount < 3) return;
            int frameSlotCount = 0;
            for (int i = 0; i < childCount; i++) {
                View slot = row.getChildAt(i);
                if (slot.getVisibility() == View.GONE) continue;
                if (!(slot instanceof android.widget.FrameLayout)) {
                    return;
                }
                android.widget.FrameLayout fl = (android.widget.FrameLayout) slot;
                boolean hasTopDrawableText = false;
                for (int j = 0; j < fl.getChildCount(); j++) {
                    View ch = fl.getChildAt(j);
                    if (ch instanceof TextView) {
                        TextView tv = (TextView) ch;
                        Drawable[] cd = tv.getCompoundDrawables();
                        Drawable top = (cd != null && cd.length > 1) ? cd[1] : null;
                        if (top == null && Build.VERSION.SDK_INT >= 17) {
                            Drawable[] cdr = tv.getCompoundDrawablesRelative();
                            if (cdr != null && cdr.length > 1) top = cdr[1];
                        }
                        if (top != null) {
                            hasTopDrawableText = true;
                            break;
                        }
                    }
                }
                if (!hasTopDrawableText) {
                    return;
                }
                frameSlotCount++;
            }
            if (frameSlotCount < 3) return;

            if (row.isBaselineAligned()) {
                row.setBaselineAligned(false);
            }
            if ((row.getGravity() & Gravity.VERTICAL_GRAVITY_MASK) != Gravity.TOP) {
                row.setGravity((row.getGravity() & ~Gravity.VERTICAL_GRAVITY_MASK) | Gravity.TOP);
            }
            for (int i = 0; i < childCount; i++) {
                View slot = row.getChildAt(i);
                if (!(slot instanceof android.widget.FrameLayout)) continue;
                android.widget.FrameLayout fl = (android.widget.FrameLayout) slot;
                for (int j = 0; j < fl.getChildCount(); j++) {
                    View ch = fl.getChildAt(j);
                    if (!(ch instanceof TextView)) continue;
                    TextView tv = (TextView) ch;
                    ViewGroup.LayoutParams rawLp = tv.getLayoutParams();
                    if (rawLp instanceof android.widget.FrameLayout.LayoutParams) {
                        android.widget.FrameLayout.LayoutParams flp =
                                (android.widget.FrameLayout.LayoutParams) rawLp;
                        int targetGrav = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
                        if (flp.gravity != targetGrav) {
                            flp.gravity = targetGrav;
                            tv.setLayoutParams(flp);
                        }
                    }
                    int tvGrav = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
                    if (tv.getGravity() != tvGrav) {
                        tv.setGravity(tvGrav);
                    }
                    if (Build.VERSION.SDK_INT >= 17 && tv.getTextAlignment() != View.TEXT_ALIGNMENT_CENTER) {
                        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    private static void centerRelativeVerticalIconCard(RelativeLayout card, float density) {
        try {
            ImageView iconIv = null;
            TextView labelTv = null;
            int ivCount = 0;
            int tvCount = 0;
            for (int i = 0; i < card.getChildCount(); i++) {
                View ch = card.getChildAt(i);
                if (ch.getVisibility() == View.GONE) continue;
                if (ch instanceof ImageView) {
                    iconIv = (ImageView) ch;
                    ivCount++;
                } else if (ch instanceof TextView) {
                    labelTv = (TextView) ch;
                    tvCount++;
                }
            }
            if (ivCount != 1 || tvCount != 1 || iconIv == null || labelTv == null) {
                return;
            }
            int cardId = card.getId();
            int toolGridItemId = id(card, "item_paint_left_dialog_setting", 0x7f090647);
            boolean isToolGridCard = toolGridItemId != 0 && cardId == toolGridItemId;
            boolean stackedVertically = isToolGridCard
                    || (iconIv.getBottom() > 0 && labelTv.getTop() > 0
                    && iconIv.getBottom() <= labelTv.getTop() + Math.round(6 * density));
            if (!stackedVertically) {
                return;
            }
            // Confirm icon and text are not arranged horizontally via RelativeLayout RIGHT_OF / END_OF rules.
            if (labelTv.getLayoutParams() instanceof RelativeLayout.LayoutParams) {
                int[] rules = ((RelativeLayout.LayoutParams) labelTv.getLayoutParams()).getRules();
                if (rules[RelativeLayout.RIGHT_OF] != 0
                        || (Build.VERSION.SDK_INT >= 17 && rules[RelativeLayout.END_OF] != 0)) {
                    if (!isToolGridCard || labelTv.getTop() < iconIv.getBottom() - 4) {
                        return;
                    }
                }
            }
            if (isToolGridCard) {
                if (iconIv.getId() == View.NO_ID) {
                    int resolvedIconId = id(card, "icon_iv", 0x7f0905ee);
                    if (resolvedIconId != 0) {
                        iconIv.setId(resolvedIconId);
                    }
                }
                int iconId = iconIv.getId();
                card.setGravity((card.getGravity() & ~Gravity.VERTICAL_GRAVITY_MASK) | Gravity.TOP | Gravity.CENTER_HORIZONTAL);
                if (card.getPaddingTop() != 0 || card.getPaddingBottom() != 0) {
                    card.setPadding(card.getPaddingLeft(), 0, card.getPaddingRight(), 0);
                }
                ViewGroup.LayoutParams cardLp = card.getLayoutParams();
                int minCardH = Math.round(64 * density);
                if (cardLp != null && cardLp.height > 0 && cardLp.height < minCardH) {
                    cardLp.height = minCardH;
                    card.setLayoutParams(cardLp);
                }

                if (iconIv.getLayoutParams() instanceof RelativeLayout.LayoutParams) {
                    RelativeLayout.LayoutParams iLp = (RelativeLayout.LayoutParams) iconIv.getLayoutParams();
                    int[] iRules = iLp.getRules();
                    int topMarginPx = Math.round(6 * density);
                    boolean iChanged = iRules[RelativeLayout.ALIGN_PARENT_TOP] == 0
                            || iRules[RelativeLayout.CENTER_IN_PARENT] != 0
                            || iRules[RelativeLayout.CENTER_VERTICAL] != 0
                            || iLp.topMargin != topMarginPx
                            || iLp.bottomMargin != 0;
                    if (iChanged) {
                        iLp.addRule(RelativeLayout.CENTER_IN_PARENT, 0);
                        iLp.addRule(RelativeLayout.CENTER_VERTICAL, 0);
                        iLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                        iLp.addRule(RelativeLayout.CENTER_HORIZONTAL);
                        iLp.topMargin = topMarginPx;
                        iLp.bottomMargin = 0;
                        iconIv.setLayoutParams(iLp);
                    }
                }

                if (labelTv.getLayoutParams() instanceof RelativeLayout.LayoutParams) {
                    RelativeLayout.LayoutParams tLp = (RelativeLayout.LayoutParams) labelTv.getLayoutParams();
                    int[] tRules = tLp.getRules();
                    int labelTopGap = Math.round(2 * density);
                    boolean tChanged = tLp.width != ViewGroup.LayoutParams.MATCH_PARENT
                            || tLp.height != ViewGroup.LayoutParams.WRAP_CONTENT
                            || tRules[RelativeLayout.BELOW] != iconId
                            || tRules[RelativeLayout.CENTER_VERTICAL] != 0
                            || tRules[RelativeLayout.CENTER_IN_PARENT] != 0
                            || tRules[RelativeLayout.ALIGN_PARENT_BOTTOM] != 0
                            || tLp.topMargin != labelTopGap
                            || tLp.leftMargin != 0
                            || tLp.rightMargin != 0;
                    if (tChanged) {
                        tLp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                        tLp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                        tLp.addRule(RelativeLayout.CENTER_IN_PARENT, 0);
                        tLp.addRule(RelativeLayout.CENTER_VERTICAL, 0);
                        tLp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
                        tLp.addRule(RelativeLayout.BELOW, iconId);
                        tLp.addRule(RelativeLayout.CENTER_HORIZONTAL);
                        tLp.topMargin = labelTopGap;
                        tLp.bottomMargin = 0;
                        tLp.leftMargin = 0;
                        tLp.rightMargin = 0;
                        if (Build.VERSION.SDK_INT >= 17) {
                            tLp.setMarginStart(0);
                            tLp.setMarginEnd(0);
                        }
                        labelTv.setLayoutParams(tLp);
                    }
                }

                int padH = Math.round(3 * density);
                int padB = Math.round(2 * density);
                if (labelTv.getPaddingLeft() != padH || labelTv.getPaddingRight() != padH
                        || labelTv.getPaddingTop() != 0 || labelTv.getPaddingBottom() != padB) {
                    labelTv.setPadding(padH, 0, padH, padB);
                }
                TranslationEngine.ensureSafeVietnameseFontPadding(labelTv);
                int desiredGrav = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
                setGravityIfChanged(labelTv, desiredGrav);
                setTextAlignmentIfChanged(labelTv, View.TEXT_ALIGNMENT_CENTER);
                int cardInnerW = card.getWidth() - card.getPaddingLeft() - card.getPaddingRight();
                if (cardInnerW <= 0 && cardLp != null && cardLp.width > 0) {
                    cardInnerW = cardLp.width - card.getPaddingLeft() - card.getPaddingRight();
                }
                int w = cardInnerW > 0 ? cardInnerW : labelTv.getWidth();
                if (w > padH * 2) {
                    CharSequence cs = labelTv.getText();
                    if (cs != null && cs.length() > 0) {
                        Object origGridObj = labelTv.getTag(TAG_TOOL_GRID_ORIG_PX);
                        float origPx;
                        if (origGridObj instanceof Float) {
                            origPx = (Float) origGridObj;
                        } else {
                            origPx = labelTv.getTextSize();
                            labelTv.setTag(TAG_TOOL_GRID_ORIG_PX, origPx);
                        }
                        if (origPx > 0f) {
                            String str = cs.toString();
                            int availW = w - padH * 2;
                            Locale activeLoc = TranslationEngine.getActiveLocale();
                            Typeface activeTf = UIFontManager.getActiveTypeface(labelTv.getContext());
                            TypographyNormalizer.prepareTextViewForPipeline(
                                    labelTv, str, activeLoc, activeTf, TranslationEngine.isDynamicI18nEnabled());
                            TextPaint tp = TypographyNormalizer.getScratchTextPaint(labelTv.getPaint());
                            TypographyNormalizer.preparePaintForScript(tp, str, activeLoc, activeTf);
                            int sg = TypographyNormalizer.classifyScript(str);
                            boolean rtl = TypographyNormalizer.containsRtlChars(str);
                            boolean allowBreak = TypographyNormalizer.hasNaturalBreakToken(str)
                                    || TypographyNormalizer.allowsUnspacedLineBreak(str);
                            float floorPx = TypographyNormalizer.computeProtectedFloorPx(origPx, labelTv.getResources().getDisplayMetrics());
                            float spacingMult = labelTv.getLineSpacingMultiplier();
                            float spacingAdd = labelTv.getLineSpacingExtra();
                            float targetPx = origPx;
                            float targetScaleX = 1.0f;

                            tp.setTextSize(origPx);
                            tp.setTextScaleX(1.0f);
                            boolean fit1 = TypographyNormalizer.fitsSingleLineStrictly(
                                    str, tp, availW, spacingMult, spacingAdd, true, rtl);
                            boolean fit2 = false;
                            if (!fit1 && allowBreak) {
                                StaticLayout slOrig = TypographyNormalizer.buildStaticLayout(
                                        str, tp, availW, Layout.Alignment.ALIGN_CENTER,
                                        spacingMult, spacingAdd, true, 0, null, rtl);
                                if (slOrig.getLineCount() <= 2
                                        && TypographyNormalizer.getMaxLineWidth(slOrig) <= availW
                                        && !TypographyNormalizer.hasDisallowedWordSplit(slOrig, str, sg)) {
                                    fit2 = true;
                                }
                            }
                            if (!fit1 && !fit2) {
                                float curPx = origPx * 0.94f;
                                for (int step = 0; step < 8; step++) {
                                    if (step == 7 || curPx < floorPx) curPx = floorPx;
                                    tp.setTextSize(curPx);
                                    tp.setTextScaleX(1.0f);
                                    if (TypographyNormalizer.fitsSingleLineStrictly(
                                            str, tp, availW, spacingMult, spacingAdd, true, rtl)) {
                                        targetPx = curPx;
                                        fit1 = true;
                                        break;
                                    }
                                    if (allowBreak) {
                                        StaticLayout sl = TypographyNormalizer.buildStaticLayout(
                                                str, tp, availW, Layout.Alignment.ALIGN_CENTER,
                                                spacingMult, spacingAdd, true, 0, null, rtl);
                                        if (sl.getLineCount() <= 2
                                                && TypographyNormalizer.getMaxLineWidth(sl) <= availW
                                                && !TypographyNormalizer.hasDisallowedWordSplit(sl, str, sg)) {
                                            targetPx = curPx;
                                            fit2 = true;
                                            break;
                                        }
                                    }
                                    if (curPx <= floorPx + 0.05f) break;
                                    curPx = Math.max(floorPx, curPx * 0.94f);
                                }
                                if (!fit1 && !fit2) {
                                    targetPx = floorPx;
                                    if (sg == TypographyNormalizer.SCRIPT_LATIN_CYRILLIC_GREEK && !allowBreak) {
                                        tp.setTextSize(floorPx);
                                        float wf = TypographyNormalizer.measureHarfBuzzWidth(str, tp);
                                        float strictLimit = Math.max(1f, availW - 0.5f);
                                        if (wf > strictLimit && wf > 1f) {
                                            targetScaleX = Math.max(0.90f, Math.min(1.0f, strictLimit / wf));
                                        }
                                    }
                                }
                            }
                            int solvedMaxLines = (fit1 || !allowBreak) ? 1 : 2;
                            setMaxLinesIfChanged(labelTv, solvedMaxLines);
                            if (Build.VERSION.SDK_INT >= 23 && solvedMaxLines >= 2) {
                                if (labelTv.getBreakStrategy() != Layout.BREAK_STRATEGY_BALANCED) {
                                    labelTv.setBreakStrategy(Layout.BREAK_STRATEGY_BALANCED);
                                }
                                if (labelTv.getHyphenationFrequency() != Layout.HYPHENATION_FREQUENCY_NONE) {
                                    labelTv.setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE);
                                }
                            }
                            if (!fit1 && !fit2) {
                                setEllipsizeIfChanged(labelTv, TextUtils.TruncateAt.END);
                            } else if (labelTv.getEllipsize() != null) {
                                setEllipsizeIfChanged(labelTv, null);
                            }
                            if (Math.abs(labelTv.getTextScaleX() - targetScaleX) > 0.005f) {
                                labelTv.setTextScaleX(targetScaleX);
                            }
                            if (Math.abs(labelTv.getTextSize() - targetPx) >= 0.4f) {
                                labelTv.setTextSize(TypedValue.COMPLEX_UNIT_PX, targetPx);
                            }
                            if (cardLp != null && cardLp.height > 0) {
                                tp.setTextSize(targetPx);
                                tp.setTextScaleX(targetScaleX);
                                float lineH = TypographyNormalizer.measureFontMetricsLineHeight(tp);
                                int iconH = iconIv.getHeight() > 0
                                        ? iconIv.getHeight()
                                        : (iconIv.getLayoutParams() != null && iconIv.getLayoutParams().height > 0
                                                ? iconIv.getLayoutParams().height
                                                : Math.round(22 * density));
                                int neededH = Math.round(6 * density) + iconH + Math.round(2 * density)
                                        + (int) Math.ceil(solvedMaxLines * lineH) + padB + Math.round(2 * density);
                                int wantCardH = Math.max(minCardH, neededH);
                                if (cardLp.height < wantCardH) {
                                    cardLp.height = wantCardH;
                                    card.setLayoutParams(cardLp);
                                }
                            }
                        }
                    }
                } else {
                    setMaxLinesIfChanged(labelTv, 2);
                }
                return;
            }

            int desiredGrav = (labelTv.getGravity() & Gravity.VERTICAL_GRAVITY_MASK) | Gravity.CENTER_HORIZONTAL;
            if (labelTv.getGravity() != desiredGrav) {
                labelTv.setGravity(desiredGrav);
            }
            if (Build.VERSION.SDK_INT >= 17 && labelTv.getTextAlignment() != View.TEXT_ALIGNMENT_CENTER) {
                labelTv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            }
        } catch (Throwable ignored) {}
    }

    private static void compactBottomPadding(View view, int maxBottomPadPx) {
        if (view == null) return;
        if (view.getPaddingBottom() > maxBottomPadPx) {
            view.setPadding(
                    view.getPaddingLeft(),
                    view.getPaddingTop(),
                    view.getPaddingRight(),
                    maxBottomPadPx);
        }
        if (view.getMinimumHeight() > 0 && view instanceof ViewGroup) {
            view.setMinimumHeight(0);
        }
    }

    private static void ensureHeightWrapContent(View v) {
        if (v == null) return;
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        if (lp != null && lp.height != ViewGroup.LayoutParams.WRAP_CONTENT) {
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            v.setLayoutParams(lp);
        }
        if (v.getMinimumHeight() > 0) {
            v.setMinimumHeight(0);
        }
    }

    private static void setExactBottomPadding(View v, int padPx) {
        if (v == null) return;
        if (v.getPaddingBottom() != padPx) {
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), padPx);
        }
    }

    private static void capBottomMargin(View v, int maxBottomMarginPx) {
        if (v == null) return;
        ViewGroup.LayoutParams rawLp = v.getLayoutParams();
        if (rawLp instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) rawLp;
            if (mlp.bottomMargin > maxBottomMarginPx) {
                mlp.bottomMargin = maxBottomMarginPx;
                v.setLayoutParams(mlp);
            }
        }
    }

    private static void capTopPadding(View v, int maxTopPadPx) {
        if (v == null) return;
        if (v.getPaddingTop() > maxTopPadPx) {
            v.setPadding(v.getPaddingLeft(), maxTopPadPx, v.getPaddingRight(), v.getPaddingBottom());
        }
    }

    private static boolean isThreeButtonClusterRow(View v) {
        if (!(v instanceof ViewGroup)) return false;
        return findView(v, "save_draft_bgview", 0x7f090b71) != null
                || findView(v, "save_as_bgview", 0x7f090b6d) != null
                || findView(v, "fix_draft_bgview", 0x7f090513) != null;
    }

    private static void compactBgMoreFunctionGrid(View bgMore, float density) {
        if (bgMore == null) return;
        ensureHeightWrapContent(bgMore);
        capTopPadding(bgMore, Math.round(6 * density));
        capBottomMargin(bgMore, Math.round(4 * density));
        if (!(bgMore instanceof ViewGroup)) return;

        int maxRowTopPad = Math.round(6 * density);
        int maxCellBotPad = Math.round(2 * density);
        ArrayList<ViewGroup> containers = new ArrayList<>();
        containers.add((ViewGroup) bgMore);
        for (int idx = 0; idx < containers.size(); idx++) {
            ViewGroup vg = containers.get(idx);
            for (int i = 0; i < vg.getChildCount(); i++) {
                View ch = vg.getChildAt(i);
                if (ch.getVisibility() == View.GONE || !(ch instanceof ViewGroup)) continue;
                ViewGroup chVg = (ViewGroup) ch;
                if (isThreeButtonClusterRow(chVg)) {
                    ensureHeightWrapContent(chVg);
                    capBottomMargin(chVg, 0);
                } else if (isIconAboveTextButton(chVg, density)) {
                    ViewGroup.LayoutParams cellLp = chVg.getLayoutParams();
                    if (cellLp != null && cellLp.height > 0) {
                        cellLp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                        chVg.setLayoutParams(cellLp);
                    }
                    if (chVg.getMinimumHeight() > 0) {
                        chVg.setMinimumHeight(0);
                    }
                    compactBottomPadding(chVg, maxCellBotPad);
                    capBottomMargin(chVg, 0);
                } else {
                    ensureHeightWrapContent(chVg);
                    capTopPadding(chVg, maxRowTopPad);
                    compactBottomPadding(chVg, 0);
                    capBottomMargin(chVg, Math.round(4 * density));
                    containers.add(chVg);
                }
            }
        }
    }

    public static void bindHostPopupWindow(View contentView, PopupWindow pw) {
        if (contentView == null || pw == null) return;
        try {
            contentView.setTag(TAG_HOST_POPUP_WINDOW, new WeakReference<>(pw));
        } catch (Throwable ignored) {}
    }

    private static PopupWindow getBoundPopupWindow(View root) {
        if (root == null) return null;
        try {
            View cur = root;
            while (cur != null) {
                Object tag = cur.getTag(TAG_HOST_POPUP_WINDOW);
                if (tag instanceof WeakReference) {
                    Object obj = ((WeakReference<?>) tag).get();
                    if (obj instanceof PopupWindow) {
                        return (PopupWindow) obj;
                    }
                }
                ViewParent p = cur.getParent();
                cur = (p instanceof View) ? (View) p : null;
            }
        } catch (Throwable ignored) {}
        return null;
    }

    public static boolean centerPaperTextureLabelIfApplicable(TextView tv) {
        if (!isPaperNameView(tv)) return false;
        boolean mutated = false;
        try {
            TypographyNormalizer.BaselineSnapshot snap = TypographyNormalizer.captureBaseline(tv);
            if (!TranslationEngine.isTextAutoFitActive()) {
                ViewParent pOff = tv.getParent();
                if (pOff instanceof View) {
                    mutated |= TypographyNormalizer.restoreContainerBaseline((View) pOff);
                    if (((View) pOff).getParent() instanceof View) {
                        mutated |= TypographyNormalizer.restoreContainerBaseline((View) ((View) pOff).getParent());
                    }
                }
                mutated |= TypographyNormalizer.restoreBaseline(tv);
                tv.setTag(TAG_PAPER_NAME_CENTERED, null);
                return mutated;
            }
            float density = tv.getResources().getDisplayMetrics().density;
            int desiredGrav = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            if (tv.getGravity() != desiredGrav) {
                tv.setGravity(desiredGrav);
                mutated = true;
            }
            if (Build.VERSION.SDK_INT >= 17 && tv.getTextAlignment() != View.TEXT_ALIGNMENT_CENTER) {
                tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                mutated = true;
            }

            ViewParent vp = tv.getParent();
            boolean isWrapPaperCard = vp instanceof LinearLayout
                    && ((LinearLayout) vp).getLayoutParams() != null
                    && ((LinearLayout) vp).getLayoutParams().width == ViewGroup.LayoutParams.WRAP_CONTENT;
            if (isWrapPaperCard) {
                LinearLayout card = (LinearLayout) vp;
                TypographyNormalizer.captureContainerBaseline(card);
                int minCardPadH = Math.round(6f * density);
                if (card.getPaddingLeft() < minCardPadH || card.getPaddingRight() < minCardPadH) {
                    int nPadL = Math.max(card.getPaddingLeft(), minCardPadH);
                    int nPadR = Math.max(card.getPaddingRight(), minCardPadH);
                    card.setPadding(nPadL, card.getPaddingTop(), nPadR, card.getPaddingBottom());
                    mutated = true;
                }
                int thumbW = Math.round(66f * density);
                int thumbH = Math.round(66f * density);
                if (card.getChildCount() > 0 && card.getChildAt(0) != tv) {
                    View thumbBox = card.getChildAt(0);
                    if (thumbBox.getWidth() > 0) thumbW = thumbBox.getWidth();
                    if (thumbBox.getHeight() > 0) thumbH = thumbBox.getHeight();
                }
                int cardInnerMaxW = thumbW + Math.round(12f * density);
                int tvSafePad = Math.round(4f * density);
                int textAvailW = Math.max(1, cardInnerMaxW - 2 * tvSafePad);
                CharSequence cs = tv.getText();
                if (cs != null && cs.length() > 0) {
                    String str = cs.toString();
                    Locale activeLoc = TranslationEngine.getActiveLocale();
                    Typeface activeTf = UIFontManager.getActiveTypeface(tv.getContext());
                    mutated |= TypographyNormalizer.prepareTextViewForPipeline(
                            tv, str, activeLoc, activeTf, TranslationEngine.isDynamicI18nEnabled());
                    TypographyNormalizer.CandidatePlan plan = TypographyNormalizer.evaluateCandidates(
                            str,
                            snap != null && snap.origTextSize > 0f ? snap.origTextSize : tv.getTextSize(),
                            1.0f,
                            TypographyNormalizer.classifyScript(str),
                            TypographyNormalizer.ARCHETYPE_VERTICAL_ELASTIC_STRIP,
                            true,
                            textAvailW,
                            0,
                            Math.round(18f * density),
                            tv.getLineSpacingMultiplier(),
                            tv.getLineSpacingExtra(),
                            true,
                            tv.getPaint(),
                            activeLoc,
                            activeTf,
                            tv.getResources().getDisplayMetrics(),
                            tvSafePad,
                            tvSafePad);
                    mutated |= TypographyNormalizer.applySolvedPlanToTextView(
                            tv,
                            plan,
                            plan.targetPx,
                            tvSafePad,
                            tvSafePad,
                            true,
                            false,
                            cardInnerMaxW,
                            1,
                            "paperCard");
                    TextPaint measureTp = TypographyNormalizer.getScratchTextPaint(tv.getPaint());
                    TypographyNormalizer.preparePaintForScript(measureTp, str, activeLoc, activeTf);
                    measureTp.setTextSize(plan.targetPx);
                    float neededCardH = thumbH + Math.round(11f * density)
                            + 2f * TypographyNormalizer.measureFontMetricsLineHeight(measureTp)
                            + card.getPaddingTop() + Math.max(card.getPaddingBottom(), Math.round(8f * density));
                    if (card.getParent() instanceof View) {
                        View rv = (View) card.getParent();
                        TypographyNormalizer.ContainerHeightSnapshot rvSnap =
                                TypographyNormalizer.captureContainerBaseline(rv);
                        if (rvSnap != null && rvSnap.origLpHeight > 0) {
                            int wantRvH = Math.round(neededCardH + rv.getPaddingTop() + rv.getPaddingBottom());
                            ViewGroup.LayoutParams rvLp = rv.getLayoutParams();
                            if (rvLp != null && rvLp.height > 0 && rvLp.height < wantRvH) {
                                rvLp.height = wantRvH;
                                rv.setLayoutParams(rvLp);
                                mutated = true;
                            }
                        }
                    }
                }
            } else {
                int padL = tv.getPaddingLeft();
                int padR = tv.getPaddingRight();
                if (padL != padR) {
                    int sym = Math.min(padL, padR);
                    tv.setPadding(sym, tv.getPaddingTop(), sym, tv.getPaddingBottom());
                    mutated = true;
                }
                if (Build.VERSION.SDK_INT >= 17) {
                    int padS = tv.getPaddingStart();
                    int padE = tv.getPaddingEnd();
                    if (padS != padE) {
                        int symRel = Math.min(padS, padE);
                        tv.setPaddingRelative(symRel, tv.getPaddingTop(), symRel, tv.getPaddingBottom());
                        mutated = true;
                    }
                }
                mutated |= TranslationEngine.ensureSafeVietnameseFontPadding(tv);
            }

            ViewGroup.LayoutParams rawLp = tv.getLayoutParams();
            if (rawLp instanceof LinearLayout.LayoutParams) {
                LinearLayout.LayoutParams llp = (LinearLayout.LayoutParams) rawLp;
                boolean lpChanged = false;
                if (llp.gravity != Gravity.CENTER_HORIZONTAL) {
                    llp.gravity = Gravity.CENTER_HORIZONTAL;
                    lpChanged = true;
                }
                if (llp.leftMargin != llp.rightMargin) {
                    int symM = Math.min(llp.leftMargin, llp.rightMargin);
                    llp.leftMargin = symM;
                    llp.rightMargin = symM;
                    lpChanged = true;
                }
                if (Build.VERSION.SDK_INT >= 17 && llp.getMarginStart() != llp.getMarginEnd()) {
                    int symMs = Math.min(llp.getMarginStart(), llp.getMarginEnd());
                    llp.setMarginStart(symMs);
                    llp.setMarginEnd(symMs);
                    lpChanged = true;
                }
                if (lpChanged) {
                    tv.setLayoutParams(llp);
                    mutated = true;
                }
            }
            tv.setTag(TAG_PAPER_NAME_CENTERED, Boolean.TRUE);
        } catch (Throwable ignored) {}
        return mutated;
    }

    private static void applyRigidScrollState(final View sv, boolean contentFits) {
        if (sv == null) return;
        sv.setTag(TAG_SCROLL_CONTENT_FITS, contentFits ? Boolean.TRUE : Boolean.FALSE);
        if (contentFits) {
            if (sv.isVerticalScrollBarEnabled()) {
                sv.setVerticalScrollBarEnabled(false);
            }
            if (sv.getOverScrollMode() != View.OVER_SCROLL_NEVER) {
                sv.setOverScrollMode(View.OVER_SCROLL_NEVER);
            }
            if (Build.VERSION.SDK_INT >= 21 && sv.isNestedScrollingEnabled()) {
                sv.setNestedScrollingEnabled(false);
            }
            if (sv.getScrollY() != 0) {
                sv.scrollTo(0, 0);
            }
            if (!Boolean.TRUE.equals(sv.getTag(TAG_SCROLL_RIGID_LOCKED))) {
                sv.setTag(TAG_SCROLL_RIGID_LOCKED, Boolean.TRUE);
                sv.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
                    @Override
                    public void onScrollChanged() {
                        if (Boolean.TRUE.equals(sv.getTag(TAG_SCROLL_CONTENT_FITS)) && sv.getScrollY() != 0) {
                            sv.scrollTo(0, 0);
                        }
                    }
                });
            }
        }
    }

    public static void compactPaintSettingsScrollArea(View root) {
        if (root == null) return;
        try {
            float density = root.getResources().getDisplayMetrics().density;
            int exportCushionPx = Math.round(3 * density);
            int settingFallbackCushionPx = Math.round(8 * density);
            int outerCapPx = Math.round(6 * density);

            View nsvCloud = findView(root, "nsv_cloud", 0x7f090a14);
            if (nsvCloud != null && nsvCloud.getVisibility() == View.GONE) {
                View cloudInclude = findView(nsvCloud, "layout_cloud_content", 0x7f090760);
                if (cloudInclude != null && cloudInclude.getVisibility() != View.GONE) {
                    cloudInclude.setVisibility(View.GONE);
                }
            }

            View nsvCanvas = findView(root, "nsv_canvas", 0x7f090a13);
            if (nsvCanvas != null) {
                compactBottomPadding(nsvCanvas, 0);
                if (nsvCanvas instanceof ViewGroup && ((ViewGroup) nsvCanvas).getChildCount() > 0) {
                    View canvasChild = ((ViewGroup) nsvCanvas).getChildAt(0);
                    ensureHeightWrapContent(canvasChild);
                    compactBottomPadding(canvasChild, 0);
                    capBottomMargin(canvasChild, 0);
                }
                View draftInfo = findView(nsvCanvas, "ll_draft_info", 0x7f090828);
                if (draftInfo == null) {
                    draftInfo = findView(nsvCanvas, "layout_bottom", 0x7f09075d);
                }
                if (draftInfo != null) {
                    int symPad = Math.round(10 * density);
                    if (draftInfo.getPaddingTop() != symPad || draftInfo.getPaddingBottom() != symPad) {
                        draftInfo.setPadding(draftInfo.getPaddingLeft(), symPad, draftInfo.getPaddingRight(), symPad);
                    }
                    capBottomMargin(draftInfo, outerCapPx);
                }
                View bgMore = findView(nsvCanvas, "bg_more_funcation", 0x7f0901f4);
                if (bgMore != null) {
                    View cWalker = bgMore;
                    while (cWalker != null && cWalker != nsvCanvas && cWalker.getParent() instanceof View) {
                        View parentV = (View) cWalker.getParent();
                        if (parentV != nsvCanvas) {
                            ensureHeightWrapContent(parentV);
                            compactBottomPadding(parentV, 0);
                            capBottomMargin(parentV, 0);
                        }
                        cWalker = parentV;
                    }
                    compactBgMoreFunctionGrid(bgMore, density);
                    setExactBottomPadding(bgMore, Math.round(10 * density));
                    capBottomMargin(bgMore, 0);
                }
                normalizeThreeButtonCluster(nsvCanvas);
                if (!nsvCanvas.canScrollVertically(1) && !nsvCanvas.canScrollVertically(-1)) {
                    applyRigidScrollState(nsvCanvas, true);
                }
            }

            View nsvImport = findView(root, "nsv_import", 0x7f090a15);
            if (nsvImport != null) {
                compactBottomPadding(nsvImport, 0);
                View layoutShare = findView(nsvImport, "layout_share", 0);
                View exportRoot = findView(nsvImport, "contentView", 0x7f0903cd);
                View exportBg = findView(nsvImport, "content_bg_view", 0x7f0903ce);
                ensureHeightWrapContent(layoutShare);
                ensureHeightWrapContent(exportRoot);
                ensureHeightWrapContent(exportBg);
                capBottomMargin(layoutShare, outerCapPx);
                capBottomMargin(exportRoot, outerCapPx);
                capBottomMargin(exportBg, outerCapPx);
                View child0 = (nsvImport instanceof ViewGroup && ((ViewGroup) nsvImport).getChildCount() > 0)
                        ? ((ViewGroup) nsvImport).getChildAt(0)
                        : null;
                ensureHeightWrapContent(child0);
                capBottomMargin(child0, outerCapPx);
                View walker = exportBg;
                while (walker != null && walker != nsvImport && walker.getParent() instanceof View) {
                    View parentV = (View) walker.getParent();
                    if (parentV != nsvImport) {
                        ensureHeightWrapContent(parentV);
                        compactBottomPadding(parentV, 0);
                        capBottomMargin(parentV, outerCapPx);
                    }
                    walker = parentV;
                }
                if (layoutShare != null && layoutShare != exportBg) {
                    compactBottomPadding(layoutShare, 0);
                }
                if (exportBg != null) {
                    if (exportRoot != null && exportRoot != exportBg) {
                        compactBottomPadding(exportRoot, 0);
                    }
                    if (child0 != null && child0 != exportBg) {
                        compactBottomPadding(child0, 0);
                    }
                    setExactBottomPadding(exportBg, exportCushionPx);
                } else if (child0 != null) {
                    setExactBottomPadding(child0, exportCushionPx);
                } else if (layoutShare != null) {
                    setExactBottomPadding(layoutShare, exportCushionPx);
                } else if (exportRoot != null) {
                    setExactBottomPadding(exportRoot, exportCushionPx);
                }
            }

            View nsvSetting = findView(root, "nsv_setting", 0x7f090a17);
            View settingRoot = nsvSetting != null
                    ? findView(nsvSetting, "contentView", 0x7f0903cd)
                    : findView(root, "contentView", 0x7f0903cd);
            View burSeek = nsvSetting != null
                    ? findView(nsvSetting, "bur_seek_bar", 0x7f0902ec)
                    : findView(root, "bur_seek_bar", 0x7f0902ec);
            boolean hasVisibleSeekBottom = burSeek != null && burSeek.getVisibility() == View.VISIBLE;
            if (burSeek != null) {
                View settingName = findView(burSeek, "setting_name", 0);
                if (settingName instanceof TextView) {
                    TranslationEngine.ensureSafeVietnameseFontPadding((TextView) settingName);
                }
                compactBottomPadding(burSeek, 0);
                capBottomMargin(burSeek, 0);
            }

            if (nsvSetting != null) {
                compactBottomPadding(nsvSetting, 0);
                if (nsvSetting instanceof ViewGroup && ((ViewGroup) nsvSetting).getChildCount() > 0) {
                    View settingChild = ((ViewGroup) nsvSetting).getChildAt(0);
                    ensureHeightWrapContent(settingChild);
                    compactBottomPadding(settingChild, hasVisibleSeekBottom ? outerCapPx : settingFallbackCushionPx);
                    capBottomMargin(settingChild, 0);
                }
                ensureHeightWrapContent(settingRoot);
                compactBottomPadding(settingRoot, 0);
                capBottomMargin(settingRoot, 0);
            } else if (settingRoot != null) {
                compactBottomPadding(settingRoot, 0);
            }

            View innerScroll = nsvSetting != null
                    ? findView(nsvSetting, "scroll_view", 0x7f090b87)
                    : findView(root, "scroll_view", 0x7f090b87);
            if (innerScroll != null) {
                if (nsvSetting != null) {
                    View sWalker = innerScroll;
                    while (sWalker != null && sWalker != nsvSetting && sWalker.getParent() instanceof View) {
                        View parentV = (View) sWalker.getParent();
                        if (parentV != nsvSetting) {
                            ensureHeightWrapContent(parentV);
                            compactBottomPadding(parentV, 0);
                            capBottomMargin(parentV, 0);
                        }
                        sWalker = parentV;
                    }
                    ensureHeightWrapContent(innerScroll);
                }
                compactBottomPadding(innerScroll, 0);
                capBottomMargin(innerScroll, hasVisibleSeekBottom ? outerCapPx : 0);
                if (innerScroll instanceof ViewGroup && ((ViewGroup) innerScroll).getChildCount() > 0) {
                    View innerChild = ((ViewGroup) innerScroll).getChildAt(0);
                    ensureHeightWrapContent(innerChild);
                    setExactBottomPadding(innerChild, hasVisibleSeekBottom ? 0 : settingFallbackCushionPx);
                }
                if (Boolean.TRUE.equals(innerScroll.getTag(TAG_SCROLL_CONTENT_FITS))
                        || (!innerScroll.canScrollVertically(1) && !innerScroll.canScrollVertically(-1))) {
                    applyRigidScrollState(innerScroll, true);
                }
            }
            if (nsvSetting != null
                    && (Boolean.TRUE.equals(nsvSetting.getTag(TAG_SCROLL_CONTENT_FITS))
                            || (!nsvSetting.canScrollVertically(1) && !nsvSetting.canScrollVertically(-1)))) {
                applyRigidScrollState(nsvSetting, true);
            }
        } catch (Throwable ignored) {}
    }

    private static int measureHeaderHeightBeforeScroll(ViewGroup pv, View scrollTarget, int availW) {
        if (pv == null) return 0;
        int headerH = 0;
        View tabCanvas = findView(pv, "ll_all_setting_canvas", 0x7f09081a);
        if (tabCanvas != null && tabCanvas.getParent() == pv && tabCanvas.getVisibility() != View.GONE) {
            ViewGroup.LayoutParams tLp = tabCanvas.getLayoutParams();
            int twSpec = View.MeasureSpec.makeMeasureSpec(Math.max(1, availW / 3), View.MeasureSpec.AT_MOST);
            int thSpec = (tLp != null && tLp.height > 0)
                    ? View.MeasureSpec.makeMeasureSpec(tLp.height, View.MeasureSpec.EXACTLY)
                    : View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            tabCanvas.measure(twSpec, thSpec);
            headerH = tabCanvas.getMeasuredHeight();
            if (tLp instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) tLp;
                headerH += mlp.topMargin + mlp.bottomMargin;
            }
            return headerH;
        }
        if (pv instanceof LinearLayout) {
            for (int i = 0; i < pv.getChildCount(); i++) {
                View sib = pv.getChildAt(i);
                if (sib == scrollTarget) break;
                if (sib.getVisibility() == View.GONE) continue;
                ViewGroup.LayoutParams sLp = sib.getLayoutParams();
                int swSpec = View.MeasureSpec.makeMeasureSpec(Math.max(1, availW), View.MeasureSpec.AT_MOST);
                int shSpec = (sLp != null && sLp.height > 0)
                        ? View.MeasureSpec.makeMeasureSpec(sLp.height, View.MeasureSpec.EXACTLY)
                        : View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
                sib.measure(swSpec, shSpec);
                headerH += sib.getMeasuredHeight();
                if (sLp instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) sLp;
                    headerH += mlp.topMargin + mlp.bottomMargin;
                }
            }
        }
        return headerH;
    }

    public static void resizePaintSettingsPopup(View root) {
        if (root == null) return;
        try {
            View bubbleView = findView(root, "bubbleView", 0x7f0902eb);
            if (bubbleView == null) return;
            View nsvCanvas = findView(root, "nsv_canvas", 0x7f090a13);
            View nsvImport = findView(root, "nsv_import", 0x7f090a15);
            View nsvSetting = findView(root, "nsv_setting", 0x7f090a17);
            View burSeek = findView(root, "bur_seek_bar", 0x7f0902ec);
            View standaloneScroll = findView(root, "scroll_view", 0x7f090b87);

            View activeScroll = null;
            if (nsvCanvas != null && nsvCanvas.getVisibility() == View.VISIBLE) {
                activeScroll = nsvCanvas;
            } else if (nsvImport != null && nsvImport.getVisibility() == View.VISIBLE) {
                activeScroll = nsvImport;
            } else if (nsvSetting != null && nsvSetting.getVisibility() == View.VISIBLE) {
                activeScroll = nsvSetting;
            } else if (standaloneScroll != null && standaloneScroll.getVisibility() == View.VISIBLE
                    && burSeek != null && burSeek.getVisibility() == View.VISIBLE) {
                activeScroll = standaloneScroll;
            }
            if (!(activeScroll instanceof ViewGroup) || ((ViewGroup) activeScroll).getChildCount() == 0) {
                return;
            }

            View contentChild = ((ViewGroup) activeScroll).getChildAt(0);
            if (contentChild == null) return;

            PopupWindow pw = getBoundPopupWindow(root);
            int pwBgPadH = 0;
            int pwBgPadV = 0;
            if (pw != null && pw.getBackground() != null) {
                Rect bgRect = new Rect();
                if (pw.getBackground().getPadding(bgRect)) {
                    pwBgPadH = bgRect.left + bgRect.right;
                    pwBgPadV = bgRect.top + bgRect.bottom;
                }
            }

            int displayW = root.getResources().getDisplayMetrics().widthPixels;
            int displayH = root.getResources().getDisplayMetrics().heightPixels;
            int rootW = root.getWidth();
            if (rootW <= 0 && pw != null && pw.getWidth() > 0) {
                rootW = Math.max(0, pw.getWidth() - pwBgPadH);
            }
            if (rootW <= 0 && root.getLayoutParams() != null && root.getLayoutParams().width > 0) {
                rootW = root.getLayoutParams().width;
            }
            if (rootW <= 0) {
                root.measure(
                        View.MeasureSpec.makeMeasureSpec(displayW, View.MeasureSpec.AT_MOST),
                        View.MeasureSpec.makeMeasureSpec(displayH, View.MeasureSpec.AT_MOST)
                );
                rootW = root.getMeasuredWidth();
            }
            if (rootW <= 0) return;

            int availW = 0;
            if (activeScroll.getWidth() > 0) {
                availW = activeScroll.getWidth() - activeScroll.getPaddingLeft() - activeScroll.getPaddingRight();
            } else {
                int sidePad = 0;
                View cur = activeScroll;
                while (cur != null && cur != root) {
                    sidePad += cur.getPaddingLeft() + cur.getPaddingRight();
                    if (cur.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                        ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) cur.getLayoutParams();
                        sidePad += mlp.leftMargin + mlp.rightMargin;
                    }
                    ViewParent p = cur.getParent();
                    if (p instanceof View) {
                        cur = (View) p;
                    } else {
                        break;
                    }
                }
                sidePad += root.getPaddingLeft() + root.getPaddingRight();
                availW = rootW - sidePad;
            }
            if (contentChild.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams cMlp = (ViewGroup.MarginLayoutParams) contentChild.getLayoutParams();
                availW -= cMlp.leftMargin + cMlp.rightMargin;
            }
            if (availW <= 0) return;

            int topChrome = 0;
            int botChrome = 0;
            View decor = root.getRootView();
            boolean hasLiveBounds = activeScroll.getHeight() > 0 && decor != null && decor.getHeight() > 0;
            if (hasLiveBounds) {
                View walk = activeScroll;
                while (walk != null && walk != decor) {
                    ViewParent p = walk.getParent();
                    if (!(p instanceof View)) break;
                    View pv = (View) p;
                    topChrome += walk.getTop();
                    if (pv.getHeight() > 0 && walk.getBottom() > 0) {
                        botChrome += Math.max(0, pv.getHeight() - walk.getBottom());
                    }
                    walk = pv;
                }
            } else {
                botChrome = root.getPaddingBottom();
                View walk = activeScroll;
                while (walk != null && walk != root) {
                    ViewParent p = walk.getParent();
                    if (p instanceof ViewGroup) {
                        ViewGroup pv = (ViewGroup) p;
                        topChrome += pv.getPaddingTop();
                        if (walk.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) walk.getLayoutParams();
                            topChrome += mlp.topMargin;
                            botChrome += mlp.bottomMargin;
                        }
                        topChrome += measureHeaderHeightBeforeScroll(pv, walk, rootW);
                        if (pv != root) {
                            botChrome += pv.getPaddingBottom();
                        }
                        if (walk.getBackground() != null) {
                            Rect bgR = new Rect();
                            if (walk.getBackground().getPadding(bgR)) {
                                if (walk.getPaddingTop() == 0) topChrome += bgR.top;
                                if (walk.getPaddingBottom() == 0) botChrome += bgR.bottom;
                            }
                        }
                        walk = pv;
                    } else {
                        break;
                    }
                }
                if (topChrome <= 0 && root.getPaddingTop() > 0) {
                    topChrome = root.getPaddingTop();
                }
            }
            if (topChrome <= 0) return;

            float density = root.getResources().getDisplayMetrics().density;
            if (!hasLiveBounds && bubbleView.getPaddingTop() == 0
                    && topChrome < Math.round(76f * density)) {
                topChrome += Math.round(10f * density);
            }

            int curW = (pw != null && pw.getWidth() > 0) ? pw.getWidth() : (rootW + pwBgPadH);
            int curH = (pw != null && pw.getHeight() > 0) ? pw.getHeight() : (root.getHeight() + pwBgPadV);
            int childCount = (contentChild instanceof ViewGroup) ? ((ViewGroup) contentChild).getChildCount() : 0;
            int preHash = ((((((activeScroll.getId() * 31 + availW) * 31 + childCount) * 31 + curW)
                    * 31 + pwBgPadV) * 31 + topChrome) * 31 + botChrome);
            View firstChild = (contentChild instanceof ViewGroup && childCount > 0)
                    ? ((ViewGroup) contentChild).getChildAt(0) : null;
            if (firstChild != null) {
                preHash = preHash * 31 + firstChild.getVisibility();
            }
            Object prevPreObj = root.getTag(TAG_LAST_POPUP_PRE_SIG);
            Object prevDesiredObj = root.getTag(TAG_LAST_POPUP_DESIRED_H);
            int liveRootH = root.getHeight();
            int liveDecorH = (decor != null && decor.getHeight() > 0) ? decor.getHeight() : (liveRootH + pwBgPadV);
            boolean scrollStillClipped = activeScroll.getHeight() > 0
                    && (activeScroll.canScrollVertically(1) || activeScroll.canScrollVertically(-1));
            if (prevPreObj instanceof Integer && ((Integer) prevPreObj) == preHash
                    && prevDesiredObj instanceof Integer && !scrollStillClipped) {
                int cachedDesiredH = (Integer) prevDesiredObj;
                boolean pwMatches = curH > 0 && Math.abs(curH - cachedDesiredH) <= 2;
                boolean rootMatches = liveDecorH <= 0 || Math.abs(liveDecorH - cachedDesiredH) <= 2;
                if (pwMatches && rootMatches) {
                    return;
                }
            }

            contentChild.measure(
                    View.MeasureSpec.makeMeasureSpec(availW, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            );
            int measuredInnerContentH = contentChild.getMeasuredHeight();
            View innerScroll = (activeScroll == nsvSetting)
                    ? findView(activeScroll, "scroll_view", 0x7f090b87) : null;
            if (innerScroll instanceof ViewGroup && ((ViewGroup) innerScroll).getChildCount() > 0) {
                View innerChild = ((ViewGroup) innerScroll).getChildAt(0);
                if (innerChild != null) {
                    int innerAvailW = Math.max(1, availW - innerScroll.getPaddingLeft() - innerScroll.getPaddingRight());
                    innerChild.measure(
                            View.MeasureSpec.makeMeasureSpec(innerAvailW, View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                    );
                    int innerNeededH = innerChild.getMeasuredHeight()
                            + innerScroll.getPaddingTop()
                            + innerScroll.getPaddingBottom();
                    if (innerChild.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                        ViewGroup.MarginLayoutParams iMlp = (ViewGroup.MarginLayoutParams) innerChild.getLayoutParams();
                        innerNeededH += iMlp.topMargin + iMlp.bottomMargin;
                    }
                    if (innerNeededH > measuredInnerContentH) {
                        measuredInnerContentH = innerNeededH;
                    }
                }
            }
            int measuredContentH = measuredInnerContentH
                    + activeScroll.getPaddingTop()
                    + activeScroll.getPaddingBottom()
                    + 4;
            if (contentChild.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams cMlp = (ViewGroup.MarginLayoutParams) contentChild.getLayoutParams();
                measuredContentH += cMlp.topMargin + cMlp.bottomMargin;
            }

            int desiredH = pwBgPadV + topChrome + measuredContentH + botChrome;
            int minH = Math.round(160 * density);
            int maxH = Math.round(displayH * 0.85f);
            boolean contentFits = desiredH <= maxH;
            desiredH = Math.max(minH, Math.min(desiredH, maxH));

            applyRigidScrollState(activeScroll, contentFits);
            if (innerScroll != null) {
                applyRigidScrollState(innerScroll, contentFits);
            }

            String sig = activeScroll.getId() + "|" + desiredH + "|" + curW + "|" + topChrome + "|" + botChrome;
            Object prevSig = root.getTag(TAG_LAST_POPUP_RESIZE_SIG);
            Object prevRootHObj = root.getTag(TAG_LAST_POPUP_ROOT_H);
            root.setTag(TAG_LAST_POPUP_PRE_SIG, Integer.valueOf(preHash));
            root.setTag(TAG_LAST_POPUP_DESIRED_H, Integer.valueOf(desiredH));
            if (sig.equals(prevSig)
                    && curH > 0 && Math.abs(curH - desiredH) <= 2
                    && (liveDecorH <= 0
                            || Math.abs(liveDecorH - desiredH) <= 2
                            || (prevRootHObj instanceof Integer && (Integer) prevRootHObj == liveRootH))) {
                maybeDumpLibreUiAudit(root, pw, pwBgPadV, topChrome, botChrome, measuredContentH, availW, desiredH);
                return;
            }
            root.setTag(TAG_LAST_POPUP_RESIZE_SIG, sig);
            root.setTag(TAG_LAST_POPUP_ROOT_H, liveRootH);

            if (pw != null) {
                if (pw.getHeight() <= 0 || Math.abs(pw.getHeight() - desiredH) > 2) {
                    pw.setHeight(desiredH);
                    if (pw.isShowing()) {
                        pw.update(curW > 0 ? curW : -1, desiredH);
                    }
                }
            } else {
                if (decor != null && decor.getLayoutParams() instanceof WindowManager.LayoutParams) {
                    WindowManager wm = (WindowManager) root.getContext().getSystemService(Context.WINDOW_SERVICE);
                    if (wm != null) {
                        WindowManager.LayoutParams wlp = (WindowManager.LayoutParams) decor.getLayoutParams();
                        if (wlp.height != desiredH) {
                            wlp.height = desiredH;
                            wm.updateViewLayout(decor, wlp);
                        }
                    }
                }
            }
            maybeDumpLibreUiAudit(root, pw, pwBgPadV, topChrome, botChrome, measuredContentH, availW, desiredH);
        } catch (Throwable ignored) {}
    }

    private static void maybeDumpLibreUiAudit(View root, PopupWindow pw, int pwBgPadV,
            int topC, int botC, int measH, int availW, int desH) {
        if (root == null || !Log.isLoggable("LIBRE_UI_AUDIT", Log.DEBUG)) return;
        try {
            StringBuilder sb = new StringBuilder(512);
            Rect bgRect = new Rect();
            if (pw != null && pw.getBackground() != null) {
                pw.getBackground().getPadding(bgRect);
            }
            sb.append("AUDIT pwW=").append(pw != null ? pw.getWidth() : 0)
              .append(" pwH=").append(pw != null ? pw.getHeight() : 0)
              .append(" bgPad=[").append(bgRect.left).append(",").append(bgRect.top)
              .append(",").append(bgRect.right).append(",").append(bgRect.bottom)
              .append("] padV=").append(pwBgPadV)
              .append(" topC=").append(topC).append(" botC=").append(botC)
              .append(" measH=").append(measH).append(" availW=").append(availW)
              .append(" desH=").append(desH)
              .append(" root=[").append(root.getLeft()).append(",").append(root.getTop())
              .append(",").append(root.getRight()).append(",").append(root.getBottom())
              .append("]");

            View b1 = findView(root, "save_draft_bgview", 0x7f090b71);
            View b2 = findView(root, "save_as_bgview", 0x7f090b6d);
            View b3 = findView(root, "fix_draft_bgview", 0x7f090513);
            View bCloud = findView(root, "cloud_draft_bgview", 0);
            if (bCloud != null) {
                sb.append(" cloudVis=").append(bCloud.getVisibility());
            }
            View anchor = b1 != null ? b1 : (b2 != null ? b2 : b3);
            if (anchor != null && anchor.getParent() instanceof View) {
                View row = (View) anchor.getParent();
                sb.append(" threeRow=[").append(row.getLeft()).append(",").append(row.getTop())
                  .append(",").append(row.getRight()).append(",").append(row.getBottom())
                  .append(" w=").append(row.getWidth()).append(" h=").append(row.getHeight())
                  .append(" pad=").append(row.getPaddingLeft()).append(",").append(row.getPaddingTop())
                  .append(",").append(row.getPaddingRight()).append(",").append(row.getPaddingBottom()).append("]");
            }
            int maxLblBottom = 0;
            View[] slots = new View[] { b1, b2, b3 };
            String[] slotNames = new String[] { "save", "saveAs", "fix" };
            for (int i = 0; i < slots.length; i++) {
                View slot = slots[i];
                if (slot == null) continue;
                sb.append(" ").append(slotNames[i]).append("=[").append(slot.getLeft()).append(",").append(slot.getTop())
                  .append(",").append(slot.getRight()).append(",").append(slot.getBottom())
                  .append(" w=").append(slot.getWidth()).append(" h=").append(slot.getHeight())
                  .append(" pad=").append(slot.getPaddingLeft()).append(",").append(slot.getPaddingTop())
                  .append(",").append(slot.getPaddingRight()).append(",").append(slot.getPaddingBottom()).append("]");
                if (slot instanceof ViewGroup) {
                    ViewGroup sg = (ViewGroup) slot;
                    for (int j = 0; j < sg.getChildCount(); j++) {
                        View c = sg.getChildAt(j);
                        if (c instanceof ImageView) {
                            int topM = (c.getLayoutParams() instanceof ViewGroup.MarginLayoutParams)
                                ? ((ViewGroup.MarginLayoutParams) c.getLayoutParams()).topMargin : 0;
                            sb.append(" ico=[").append(c.getLeft()).append(",").append(c.getTop())
                              .append(",").append(c.getRight()).append(",").append(c.getBottom())
                              .append(" tm=").append(topM).append("]");
                        } else if (c instanceof TextView) {
                            TextView tv = (TextView) c;
                            int topM = (tv.getLayoutParams() instanceof ViewGroup.MarginLayoutParams)
                                ? ((ViewGroup.MarginLayoutParams) tv.getLayoutParams()).topMargin : 0;
                            android.text.Layout lay = tv.getLayout();
                            int ellip = (lay != null && lay.getLineCount() > 0) ? lay.getEllipsisCount(0) : 0;
                            if (tv.getBottom() > maxLblBottom) maxLblBottom = tv.getBottom();
                            sb.append(" lbl=[").append(tv.getLeft()).append(",").append(tv.getTop())
                              .append(",").append(tv.getRight()).append(",").append(tv.getBottom())
                              .append(" tm=").append(topM).append(" grav=").append(tv.getGravity())
                              .append(" lc=").append(tv.getLineCount()).append(" sp=").append(tv.getTextSize())
                              .append(" ell=").append(ellip).append(" txt='").append(tv.getText()).append("']");
                        }
                    }
                }
            }
            View line = findView(root, "line", 0);
            if (line != null) {
                sb.append(" line=[").append(line.getLeft()).append(",").append(line.getTop())
                  .append(",").append(line.getRight()).append(",").append(line.getBottom())
                  .append(" gap=").append(line.getTop() - maxLblBottom).append("]");
            }
            String[] scrollNames = new String[] { "nsv_canvas", "nsv_setting", "scroll_view" };
            int[] scrollIds = new int[] { 0x7f090a13, 0x7f090a17, 0 };
            for (int i = 0; i < scrollNames.length; i++) {
                View sv = findView(root, scrollNames[i], scrollIds[i]);
                if (sv != null && sv.getVisibility() == View.VISIBLE) {
                    int chH = (sv instanceof ViewGroup && ((ViewGroup) sv).getChildCount() > 0)
                        ? ((ViewGroup) sv).getChildAt(0).getHeight() : 0;
                    sb.append(" ").append(scrollNames[i]).append("=[h=").append(sv.getHeight())
                      .append(" chH=").append(chH).append(" sy=").append(sv.getScrollY())
                      .append(" canUp=").append(sv.canScrollVertically(-1))
                      .append(" canDown=").append(sv.canScrollVertically(1))
                      .append(" os=").append(sv.getOverScrollMode())
                      .append(" bar=").append(sv.isVerticalScrollBarEnabled()).append("]");
                }
            }
            View sn = findView(root, "setting_name", 0);
            if (sn instanceof TextView && sn.getVisibility() == View.VISIBLE) {
                TextView stv = (TextView) sn;
                android.text.Layout slay = stv.getLayout();
                int sell = (slay != null && slay.getLineCount() > 0) ? slay.getEllipsisCount(0) : 0;
                sb.append(" settingName=[").append(stv.getLeft()).append(",").append(stv.getTop())
                  .append(",").append(stv.getRight()).append(",").append(stv.getBottom())
                  .append(" lc=").append(stv.getLineCount()).append(" sp=").append(stv.getTextSize())
                  .append(" ell=").append(sell).append(" txt='").append(stv.getText()).append("']");
            }
            String out = sb.toString();
            int hash = out.hashCode();
            Object prev = root.getTag(TAG_LAST_AUDIT_HASH);
            if (prev instanceof Integer && ((Integer) prev).intValue() == hash) {
                return;
            }
            root.setTag(TAG_LAST_AUDIT_HASH, Integer.valueOf(hash));
            Log.d("LIBRE_UI_AUDIT", out);
        } catch (Throwable ignored) {}
    }

    private static ViewGroup resolveDraftInfoContainer(View popupView) {
        if (popupView == null) return null;
        ViewGroup draftInfo = (ViewGroup) findView(popupView, "ll_draft_info", 0x7f090828);
        if (draftInfo != null) {
            return draftInfo;
        }
        View anchor = findView(popupView, "create_time", 0x7f090407);
        if (anchor == null) {
            anchor = findView(popupView, "canvas_size", 0x7f090320);
        }
        if (anchor != null && anchor.getParent() != null && anchor.getParent().getParent() instanceof LinearLayout) {
            return (ViewGroup) anchor.getParent().getParent();
        }
        return null;
    }

    public static void appendCanvasHsjPathRow(View popupView) {
        if (popupView == null || !isCanvasHsjPathRowActive()) return;
        try {
            ViewGroup draftInfo = resolveDraftInfoContainer(popupView);
            if (draftInfo == null || draftInfo.findViewWithTag("libre_canvas_hsj_path") != null) {
                return;
            }

            final android.content.Context ctx = draftInfo.getContext();
            float density = ctx.getResources().getDisplayMetrics().density;
            boolean isDark = (ctx.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
            int labelColor = isDark ? Color.parseColor("#A0A0A4") : Color.parseColor("#666666");
            int valueColor = isDark ? Color.parseColor("#E4E4E6") : Color.parseColor("#1F1F1F");
            float textSizePx = 12.0f * ctx.getResources().getDisplayMetrics().scaledDensity;
            int topPad = Math.round(6 * density);
            int bottomPad = Math.round(6 * density);
            int leftPad = 0;
            int rightPad = 0;

            int rowHeightPx = Math.round(21 * density);
            if (draftInfo.getChildCount() > 0 && draftInfo.getChildAt(0) instanceof ViewGroup) {
                ViewGroup sampleRow = (ViewGroup) draftInfo.getChildAt(0);
                leftPad = sampleRow.getPaddingLeft();
                rightPad = sampleRow.getPaddingRight();
                topPad = 0;
                bottomPad = 0;
                for (int c = 0; c < draftInfo.getChildCount(); c++) {
                    View existingRow = draftInfo.getChildAt(c);
                    existingRow.setLayoutParams(new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));
                    existingRow.setMinimumHeight(rowHeightPx);
                    existingRow.setPadding(
                            existingRow.getPaddingLeft(),
                            0,
                            existingRow.getPaddingRight(),
                            0);
                }
                for (int i = 0; i < sampleRow.getChildCount(); i++) {
                    View ch = sampleRow.getChildAt(i);
                    if (ch instanceof TextView) {
                        TextView tv = (TextView) ch;
                        if (isDraftInfoValueView(tv)) {
                            valueColor = tv.getCurrentTextColor();
                        } else {
                            labelColor = tv.getCurrentTextColor();
                            textSizePx = tv.getTextSize();
                        }
                    }
                }
            }

            final String realDraftDir = resolveActiveDraftDirectory(ctx);

            RelativeLayout row = new RelativeLayout(ctx);
            row.setTag("libre_canvas_hsj_path");
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            row.setMinimumHeight(rowHeightPx);
            row.setPadding(leftPad, topPad, rightPad, bottomPad);

            TextView tvVal = new TextView(ctx);
            int valId = SYNTHETIC_HSJ_PATH_VAL_ID;
            tvVal.setId(valId);
            tvVal.setText(formatShortDraftPath(realDraftDir));
            tvVal.setTextColor(valueColor);
            tvVal.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizePx);
            tvVal.setSingleLine(true);
            RelativeLayout.LayoutParams valLp = new RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            valLp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            if (Build.VERSION.SDK_INT >= 17) {
                valLp.addRule(RelativeLayout.ALIGN_PARENT_END);
            }
            valLp.addRule(RelativeLayout.CENTER_VERTICAL);
            tvVal.setLayoutParams(valLp);

            TextView tvLbl = new TextView(ctx);
            tvLbl.setText(TranslationEngine.get("libre_canvas_hsj_path_label", "工程目录 (.hsj)"));
            tvLbl.setTextColor(labelColor);
            tvLbl.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizePx);
            tvLbl.setSingleLine(true);
            tvLbl.setEllipsize(TextUtils.TruncateAt.END);
            RelativeLayout.LayoutParams lblLp = new RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            lblLp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            lblLp.addRule(RelativeLayout.LEFT_OF, valId);
            if (Build.VERSION.SDK_INT >= 17) {
                lblLp.addRule(RelativeLayout.ALIGN_PARENT_START);
                lblLp.addRule(RelativeLayout.START_OF, valId);
                lblLp.setMarginEnd(Math.round(12 * density));
            }
            lblLp.rightMargin = Math.round(12 * density);
            lblLp.addRule(RelativeLayout.CENTER_VERTICAL);
            tvLbl.setLayoutParams(lblLp);

            row.addView(tvVal);
            row.addView(tvLbl);
            row.setOnClickListener(v -> {
                try {
                    ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cm != null) {
                        cm.setPrimaryClip(ClipData.newPlainText("hsj_draft_path", realDraftDir));
                    }
                } catch (Throwable ignored) {}
                android.widget.Toast.makeText(
                        ctx,
                        realDraftDir,
                        android.widget.Toast.LENGTH_LONG).show();
            });
            draftInfo.addView(row);
        } catch (Throwable ignored) {}
    }

    public static void applyCanvasSettingPopupLayout(final View popupView) {
        if (popupView == null) return;
        try {
            appendCanvasHsjPathRow(popupView);

            ViewGroup draftInfo = resolveDraftInfoContainer(popupView);
            if (draftInfo != null && (isCanvasPopupPolishActive() || isCanvasHsjPathRowActive())) {
                float density = draftInfo.getResources().getDisplayMetrics().density;
                int rowCount = draftInfo.getChildCount();
                for (int i = 0; i < rowCount; i++) {
                    View row = draftInfo.getChildAt(i);
                    if (row instanceof ViewGroup) {
                        ViewGroup rowVg = (ViewGroup) row;
                        TextView tvLabel = null;
                        TextView tvValue = null;
                        int childCount = rowVg.getChildCount();
                        for (int j = 0; j < childCount; j++) {
                            View child = rowVg.getChildAt(j);
                            if (child instanceof TextView) {
                                TextView candidate = (TextView) child;
                                if (isDraftInfoValueView(candidate)) {
                                    tvValue = candidate;
                                } else {
                                    tvLabel = candidate;
                                }
                            }
                        }
                        if (tvValue == null && childCount >= 2) {
                            View c0 = rowVg.getChildAt(0);
                            View c1 = rowVg.getChildAt(1);
                            if (c0 instanceof TextView && c1 instanceof TextView) {
                                tvLabel = (TextView) c0;
                                tvValue = (TextView) c1;
                            }
                        }
                        if (tvLabel != null && tvValue != null) {
                            int valId = tvValue.getId();
                            if (valId == View.NO_ID || valId <= 0) {
                                valId = SYNTHETIC_HSJ_VAL_BASE + i;
                                tvValue.setId(valId);
                            }

                            if (rowVg instanceof RelativeLayout) {
                                RelativeLayout.LayoutParams lpVal = (RelativeLayout.LayoutParams) tvValue.getLayoutParams();
                                if (lpVal != null) {
                                    int[] rulesVal = lpVal.getRules();
                                    boolean changedVal = rulesVal[RelativeLayout.ALIGN_PARENT_RIGHT] == 0
                                            || rulesVal[RelativeLayout.RIGHT_OF] != 0
                                            || rulesVal[RelativeLayout.ALIGN_PARENT_LEFT] != 0
                                            || rulesVal[RelativeLayout.CENTER_VERTICAL] == 0
                                            || lpVal.leftMargin != 0
                                            || (Build.VERSION.SDK_INT >= 17 && (rulesVal[RelativeLayout.ALIGN_PARENT_END] == 0
                                                    || rulesVal[RelativeLayout.ALIGN_PARENT_START] != 0
                                                    || rulesVal[RelativeLayout.END_OF] != 0
                                                    || lpVal.getMarginStart() != 0));
                                    if (changedVal) {
                                        if (lpVal.leftMargin != 0) {
                                            lpVal.leftMargin = 0;
                                        }
                                        if (Build.VERSION.SDK_INT >= 17) {
                                            if (lpVal.getMarginStart() != 0) {
                                                lpVal.setMarginStart(0);
                                            }
                                            lpVal.removeRule(RelativeLayout.ALIGN_PARENT_START);
                                            lpVal.removeRule(RelativeLayout.ALIGN_PARENT_LEFT);
                                            lpVal.removeRule(RelativeLayout.RIGHT_OF);
                                            lpVal.removeRule(RelativeLayout.END_OF);
                                            lpVal.addRule(RelativeLayout.ALIGN_PARENT_END);
                                        } else {
                                            lpVal.addRule(RelativeLayout.ALIGN_PARENT_START, 0);
                                            lpVal.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0);
                                            lpVal.addRule(RelativeLayout.RIGHT_OF, 0);
                                        }
                                        lpVal.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                                        lpVal.addRule(RelativeLayout.CENTER_VERTICAL);
                                        tvValue.setLayoutParams(lpVal);
                                    }
                                    setGravityIfChanged(tvValue, Gravity.RIGHT | Gravity.CENTER_VERTICAL);
                                    setTextAlignmentIfChanged(tvValue, View.TEXT_ALIGNMENT_VIEW_END);
                                    setSingleLineIfChanged(tvValue, true);
                                    TranslationEngine.ensureSafeVietnameseFontPadding(tvValue);
                                }

                                RelativeLayout.LayoutParams lpLabel = (RelativeLayout.LayoutParams) tvLabel.getLayoutParams();
                                if (lpLabel != null) {
                                    boolean changedLabel = false;
                                    int safeMarginEnd = Math.round(12 * density);
                                    if (lpLabel.rightMargin != safeMarginEnd) {
                                        lpLabel.rightMargin = safeMarginEnd;
                                        changedLabel = true;
                                    }
                                    int[] rulesLbl = lpLabel.getRules();
                                    if (rulesLbl[RelativeLayout.CENTER_VERTICAL] == 0) {
                                        changedLabel = true;
                                    }
                                    if (Build.VERSION.SDK_INT >= 17) {
                                        if (lpLabel.getMarginEnd() != safeMarginEnd) {
                                            lpLabel.setMarginEnd(safeMarginEnd);
                                            changedLabel = true;
                                        }
                                        if (rulesLbl[RelativeLayout.ALIGN_PARENT_START] == 0
                                                || rulesLbl[RelativeLayout.START_OF] != valId) {
                                            changedLabel = true;
                                        }
                                    } else {
                                        if (rulesLbl[RelativeLayout.ALIGN_PARENT_LEFT] == 0
                                                || rulesLbl[RelativeLayout.LEFT_OF] != valId) {
                                            changedLabel = true;
                                        }
                                    }
                                    if (changedLabel) {
                                        if (Build.VERSION.SDK_INT >= 17) {
                                            lpLabel.removeRule(RelativeLayout.ALIGN_PARENT_END);
                                            lpLabel.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                                            lpLabel.addRule(RelativeLayout.ALIGN_PARENT_START);
                                            lpLabel.addRule(RelativeLayout.START_OF, valId);
                                        } else {
                                            lpLabel.addRule(RelativeLayout.ALIGN_PARENT_END, 0);
                                            lpLabel.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
                                        }
                                        lpLabel.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                                        lpLabel.addRule(RelativeLayout.LEFT_OF, valId);
                                        lpLabel.addRule(RelativeLayout.CENTER_VERTICAL);
                                        tvLabel.setLayoutParams(lpLabel);
                                    }
                                    setGravityIfChanged(tvLabel, Gravity.LEFT | Gravity.CENTER_VERTICAL);
                                    setTextAlignmentIfChanged(tvLabel, View.TEXT_ALIGNMENT_VIEW_START);
                                    setSingleLineIfChanged(tvLabel, true);
                                    setEllipsizeIfChanged(tvLabel, TextUtils.TruncateAt.END);
                                    TranslationEngine.ensureSafeVietnameseFontPadding(tvLabel);
                                }
                            }
                        }
                    }
                }
            }

            if (TranslationEngine.isDynamicI18nEnabled()) {
                TextView tvFlipHori = (TextView) findView(popupView, "canvas_fan_tv_hori", 0x7f09031c);
                if (tvFlipHori == null) {
                    View fanHori = findView(popupView, "fan_hori", 0x7f0904ec);
                    if (fanHori instanceof ViewGroup) {
                        for (int k = 0; k < ((ViewGroup) fanHori).getChildCount(); k++) {
                            View ch = ((ViewGroup) fanHori).getChildAt(k);
                            if (ch instanceof TextView) {
                                tvFlipHori = (TextView) ch;
                                break;
                            }
                        }
                    }
                }
                if (tvFlipHori != null) {
                    String targetHori = TranslationEngine.get("libre_canvas_flip_hori", "\u6c34\u5e73\u7ffb\u8f6c");
                    CharSequence curHori = tvFlipHori.getText();
                    if (curHori == null || !curHori.toString().equals(targetHori)) {
                        tvFlipHori.setText(targetHori);
                    }
                }

                TextView tvFlipVerti = (TextView) findView(popupView, "canvas_fan_tv_verti", 0x7f09031d);
                if (tvFlipVerti == null) {
                    View fanVerti = findView(popupView, "fan_vertical", 0x7f0904ed);
                    if (fanVerti instanceof ViewGroup) {
                        for (int k = 0; k < ((ViewGroup) fanVerti).getChildCount(); k++) {
                            View ch = ((ViewGroup) fanVerti).getChildAt(k);
                            if (ch instanceof TextView) {
                                tvFlipVerti = (TextView) ch;
                                break;
                            }
                        }
                    }
                }
                if (tvFlipVerti != null) {
                    String targetVerti = TranslationEngine.get("libre_canvas_flip_verti", "\u5782\u76f4\u7ffb\u8f6c");
                    CharSequence curVerti = tvFlipVerti.getText();
                    if (curVerti == null || !curVerti.toString().equals(targetVerti)) {
                        tvFlipVerti.setText(targetVerti);
                    }
                }
            }

            optimizePaintCloudViews(popupView);
            optimizeExportDialog(popupView);
            polishGestureSwitchRows(popupView);

            boolean polish = isCanvasPopupPolishActive();
            boolean autoFit = TranslationEngine.isTextAutoFitActive();
            if (!polish && (autoFit || GlobalCompatibilityController.isHideExportSocialIconsActive() || isHideCanvasPopupCloudActive())) {
                compactPaintSettingsScrollArea(popupView);
                resizePaintSettingsPopup(popupView);
            }
            if ((polish || autoFit || isHideCanvasPopupCloudActive() || GlobalCompatibilityController.isHideExportSocialIconsActive())
                    && popupView instanceof ViewGroup) {
                unclipContainerSafely((ViewGroup) popupView);
            }
        } catch (Throwable ignored) {}
    }

    public static void optimizeCanvasSettingPopup(final View popupView) {
        if (popupView == null) return;
        try {
            popupView.setTag(TAG_SETTING_POPUP_HOOKED, Boolean.TRUE);
            TranslationEngine.attachViewTreeObserver(popupView);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static void reflowCanvasSettingsTabs(View root) {
        if (root == null) return;
        try {
            View tabCanvas = findView(root, "ll_all_setting_canvas", 0x7f09081a);
            View tabShare = findView(root, "ll_all_setting_share", 0x7f09081c);
            View tabApp = findView(root, "ll_all_setting_app_setting", 0x7f090819);
            View tabCloud = findView(root, "ll_all_setting_cloud_setting", 0x7f09081b);
            if (tabCanvas == null || tabShare == null || tabApp == null) return;
            if (tabCloud != null && tabCloud.getVisibility() != View.GONE) return;
            int idTabCanvas = tabCanvas.getId();
            int idTabShare = tabShare.getId();
            int idTabApp = tabApp.getId();

            ViewParent parent = tabCanvas.getParent();
            if (parent == null) return;
            String pName = parent.getClass().getName();
            if ("androidx.constraintlayout.widget.ConstraintLayout".equals(pName)) {
                ViewGroup.LayoutParams cLp = tabCanvas.getLayoutParams();
                ViewGroup.LayoutParams sLp = tabShare.getLayoutParams();
                ViewGroup.LayoutParams aLp = tabApp.getLayoutParams();
                if (cLp == null || sLp == null || aLp == null) return;
                resolveConstraintFields(cLp.getClass());
                if (sConstraintLpClass == null || !sConstraintLpClass.isInstance(cLp)) return;

                boolean changedC = false;
                boolean changedS = false;
                boolean changedA = false;

                if (cLp.width != 0) { cLp.width = 0; changedC = true; }
                changedC |= setIntField(fStartToStart, cLp, 0);
                changedC |= setIntField(fStartToEnd, cLp, -1);
                changedC |= setIntField(fEndToStart, cLp, idTabShare);
                changedC |= setIntField(fEndToEnd, cLp, -1);
                changedC |= setIntField(fHoriChainStyle, cLp, 0);
                try {
                    if (fHoriWeight != null && Math.abs(fHoriWeight.getFloat(cLp) - 1.0f) > 0.01f) {
                        fHoriWeight.setFloat(cLp, 1.0f);
                        changedC = true;
                    }
                } catch (Throwable ignored) {}
                if (cLp instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) cLp;
                    if (mlp.leftMargin != 0 || mlp.rightMargin != 0) {
                        mlp.leftMargin = 0;
                        mlp.rightMargin = 0;
                        changedC = true;
                    }
                    if (Build.VERSION.SDK_INT >= 17 && (mlp.getMarginStart() != 0 || mlp.getMarginEnd() != 0)) {
                        mlp.setMarginStart(0);
                        mlp.setMarginEnd(0);
                        changedC = true;
                    }
                }

                if (sLp.width != 0) { sLp.width = 0; changedS = true; }
                changedS |= setIntField(fStartToStart, sLp, -1);
                changedS |= setIntField(fStartToEnd, sLp, idTabCanvas);
                changedS |= setIntField(fEndToStart, sLp, idTabApp);
                changedS |= setIntField(fEndToEnd, sLp, -1);
                try {
                    if (fHoriWeight != null && Math.abs(fHoriWeight.getFloat(sLp) - 1.0f) > 0.01f) {
                        fHoriWeight.setFloat(sLp, 1.0f);
                        changedS = true;
                    }
                } catch (Throwable ignored) {}

                if (aLp.width != 0) { aLp.width = 0; changedA = true; }
                changedA |= setIntField(fStartToStart, aLp, -1);
                changedA |= setIntField(fStartToEnd, aLp, idTabShare);
                changedA |= setIntField(fEndToStart, aLp, -1);
                changedA |= setIntField(fEndToEnd, aLp, 0);
                try {
                    if (fHoriWeight != null && Math.abs(fHoriWeight.getFloat(aLp) - 1.0f) > 0.01f) {
                        fHoriWeight.setFloat(aLp, 1.0f);
                        changedA = true;
                    }
                } catch (Throwable ignored) {}

                if (changedC) tabCanvas.setLayoutParams(cLp);
                if (changedS) tabShare.setLayoutParams(sLp);
                if (changedA) tabApp.setLayoutParams(aLp);
            } else if (parent instanceof LinearLayout) {
                LinearLayout tabRow = (LinearLayout) parent;
                tabRow.setWeightSum(3.0f);
                View[] tabs = new View[]{tabCanvas, tabShare, tabApp};
                for (View tab : tabs) {
                    if (tab.getLayoutParams() instanceof LinearLayout.LayoutParams) {
                        LinearLayout.LayoutParams llp = (LinearLayout.LayoutParams) tab.getLayoutParams();
                        if (llp.width != 0 || Math.abs(llp.weight - 1.0f) > 0.01f) {
                            llp.width = 0;
                            llp.weight = 1.0f;
                            tab.setLayoutParams(llp);
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    public static void normalizeThreeButtonCluster(View root) {
        if (root == null) return;
        try {
            View b1 = findView(root, "save_draft_bgview", 0x7f090b71);
            View b2 = findView(root, "save_as_bgview", 0x7f090b6d);
            View b3 = findView(root, "fix_draft_bgview", 0x7f090513);
            if (isHideCanvasPopupCloudActive()) {
                View bCloud = findView(root, "cloud_draft_bgview", 0);
                if (bCloud != null && bCloud.getVisibility() != View.GONE) {
                    bCloud.setVisibility(View.GONE);
                }
            }
            View anchor = b1 != null ? b1 : (b2 != null ? b2 : b3);
            if (anchor == null || !(anchor.getParent() instanceof LinearLayout)) return;
            LinearLayout row = (LinearLayout) anchor.getParent();
            float density = root.getResources().getDisplayMetrics().density;
            int padTopRow = Math.round(4f * density);
            int padBotRow = Math.round(12f * density);
            int padEdgeRow = Math.max(row.getPaddingLeft(), row.getPaddingRight());
            if (padEdgeRow <= 0) padEdgeRow = Math.round(4f * density);
            row.setBaselineAligned(false);
            row.setGravity(android.view.Gravity.TOP);
            if (row.getPaddingLeft() != padEdgeRow || row.getPaddingTop() != padTopRow
                    || row.getPaddingRight() != padEdgeRow || row.getPaddingBottom() != padBotRow) {
                row.setPadding(padEdgeRow, padTopRow, padEdgeRow, padBotRow);
            }

            int slotPadH = Math.round(2f * density);
            int slotPadTop = Math.round(6f * density);
            int slotPadBot = Math.round(4f * density);
            int iconSize = Math.round(24f * density);
            int lblTopM = Math.round(6f * density);

            int visibleWeighted = 0;
            float sum = 0f;
            int childCount = row.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View ch = row.getChildAt(i);
                if (ch.getVisibility() != View.GONE) {
                    visibleWeighted++;
                    sum += 1.0f;
                    if (ch.getLayoutParams() instanceof LinearLayout.LayoutParams) {
                        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) ch.getLayoutParams();
                        boolean changed = false;
                        if (lp.width != 0 || Math.abs(lp.weight - 1.0f) > 0.01f) {
                            lp.width = 0;
                            lp.weight = 1.0f;
                            changed = true;
                        }
                        if (lp.height != ViewGroup.LayoutParams.WRAP_CONTENT) {
                            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                            changed = true;
                        }
                        if (lp.gravity != android.view.Gravity.TOP) {
                            lp.gravity = android.view.Gravity.TOP;
                            changed = true;
                        }
                        if (lp.leftMargin != 0 || lp.rightMargin != 0 || lp.topMargin != 0 || lp.bottomMargin != 0) {
                            lp.setMargins(0, 0, 0, 0);
                            changed = true;
                        }
                        if (Build.VERSION.SDK_INT >= 17 && (lp.getMarginStart() != 0 || lp.getMarginEnd() != 0)) {
                            lp.setMarginStart(0);
                            lp.setMarginEnd(0);
                            changed = true;
                        }
                        if (changed) ch.setLayoutParams(lp);
                    }
                    if (ch.getPaddingLeft() != slotPadH || ch.getPaddingTop() != slotPadTop
                            || ch.getPaddingRight() != slotPadH || ch.getPaddingBottom() != slotPadBot) {
                        ch.setPadding(slotPadH, slotPadTop, slotPadH, slotPadBot);
                    }
                    if (ch instanceof LinearLayout) {
                        LinearLayout slot = (LinearLayout) ch;
                        slot.setTag(TAG_THREE_BTN_NORMALIZED, Boolean.TRUE);
                        slot.setGravity(android.view.Gravity.TOP | android.view.Gravity.CENTER_HORIZONTAL);
                        for (int j = 0; j < slot.getChildCount(); j++) {
                            View inner = slot.getChildAt(j);
                            if (inner instanceof ImageView) {
                                LinearLayout.LayoutParams ilp = inner.getLayoutParams() instanceof LinearLayout.LayoutParams
                                    ? (LinearLayout.LayoutParams) inner.getLayoutParams()
                                    : new LinearLayout.LayoutParams(iconSize, iconSize);
                                boolean ichg = false;
                                if (ilp.width != iconSize || ilp.height != iconSize) {
                                    ilp.width = iconSize;
                                    ilp.height = iconSize;
                                    ichg = true;
                                }
                                if (ilp.gravity != android.view.Gravity.CENTER_HORIZONTAL) {
                                    ilp.gravity = android.view.Gravity.CENTER_HORIZONTAL;
                                    ichg = true;
                                }
                                if (ilp.topMargin != 0 || ilp.bottomMargin != 0 || ilp.leftMargin != 0 || ilp.rightMargin != 0) {
                                    ilp.setMargins(0, 0, 0, 0);
                                    ichg = true;
                                }
                                if (ichg) inner.setLayoutParams(ilp);
                            } else if (inner instanceof TextView) {
                                TextView tv = (TextView) inner;
                                LinearLayout.LayoutParams tlp = tv.getLayoutParams() instanceof LinearLayout.LayoutParams
                                    ? (LinearLayout.LayoutParams) tv.getLayoutParams()
                                    : new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                boolean tchg = false;
                                if (tlp.width != ViewGroup.LayoutParams.MATCH_PARENT || tlp.height != ViewGroup.LayoutParams.WRAP_CONTENT) {
                                    tlp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                                    tlp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                                    tchg = true;
                                }
                                int desiredTGrav = android.view.Gravity.TOP | android.view.Gravity.CENTER_HORIZONTAL;
                                if (tlp.gravity != desiredTGrav) {
                                    tlp.gravity = desiredTGrav;
                                    tchg = true;
                                }
                                if (tlp.topMargin != lblTopM || tlp.bottomMargin != 0 || tlp.leftMargin != 0 || tlp.rightMargin != 0) {
                                    tlp.setMargins(0, lblTopM, 0, 0);
                                    tchg = true;
                                }
                                if (tchg) tv.setLayoutParams(tlp);
                                setGravityIfChanged(tv, desiredTGrav);
                                if (Build.VERSION.SDK_INT >= 17) {
                                    setTextAlignmentIfChanged(tv, View.TEXT_ALIGNMENT_CENTER);
                                }
                                setSingleLineIfChanged(tv, false);
                                setMaxLinesIfChanged(tv, 2);
                                setEllipsizeIfChanged(tv, null);
                                if (tv.getPaddingLeft() != slotPadH || tv.getPaddingTop() != 0
                                        || tv.getPaddingRight() != slotPadH || tv.getPaddingBottom() != 0) {
                                    tv.setPadding(slotPadH, 0, slotPadH, 0);
                                }
                                TranslationEngine.ensureSafeVietnameseFontPadding(tv);
                            }
                        }
                    }
                }
            }
            if (visibleWeighted > 0 && Math.abs(row.getWeightSum() - sum) > 0.01f) {
                row.setWeightSum(sum);
            }
        } catch (Throwable ignored) {}
    }

    public static void hideCanvasPopupCloudViews(View root) {
        if (root == null || !isHideCanvasPopupCloudActive()) return;
        optimizePaintCloudViews(root);
    }

    public static void optimizePaintCloudViews(View root) {
        if (root == null) return;
        try {
            boolean hidePopupCloud = isHideCanvasPopupCloudActive();

            if (hidePopupCloud) {
                View syncDraft = findView(root, "bg_sync_draft", 0x7f090213);
                if (syncDraft != null) {
                    hideViewCleanly(syncDraft);
                }
                View cloudBtn = findView(root, "cloud_draft_bgview", 0x7f09036f);
                if (cloudBtn != null) {
                    hideViewCleanly(cloudBtn);
                }
                View cloudTab = findView(root, "ll_all_setting_cloud_setting", 0x7f09081b);
                if (cloudTab != null) {
                    hideViewCleanly(cloudTab);
                }
                View nsvCloud = findView(root, "nsv_cloud", 0x7f090a14);
                if (nsvCloud != null && nsvCloud.getVisibility() != View.GONE) {
                    nsvCloud.setVisibility(View.GONE);
                }
                reflowCanvasSettingsTabs(root);
                normalizeThreeButtonCluster(root);
            }
        } catch (Throwable ignored) {}
    }

    public static void normalizeExportFormatRows(View root) {
        if (root == null) return;
        try {
            TextView tvPng = (TextView) findView(root, "tv_png", 0x7f090ee1);
            if (tvPng == null) return;

            int targetGravity = Gravity.START | Gravity.CENTER_VERTICAL;
            int marginLeft = 0;
            int marginRight = 0;
            int marginStart = 0;
            int marginEnd = 0;
            if (tvPng.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams aLp = (ViewGroup.MarginLayoutParams) tvPng.getLayoutParams();
                marginLeft = aLp.leftMargin;
                marginRight = aLp.rightMargin;
                if (Build.VERSION.SDK_INT >= 17) {
                    marginStart = aLp.getMarginStart();
                    marginEnd = aLp.getMarginEnd();
                }
            }
            int padLeft = tvPng.getPaddingLeft();
            int padRight = tvPng.getPaddingRight();
            int padStart = Build.VERSION.SDK_INT >= 17 ? tvPng.getPaddingStart() : padLeft;
            int padEnd = Build.VERSION.SDK_INT >= 17 ? tvPng.getPaddingEnd() : padRight;
            int drawablePad = tvPng.getCompoundDrawablePadding();

            for (int i = 0; i < EXPORT_ROW_TEXT_NAMES.length; i++) {
                String name = EXPORT_ROW_TEXT_NAMES[i];
                if ("tv_pdf".equals(name)) continue;
                View v = findView(root, name, EXPORT_ROW_TEXT_FALLBACKS[i]);
                if (!(v instanceof TextView)) continue;
                TextView tv = (TextView) v;
                tv.setTag(TAG_EXPORT_ROW_NORMALIZED, Boolean.TRUE);
                if (tv.getGravity() != targetGravity) {
                    tv.setGravity(targetGravity);
                }
                if (Build.VERSION.SDK_INT >= 17 && tv.getTextAlignment() != View.TEXT_ALIGNMENT_VIEW_START) {
                    tv.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
                }
                if (tv.getCompoundDrawablePadding() != drawablePad) {
                    tv.setCompoundDrawablePadding(drawablePad);
                }
                if (tv.getEllipsize() != null) {
                    tv.setEllipsize(null);
                }
                if (tv.getPaddingLeft() != padLeft || tv.getPaddingRight() != padRight) {
                    tv.setPadding(padLeft, tv.getPaddingTop(), padRight, tv.getPaddingBottom());
                }
                if (tv.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) tv.getLayoutParams();
                    boolean mChanged = false;
                    if (lp.leftMargin != marginLeft || lp.rightMargin != marginRight) {
                        lp.leftMargin = marginLeft;
                        lp.rightMargin = marginRight;
                        mChanged = true;
                    }
                    if (Build.VERSION.SDK_INT >= 17 && (lp.getMarginStart() != marginStart || lp.getMarginEnd() != marginEnd)) {
                        lp.setMarginStart(marginStart);
                        lp.setMarginEnd(marginEnd);
                        mChanged = true;
                    }
                    if (mChanged) tv.setLayoutParams(lp);
                }
            }

            View bgPdf = findView(root, "bg_pdf", 0x7f0901fb);
            TextView tvPdf = (TextView) findView(root, "tv_pdf", 0x7f090ed6);
            if (bgPdf instanceof LinearLayout) {
                LinearLayout llPdf = (LinearLayout) bgPdf;
                llPdf.setTag(TAG_EXPORT_ROW_NORMALIZED, Boolean.TRUE);
                if (llPdf.getGravity() != targetGravity) {
                    llPdf.setGravity(targetGravity);
                }
                if (llPdf.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) llPdf.getLayoutParams();
                    boolean mChanged = false;
                    if (lp.width != ViewGroup.LayoutParams.MATCH_PARENT) {
                        lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                        mChanged = true;
                    }
                    if (lp.leftMargin != marginLeft || lp.rightMargin != marginRight) {
                        lp.leftMargin = marginLeft;
                        lp.rightMargin = marginRight;
                        mChanged = true;
                    }
                    if (Build.VERSION.SDK_INT >= 17 && (lp.getMarginStart() != marginStart || lp.getMarginEnd() != marginEnd)) {
                        lp.setMarginStart(marginStart);
                        lp.setMarginEnd(marginEnd);
                        mChanged = true;
                    }
                    if (mChanged) llPdf.setLayoutParams(lp);
                }
                if (llPdf.getPaddingLeft() != 0 || llPdf.getPaddingRight() != 0) {
                    llPdf.setPadding(0, llPdf.getPaddingTop(), 0, llPdf.getPaddingBottom());
                }
            }
            if (tvPdf != null) {
                tvPdf.setTag(TAG_EXPORT_ROW_NORMALIZED, Boolean.TRUE);
                if (tvPdf.getGravity() != targetGravity) {
                    tvPdf.setGravity(targetGravity);
                }
                if (Build.VERSION.SDK_INT >= 17 && tvPdf.getTextAlignment() != View.TEXT_ALIGNMENT_VIEW_START) {
                    tvPdf.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
                }
                if (tvPdf.getCompoundDrawablePadding() != drawablePad) {
                    tvPdf.setCompoundDrawablePadding(drawablePad);
                }
                if (tvPdf.getEllipsize() != null) {
                    tvPdf.setEllipsize(null);
                }
                if (tvPdf.getPaddingLeft() != padLeft || tvPdf.getPaddingRight() != padRight) {
                    tvPdf.setPadding(padLeft, tvPdf.getPaddingTop(), padRight, tvPdf.getPaddingBottom());
                }
                if (Build.VERSION.SDK_INT >= 17 && (tvPdf.getPaddingStart() != padStart || tvPdf.getPaddingEnd() != padEnd)) {
                    tvPdf.setPaddingRelative(padStart, tvPdf.getPaddingTop(), padEnd, tvPdf.getPaddingBottom());
                }
                if (tvPdf.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) tvPdf.getLayoutParams();
                    boolean mChanged = false;
                    if (lp.leftMargin != 0) {
                        lp.leftMargin = 0;
                        mChanged = true;
                    }
                    if (Build.VERSION.SDK_INT >= 17 && lp.getMarginStart() != 0) {
                        lp.setMarginStart(0);
                        mChanged = true;
                    }
                    if (mChanged) tvPdf.setLayoutParams(lp);
                }
            }
            View ivPdfVip = findView(root, "iv_pdf_vip", 0x7f0906e8);
            if (ivPdfVip != null && OfflineAccountController.isOfflineProfileActive()) {
                ivPdfVip.setTag(TAG_EXPORT_ROW_NORMALIZED, Boolean.TRUE);
                if (ivPdfVip.getVisibility() != View.GONE) {
                    ivPdfVip.setVisibility(View.GONE);
                }
            }
        } catch (Throwable ignored) {}
    }

    private static final String[] EXPORT_SOCIAL_NAMES = {
        "recyclerView2", "recyclerview2_divider", "share_line",
        "share_bgview", "publish_bgview", "publish_anim_bgview"
    };
    private static final int[] EXPORT_SOCIAL_FALLBACKS = {
        0x7f090af0, 0x7f090afb, 0x7f090c02,
        0x7f090bfe, 0x7f090aae, 0x7f090aac
    };

    private static boolean isTimelapseExportView(View view) {
        return view != null && findView(view, "d3_1080_vip", 0x7f090429) != null;
    }

    /**
     * Optimizes Export Dialogs and Popups (ExportPictureView, ExportPictureView3, Publish/Export popups):
     * Hides domestic social share views cleanly and aligns export rows.
     */
    public static void optimizeExportDialog(View dialogView) {
        if (dialogView == null) return;
        try {
            boolean isTimelapse = isTimelapseExportView(dialogView);
            if (isCanvasPopupPolishActive()) {
                normalizeExportFormatRows(dialogView);
            }

            if (OfflineAccountController.isOfflineProfileActive()) {
                View ivPdfVip = findView(dialogView, "iv_pdf_vip", 0x7f0906e8);
                if (ivPdfVip != null && ivPdfVip.getVisibility() != View.GONE) {
                    ivPdfVip.setVisibility(View.GONE);
                }
                GlobalCompatibilityController.sanitizeExportView3(dialogView);
            }

            boolean hideSocial = isTimelapse
                    ? GlobalCompatibilityController.isHideTimelapseExportSocialActive()
                    : GlobalCompatibilityController.isHideExportSocialIconsActive();
            if (hideSocial) {
                boolean keepShareEntry = LibreConfig.isFeatureActive(LibreConfig.KEY_SYSTEM_SHARE);
                for (int i = 0; i < EXPORT_SOCIAL_NAMES.length; i++) {
                    if ("share_bgview".equals(EXPORT_SOCIAL_NAMES[i]) && keepShareEntry) {
                        continue;
                    }
                    View target = findView(dialogView, EXPORT_SOCIAL_NAMES[i], EXPORT_SOCIAL_FALLBACKS[i]);
                    if (target != null) {
                        hideViewCleanly(target);
                    }
                }
            }

            if (isCanvasPopupPolishActive()) {
                centerExportShareGridItems(dialogView);
            }

            if (dialogView instanceof ViewGroup) {
                unclipContainerSafely((ViewGroup) dialogView);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Optimizes timelapse video export view (ExportPictureView3) independently of ExportPictureView.
     */
    public static void optimizeTimelapseExportDialog(View view) {
        if (view == null) return;
        try {
            if (OfflineAccountController.isOfflineProfileActive()) {
                GlobalCompatibilityController.sanitizeExportView3(view);
            }
            if (GlobalCompatibilityController.isHideTimelapseExportSocialActive()) {
                for (int i = 0; i < EXPORT_SOCIAL_NAMES.length; i++) {
                    View target = findView(view, EXPORT_SOCIAL_NAMES[i], EXPORT_SOCIAL_FALLBACKS[i]);
                    if (target != null) {
                        hideViewCleanly(target);
                    }
                }
            }
            if (isCanvasPopupPolishActive()) {
                normalizeExportFormatRows(view);
                centerExportShareGridItems(view);
            }
            if (view instanceof ViewGroup) {
                unclipContainerSafely((ViewGroup) view);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static volatile int sIdShareTv = -1;
    private static volatile int sIdShareIv = -1;
    private static volatile int sIdRefToggleView = -1;
    private static volatile int sIdRefBgPic = -1;
    private static volatile int sIdRefBgCanvas = -1;
    private static volatile int sIdRefIvPic = -1;
    private static volatile int sIdRefIvCanvas = -1;
    private static volatile int sIdRefTvPic = -1;
    private static volatile int sIdRefTvCanvas = -1;

    public static void centerShareLabelIfApplicable(TextView tv) {
        if (tv == null) return;
        int id = tv.getId();
        if (id == View.NO_ID) return;
        if (sIdShareTv == -1) {
            String pkg = tv.getContext().getPackageName();
            sIdShareTv = tv.getResources().getIdentifier("share_tv", "id", pkg);
        }
        if (sIdShareTv == 0 || id != sIdShareTv) return;
        int grav = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        if (tv.getGravity() != grav) {
            tv.setGravity(grav);
        }
        if (Build.VERSION.SDK_INT >= 17 && tv.getTextAlignment() != View.TEXT_ALIGNMENT_CENTER) {
            tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        }
        if (tv.getLayoutParams() instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) tv.getLayoutParams();
            if (lp.gravity != Gravity.CENTER_HORIZONTAL) {
                lp.gravity = Gravity.CENTER_HORIZONTAL;
                tv.setLayoutParams(lp);
            }
        }
    }

    private static void centerExportShareGridItems(View view) {
        if (view == null) return;
        if (sIdShareTv == -1) {
            String pkg = view.getContext().getPackageName();
            android.content.res.Resources res = view.getResources();
            sIdShareTv = res.getIdentifier("share_tv", "id", pkg);
            sIdShareIv = res.getIdentifier("share_iv", "id", pkg);
        }
        if (sIdShareTv == 0 && sIdShareIv == 0) return;
        centerShareNodeRecursively(view);
    }

    private static void centerShareNodeRecursively(View node) {
        if (node == null || node.getVisibility() == View.GONE) return;
        int id = node.getId();
        if (id != 0 && id == sIdShareTv && node instanceof TextView) {
            TextView tv = (TextView) node;
            int grav = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            if (tv.getGravity() != grav) {
                tv.setGravity(grav);
            }
            if (Build.VERSION.SDK_INT >= 17 && tv.getTextAlignment() != View.TEXT_ALIGNMENT_CENTER) {
                tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            }
            if (tv.getLayoutParams() instanceof LinearLayout.LayoutParams) {
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) tv.getLayoutParams();
                if (lp.gravity != Gravity.CENTER_HORIZONTAL) {
                    lp.gravity = Gravity.CENTER_HORIZONTAL;
                    tv.setLayoutParams(lp);
                }
            } else if (tv.getLayoutParams() instanceof RelativeLayout.LayoutParams) {
                RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) tv.getLayoutParams();
                rlp.addRule(RelativeLayout.CENTER_HORIZONTAL);
                tv.setLayoutParams(rlp);
            }
        } else if (id != 0 && id == sIdShareIv && node.getLayoutParams() instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) node.getLayoutParams();
            if (lp.gravity != Gravity.CENTER_HORIZONTAL) {
                lp.gravity = Gravity.CENTER_HORIZONTAL;
                node.setLayoutParams(lp);
            }
        }
        if (node instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) node;
            for (int i = 0; i < vg.getChildCount(); i++) {
                centerShareNodeRecursively(vg.getChildAt(i));
            }
        }
    }

    public static boolean optimizeReferenceFloatingWindow(View root) {
        if (root == null) return false;
        boolean mutated = false;
        try {
            if (sIdRefToggleView == -1) {
                String pkg = root.getContext().getPackageName();
                android.content.res.Resources res = root.getResources();
                sIdRefToggleView = res.getIdentifier("toggleView", "id", pkg);
                sIdRefBgPic = res.getIdentifier("bg_pic", "id", pkg);
                sIdRefBgCanvas = res.getIdentifier("bg_canvas", "id", pkg);
                sIdRefIvPic = res.getIdentifier("iv_pic", "id", pkg);
                sIdRefIvCanvas = res.getIdentifier("iv_canvas", "id", pkg);
                sIdRefTvPic = res.getIdentifier("tv_pic", "id", pkg);
                sIdRefTvCanvas = res.getIdentifier("tv_canvas", "id", pkg);
            }
            if (sIdRefToggleView == 0 || sIdRefBgPic == 0 || sIdRefBgCanvas == 0) return false;

            View toggleView = root.findViewById(sIdRefToggleView);
            if (!(toggleView instanceof LinearLayout) || toggleView.getVisibility() != View.VISIBLE) {
                return false;
            }
            View bgPic = toggleView.findViewById(sIdRefBgPic);
            View bgCanvas = toggleView.findViewById(sIdRefBgCanvas);
            if (bgPic == null || bgCanvas == null) {
                return false;
            }

            float density = root.getResources().getDisplayMetrics().density;
            LinearLayout llToggle = (LinearLayout) toggleView;
            if (Math.abs(llToggle.getWeightSum() - 2.0f) > 0.01f) {
                llToggle.setWeightSum(2.0f);
                mutated = true;
            }
            final int targetToggleGravity = Gravity.START | Gravity.CENTER_VERTICAL;
            if (llToggle.getGravity() != targetToggleGravity) {
                llToggle.setGravity(targetToggleGravity);
                mutated = true;
            }
            if (llToggle.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) llToggle.getLayoutParams();
                int sideMargin = Math.round(4 * density);
                boolean changed = false;
                if (mlp.width != ViewGroup.LayoutParams.MATCH_PARENT) {
                    mlp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                    changed = true;
                }
                if (mlp.leftMargin != sideMargin || mlp.rightMargin != sideMargin) {
                    mlp.leftMargin = sideMargin;
                    mlp.rightMargin = sideMargin;
                    changed = true;
                }
                if (Build.VERSION.SDK_INT >= 17 && (mlp.getMarginStart() != sideMargin || mlp.getMarginEnd() != sideMargin)) {
                    mlp.setMarginStart(sideMargin);
                    mlp.setMarginEnd(sideMargin);
                    changed = true;
                }
                if (changed) {
                    llToggle.setLayoutParams(mlp);
                    mutated = true;
                }
            }

            View[] tabs = new View[]{bgPic, bgCanvas};
            int tabPadH = Math.round(2 * density);
            int tabMargin = Math.round(1 * density);
            for (View tab : tabs) {
                if (tab instanceof LinearLayout) {
                    LinearLayout tabLl = (LinearLayout) tab;
                    if (tabLl.getGravity() != Gravity.CENTER) {
                        tabLl.setGravity(Gravity.CENTER);
                        mutated = true;
                    }
                }
                if (tab.getPaddingLeft() != tabPadH || tab.getPaddingRight() != tabPadH) {
                    tab.setPadding(tabPadH, tab.getPaddingTop(), tabPadH, tab.getPaddingBottom());
                    mutated = true;
                }
                if (tab.getLayoutParams() instanceof LinearLayout.LayoutParams) {
                    LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) tab.getLayoutParams();
                    boolean changed = false;
                    if (lp.width != 0 || Math.abs(lp.weight - 1.0f) > 0.01f) {
                        lp.width = 0;
                        lp.weight = 1.0f;
                        changed = true;
                    }
                    if (lp.leftMargin != tabMargin || lp.rightMargin != tabMargin) {
                        lp.leftMargin = tabMargin;
                        lp.rightMargin = tabMargin;
                        changed = true;
                    }
                    if (Build.VERSION.SDK_INT >= 17 && (lp.getMarginStart() != tabMargin || lp.getMarginEnd() != tabMargin)) {
                        lp.setMarginStart(tabMargin);
                        lp.setMarginEnd(tabMargin);
                        changed = true;
                    }
                    if (changed) {
                        tab.setLayoutParams(lp);
                        mutated = true;
                    }
                }
            }

            View[] ivViews = new View[]{
                sIdRefIvPic != 0 ? bgPic.findViewById(sIdRefIvPic) : null,
                sIdRefIvCanvas != 0 ? bgCanvas.findViewById(sIdRefIvCanvas) : null
            };
            int iconSize = Math.round(18 * density);
            int iconGap = Math.round(2 * density);
            for (View iv : ivViews) {
                if (iv != null && iv.getLayoutParams() instanceof LinearLayout.LayoutParams) {
                    LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) iv.getLayoutParams();
                    boolean changed = false;
                    if (lp.width != iconSize || lp.height != iconSize) {
                        lp.width = iconSize;
                        lp.height = iconSize;
                        changed = true;
                    }
                    if (lp.rightMargin != iconGap) {
                        lp.rightMargin = iconGap;
                        changed = true;
                    }
                    if (Build.VERSION.SDK_INT >= 17 && lp.getMarginEnd() != iconGap) {
                        lp.setMarginEnd(iconGap);
                        changed = true;
                    }
                    if (changed) {
                        iv.setLayoutParams(lp);
                        mutated = true;
                    }
                }
            }

            View[] tvViews = new View[]{
                sIdRefTvPic != 0 ? bgPic.findViewById(sIdRefTvPic) : null,
                sIdRefTvCanvas != 0 ? bgCanvas.findViewById(sIdRefTvCanvas) : null
            };
            for (View tvRaw : tvViews) {
                if (tvRaw instanceof TextView) {
                    TextView tv = (TextView) tvRaw;
                    Object origRefObj = tv.getTag(TAG_REF_LABEL_ORIG_PX);
                    float origRefPx;
                    if (origRefObj instanceof Float) {
                        origRefPx = (Float) origRefObj;
                    } else {
                        origRefPx = tv.getTextSize();
                        tv.setTag(TAG_REF_LABEL_ORIG_PX, origRefPx);
                    }
                    mutated |= setSingleLineIfChanged(tv, true);
                    mutated |= setMaxLinesIfChanged(tv, 1);
                    mutated |= setEllipsizeIfChanged(tv, TextUtils.TruncateAt.END);
                    mutated |= TranslationEngine.ensureSafeVietnameseFontPadding(tv);
                    mutated |= setGravityIfChanged(tv, Gravity.START | Gravity.CENTER_VERTICAL);
                    if (origRefPx > 0f) {
                        CharSequence tvCs = tv.getText();
                        ViewParent tvParent = tv.getParent();
                        if (tvCs != null && tvCs.length() > 0 && tvParent instanceof View) {
                            View parentView = (View) tvParent;
                            int pw = parentView.getWidth();
                            if (pw > 0) {
                                String textStr = tvCs.toString();
                                Context tvCtx = tv.getContext();
                                Locale activeLoc = TranslationEngine.getActiveLocale();
                                Typeface activeTf = UIFontManager.getActiveTypeface(tvCtx);
                                mutated |= TypographyNormalizer.prepareTextViewForPipeline(
                                        tv, textStr, activeLoc, activeTf, TranslationEngine.isDynamicI18nEnabled());
                                String measureKey = textStr + "|" + Float.floatToRawIntBits(origRefPx)
                                        + "|" + UIFontManager.getActiveTypefaceIdentity(tvCtx)
                                        + "|" + TranslationEngine.getActiveLocaleTag();
                                Object prevPw = tv.getTag(TAG_REF_LABEL_LAST_PW);
                                Object prevKey = tv.getTag(TAG_REF_LABEL_LAST_KEY);
                                if (!(prevPw instanceof Integer
                                        && ((Integer) prevPw).intValue() == pw
                                        && measureKey.equals(prevKey))) {
                                    float targetRefPx = origRefPx;
                                    int availRefW = pw - parentView.getPaddingLeft() - parentView.getPaddingRight()
                                            - iconSize - iconGap - Math.round(6 * density);
                                    if (availRefW > 0) {
                                        float measuredRefW;
                                        Object cachedMeasureKey = tv.getTag(TAG_REF_LABEL_MEASURE_KEY);
                                        Object cachedMeasureVal = tv.getTag(TAG_REF_LABEL_MEASURE_VAL);
                                        if (measureKey.equals(cachedMeasureKey) && cachedMeasureVal instanceof Float) {
                                            measuredRefW = (Float) cachedMeasureVal;
                                        } else {
                                            TextPaint refPaint = TypographyNormalizer.getScratchTextPaint(tv.getPaint());
                                            refPaint.setTextSize(origRefPx);
                                            TypographyNormalizer.preparePaintForScript(
                                                    refPaint, textStr, activeLoc, activeTf);
                                            measuredRefW = TypographyNormalizer.measureHarfBuzzWidth(textStr, refPaint);
                                            tv.setTag(TAG_REF_LABEL_MEASURE_KEY, measureKey);
                                            tv.setTag(TAG_REF_LABEL_MEASURE_VAL, Float.valueOf(measuredRefW));
                                        }
                                        if (measuredRefW > availRefW) {
                                            float minRefPx = Math.max(origRefPx * 0.85f, 10.0f * density);
                                            targetRefPx = Math.max(minRefPx, origRefPx * ((float) availRefW / Math.max(1f, measuredRefW)));
                                        }
                                    } else {
                                        targetRefPx = Math.max(origRefPx * 0.85f, 10.0f * density);
                                    }
                                    if (Math.abs(tv.getTextSize() - targetRefPx) >= 0.5f) {
                                        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, targetRefPx);
                                        mutated = true;
                                    }
                                    tv.setTag(TAG_REF_LABEL_LAST_PW, Integer.valueOf(pw));
                                    tv.setTag(TAG_REF_LABEL_LAST_KEY, measureKey);
                                }
                            }
                        }
                    }
                    if (tv.getPaddingLeft() != 0 || tv.getPaddingRight() != 0) {
                        tv.setPadding(0, tv.getPaddingTop(), 0, tv.getPaddingBottom());
                        mutated = true;
                    }
                    if (tv.getLayoutParams() instanceof ViewGroup.LayoutParams) {
                        ViewGroup.LayoutParams lp = tv.getLayoutParams();
                        if (lp.width != ViewGroup.LayoutParams.WRAP_CONTENT) {
                            lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                            tv.setLayoutParams(lp);
                            mutated = true;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
        return mutated;
    }

    /**
     * Hides the online Brush Shop (market_view) button inside the brush panel
     * when pure_studio_hide_brush_shop is registered and enabled, keeping all
     * preset and imported local brushes intact.
     */
    public static void hideBrushShopIfEnabled(View brushView) {
        if (brushView == null) return;
        try {
            if (!LibreConfig.isFeatureActive(LibreConfig.KEY_HIDE_BRUSH_SHOP)) {
                return;
            }
            View marketView = findView(brushView, "market_view", 0x7f090870);
            if (marketView != null) {
                marketView.setVisibility(View.GONE);
                marketView.setEnabled(false);
                marketView.setClickable(false);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Hides the online Brush Shop button inside ChooseBrushFragment (Brush Library)
     * when pure_studio_hide_brush_library_shop is enabled, isolated from SelectPenView.
     */
    public static void hideBrushLibraryShopIfEnabled(View brushView) {
        if (brushView == null) return;
        try {
            if (!isHideBrushLibraryShopActive()) {
                return;
            }
            View marketView = findView(brushView, "market_view", 0x7f090870);
            if (marketView != null) {
                marketView.setVisibility(View.GONE);
                marketView.setEnabled(false);
                marketView.setClickable(false);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Optimizes Brush Selection Panel (SelectPenView / ChooseBrushFragment):
     * 1. Hides Brush Shop / Market button when pure_studio_hide_brush_shop is registered and enabled.
     * 2. Cleans up domestic cloud icons and translates panel.
     */
    public static void optimizeBrushPanel(View brushView) {
        if (brushView == null) return;
        try {
            hideBrushShopIfEnabled(brushView);
            TranslationEngine.translateViewHierarchy(brushView);
            TranslationEngine.attachViewTreeObserver(brushView);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static void setHorizontalMargins(View child, int leftPx, int rightPx) {
        if (child == null || !(child.getLayoutParams() instanceof ViewGroup.MarginLayoutParams)) return;
        ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) child.getLayoutParams();
        boolean changed = false;
        if (mlp.leftMargin != leftPx || mlp.rightMargin != rightPx) {
            mlp.leftMargin = leftPx;
            mlp.rightMargin = rightPx;
            changed = true;
        }
        if (Build.VERSION.SDK_INT >= 17 && (mlp.getMarginStart() != leftPx || mlp.getMarginEnd() != rightPx)) {
            mlp.setMarginStart(leftPx);
            mlp.setMarginEnd(rightPx);
            changed = true;
        }
        if (changed) child.setLayoutParams(mlp);
    }

    private static void applySingleLineToolbarLabel(TextView tv) {
        if (tv == null) return;
        setSingleLineIfChanged(tv, true);
        setMaxLinesIfChanged(tv, 1);
        TranslationEngine.ensureSafeVietnameseFontPadding(tv);
    }

    private static void fitToolbarLabelToWidth(TextView tv, float defaultSp, float minSp, float density) {
        if (tv == null || tv.getVisibility() != View.VISIBLE) return;
        applySingleLineToolbarLabel(tv);
        Object origObj = tv.getTag(TAG_TOOLBAR_ORIG_PX);
        DisplayMetrics dm = tv.getResources().getDisplayMetrics();
        float basePx;
        if (origObj instanceof Float) {
            basePx = (Float) origObj;
        } else {
            basePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, defaultSp, dm);
            tv.setTag(TAG_TOOLBAR_ORIG_PX, basePx);
        }
        float minPx = Math.min(basePx, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, minSp, dm));
        int boxW = tv.getWidth();
        int safeInset = boxW > 0
                ? TypographyNormalizer.clamp(Math.round(boxW * 0.08f), Math.round(4f * density), Math.round(10f * density))
                : Math.round(4f * density);
        int effPadL = Math.max(tv.getPaddingLeft(), safeInset);
        int effPadR = Math.max(tv.getPaddingRight(), safeInset);
        int availW = boxW - effPadL - effPadR;
        float targetPx = basePx;
        float targetScaleX = 1.0f;
        CharSequence cs = tv.getText();
        boolean stillOverflows = false;
        if (availW > 0 && cs != null && cs.length() > 0) {
            String str = cs.toString();
            Locale activeLoc = TranslationEngine.getActiveLocale();
            Typeface activeTf = UIFontManager.getActiveTypeface(tv.getContext());
            TypographyNormalizer.prepareTextViewForPipeline(
                    tv, str, activeLoc, activeTf, TranslationEngine.isDynamicI18nEnabled());
            TextPaint tp = TypographyNormalizer.getScratchTextPaint(tv.getPaint());
            TypographyNormalizer.preparePaintForScript(tp, str, activeLoc, activeTf);
            tp.setTextScaleX(1.0f);
            tp.setTextSize(targetPx);
            float strictAvail = Math.max(1f, availW - 0.5f);
            while (TypographyNormalizer.measureHarfBuzzWidth(str, tp) > strictAvail && targetPx > minPx) {
                targetPx = Math.max(minPx, targetPx * 0.92f);
                tp.setTextSize(targetPx);
            }
            float wAfter = TypographyNormalizer.measureHarfBuzzWidth(str, tp);
            if (wAfter > strictAvail) {
                if (TypographyNormalizer.classifyScript(str) == TypographyNormalizer.SCRIPT_LATIN_CYRILLIC_GREEK
                        && wAfter > 1f) {
                    float scaled = Math.max(0.90f, Math.min(1.0f, strictAvail / wAfter));
                    tp.setTextScaleX(scaled);
                    targetScaleX = scaled;
                    stillOverflows = TypographyNormalizer.measureHarfBuzzWidth(str, tp) > strictAvail;
                } else {
                    stillOverflows = true;
                }
            }
        }
        if (tv.getPaddingLeft() != effPadL || tv.getPaddingRight() != effPadR) {
            tv.setPadding(effPadL, tv.getPaddingTop(), effPadR, tv.getPaddingBottom());
        }
        if (stillOverflows && tv.getEllipsize() != TextUtils.TruncateAt.END) {
            tv.setEllipsize(TextUtils.TruncateAt.END);
        }
        if (Math.abs(tv.getTextScaleX() - targetScaleX) > 0.005f) {
            tv.setTextScaleX(targetScaleX);
        }
        if (Math.abs(tv.getTextSize() - targetPx) > 0.4f) {
            tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, targetPx);
        }
    }

    private static void centerHorizontalSlotLabel(View slot, TextView tv) {
        if (slot instanceof LinearLayout) {
            LinearLayout ll = (LinearLayout) slot;
            if (ll.isBaselineAligned()) {
                ll.setBaselineAligned(false);
            }
            ll.setGravity(Gravity.CENTER);
        }
        if (tv != null) {
            applySingleLineToolbarLabel(tv);
            setGravityIfChanged(tv, Gravity.CENTER_VERTICAL);
            if (tv.getLayoutParams() instanceof LinearLayout.LayoutParams) {
                LinearLayout.LayoutParams tlp = (LinearLayout.LayoutParams) tv.getLayoutParams();
                if (tlp.gravity != Gravity.CENTER_VERTICAL || tlp.height != ViewGroup.LayoutParams.WRAP_CONTENT) {
                    tlp.gravity = Gravity.CENTER_VERTICAL;
                    tlp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    tv.setLayoutParams(tlp);
                }
            }
        }
    }

    private static void polishGestureSwitchRows(View root) {
        if (root == null) return;
        try {
            View gestureSwitch = findView(root, "single_fingers_switch", 0);
            if (gestureSwitch != null && gestureSwitch.getParent() instanceof View
                    && gestureSwitch.getParent().getParent() instanceof ViewGroup) {
                ViewGroup rowContainer = (ViewGroup) gestureSwitch.getParent().getParent();
                float density = root.getResources().getDisplayMetrics().density;
                if (density > 0f) {
                    fixSwitchRowClipping(rowContainer, density);
                }
            }
        } catch (Throwable ignored) {}
    }

    private static void setLinearItemWeight(View v, float weight) {
        if (v == null || !(v.getLayoutParams() instanceof LinearLayout.LayoutParams)) return;
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) v.getLayoutParams();
        if (Math.abs(lp.weight - weight) > 0.01f) {
            lp.weight = weight;
            v.setLayoutParams(lp);
        }
    }

    /**
     * Computes open horizontal canvas bounds [usableLeft, usableRight, usableW, freeCenterShiftXpx]
     * clear of the docked PaintActivity tool rail.
     */
    private static int[] computeUsableCanvasSpan(View root, DisplayMetrics dm) {
        int screenW = dm.widthPixels;
        float density = dm.density;
        int usableLeft = 0;
        int usableRight = screenW;
        if (root != null) {
            View toolRail = findView(root, "paint_left_setting", 0);
            if (toolRail == null) {
                toolRail = findView(root, "leftSettingView", 0);
            }
            if (toolRail != null && toolRail.getVisibility() == View.VISIBLE
                    && toolRail.getWidth() > 0 && toolRail.getWidth() < screenW / 3) {
                int rLeft = toolRail.getLeft();
                int rRight = toolRail.getRight();
                if (rLeft > screenW / 2) {
                    usableRight = rLeft;
                } else if (rRight > 0 && rRight < screenW / 2) {
                    usableLeft = rRight;
                }
            }
        }
        int usableW = Math.max(Math.round(240f * density), usableRight - usableLeft);
        int centerShiftPx = Math.round(((usableLeft + usableRight) - screenW) * 0.5f);
        return new int[]{usableLeft, usableRight, usableW, centerShiftPx};
    }

    private static float measureLabelDesiredWidth(TextView tv) {
        if (tv == null) return 0f;
        CharSequence cs = tv.getText();
        if (cs == null || cs.length() == 0) return 0f;
        TypographyNormalizer.BaselineSnapshot snap = TypographyNormalizer.captureBaseline(tv);
        float origPx = (snap != null && snap.origTextSize > 0f) ? snap.origTextSize : tv.getTextSize();
        TextPaint tp = TypographyNormalizer.getScratchTextPaint(tv.getPaint());
        if (origPx > 0f) {
            tp.setTextSize(origPx);
        }
        Locale activeLoc = TranslationEngine.getActiveLocale();
        Typeface activeTf = UIFontManager.getActiveTypeface(tv.getContext());
        TypographyNormalizer.preparePaintForScript(tp, cs, activeLoc, activeTf);
        tp.setTextScaleX(1.0f);
        return TypographyNormalizer.measureHarfBuzzWidth(cs, tp);
    }

    private static volatile int sSolverPassEpoch = 1;

    private static int measureTextSlotAvailH(View rowContainer, TextView tv, float density) {
        if (tv == null) return 0;
        ViewGroup.LayoutParams tvLp = tv.getLayoutParams();
        if (tvLp != null && tvLp.height > 0 && tv.getHeight() > 0) {
            return Math.max(1, tv.getHeight() - tv.getCompoundPaddingTop() - tv.getCompoundPaddingBottom());
        }
        int baseRowH = 0;
        if (rowContainer != null) {
            TypographyNormalizer.ContainerHeightSnapshot rowSnap =
                    TypographyNormalizer.captureContainerBaseline(rowContainer);
            if (rowSnap != null && rowSnap.origLpHeight > 0) {
                baseRowH = rowSnap.origLpHeight;
            } else if (rowContainer.getLayoutParams() != null && rowContainer.getLayoutParams().height > 0) {
                baseRowH = rowContainer.getLayoutParams().height;
            } else if (rowContainer.getHeight() > 0) {
                baseRowH = rowContainer.getHeight();
            }
        }
        if (baseRowH > 0) {
            int iconUsed = Math.round(33f * density);
            if (rowContainer instanceof ViewGroup && tv.getTop() > 0) {
                int tvTopInRow = 0;
                View cur = tv;
                while (cur != null && cur != rowContainer) {
                    tvTopInRow += cur.getTop();
                    ViewParent vp = cur.getParent();
                    cur = (vp instanceof View) ? (View) vp : null;
                }
                if (cur == rowContainer && tvTopInRow >= Math.round(22f * density)
                        && tvTopInRow <= Math.round(42f * density)) {
                    iconUsed = tvTopInRow;
                }
            }
            int reserveBottom = Math.round(5f * density);
            return Math.max(Math.round(14f * density), baseRowH - iconUsed - reserveBottom);
        }
        return 0;
    }

    /**
     * Aligns mixed LinearLayout(ImageView+TextView) and compound drawableTop TextView slots
     * inside a horizontal canvas bar so all icons and label baselines sit on level axes.
     */
    private static boolean alignMixedIconRow(LinearLayout row, float density) {
        if (row == null) return false;
        boolean mutated = false;
        TypographyNormalizer.captureContainerBaseline(row);
        if (row.isBaselineAligned()) {
            row.setBaselineAligned(false);
            mutated = true;
        }
        int rowVertGrav = row.getGravity() & Gravity.VERTICAL_GRAVITY_MASK;
        if (rowVertGrav != Gravity.TOP) {
            row.setGravity((row.getGravity() & ~Gravity.VERTICAL_GRAVITY_MASK) | Gravity.TOP);
            mutated = true;
        }
        int maxIconBoxH = 0;
        for (int i = 0; i < row.getChildCount(); i++) {
            View ch = row.getChildAt(i);
            if (ch.getVisibility() == View.GONE) continue;
            TypographyNormalizer.captureContainerBaseline(ch);
            if (ch.getLayoutParams() instanceof LinearLayout.LayoutParams) {
                LinearLayout.LayoutParams clp = (LinearLayout.LayoutParams) ch.getLayoutParams();
                if ((clp.gravity & Gravity.VERTICAL_GRAVITY_MASK) != Gravity.TOP) {
                    clp.gravity = (clp.gravity <= 0 ? Gravity.TOP : ((clp.gravity & ~Gravity.VERTICAL_GRAVITY_MASK) | Gravity.TOP));
                    ch.setLayoutParams(clp);
                    mutated = true;
                }
            }
            if (ch instanceof ViewGroup && ((ViewGroup) ch).getChildCount() > 0) {
                View iconBox = ((ViewGroup) ch).getChildAt(0);
                if (!(iconBox instanceof TextView)) {
                    int h = iconBox.getHeight() > 0 ? iconBox.getHeight() : iconBox.getMeasuredHeight();
                    if (h > 0) {
                        maxIconBoxH = Math.max(maxIconBoxH, ch.getPaddingTop() + h);
                    }
                }
            }
        }
        if (maxIconBoxH <= 0) return mutated;
        for (int i = 0; i < row.getChildCount(); i++) {
            View ch = row.getChildAt(i);
            if (ch.getVisibility() == View.GONE || !(ch instanceof TextView)) continue;
            TextView tv = (TextView) ch;
            TypographyNormalizer.BaselineSnapshot snap = TypographyNormalizer.captureBaseline(tv);
            android.graphics.drawable.Drawable[] ds = tv.getCompoundDrawables();
            android.graphics.drawable.Drawable topD = (ds != null && ds.length > 1) ? ds[1] : null;
            if (topD == null && Build.VERSION.SDK_INT >= 17) {
                android.graphics.drawable.Drawable[] rds = tv.getCompoundDrawablesRelative();
                topD = (rds != null && rds.length > 1) ? rds[1] : null;
            }
            if (topD != null) {
                int dH = topD.getBounds().height() > 0 ? topD.getBounds().height() : topD.getIntrinsicHeight();
                if (dH > 0) {
                    int basePadT = snap != null ? snap.origPadT : tv.getPaddingTop();
                    int wantPadT = Math.max(basePadT, Math.max(0, (maxIconBoxH - dH) / 2));
                    if (tv.getPaddingTop() != wantPadT) {
                        tv.setPadding(tv.getPaddingLeft(), wantPadT, tv.getPaddingRight(), tv.getPaddingBottom());
                        mutated = true;
                    }
                }
            }
        }
        return mutated;
    }

    private static boolean alignVerticalIconSlotContainer(View slot, View iconView, TextView tv, int topPadPx) {
        if (slot == null) return false;
        boolean mutated = false;
        TypographyNormalizer.captureContainerBaseline(slot);
        if (slot instanceof LinearLayout) {
            LinearLayout ll = (LinearLayout) slot;
            if (ll.isBaselineAligned()) {
                ll.setBaselineAligned(false);
                mutated = true;
            }
            int wantG = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            if (ll.getGravity() != wantG) {
                ll.setGravity(wantG);
                mutated = true;
            }
        }
        if (topPadPx >= 0 && slot.getPaddingTop() != topPadPx) {
            slot.setPadding(slot.getPaddingLeft(), topPadPx, slot.getPaddingRight(), slot.getPaddingBottom());
            mutated = true;
        }
        if (iconView != null && iconView.getLayoutParams() instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams ilp = (LinearLayout.LayoutParams) iconView.getLayoutParams();
            if (ilp.gravity != Gravity.CENTER_HORIZONTAL) {
                ilp.gravity = Gravity.CENTER_HORIZONTAL;
                iconView.setLayoutParams(ilp);
                mutated = true;
            }
        }
        if (tv != null) {
            int tvG = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            if (tv.getGravity() != tvG) {
                tv.setGravity(tvG);
                mutated = true;
            }
        }
        return mutated;
    }

    private static boolean solveStripPeerGroup(
            View rowContainer,
            List<TypographyNormalizer.GroupMember> members,
            int archetype,
            int maxGrowthPx,
            boolean enforceTopCenter,
            boolean fillSlotWidth,
            boolean expandAncestors,
            int maxAncestorHops,
            int epoch,
            String groupTag,
            float density) {
        if (members == null || members.isEmpty()) return false;
        boolean mutated = false;
        Locale activeLoc = TranslationEngine.getActiveLocale();
        Typeface activeTf = UIFontManager.getActiveTypeface(members.get(0).tv.getContext());
        DisplayMetrics dm = members.get(0).tv.getResources().getDisplayMetrics();

        for (int i = 0; i < members.size(); i++) {
            TypographyNormalizer.GroupMember m = members.get(i);
            TypographyNormalizer.captureBaseline(m.tv);
            mutated |= TypographyNormalizer.prepareTextViewForPipeline(
                    m.tv, m.text, activeLoc, activeTf, TranslationEngine.isDynamicI18nEnabled());
        }

        TypographyNormalizer.GroupSolveResult res = TypographyNormalizer.solvePeerGroup(
                members, archetype, maxGrowthPx, activeLoc, activeTf, dm);

        if (res.maxDeltaH > 0 && rowContainer != null) {
            if (expandAncestors) {
                mutated |= TypographyNormalizer.expandFixedHeightChain(rowContainer, res.maxDeltaH, maxAncestorHops);
            }
        }

        for (int i = 0; i < members.size(); i++) {
            TypographyNormalizer.GroupMember m = members.get(i);
            if (m.plan == null) continue;
            float applyPx = m.plan.extremeOutlier
                    ? m.plan.targetPx
                    : Math.min(m.plan.targetPx, res.groupPx);
            int effPadL = m.plan.resolvedSafeInsetPx > 0 ? m.plan.resolvedSafeInsetPx : m.safePadL;
            int effPadR = m.plan.resolvedSafeInsetPx > 0 ? m.plan.resolvedSafeInsetPx : m.safePadR;
            int targetMaxW = (m.plan.useEllipsis || fillSlotWidth || m.plan.resolvedSafeInsetPx > 0)
                    ? (m.safeAvailW + m.safePadL + m.safePadR)
                    : 0;
            mutated |= TypographyNormalizer.applySolvedPlanToTextView(
                    m.tv,
                    m.plan,
                    applyPx,
                    effPadL,
                    effPadR,
                    enforceTopCenter,
                    fillSlotWidth,
                    targetMaxW,
                    epoch,
                    groupTag);
            if (enforceTopCenter && m.slotView != null && m.slotView != m.tv) {
                mutated |= alignVerticalIconSlotContainer(m.slotView, null, m.tv, -1);
            }
        }
        return mutated;
    }

    /**
     * Widens cramped horizontal canvas sub-toolbars (Lasso selection, Shape selector, Symmetry selector,
     * Bucket/Gradient bar, 3D edit bar, 3D preview tabs, and Liquify bar) in PaintActivity and runs
     * Phase 2 peer synchronization across button rows with clean toggle-OFF baseline restoration.
     */
    public static boolean optimizeCanvasToolBars(View root) {
        if (root == null) return false;
        if (!isCanvasPopupPolishActive() && !TranslationEngine.isTextAutoFitActive()) {
            return false;
        }
        Context ctx = root.getContext();
        if (ctx == null || !TranslationEngine.resolveHostActivityName(root).endsWith("PaintActivity")) return false;
        boolean mutated = false;
        try {
            DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
            int screenW = dm.widthPixels;
            float density = dm.density;
            if (screenW <= 0 || density <= 0f) return false;

            int[] span = computeUsableCanvasSpan(root, dm);
            int usableW = span[2];
            float freeCenterShiftX = span[3];
            boolean autoFitOn = TranslationEngine.isTextAutoFitActive();
            int epoch = ++sSolverPassEpoch;

            // 1. Expand ShapeSettingView and standard_view dynamically and solve 6 buttons uniformly.
            int idShape = LibreResourceResolver.resolveViewId(ctx, "shape_setting", 0);
            if (idShape != 0) {
                View shapeBar = root.findViewById(idShape);
                if (shapeBar != null && shapeBar.getVisibility() == View.VISIBLE) {
                    View stdView = findView(shapeBar, "standard_view", 0);
                    View stdClickView = findView(shapeBar, "standard_click_view", 0);
                    View contentView = findView(shapeBar, "content_view", 0);
                    View shapeStripRow = (contentView != null && contentView.getParent() instanceof View)
                            ? (View) contentView.getParent()
                            : null;
                    TextView stdTv = (findView(shapeBar, "standard_tv", 0) instanceof TextView)
                            ? (TextView) findView(shapeBar, "standard_tv", 0)
                            : null;

                    float maxSlotSingleW = 0f;
                    float maxSlotWordW = 0f;
                    List<TextView> shapeTvs = new ArrayList<>(6);
                    String[] shapeIds = new String[]{"rect_tv", "oval_tv", "line_tv", "mutil_line_tv", "curves_tv"};
                    for (String lblId : shapeIds) {
                        View v = findView(shapeBar, lblId, 0);
                        if (v instanceof TextView) {
                            TextView st = (TextView) v;
                            shapeTvs.add(st);
                            TypographyNormalizer.captureBaseline(st);
                            maxSlotSingleW = Math.max(maxSlotSingleW, measureLabelDesiredWidth(st));
                            CharSequence sc = st.getText();
                            if (sc != null) {
                                TextPaint tp = TypographyNormalizer.getScratchTextPaint(st.getPaint());
                                TypographyNormalizer.BaselineSnapshot snap = TypographyNormalizer.captureBaseline(st);
                                if (snap != null && snap.origTextSize > 0f) tp.setTextSize(snap.origTextSize);
                                TypographyNormalizer.preparePaintForScript(
                                        tp, sc, TranslationEngine.getActiveLocale(), UIFontManager.getActiveTypeface(ctx));
                                maxSlotWordW = Math.max(maxSlotWordW, TranslationEngine.computeMaxWordWidth(tp, sc.toString()));
                            }
                        }
                    }
                    float stdSingleW = stdTv != null ? measureLabelDesiredWidth(stdTv) : 0f;
                    float stdWordW = 0f;
                    if (stdTv != null && stdTv.getText() != null) {
                        TextPaint stdTp = TypographyNormalizer.getScratchTextPaint(stdTv.getPaint());
                        TypographyNormalizer.BaselineSnapshot stdSnap = TypographyNormalizer.captureBaseline(stdTv);
                        if (stdSnap != null && stdSnap.origTextSize > 0f) stdTp.setTextSize(stdSnap.origTextSize);
                        TypographyNormalizer.preparePaintForScript(
                                stdTp, stdTv.getText(), TranslationEngine.getActiveLocale(), UIFontManager.getActiveTypeface(ctx));
                        stdWordW = TranslationEngine.computeMaxWordWidth(stdTp, stdTv.getText().toString());
                    }
                    boolean stdBreakable = stdTv != null && stdTv.getText() != null
                            && TypographyNormalizer.hasNaturalBreakToken(stdTv.getText());

                    TypographyNormalizer.captureContainerBaseline(shapeBar);
                    TypographyNormalizer.captureContainerBaseline(shapeStripRow);
                    TypographyNormalizer.captureContainerBaseline(contentView);
                    TypographyNormalizer.captureContainerBaseline(stdView);
                    TypographyNormalizer.captureContainerBaseline(stdClickView);

                    ViewGroup.LayoutParams sLp = shapeBar.getLayoutParams();
                    if (sLp != null && shapeBar.getTag(TAG_ORIG_SHAPE_W) == null && sLp.width > 0) {
                        shapeBar.setTag(TAG_ORIG_SHAPE_W, Integer.valueOf(sLp.width));
                    }
                    if (stdView != null && stdView.getTag(TAG_ORIG_SHAPE_STD_W) == null
                            && stdView.getLayoutParams() != null && stdView.getLayoutParams().width > 0) {
                        stdView.setTag(TAG_ORIG_SHAPE_STD_W, Integer.valueOf(stdView.getLayoutParams().width));
                    }
                    if (contentView != null && contentView.getTag(TAG_ORIG_SHAPE_CONTENT_W) == null
                            && contentView.getLayoutParams() != null && contentView.getLayoutParams().width > 0) {
                        contentView.setTag(TAG_ORIG_SHAPE_CONTENT_W, Integer.valueOf(contentView.getLayoutParams().width));
                    }

                    int desiredStdW = stdView != null
                            ? TypographyNormalizer.clamp(
                                    Math.round(stdBreakable
                                            ? Math.max(stdWordW + 10f * density, stdSingleW + 8f * density)
                                            : (stdSingleW + 10f * density)),
                                    Math.round(50f * density),
                                    Math.round(stdBreakable ? 70f * density : 86f * density))
                            : Math.round(50f * density);
                    float wantSlotText = Math.max(maxSlotWordW + 9f * density, Math.min(maxSlotSingleW + 9f * density, 68f * density));
                    int desiredSlotW = TypographyNormalizer.clamp(Math.round(wantSlotText),
                            Math.round(48f * density), Math.round(74f * density));
                    int desiredContentW = 5 * desiredSlotW + Math.round(4f * density);
                    int gapPx = Math.round(4f * density);
                    int maxAllowedShapeW = Math.max(Math.round(240f * density), usableW - Math.round(4f * density));

                    int targetStdW = desiredStdW;
                    int targetContentW = desiredContentW;
                    if (targetContentW + targetStdW + gapPx > maxAllowedShapeW) {
                        int availBoth = Math.max(Math.round(224f * density), maxAllowedShapeW - gapPx);
                        int minStdW = stdView != null
                                ? TypographyNormalizer.clamp(
                                        Math.round((stdBreakable ? stdWordW : stdSingleW * 0.90f) + 6f * density),
                                        Math.round(48f * density),
                                        desiredStdW)
                                : desiredStdW;
                        int minSlotW = TypographyNormalizer.clamp(
                                Math.round(maxSlotWordW + 6f * density),
                                Math.round(46f * density),
                                desiredSlotW);
                        int minContentW = Math.min(availBoth - Math.round(48f * density),
                                5 * minSlotW + Math.round(4f * density));
                        if (availBoth <= minContentW + minStdW) {
                            float totalMin = Math.max(1f, minContentW + minStdW);
                            int maxStdShare = Math.round(availBoth * 0.28f);
                            targetStdW = stdView != null
                                    ? TypographyNormalizer.clamp(
                                            Math.round(availBoth * (minStdW / totalMin)),
                                            Math.round(48f * density),
                                            Math.max(Math.round(48f * density), maxStdShare))
                                    : desiredStdW;
                            targetContentW = Math.max(Math.round(170f * density), availBoth - targetStdW);
                        } else {
                            int contentSlack = Math.max(0, desiredContentW - minContentW);
                            int stdSlack = Math.max(0, desiredStdW - minStdW);
                            int excess = (desiredContentW + desiredStdW) - availBoth;
                            int totalSlack = Math.max(1, contentSlack + stdSlack);
                            int cutStd = Math.min(stdSlack, Math.round(excess * (stdSlack / (float) totalSlack)));
                            int cutContent = Math.min(contentSlack, excess - cutStd);
                            int leftover = excess - (cutStd + cutContent);
                            if (leftover > 0 && contentSlack > cutContent) {
                                int extraC = Math.min(leftover, contentSlack - cutContent);
                                cutContent += extraC;
                                leftover -= extraC;
                            }
                            if (leftover > 0 && stdSlack > cutStd) {
                                cutStd += Math.min(leftover, stdSlack - cutStd);
                            }
                            targetStdW = desiredStdW - cutStd;
                            targetContentW = desiredContentW - cutContent;
                        }
                    }
                    int targetShapeW = Math.min(maxAllowedShapeW, targetContentW + targetStdW + gapPx);
                    if (stdView != null) {
                        setHorizontalMargins(stdView, gapPx, 0);
                    }

                    if (contentView instanceof ViewGroup) {
                        ViewGroup cvGroup = (ViewGroup) contentView;
                        int visIdx = 0;
                        int visTotal = 0;
                        for (int k = 0; k < cvGroup.getChildCount(); k++) {
                            if (cvGroup.getChildAt(k).getVisibility() != View.GONE) visTotal++;
                        }
                        int outerM = Math.round(2f * density);
                        for (int k = 0; k < cvGroup.getChildCount(); k++) {
                            View slot = cvGroup.getChildAt(k);
                            if (slot.getVisibility() == View.GONE) continue;
                            TypographyNormalizer.captureContainerBaseline(slot);
                            setHorizontalMargins(slot, visIdx == 0 ? outerM : 0, visIdx == visTotal - 1 ? outerM : 0);
                            if (slot.getPaddingLeft() != 0 || slot.getPaddingRight() != 0) {
                                slot.setPadding(0, slot.getPaddingTop(), 0, slot.getPaddingBottom());
                            }
                            if (slot instanceof LinearLayout) {
                                ((LinearLayout) slot).setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
                            }
                            visIdx++;
                        }
                    }
                    if (stdView instanceof LinearLayout && contentView != null) {
                        LinearLayout stdLl = (LinearLayout) stdView;
                        stdLl.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
                        int matchTop = contentView.getPaddingTop();
                        if (stdLl.getPaddingTop() != matchTop) {
                            stdLl.setPadding(stdLl.getPaddingLeft(), matchTop, stdLl.getPaddingRight(), stdLl.getPaddingBottom());
                        }
                    }
                    if (stdClickView != null) {
                        if (stdClickView instanceof LinearLayout) {
                            ((LinearLayout) stdClickView).setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
                        }
                        ViewGroup.LayoutParams scLp = stdClickView.getLayoutParams();
                        boolean scChanged = false;
                        if (scLp != null) {
                            // CRITICAL: res/vk.xml declares standard_click_view with WRAP_CONTENT width, which clamps MATCH_PARENT standard_tv to the 24dp ImageView width.
                            if (scLp.width != ViewGroup.LayoutParams.MATCH_PARENT) {
                                scLp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                                scChanged = true;
                            }
                            if (scLp.height != ViewGroup.LayoutParams.WRAP_CONTENT) {
                                scLp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                                scChanged = true;
                            }
                            if (scLp instanceof ViewGroup.MarginLayoutParams) {
                                ViewGroup.MarginLayoutParams sMlp = (ViewGroup.MarginLayoutParams) scLp;
                                if (sMlp.topMargin != 0) {
                                    sMlp.topMargin = 0;
                                    scChanged = true;
                                }
                            }
                            if (scChanged) {
                                stdClickView.setLayoutParams(scLp);
                                mutated = true;
                            }
                        }
                    }

                    if (sLp != null && sLp.width > 0 && sLp.width != targetShapeW) {
                        sLp.width = targetShapeW;
                        shapeBar.setLayoutParams(sLp);
                        mutated = true;
                    }
                    if (contentView != null) {
                        ViewGroup.LayoutParams cLp = contentView.getLayoutParams();
                        if (cLp != null && cLp.width > 0 && cLp.width != targetContentW) {
                            cLp.width = targetContentW;
                            contentView.setLayoutParams(cLp);
                            mutated = true;
                        }
                        if (contentView instanceof LinearLayout) {
                            ((LinearLayout) contentView).setGravity(Gravity.TOP);
                        }
                    }
                    if (stdView != null) {
                        ViewGroup.LayoutParams stdLp = stdView.getLayoutParams();
                        if (stdLp != null && stdLp.width > 0 && stdLp.width != targetStdW) {
                            stdLp.width = targetStdW;
                            stdView.setLayoutParams(stdLp);
                            mutated = true;
                        }
                    }
                    if (Math.abs(shapeBar.getTranslationX() - freeCenterShiftX) > 0.5f) {
                        shapeBar.setTranslationX(freeCenterShiftX);
                        mutated = true;
                    }

                    if (autoFitOn && !shapeTvs.isEmpty()) {
                        int perSlotW = Math.max(1, (targetContentW - Math.round(4f * density)) / 5);
                        int slotInnerW = perSlotW;
                        int safeSlotPad = TypographyNormalizer.clamp(
                                Math.round(slotInnerW * 0.04f), Math.round(2f * density), Math.round(3.5f * density));
                        int slotAvailTextW = Math.max(1, slotInnerW - 2 * safeSlotPad);
                        int hardAvailH = measureTextSlotAvailH(shapeStripRow, shapeTvs.get(0), density);

                        List<TypographyNormalizer.GroupMember> members = new ArrayList<>(6);
                        for (int i = 0; i < shapeTvs.size(); i++) {
                            TextView st = shapeTvs.get(i);
                            CharSequence cs = st.getText();
                            if (cs == null || cs.length() == 0) continue;
                            View slot = (st.getParent() instanceof View) ? (View) st.getParent() : null;
                            members.add(new TypographyNormalizer.GroupMember(
                                    st, cs.toString(), slotAvailTextW, hardAvailH, safeSlotPad, safeSlotPad, true, slot));
                        }
                        if (stdTv != null && stdTv.getText() != null && stdTv.getText().length() > 0) {
                            int safeStdPad = TypographyNormalizer.clamp(
                                    Math.round(targetStdW * 0.05f), Math.round(2.5f * density), Math.round(5f * density));
                            int stdAvailTextW = Math.max(1, targetStdW - 2 * safeStdPad);
                            members.add(new TypographyNormalizer.GroupMember(
                                    stdTv, stdTv.getText().toString(), stdAvailTextW, hardAvailH, safeStdPad, safeStdPad, true, stdClickView));
                        }
                        unclipContainerSafely(shapeBar instanceof ViewGroup ? (ViewGroup) shapeBar : null);
                        unclipContainerSafely(shapeBar.getParent() instanceof ViewGroup ? (ViewGroup) shapeBar.getParent() : null);
                        unclipContainerSafely(shapeStripRow instanceof ViewGroup ? (ViewGroup) shapeStripRow : null);
                        unclipContainerSafely(contentView instanceof ViewGroup ? (ViewGroup) contentView : null);
                        unclipContainerSafely(stdView instanceof ViewGroup ? (ViewGroup) stdView : null);
                        unclipContainerSafely(stdClickView instanceof ViewGroup ? (ViewGroup) stdClickView : null);
                        mutated |= solveStripPeerGroup(
                                shapeStripRow,
                                members,
                                TypographyNormalizer.ARCHETYPE_VERTICAL_ELASTIC_STRIP,
                                Math.round(24f * density),
                                true,
                                true,
                                true,
                                0,
                                epoch,
                                "shapeBar",
                                density);
                        if (sLp != null && sLp.height != ViewGroup.LayoutParams.WRAP_CONTENT) {
                            sLp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                            shapeBar.setLayoutParams(sLp);
                            mutated = true;
                        }
                    }
                }
            }

            // 2. Expand SymmetrySettingView across symmetry buttons and center in open canvas span.
            int idToolsView = LibreResourceResolver.resolveViewId(ctx, "tools_view", 0);
            if (idToolsView != 0) {
                View toolsView = root.findViewById(idToolsView);
                if (toolsView != null && toolsView.getParent() instanceof View) {
                    View symRoot = (View) toolsView.getParent();
                    if (symRoot.getVisibility() == View.VISIBLE
                            && symRoot.getClass().getName().endsWith("SymmetrySettingView")) {
                        TypographyNormalizer.captureContainerBaseline(symRoot);
                        int targetSymW = Math.min(Math.round(376 * density), usableW - Math.round(16 * density));
                        ViewGroup.LayoutParams symLp = symRoot.getLayoutParams();
                        if (symLp != null && symLp.width > 0 && symLp.width != targetSymW) {
                            symLp.width = targetSymW;
                            symRoot.setLayoutParams(symLp);
                            mutated = true;
                        }
                        if (Math.abs(symRoot.getTranslationX() - freeCenterShiftX) > 0.5f) {
                            symRoot.setTranslationX(freeCenterShiftX);
                            mutated = true;
                        }
                    }
                }
            }

            // 3. Optimize XuanQuSettingView dynamically (5-btn Lasso & 6-btn Magic Wand + Deform menu).
            int idDeformMenu = LibreResourceResolver.resolveViewId(ctx, "ll_img_deformation_menu", 0);
            if (idDeformMenu != 0) {
                View deformMenu = root.findViewById(idDeformMenu);
                if (deformMenu instanceof ViewGroup && deformMenu.getParent() instanceof View) {
                    View view2 = (View) deformMenu.getParent();
                    View shadowLayout = (view2.getParent() instanceof View) ? (View) view2.getParent() : null;
                    if (shadowLayout instanceof ViewGroup) {
                        View xuanQuRoot = (shadowLayout.getParent() instanceof View) ? (View) shadowLayout.getParent() : null;
                        if (xuanQuRoot != null && xuanQuRoot.getVisibility() == View.VISIBLE
                                && Math.abs(xuanQuRoot.getTranslationX() - freeCenterShiftX) > 0.5f) {
                            xuanQuRoot.setTranslationX(freeCenterShiftX);
                            mutated = true;
                        }
                        int idView1 = LibreResourceResolver.resolveViewId(ctx, "view1", 0);
                        View view1 = idView1 != 0 ? shadowLayout.findViewById(idView1) : null;
                        if (view1 instanceof ViewGroup && view1.getVisibility() == View.VISIBLE) {
                            TypographyNormalizer.captureContainerBaseline(view1);
                            View sw1 = findView(view1, "magic_stick_sampler_switch_view", 0);
                            if (sw1 == null) sw1 = findView(view1, "switchView", 0);
                            int desiredSw1 = computeDesiredToggleSwitchBarWidth(sw1, usableW, density);
                            int maxV1W = Math.max(Math.round(280 * density), usableW - Math.round(12 * density));

                            ViewGroup vg1 = (ViewGroup) view1;
                            LinearLayout hRow1 = null;
                            int visibleBtns = 0;
                            float rowNeededW = 0f;
                            for (int i = 0; i < vg1.getChildCount(); i++) {
                                View row = vg1.getChildAt(i);
                                if (row instanceof LinearLayout
                                        && ((LinearLayout) row).getOrientation() == LinearLayout.HORIZONTAL) {
                                    hRow1 = (LinearLayout) row;
                                    break;
                                }
                            }
                            if (hRow1 != null) {
                                TypographyNormalizer.captureContainerBaseline(hRow1);
                                for (int j = 0; j < hRow1.getChildCount(); j++) {
                                    View ch = hRow1.getChildAt(j);
                                    if (ch.getVisibility() == View.GONE) continue;
                                    visibleBtns++;
                                    TextView btnTv = findDirectSlotTextView(ch);
                                    if (btnTv != null) {
                                        rowNeededW += measureLabelDesiredWidth(btnTv) + Math.round(12 * density);
                                    } else {
                                        rowNeededW += Math.round(52 * density);
                                    }
                                }
                                rowNeededW += Math.round(22 * density);
                            }

                            int targetV1W = TypographyNormalizer.clamp(
                                    Math.round(Math.max(Math.round(280 * density), Math.max(desiredSw1, rowNeededW))),
                                    Math.round(280 * density),
                                    maxV1W);
                            ViewGroup.LayoutParams v1Lp = view1.getLayoutParams();
                            if (v1Lp != null && v1Lp.width > 0 && v1Lp.width != targetV1W) {
                                v1Lp.width = targetV1W;
                                view1.setLayoutParams(v1Lp);
                                mutated = true;
                            }
                            if (hRow1 != null && visibleBtns >= 2) {
                                if (hRow1.isBaselineAligned()) {
                                    hRow1.setBaselineAligned(false);
                                    mutated = true;
                                }
                                if ((hRow1.getGravity() & Gravity.VERTICAL_GRAVITY_MASK) != Gravity.TOP) {
                                    hRow1.setGravity((hRow1.getGravity() & ~Gravity.VERTICAL_GRAVITY_MASK) | Gravity.TOP);
                                    mutated = true;
                                }
                                int gapPx = Math.round((visibleBtns >= 6 ? 2 : 3) * density);
                                int edgePx = Math.round((visibleBtns >= 6 ? 3 : 5) * density);
                                int idx = 0;
                                float totalWeight = 0f;
                                for (int j = 0; j < hRow1.getChildCount(); j++) {
                                    View ch = hRow1.getChildAt(j);
                                    if (ch.getVisibility() == View.GONE) continue;
                                    TypographyNormalizer.captureContainerBaseline(ch);
                                    setHorizontalMargins(ch, idx == 0 ? edgePx : gapPx,
                                            idx == visibleBtns - 1 ? edgePx : gapPx);
                                    if (ch.getLayoutParams() instanceof LinearLayout.LayoutParams) {
                                        float w = ((LinearLayout.LayoutParams) ch.getLayoutParams()).weight;
                                        totalWeight += (w > 0f ? w : 1f);
                                    } else {
                                        totalWeight += 1f;
                                    }
                                    idx++;
                                }
                                if (autoFitOn && totalWeight > 0f) {
                                    int hRowMargins = 0;
                                    if (hRow1.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                                        ViewGroup.MarginLayoutParams rMlp = (ViewGroup.MarginLayoutParams) hRow1.getLayoutParams();
                                        hRowMargins = rMlp.leftMargin + rMlp.rightMargin;
                                    }
                                    int innerRowW = Math.max(1, targetV1W - hRowMargins - hRow1.getPaddingLeft() - hRow1.getPaddingRight()
                                            - 2 * edgePx - Math.max(0, visibleBtns - 1) * 2 * gapPx);
                                    List<TypographyNormalizer.GroupMember> xMembers = new ArrayList<>(visibleBtns);
                                    for (int j = 0; j < hRow1.getChildCount(); j++) {
                                        View ch = hRow1.getChildAt(j);
                                        if (ch.getVisibility() == View.GONE) continue;
                                        TextView btnTv = findDirectSlotTextView(ch);
                                        if (btnTv == null || btnTv.getText() == null || btnTv.getText().length() == 0) continue;
                                        float w = (ch.getLayoutParams() instanceof LinearLayout.LayoutParams)
                                                ? Math.max(1f, ((LinearLayout.LayoutParams) ch.getLayoutParams()).weight)
                                                : 1f;
                                        int slotW = Math.max(1, Math.round(innerRowW * (w / totalWeight)));
                                        int safePad = TypographyNormalizer.clamp(
                                                Math.round(slotW * 0.06f), Math.round(2f * density), Math.round(5f * density));
                                        int textAvail = Math.max(1, slotW - 2 * safePad);
                                        xMembers.add(new TypographyNormalizer.GroupMember(
                                                btnTv, btnTv.getText().toString(), textAvail, 0, safePad, safePad, true, ch));
                                    }
                                    unclipContainerSafely(hRow1);
                                    mutated |= alignMixedIconRow(hRow1, density);
                                    mutated |= solveStripPeerGroup(
                                            hRow1,
                                            xMembers,
                                            TypographyNormalizer.ARCHETYPE_VERTICAL_ELASTIC_STRIP,
                                            Math.round(18f * density),
                                            true,
                                            false,
                                            false,
                                            0,
                                            epoch,
                                            "xuanquV1_" + visibleBtns,
                                            density);
                                }
                            }
                        }
                        if (view2.getVisibility() == View.VISIBLE) {
                            TypographyNormalizer.captureContainerBaseline(view2);
                            View sw2 = findView(view2, "switchView", 0);
                            int desiredSw2 = computeDesiredToggleSwitchBarWidth(sw2, usableW, density);
                            int maxV2W = Math.max(Math.round(280 * density), usableW - Math.round(14 * density));
                            int targetV2W = Math.min(Math.max(Math.round(415 * density), desiredSw2), maxV2W);
                            ViewGroup.LayoutParams v2Lp = view2.getLayoutParams();
                            if (v2Lp != null && v2Lp.width > 0 && v2Lp.width != targetV2W) {
                                v2Lp.width = targetV2W;
                                view2.setLayoutParams(v2Lp);
                                mutated = true;
                            }
                            ViewGroup deformGroup = (ViewGroup) deformMenu;
                            TypographyNormalizer.captureContainerBaseline(deformGroup);
                            int visDeform = 0;
                            for (int i = 0; i < deformGroup.getChildCount(); i++) {
                                if (deformGroup.getChildAt(i).getVisibility() != View.GONE) visDeform++;
                            }
                            int smallGap = Math.round(4 * density);
                            int idx2 = 0;
                            float sumW2 = 0f;
                            for (int i = 0; i < deformGroup.getChildCount(); i++) {
                                View ch = deformGroup.getChildAt(i);
                                if (ch.getVisibility() == View.GONE) continue;
                                TypographyNormalizer.captureContainerBaseline(ch);
                                setHorizontalMargins(ch, 0, idx2 == visDeform - 1 ? 0 : smallGap);
                                if (ch.getLayoutParams() instanceof LinearLayout.LayoutParams) {
                                    float w = ((LinearLayout.LayoutParams) ch.getLayoutParams()).weight;
                                    sumW2 += (w > 0f ? w : 1f);
                                } else {
                                    sumW2 += 1f;
                                }
                                idx2++;
                            }
                            if (autoFitOn && visDeform >= 2 && sumW2 > 0f) {
                                if (deformGroup instanceof LinearLayout) {
                                    LinearLayout dLl = (LinearLayout) deformGroup;
                                    if (dLl.isBaselineAligned()) dLl.setBaselineAligned(false);
                                    if ((dLl.getGravity() & Gravity.VERTICAL_GRAVITY_MASK) != Gravity.TOP) {
                                        dLl.setGravity((dLl.getGravity() & ~Gravity.VERTICAL_GRAVITY_MASK) | Gravity.TOP);
                                    }
                                    ViewGroup.LayoutParams dLp = dLl.getLayoutParams();
                                    if (dLp != null && dLp.height == 0) {
                                        dLp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                                        dLl.setLayoutParams(dLp);
                                    }
                                }
                                int innerW2 = Math.max(1, targetV2W - deformGroup.getPaddingLeft() - deformGroup.getPaddingRight()
                                        - Math.max(0, visDeform - 1) * smallGap);
                                List<TypographyNormalizer.GroupMember> m2 = new ArrayList<>(visDeform);
                                for (int i = 0; i < deformGroup.getChildCount(); i++) {
                                    View ch = deformGroup.getChildAt(i);
                                    if (ch.getVisibility() == View.GONE) continue;
                                    TextView btnTv = findDirectSlotTextView(ch);
                                    if (btnTv == null || btnTv.getText() == null || btnTv.getText().length() == 0) continue;
                                    float w = (ch.getLayoutParams() instanceof LinearLayout.LayoutParams)
                                            ? Math.max(1f, ((LinearLayout.LayoutParams) ch.getLayoutParams()).weight)
                                            : 1f;
                                    int slotW = Math.max(1, Math.round(innerW2 * (w / sumW2)));
                                    int safePad = TypographyNormalizer.clamp(
                                            Math.round(slotW * 0.06f), Math.round(2f * density), Math.round(5f * density));
                                    int textAvail = Math.max(1, slotW - 2 * safePad);
                                    m2.add(new TypographyNormalizer.GroupMember(
                                            btnTv, btnTv.getText().toString(), textAvail, 0, safePad, safePad, true, ch));
                                }
                                unclipContainerSafely(deformGroup);
                                if (deformGroup instanceof LinearLayout) {
                                    mutated |= alignMixedIconRow((LinearLayout) deformGroup, density);
                                }
                                mutated |= solveStripPeerGroup(
                                        deformGroup,
                                        m2,
                                        TypographyNormalizer.ARCHETYPE_VERTICAL_ELASTIC_STRIP,
                                        Math.round(18f * density),
                                        true,
                                        false,
                                        false,
                                        0,
                                        epoch,
                                        "xuanquV2_" + visDeform,
                                        density);
                            }
                        }
                    }
                }
            }

            // 4. Expand LiuyebiSettingView (Bucket & Gradient Fill) dynamically from its ToggleSwitchView.
            View liuyebiBar = findView(root, "liuyebi_setting", 0);
            if (liuyebiBar != null && liuyebiBar.getVisibility() == View.VISIBLE) {
                mutated |= normalizeLiuyebiBar(liuyebiBar, root, usableW, freeCenterShiftX, density);
            }

            // 5. Top3DEditSettingView (Archetype III Rigid 1D strip): prevent equal phone weight=1 from squeezing.
            View viewMove = findView(root, "view_move", 0);
            View viewRotate = findView(root, "view_rorate", 0);
            View bgReset = findView(root, "bg_reset", 0);
            if (viewMove != null && viewRotate != null && bgReset != null
                    && viewMove.getParent() instanceof LinearLayout
                    && viewRotate.getParent() == viewMove.getParent()
                    && bgReset.getParent() == viewMove.getParent()) {
                LinearLayout bg3d = (LinearLayout) viewMove.getParent();
                if (bg3d.getVisibility() == View.VISIBLE) {
                    int padL = Math.round(10 * density);
                    int padR = Math.round(4 * density);
                    for (View slot : new View[]{viewMove, viewRotate}) {
                        TypographyNormalizer.captureContainerBaseline(slot);
                        if (slot.getLayoutParams() instanceof LinearLayout.LayoutParams) {
                            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) slot.getLayoutParams();
                            boolean slotChg = false;
                            if (lp.width != ViewGroup.LayoutParams.WRAP_CONTENT) {
                                lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                                slotChg = true;
                            }
                            if (lp.weight != 0f) {
                                lp.weight = 0f;
                                slotChg = true;
                            }
                            if (lp.leftMargin != padL || lp.rightMargin != padR) {
                                lp.leftMargin = padL;
                                lp.rightMargin = padR;
                                if (Build.VERSION.SDK_INT >= 17) {
                                    lp.setMarginStart(padL);
                                    lp.setMarginEnd(padR);
                                }
                                slotChg = true;
                            }
                            if (slotChg) {
                                slot.setLayoutParams(lp);
                                mutated = true;
                            }
                        }
                    }
                    for (String lblId : new String[]{"tv_move", "tv_rorate"}) {
                        View tv = findView(bg3d, lblId, 0);
                        if (tv instanceof TextView) {
                            centerHorizontalSlotLabel(tv.getParent() instanceof View ? (View) tv.getParent() : null, (TextView) tv);
                        }
                    }
                }
            }
            View tvEdit3d = findView(root, "tv_edit", 0);
            if (tvEdit3d instanceof TextView && tvEdit3d.getVisibility() == View.VISIBLE
                    && tvEdit3d.getParent() instanceof LinearLayout) {
                centerHorizontalSlotLabel((View) tvEdit3d.getParent(), (TextView) tvEdit3d);
            }
            View tvRotStretch = findView(root, "tv_rotate_stretch", 0);
            if (tvRotStretch instanceof TextView && tvRotStretch.getVisibility() == View.VISIBLE) {
                applySingleLineToolbarLabel((TextView) tvRotStretch);
            }
            polishGestureSwitchRows(root);

            // 6. PreView3D view tabs (Archetype III Rigid 1D strip): uniform single-line peer sizing.
            View tvPersp = findView(root, "tv_scenograph", 0);
            if (tvPersp instanceof TextView && tvPersp.getParent() instanceof LinearLayout) {
                LinearLayout tabRow = (LinearLayout) tvPersp.getParent();
                if (tabRow.getVisibility() == View.VISIBLE) {
                    View tvTop = findView(tabRow, "tv_top", 0);
                    View tvSide = findView(tabRow, "tv_side", 0);
                    View tvFront = findView(tabRow, "tv_front", 0);
                    setLinearItemWeight(tvTop, 1.0f);
                    setLinearItemWeight(tvSide, 1.0f);
                    setLinearItemWeight(tvFront, 1.05f);
                    setLinearItemWeight(tvPersp, 1.3f);
                    if (autoFitOn) {
                        View[] tabs = new View[]{tvTop, tvSide, tvFront, tvPersp};
                        int rowW = tabRow.getWidth() > 0 ? tabRow.getWidth() : usableW;
                        int innerTabW = Math.max(1, rowW - tabRow.getPaddingLeft() - tabRow.getPaddingRight());
                        float sumW = 1.0f + 1.0f + 1.05f + 1.3f;
                        float[] ws = new float[]{1.0f, 1.0f, 1.05f, 1.3f};
                        List<TypographyNormalizer.GroupMember> tabMembers = new ArrayList<>(4);
                        for (int i = 0; i < tabs.length; i++) {
                            if (tabs[i] instanceof TextView && tabs[i].getVisibility() != View.GONE) {
                                TextView tTv = (TextView) tabs[i];
                                CharSequence cs = tTv.getText();
                                if (cs == null || cs.length() == 0) continue;
                                int slotW = Math.max(1, Math.round(innerTabW * (ws[i] / sumW)));
                                int safePad = TypographyNormalizer.clamp(
                                        Math.round(slotW * 0.08f), Math.round(4f * density), Math.round(8f * density));
                                int avail = Math.max(1, slotW - 2 * safePad);
                                tabMembers.add(new TypographyNormalizer.GroupMember(
                                        tTv, cs.toString(), avail, 0, safePad, safePad, false, tTv));
                            }
                        }
                        mutated |= solveStripPeerGroup(
                                tabRow,
                                tabMembers,
                                TypographyNormalizer.ARCHETYPE_RIGID_1D_STRIP,
                                0,
                                false,
                                false,
                                false,
                                0,
                                epoch,
                                "d3Tabs",
                                density);
                    }
                }
            }

            // 7. LiquefySettingPhoneView (Archetype II Vertical Elastic Strip): solve 6 buttons uniformly.
            View liquefyPhone = findView(root, "liquefy_phone_view", 0);
            if (liquefyPhone != null && liquefyPhone.getVisibility() == View.VISIBLE) {
                TypographyNormalizer.captureContainerBaseline(liquefyPhone);
                int minPadH = Math.round(3 * density);
                int minPadB = Math.max(liquefyPhone.getPaddingBottom(), Math.round(5 * density));
                if (liquefyPhone.getPaddingLeft() != minPadH
                        || liquefyPhone.getPaddingRight() != minPadH
                        || liquefyPhone.getPaddingBottom() != minPadB) {
                    liquefyPhone.setPadding(minPadH, liquefyPhone.getPaddingTop(), minPadH, minPadB);
                    mutated = true;
                }
                int liqMarginPx = Math.round(1 * density);
                ViewGroup buttonRow = null;
                List<TextView> liqTvs = new ArrayList<>(6);
                List<View> liqSlots = new ArrayList<>(6);
                List<TypographyNormalizer.ContainerHeightSnapshot> liqSlotSnaps = new ArrayList<>(6);
                String[] liqIds = new String[]{
                        "liquefy_type_tv",
                        "liquid_recreate_tv_phone",
                        "liquid_sharping_tv_phone",
                        "liquid_reset_tv_phone",
                        "liquid_cancel_tv_phone",
                        "liquid_sure_tv_phone"
                };
                for (String lblId : liqIds) {
                    View v = findView(liquefyPhone, lblId, 0);
                    if (v instanceof TextView) {
                        TextView lTv = (TextView) v;
                        TypographyNormalizer.captureBaseline(lTv);
                        if (lTv.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lTv.getLayoutParams();
                            if (mlp.leftMargin != 0 || mlp.rightMargin != 0) {
                                setHorizontalMargins(lTv, 0, 0);
                                mutated = true;
                            }
                        }
                        liqTvs.add(lTv);
                        View slot = (lTv.getParent() instanceof View) ? (View) lTv.getParent() : null;
                        TypographyNormalizer.ContainerHeightSnapshot slotSnap = null;
                        if (slot != null) {
                            slotSnap = TypographyNormalizer.captureContainerBaseline(slot);
                            setHorizontalMargins(slot, liqMarginPx, liqMarginPx);
                            if (slot.getParent() instanceof ViewGroup && buttonRow == null) {
                                buttonRow = (ViewGroup) slot.getParent();
                            }
                        }
                        liqSlots.add(slot);
                        liqSlotSnaps.add(slotSnap);
                    }
                }
                if (autoFitOn && !liqTvs.isEmpty() && buttonRow != null) {
                    TypographyNormalizer.captureContainerBaseline(buttonRow);
                    if (buttonRow instanceof LinearLayout) {
                        LinearLayout bLl = (LinearLayout) buttonRow;
                        if (bLl.isBaselineAligned()) bLl.setBaselineAligned(false);
                        if ((bLl.getGravity() & Gravity.VERTICAL_GRAVITY_MASK) != Gravity.TOP) {
                            bLl.setGravity((bLl.getGravity() & ~Gravity.VERTICAL_GRAVITY_MASK) | Gravity.TOP);
                        }
                        ViewGroup.LayoutParams bLp = bLl.getLayoutParams();
                        if (bLp != null && bLp.height != ViewGroup.LayoutParams.WRAP_CONTENT) {
                            bLp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                            bLl.setLayoutParams(bLp);
                            mutated = true;
                        }
                    }
                    int parentInnerW = liquefyPhone.getWidth() > 0
                            ? Math.max(1, liquefyPhone.getWidth() - 2 * minPadH)
                            : Math.max(1, usableW - 2 * minPadH);
                    int rowW = Math.max(buttonRow.getWidth(), parentInnerW);
                    int count = Math.max(1, liqTvs.size());
                    int totalMargins = count * 2 * liqMarginPx;
                    int netRowW = Math.max(1, rowW - buttonRow.getPaddingLeft() - buttonRow.getPaddingRight() - totalMargins);
                    float equalSlotW = netRowW / (float) count;
                    int safePad = Math.round(1.5f * density);

                    Locale activeLoc = TranslationEngine.getActiveLocale();
                    Typeface activeTf = UIFontManager.getActiveTypeface(ctx);
                    float[] reqSlotW = new float[count];
                    float[] donorFloor = new float[count];
                    float[] allocW = new float[count];
                    float totalDeficit = 0f;
                    float totalSlack = 0f;
                    int minFloorPx = Math.round(42f * density);
                    float maxCapW = equalSlotW * 1.48f;
                    float minDonorCapW = equalSlotW * 0.72f;

                    for (int i = 0; i < count; i++) {
                        TextView lTv = liqTvs.get(i);
                        CharSequence cs = lTv.getText();
                        float wordW = 0f;
                        if (cs != null && cs.length() > 0) {
                            TextPaint lTp = TypographyNormalizer.getScratchTextPaint(lTv.getPaint());
                            TypographyNormalizer.BaselineSnapshot lSnap = TypographyNormalizer.captureBaseline(lTv);
                            if (lSnap != null && lSnap.origTextSize > 0f) lTp.setTextSize(lSnap.origTextSize);
                            TypographyNormalizer.preparePaintForScript(lTp, cs, activeLoc, activeTf);
                            wordW = TranslationEngine.computeMaxWordWidth(lTp, cs.toString());
                        }
                        reqSlotW[i] = Math.min(maxCapW, Math.max(minFloorPx, wordW + 2 * safePad + 5f * density));
                        donorFloor[i] = Math.max(minDonorCapW, reqSlotW[i] + 2f * density);
                        if (reqSlotW[i] > equalSlotW) {
                            totalDeficit += (reqSlotW[i] - equalSlotW);
                        } else if (donorFloor[i] < equalSlotW) {
                            totalSlack += (equalSlotW - donorFloor[i]);
                        }
                    }

                    boolean redistributed = totalDeficit > 1f && totalSlack > 1f && (buttonRow instanceof LinearLayout);
                    if (redistributed) {
                        float donate = Math.min(totalDeficit, totalSlack);
                        for (int i = 0; i < count; i++) {
                            if (reqSlotW[i] > equalSlotW) {
                                allocW[i] = equalSlotW + ((reqSlotW[i] - equalSlotW) * (donate / totalDeficit));
                            } else if (donorFloor[i] < equalSlotW) {
                                allocW[i] = equalSlotW - ((equalSlotW - donorFloor[i]) * (donate / totalSlack));
                            } else {
                                allocW[i] = equalSlotW;
                            }
                        }
                    } else {
                        for (int i = 0; i < count; i++) {
                            allocW[i] = equalSlotW;
                        }
                    }

                    List<TypographyNormalizer.GroupMember> liqMembers = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) {
                        TextView lTv = liqTvs.get(i);
                        View slot = liqSlots.get(i);
                        TypographyNormalizer.ContainerHeightSnapshot slotSnap = liqSlotSnaps.get(i);
                        if (buttonRow instanceof LinearLayout && slot != null && slot.getLayoutParams() instanceof LinearLayout.LayoutParams) {
                            LinearLayout.LayoutParams llp = (LinearLayout.LayoutParams) slot.getLayoutParams();
                            int targetLpW = redistributed ? 0 : (slotSnap != null ? slotSnap.origLpWidth : llp.width);
                            float targetWeight = redistributed ? Math.max(1f, allocW[i]) : (slotSnap != null ? slotSnap.origLpWeight : 1f);
                            if (llp.width != targetLpW || Math.abs(llp.weight - targetWeight) > 0.05f) {
                                llp.width = targetLpW;
                                llp.weight = targetWeight;
                                slot.setLayoutParams(llp);
                                mutated = true;
                            }
                        }
                        CharSequence cs = lTv.getText();
                        if (cs == null || cs.length() == 0) continue;
                        int textAvailW = Math.max(1, Math.round(allocW[i]) - 2 * safePad);
                        liqMembers.add(new TypographyNormalizer.GroupMember(
                                lTv, cs.toString(), textAvailW, 0, safePad, safePad, true, slot));
                    }
                    unclipContainerSafely(buttonRow);
                    unclipContainerSafely(liquefyPhone instanceof ViewGroup ? (ViewGroup) liquefyPhone : null);
                    mutated |= solveStripPeerGroup(
                            buttonRow,
                            liqMembers,
                            TypographyNormalizer.ARCHETYPE_VERTICAL_ELASTIC_STRIP,
                            Math.round(18f * density),
                            true,
                            true,
                            false,
                            0,
                            epoch,
                            "liquefyPhone",
                            density);
                }
            }
        } catch (Throwable ignored) {}
        return mutated;
    }

    private static TextView findDirectSlotTextView(View slot) {
        if (slot instanceof TextView) return (TextView) slot;
        if (slot instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) slot;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View ch = vg.getChildAt(i);
                if (ch instanceof TextView && ch.getVisibility() != View.GONE) {
                    return (TextView) ch;
                }
            }
        }
        return null;
    }

    /**
     * Archetype I (ELASTIC_2D): Expands vertical PopupWindow menus (e.g., MainActivity 3-dot menu)
     * horizontally up to 72% screen width with uniform 100% original font sizing and right-edge
     * anchor compensation so menus never push past the right viewport boundary or staircase font sizes.
     */
    private static final class PopupElasticSnapshot {
        final int origPwW;
        final int origPwH;
        final int origBubbleW;
        final int origBubbleH;
        final int origLlPadL;
        final int origLlPadR;
        final int origWinX;
        final int origWinY;
        final int origWinW;
        String lastAppliedSig = "";

        PopupElasticSnapshot(
                int origPwW,
                int origPwH,
                int origBubbleW,
                int origBubbleH,
                int origLlPadL,
                int origLlPadR,
                int origWinX,
                int origWinY,
                int origWinW) {
            this.origPwW = origPwW;
            this.origPwH = origPwH;
            this.origBubbleW = origBubbleW;
            this.origBubbleH = origBubbleH;
            this.origLlPadL = origLlPadL;
            this.origLlPadR = origLlPadR;
            this.origWinX = origWinX;
            this.origWinY = origWinY;
            this.origWinW = origWinW;
        }
    }

    private static final class DialogTitleSnapshot {
        final int origWidth;
        final int origPadL;
        final int origPadR;
        final int origMarginL;
        final int origMarginR;
        final int origMarginS;
        final int origMarginE;
        final int origGravity;
        final int origLlpGravity;
        final int origTextAlignment;

        DialogTitleSnapshot(TextView tv) {
            this.origPadL = tv.getPaddingLeft();
            this.origPadR = tv.getPaddingRight();
            this.origGravity = tv.getGravity();
            this.origTextAlignment = Build.VERSION.SDK_INT >= 17 ? tv.getTextAlignment() : View.TEXT_ALIGNMENT_INHERIT;
            ViewGroup.LayoutParams lp = tv.getLayoutParams();
            if (lp != null) {
                this.origWidth = lp.width;
                this.origLlpGravity = (lp instanceof LinearLayout.LayoutParams) ? ((LinearLayout.LayoutParams) lp).gravity : -1;
                if (lp instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
                    this.origMarginL = mlp.leftMargin;
                    this.origMarginR = mlp.rightMargin;
                    if (Build.VERSION.SDK_INT >= 17) {
                        this.origMarginS = mlp.getMarginStart();
                        this.origMarginE = mlp.getMarginEnd();
                    } else {
                        this.origMarginS = mlp.leftMargin;
                        this.origMarginE = mlp.rightMargin;
                    }
                } else {
                    this.origMarginL = 0;
                    this.origMarginR = 0;
                    this.origMarginS = 0;
                    this.origMarginE = 0;
                }
            } else {
                this.origWidth = ViewGroup.LayoutParams.WRAP_CONTENT;
                this.origLlpGravity = -1;
                this.origMarginL = 0;
                this.origMarginR = 0;
                this.origMarginS = 0;
                this.origMarginE = 0;
            }
        }
    }

    public static boolean optimizeElasticPopupMenus(View root) {
        if (root == null) return false;
        PopupWindow pw = getBoundPopupWindow(root);
        if (pw == null) return false;
        try {
            if (findView(root, "nsv_canvas", 0x7f090a13) != null
                    || findView(root, "nsv_setting", 0x7f090a17) != null
                    || findView(root, "nsv_import", 0x7f090a15) != null
                    || findView(root, "dialog_setting_iv", 0) != null
                    || findView(root, "toggleView", 0) != null
                    || findFastKeyPopupHeaderView(root, 0) != null) {
                return false;
            }
            LinearLayout menuCol = findVerticalMenuColumn(root, 0);
            if (menuCol == null) return false;

            View bubbleView = findView(root, "bubbleView", 0);
            if (bubbleView == null && menuCol.getParent() instanceof View) {
                bubbleView = (View) menuCol.getParent();
            }

            Object snapObj = root.getTag(TypographyNormalizer.TAG_POPUP_ELASTIC_SNAP);
            View winRoot = root.getRootView();
            int[] winLoc = new int[2];
            if (winRoot != null) winRoot.getLocationOnScreen(winLoc);
            WindowManager.LayoutParams wlp = (winRoot != null && winRoot.getLayoutParams() instanceof WindowManager.LayoutParams)
                    ? (WindowManager.LayoutParams) winRoot.getLayoutParams()
                    : null;

            if (!TranslationEngine.isTextAutoFitActive()) {
                if (snapObj instanceof PopupElasticSnapshot) {
                    PopupElasticSnapshot snap = (PopupElasticSnapshot) snapObj;
                    snap.lastAppliedSig = "";
                    if (menuCol.getPaddingLeft() != snap.origLlPadL || menuCol.getPaddingRight() != snap.origLlPadR) {
                        menuCol.setPadding(snap.origLlPadL, menuCol.getPaddingTop(), snap.origLlPadR, menuCol.getPaddingBottom());
                    }
                    if (bubbleView != null && bubbleView.getLayoutParams() != null) {
                        ViewGroup.LayoutParams bLp = bubbleView.getLayoutParams();
                        if (bLp.width != snap.origBubbleW || bLp.height != snap.origBubbleH) {
                            bLp.width = snap.origBubbleW;
                            bLp.height = snap.origBubbleH;
                            bubbleView.setLayoutParams(bLp);
                        }
                    }
                    for (int i = 0; i < menuCol.getChildCount(); i++) {
                        View ch = menuCol.getChildAt(i);
                        if (ch instanceof TextView) {
                            TypographyNormalizer.restoreBaseline((TextView) ch);
                        }
                    }
                    if (pw.isShowing() && snap.origPwW > 0) {
                        pw.update(snap.origWinX, snap.origWinY, snap.origPwW, snap.origPwH);
                    }
                }
                return false;
            }

            DisplayMetrics dm = root.getResources().getDisplayMetrics();
            int screenW = dm.widthPixels;
            int screenH = dm.heightPixels;
            float density = dm.density;
            if (screenW <= 0 || density <= 0f) return false;

            PopupElasticSnapshot snap;
            if (snapObj instanceof PopupElasticSnapshot) {
                snap = (PopupElasticSnapshot) snapObj;
            } else {
                int oPwW = pw.getWidth();
                int oPwH = pw.getHeight();
                int oBubW = (bubbleView != null && bubbleView.getLayoutParams() != null) ? bubbleView.getLayoutParams().width : 0;
                int oBubH = (bubbleView != null && bubbleView.getLayoutParams() != null) ? bubbleView.getLayoutParams().height : 0;
                int oWinW = (winRoot != null && winRoot.getWidth() > 0) ? winRoot.getWidth() : oPwW;
                if (oWinW <= 0) return false;
                snap = new PopupElasticSnapshot(
                        oPwW > 0 ? oPwW : oWinW,
                        oPwH,
                        oBubW,
                        oBubH,
                        menuCol.getPaddingLeft(),
                        menuCol.getPaddingRight(),
                        wlp != null ? wlp.x : winLoc[0],
                        wlp != null ? wlp.y : winLoc[1],
                        oWinW);
                root.setTag(TypographyNormalizer.TAG_POPUP_ELASTIC_SNAP, snap);
            }

            List<TextView> rows = new ArrayList<>(menuCol.getChildCount());
            float maxRowTextW = 0f;
            int maxRowPadH = 0;
            Locale activeLoc = TranslationEngine.getActiveLocale();
            Typeface activeTf = UIFontManager.getActiveTypeface(root.getContext());
            boolean mutated = false;

            for (int i = 0; i < menuCol.getChildCount(); i++) {
                View ch = menuCol.getChildAt(i);
                if (ch.getVisibility() == View.GONE || !(ch instanceof TextView)) continue;
                TextView tv = (TextView) ch;
                CharSequence cs = tv.getText();
                if (cs == null || cs.length() == 0) continue;
                TypographyNormalizer.BaselineSnapshot b = TypographyNormalizer.captureBaseline(tv);
                mutated |= TypographyNormalizer.prepareTextViewForPipeline(
                        tv, cs, activeLoc, activeTf, TranslationEngine.isDynamicI18nEnabled());
                rows.add(tv);
                float w = measureLabelDesiredWidth(tv);
                if (w > maxRowTextW) maxRowTextW = w;
                int pH = b.origPadL + b.origPadR;
                if (pH > maxRowPadH) maxRowPadH = pH;
            }
            if (rows.isEmpty()) return mutated;

            int wantLlPadL = snap.origLlPadL;
            int wantLlPadR = snap.origLlPadR;
            if (wantLlPadL > Math.round(4f * density) && wantLlPadR < Math.round(4f * density)) {
                wantLlPadR = wantLlPadL;
            }
            if (menuCol.getPaddingLeft() != wantLlPadL || menuCol.getPaddingRight() != wantLlPadR) {
                menuCol.setPadding(wantLlPadL, menuCol.getPaddingTop(), wantLlPadR, menuCol.getPaddingBottom());
                mutated = true;
            }

            int bubPadH = (bubbleView != null) ? (bubbleView.getPaddingLeft() + bubbleView.getPaddingRight()) : 0;
            int curWinW = (winRoot != null && winRoot.getWidth() > 0) ? winRoot.getWidth() : snap.origWinW;
            int curBubW = (bubbleView != null && bubbleView.getWidth() > 0) ? bubbleView.getWidth() : curWinW;
            int outerShadowChrome = Math.max(0, snap.origWinW - (snap.origBubbleW > 0 ? snap.origBubbleW : curBubW));
            if (outerShadowChrome == 0 && bubbleView != null && bubbleView.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams bMlp = (ViewGroup.MarginLayoutParams) bubbleView.getLayoutParams();
                outerShadowChrome = bMlp.leftMargin + bMlp.rightMargin + root.getPaddingLeft() + root.getPaddingRight();
            }

            int neededInnerW = (int) Math.ceil(maxRowTextW) + maxRowPadH + wantLlPadL + wantLlPadR + bubPadH + Math.round(8f * density);
            int maxWinCapW = Math.min(Math.round(screenW * 0.72f), screenW - Math.round(16f * density));
            int origBubPx = snap.origBubbleW > 0 ? snap.origBubbleW : curBubW;
            int minWinW = snap.origWinW > 0 ? snap.origWinW : origBubPx + outerShadowChrome;
            int targetWinW = TypographyNormalizer.clamp(neededInnerW + outerShadowChrome, minWinW, maxWinCapW);
            int targetBubW = Math.max(origBubPx, targetWinW - outerShadowChrome);

            if (bubbleView != null && bubbleView.getLayoutParams() != null) {
                ViewGroup.LayoutParams bLp = bubbleView.getLayoutParams();
                if (bLp.width != targetBubW) {
                    bLp.width = targetBubW;
                    bubbleView.setLayoutParams(bLp);
                    mutated = true;
                }
                if (bLp.height > 0) {
                    bLp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    bubbleView.setLayoutParams(bLp);
                }
            }

            int rowTextAvailW = Math.max(1, targetBubW - bubPadH - wantLlPadL - wantLlPadR - maxRowPadH - Math.round(2f * density));
            int epoch = ++sSolverPassEpoch;
            List<TypographyNormalizer.GroupMember> menuMembers = new ArrayList<>(rows.size());
            boolean anyRowWrapped = false;
            for (int i = 0; i < rows.size(); i++) {
                TextView tv = rows.get(i);
                TypographyNormalizer.BaselineSnapshot b = TypographyNormalizer.captureBaseline(tv);
                menuMembers.add(new TypographyNormalizer.GroupMember(
                        tv, tv.getText().toString(), rowTextAvailW, 0, b.origPadL, b.origPadR, true, tv));
            }
            TypographyNormalizer.GroupSolveResult gRes = TypographyNormalizer.solvePeerGroup(
                    menuMembers, TypographyNormalizer.ARCHETYPE_ELASTIC_2D, Math.round(48f * density), activeLoc, activeTf, dm);
            for (int i = 0; i < menuMembers.size(); i++) {
                TypographyNormalizer.GroupMember m = menuMembers.get(i);
                if (m.plan == null) continue;
                float applyPx = m.plan.extremeOutlier
                        ? m.plan.targetPx
                        : Math.min(m.plan.targetPx, gRes.groupPx);
                if (m.plan.targetLines >= 2) {
                    anyRowWrapped = true;
                    if (m.tv.getLayoutParams() != null
                            && m.tv.getLayoutParams().height == ViewGroup.LayoutParams.MATCH_PARENT) {
                        ViewGroup.LayoutParams rlp = m.tv.getLayoutParams();
                        rlp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                        m.tv.setLayoutParams(rlp);
                    }
                }
                mutated |= TypographyNormalizer.applySolvedPlanToTextView(
                        m.tv,
                        m.plan,
                        applyPx,
                        m.safePadL,
                        m.safePadR,
                        false,
                        false,
                        0,
                        epoch,
                        "popupMenu_" + targetBubW);
            }

            int targetWinH = snap.origPwH;
            if (anyRowWrapped) {
                root.measure(
                        View.MeasureSpec.makeMeasureSpec(targetWinW, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(Math.max(1, screenH - Math.round(32f * density)), View.MeasureSpec.AT_MOST));
                int measuredH = root.getMeasuredHeight();
                if (measuredH > 0) {
                    targetWinH = Math.min(Math.max(snap.origPwH, measuredH + Math.round(8f * density)),
                            screenH - Math.round(24f * density));
                }
            } else {
                for (int i = 0; i < menuCol.getChildCount(); i++) {
                    View ch = menuCol.getChildAt(i);
                    if (ch instanceof TextView) {
                        TypographyNormalizer.BaselineSnapshot b = TypographyNormalizer.captureBaseline((TextView) ch);
                        ViewGroup.LayoutParams rlp = ch.getLayoutParams();
                        if (b != null && rlp != null && rlp.height != b.origLpHeight) {
                            rlp.height = b.origLpHeight;
                            ch.setLayoutParams(rlp);
                        }
                    }
                }
            }

            boolean rightAnchoredGravity = wlp != null
                    && (((wlp.gravity & Gravity.HORIZONTAL_GRAVITY_MASK) == Gravity.RIGHT)
                    || (Build.VERSION.SDK_INT >= 17 && (wlp.gravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) == Gravity.END));
            int newX = snap.origWinX;
            if (rightAnchoredGravity) {
                int rightMargin = Math.max(Math.round(8f * density), snap.origWinX);
                int maxWByLeftEdge = screenW - rightMargin - Math.round(8f * density);
                if (targetWinW > maxWByLeftEdge && maxWByLeftEdge > 0) {
                    targetWinW = maxWByLeftEdge;
                }
            } else {
                int deltaW = targetWinW - snap.origWinW;
                int screenMargin = Math.round(8f * density);
                if (winLoc[0] + snap.origWinW >= screenW * 0.50f || snap.origWinX + targetWinW > screenW - screenMargin) {
                    newX = snap.origWinX - deltaW;
                }
                newX = TypographyNormalizer.clamp(newX, screenMargin, Math.max(screenMargin, screenW - screenMargin - targetWinW));
            }

            String applySig = targetWinW + "|" + targetWinH + "|" + newX;
            if (!applySig.equals(snap.lastAppliedSig) && pw.isShowing()) {
                snap.lastAppliedSig = applySig;
                pw.update(newX, snap.origWinY, targetWinW, targetWinH > 0 ? targetWinH : ViewGroup.LayoutParams.WRAP_CONTENT);
                mutated = true;
            }
            return mutated;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static LinearLayout findVerticalMenuColumn(View v, int depth) {
        if (v == null || depth > 8 || v.getVisibility() == View.GONE) return null;
        if (v instanceof ScrollView || v.getClass().getName().endsWith("RecyclerView") || v instanceof android.widget.ListView) {
            return null;
        }
        if (v instanceof LinearLayout && ((LinearLayout) v).getOrientation() == LinearLayout.VERTICAL) {
            LinearLayout ll = (LinearLayout) v;
            int visTextRows = 0;
            float firstSize = -1f;
            boolean uniform = true;
            boolean allLeafText = true;
            for (int i = 0; i < ll.getChildCount(); i++) {
                View ch = ll.getChildAt(i);
                if (ch.getVisibility() == View.GONE) continue;
                if (!(ch instanceof TextView) || ch instanceof CompoundButton) {
                    allLeafText = false;
                    break;
                }
                TextView tv = (TextView) ch;
                if (tv.getText() == null || tv.getText().length() == 0) continue;
                TypographyNormalizer.BaselineSnapshot b = TypographyNormalizer.captureBaseline(tv);
                float sz = b != null ? b.origTextSize : tv.getTextSize();
                if (firstSize < 0f) firstSize = sz;
                else if (Math.abs(firstSize - sz) > 1.5f) uniform = false;
                visTextRows++;
            }
            if (allLeafText && uniform && visTextRows >= 2) {
                return ll;
            }
        }
        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++) {
                LinearLayout found = findVerticalMenuColumn(vg.getChildAt(i), depth + 1);
                if (found != null) return found;
            }
        }
        return null;
    }

    /**
     * Fixes CommonDialog title horizontal axis asymmetry caused by uneven left/right margins,
     * uneven paddings, or asymmetric INVISIBLE corner placeholders, cleanly skipping any header
     * flanked by visible corner buttons and preserving original baselines for toggle OFF restore.
     */
    public static boolean normalizeDialogTitles(View root) {
        if (root == null) return false;
        boolean isDialog = Boolean.TRUE.equals(root.getTag(0x7e090d04)); // TAG_KEY_DIALOG_ROOT
        View folderEt = findView(root, "folder_name_et", 0);
        if (!isDialog && folderEt == null) return false;
        if (findView(root, "cloud_sync_switch_container", 0x7f09037a) != null
                || findView(root, "cloud_sync_switch", 0x7f090379) != null) {
            return false;
        }
        boolean mutated = false;
        try {
            if (!TranslationEngine.isTextAutoFitActive()) {
                return restoreDialogTitlesRecursive(root);
            }
            if (folderEt != null && folderEt.getParent() instanceof ViewGroup) {
                ViewGroup card = (ViewGroup) folderEt.getParent();
                for (int i = 0; i < card.getChildCount(); i++) {
                    View ch = card.getChildAt(i);
                    if (ch == folderEt) break;
                    if (ch instanceof TextView && ch.getVisibility() == View.VISIBLE) {
                        mutated |= symCenterDialogTitleView((TextView) ch);
                        break;
                    }
                }
            }
            Context ctx = root.getContext();
            if (ctx != null) {
                String[] titleNames = new String[]{"title_tv", "tv_title", "dialog_title"};
                for (String tName : titleNames) {
                    int idT = LibreResourceResolver.resolveViewId(ctx, tName, 0);
                    if (idT == 0) continue;
                    View tvRaw = root.findViewById(idT);
                    if (tvRaw instanceof TextView && tvRaw.getVisibility() == View.VISIBLE) {
                        TextView tv = (TextView) tvRaw;
                        if (isDialogTitleCenteredIntent(tv)) {
                            mutated |= symCenterDialogTitleView(tv);
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
        return mutated;
    }

    private static boolean isDialogTitleCenteredIntent(TextView tv) {
        if (tv == null || tv.getText() == null) return false;
        String s = tv.getText().toString();
        if (s.isEmpty() || s.length() > 48 || s.contains("\n\n")) return false;
        int grav = tv.getGravity() & Gravity.HORIZONTAL_GRAVITY_MASK;
        if (grav == Gravity.CENTER_HORIZONTAL) return true;
        if (Build.VERSION.SDK_INT >= 17 && tv.getTextAlignment() == View.TEXT_ALIGNMENT_CENTER) return true;
        if (tv.getLayoutParams() instanceof RelativeLayout.LayoutParams) {
            int[] rules = ((RelativeLayout.LayoutParams) tv.getLayoutParams()).getRules();
            return rules[RelativeLayout.CENTER_HORIZONTAL] != 0 || rules[RelativeLayout.CENTER_IN_PARENT] != 0;
        }
        return tv.getLayoutParams() instanceof LinearLayout.LayoutParams
                && (((LinearLayout.LayoutParams) tv.getLayoutParams()).gravity & Gravity.HORIZONTAL_GRAVITY_MASK) == Gravity.CENTER_HORIZONTAL;
    }

    private static boolean symCenterDialogTitleView(TextView tv) {
        if (tv == null || !(tv.getParent() instanceof ViewGroup)) return false;
        ViewGroup parent = (ViewGroup) tv.getParent();
        if (parent instanceof ScrollView || parent.getClass().getName().endsWith("RecyclerView")) {
            return false;
        }
        // Abort if any visible side corner button overlaps or shares the header horizontal band.
        int tTop = tv.getTop();
        int tBot = tv.getBottom();
        boolean isHorizontalRow = parent instanceof LinearLayout
                && ((LinearLayout) parent).getOrientation() == LinearLayout.HORIZONTAL;
        for (int i = 0; i < parent.getChildCount(); i++) {
            View sib = parent.getChildAt(i);
            if (sib == null || sib == tv) continue;
            if (sib.getVisibility() == View.VISIBLE) {
                boolean sameBand = isHorizontalRow || (tBot > tTop && sib.getBottom() > tTop + 4 && sib.getTop() < tBot - 4);
                if (sameBand && sib.getWidth() > 0 && !(sib instanceof android.widget.EditText)) {
                    return false;
                }
            } else if (sib.getVisibility() == View.INVISIBLE && isHorizontalRow) {
                if (sib.getTag(TypographyNormalizer.TAG_ORIG_VISIBILITY) == null) {
                    sib.setTag(TypographyNormalizer.TAG_ORIG_VISIBILITY, Integer.valueOf(View.INVISIBLE));
                }
                sib.setVisibility(View.GONE);
            }
        }

        if (!(tv.getTag(TypographyNormalizer.TAG_DIALOG_TITLE_SNAP) instanceof DialogTitleSnapshot)) {
            tv.setTag(TypographyNormalizer.TAG_DIALOG_TITLE_SNAP, new DialogTitleSnapshot(tv));
        }
        DialogTitleSnapshot snap = (DialogTitleSnapshot) tv.getTag(TypographyNormalizer.TAG_DIALOG_TITLE_SNAP);
        boolean mutated = false;

        int symPad = Math.min(snap.origPadL, snap.origPadR);
        if (tv.getPaddingLeft() != symPad || tv.getPaddingRight() != symPad) {
            tv.setPadding(symPad, tv.getPaddingTop(), symPad, tv.getPaddingBottom());
            mutated = true;
        }
        int wantGrav = (snap.origGravity & Gravity.VERTICAL_GRAVITY_MASK) | Gravity.CENTER_HORIZONTAL;
        if (wantGrav == Gravity.CENTER_HORIZONTAL) {
            wantGrav = Gravity.CENTER;
        }
        if (tv.getGravity() != wantGrav) {
            tv.setGravity(wantGrav);
            mutated = true;
        }
        if (Build.VERSION.SDK_INT >= 17 && tv.getTextAlignment() != View.TEXT_ALIGNMENT_CENTER) {
            tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            mutated = true;
        }
        ViewGroup.LayoutParams lp = tv.getLayoutParams();
        if (lp != null) {
            boolean lpChanged = false;
            if (!isHorizontalRow && lp.width != ViewGroup.LayoutParams.WRAP_CONTENT) {
                lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                lpChanged = true;
            }
            if (lp instanceof LinearLayout.LayoutParams) {
                LinearLayout.LayoutParams llp = (LinearLayout.LayoutParams) lp;
                if (llp.gravity != Gravity.CENTER_HORIZONTAL) {
                    llp.gravity = Gravity.CENTER_HORIZONTAL;
                    lpChanged = true;
                }
            }
            if (lp instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
                int symM = isHorizontalRow ? Math.min(snap.origMarginL, snap.origMarginR) : Math.max(snap.origMarginL, snap.origMarginR);
                if (mlp.leftMargin != symM || mlp.rightMargin != symM) {
                    mlp.leftMargin = symM;
                    mlp.rightMargin = symM;
                    lpChanged = true;
                }
                if (Build.VERSION.SDK_INT >= 17 && (mlp.getMarginStart() != symM || mlp.getMarginEnd() != symM)) {
                    mlp.setMarginStart(symM);
                    mlp.setMarginEnd(symM);
                    lpChanged = true;
                }
            }
            if (lpChanged) {
                tv.setLayoutParams(lp);
                mutated = true;
            }
        }
        if (mutated) {
            tv.requestLayout();
            tv.invalidate();
        }
        return mutated;
    }

    private static boolean restoreDialogTitlesRecursive(View v) {
        if (v == null) return false;
        boolean mutated = false;
        Object visObj = v.getTag(TypographyNormalizer.TAG_ORIG_VISIBILITY);
        if (visObj instanceof Integer && v.getVisibility() != (Integer) visObj) {
            v.setVisibility((Integer) visObj);
            mutated = true;
        }
        if (v instanceof TextView && v.getTag(TypographyNormalizer.TAG_DIALOG_TITLE_SNAP) instanceof DialogTitleSnapshot) {
            TextView tv = (TextView) v;
            DialogTitleSnapshot s = (DialogTitleSnapshot) v.getTag(TypographyNormalizer.TAG_DIALOG_TITLE_SNAP);
            if (tv.getPaddingLeft() != s.origPadL || tv.getPaddingRight() != s.origPadR) {
                tv.setPadding(s.origPadL, tv.getPaddingTop(), s.origPadR, tv.getPaddingBottom());
                mutated = true;
            }
            if (tv.getGravity() != s.origGravity) {
                tv.setGravity(s.origGravity);
                mutated = true;
            }
            if (Build.VERSION.SDK_INT >= 17 && tv.getTextAlignment() != s.origTextAlignment) {
                tv.setTextAlignment(s.origTextAlignment);
                mutated = true;
            }
            ViewGroup.LayoutParams lp = tv.getLayoutParams();
            if (lp != null) {
                boolean lpChg = false;
                if (lp.width != s.origWidth) {
                    lp.width = s.origWidth;
                    lpChg = true;
                }
                if (lp instanceof LinearLayout.LayoutParams && ((LinearLayout.LayoutParams) lp).gravity != s.origLlpGravity) {
                    ((LinearLayout.LayoutParams) lp).gravity = s.origLlpGravity;
                    lpChg = true;
                }
                if (lp instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
                    if (mlp.leftMargin != s.origMarginL || mlp.rightMargin != s.origMarginR) {
                        mlp.leftMargin = s.origMarginL;
                        mlp.rightMargin = s.origMarginR;
                        lpChg = true;
                    }
                    if (Build.VERSION.SDK_INT >= 17 && (mlp.getMarginStart() != s.origMarginS || mlp.getMarginEnd() != s.origMarginE)) {
                        mlp.setMarginStart(s.origMarginS);
                        mlp.setMarginEnd(s.origMarginE);
                        lpChg = true;
                    }
                }
                if (lpChg) {
                    tv.setLayoutParams(lp);
                    mutated = true;
                }
            }
        }
        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++) {
                mutated |= restoreDialogTitlesRecursive(vg.getChildAt(i));
            }
        }
        return mutated;
    }

    private static boolean isInsideReplayBottomBar(View v) {
        if (v == null) return false;
        if (matchesId(v, "replay_bottom_view", 0x7f090b0c)) return true;
        ViewParent cur = v.getParent();
        for (int depth = 0; cur instanceof View && depth < 5; depth++) {
            View pv = (View) cur;
            if (matchesId(pv, "replay_bottom_view", 0x7f090b0c)) {
                return true;
            }
            cur = pv.getParent();
        }
        return false;
    }

    private static int[] ensureReplayOrigTopDrawableSaved(TextView tv) {
        if (tv == null) return null;
        Object existing = tv.getTag(TAG_REPLAY_ORIG_TOP_DRAWABLE_BOUNDS);
        if (existing instanceof int[]) {
            return (int[]) existing;
        }
        Drawable[] ds = tv.getCompoundDrawables();
        Drawable topD = (ds != null && ds.length > 1) ? ds[1] : null;
        if (topD == null && Build.VERSION.SDK_INT >= 17) {
            Drawable[] rds = tv.getCompoundDrawablesRelative();
            topD = (rds != null && rds.length > 1) ? rds[1] : null;
        }
        int left = 0;
        int top = 0;
        int right = 0;
        int bottom = 0;
        if (topD != null) {
            Rect b = topD.getBounds();
            if (b != null && b.width() > 0 && b.height() > 0) {
                left = b.left;
                top = b.top;
                right = b.right;
                bottom = b.bottom;
            } else {
                right = Math.max(0, topD.getIntrinsicWidth());
                bottom = Math.max(0, topD.getIntrinsicHeight());
            }
        }
        int[] snap = new int[]{
            left,
            top,
            right,
            bottom,
            tv.getCompoundDrawablePadding(),
            tv.getIncludeFontPadding() ? 1 : 0,
            (Build.VERSION.SDK_INT >= 28 && tv.isFallbackLineSpacing()) ? 1 : 0
        };
        tv.setTag(TAG_REPLAY_ORIG_TOP_DRAWABLE_BOUNDS, snap);
        return snap;
    }

    private static boolean restoreReplayTopDrawableBaseline(TextView tv) {
        if (tv == null) return false;
        Object obj = tv.getTag(TAG_REPLAY_ORIG_TOP_DRAWABLE_BOUNDS);
        if (!(obj instanceof int[])) return false;
        int[] snap = (int[]) obj;
        boolean mutated = false;
        Drawable[] ds = tv.getCompoundDrawables();
        Drawable topD = (ds != null && ds.length > 1) ? ds[1] : null;
        boolean useRelative = false;
        if (topD == null && Build.VERSION.SDK_INT >= 17) {
            ds = tv.getCompoundDrawablesRelative();
            topD = (ds != null && ds.length > 1) ? ds[1] : null;
            useRelative = (topD != null);
        }
        if (topD != null && snap[2] > snap[0] && snap[3] > snap[1]) {
            Rect cur = topD.getBounds();
            if (cur.left != snap[0] || cur.top != snap[1] || cur.right != snap[2] || cur.bottom != snap[3]) {
                topD = topD.mutate();
                topD.setBounds(snap[0], snap[1], snap[2], snap[3]);
                if (useRelative && Build.VERSION.SDK_INT >= 17) {
                    tv.setCompoundDrawablesRelative(ds[0], topD, ds[2], ds[3]);
                } else {
                    tv.setCompoundDrawables(ds[0], topD, ds[2], ds[3]);
                }
                mutated = true;
            }
        }
        if (tv.getCompoundDrawablePadding() != snap[4]) {
            tv.setCompoundDrawablePadding(snap[4]);
            mutated = true;
        }
        boolean wantIncPad = snap[5] != 0;
        if (tv.getIncludeFontPadding() != wantIncPad) {
            tv.setIncludeFontPadding(wantIncPad);
            mutated = true;
        }
        if (Build.VERSION.SDK_INT >= 28) {
            boolean wantFallbackLs = snap[6] != 0;
            if (tv.isFallbackLineSpacing() != wantFallbackLs) {
                tv.setFallbackLineSpacing(wantFallbackLs);
                mutated = true;
            }
        }
        tv.setTag(TAG_REPLAY_ORIG_TOP_DRAWABLE_BOUNDS, null);
        return mutated;
    }

    /**
     * Synchronizes icon box bounds, top-anchored slot alignment, font padding metrics, and uniform
     * peer font size across all FrameLayout button slots inside ReplayActivity replay_bottom_view,
     * including stacked toggle twins (pause_btn/play_btn and normal_mode/strengthen_mode).
     */
    public static boolean optimizeReplayBottomBar(View root) {
        return optimizeReplayBottomBarInternal(root, ++sSolverPassEpoch);
    }

    private static boolean optimizeReplayBottomBarInternal(View root, int epoch) {
        if (root == null || !TranslationEngine.isTextAutoFitActive()) return false;
        if (!TranslationEngine.resolveHostActivityName(root).endsWith("ReplayActivity")) {
            return false;
        }
        try {
            View bottomView = findView(root, "replay_bottom_view", 0x7f090b0c);
            if (!(bottomView instanceof ViewGroup) || bottomView.getVisibility() == View.GONE) {
                return false;
            }
            ViewGroup bottomGroup = (ViewGroup) bottomView;
            LinearLayout bottomRow = null;
            for (int i = 0; i < bottomGroup.getChildCount(); i++) {
                View ch = bottomGroup.getChildAt(i);
                if (ch instanceof LinearLayout
                        && ((LinearLayout) ch).getOrientation() == LinearLayout.HORIZONTAL) {
                    bottomRow = (LinearLayout) ch;
                    break;
                }
            }
            if (bottomRow == null || bottomRow.getVisibility() == View.GONE) {
                return false;
            }

            DisplayMetrics dm = bottomRow.getResources().getDisplayMetrics();
            float density = dm.density;
            if (density <= 0f) return false;

            boolean mutated = false;
            TypographyNormalizer.captureContainerBaseline(bottomGroup);
            TypographyNormalizer.captureContainerBaseline(bottomRow);
            unclipContainerSafely(bottomGroup);
            unclipContainerSafely(bottomRow);

            if (bottomRow.isBaselineAligned()) {
                bottomRow.setBaselineAligned(false);
                mutated = true;
            }
            int rowGrav = bottomRow.getGravity();
            if ((rowGrav & Gravity.VERTICAL_GRAVITY_MASK) != Gravity.TOP) {
                bottomRow.setGravity((rowGrav & ~Gravity.VERTICAL_GRAVITY_MASK) | Gravity.TOP);
                mutated = true;
            }

            List<ViewGroup> activeSlots = new ArrayList<>(6);
            List<TextView> allButtonTvs = new ArrayList<>(8);
            List<ViewGroup> ownerSlots = new ArrayList<>(8);
            float totalWeight = 0f;
            boolean needTallFontMetrics = false;

            for (int i = 0; i < bottomRow.getChildCount(); i++) {
                View slotView = bottomRow.getChildAt(i);
                if (!(slotView instanceof ViewGroup) || slotView.getVisibility() == View.GONE) {
                    continue;
                }
                ViewGroup slot = (ViewGroup) slotView;
                TypographyNormalizer.captureContainerBaseline(slot);
                ViewGroup.LayoutParams rawSlp = slot.getLayoutParams();
                if (rawSlp instanceof LinearLayout.LayoutParams) {
                    LinearLayout.LayoutParams slp = (LinearLayout.LayoutParams) rawSlp;
                    boolean slpChanged = false;
                    if (slp.height != ViewGroup.LayoutParams.WRAP_CONTENT) {
                        slp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                        slpChanged = true;
                    }
                    if ((slp.gravity & Gravity.VERTICAL_GRAVITY_MASK) != Gravity.TOP) {
                        slp.gravity = (slp.gravity <= 0)
                                ? Gravity.TOP
                                : ((slp.gravity & ~Gravity.VERTICAL_GRAVITY_MASK) | Gravity.TOP);
                        slpChanged = true;
                    }
                    if (slpChanged) {
                        slot.setLayoutParams(slp);
                        mutated = true;
                    }
                    totalWeight += (slp.weight > 0f ? slp.weight : 1f);
                } else {
                    totalWeight += 1f;
                }

                boolean hasVisibleChild = false;
                for (int j = 0; j < slot.getChildCount(); j++) {
                    View sub = slot.getChildAt(j);
                    if (sub instanceof TextView && sub.getVisibility() == View.VISIBLE) {
                        hasVisibleChild = true;
                        break;
                    }
                }
                for (int j = 0; j < slot.getChildCount(); j++) {
                    View sub = slot.getChildAt(j);
                    if (!(sub instanceof TextView)) continue;
                    TextView tv = (TextView) sub;
                    if (tv.getVisibility() == View.GONE) {
                        mutated |= TranslationEngine.translateSingleTextView(tv);
                    } else if (hasVisibleChild && tv.getVisibility() == View.INVISIBLE) {
                        mutated |= TranslationEngine.translateSingleTextView(tv);
                        tv.setVisibility(View.GONE);
                        mutated = true;
                    }
                    TypographyNormalizer.captureBaseline(tv);
                    ensureReplayOrigTopDrawableSaved(tv);
                    CharSequence cs = tv.getText();
                    if (tv.getIncludeFontPadding()
                            || (cs != null && (TypographyNormalizer.containsNonAscii(cs)
                                    || TypographyNormalizer.classifyScript(cs)
                                            == TypographyNormalizer.SCRIPT_COMPLEX_SHAPING_OR_TALL))) {
                        needTallFontMetrics = true;
                    }
                    allButtonTvs.add(tv);
                    ownerSlots.add(slot);
                }
                activeSlots.add(slot);
            }

            if (activeSlots.size() < 2 || allButtonTvs.size() < 2 || totalWeight <= 0f) {
                return mutated;
            }

            int targetIconH = Math.round(20f * density);
            int refPadTop = Math.round(5f * density);
            int refPadBot = Math.round(5f * density);
            int refDrawablePad = Math.round(4f * density);
            View backRef = findView(bottomRow, "back_btn", 0x7f0900f5);
            if (backRef instanceof TextView) {
                TextView backTv = (TextView) backRef;
                TypographyNormalizer.BaselineSnapshot backSnap = TypographyNormalizer.captureBaseline(backTv);
                int[] backOrigD = ensureReplayOrigTopDrawableSaved(backTv);
                if (backOrigD != null && (backOrigD[3] - backOrigD[1]) > 0) {
                    targetIconH = backOrigD[3] - backOrigD[1];
                    refDrawablePad = backOrigD[4];
                }
                if (backSnap != null) {
                    refPadTop = backSnap.origPadT;
                    refPadBot = backSnap.origPadB;
                }
            }

            int targetFlpGrav = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            for (int i = 0; i < allButtonTvs.size(); i++) {
                TextView tv = allButtonTvs.get(i);
                int[] origD = ensureReplayOrigTopDrawableSaved(tv);
                Drawable[] ds = tv.getCompoundDrawables();
                Drawable topD = (ds != null && ds.length > 1) ? ds[1] : null;
                boolean useRel = false;
                if (topD == null && Build.VERSION.SDK_INT >= 17) {
                    ds = tv.getCompoundDrawablesRelative();
                    topD = (ds != null && ds.length > 1) ? ds[1] : null;
                    useRel = (topD != null);
                }
                if (topD != null && origD != null) {
                    int origW = origD[2] - origD[0];
                    int origH = origD[3] - origD[1];
                    if (origW > 0 && origH > 0) {
                        int scaledW = (origH == targetIconH)
                                ? origW
                                : Math.max(1, Math.round(origW * (targetIconH / (float) origH)));
                        Rect cur = topD.getBounds();
                        if (cur.left != 0 || cur.top != 0 || cur.right != scaledW || cur.bottom != targetIconH) {
                            topD = topD.mutate();
                            topD.setBounds(0, 0, scaledW, targetIconH);
                            if (useRel && Build.VERSION.SDK_INT >= 17) {
                                tv.setCompoundDrawablesRelative(ds[0], topD, ds[2], ds[3]);
                            } else {
                                tv.setCompoundDrawables(ds[0], topD, ds[2], ds[3]);
                            }
                            mutated = true;
                        }
                    }
                }
                if (tv.getCompoundDrawablePadding() != refDrawablePad) {
                    tv.setCompoundDrawablePadding(refDrawablePad);
                    mutated = true;
                }
                if (tv.getPaddingTop() != refPadTop || tv.getPaddingBottom() != refPadBot) {
                    tv.setPadding(tv.getPaddingLeft(), refPadTop, tv.getPaddingRight(), refPadBot);
                    mutated = true;
                }
                if (needTallFontMetrics) {
                    if (!tv.getIncludeFontPadding()) {
                        tv.setIncludeFontPadding(true);
                        mutated = true;
                    }
                    if (Build.VERSION.SDK_INT >= 28 && !tv.isFallbackLineSpacing()) {
                        tv.setFallbackLineSpacing(true);
                        mutated = true;
                    }
                }
                ViewGroup.LayoutParams rawLp = tv.getLayoutParams();
                if (rawLp instanceof android.widget.FrameLayout.LayoutParams) {
                    android.widget.FrameLayout.LayoutParams flp =
                            (android.widget.FrameLayout.LayoutParams) rawLp;
                    boolean flpChanged = false;
                    if (flp.width != ViewGroup.LayoutParams.MATCH_PARENT) {
                        flp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                        flpChanged = true;
                    }
                    if (flp.height != ViewGroup.LayoutParams.WRAP_CONTENT) {
                        flp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                        flpChanged = true;
                    }
                    if (flp.gravity != targetFlpGrav) {
                        flp.gravity = targetFlpGrav;
                        flpChanged = true;
                    }
                    if (flpChanged) {
                        tv.setLayoutParams(flp);
                        mutated = true;
                    }
                }
                if (tv.getGravity() != targetFlpGrav) {
                    tv.setGravity(targetFlpGrav);
                    mutated = true;
                }
                if (Build.VERSION.SDK_INT >= 17 && tv.getTextAlignment() != View.TEXT_ALIGNMENT_CENTER) {
                    tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    mutated = true;
                }
            }

            int rowW = bottomRow.getWidth();
            if (rowW <= 0) {
                rowW = computeUsableCanvasSpan(root, dm)[2];
            }
            int innerRowW = Math.max(1, rowW - bottomRow.getPaddingLeft() - bottomRow.getPaddingRight());
            List<TypographyNormalizer.GroupMember> members = new ArrayList<>(allButtonTvs.size());
            for (int i = 0; i < allButtonTvs.size(); i++) {
                TextView tv = allButtonTvs.get(i);
                ViewGroup slot = ownerSlots.get(i);
                CharSequence cs = tv.getText();
                if (cs == null || cs.length() == 0) continue;
                float w = 1f;
                if (slot.getLayoutParams() instanceof LinearLayout.LayoutParams) {
                    float lw = ((LinearLayout.LayoutParams) slot.getLayoutParams()).weight;
                    if (lw > 0f) w = lw;
                }
                int slotW = slot.getWidth() > 0
                        ? slot.getWidth()
                        : Math.max(1, Math.round(innerRowW * (w / totalWeight)));
                int safePad = TypographyNormalizer.clamp(
                        Math.round(slotW * 0.06f), Math.round(2f * density), Math.round(4f * density));
                int textAvail = Math.max(1, slotW - slot.getPaddingLeft() - slot.getPaddingRight() - 2 * safePad);
                members.add(new TypographyNormalizer.GroupMember(
                        tv, cs.toString(), textAvail, 0, safePad, safePad, true, tv));
            }
            if (members.size() >= 2) {
                mutated |= solveStripPeerGroup(
                        bottomRow,
                        members,
                        TypographyNormalizer.ARCHETYPE_VERTICAL_ELASTIC_STRIP,
                        Math.round(18f * density),
                        true,
                        false,
                        false,
                        0,
                        epoch,
                        "replayBottomBar_" + activeSlots.size(),
                        density);

                Locale activeLoc = TranslationEngine.getActiveLocale();
                Typeface activeTf = UIFontManager.getActiveTypeface(bottomRow.getContext());
                int maxSolvedSlotH = 0;
                for (int i = 0; i < members.size(); i++) {
                    TypographyNormalizer.GroupMember m = members.get(i);
                    if (m.plan == null) continue;
                    TextPaint tp = TypographyNormalizer.getScratchTextPaint(m.tv.getPaint());
                    TypographyNormalizer.preparePaintForScript(tp, m.text, activeLoc, activeTf);
                    tp.setTextSize(m.tv.getTextSize());
                    tp.setTextScaleX(m.tv.getTextScaleX());
                    StaticLayout sl = TypographyNormalizer.buildStaticLayout(
                            m.text,
                            tp,
                            m.safeAvailW,
                            Layout.Alignment.ALIGN_CENTER,
                            m.tv.getLineSpacingMultiplier(),
                            m.tv.getLineSpacingExtra(),
                            m.tv.getIncludeFontPadding(),
                            m.plan.targetLines,
                            m.plan.useEllipsis ? TextUtils.TruncateAt.END : null,
                            TypographyNormalizer.containsRtlChars(m.text));
                    int memberH = refPadTop + targetIconH + refDrawablePad + sl.getHeight() + refPadBot;
                    if (memberH > maxSolvedSlotH) {
                        maxSolvedSlotH = memberH;
                    }
                }
                if (maxSolvedSlotH > 0 && Math.abs(bottomRow.getMinimumHeight() - maxSolvedSlotH) > 1) {
                    bottomRow.setMinimumHeight(maxSolvedSlotH);
                    mutated = true;
                }
            }
            return mutated;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Phase 2 static horizontal button-strip solver for generic horizontal icon-above-text rows
     * inside popups and sub-panels (excluding RecyclerView/ListView/ScrollView lists),
     * plus clean full restoration when Text Auto-Fit is toggled OFF.
     */
    public static boolean solveAllCanvasStripPeerGroups(View root) {
        if (root == null || !TranslationEngine.isTextAutoFitActive()) return false;
        int epoch = ++sSolverPassEpoch;
        boolean mutated = optimizeReplayBottomBarInternal(root, epoch);
        mutated |= solveGenericIconRowsRecursive(root, epoch, 0);
        return mutated;
    }

    private static boolean solveGenericIconRowsRecursive(View v, int epoch, int depth) {
        if (v == null || depth > 14 || v.getVisibility() == View.GONE) return false;
        String cls = v.getClass().getName();
        if ("libre_settings_root".equals(v.getTag())
                || cls.endsWith("RecyclerView")
                || cls.endsWith("ListView")
                || cls.endsWith("GridView")
                || cls.endsWith("ShapeSettingView")
                || cls.endsWith("XuanQuSettingView")
                || cls.endsWith("LiquefySettingPhoneView")
                || matchesId(v, "replay_bottom_view", 0x7f090b0c)
                || isExportRowView(v)) {
            return false;
        }
        boolean mutated = false;
        if (v instanceof LinearLayout && ((LinearLayout) v).getOrientation() == LinearLayout.HORIZONTAL) {
            LinearLayout row = (LinearLayout) v;
            if (row.getWidth() > 0) {
                float density = row.getResources().getDisplayMetrics().density;
                List<TypographyNormalizer.GroupMember> members = new ArrayList<>();
                for (int i = 0; i < row.getChildCount(); i++) {
                    View ch = row.getChildAt(i);
                    if (ch.getVisibility() == View.GONE || !isIconAboveTextButton(ch, density)) continue;
                    TextView tv = findDirectSlotTextView(ch);
                    if (tv == null || tv.getText() == null || tv.getText().length() == 0) continue;
                    if (tv.getTag(TypographyNormalizer.TAG_PHASE2_MANAGED_EPOCH) != null
                            && ch.getWidth() <= 0) {
                        continue;
                    }
                    int slotW = ch.getWidth() > 0 ? ch.getWidth() : (row.getWidth() / Math.max(1, row.getChildCount()));
                    if (slotW <= 0) continue;
                    int safePad = TypographyNormalizer.clamp(
                            Math.round(slotW * 0.08f), Math.round(3f * density), Math.round(7f * density));
                    int textAvail = Math.max(1, slotW - ch.getPaddingLeft() - ch.getPaddingRight() - 2 * safePad);
                    members.add(new TypographyNormalizer.GroupMember(
                            tv, tv.getText().toString(), textAvail, 0, safePad, safePad, true, ch));
                }
                if (members.size() >= 2) {
                    mutated |= solveStripPeerGroup(
                            row,
                            members,
                            TypographyNormalizer.ARCHETYPE_VERTICAL_ELASTIC_STRIP,
                            Math.round(18f * density),
                            true,
                            false,
                            false,
                            0,
                            epoch,
                            "genericIconRow",
                            density);
                }
            }
        }
        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++) {
                mutated |= solveGenericIconRowsRecursive(vg.getChildAt(i), epoch, depth + 1);
            }
        }
        return mutated;
    }

    public static boolean isCanvasPolishToolGridLabel(TextView tv) {
        return tv != null && tv.getTag(TAG_TOOL_GRID_ORIG_PX) != null && isCanvasPopupPolishActive();
    }

    public static boolean isPhase2ManagedStripView(TextView tv) {
        if (tv == null) return false;
        if (tv.getTag(TypographyNormalizer.TAG_PHASE2_MANAGED_EPOCH) != null) {
            return true;
        }
        if (isCanvasPolishToolGridLabel(tv)) {
            return true;
        }
        if (tv.getTag(TypographyNormalizer.TAG_DIALOG_TITLE_SNAP) != null) {
            return true;
        }
        if (isInsideReplayBottomBar(tv)) {
            return true;
        }
        ViewParent p = tv.getParent();
        for (int depth = 0; p instanceof View && depth < 5; depth++) {
            View pv = (View) p;
            String cls = pv.getClass().getName();
            if (cls.endsWith("ShapeSettingView")
                    || cls.endsWith("XuanQuSettingView")
                    || cls.endsWith("LiquefySettingPhoneView")) {
                return true;
            }
            p = pv.getParent();
        }
        if (hasBoundPopupWindow(tv)) {
            View root = tv.getRootView();
            if (findView(root, "nsv_canvas", 0x7f090a13) == null
                    && findView(root, "nsv_setting", 0x7f090a17) == null
                    && findView(root, "nsv_import", 0x7f090a15) == null
                    && findView(root, "dialog_setting_iv", 0) == null
                    && findView(root, "toggleView", 0) == null
                    && findFastKeyPopupHeaderView(root, 0) == null) {
                ViewParent colP = tv.getParent();
                if (colP instanceof LinearLayout
                        && ((LinearLayout) colP).getOrientation() == LinearLayout.VERTICAL
                        && findVerticalMenuColumn((View) colP, 0) == colP) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean restoreAllSolverStates(View root) {
        if (root == null) return false;
        boolean mutated = false;
        mutated |= optimizeElasticPopupMenus(root);
        mutated |= restoreDialogTitlesRecursive(root);
        mutated |= restoreSolverNodeRecursive(root, 0);
        return mutated;
    }

    private static boolean restoreSolverNodeRecursive(View v, int depth) {
        if (v == null || depth > 22) return false;
        boolean mutated = false;
        if ("libre_settings_root".equals(v.getTag())) return false;
        mutated |= TypographyNormalizer.restoreContainerBaseline(v);
        if (v instanceof TextView) {
            TextView tv = (TextView) v;
            mutated |= TypographyNormalizer.restoreBaseline(tv);
            mutated |= restoreReplayTopDrawableBaseline(tv);
        }
        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++) {
                mutated |= restoreSolverNodeRecursive(vg.getChildAt(i), depth + 1);
            }
        }
        return mutated;
    }

    /**
     * Prevents the "Use current brush params" label from overlapping use_curbrush_switch
     * inside the Add Custom Brush dialog, and adds clean breathing space after Brush Info labels.
     */
    public static void optimizeAddBrushDialog(View root) {
        if (root == null) return;
        try {
            Context ctx = root.getContext();
            if (ctx == null) return;
            View switchView = findView(root, "use_curbrush_switch", 0);
            if (switchView == null || switchView.getVisibility() != View.VISIBLE) return;
            ViewParent vp = switchView.getParent();
            if (!(vp instanceof ViewGroup)) return;
            ViewGroup container = (ViewGroup) vp;
            View groupChoose = findView(root, "group_choose_layout", 0);
            int idTitle = id(ctx, "title_tv", 0);
            int idCancel = id(ctx, "tv_cancel", 0);
            int idSure = id(ctx, "tv_sure", 0);
            float density = ctx.getResources().getDisplayMetrics().density;
            int gapPx = Math.max(4, Math.round(10f * density));

            int sTop = switchView.getTop();
            int sBot = switchView.getBottom();
            for (int i = 0; i < container.getChildCount(); i++) {
                View child = container.getChildAt(i);
                if (child == switchView || child == groupChoose || !(child instanceof TextView)
                        || child instanceof android.widget.CompoundButton) {
                    continue;
                }
                int cid = child.getId();
                if ((idTitle != 0 && cid == idTitle)
                        || (idCancel != 0 && cid == idCancel)
                        || (idSure != 0 && cid == idSure)) {
                    continue;
                }
                TextView tv = (TextView) child;
                CharSequence txt = tv.getText();
                if (txt == null || txt.length() == 0) continue;
                if (sBot > sTop && tv.getHeight() > 0) {
                    int vOverlap = Math.min(sBot, tv.getBottom()) - Math.max(sTop, tv.getTop());
                    if (vOverlap <= 0 && Math.abs(tv.getTop() - sTop) > Math.round(28f * density)) {
                        continue;
                    }
                }
                ViewGroup.LayoutParams lp = tv.getLayoutParams();
                boolean changed = false;
                if (lp instanceof RelativeLayout.LayoutParams) {
                    RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) lp;
                    int[] rules = rlp.getRules();
                    int swId = switchView.getId();
                    if (swId != View.NO_ID) {
                        if (rules[RelativeLayout.ALIGN_RIGHT] != 0) {
                            rlp.removeRule(RelativeLayout.ALIGN_RIGHT);
                            changed = true;
                        }
                        if (rules[RelativeLayout.ALIGN_PARENT_RIGHT] != 0) {
                            rlp.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                            changed = true;
                        }
                        if (Build.VERSION.SDK_INT >= 17) {
                            if (rules[RelativeLayout.ALIGN_END] != 0) {
                                rlp.removeRule(RelativeLayout.ALIGN_END);
                                changed = true;
                            }
                            if (rules[RelativeLayout.ALIGN_PARENT_END] != 0) {
                                rlp.removeRule(RelativeLayout.ALIGN_PARENT_END);
                                changed = true;
                            }
                            if (rules[RelativeLayout.END_OF] != 0) {
                                rlp.removeRule(RelativeLayout.END_OF);
                                changed = true;
                            }
                        }
                        if (rules[RelativeLayout.LEFT_OF] != swId) {
                            rlp.addRule(RelativeLayout.LEFT_OF, swId);
                            changed = true;
                        }
                        if (groupChoose != null && groupChoose.getId() != View.NO_ID
                                && groupChoose.getParent() == container) {
                            if (rules[RelativeLayout.ALIGN_LEFT] != groupChoose.getId()) {
                                rlp.addRule(RelativeLayout.ALIGN_LEFT, groupChoose.getId());
                                changed = true;
                            }
                        } else if (rules[RelativeLayout.ALIGN_PARENT_LEFT] == 0) {
                            rlp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                            changed = true;
                        }
                    }
                    if (rlp.width != ViewGroup.LayoutParams.MATCH_PARENT) {
                        rlp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                        changed = true;
                    }
                    if (rlp.rightMargin != gapPx) {
                        rlp.rightMargin = gapPx;
                        changed = true;
                    }
                    if (Build.VERSION.SDK_INT >= 17 && rlp.getMarginEnd() != gapPx) {
                        rlp.setMarginEnd(gapPx);
                        changed = true;
                    }
                    if (changed) {
                        tv.setLayoutParams(rlp);
                    }
                } else if (lp instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
                    if (mlp.rightMargin != gapPx) {
                        mlp.rightMargin = gapPx;
                        tv.setLayoutParams(mlp);
                    }
                }
                setGravityIfChanged(tv, Gravity.START | Gravity.CENTER_VERTICAL);
                setSingleLineIfChanged(tv, false);
                setMaxLinesIfChanged(tv, 2);
                setEllipsizeIfChanged(tv, TextUtils.TruncateAt.END);
                TranslationEngine.ensureSafeVietnameseFontPadding(tv);
                break;
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Adds horizontal spacing after the Brush Name label in BrushSettingActivity Info tab.
     */
    public static void polishBrushInfoRow(View root) {
        if (root == null) return;
        try {
            Context ctx = root.getContext();
            if (ctx == null) return;
            View nameTitle = findView(root, "name_title", 0);
            if (!(nameTitle instanceof TextView) || nameTitle.getVisibility() != View.VISIBLE) return;
            float density = ctx.getResources().getDisplayMetrics().density;
            int minGap = Math.max(4, Math.round(5f * density));
            ViewGroup.LayoutParams lp = nameTitle.getLayoutParams();
            if (lp instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
                boolean changed = false;
                if (mlp.rightMargin < minGap) {
                    mlp.rightMargin = minGap;
                    changed = true;
                }
                if (Build.VERSION.SDK_INT >= 17 && mlp.getMarginEnd() < minGap) {
                    mlp.setMarginEnd(minGap);
                    changed = true;
                }
                if (changed) {
                    nameTitle.setLayoutParams(mlp);
                }
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Responsively lays out the Paint left tool grid popup for both portrait (2 columns)
     * and landscape/tablet (3 or 4 columns), capping height within visible safe bounds
     * and enabling smooth vertical scrolling when content exceeds safe viewport height.
     */
    public static void expandLeftToolGridIfApplicable(View root) {
        if (root == null || !isCanvasPopupPolishActive()) return;
        try {
            View nsvCanvas = findView(root, "nsv_canvas", 0x7f090a13);
            if (nsvCanvas != null) return;

            View rv = findView(root, "recyclerview", 0);
            View settingIv = findView(root, "dialog_setting_iv", 0);
            if (!(rv instanceof ViewGroup) || settingIv == null) return;

            ViewGroup rvGroup = (ViewGroup) rv;
            int childCount = rvGroup.getChildCount();
            if (childCount == 0) return;

            DisplayMetrics dm = root.getResources().getDisplayMetrics();
            float density = dm.density;
            int displayW = dm.widthPixels;
            int displayH = dm.heightPixels;

            Rect visibleFrame = new Rect();
            try {
                root.getWindowVisibleDisplayFrame(visibleFrame);
            } catch (Throwable ignored) {}
            if (visibleFrame.width() <= 0 || visibleFrame.height() <= 0) {
                visibleFrame.set(0, 0, displayW, displayH);
            }

            int insetTop = 0;
            int insetBottom = 0;
            int insetLeft = 0;
            int insetRight = 0;
            if (Build.VERSION.SDK_INT >= 23) {
                try {
                    WindowInsets wi = root.getRootWindowInsets();
                    if (wi != null) {
                        if (Build.VERSION.SDK_INT >= 30) {
                            android.graphics.Insets insets = wi.getInsets(
                                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                            insetTop = insets.top;
                            insetBottom = insets.bottom;
                            insetLeft = insets.left;
                            insetRight = insets.right;
                        } else {
                            insetTop = wi.getSystemWindowInsetTop();
                            insetBottom = wi.getSystemWindowInsetBottom();
                            insetLeft = wi.getSystemWindowInsetLeft();
                            insetRight = wi.getSystemWindowInsetRight();
                        }
                    }
                } catch (Throwable ignored) {}
            }

            int safeW = Math.min(visibleFrame.width(), Math.max(1, displayW - insetLeft - insetRight));
            int safeH = Math.min(visibleFrame.height(), Math.max(1, displayH - insetTop - insetBottom));
            boolean isLandscape = safeW > safeH;
            int targetSpan = isLandscape
                    ? (safeW >= Math.round(900 * density) ? 4 : 3)
                    : 2;

            PopupWindow pw = getBoundPopupWindow(root);
            int curW = (pw != null && pw.getWidth() > 0) ? pw.getWidth() : root.getWidth();
            Object origWObj = root.getTag(TAG_ORIG_TOOL_GRID_W);
            int origTwoColW;
            if (origWObj instanceof Integer && (Integer) origWObj > 0) {
                origTwoColW = (Integer) origWObj;
            } else {
                origTwoColW = curW > 0 ? curW : Math.round(150 * density);
                if (curW > 0) {
                    root.setTag(TAG_ORIG_TOOL_GRID_W, origTwoColW);
                }
            }

            View bubbleView = findView(root, "bubbleView", 0x7f0902eb);
            int curSpan = 2;
            Object layoutMgr = null;
            Method setSpanMethod = null;
            try {
                Method getLm = rv.getClass().getMethod("getLayoutManager");
                layoutMgr = getLm.invoke(rv);
                if (layoutMgr != null) {
                    Method getSpan = layoutMgr.getClass().getMethod("getSpanCount");
                    Object spanObj = getSpan.invoke(layoutMgr);
                    if (spanObj instanceof Integer && (Integer) spanObj > 0) {
                        curSpan = (Integer) spanObj;
                    }
                    setSpanMethod = layoutMgr.getClass().getMethod("setSpanCount", int.class);
                }
            } catch (Throwable ignored) {}

            Object origRvPadObj = rv.getTag(TAG_ORIG_TOOL_GRID_RV_PAD_R);
            int origRvPadR;
            if (origRvPadObj instanceof Integer) {
                origRvPadR = (Integer) origRvPadObj;
            } else {
                origRvPadR = rv.getPaddingRight();
                rv.setTag(TAG_ORIG_TOOL_GRID_RV_PAD_R, origRvPadR);
            }
            int extraRightPad = (targetSpan >= 3 && origRvPadR < Math.round(4 * density))
                    ? Math.round(6 * density)
                    : 0;
            int targetRvPadR = origRvPadR + extraRightPad;
            if (rv.getPaddingRight() != targetRvPadR) {
                rv.setPadding(rv.getPaddingLeft(), rv.getPaddingTop(), targetRvPadR, rv.getPaddingBottom());
            }

            int chromeW = Math.max(Math.round(28 * density),
                    origTwoColW - (curSpan == 2 && rv.getWidth() > 0 ? rv.getWidth() : Math.round(120 * density)));
            int colW = Math.max(Math.round(52 * density), (origTwoColW - chromeW) / 2);
            int desiredPopupW = (targetSpan == 2)
                    ? origTwoColW
                    : Math.min(Math.round(safeW * 0.78f), chromeW + colW * targetSpan + extraRightPad);

            if (setSpanMethod != null && layoutMgr != null && curSpan != targetSpan) {
                try {
                    setSpanMethod.invoke(layoutMgr, targetSpan);
                    rv.requestLayout();
                } catch (Throwable ignored) {}
            } else if (setSpanMethod == null) {
                targetSpan = curSpan;
                desiredPopupW = origTwoColW;
            }

            if (bubbleView != null && bubbleView.getLayoutParams() != null
                    && bubbleView.getLayoutParams().width > 0 && curW > 0 && desiredPopupW > 0) {
                ViewGroup.LayoutParams bLpW = bubbleView.getLayoutParams();
                int rootHPad = root.getPaddingLeft() + root.getPaddingRight();
                if (bLpW instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams bMlp = (ViewGroup.MarginLayoutParams) bLpW;
                    rootHPad += bMlp.leftMargin + bMlp.rightMargin;
                }
                int targetBubbleW = Math.max(colW, desiredPopupW - rootHPad);
                if (bLpW.width != targetBubbleW) {
                    bLpW.width = targetBubbleW;
                    bubbleView.setLayoutParams(bLpW);
                }
            }
            if (rv.getLayoutParams() != null && rv.getLayoutParams().width > 0 && desiredPopupW > 0) {
                ViewGroup.LayoutParams rvLpW = rv.getLayoutParams();
                int targetRvW = Math.max(colW, desiredPopupW - chromeW);
                if (rvLpW.width != targetRvW) {
                    rvLpW.width = targetRvW;
                    rv.setLayoutParams(rvLpW);
                }
            }

            int rowH = 0;
            int sampleMax = Math.min(childCount, 4);
            for (int i = 0; i < sampleMax; i++) {
                View ch = rvGroup.getChildAt(i);
                if (ch != null && ch.getHeight() > rowH) {
                    rowH = ch.getHeight();
                }
            }
            if (rowH <= 0) {
                rowH = Math.round(58.5f * density);
            }

            int itemCount = childCount;
            try {
                Method getAdapter = rv.getClass().getMethod("getAdapter");
                Object adapter = getAdapter.invoke(rv);
                if (adapter != null) {
                    Method getItemCount = adapter.getClass().getMethod("getItemCount");
                    Object cntObj = getItemCount.invoke(adapter);
                    if (cntObj instanceof Integer && (Integer) cntObj > 0) {
                        itemCount = (Integer) cntObj;
                    }
                }
            } catch (Throwable ignored) {}
            if (itemCount < childCount) {
                itemCount = childCount;
            }

            int rows = (itemCount + targetSpan - 1) / targetSpan;
            int desiredRvH = rows * rowH + rv.getPaddingTop() + rv.getPaddingBottom() + Math.round(2 * density);

            int footerH = settingIv.getHeight() > 0 ? settingIv.getHeight() : Math.round(37 * density);
            View lineView = findView(root, "line", 0);
            if (lineView != null) {
                footerH += Math.max(1, lineView.getHeight());
            }

            int chromeH = root.getPaddingTop() + root.getPaddingBottom();
            if (bubbleView != null) {
                chromeH += bubbleView.getPaddingTop() + bubbleView.getPaddingBottom();
                if (bubbleView.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams bMlp = (ViewGroup.MarginLayoutParams) bubbleView.getLayoutParams();
                    chromeH += bMlp.topMargin + bMlp.bottomMargin;
                }
            }
            if (rv.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams rMlp = (ViewGroup.MarginLayoutParams) rv.getLayoutParams();
                chromeH += rMlp.topMargin + rMlp.bottomMargin;
            }

            int maxPopupH = Math.max(footerH + chromeH + rowH, safeH - Math.round(28 * density));
            int desiredPopupH = Math.min(maxPopupH, desiredRvH + footerH + chromeH);
            int cappedRvH = Math.max(rowH, desiredPopupH - footerH - chromeH);
            boolean needsScroll = desiredRvH > cappedRvH;

            rv.setVerticalScrollBarEnabled(needsScroll);
            rv.setOverScrollMode(needsScroll ? View.OVER_SCROLL_ALWAYS : View.OVER_SCROLL_NEVER);

            String sig = targetSpan + "|" + itemCount + "|" + cappedRvH + "|" + desiredPopupH + "|" + desiredPopupW;
            if (sig.equals(root.getTag(TAG_LAST_TOOL_GRID_SIG))
                    && Math.abs(rv.getHeight() - cappedRvH) <= 2
                    && (pw == null || Math.abs(pw.getWidth() - desiredPopupW) <= 2)) {
                return;
            }
            root.setTag(TAG_LAST_TOOL_GRID_SIG, sig);

            ViewGroup.LayoutParams rvLp = rv.getLayoutParams();
            if (rvLp != null && rvLp.height != cappedRvH) {
                rvLp.height = cappedRvH;
                rv.setLayoutParams(rvLp);
            }
            if (bubbleView != null && bubbleView.getLayoutParams() != null) {
                ViewGroup.LayoutParams bLp = bubbleView.getLayoutParams();
                int targetBubbleH = desiredPopupH - root.getPaddingTop() - root.getPaddingBottom();
                if (bLp instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams bMlp = (ViewGroup.MarginLayoutParams) bLp;
                    targetBubbleH -= bMlp.topMargin + bMlp.bottomMargin;
                }
                if (targetBubbleH > 0 && bLp.height != targetBubbleH) {
                    bLp.height = targetBubbleH;
                    bubbleView.setLayoutParams(bLp);
                }
            }
            if (root instanceof ViewGroup) {
                unclipContainerSafely((ViewGroup) root);
            }
            if (pw != null) {
                pw.setWidth(desiredPopupW);
                pw.setHeight(desiredPopupH);
                if (pw.isShowing()) {
                    pw.update(desiredPopupW, desiredPopupH);
                }
            }
        } catch (Throwable ignored) {}
    }

    private static int findVisibleFastKeyCardWidth() {
        try {
            List<View> windows = TranslationEngine.getWindowManagerViews();
            if (windows == null) return 0;
            for (int i = windows.size() - 1; i >= 0; i--) {
                View wRoot = windows.get(i);
                int found = scanFastKeyGridCardWidth(wRoot, 0);
                if (found > 0) return found;
            }
        } catch (Throwable ignored) {}
        return 0;
    }

    private static int scanFastKeyGridCardWidth(View v, int depth) {
        if (v == null || depth > 14 || v.getVisibility() != View.VISIBLE) return 0;
        if (v instanceof android.widget.GridLayout) {
            android.widget.GridLayout gl = (android.widget.GridLayout) v;
            if (gl.getChildCount() == 8 && gl.getWidth() > 0) {
                ViewParent p = gl.getParent();
                if (p instanceof View) {
                    int pw = ((View) p).getWidth();
                    if (pw >= gl.getWidth()) return pw;
                }
                return gl.getWidth();
            }
        }
        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++) {
                int w = scanFastKeyGridCardWidth(vg.getChildAt(i), depth + 1);
                if (w > 0) return w;
            }
        }
        return 0;
    }

    private static boolean isFastKeyPopupHeaderLabel(String t) {
        return "\u64cd\u4f5c\u83dc\u5355".equals(t)
                || "\u8bbe\u7f6e\u64cd\u4f5c".equals(t)
                || "\u9009\u62e9\u64cd\u4f5c".equals(t)
                || "Menu Thao t\u00e1c".equals(t)
                || "Action Menu".equals(t)
                || "Ch\u1ecdn thao t\u00e1c".equals(t)
                || "Set Action".equals(t)
                || "Select Action".equals(t);
    }

    private static View findFastKeyPopupHeaderView(View v, int depth) {
        if (v == null || depth > 14 || v.getVisibility() != View.VISIBLE) return null;
        if (v instanceof TextView) {
            CharSequence cs = ((TextView) v).getText();
            if (cs != null && isFastKeyPopupHeaderLabel(cs.toString().trim())) {
                return v;
            }
        }
        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View found = findFastKeyPopupHeaderView(vg.getChildAt(i), depth + 1);
                if (found != null) return found;
            }
        }
        return null;
    }

    public static void polishFastKeyHostCardHeader(View root) {
        if (root == null || !isCanvasPopupPolishActive()) return;
        try {
            android.widget.GridLayout grid = findEightSlotFastKeyGrid(root, 0);
            if (grid == null || !(grid.getParent() instanceof ViewGroup)) return;
            ViewGroup card = (ViewGroup) grid.getParent();
            float density = root.getResources().getDisplayMetrics().density;
            int gapPx = Math.max(4, Math.round(8f * density));
            for (int i = 0; i < card.getChildCount(); i++) {
                View ch = card.getChildAt(i);
                if (ch == grid || !(ch instanceof LinearLayout)) continue;
                LinearLayout headerRow = (LinearLayout) ch;
                if (headerRow.getOrientation() != LinearLayout.HORIZONTAL
                        || headerRow.getChildCount() < 2) {
                    continue;
                }
                View first = headerRow.getChildAt(0);
                View second = headerRow.getChildAt(1);
                if (!(first instanceof TextView) || !(second instanceof TextView)) continue;
                TextView tvLeft = (TextView) first;
                TextView tvRight = (TextView) second;
                if (tvLeft.getLayoutParams() instanceof LinearLayout.LayoutParams) {
                    LinearLayout.LayoutParams lLp = (LinearLayout.LayoutParams) tvLeft.getLayoutParams();
                    if (lLp.width != 0 || lLp.weight != 1f || lLp.rightMargin < gapPx) {
                        lLp.width = 0;
                        lLp.weight = 1f;
                        lLp.rightMargin = Math.max(lLp.rightMargin, gapPx);
                        tvLeft.setLayoutParams(lLp);
                    }
                }
                if (tvRight.getMaxWidth() < Integer.MAX_VALUE) {
                    tvRight.setMaxWidth(Integer.MAX_VALUE);
                }
                if (tvRight.getMinWidth() != 0) {
                    tvRight.setMinWidth(0);
                }
                if (tvRight.getLayoutParams() instanceof LinearLayout.LayoutParams) {
                    LinearLayout.LayoutParams rLp = (LinearLayout.LayoutParams) tvRight.getLayoutParams();
                    if (rLp.width != ViewGroup.LayoutParams.WRAP_CONTENT || rLp.weight != 0f) {
                        rLp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                        rLp.weight = 0f;
                        tvRight.setLayoutParams(rLp);
                    }
                }
                int minPadH = Math.max(4, Math.round(6f * density));
                if (tvRight.getPaddingLeft() < minPadH) {
                    tvRight.setPadding(minPadH, tvRight.getPaddingTop(),
                            tvRight.getPaddingRight(), tvRight.getPaddingBottom());
                }
                setSingleLineIfChanged(tvRight, true);
                setMaxLinesIfChanged(tvRight, 1);
                TranslationEngine.ensureSafeVietnameseFontPadding(tvRight);
                return;
            }
        } catch (Throwable ignored) {}
    }

    private static android.widget.GridLayout findEightSlotFastKeyGrid(View v, int depth) {
        if (v == null || depth > 12 || v.getVisibility() != View.VISIBLE) return null;
        if (v instanceof android.widget.GridLayout) {
            android.widget.GridLayout gl = (android.widget.GridLayout) v;
            if (gl.getChildCount() == 8) return gl;
        }
        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++) {
                android.widget.GridLayout found = findEightSlotFastKeyGrid(vg.getChildAt(i), depth + 1);
                if (found != null) return found;
            }
        }
        return null;
    }

    /**
     * Matches FastKey preset dropdown and command picker widths to the host Quick Actions card
     * and enforces an opaque rounded surface so underlying right column buttons never show through.
     */
    public static void optimizeFastKeyPopups(View root) {
        if (root == null || !isCanvasPopupPolishActive()) return;
        try {
            polishFastKeyHostCardHeader(root);
            View headerTv = findFastKeyPopupHeaderView(root, 0);
            if (headerTv == null || !headerTv.isShown()) return;

            ViewParent p1 = headerTv.getParent();
            if (!(p1 instanceof ViewGroup)) return;
            ViewGroup headerRow = (ViewGroup) p1;
            ViewParent p2 = headerRow.getParent();
            ViewGroup card = (p2 instanceof ViewGroup) ? (ViewGroup) p2 : headerRow;

            int screenW = root.getResources().getDisplayMetrics().widthPixels;
            if (card.getWidth() >= Math.round(screenW * 0.90f)) {
                card = headerRow;
            }

            float density = root.getResources().getDisplayMetrics().density;
            int cardW = findVisibleFastKeyCardWidth();
            if (cardW <= 0) {
                cardW = Math.round(260f * density);
            }

            int curW = card.getWidth();
            if (cardW > 0 && curW > 0 && curW < cardW - 2) {
                ViewGroup.LayoutParams cLp = card.getLayoutParams();
                if (cLp != null && cLp.width != cardW) {
                    cLp.width = cardW;
                    card.setLayoutParams(cLp);
                }
                for (int i = 0; i < card.getChildCount(); i++) {
                    View ch = card.getChildAt(i);
                    ViewGroup.LayoutParams chLp = ch.getLayoutParams();
                    if (chLp != null && chLp.width != ViewGroup.LayoutParams.MATCH_PARENT && ch.getWidth() < cardW - 2) {
                        chLp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                        ch.setLayoutParams(chLp);
                    }
                }
                if (headerRow instanceof LinearLayout && headerRow.getChildCount() >= 2
                        && headerTv.getLayoutParams() instanceof LinearLayout.LayoutParams) {
                    LinearLayout.LayoutParams hLp = (LinearLayout.LayoutParams) headerTv.getLayoutParams();
                    if (hLp.weight <= 0f || hLp.width != 0) {
                        hLp.width = 0;
                        hLp.weight = 1f;
                        headerTv.setLayoutParams(hLp);
                    }
                }
            }

            PopupWindow pw = getBoundPopupWindow(root);
            if (pw != null) {
                int pwCurW = pw.getWidth();
                int pwCurH = pw.getHeight();
                if (cardW > 0 && (pwCurW <= 0 || pwCurW < cardW - 2)) {
                    pw.setWidth(cardW);
                    if (pw.isShowing()) {
                        pw.update(cardW, pwCurH > 0 ? pwCurH : -1);
                    }
                }
            }

            if (card.getTag(TAG_FASTKEY_STYLED) == null) {
                card.setTag(TAG_FASTKEY_STYLED, Boolean.TRUE);
                GradientDrawable bg = new GradientDrawable();
                bg.setColor(0xFF28292D);
                bg.setCornerRadius(10f * density);
                bg.setStroke(Math.max(1, Math.round(1f * density)), 0xFF3E3F45);
                card.setBackground(bg);
                card.setClipChildren(true);
            }

            // Keep the active dropdown or picker cleanly above the Quick Actions host card.
            View overlayNode = card;
            ViewParent hostP = overlayNode.getParent();
            while (hostP instanceof ViewGroup && ((ViewGroup) hostP).getChildCount() == 1
                    && ((ViewGroup) hostP).getWidth() < Math.round(screenW * 0.90f)) {
                overlayNode = (View) hostP;
                hostP = overlayNode.getParent();
            }
            if (Build.VERSION.SDK_INT >= 21) {
                float highZ = 48f * density;
                if (card.getElevation() < highZ - 1f) card.setElevation(highZ);
                if (card.getTranslationZ() < highZ - 1f) card.setTranslationZ(highZ);
                if (overlayNode != card) {
                    if (overlayNode.getElevation() < highZ - 1f) overlayNode.setElevation(highZ);
                    if (overlayNode.getTranslationZ() < highZ - 1f) overlayNode.setTranslationZ(highZ);
                }
            }
            if (hostP instanceof ViewGroup) {
                ViewGroup host = (ViewGroup) hostP;
                if (Build.VERSION.SDK_INT >= 21) {
                    float maxSibZ = 4f * density;
                    for (int i = 0; i < host.getChildCount(); i++) {
                        View sib = host.getChildAt(i);
                        if (sib != overlayNode && sib.getWidth() < Math.round(screenW * 0.90f)) {
                            if (sib.getElevation() > maxSibZ) {
                                sib.setElevation(maxSibZ);
                            }
                            if (sib.getTranslationZ() != 0f) {
                                sib.setTranslationZ(0f);
                            }
                        }
                    }
                }
                if (host.indexOfChild(overlayNode) != host.getChildCount() - 1) {
                    overlayNode.bringToFront();
                }
            }
        } catch (Throwable ignored) {}
    }

    // ---- Dynamic ToggleSwitchView, LiuyebiSettingView & HSVSeekbarView Normalization ----

    private static volatile boolean sToggleFieldsResolved = false;
    private static Field fToggleNames;
    private static Field fToggleBgRect;
    private static Field fToggleSelectIndex;
    private static Field fToggleTextPaint;
    private static Field fToggleSelectTextPaint;
    private static Field fToggleFontSize;

    private static volatile boolean sHsvFieldsResolved = false;
    private static Field fHsvTitle;
    private static Field fHsvNameRect;
    private static Field fHsvValueBgRect;
    private static Field fHsvTextPaint;
    private static Field fHsvTextColor;

    private static final class ToggleRenderPlan {
        final String key;
        final float availW;
        final float targetPx;
        final StaticLayout[] normalLayouts;
        final StaticLayout[] selectedLayouts;

        ToggleRenderPlan(
                String key,
                float availW,
                float targetPx,
                StaticLayout[] normalLayouts,
                StaticLayout[] selectedLayouts) {
            this.key = key;
            this.availW = availW;
            this.targetPx = targetPx;
            this.normalLayouts = normalLayouts;
            this.selectedLayouts = selectedLayouts;
        }
    }

    private static final class HsvNameRenderPlan {
        final String key;
        final StaticLayout layout;
        final float drawX;
        final float drawY;

        HsvNameRenderPlan(String key, StaticLayout layout, float drawX, float drawY) {
            this.key = key;
            this.layout = layout;
            this.drawX = drawX;
            this.drawY = drawY;
        }
    }

    private static Field resolveTypedToggleField(Class<?> cls, String exactName, Class<?> expectedType) {
        Field field = LibreResourceResolver.findField(cls, exactName, expectedType, null);
        if (field == null) return null;
        Class<?> actualType = field.getType();
        if (expectedType.isPrimitive()) {
            return actualType == expectedType ? field : null;
        }
        return expectedType.isAssignableFrom(actualType) ? field : null;
    }

    private static void ensureToggleFields(Class<?> cls) {
        if (sToggleFieldsResolved || cls == null) return;
        synchronized (WorkspaceUiController.class) {
            if (sToggleFieldsResolved) return;
            fToggleNames = resolveTypedToggleField(cls, "names", ArrayList.class);
            fToggleBgRect = resolveTypedToggleField(cls, "bgRect", RectF.class);
            fToggleSelectIndex = resolveTypedToggleField(cls, "selectIndex", int.class);
            fToggleTextPaint = resolveTypedToggleField(cls, "textPaint", Paint.class);
            fToggleSelectTextPaint = resolveTypedToggleField(cls, "selectTextPaint", Paint.class);
            fToggleFontSize = resolveTypedToggleField(cls, "fontSize", int.class);
            sToggleFieldsResolved = true;
        }
    }

    private static void ensureHsvFields(Class<?> cls) {
        if (sHsvFieldsResolved || cls == null) return;
        synchronized (WorkspaceUiController.class) {
            if (sHsvFieldsResolved) return;
            try {
                fHsvTitle = cls.getDeclaredField("title");
                fHsvTitle.setAccessible(true);
                fHsvNameRect = cls.getDeclaredField("nameRect");
                fHsvNameRect.setAccessible(true);
                fHsvValueBgRect = cls.getDeclaredField("valueTextBgRect");
                fHsvValueBgRect.setAccessible(true);
                fHsvTextPaint = cls.getDeclaredField("textPaint");
                fHsvTextPaint.setAccessible(true);
                fHsvTextColor = cls.getDeclaredField("textColor");
                fHsvTextColor.setAccessible(true);
            } catch (Throwable ignored) {}
            sHsvFieldsResolved = true;
        }
    }

    private static String resolveToggleSegmentLabel(Object raw) {
        String s = raw != null ? raw.toString() : "";
        if (s.isEmpty() || !TranslationEngine.isDynamicI18nEnabled()) return s;
        Object mapped = TranslationEngine.dispatch(TranslationEngine.OP_TRANSLATE_STRING, s);
        return mapped instanceof String ? (String) mapped : s;
    }

    private static int computeDesiredToggleSwitchBarWidth(View switchView, int usableW, float density) {
        if (switchView == null) return 0;
        try {
            ensureToggleFields(switchView.getClass());
            if (fToggleNames == null || fToggleFontSize == null) return 0;
            Object namesObj = fToggleNames.get(switchView);
            if (!(namesObj instanceof List)) return 0;
            List<?> names = (List<?>) namesObj;
            int n = names.size();
            if (n <= 0) return 0;

            Context ctx = switchView.getContext();
            DisplayMetrics dm = switchView.getResources().getDisplayMetrics();
            Locale activeLoc = TranslationEngine.getActiveLocale();
            Typeface activeTf = ctx != null ? UIFontManager.getActiveTypeface(ctx) : null;

            float origFontPx;
            Object origTag = switchView.getTag(TAG_TOGGLE_ORIG_FONT_PX);
            if (origTag instanceof Float && (Float) origTag > 0f) {
                origFontPx = (Float) origTag;
            } else {
                origFontPx = fToggleFontSize.getInt(switchView);
                if (origFontPx <= 0f) {
                    origFontPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12f, dm);
                }
            }

            Paint basePaint = (fToggleTextPaint != null && fToggleTextPaint.get(switchView) instanceof Paint)
                    ? (Paint) fToggleTextPaint.get(switchView)
                    : null;
            TextPaint tp = TypographyNormalizer.getScratchTextPaint(basePaint);
            tp.setTextAlign(Paint.Align.LEFT);
            tp.setTextSize(origFontPx);

            float maxLabelW = 0f;
            for (int i = 0; i < n; i++) {
                String label = resolveToggleSegmentLabel(names.get(i));
                if (label.isEmpty()) continue;
                String wrapped = TypographyNormalizer.wrapForCanvas(label, activeLoc);
                TypographyNormalizer.preparePaintForScript(tp, wrapped, activeLoc, activeTf);
                tp.setTextScaleX(1.0f);
                tp.setTextSize(origFontPx);
                float w = TypographyNormalizer.measureHarfBuzzWidth(wrapped, tp);
                if (w > maxLabelW) maxLabelW = w;
            }

            int segNeed = Math.round(maxLabelW + 18f * density);
            int swMargins = 0;
            if (switchView.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) switchView.getLayoutParams();
                swMargins = mlp.leftMargin + mlp.rightMargin;
            }
            if (swMargins <= 0) {
                swMargins = Math.round(24f * density);
            }
            int minBarW = Math.round(210f * density);
            int maxBarW = Math.max(minBarW, usableW - Math.round(16f * density));
            return TypographyNormalizer.clamp(n * segNeed + swMargins, minBarW, maxBarW);
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static boolean normalizeLiuyebiBar(
            View liuyebiBar,
            View root,
            int usableW,
            float freeCenterShiftX,
            float density) {
        if (liuyebiBar == null) return false;
        boolean mutated = false;
        View switchView = findView(liuyebiBar, "switchView", 0);
        View contentView = findView(liuyebiBar, "contentView", 0);
        if (contentView != null && switchView != null && switchView.getVisibility() == View.VISIBLE) {
            ViewGroup.LayoutParams cLp = contentView.getLayoutParams();
            if (cLp != null && cLp.width > 0) {
                if (liuyebiBar.getTag(TAG_ORIG_LIUYEBI_W) == null) {
                    liuyebiBar.setTag(TAG_ORIG_LIUYEBI_W, Integer.valueOf(cLp.width));
                }
                int dynamicTargetW = computeDesiredToggleSwitchBarWidth(switchView, usableW, density);
                if (dynamicTargetW <= 0) {
                    dynamicTargetW = Math.min(Math.round(284f * density), Math.max(Math.round(210f * density), usableW - Math.round(16f * density)));
                }
                if (cLp.width != dynamicTargetW) {
                    cLp.width = dynamicTargetW;
                    contentView.setLayoutParams(cLp);
                    mutated = true;
                }
            }
            if (Math.abs(liuyebiBar.getTranslationX() - freeCenterShiftX) > 0.5f) {
                liuyebiBar.setTranslationX(freeCenterShiftX);
                mutated = true;
            }
        }
        for (String lblId : new String[]{"fill_tv", "eraser_tv", "pot_setting_tv"}) {
            View v = findView(liuyebiBar, lblId, 0);
            if (v instanceof TextView) {
                applySingleLineToolbarLabel((TextView) v);
            }
        }
        return mutated;
    }

    public static void onLiuyebiInitData(Object viewObj) {
        if (!(viewObj instanceof View) || !isCanvasPopupPolishActive()) return;
        View liuyebiBar = (View) viewObj;
        Context ctx = liuyebiBar.getContext();
        if (ctx == null) return;
        try {
            DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
            if (dm.widthPixels <= 0 || dm.density <= 0f) return;
            View root = liuyebiBar.getRootView();
            int[] span = computeUsableCanvasSpan(root, dm);
            normalizeLiuyebiBar(liuyebiBar, root, span[2], span[3], dm.density);
            View switchView = findView(liuyebiBar, "switchView", 0);
            if (switchView != null) {
                switchView.setTag(TAG_TOGGLE_PLAN_KEY, null);
                switchView.setTag(TAG_TOGGLE_PLAN, null);
                switchView.invalidate();
            }
        } catch (Throwable ignored) {}
    }

    public static void onToggleSwitchUpdate(Object viewObj) {
        if (!(viewObj instanceof View)) return;
        View switchView = (View) viewObj;
        try {
            ensureToggleFields(switchView.getClass());
            if (fToggleNames != null && fToggleNames.get(switchView) instanceof ArrayList && TranslationEngine.isDynamicI18nEnabled()) {
                @SuppressWarnings("unchecked")
                ArrayList<Object> list = (ArrayList<Object>) fToggleNames.get(switchView);
                if (list != null) {
                    for (int i = 0; i < list.size(); i++) {
                        Object raw = list.get(i);
                        if (raw instanceof String && TranslationEngine.containsChinese((String) raw)) {
                            Object tr = TranslationEngine.dispatch(TranslationEngine.OP_TRANSLATE_STRING, raw);
                            if (tr instanceof String) {
                                list.set(i, tr);
                            }
                        }
                    }
                }
            }
            if (switchView.getTag(TAG_TOGGLE_ORIG_FONT_PX) == null && fToggleFontSize != null) {
                int fs = fToggleFontSize.getInt(switchView);
                if (fs > 0) {
                    switchView.setTag(TAG_TOGGLE_ORIG_FONT_PX, Float.valueOf(fs));
                }
            }
            switchView.setTag(TAG_TOGGLE_PLAN_KEY, null);
            switchView.setTag(TAG_TOGGLE_PLAN, null);
            switchView.invalidate();
            ViewParent p = switchView.getParent();
            for (int depth = 0; depth < 3 && p instanceof View; depth++) {
                View pv = (View) p;
                if (pv.getClass().getName().endsWith("LiuyebiSettingView")) {
                    onLiuyebiInitData(pv);
                    break;
                }
                p = pv.getParent();
            }
        } catch (Throwable ignored) {}
    }

    public static Object onDrawToggleSwitchLabels(Object arg) {
        if (!(arg instanceof Object[])) return null;
        Object[] args = (Object[]) arg;
        if (args.length < 2 || !(args[0] instanceof View) || !(args[1] instanceof Canvas)) return null;
        View switchView = (View) args[0];
        Canvas canvas = (Canvas) args[1];
        try {
            ensureToggleFields(switchView.getClass());
            if (fToggleNames == null || fToggleBgRect == null || fToggleSelectIndex == null
                    || fToggleTextPaint == null || fToggleSelectTextPaint == null || fToggleFontSize == null) {
                return null;
            }
            Object origFontObj = switchView.getTag(TAG_TOGGLE_ORIG_FONT_PX);
            if (!(origFontObj instanceof Float)) {
                int rawFs = fToggleFontSize.getInt(switchView);
                origFontObj = Float.valueOf(rawFs > 0 ? rawFs
                        : TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12f,
                                switchView.getResources().getDisplayMetrics()));
                switchView.setTag(TAG_TOGGLE_ORIG_FONT_PX, origFontObj);
            }
            float origPx = (Float) origFontObj;
            if (!TranslationEngine.isTextAutoFitActive()) {
                int origInt = Math.round(origPx);
                if (fToggleFontSize.getInt(switchView) != origInt) {
                    fToggleFontSize.setInt(switchView, origInt);
                }
                Paint tp0 = (Paint) fToggleTextPaint.get(switchView);
                Paint sp0 = (Paint) fToggleSelectTextPaint.get(switchView);
                if (tp0 != null) tp0.setTextSize(origPx);
                if (sp0 != null) sp0.setTextSize(origPx);
                return null;
            }

            Object namesObj = fToggleNames.get(switchView);
            Object rectObj = fToggleBgRect.get(switchView);
            if (!(namesObj instanceof List) || !(rectObj instanceof RectF)) return null;
            List<?> names = (List<?>) namesObj;
            RectF bgRect = (RectF) rectObj;
            int n = names.size();
            if (n <= 0 || bgRect.width() <= 4f || bgRect.height() <= 4f) return null;

            Paint textPaint = (Paint) fToggleTextPaint.get(switchView);
            Paint selectPaint = (Paint) fToggleSelectTextPaint.get(switchView);
            if (textPaint == null || selectPaint == null) return null;

            Context ctx = switchView.getContext();
            DisplayMetrics dm = switchView.getResources().getDisplayMetrics();
            float density = dm.density > 0f ? dm.density : 2f;
            Locale activeLoc = TranslationEngine.getActiveLocale();
            String locTag = TranslationEngine.getActiveLocaleTag();
            Typeface activeTf = ctx != null ? UIFontManager.getActiveTypeface(ctx) : null;
            int tfId = ctx != null ? UIFontManager.getActiveTypefaceIdentity(ctx) : 0;

            StringBuilder keySb = new StringBuilder(96);
            keySb.append(locTag).append('|').append(tfId).append('|')
                    .append(Math.round(bgRect.width())).append('x').append(Math.round(bgRect.height()))
                    .append('|').append(Float.floatToRawIntBits(origPx));
            String[] labels = new String[n];
            for (int i = 0; i < n; i++) {
                String lbl = resolveToggleSegmentLabel(names.get(i));
                labels[i] = TypographyNormalizer.wrapForCanvas(lbl, activeLoc);
                keySb.append('|').append(labels[i]);
            }
            String key = keySb.toString();

            ToggleRenderPlan plan = null;
            Object cachedPlanObj = switchView.getTag(TAG_TOGGLE_PLAN);
            if (key.equals(switchView.getTag(TAG_TOGGLE_PLAN_KEY)) && cachedPlanObj instanceof ToggleRenderPlan) {
                plan = (ToggleRenderPlan) cachedPlanObj;
            } else {
                float segW = bgRect.width() / n;
                float segH = Math.max(1f, bgRect.height() - 2f * density);
                int safePad = TypographyNormalizer.clamp(
                        Math.round(segW * 0.08f), Math.round(4f * density), Math.round(8f * density));
                int availSegW = Math.max(1, Math.round(segW) - 2 * safePad);

                boolean allowScaleX = true;
                for (int i = 0; i < n; i++) {
                    if (TypographyNormalizer.classifyScript(labels[i]) != TypographyNormalizer.SCRIPT_LATIN_CYRILLIC_GREEK) {
                        allowScaleX = false;
                        break;
                    }
                }

                float floorPx = TypographyNormalizer.computeProtectedFloorPx(origPx, dm);

                TextPaint measureTp = TypographyNormalizer.getScratchTextPaint(textPaint);
                measureTp.setTextAlign(Paint.Align.LEFT);

                // Phase 1 / Archetype IV ToggleSwitchView 5-State Evaluation:
                // Try 1 line @ 100%, then 2 lines @ 100% with FontMetrics guard before any downscale.
                float targetPx = origPx;
                float targetScaleX = 1.0f;
                boolean allFitSingle = true;
                for (int i = 0; i < n; i++) {
                    TypographyNormalizer.preparePaintForScript(measureTp, labels[i], activeLoc, activeTf);
                    measureTp.setTextSize(origPx);
                    measureTp.setTextScaleX(1.0f);
                    float mw = TypographyNormalizer.measureHarfBuzzWidth(labels[i], measureTp);
                    float mh = TypographyNormalizer.measureFontMetricsLineHeight(measureTp);
                    if (mw > availSegW + 0.5f || mh > segH + 1.5f) {
                        allFitSingle = false;
                        break;
                    }
                }

                boolean useTwoLines = false;
                if (!allFitSingle) {
                    boolean canAllTwo = true;
                    for (int i = 0; i < n; i++) {
                        int sg = TypographyNormalizer.classifyScript(labels[i]);
                        boolean canBreak = TypographyNormalizer.hasNaturalBreakToken(labels[i])
                                || TypographyNormalizer.allowsUnspacedLineBreak(labels[i]);
                        TypographyNormalizer.preparePaintForScript(measureTp, labels[i], activeLoc, activeTf);
                        measureTp.setTextSize(origPx);
                        measureTp.setTextScaleX(1.0f);
                        float mw1 = TypographyNormalizer.measureHarfBuzzWidth(labels[i], measureTp);
                        if (mw1 <= availSegW + 0.5f && TypographyNormalizer.measureFontMetricsLineHeight(measureTp) <= segH + 1.5f) {
                            continue;
                        }
                        if (!canBreak) {
                            canAllTwo = false;
                            break;
                        }
                        float fm2H = 2f * TypographyNormalizer.measureFontMetricsLineHeight(measureTp);
                        StaticLayout slTest = TypographyNormalizer.buildStaticLayout(
                                labels[i], measureTp, availSegW, Layout.Alignment.ALIGN_CENTER,
                                0.96f, 0f, true, 0, null, TypographyNormalizer.containsRtlChars(labels[i]));
                        if (slTest.getLineCount() > 2
                                || TypographyNormalizer.getMaxLineWidth(slTest) > availSegW + 0.5f
                                || TypographyNormalizer.hasDisallowedWordSplit(slTest, labels[i], sg)
                                || slTest.getHeight() > segH + 1.5f
                                || fm2H > segH + 2.5f) {
                            canAllTwo = false;
                            break;
                        }
                    }
                    if (canAllTwo) {
                        useTwoLines = true;
                    }
                }

                if (!allFitSingle && !useTwoLines) {
                    for (int step = 0; step < 8; step++) {
                        targetPx = (step == 7) ? floorPx : Math.max(floorPx, targetPx * 0.94f);
                        allFitSingle = true;
                        for (int i = 0; i < n; i++) {
                            TypographyNormalizer.preparePaintForScript(measureTp, labels[i], activeLoc, activeTf);
                            measureTp.setTextSize(targetPx);
                            measureTp.setTextScaleX(1.0f);
                            float mw = TypographyNormalizer.measureHarfBuzzWidth(labels[i], measureTp);
                            float mh = TypographyNormalizer.measureFontMetricsLineHeight(measureTp);
                            if (mw > availSegW + 0.5f || mh > segH + 1.5f) {
                                allFitSingle = false;
                                break;
                            }
                        }
                        if (allFitSingle || targetPx <= floorPx + 0.05f) break;
                    }

                    if (!allFitSingle && allowScaleX) {
                        float maxW = 0f;
                        measureTp.setTextSize(targetPx);
                        measureTp.setTextScaleX(1.0f);
                        for (int i = 0; i < n; i++) {
                            TypographyNormalizer.preparePaintForScript(measureTp, labels[i], activeLoc, activeTf);
                            float mw = TypographyNormalizer.measureHarfBuzzWidth(labels[i], measureTp);
                            if (mw > maxW) maxW = mw;
                        }
                        if (maxW > availSegW + 0.5f && maxW > 1f) {
                            targetScaleX = Math.max(0.90f, Math.min(1.0f, availSegW / maxW));
                            allFitSingle = true;
                            for (int i = 0; i < n; i++) {
                                TypographyNormalizer.preparePaintForScript(measureTp, labels[i], activeLoc, activeTf);
                                measureTp.setTextScaleX(targetScaleX);
                                if (TypographyNormalizer.measureHarfBuzzWidth(labels[i], measureTp) > availSegW + 0.5f) {
                                    allFitSingle = false;
                                    break;
                                }
                            }
                        }
                    }

                    if (!allFitSingle) {
                        float curTwoPx = Math.min(origPx * 0.92f, targetPx > floorPx ? targetPx : origPx * 0.92f);
                        for (int step = 0; step < 6; step++) {
                            if (step == 5 || curTwoPx < floorPx) curTwoPx = floorPx;
                            boolean canTwoDown = true;
                            for (int i = 0; i < n; i++) {
                                int sg = TypographyNormalizer.classifyScript(labels[i]);
                                boolean canBreak = TypographyNormalizer.hasNaturalBreakToken(labels[i])
                                        || TypographyNormalizer.allowsUnspacedLineBreak(labels[i]);
                                TypographyNormalizer.preparePaintForScript(measureTp, labels[i], activeLoc, activeTf);
                                measureTp.setTextSize(curTwoPx);
                                measureTp.setTextScaleX(1.0f);
                                float mw1 = TypographyNormalizer.measureHarfBuzzWidth(labels[i], measureTp);
                                if (mw1 <= availSegW + 0.5f && TypographyNormalizer.measureFontMetricsLineHeight(measureTp) <= segH + 1.5f) {
                                    continue;
                                }
                                if (!canBreak) {
                                    canTwoDown = false;
                                    break;
                                }
                                float fm2H = 2f * TypographyNormalizer.measureFontMetricsLineHeight(measureTp);
                                StaticLayout slTest = TypographyNormalizer.buildStaticLayout(
                                        labels[i], measureTp, availSegW, Layout.Alignment.ALIGN_CENTER,
                                        0.96f, 0f, true, 0, null, TypographyNormalizer.containsRtlChars(labels[i]));
                                if (slTest.getLineCount() > 2
                                        || TypographyNormalizer.getMaxLineWidth(slTest) > availSegW + 0.5f
                                        || TypographyNormalizer.hasDisallowedWordSplit(slTest, labels[i], sg)
                                        || slTest.getHeight() > segH + 1.5f
                                        || fm2H > segH + 2.5f) {
                                    canTwoDown = false;
                                    break;
                                }
                            }
                            if (canTwoDown) {
                                targetPx = curTwoPx;
                                useTwoLines = true;
                                targetScaleX = 1.0f;
                                break;
                            }
                            if (curTwoPx <= floorPx + 0.05f) break;
                            curTwoPx = Math.max(floorPx, curTwoPx * 0.94f);
                        }
                    }
                }

                StaticLayout[] normalLayouts = new StaticLayout[n];
                StaticLayout[] selectedLayouts = new StaticLayout[n];
                int maxLinesCap = useTwoLines ? 2 : 1;
                TextUtils.TruncateAt truncMode = (!allFitSingle && !useTwoLines) ? TextUtils.TruncateAt.END : null;
                if (useTwoLines) {
                    truncMode = TextUtils.TruncateAt.END;
                }
                for (int i = 0; i < n; i++) {
                    boolean rtl = TypographyNormalizer.containsRtlChars(labels[i]);
                    TextPaint np = new TextPaint(textPaint);
                    np.setTextAlign(Paint.Align.LEFT);
                    np.setTextSize(targetPx);
                    TypographyNormalizer.preparePaintForScript(np, labels[i], activeLoc, activeTf);
                    if (allowScaleX) np.setTextScaleX(targetScaleX);
                    normalLayouts[i] = TypographyNormalizer.buildStaticLayout(
                            labels[i], np, availSegW, Layout.Alignment.ALIGN_CENTER,
                            useTwoLines ? 0.96f : 1.0f, 0f, true, maxLinesCap, truncMode, rtl);

                    TextPaint sp = new TextPaint(selectPaint);
                    sp.setTextAlign(Paint.Align.LEFT);
                    sp.setTextSize(targetPx);
                    TypographyNormalizer.preparePaintForScript(sp, labels[i], activeLoc, activeTf);
                    if (allowScaleX) sp.setTextScaleX(targetScaleX);
                    selectedLayouts[i] = TypographyNormalizer.buildStaticLayout(
                            labels[i], sp, availSegW, Layout.Alignment.ALIGN_CENTER,
                            useTwoLines ? 0.96f : 1.0f, 0f, true, maxLinesCap, truncMode, rtl);
                }

                plan = new ToggleRenderPlan(key, availSegW, targetPx, normalLayouts, selectedLayouts);
                switchView.setTag(TAG_TOGGLE_PLAN_KEY, key);
                switchView.setTag(TAG_TOGGLE_PLAN, plan);
                fToggleFontSize.setInt(switchView, Math.round(targetPx));
            }

            int selIdx = fToggleSelectIndex.getInt(switchView);
            float segW = bgRect.width() / n;
            int normColor = textPaint.getColor();
            int selColor = selectPaint.getColor();
            for (int i = 0; i < n; i++) {
                boolean selected = (i == selIdx);
                StaticLayout sl = selected ? plan.selectedLayouts[i] : plan.normalLayouts[i];
                if (sl == null) continue;
                sl.getPaint().setColor(selected ? selColor : normColor);
                float segLeft = bgRect.left + i * segW;
                float dx = segLeft + (segW - plan.availW) * 0.5f;
                float dy = bgRect.top + (bgRect.height() - sl.getHeight()) * 0.5f;
                canvas.save();
                canvas.translate(dx, dy);
                sl.draw(canvas);
                canvas.restore();
            }
            return Boolean.TRUE;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static Object onDrawHsvSeekbarName(Object arg) {
        if (!TranslationEngine.isTextAutoFitActive() || !(arg instanceof Object[])) return null;
        Object[] args = (Object[]) arg;
        if (args.length < 2 || !(args[0] instanceof View) || !(args[1] instanceof Canvas)) return null;
        View hsvView = (View) args[0];
        Canvas canvas = (Canvas) args[1];
        try {
            ensureHsvFields(hsvView.getClass());
            if (fHsvTitle == null || fHsvNameRect == null || fHsvTextPaint == null || fHsvTextColor == null) {
                return null;
            }
            Object titleObj = fHsvTitle.get(hsvView);
            Object nameRectObj = fHsvNameRect.get(hsvView);
            Object paintObj = fHsvTextPaint.get(hsvView);
            if (!(titleObj instanceof String) || !(nameRectObj instanceof RectF) || !(paintObj instanceof Paint)) {
                return null;
            }
            String titleStr = (String) titleObj;
            if (titleStr.isEmpty()) return null;
            RectF nameRect = (RectF) nameRectObj;
            Paint textPaint = (Paint) paintObj;
            Object valBgObj = fHsvValueBgRect != null ? fHsvValueBgRect.get(hsvView) : null;
            RectF valBgRect = (valBgObj instanceof RectF) ? (RectF) valBgObj : null;

            Context ctx = hsvView.getContext();
            DisplayMetrics dm = hsvView.getResources().getDisplayMetrics();
            float density = dm.density > 0f ? dm.density : 2f;
            float leftX = nameRect.left;
            float maxRight = (valBgRect != null && valBgRect.left > leftX + 16f * density)
                    ? (valBgRect.left - 6f * density)
                    : nameRect.right;
            int availW = Math.max(1, Math.round(maxRight - leftX));
            float boxH = nameRect.height() > 1f
                    ? nameRect.height()
                    : (hsvView.getHeight() > 0 ? hsvView.getHeight() : 20f * density);
            int textColor = fHsvTextColor.getInt(hsvView);

            Locale activeLoc = TranslationEngine.getActiveLocale();
            String locTag = TranslationEngine.getActiveLocaleTag();
            Typeface activeTf = ctx != null ? UIFontManager.getActiveTypeface(ctx) : null;
            int tfId = ctx != null ? UIFontManager.getActiveTypefaceIdentity(ctx) : 0;

            String resolvedTitle = titleStr;
            if (TranslationEngine.isDynamicI18nEnabled()) {
                Object tr = TranslationEngine.dispatch(TranslationEngine.OP_TRANSLATE_STRING, titleStr);
                if (tr instanceof String) {
                    resolvedTitle = (String) tr;
                }
            }
            String wrapped = TypographyNormalizer.wrapForCanvas(resolvedTitle, activeLoc);
            String key = wrapped + "|" + locTag + "|" + tfId + "|" + availW + "|" + Math.round(boxH) + "|" + textColor;

            HsvNameRenderPlan plan;
            Object cachedPlanObj = hsvView.getTag(TAG_HSV_PLAN);
            if (key.equals(hsvView.getTag(TAG_HSV_PLAN_KEY)) && cachedPlanObj instanceof HsvNameRenderPlan) {
                plan = (HsvNameRenderPlan) cachedPlanObj;
            } else {
                float origPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12f, dm);
                float floorPx = Math.min(origPx, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 8.5f, dm));
                TextPaint tp = new TextPaint(textPaint);
                tp.setColor(textColor);
                tp.setTextAlign(Paint.Align.LEFT);
                TypographyNormalizer.preparePaintForScript(tp, wrapped, activeLoc, activeTf);

                float targetPx = origPx;
                tp.setTextSize(targetPx);
                tp.setTextScaleX(1.0f);
                while (TypographyNormalizer.measureHarfBuzzWidth(wrapped, tp) > availW + 0.5f && targetPx > floorPx) {
                    targetPx = Math.max(floorPx, targetPx * 0.92f);
                    tp.setTextSize(targetPx);
                }
                float wAfter = TypographyNormalizer.measureHarfBuzzWidth(wrapped, tp);
                if (wAfter > availW + 0.5f
                        && TypographyNormalizer.classifyScript(wrapped) == TypographyNormalizer.SCRIPT_LATIN_CYRILLIC_GREEK
                        && wAfter > 1f) {
                    tp.setTextScaleX(Math.max(0.90f, Math.min(1.0f, availW / wAfter)));
                }
                boolean rtl = TypographyNormalizer.containsRtlChars(wrapped);
                StaticLayout sl = TypographyNormalizer.buildStaticLayout(
                        wrapped, tp, availW, Layout.Alignment.ALIGN_NORMAL,
                        1.0f, 0f, true, 1, TextUtils.TruncateAt.END, rtl);
                float drawY = nameRect.centerY() - sl.getHeight() * 0.5f;
                plan = new HsvNameRenderPlan(key, sl, leftX, drawY);
                hsvView.setTag(TAG_HSV_PLAN_KEY, key);
                hsvView.setTag(TAG_HSV_PLAN, plan);
            }

            if (plan.layout != null) {
                plan.layout.getPaint().setColor(textColor);
                canvas.save();
                canvas.translate(plan.drawX, plan.drawY);
                plan.layout.draw(canvas);
                canvas.restore();
                return Boolean.TRUE;
            }
            return null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}

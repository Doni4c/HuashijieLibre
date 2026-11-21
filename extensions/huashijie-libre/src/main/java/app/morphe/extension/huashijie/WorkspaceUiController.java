package app.morphe.extension.huashijie;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
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
            Class<?> clz = Class.forName("net.huanci.hsjpro.utils.o00O000o");
            Method m = LibreResourceResolver.findMethod(
                    clz, "Oooo0oo", String.class, true, boolean.class, String.class);
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
            Class<?> clz = Class.forName("net.huanci.hsjpro.utils.o00O000o");
            Method m = LibreResourceResolver.findMethod(clz, "Oooo00o", String.class, true);
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
                tvHeader.setText(TranslationEngine.get("libre_storage_box_title", "\u753b\u4e16\u754c Libre\uff1a\u5de5\u7a0b\u5b58\u50a8\u8def\u5f84 (.hsj)"));
                tvHeader.setTextColor(isDark ? Color.parseColor("#E0E0E0") : Color.parseColor("#1A1A1A"));
                tvHeader.setTextSize(14.0f);
                tvHeader.setTypeface(null, android.graphics.Typeface.BOLD);

                String draftPrefix = extractLabelPrefix(TranslationEngine.get(
                        "libre_storage_box_draft", "\u672c\u5730\u8349\u7a3f\u76ee\u5f55\uff1a/storage/emulated/0/HuashijiePro/Draft/"));
                String exportPrefix = extractLabelPrefix(TranslationEngine.get(
                        "libre_storage_box_export", "\u5de5\u7a0b\u5bfc\u51fa\u76ee\u5f55\uff1a/storage/emulated/0/HuashijiePro/exportDrafts/"));
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
            int count = container.getChildCount();
            for (int i = 0; i < count; i++) {
                View child = container.getChildAt(i);
                if (child instanceof ViewGroup) {
                    ViewGroup vg = (ViewGroup) child;
                    TextView label = null;
                    View toggle = null;
                    for (int j = 0; j < vg.getChildCount(); j++) {
                        View sub = vg.getChildAt(j);
                        if (sub instanceof TextView) {
                            label = (TextView) sub;
                        } else if (sub instanceof android.widget.CompoundButton) {
                            toggle = sub;
                        }
                    }
                    if (label != null && toggle != null) {
                        ViewGroup.LayoutParams lp = label.getLayoutParams();
                        if (lp instanceof ViewGroup.MarginLayoutParams) {
                            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
                            mlp.rightMargin = safeEndMargin;
                            if (android.os.Build.VERSION.SDK_INT >= 17) {
                                mlp.setMarginEnd(safeEndMargin);
                            }
                            label.setLayoutParams(mlp);
                        }
                        label.setMaxLines(2);
                        label.setSingleLine(false);
                        label.setEllipsize(TextUtils.TruncateAt.END);
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
                    Class<?> utilClass = Class.forName("net.huanci.hsjpro.utils.oo0oO0");
                    java.lang.reflect.Method setMethod = utilClass.getMethod("o000", String.class, boolean.class);
                    setMethod.invoke(null, "quit_draft_cloud_sync_hint", true);
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
                setMaxLinesIfChanged(labelTv, 2);
                int desiredGrav = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
                setGravityIfChanged(labelTv, desiredGrav);
                setTextAlignmentIfChanged(labelTv, View.TEXT_ALIGNMENT_CENTER);
                int w = labelTv.getWidth();
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
                            TextPaint tp = new TextPaint(labelTv.getPaint());
                            float minGridPx = origPx * 0.72f;
                            float targetPx = origPx;
                            for (int step = 0; step < 8; step++) {
                                tp.setTextSize(targetPx);
                                float maxWordW = TranslationEngine.computeMaxWordWidth(tp, str);
                                StaticLayout sl;
                                if (Build.VERSION.SDK_INT >= 23) {
                                    sl = StaticLayout.Builder.obtain(str, 0, str.length(), tp, Math.max(1, availW))
                                            .setAlignment(Layout.Alignment.ALIGN_CENTER)
                                            .setLineSpacing(labelTv.getLineSpacingExtra(), labelTv.getLineSpacingMultiplier())
                                            .setIncludePad(labelTv.getIncludeFontPadding())
                                            .build();
                                } else {
                                    sl = new StaticLayout(str, tp, Math.max(1, availW), Layout.Alignment.ALIGN_CENTER,
                                            labelTv.getLineSpacingMultiplier(), labelTv.getLineSpacingExtra(),
                                            labelTv.getIncludeFontPadding());
                                }
                                float maxLineW = 0f;
                                for (int li = 0; li < sl.getLineCount(); li++) {
                                    float lw = sl.getLineWidth(li);
                                    if (lw > maxLineW) maxLineW = lw;
                                }
                                if ((sl.getLineCount() <= 2 && maxWordW <= availW && maxLineW <= availW)
                                        || targetPx <= minGridPx) {
                                    break;
                                }
                                targetPx = Math.max(minGridPx, targetPx * 0.94f);
                            }
                            if (Math.abs(labelTv.getTextSize() - targetPx) >= 0.5f) {
                                labelTv.setTextSize(TypedValue.COMPLEX_UNIT_PX, targetPx);
                            }
                        }
                    }
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
            int desiredGrav = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            if (tv.getGravity() != desiredGrav) {
                tv.setGravity(desiredGrav);
                mutated = true;
            }
            if (Build.VERSION.SDK_INT >= 17 && tv.getTextAlignment() != View.TEXT_ALIGNMENT_CENTER) {
                tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                mutated = true;
            }
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
                                String measureKey = textStr + "|" + Float.floatToRawIntBits(origRefPx)
                                        + "|" + System.identityHashCode(tv.getTypeface());
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
                                            TextPaint refPaint = new TextPaint(tv.getPaint());
                                            refPaint.setTextSize(origRefPx);
                                            measuredRefW = refPaint.measureText(textStr);
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

    // State tracking for Reference Window Vertical Flip
    private static volatile boolean sCanvasVertiFlip = false;
    private static final String TAG_FLIP_VERTI_BTN = "tag_ref_flip_verti";

    public static boolean isCanvasVertiFlip() {
        return sCanvasVertiFlip;
    }

    public static void setCanvasVertiFlip(boolean flip) {
        sCanvasVertiFlip = flip;
    }

    // Cached reflection members for high-frequency GL texture buffer draws.
    private static volatile Class<?> sCachedNavClass = null;
    private static volatile Field sFieldGlTool = null;
    private static volatile Field sFieldHoriFlip = null;
    private static volatile Class<?> sCachedGlToolClass = null;
    private static volatile Method sMethodGlVerti = null;
    private static volatile Method sMethodGlHori = null;
    private static volatile Method sMethodGlNormal = null;

    private static void ensureNavReflection(Class<?> navClass) {
        if (sCachedNavClass == navClass) return;
        sFieldGlTool = LibreResourceResolver.findField(navClass, "mGLTool", null, null);
        sFieldHoriFlip = LibreResourceResolver.findField(navClass, "mHoriFlip", null, null);
        sCachedNavClass = navClass;
    }

    private static void ensureGlToolMethods(Class<?> glClass) {
        if (sCachedGlToolClass == glClass) return;
        try {
            sMethodGlVerti = glClass.getMethod("Oooo0O0");
        } catch (Throwable ignored) {
            sMethodGlVerti = null;
        }
        try {
            sMethodGlHori = glClass.getMethod("Oooo0");
        } catch (Throwable ignored) {
            sMethodGlHori = null;
        }
        try {
            sMethodGlNormal = glClass.getMethod("OooOOo0");
        } catch (Throwable ignored) {
            sMethodGlNormal = null;
        }
        sCachedGlToolClass = glClass;
    }

    public static java.nio.FloatBuffer getCanvasTextureBuffer(View navView) {
        if (navView == null) return null;
        try {
            ensureNavReflection(navView.getClass());
            if (sFieldGlTool == null || sFieldHoriFlip == null) return null;
            Object glToolObj = sFieldGlTool.get(navView);
            boolean horiFlip = sFieldHoriFlip.getBoolean(navView);
            return getCanvasTextureBuffer(glToolObj, horiFlip);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static java.nio.FloatBuffer getCanvasTextureBuffer(Object glToolObj, boolean horiFlip) {
        if (glToolObj == null) return null;
        try {
            ensureGlToolMethods(glToolObj.getClass());
            Method m = sCanvasVertiFlip ? sMethodGlVerti : (horiFlip ? sMethodGlHori : sMethodGlNormal);
            return m != null ? (java.nio.FloatBuffer) m.invoke(glToolObj) : null;
        } catch (Throwable ignored) {
            return null;
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

    /**
     * Widens cramped horizontal canvas sub-toolbars (Lasso selection, Shape selector, Symmetry selector)
     * in PaintActivity so localized labels fit cleanly without unnatural wrapping or horizontal clipping.
     */
    public static boolean optimizeCanvasToolBars(View root) {
        if (root == null || !isCanvasPopupPolishActive()) return false;
        Context ctx = root.getContext();
        if (ctx == null || !TranslationEngine.resolveHostActivityName(root).endsWith("PaintActivity")) return false;
        boolean mutated = false;
        try {
            android.util.DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
            int screenW = dm.widthPixels;
            float density = dm.density;
            if (screenW <= 0 || density <= 0f) return false;

            // 1. Expand ShapeSettingView (295dp -> up to 370dp) and content_view (240dp -> up to 312dp).
            int idShape = LibreResourceResolver.resolveViewId(ctx, "shape_setting", 0);
            if (idShape != 0) {
                View shapeBar = root.findViewById(idShape);
                if (shapeBar != null && shapeBar.getVisibility() == View.VISIBLE) {
                    int targetShapeW = Math.min(Math.round(370 * density), screenW - Math.round(16 * density));
                    ViewGroup.LayoutParams sLp = shapeBar.getLayoutParams();
                    if (sLp != null && sLp.width > 0 && sLp.width < targetShapeW) {
                        sLp.width = targetShapeW;
                        shapeBar.setLayoutParams(sLp);
                        mutated = true;
                    }
                    int idContent = LibreResourceResolver.resolveViewId(ctx, "content_view", 0);
                    View contentView = idContent != 0 ? shapeBar.findViewById(idContent) : null;
                    if (contentView != null) {
                        int targetContentW = Math.max(Math.round(240 * density), targetShapeW - Math.round(58 * density));
                        ViewGroup.LayoutParams cLp = contentView.getLayoutParams();
                        if (cLp != null && cLp.width > 0 && cLp.width < targetContentW) {
                            cLp.width = targetContentW;
                            contentView.setLayoutParams(cLp);
                            mutated = true;
                        }
                    }
                }
            }

            // 2. Expand SymmetrySettingView (290dp -> up to 376dp) across up to 11 symmetry buttons.
            int idToolsView = LibreResourceResolver.resolveViewId(ctx, "tools_view", 0);
            if (idToolsView != 0) {
                View toolsView = root.findViewById(idToolsView);
                if (toolsView != null && toolsView.getParent() instanceof View) {
                    View symRoot = (View) toolsView.getParent();
                    if (symRoot.getVisibility() == View.VISIBLE
                            && symRoot.getClass().getName().endsWith("SymmetrySettingView")) {
                        int targetSymW = Math.min(Math.round(376 * density), screenW - Math.round(16 * density));
                        ViewGroup.LayoutParams symLp = symRoot.getLayoutParams();
                        if (symLp != null && symLp.width > 0 && symLp.width < targetSymW) {
                            symLp.width = targetSymW;
                            symRoot.setLayoutParams(symLp);
                            mutated = true;
                        }
                    }
                }
            }

            // 3. Optimize XuanQuSettingView: expand view1 (280dp -> ~356dp) with compact 6dp gaps,
            // and clamp view2 (415dp) to viewport while trimming 16dp transform gaps down to 4dp.
            int idDeformMenu = LibreResourceResolver.resolveViewId(ctx, "ll_img_deformation_menu", 0);
            if (idDeformMenu != 0) {
                View deformMenu = root.findViewById(idDeformMenu);
                if (deformMenu instanceof ViewGroup && deformMenu.getParent() instanceof View) {
                    View view2 = (View) deformMenu.getParent();
                    View shadowLayout = (view2.getParent() instanceof View) ? (View) view2.getParent() : null;
                    if (shadowLayout instanceof ViewGroup) {
                        int idView1 = LibreResourceResolver.resolveViewId(ctx, "view1", 0);
                        View view1 = idView1 != 0 ? shadowLayout.findViewById(idView1) : null;
                        if (view1 instanceof ViewGroup && view1.getVisibility() == View.VISIBLE) {
                            int targetV1W = Math.max(Math.round(280 * density),
                                    Math.min(Math.round(356 * density), screenW - Math.round(24 * density)));
                            ViewGroup.LayoutParams v1Lp = view1.getLayoutParams();
                            if (v1Lp != null && v1Lp.width > 0 && v1Lp.width != targetV1W) {
                                v1Lp.width = targetV1W;
                                view1.setLayoutParams(v1Lp);
                                mutated = true;
                            }
                            if (!Boolean.TRUE.equals(view1.getTag(TAG_TOOLBAR_POLISHED))) {
                                ViewGroup vg1 = (ViewGroup) view1;
                                for (int i = 0; i < vg1.getChildCount(); i++) {
                                    View row = vg1.getChildAt(i);
                                    if (row instanceof LinearLayout
                                            && ((LinearLayout) row).getOrientation() == LinearLayout.HORIZONTAL) {
                                        LinearLayout hRow = (LinearLayout) row;
                                        int gapPx = Math.round(4 * density);
                                        int edgePx = Math.round(6 * density);
                                        for (int j = 0; j < hRow.getChildCount(); j++) {
                                            View ch = hRow.getChildAt(j);
                                            setHorizontalMargins(ch, j == 0 ? edgePx : gapPx,
                                                    j == hRow.getChildCount() - 1 ? edgePx : gapPx);
                                        }
                                        mutated = true;
                                    }
                                }
                                view1.setTag(TAG_TOOLBAR_POLISHED, Boolean.TRUE);
                            }
                        }
                        if (view2.getVisibility() == View.VISIBLE) {
                            int maxV2W = screenW - Math.round(20 * density);
                            int targetV2W = Math.min(Math.round(415 * density), maxV2W);
                            ViewGroup.LayoutParams v2Lp = view2.getLayoutParams();
                            if (v2Lp != null && v2Lp.width > 0 && v2Lp.width != targetV2W) {
                                v2Lp.width = targetV2W;
                                view2.setLayoutParams(v2Lp);
                                mutated = true;
                            }
                            if (!Boolean.TRUE.equals(deformMenu.getTag(TAG_TOOLBAR_POLISHED))) {
                                ViewGroup deformGroup = (ViewGroup) deformMenu;
                                int smallGap = Math.round(4 * density);
                                for (int i = 0; i < deformGroup.getChildCount(); i++) {
                                    View ch = deformGroup.getChildAt(i);
                                    setHorizontalMargins(ch, 0, i == deformGroup.getChildCount() - 1 ? 0 : smallGap);
                                }
                                deformMenu.setTag(TAG_TOOLBAR_POLISHED, Boolean.TRUE);
                                mutated = true;
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
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
}

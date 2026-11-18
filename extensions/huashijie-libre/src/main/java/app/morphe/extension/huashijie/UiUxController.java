package app.morphe.extension.huashijie;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Huashijie Libre: Canvas and UI/UX Controller
 * Manages touch-outside dismissal, floating brush sliders with dedicated drag handle,
 * reference window rotation, and UI typeface integration.
 */
public final class UiUxController {
    private static final String NAME_TOP_BAR = "top_setting_rootView";
    private static final int FALLBACK_TOP_BAR = 0x7f090d8a;
    private static final String NAME_LEFT_SETTING = "layout_refresh_draft";
    private static final int FALLBACK_LEFT_SETTING = 0x7f09077e;
    private static final String NAME_CLEAR_TV_VIEW = "clear_tv_view";
    private static final int FALLBACK_CLEAR_TV_VIEW = 0x7f090359;
    private static final String NAME_TV_CANCEL = "tv_cancle";
    private static final int FALLBACK_TV_CANCEL = 0x7f090de9;
    private static final String NAME_VIEW1 = "view1";
    private static final int FALLBACK_VIEW1 = 0x7f090fc6;
    private static final String NAME_ITEM_PAINT_LEFT_SETTING = "item_paint_left_setting";
    private static final int FALLBACK_ITEM_PAINT_LEFT_SETTING = 0x7f090648;
    private static final String TAG_DRAG_HANDLE = "libre_brush_slider_handle";

    private UiUxController() {}

    /**
     * Initializes activity lifecycle callbacks for UI font injection.
     */
    public static void initFontLifecycle(Application app) {
        UIFontManager.init(app);
    }

    private static volatile boolean sRefRotationFlagReady = false;
    private static volatile boolean sRefRotationEnabled = true;

    private static boolean isReferenceRotationEnabledFast() {
        ensureFlagListenerRegistered(null);
        if (!sRefRotationFlagReady) {
            sRefRotationEnabled = LibreConfig.isFeatureActive(LibreConfig.KEY_FREE_REF_ROTATION);
            sRefRotationFlagReady = true;
        }
        return sRefRotationEnabled;
    }

    /**
     * Hook called from ReferenceModel.isAllowRotate()Z.
     * Forces reference rotation to be enabled when free reference rotation switch is on.
     */
    public static boolean isReferenceRotationEnabled(boolean original) {
        try {
            if (isReferenceRotationEnabledFast()) {
                return true;
            }
        } catch (Throwable ignored) {}
        return original;
    }

    /**
     * Touch outside dismissal handler.
     * Hooked at entry point of PaintActivity.dispatchTouchEvent(MotionEvent).
     * Non consuming: returns void and lets the event continue to super.dispatchTouchEvent.
     * Only dismisses active temporary tool panels (Selection, Symmetry, Perspective, AnimFrame)
     * if the tap is outside the panel AND not on the top bar or left setting view.
     */
    private static float sTouchDownX = 0f;
    private static float sTouchDownY = 0f;
    private static long sTouchDownTime = 0L;
    private static boolean sIsPotentialTap = false;

    private static volatile boolean sTouchOutsideFlagReady = false;
    private static volatile boolean sTouchOutsideEnabled = true;
    private static volatile boolean sTouchOutsideListenerRegistered = false;
    private static volatile float sCachedDensity = 0f;
    private static volatile Class<?> sResolvedActivityClass = null;
    private static volatile Field sOutCloseField = null;
    private static volatile Field sXuanQuField = null;
    private static volatile Field sMoveViewField = null;
    private static volatile Field sLeftViewField = null;
    private static volatile Field sPaintViewField = null;
    private static volatile boolean sCurItemMethodResolved = false;
    private static volatile Method sCurItemMethod = null;
    private static volatile boolean sPaintViewMembersResolved = false;
    private static volatile Field sMineKeyField = null;
    private static volatile Method sIsSelectEmptyMethod = null;

    private static void ensureFlagListenerRegistered(Activity activity) {
        if (LibreConfig.getInstance() == null && activity != null) {
            LibreConfig.init(activity);
        }
        if (!sTouchOutsideListenerRegistered) {
            LibreConfig cfg = LibreConfig.getInstance();
            if (cfg != null) {
                sTouchOutsideListenerRegistered = true;
                cfg.addListener(key -> {
                    if (LibreConfig.KEY_TOUCH_OUTSIDE_DISMISS.equals(key)) {
                        sTouchOutsideFlagReady = false;
                    } else if (LibreConfig.KEY_FREE_REF_ROTATION.equals(key)) {
                        sRefRotationFlagReady = false;
                    }
                });
            }
        }
    }

    private static boolean isTouchOutsideDismissEnabled(Activity activity) {
        ensureFlagListenerRegistered(activity);
        if (!sTouchOutsideFlagReady) {
            sTouchOutsideEnabled = LibreConfig.isFeatureActive(LibreConfig.KEY_TOUCH_OUTSIDE_DISMISS);
            sTouchOutsideFlagReady = true;
        }
        return sTouchOutsideEnabled;
    }

    private static void ensureActivityReflectionCache(Activity activity) {
        Class<?> actClass = activity.getClass();
        if (sResolvedActivityClass == actClass) {
            return;
        }
        sOutCloseField = LibreResourceResolver.findField(actClass, "o000O0o0", null, "OutCloseView");
        sXuanQuField = LibreResourceResolver.findField(actClass, "Oooooo0", null, "XuanQuSettingView");
        sMoveViewField = LibreResourceResolver.findField(actClass, "Ooooooo", null, "XuanquMoveView");
        sLeftViewField = LibreResourceResolver.findField(actClass, "Oooo00O", null, "PaintLeftSettingView");
        sPaintViewField = LibreResourceResolver.findField(actClass, null, null, "PaintView");
        sResolvedActivityClass = actClass;
    }

    private static View getCachedActivityView(Activity activity, Field f) {
        if (activity == null || f == null) return null;
        try {
            Object v = f.get(activity);
            return (v instanceof View) ? (View) v : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Handles touch events for selection tools (Lasso / OutCloseView).
     * Strictly differentiates between a stationary single tap and continuous brush strokes:
     * (a) ACTION_DOWN: only records start coordinates and time. Never triggers dismiss.
     * (b) ACTION_MOVE: cancels tap state if movement exceeds touch slop (preserving drawing strokes).
     * (c) ACTION_UP: only triggers cancel/dismiss on a stationary single tap outside active panels.
     */
    private static boolean checkLeftToolSelection(Object leftViewObj) {
        if (leftViewObj == null) return false;
        try {
            if (!sCurItemMethodResolved) {
                for (Method m : leftViewObj.getClass().getMethods()) {
                    if (m.getName().startsWith("getCurrent") && m.getName().endsWith("ItemModelType") && m.getParameterTypes().length == 0) {
                        sCurItemMethod = m;
                        break;
                    }
                }
                sCurItemMethodResolved = true;
            }
            if (sCurItemMethod != null) {
                Object curType = sCurItemMethod.invoke(leftViewObj);
                if (curType != null && curType.toString().contains("XuanQu")) {
                    return true;
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public static void handlePaintTouchOutside(Activity activity, MotionEvent ev) {
        if (activity == null || ev == null) return;

        try {
            int action = ev.getActionMasked();
            if (ev.getPointerCount() > 1) {
                sIsPotentialTap = false;
                return;
            }

            float rawX = ev.getRawX();
            float rawY = ev.getRawY();

            if (action == MotionEvent.ACTION_DOWN) {
                sTouchDownX = rawX;
                sTouchDownY = rawY;
                sTouchDownTime = android.os.SystemClock.uptimeMillis();
                sIsPotentialTap = true;
                return;
            }

            if (action == MotionEvent.ACTION_MOVE) {
                if (!sIsPotentialTap) return;
                float density = sCachedDensity;
                if (density <= 0f) {
                    density = activity.getResources().getDisplayMetrics().density;
                    sCachedDensity = density;
                }
                float slop = 15f * density;
                if (Math.abs(rawX - sTouchDownX) > slop || Math.abs(rawY - sTouchDownY) > slop) {
                    sIsPotentialTap = false;
                }
                return;
            }

            if (action == MotionEvent.ACTION_UP) {
                if (!sIsPotentialTap) return;
                sIsPotentialTap = false;

                long duration = android.os.SystemClock.uptimeMillis() - sTouchDownTime;
                if (duration > 400L) return;

                float density = sCachedDensity;
                if (density <= 0f) {
                    density = activity.getResources().getDisplayMetrics().density;
                    sCachedDensity = density;
                }
                float slop = 15f * density;
                if (Math.abs(rawX - sTouchDownX) > slop || Math.abs(rawY - sTouchDownY) > slop) {
                    return;
                }

                // Mark layout dirty strictly on stationary single-tap release (zero brush-stroke overhead)
                // so in-place visibility switches or button text updates translate before Frame 0.
                TranslationEngine.markLayoutDirty();

                if (!isTouchOutsideDismissEnabled(activity)) {
                    return;
                }

                int tapX = (int) sTouchDownX;
                int tapY = (int) sTouchDownY;

                // Fast coordinate guard
                if (tapY <= 110 || tapX <= 110) return;

                ensureActivityReflectionCache(activity);

                // Check cached selection overlays and left toolbar mode BEFORE expensive top/left bar View lookups + Rect allocations
                View outCloseRaw = getCachedActivityView(activity, sOutCloseField);
                ViewGroup outCloseView = (outCloseRaw instanceof ViewGroup) ? (ViewGroup) outCloseRaw : null;
                boolean outCloseActive = (outCloseView != null && outCloseView.getVisibility() == View.VISIBLE && outCloseView.getChildCount() > 0);

                View xuanQuView = getCachedActivityView(activity, sXuanQuField);
                boolean xuanQuActive = (xuanQuView != null && xuanQuView.getVisibility() == View.VISIBLE);

                View moveView = getCachedActivityView(activity, sMoveViewField);
                boolean moveActive = (moveView != null && moveView.getVisibility() == View.VISIBLE);

                View leftSetting = null;
                Object leftViewObj = getCachedActivityView(activity, sLeftViewField);
                if (leftViewObj == null && sLeftViewField == null) {
                    leftSetting = LibreResourceResolver.findViewByName(activity, NAME_LEFT_SETTING, FALLBACK_LEFT_SETTING);
                    leftViewObj = leftSetting;
                }
                boolean isLeftToolSelection = checkLeftToolSelection(leftViewObj);

                // Fast exit before top/left Rect lookups or PaintView selection area check unless a selection indicator is active
                if (!outCloseActive && !xuanQuActive && !moveActive && !isLeftToolSelection) {
                    return;
                }

                // 1. Check Top Bar bounds
                View topBar = LibreResourceResolver.findViewByName(activity, NAME_TOP_BAR, FALLBACK_TOP_BAR);
                if (topBar != null && topBar.getVisibility() == View.VISIBLE) {
                    Rect topBarRect = new Rect();
                    topBar.getGlobalVisibleRect(topBarRect);
                    if (topBarRect.contains(tapX, tapY)) return;
                }

                // 2. Check Left Toolbar bounds
                if (leftSetting == null) {
                    leftSetting = LibreResourceResolver.findViewByName(activity, NAME_LEFT_SETTING, FALLBACK_LEFT_SETTING);
                }
                if (leftSetting != null && leftSetting.getVisibility() == View.VISIBLE) {
                    Rect leftRect = new Rect();
                    leftSetting.getGlobalVisibleRect(leftRect);
                    if (leftRect.contains(tapX, tapY)) return;
                }

                // 3. Check active OutCloseView (Selection floating action buttons)
                if (outCloseActive) {
                    for (int i = 0; i < outCloseView.getChildCount(); i++) {
                        View child = outCloseView.getChildAt(i);
                        if (child != null && child.getVisibility() == View.VISIBLE) {
                            Rect cRect = new Rect();
                            child.getGlobalVisibleRect(cRect);
                            if (cRect.contains(tapX, tapY)) return;
                        }
                    }
                }

                // 4. Check active XuanQuSettingView (Lasso/Selection bottom control toolbar)
                if (xuanQuActive) {
                    // If tap is inside the bottom control bar area (y >= 2000), do not dismiss
                    if (tapY >= 2000) return;
                    if (xuanQuView instanceof ViewGroup) {
                        ViewGroup xqGroup = (ViewGroup) xuanQuView;
                        int totalH = xqGroup.getHeight();
                        if (totalH == 0) totalH = activity.getResources().getDisplayMetrics().heightPixels;
                        for (int ci = 0; ci < xqGroup.getChildCount(); ci++) {
                            View child = xqGroup.getChildAt(ci);
                            if (child != null && child.getVisibility() == View.VISIBLE) {
                                Rect cRect = new Rect();
                                child.getGlobalVisibleRect(cRect);
                                if (cRect.width() > 0 && cRect.height() > 0 && cRect.height() < totalH * 0.7f) {
                                    if (cRect.contains(tapX, tapY)) {
                                        return;
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. Check active XuanquMoveView (Selection move overlay buttons only)
                if (moveActive && moveView instanceof ViewGroup) {
                    ViewGroup mvGroup = (ViewGroup) moveView;
                    for (int mi = 0; mi < mvGroup.getChildCount(); mi++) {
                        View mChild = mvGroup.getChildAt(mi);
                        if (mChild != null && mChild.getVisibility() == View.VISIBLE) {
                            Rect mcRect = new Rect();
                            mChild.getGlobalVisibleRect(mcRect);
                            if (mcRect.width() > 0 && mcRect.height() > 0 && mcRect.contains(tapX, tapY)) {
                                return;
                            }
                        }
                    }
                }

                // 7. Check if PaintView has an active selection area
                boolean hasActiveSelectionArea = false;
                try {
                    Object pv = sPaintViewField != null ? sPaintViewField.get(activity) : null;
                    if (pv != null) {
                        if (!sPaintViewMembersResolved) {
                            sMineKeyField = pv.getClass().getField("MINE_KEY");
                            sIsSelectEmptyMethod = pv.getClass().getMethod("isLayerSelectAreaEmpty", int.class);
                            sPaintViewMembersResolved = true;
                        }
                        if (sMineKeyField != null && sIsSelectEmptyMethod != null) {
                            int mineKey = sMineKeyField.getInt(pv);
                            boolean isEmpty = (Boolean) sIsSelectEmptyMethod.invoke(pv, mineKey);
                            if (!isEmpty) {
                                hasActiveSelectionArea = true;
                            }
                        }
                    }
                } catch (Throwable ignored) {}

                // Only dismiss if selection or lasso mode is currently active
                if (!outCloseActive && !xuanQuActive && !moveActive && !isLeftToolSelection && !hasActiveSelectionArea) {
                    return;
                }

                android.util.Log.d("UiUxController", "Stationary tap outside selection bounds at (" + tapX + ", " + tapY + ") -> scheduling dismissal on main looper");
                final ViewGroup fOutClose = outCloseView;
                final View fXuanQu = xuanQuView;
                final View fMove = moveView;
                activity.getWindow().getDecorView().post(new Runnable() {
                    @Override
                    public void run() {
                        dismissSelectionOrLasso(activity, fOutClose, fXuanQu, fMove);
                    }
                });
            }
        } catch (Throwable ignored) {}
    }

    private static View resolveActivityViewField(Activity activity, String exactName, String typeContains) {
        if (activity == null) return null;
        try {
            Field f = LibreResourceResolver.findField(activity.getClass(), exactName, null, typeContains);
            if (f != null) {
                Object v = f.get(activity);
                if (v instanceof View) return (View) v;
            }
        } catch (Throwable ignored) {}
        return null;
    }

    /**
     * Dismisses active Selection or Lasso mode and returns to normal drawing state.
     */
    public static void dismissSelectionOrLasso(Activity activity, ViewGroup outCloseView, View xuanQuView, View moveView) {
        if (activity == null) return;
        try {
            // 1. If OutCloseView is visible, remove and hide
            if (outCloseView != null) {
                try {
                    View closeBtn = LibreResourceResolver.findViewByName(outCloseView, NAME_CLEAR_TV_VIEW, FALLBACK_CLEAR_TV_VIEW);
                    if (closeBtn != null) {
                        closeBtn.performClick();
                    }
                } catch (Throwable ignored) {}
                try {
                    Method removeBtn = LibreResourceResolver.findMethod(
                            outCloseView.getClass(), "removeCloseBtn", void.class, false, int.class);
                    if (removeBtn != null) {
                        removeBtn.invoke(outCloseView, 1);
                    }
                } catch (Throwable ignored) {}
                try {
                    outCloseView.removeAllViews();
                    outCloseView.setVisibility(View.GONE);
                } catch (Throwable ignored) {}
            }

            // 2. XuanQuSettingView dismissal
            View xq = xuanQuView != null ? xuanQuView : resolveActivityViewField(activity, "Oooooo0", "XuanQuSettingView");
            if (xq != null) {
                try {
                    Field v1Field = LibreResourceResolver.findField(xq.getClass(), "view1", null, null);
                    View v1 = v1Field != null ? (View) v1Field.get(xq) : null;
                    if (v1 != null) v1.setVisibility(View.GONE);
                } catch (Throwable ignored) {}
                try {
                    Field v2Field = LibreResourceResolver.findField(xq.getClass(), "view2", null, null);
                    View v2 = v2Field != null ? (View) v2Field.get(xq) : null;
                    if (v2 != null) v2.setVisibility(View.GONE);
                } catch (Throwable ignored) {}
                try {
                    Method cancelMethod = LibreResourceResolver.findMethod(xq.getClass(), "cancel", void.class, false);
                    if (cancelMethod != null) cancelMethod.invoke(xq);
                } catch (Throwable ignored) {}
                try {
                    View cancelBtn = LibreResourceResolver.findViewByName(xq, NAME_TV_CANCEL, FALLBACK_TV_CANCEL);
                    if (cancelBtn != null) {
                        cancelBtn.performClick();
                    }
                } catch (Throwable ignored) {}
                xq.setVisibility(View.GONE);
            }

            // 3. Hide XuanquMoveView
            View mv = moveView != null ? moveView : resolveActivityViewField(activity, "Ooooooo", "XuanquMoveView");
            if (mv != null) {
                mv.setVisibility(View.GONE);
            }

            // 4. Clear selection area in PaintView and end photo placement
            try {
                Field pvField = LibreResourceResolver.findField(activity.getClass(), null, null, "PaintView");
                if (pvField != null) {
                    Object paintView = pvField.get(activity);
                    if (paintView != null) {
                        Field mineKeyField = paintView.getClass().getField("MINE_KEY");
                        int mineKey = mineKeyField.getInt(paintView);
                        int userId = 0;
                        try {
                            Field userField = LibreResourceResolver.findField(activity.getClass(), "o0000oOO", null, null);
                            if (userField != null) {
                                userId = userField.getInt(activity);
                            }
                        } catch (Throwable ignored) {}

                        try {
                            Method endPlace = LibreResourceResolver.findMethod(
                                    paintView.getClass(), "endPlacePhoto", void.class, false, boolean.class, Runnable.class);
                            if (endPlace != null) {
                                endPlace.invoke(paintView, false, null);
                            }
                        } catch (Throwable ignored) {}

                        clearGlSelection(paintView, mineKey, userId);
                    }
                }
            } catch (Throwable ignored) {}

            // 5. Switch tool back to Brush natively with retry until GL thread resets state
            final View decor = activity.getWindow().getDecorView();
            final int[] delays = {80, 220, 450, 800};
            for (int d : delays) {
                decor.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            View v1 = LibreResourceResolver.findViewByName(activity, NAME_VIEW1, FALLBACK_VIEW1);
                            if (v1 != null && v1.isShown()) {
                                switchBackToBrush(activity);
                            }
                        } catch (Throwable ignored) {}
                    }
                }, d);
            }
            android.util.Log.d("UiUxController", "Selection/Lasso dismissal scheduled, tool switch to Brush pending.");
        } catch (Throwable ignored) {}
    }

    private static void switchBackToBrush(Activity activity) {
        try {
            try {
                View brushItem = findLeftToolItem(activity.getWindow().getDecorView(), "Brush");
                if (brushItem == null) brushItem = findLeftToolItem(activity.getWindow().getDecorView(), null);
                boolean clicked = (brushItem != null) && brushItem.performClick();
                android.util.Log.d("UiUxController", "Brush item found=" + (brushItem != null) + " clicked=" + clicked);
            } catch (Throwable ignored) {}

            try {
                Field leftField = LibreResourceResolver.findField(
                        activity.getClass(), "Oooo00O", null, "PaintLeftSettingView");
                if (leftField != null) {
                    Object leftView = leftField.get(activity);
                    if (leftView != null) {
                        Method rb = LibreResourceResolver.findMethod(
                                leftView.getClass(), "rollBackLastDrawItemModel", void.class, false);
                        if (rb != null) {
                            rb.invoke(leftView);
                        }
                    }
                }
            } catch (Throwable ignored) {}

            try {
                Class<?> enumClass = Class.forName("net.huanci.hsjpro.enums.PaintLeftItemType");
                Object brushEnum = null;
                for (Object c : enumClass.getEnumConstants()) {
                    if ("Brush".equals(c.toString())) {
                        brushEnum = c;
                        break;
                    }
                }
                if (brushEnum != null) {
                    Method switchMethod = LibreResourceResolver.findMethod(
                            activity.getClass(), "o0ooo0oo", void.class, false, enumClass, Runnable.class);
                    if (switchMethod != null) {
                        switchMethod.invoke(activity, brushEnum, null);
                    }
                }
            } catch (Throwable ignored) {}

            android.util.Log.d("UiUxController", "Selection/Lasso successfully dismissed, tool switched to Brush.");
        } catch (Throwable ignored) {}
    }

    /** Finds the left toolbar item (item_paint_left_setting) whose tag model type matches typeName. */
    private static View findLeftToolItem(View root, String typeName) {
        if (root == null) return null;
        try {
            int itemId = LibreResourceResolver.resolveViewId(
                    root.getContext(), NAME_ITEM_PAINT_LEFT_SETTING, FALLBACK_ITEM_PAINT_LEFT_SETTING);
            if (itemId != 0 && root.getId() == itemId) {
                if (typeName == null) return root;
                Object tag = root.getTag();
                if (tag != null) {
                    Method m = LibreResourceResolver.findMethod(tag.getClass(), "getType", null, false);
                    Object type = m != null ? m.invoke(tag) : tag.getClass().getMethod("getType").invoke(tag);
                    if (type != null && typeName.equals(type.toString())) return root;
                }
            }
            if (root instanceof ViewGroup) {
                ViewGroup g = (ViewGroup) root;
                for (int i = 0; i < g.getChildCount(); i++) {
                    View r = findLeftToolItem(g.getChildAt(i), typeName);
                    if (r != null) return r;
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static void clearGlSelection(final Object paintView, final int mineKey, final int userId) {
        if (paintView == null) return;
        try {
            Method queueEvent = paintView.getClass().getMethod("queueEvent", Runnable.class);
            queueEvent.invoke(paintView, new Runnable() {
                @Override
                public void run() {
                    try {
                        Method clearMethod = paintView.getClass().getMethod("clearSelectionArea", int.class, int.class, boolean.class, boolean.class, boolean.class);
                        clearMethod.invoke(paintView, mineKey, userId, false, true, true);
                    } catch (Throwable ignored) {}
                }
            });
        } catch (Throwable ignored) {}
    }

    /**
     * Purges domestic Chinese social media platforms (QQ, WeChat, QZone, WechatMoments)
     * from all export flows, preserving only Album, local File, and system sharesheet.
     */
    public static void purgeSocialMediaExportPlatforms(java.util.ArrayList<?> list) {
        if (list == null) return;
        try {
            java.util.Iterator<?> it = list.iterator();
            while (it.hasNext()) {
                Object item = it.next();
                if (item != null) {
                    String clsName = item.getClass().getName();
                    if (clsName.contains("PlatformInfo")) {
                        it.remove();
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    private static void disallowParentInterceptRecursive(View v) {
        if (v == null) return;
        if (v instanceof android.widget.SeekBar || v.getClass().getName().contains("Seekbar")) {
            v.setOnTouchListener((view, event) -> {
                int action = event.getActionMasked();
                if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
                    ViewParent p = view.getParent();
                    if (p != null) p.requestDisallowInterceptTouchEvent(true);
                } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    ViewParent p = view.getParent();
                    if (p != null) p.requestDisallowInterceptTouchEvent(false);
                }
                return false;
            });
        }
        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++) {
                disallowParentInterceptRecursive(vg.getChildAt(i));
            }
        }
    }

    /**
     * Floating Brush Sliders Setup:
     * Hooked at the end of PaintActivity.initView()V.
     * Injects a dedicated Drag Handle at index 0 of the container.
     * Attaches touch listener EXCLUSIVELY to this handle to guarantee zero interference
     * with the seekbars (CustomSeekbar / BrushWidthSeekbar) below it.
     */
    public static void setupFloatingSliders(final Activity activity, final View sliderContainer) {
        if (activity == null || activity.isFinishing()) return;
        attachFloatingSlidersWithRetry(activity, sliderContainer, 0);
    }

    private static void attachFloatingSlidersWithRetry(
            final Activity activity, final View sliderContainer, final int attempt) {
        if (activity == null || activity.isFinishing()) return;
        try {
            LibreConfig.init(activity);
            if (!LibreConfig.isFeatureActive(LibreConfig.KEY_FLOATING_BRUSH_SLIDERS, false)) {
                return;
            }
            final LibreConfig config = LibreConfig.get(activity);

            final ViewGroup container = resolveSliderContainer(activity, sliderContainer);
            if (container == null) {
                if (attempt < 10 && activity.getWindow() != null && activity.getWindow().getDecorView() != null) {
                    activity.getWindow().getDecorView().postDelayed(
                            () -> attachFloatingSlidersWithRetry(activity, sliderContainer, attempt + 1),
                            100L);
                }
                return;
            }

            if (container.findViewWithTag(TAG_DRAG_HANDLE) != null) return;

            // Create dedicated Drag Handle Bar
            final LinearLayout dragHandle = new LinearLayout(activity);
            dragHandle.setTag(TAG_DRAG_HANDLE);
            dragHandle.setContentDescription(TAG_DRAG_HANDLE);
            dragHandle.setOrientation(LinearLayout.HORIZONTAL);
            dragHandle.setGravity(Gravity.CENTER);
            dragHandle.setClickable(true);
            final int handleHeight = dpToPx(activity, 24);
            final int handleBottomMargin = dpToPx(activity, 2);

            GradientDrawable handleBg = new GradientDrawable();
            handleBg.setColor(Color.parseColor("#992B2B2E"));
            float cardRadius = dpToPx(activity, 8);
            handleBg.setCornerRadii(new float[]{cardRadius, cardRadius, cardRadius, cardRadius, 0f, 0f, 0f, 0f});
            dragHandle.setBackground(handleBg);

            // Grip indicator graphic
            View gripPill = new View(activity);
            int pillWidth = dpToPx(activity, 16);
            int pillHeight = dpToPx(activity, 3);
            LinearLayout.LayoutParams pillParams = new LinearLayout.LayoutParams(pillWidth, pillHeight);
            gripPill.setLayoutParams(pillParams);

            GradientDrawable pillBg = new GradientDrawable();
            pillBg.setColor(Color.parseColor("#E6F0F4F8"));
            pillBg.setCornerRadius(pillHeight / 2.0f);
            gripPill.setBackground(pillBg);
            dragHandle.addView(gripPill);

            // Attach drag gesture ONLY to the handle
            dragHandle.setOnTouchListener(new View.OnTouchListener() {
                private float dX, dY;
                private boolean isDragging = false;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            ViewParent p = container.getParent();
                            if (p != null) p.requestDisallowInterceptTouchEvent(true);
                            dX = container.getX() - event.getRawX();
                            dY = container.getY() - event.getRawY();
                            isDragging = true;
                            return true;

                        case MotionEvent.ACTION_MOVE:
                            if (isDragging) {
                                float newX = event.getRawX() + dX;
                                float newY = event.getRawY() + dY;
                                if (container.getParent() instanceof View) {
                                    View parent = (View) container.getParent();
                                    float maxX = Math.max(0f, parent.getWidth() - container.getWidth());
                                    float maxY = Math.max(0f, parent.getHeight() - container.getHeight());
                                    if (newX < 0f) newX = 0f;
                                    if (newY < 0f) newY = 0f;
                                    if (maxX > 0f && newX > maxX) newX = maxX;
                                    if (maxY > 0f && newY > maxY) newY = maxY;
                                }
                                container.setX(newX);
                                container.setY(newY);
                                return true;
                            }
                            break;

                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            if (isDragging) {
                                ViewParent pUp = container.getParent();
                                if (pUp != null) pUp.requestDisallowInterceptTouchEvent(false);
                                config.setSliderPos(container.getX(), container.getY());
                                isDragging = false;
                                return true;
                            }
                            break;
                    }
                    return false;
                }
            });

            if (container instanceof LinearLayout) {
                LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams(
                        dpToPx(activity, 26),
                        handleHeight
                );
                handleParams.gravity = Gravity.CENTER_HORIZONTAL;
                handleParams.setMargins(0, 0, 0, handleBottomMargin);
                dragHandle.setLayoutParams(handleParams);
                container.addView(dragHandle, 0);
            } else {
                container.setPadding(
                        container.getPaddingLeft(),
                        container.getPaddingTop() + handleHeight,
                        container.getPaddingRight(),
                        container.getPaddingBottom());
                ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        handleHeight);
                lp.topMargin = -handleHeight;
                dragHandle.setLayoutParams(lp);
                container.addView(dragHandle);
            }

            // Promote container out of narrow 96px left sidebar ONLY after rl_left_setting_root lays it out at bottom-left
            container.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                private boolean mPromoted = false;

                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                           int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    if (mPromoted) {
                        container.removeOnLayoutChangeListener(this);
                        return;
                    }
                    int w = right - left;
                    int h = bottom - top;
                    if (w <= 0 || h <= 0) return;

                    final ViewGroup fullRoot = resolveFullCanvasRoot(activity);
                    if (fullRoot == null || fullRoot.getHeight() <= 0) return;

                    int[] cLoc = new int[2];
                    int[] rLoc = new int[2];
                    container.getLocationInWindow(cLoc);
                    fullRoot.getLocationInWindow(rLoc);
                    if ((cLoc[1] - rLoc[1]) < Math.max(400, fullRoot.getHeight() / 2)) {
                        return;
                    }

                    mPromoted = true;
                    container.removeOnLayoutChangeListener(this);
                    final int finalW = w;
                    final int startX = Math.max(0, cLoc[0] - rLoc[0]);
                    final int startY = Math.max(0, cLoc[1] - rLoc[1]);

                    container.post(() -> {
                        try {
                            if (container.getParent() != fullRoot && container.getParent() instanceof ViewGroup) {
                                final View origParent = (View) container.getParent();
                                ((ViewGroup) origParent).removeView(container);
                                ViewGroup.MarginLayoutParams freeLp = (fullRoot instanceof android.widget.RelativeLayout)
                                        ? new android.widget.RelativeLayout.LayoutParams(finalW, ViewGroup.LayoutParams.WRAP_CONTENT)
                                        : new android.widget.FrameLayout.LayoutParams(finalW, ViewGroup.LayoutParams.WRAP_CONTENT);
                                container.setLayoutParams(freeLp);
                                fullRoot.addView(container);

                                float savedX = config.getSliderPosX(-1f);
                                float savedY = config.getSliderPosY(-1f);
                                float maxX = Math.max(0f, fullRoot.getWidth() - finalW);
                                float maxY = Math.max(0f, fullRoot.getHeight() - dpToPx(activity, 240));
                                float targetX;
                                float targetY;
                                if (savedX >= 0f && savedY >= 0f) {
                                    targetX = Math.min(Math.max(0f, savedX), maxX > 0f ? maxX : savedX);
                                    targetY = Math.min(Math.max(0f, savedY), maxY > 0f ? maxY : savedY);
                                } else {
                                    targetX = Math.min(startX, maxX > 0f ? maxX : startX);
                                    targetY = Math.min(startY, maxY > 0f ? maxY : startY);
                                }
                                container.setTranslationX(targetX);
                                container.setTranslationY(targetY);

                                final int initVis = origParent.getVisibility();
                                if (container.getVisibility() != initVis) {
                                    container.setVisibility(initVis);
                                }
                                origParent.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
                                    private int mLastOrigVis = initVis;

                                    @Override
                                    public void onGlobalLayout() {
                                        int targetVis = origParent.getVisibility();
                                        if (targetVis != mLastOrigVis) {
                                            mLastOrigVis = targetVis;
                                            if (container.getVisibility() != targetVis) {
                                                container.setVisibility(targetVis);
                                            }
                                        }
                                    }
                                });
                            }
                        } catch (Throwable ignored) {}
                    });
                }
            });

            // Prevent inner slider touch events from conflicting with outer container dragging
            disallowParentInterceptRecursive(container);

            android.util.Log.i("UiUxController", "Attached floating slider handle to "
                    + container.getClass().getSimpleName() + " id=" + container.getId());
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static ViewGroup resolveFullCanvasRoot(Activity activity) {
        try {
            View v = LibreResourceResolver.findViewByName(activity, "paint_root_view", 0);
            if (v instanceof android.widget.FrameLayout || v instanceof android.widget.RelativeLayout) {
                return (ViewGroup) v;
            }
            View content = activity.findViewById(android.R.id.content);
            if (content instanceof ViewGroup) {
                return (ViewGroup) content;
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static ViewGroup resolveSliderContainer(Activity activity, View candidate) {
        try {
            View found = LibreResourceResolver.findViewByName(activity, "seekbars_view", 0);
            if (found instanceof ViewGroup) {
                return (ViewGroup) found;
            }
        } catch (Throwable ignored) {}
        if (candidate instanceof ViewGroup) {
            return (ViewGroup) candidate;
        }
        return null;
    }

    /**
     * Fallback Continuous Matrix Rotation Math for 2-finger touch gestures.
     * Midpoint pivot calculation with seamless delta tracking.
     */
    public static class RotationHelper {
        private float lastAngle = 0f;
        private boolean isRotating = false;

        public void onTouch(Matrix matrix, MotionEvent event, float viewWidth, float viewHeight) {
            if (matrix == null || event == null) return;

            int action = event.getActionMasked();
            if (event.getPointerCount() >= 2) {
                float x0 = event.getX(0);
                float y0 = event.getY(0);
                float x1 = event.getX(1);
                float y1 = event.getY(1);

                // Pivot is the midpoint between both fingers
                float px = (x0 + x1) / 2.0f;
                float py = (y0 + y1) / 2.0f;

                float deltaX = x1 - x0;
                float deltaY = y1 - y0;
                float currentAngle = (float) Math.toDegrees(Math.atan2(deltaY, deltaX));

                if (action == MotionEvent.ACTION_POINTER_DOWN || !isRotating) {
                    lastAngle = currentAngle;
                    isRotating = true;
                } else if (action == MotionEvent.ACTION_MOVE && isRotating) {
                    float deltaAngle = currentAngle - lastAngle;
                    // Normalize delta across -180 .. 180 boundary
                    if (deltaAngle > 180f) deltaAngle -= 360f;
                    if (deltaAngle < -180f) deltaAngle += 360f;

                    matrix.postRotate(deltaAngle, px, py);
                    lastAngle = currentAngle;
                }
            } else {
                isRotating = false;
            }
        }
    }

    public static File getFontsDirectory(Context context) {
        return UIFontManager.getFontsDirectory(context);
    }

    public static List<File> getAvailableFontFiles(Context context) {
        return UIFontManager.getAvailableFontFiles(context);
    }

    private static int dpToPx(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}

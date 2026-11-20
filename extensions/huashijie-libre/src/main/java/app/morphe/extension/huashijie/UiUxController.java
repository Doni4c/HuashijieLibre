package app.morphe.extension.huashijie;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowInsets;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.lang.ref.WeakReference;
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
    private static final int TAG_SLIDER_LAYOUT_LISTENER = 0x7e098640;
    private static final int TAG_PLS_COMPACTED = 0x7e098642;
    private static final int TAG_PLS_ORIG_W = 0x7e098643;
    private static final int TAG_PLS_ORIG_MIN_W = 0x7e098644;
    private static final int TAG_PLS_ORIG_PAD_R = 0x7e098645;
    private static final int TAG_PLS_LAYOUT_HOOKED = 0x7e098646;
    private static final int TAG_PLS_ORIG_PAD_L = 0x7e098647;
    private static final int TAG_SLIDER_SELF_LISTENER = 0x7e098648;
    private static final int TAG_SLIDER_VIS_LISTENER = 0x7e098649;
    private static final float REF_ROTATION_DEADZONE_DEG = 8.0f;

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
    private static volatile Method sGetPhotoTypeMethod = null;
    private static volatile Method sGetRealCoordMethod = null;
    private static volatile Field sPaintParamsField = null;
    private static volatile Field sParamsSparseField = null;
    private static volatile Field sSelectionRectField = null;
    private static volatile boolean sOutCloseHasTypeResolved = false;
    private static volatile Method sOutCloseHasTypeMethod = null;
    private static volatile boolean sXuanQuFieldsResolved = false;
    private static volatile Field sXuanQuView1Field = null;
    private static volatile Field sXuanQuView2Field = null;

    private static WeakReference<ViewGroup> sPromotedSliderRef = null;
    private static WeakReference<ViewGroup> sPromotedOrigParentRef = null;
    private static WeakReference<ViewGroup> sPromotedFullRootRef = null;
    private static ViewGroup.LayoutParams sPromotedOrigLp = null;
    private static int sPromotedOrigIndex = 0;
    private static int sPromotedWidth = 0;
    private static int sPromotedHeight = 0;
    private static float sPromotedStartX = -1f;
    private static float sPromotedStartY = -1f;
    private static volatile boolean sSliderTempRestored = false;
    private static volatile boolean sSliderDragging = false;

    private static volatile boolean sRefRotFieldsResolved = false;
    private static volatile Field sRefRunNavField = null;
    private static volatile Field sRefRunGestureField = null;
    private static volatile Field sRefRunX0Field = null;
    private static volatile Field sRefRunY0Field = null;
    private static volatile Field sRefRunX1Field = null;
    private static volatile Field sRefRunY1Field = null;
    private static volatile Field sNavRenderTypeField = null;
    private static WeakReference<Object> sLastRefGestureRef = new WeakReference<>(null);
    private static boolean sRefRotUnlocked = false;
    private static float sRefRotAccum = 0f;
    private static float sRefRotLastAngle = 0f;

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
        Class<?> paintViewClass = null;
        try {
            paintViewClass = Class.forName("net.huanci.paintlib.views.PaintView");
        } catch (Throwable ignored) {}
        sPaintViewField = paintViewClass != null
                ? LibreResourceResolver.findField(actClass, "o0000O0", paintViewClass, null)
                : LibreResourceResolver.findField(actClass, "o0000O0", null, "PaintView");
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

                if (!isTouchOutsideDismissEnabled(activity)) {
                    return;
                }

                int tapX = (int) sTouchDownX;
                int tapY = (int) sTouchDownY;

                ensureActivityReflectionCache(activity);

                View moveView = getCachedActivityView(activity, sMoveViewField);
                boolean moveActive = (moveView != null && moveView.getVisibility() == View.VISIBLE);
                if (moveActive) {
                    return;
                }

                View outCloseRaw = getCachedActivityView(activity, sOutCloseField);
                ViewGroup outCloseView = (outCloseRaw instanceof ViewGroup) ? (ViewGroup) outCloseRaw : null;
                boolean outCloseActive = isOutCloseSelectionActive(outCloseView);

                View xuanQuView = getCachedActivityView(activity, sXuanQuField);
                boolean xuanQuActive = (xuanQuView != null && xuanQuView.getVisibility() == View.VISIBLE);

                View leftSetting = null;
                Object leftViewObj = getCachedActivityView(activity, sLeftViewField);
                if (leftViewObj == null && sLeftViewField == null) {
                    leftSetting = LibreResourceResolver.findViewByName(activity, NAME_LEFT_SETTING, FALLBACK_LEFT_SETTING);
                    leftViewObj = leftSetting;
                }
                boolean isLeftToolSelection = checkLeftToolSelection(leftViewObj);

                if (!outCloseActive && !xuanQuActive && !isLeftToolSelection) {
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

                // 4. Check active XuanQuSettingView (view1, view2, and visible control buttons)
                int screenHeight = activity.getResources().getDisplayMetrics().heightPixels;
                if (xuanQuActive) {
                    if (!sXuanQuFieldsResolved) {
                        sXuanQuView1Field = LibreResourceResolver.findField(xuanQuView.getClass(), "view1", null, null);
                        sXuanQuView2Field = LibreResourceResolver.findField(xuanQuView.getClass(), "view2", null, null);
                        sXuanQuFieldsResolved = true;
                    }
                    if (isTapInsideBoundedPanel(getFieldView(xuanQuView, sXuanQuView1Field), tapX, tapY, screenHeight)) {
                        return;
                    }
                    if (isTapInsideBoundedPanel(getFieldView(xuanQuView, sXuanQuView2Field), tapX, tapY, screenHeight)) {
                        return;
                    }
                    if (xuanQuView instanceof ViewGroup) {
                        ViewGroup xqGroup = (ViewGroup) xuanQuView;
                        for (int ci = 0; ci < xqGroup.getChildCount(); ci++) {
                            if (isTapInsideBoundedPanel(xqGroup.getChildAt(ci), tapX, tapY, screenHeight)) {
                                return;
                            }
                        }
                    }
                }

                // 5. Verify PaintView is not placing a photo, HAS an active selection area,
                // and tap is outside the selection bounding box.
                boolean hasActiveSelectionArea = false;
                try {
                    Object pv = sPaintViewField != null ? sPaintViewField.get(activity) : null;
                    if (pv != null) {
                        if (!sPaintViewMembersResolved) {
                            sMineKeyField = LibreResourceResolver.findField(pv.getClass(), "MINE_KEY", null, null);
                            sIsSelectEmptyMethod = LibreResourceResolver.findMethod(
                                    pv.getClass(), "isLayerSelectAreaEmpty", boolean.class, false, int.class);
                            sGetPhotoTypeMethod = LibreResourceResolver.findMethod(
                                    pv.getClass(), "getPhotoType", int.class, false);
                            sGetRealCoordMethod = LibreResourceResolver.findMethod(
                                    pv.getClass(), "getRealCoordXY", float[].class, false, float.class, float.class);
                            sPaintParamsField = LibreResourceResolver.findField(
                                    pv.getClass(), "mPaintParams", null, "PAINT_PARAMS");
                            sPaintViewMembersResolved = true;
                        }
                        if (sGetPhotoTypeMethod != null) {
                            int photoType = (Integer) sGetPhotoTypeMethod.invoke(pv);
                            if (photoType != 0) {
                                return;
                            }
                        }
                        if (sMineKeyField != null && sIsSelectEmptyMethod != null) {
                            int mineKey = sMineKeyField.getInt(pv);
                            boolean isEmpty = (Boolean) sIsSelectEmptyMethod.invoke(pv, mineKey);
                            if (!isEmpty) {
                                hasActiveSelectionArea = true;
                            }
                            if (hasActiveSelectionArea && isTapInsideSelectionArea(pv, mineKey, tapX, tapY)) {
                                return;
                            }
                        }
                    }
                } catch (Throwable ignored) {}

                if (!hasActiveSelectionArea) {
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

    private static boolean isTapInsideSelectionArea(Object pv, int mineKey, int tapX, int tapY) {
        if (!(pv instanceof View) || sPaintParamsField == null || sGetRealCoordMethod == null) {
            return false;
        }
        try {
            Object paintParams = sPaintParamsField.get(pv);
            if (paintParams == null) return false;
            if (sParamsSparseField == null) {
                sParamsSparseField = LibreResourceResolver.findField(
                        paintParams.getClass(), "OooO00o", android.util.SparseArray.class, null);
            }
            if (sParamsSparseField == null) return false;
            Object sparseObj = sParamsSparseField.get(paintParams);
            if (!(sparseObj instanceof android.util.SparseArray)) return false;
            Object stateObj = ((android.util.SparseArray<?>) sparseObj).get(mineKey);
            if (stateObj == null) return false;
            if (sSelectionRectField == null) {
                sSelectionRectField = LibreResourceResolver.findField(stateObj.getClass(), "OooOo0", Rect.class, null);
            }
            if (sSelectionRectField == null) return false;
            Object rectObj = sSelectionRectField.get(stateObj);
            if (!(rectObj instanceof Rect)) return false;
            Rect selRect = (Rect) rectObj;
            if (selRect.isEmpty() || selRect.width() <= 0 || selRect.height() <= 0) {
                return false;
            }
            int[] pvLoc = new int[2];
            ((View) pv).getLocationOnScreen(pvLoc);
            Object ptObj = sGetRealCoordMethod.invoke(pv, (float) (tapX - pvLoc[0]), (float) (tapY - pvLoc[1]));
            if (ptObj instanceof float[]) {
                float[] pt = (float[]) ptObj;
                if (pt.length >= 2) {
                    return selRect.contains(Math.round(pt[0]), Math.round(pt[1]));
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private static boolean isOutCloseSelectionActive(ViewGroup outCloseView) {
        if (outCloseView == null || outCloseView.getVisibility() != View.VISIBLE || outCloseView.getChildCount() <= 0) {
            return false;
        }
        try {
            if (!sOutCloseHasTypeResolved) {
                sOutCloseHasTypeMethod = LibreResourceResolver.findMethod(
                        outCloseView.getClass(), "hasType", boolean.class, false, int.class);
                sOutCloseHasTypeResolved = true;
            }
            if (sOutCloseHasTypeMethod != null) {
                return Boolean.TRUE.equals(sOutCloseHasTypeMethod.invoke(outCloseView, 1));
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private static View getFieldView(Object host, Field f) {
        if (host == null || f == null) return null;
        try {
            Object v = f.get(host);
            return v instanceof View ? (View) v : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean isTapInsideBoundedPanel(View panel, int tapX, int tapY, int screenHeight) {
        if (panel == null || panel.getVisibility() != View.VISIBLE) return false;
        Rect r = new Rect();
        panel.getGlobalVisibleRect(r);
        return r.width() > 0
                && r.height() > 0
                && r.height() < screenHeight * 0.7f
                && r.contains(tapX, tapY);
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
            // 1. Remove only the XUAN_QU (1) close button without touching other active tool buttons
            if (outCloseView != null) {
                try {
                    Method removeBtn = LibreResourceResolver.findMethod(
                            outCloseView.getClass(), "removeCloseBtn", void.class, false, int.class);
                    if (removeBtn != null) {
                        removeBtn.invoke(outCloseView, 1);
                    }
                } catch (Throwable ignored) {}
                try {
                    if (outCloseView.getChildCount() == 0) {
                        outCloseView.setVisibility(View.GONE);
                    }
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

            // 4. Clear selection area in PaintView
            try {
                ensureActivityReflectionCache(activity);
                Field pvField = sPaintViewField;
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
        sPromotedSliderRef = null;
        sPromotedOrigParentRef = null;
        sPromotedFullRootRef = null;
        sPromotedOrigLp = null;
        sPromotedOrigIndex = 0;
        sPromotedWidth = 0;
        sPromotedHeight = 0;
        sPromotedStartX = -1f;
        sPromotedStartY = -1f;
        sSliderTempRestored = false;
        attachFloatingSlidersWithRetry(activity, sliderContainer, 0);
    }

    private static boolean isViewDescendantOf(View ancestor, View child) {
        if (ancestor == null || child == null) return false;
        if (ancestor == child) return true;
        ViewParent p = child.getParent();
        while (p instanceof View) {
            if (p == ancestor) return true;
            p = p.getParent();
        }
        return false;
    }

    private static ViewGroup.LayoutParams ensureCompatibleLayoutParams(
            ViewGroup parent, ViewGroup.LayoutParams candidate) {
        if (parent instanceof LinearLayout) {
            if (candidate instanceof LinearLayout.LayoutParams) return candidate;
            int w = candidate != null ? candidate.width : ViewGroup.LayoutParams.WRAP_CONTENT;
            int h = candidate != null ? candidate.height : ViewGroup.LayoutParams.WRAP_CONTENT;
            return new LinearLayout.LayoutParams(w, h);
        }
        if (parent instanceof android.widget.RelativeLayout) {
            if (candidate instanceof android.widget.RelativeLayout.LayoutParams) return candidate;
            int w = candidate != null ? candidate.width : ViewGroup.LayoutParams.WRAP_CONTENT;
            int h = candidate != null ? candidate.height : ViewGroup.LayoutParams.WRAP_CONTENT;
            return new android.widget.RelativeLayout.LayoutParams(w, h);
        }
        return candidate != null
                ? candidate
                : new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private static void restorePromotedViewToParent(
            ViewGroup slider, ViewGroup origParent, int origIndex, ViewGroup.LayoutParams origLp) {
        if (slider == null || origParent == null) return;
        View handle = slider.findViewWithTag(TAG_DRAG_HANDLE);
        if (handle != null && handle.getVisibility() != View.GONE) {
            handle.setVisibility(View.GONE);
        }
        if (slider.getParent() instanceof ViewGroup) {
            ((ViewGroup) slider.getParent()).removeView(slider);
        }
        slider.setTranslationX(0f);
        slider.setTranslationY(0f);
        if (Build.VERSION.SDK_INT >= 29) {
            try {
                slider.setSystemGestureExclusionRects(java.util.Collections.<Rect>emptyList());
            } catch (Throwable ignored) {}
        }
        int insertIdx = Math.max(0, Math.min(origIndex, origParent.getChildCount()));
        ViewGroup.LayoutParams safeLp = ensureCompatibleLayoutParams(origParent, origLp);
        origParent.addView(slider, insertIdx, safeLp);
    }

    /**
     * Temporarily reattaches the promoted seekbars_view back to its original PaintLeftSettingView
     * parent before setSettingViewLocation()V executes so layout height logic completes intact.
     */
    public static void onBeforeLeftSettingLayout(Object leftSettingView) {
        ViewGroup pls = null;
        try {
            ViewGroup fullRootPre = sPromotedFullRootRef != null ? sPromotedFullRootRef.get() : null;
            pls = resolvePaintLeftSettingView(leftSettingView, fullRootPre);
            restorePaintLeftSettingOrigin(pls);
        } catch (Throwable ignored) {}
        try {
            ViewGroup slider = sPromotedSliderRef != null ? sPromotedSliderRef.get() : null;
            ViewGroup origParent = sPromotedOrigParentRef != null ? sPromotedOrigParentRef.get() : null;
            ViewGroup fullRoot = sPromotedFullRootRef != null ? sPromotedFullRootRef.get() : null;
            if (!sSliderTempRestored && slider != null && origParent != null && fullRoot != null) {
                boolean sameTree = pls == null || (slider.getRootView() == pls.getRootView() && isViewDescendantOf(pls, origParent));
                if (sameTree && slider.getParent() == fullRoot) {
                    restorePromotedViewToParent(slider, origParent, sPromotedOrigIndex, sPromotedOrigLp);
                    sSliderTempRestored = true;
                }
            }

            // Unconditional rescue: guarantee seekbars_view and rl_left_setting_root_seekbar live inside pls
            // before setSettingViewLocation() runs findViewById() regardless of prior state transitions.
            if (pls != null) {
                View searchRoot = pls.getRootView();
                if (searchRoot == null) searchRoot = fullRoot;

                View sbLocal = LibreResourceResolver.findViewByName(pls, "seekbars_view", 0);
                if (sbLocal == null && searchRoot != null) {
                    View sbRemote = LibreResourceResolver.findViewByName(searchRoot, "seekbars_view", 0);
                    View rlRoot = LibreResourceResolver.findViewByName(pls, "rl_left_setting_root", 0);
                    if (sbRemote instanceof ViewGroup && rlRoot instanceof ViewGroup && sbRemote != pls) {
                        ViewGroup.LayoutParams lpToUse = (slider == sbRemote && origParent == rlRoot) ? sPromotedOrigLp : null;
                        int idxToUse = (slider == sbRemote && origParent == rlRoot) ? sPromotedOrigIndex : ((ViewGroup) rlRoot).getChildCount();
                        restorePromotedViewToParent((ViewGroup) sbRemote, (ViewGroup) rlRoot, idxToUse, lpToUse);
                        sPromotedSliderRef = new WeakReference<>((ViewGroup) sbRemote);
                        sPromotedOrigParentRef = new WeakReference<>((ViewGroup) rlRoot);
                        if (searchRoot instanceof ViewGroup) {
                            ViewGroup resolvedRoot = LibreResourceResolver.findViewByName(searchRoot, "paint_root_view", 0) instanceof ViewGroup
                                    ? (ViewGroup) LibreResourceResolver.findViewByName(searchRoot, "paint_root_view", 0)
                                    : fullRoot;
                            if (resolvedRoot != null) {
                                sPromotedFullRootRef = new WeakReference<>(resolvedRoot);
                            }
                        }
                        sSliderTempRestored = true;
                    }
                }

                View landSbLocal = LibreResourceResolver.findViewByName(pls, "rl_left_setting_root_seekbar", 0);
                if (landSbLocal == null && searchRoot != null) {
                    View landSbRemote = LibreResourceResolver.findViewByName(searchRoot, "rl_left_setting_root_seekbar", 0);
                    if (landSbRemote instanceof ViewGroup && landSbRemote != pls) {
                        ViewGroup.LayoutParams lpToUse = (slider == landSbRemote && origParent == pls) ? sPromotedOrigLp : null;
                        int idxToUse = (slider == landSbRemote && origParent == pls) ? sPromotedOrigIndex : pls.getChildCount();
                        restorePromotedViewToParent((ViewGroup) landSbRemote, pls, idxToUse, lpToUse);
                        sPromotedSliderRef = new WeakReference<>((ViewGroup) landSbRemote);
                        sPromotedOrigParentRef = new WeakReference<>(pls);
                        sSliderTempRestored = true;
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    private static void ensureHandleBeforeOpacity(ViewGroup container, View handle, View opacityFl) {
        if (container == null || handle == null || opacityFl == null || opacityFl.getParent() != container) {
            return;
        }
        if (handle.getParent() instanceof ViewGroup && handle.getParent() != container) {
            ((ViewGroup) handle.getParent()).removeView(handle);
            int opIdx = container.indexOfChild(opacityFl);
            container.addView(handle, opIdx >= 0 ? opIdx : Math.min(1, container.getChildCount()));
        } else if (handle.getParent() == container) {
            int opIdx = container.indexOfChild(opacityFl);
            int hIdx = container.indexOfChild(handle);
            if (opIdx >= 0 && hIdx != opIdx - 1) {
                if (hIdx >= 0) {
                    container.removeView(handle);
                    opIdx = container.indexOfChild(opacityFl);
                }
                container.addView(handle, Math.max(0, opIdx));
            }
        } else {
            int opIdx = container.indexOfChild(opacityFl);
            container.addView(handle, opIdx >= 0 ? opIdx : Math.min(1, container.getChildCount()));
        }
        if (handle.getVisibility() != View.VISIBLE) {
            handle.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Promotes seekbars container back into fullRoot immediately after setSettingViewLocation()V
     * finishes, migrates handle if orientation switched containers (seekbars_view <-> rl_left_setting_root_seekbar),
     * ensures handle sits at index 1 between brush_seekbar_fl and opacity_seekbar_fl, and clamps bounds.
     */
    public static void onAfterLeftSettingLayout(Object leftSettingView) {
        boolean wasTempRestored = sSliderTempRestored;
        sSliderTempRestored = false;
        try {
            ViewGroup slider = sPromotedSliderRef != null ? sPromotedSliderRef.get() : null;
            ViewGroup origParent = sPromotedOrigParentRef != null ? sPromotedOrigParentRef.get() : null;
            ViewGroup fullRoot = sPromotedFullRootRef != null ? sPromotedFullRootRef.get() : null;
            if (!wasTempRestored || slider == null || fullRoot == null) {
                if (leftSettingView instanceof View) {
                    View searchView = (View) leftSettingView;
                    View brushPre = LibreResourceResolver.findViewByName(searchView, "brush_seekbar_fl", 0);
                    View opacityPre = LibreResourceResolver.findViewByName(searchView, "opacity_seekbar_fl", 0);
                    if (brushPre != null && opacityPre != null
                            && opacityPre.getParent() instanceof ViewGroup
                            && brushPre.getParent() == opacityPre.getParent()) {
                        ensureHandleBeforeOpacity((ViewGroup) opacityPre.getParent(), searchView.findViewWithTag(TAG_DRAG_HANDLE), opacityPre);
                    }
                    if (slider != null && fullRoot != null && slider.getParent() == fullRoot) {
                        syncPaintLeftSettingCompactWidth(searchView.getContext(), leftSettingView, fullRoot);
                    }
                }
                return;
            }

            View handle = slider.findViewWithTag(TAG_DRAG_HANDLE);
            if (handle == null) {
                handle = fullRoot.findViewWithTag(TAG_DRAG_HANDLE);
            }

            View searchRoot = (leftSettingView instanceof View) ? (View) leftSettingView : fullRoot;
            View brushFl = LibreResourceResolver.findViewByName(searchRoot, "brush_seekbar_fl", 0);
            View opacityFl = LibreResourceResolver.findViewByName(searchRoot, "opacity_seekbar_fl", 0);
            if (brushFl == null || opacityFl == null) {
                brushFl = LibreResourceResolver.findViewByName(fullRoot, "brush_seekbar_fl", 0);
                opacityFl = LibreResourceResolver.findViewByName(fullRoot, "opacity_seekbar_fl", 0);
            }

            if (brushFl != null && opacityFl != null
                    && opacityFl.getParent() instanceof ViewGroup
                    && brushFl.getParent() == opacityFl.getParent()) {
                ViewGroup activeContainer = (ViewGroup) opacityFl.getParent();
                if (activeContainer != slider && activeContainer.getParent() instanceof ViewGroup) {
                    if (handle != null && handle.getParent() instanceof ViewGroup) {
                        ((ViewGroup) handle.getParent()).removeView(handle);
                    }
                    if (origParent != null && slider.getParent() != origParent) {
                        restorePromotedViewToParent(slider, origParent, sPromotedOrigIndex, sPromotedOrigLp);
                    }
                    ViewGroup newOrigParent = (ViewGroup) activeContainer.getParent();
                    sPromotedOrigIndex = Math.max(0, newOrigParent.indexOfChild(activeContainer));
                    sPromotedOrigLp = activeContainer.getLayoutParams();
                    if (activeContainer.getWidth() > 0) sPromotedWidth = activeContainer.getWidth();
                    if (activeContainer.getHeight() > 0) sPromotedHeight = activeContainer.getHeight();
                    sPromotedSliderRef = new WeakReference<>(activeContainer);
                    sPromotedOrigParentRef = new WeakReference<>(newOrigParent);
                    slider = activeContainer;
                    origParent = newOrigParent;
                    if (handle != null) {
                        ensureHandleBeforeOpacity(slider, handle, opacityFl);
                    }
                    disallowParentInterceptRecursive(slider);
                } else if (handle != null) {
                    ensureHandleBeforeOpacity(slider, handle, opacityFl);
                }
            }

            if (handle != null && handle.getVisibility() != View.VISIBLE) {
                handle.setVisibility(View.VISIBLE);
            }
            if (origParent != null && slider.getParent() == origParent) {
                origParent.removeView(slider);
            } else if (slider.getParent() instanceof ViewGroup && slider.getParent() != fullRoot) {
                ((ViewGroup) slider.getParent()).removeView(slider);
            }
            if (slider.getParent() != fullRoot) {
                Context ctx = fullRoot.getContext();
                int width = sPromotedWidth > 0 ? sPromotedWidth
                        : (slider.getWidth() > 0 ? slider.getWidth() : dpToPx(ctx, 48));
                slider.setLayoutParams(createFreeSliderLp(fullRoot, width));
                fullRoot.addView(slider);
            }
            hookSliderLayoutListener(slider);
            installFullRootSliderListeners(fullRoot);
            if (origParent != null && slider.getVisibility() != origParent.getVisibility()) {
                slider.setVisibility(origParent.getVisibility());
            }
            Context ctx = fullRoot.getContext();
            LibreConfig.init(ctx);
            syncPaintLeftSettingCompactWidth(ctx, leftSettingView, fullRoot);
            applyClampedNormalizedSliderPosition(ctx, fullRoot, slider, LibreConfig.get(ctx));
        } catch (Throwable ignored) {}
    }

    private static ViewGroup resolvePaintLeftSettingView(Object leftSettingView, ViewGroup fullRoot) {
        View v = null;
        if (leftSettingView instanceof View) {
            View lv = (View) leftSettingView;
            v = LibreResourceResolver.findViewByName(lv, "paint_left_setting", 0);
            if (v == null) {
                try {
                    int id = lv.getId();
                    if (id > 0 && "paint_left_setting".equals(lv.getResources().getResourceEntryName(id))) {
                        v = lv;
                    }
                } catch (Throwable ignored) {}
            }
        }
        if (v == null && fullRoot != null) {
            v = LibreResourceResolver.findViewByName(fullRoot, "paint_left_setting", 0);
        }
        return (v instanceof ViewGroup) ? (ViewGroup) v : null;
    }

    private static int getXRightRelativeTo(View target, View ancestor) {
        if (target == null || ancestor == null) return 0;
        int x = target.getLeft();
        int right = target.getRight();
        if (target.getParent() == ancestor) {
            return right;
        }
        View cur = target;
        while (cur != null && cur != ancestor) {
            x += cur.getLeft();
            ViewParent p = cur.getParent();
            if (!(p instanceof View)) break;
            cur = (View) p;
        }
        return x - target.getLeft() + target.getWidth();
    }

    private static int resolveSafeCutoutLeft(View v) {
        if (v == null || Build.VERSION.SDK_INT < 28) return 0;
        try {
            WindowInsets wi = v.getRootWindowInsets();
            if (wi != null && wi.getDisplayCutout() != null) {
                return Math.max(0, wi.getDisplayCutout().getSafeInsetLeft());
            }
        } catch (Throwable ignored) {}
        return 0;
    }

    private static void restorePaintLeftSettingOrigin(ViewGroup pls) {
        if (pls == null) return;
        try {
            if (!Boolean.TRUE.equals(pls.getTag(TAG_PLS_COMPACTED))) {
                return;
            }
            Object origW = pls.getTag(TAG_PLS_ORIG_W);
            Object origMinW = pls.getTag(TAG_PLS_ORIG_MIN_W);
            Object origPadL = pls.getTag(TAG_PLS_ORIG_PAD_L);
            Object origPadR = pls.getTag(TAG_PLS_ORIG_PAD_R);
            pls.setTag(TAG_PLS_COMPACTED, null);
            pls.setTag(TAG_PLS_ORIG_W, null);
            pls.setTag(TAG_PLS_ORIG_MIN_W, null);
            pls.setTag(TAG_PLS_ORIG_PAD_L, null);
            pls.setTag(TAG_PLS_ORIG_PAD_R, null);

            if (origMinW instanceof Integer && pls.getMinimumWidth() != (Integer) origMinW) {
                pls.setMinimumWidth((Integer) origMinW);
            }
            int targetPadL = (origPadL instanceof Integer) ? (Integer) origPadL : pls.getPaddingLeft();
            int targetPadR = (origPadR instanceof Integer) ? (Integer) origPadR : pls.getPaddingRight();
            if (pls.getPaddingLeft() != targetPadL || pls.getPaddingRight() != targetPadR) {
                pls.setPadding(targetPadL, pls.getPaddingTop(), targetPadR, pls.getPaddingBottom());
            }
            ViewGroup.LayoutParams lp = pls.getLayoutParams();
            if (lp != null && origW instanceof Integer && lp.width != (Integer) origW) {
                lp.width = (Integer) origW;
                pls.setLayoutParams(lp);
            }
        } catch (Throwable ignored) {}
    }

    private static void snapPaintLeftSettingWidthIfReady(Context ctx, final ViewGroup pls) {
        if (ctx == null || pls == null) return;
        try {
            LibreConfig.init(ctx);
            boolean active = LibreConfig.isFeatureActive(LibreConfig.KEY_FLOATING_BRUSH_SLIDERS, false);
            ViewGroup slider = sPromotedSliderRef != null ? sPromotedSliderRef.get() : null;
            ViewGroup fullRoot = sPromotedFullRootRef != null ? sPromotedFullRootRef.get() : null;
            if (!active || slider == null || fullRoot == null || slider.getParent() != fullRoot) {
                restorePaintLeftSettingOrigin(pls);
                return;
            }
            if (sSliderTempRestored) {
                return;
            }
            final int targetPadL = resolveSafeCutoutLeft(pls);
            View rlRoot = LibreResourceResolver.findViewByName(pls, "rl_left_setting_root", 0);
            if (rlRoot == null || rlRoot.getVisibility() != View.VISIBLE || rlRoot.getWidth() <= 0) {
                return;
            }
            final int exactRight = getXRightRelativeTo(rlRoot, pls);
            if (exactRight <= 0) {
                return;
            }
            ViewGroup.LayoutParams lp = pls.getLayoutParams();
            if (pls.getWidth() > exactRight + 1
                    || pls.getMinimumWidth() != 0
                    || pls.getPaddingLeft() != targetPadL
                    || pls.getPaddingRight() != 0
                    || (lp != null && lp.width != exactRight && lp.width != ViewGroup.LayoutParams.WRAP_CONTENT)) {
                pls.post(() -> {
                    try {
                        if (sSliderTempRestored) return;
                        LibreConfig.init(ctx);
                        ViewGroup s = sPromotedSliderRef != null ? sPromotedSliderRef.get() : null;
                        ViewGroup fr = sPromotedFullRootRef != null ? sPromotedFullRootRef.get() : null;
                        if (!LibreConfig.isFeatureActive(LibreConfig.KEY_FLOATING_BRUSH_SLIDERS, false)
                                || s == null || fr == null || s.getParent() != fr) {
                            restorePaintLeftSettingOrigin(pls);
                            return;
                        }
                        if (pls.getMinimumWidth() != 0) pls.setMinimumWidth(0);
                        int safePadL = resolveSafeCutoutLeft(pls);
                        if (pls.getPaddingLeft() != safePadL || pls.getPaddingRight() != 0) {
                            pls.setPadding(safePadL, pls.getPaddingTop(), 0, pls.getPaddingBottom());
                        }
                        View r = LibreResourceResolver.findViewByName(pls, "rl_left_setting_root", 0);
                        if (r != null && r.getVisibility() == View.VISIBLE && r.getWidth() > 0) {
                            int targetW = r.getWidth() + safePadL;
                            int measuredRight = getXRightRelativeTo(r, pls);
                            if (pls.getPaddingLeft() == safePadL && measuredRight > 0) {
                                targetW = measuredRight;
                            }
                            if (targetW > 0) {
                                ViewGroup.LayoutParams curLp = pls.getLayoutParams();
                                if (curLp != null && (pls.getWidth() != targetW || curLp.width != targetW)) {
                                    curLp.width = targetW;
                                    pls.setLayoutParams(curLp);
                                    if (isLandscapeRoot(fr)) {
                                        pls.post(() -> {
                                            try {
                                                if (sSliderDragging || sSliderTempRestored) return;
                                                ViewGroup s2 = sPromotedSliderRef != null ? sPromotedSliderRef.get() : null;
                                                ViewGroup fr2 = sPromotedFullRootRef != null ? sPromotedFullRootRef.get() : null;
                                                if (s2 != null && fr2 != null && s2.getParent() == fr2) {
                                                    applyClampedNormalizedSliderPosition(ctx, fr2, s2, LibreConfig.get(ctx));
                                                }
                                            } catch (Throwable ignored) {}
                                        });
                                    }
                                }
                            }
                        }
                    } catch (Throwable ignored) {}
                });
            }
        } catch (Throwable ignored) {}
    }

    private static void syncPaintLeftSettingCompactWidth(
            final Context ctx, Object leftSettingView, ViewGroup fullRoot) {
        if (ctx == null) return;
        try {
            final ViewGroup pls = resolvePaintLeftSettingView(leftSettingView, fullRoot);
            if (pls == null) return;

            LibreConfig.init(ctx);
            boolean active = LibreConfig.isFeatureActive(LibreConfig.KEY_FLOATING_BRUSH_SLIDERS, false);
            ViewGroup slider = sPromotedSliderRef != null ? sPromotedSliderRef.get() : null;
            ViewGroup fRoot = fullRoot != null ? fullRoot : (sPromotedFullRootRef != null ? sPromotedFullRootRef.get() : null);
            if (!active || slider == null || fRoot == null || slider.getParent() != fRoot) {
                restorePaintLeftSettingOrigin(pls);
                return;
            }

            ViewGroup.LayoutParams lp = pls.getLayoutParams();
            if (!Boolean.TRUE.equals(pls.getTag(TAG_PLS_COMPACTED))) {
                pls.setTag(TAG_PLS_ORIG_W, lp != null ? lp.width : ViewGroup.LayoutParams.WRAP_CONTENT);
                pls.setTag(TAG_PLS_ORIG_MIN_W, pls.getMinimumWidth());
                pls.setTag(TAG_PLS_ORIG_PAD_L, pls.getPaddingLeft());
                pls.setTag(TAG_PLS_ORIG_PAD_R, pls.getPaddingRight());
                pls.setTag(TAG_PLS_COMPACTED, Boolean.TRUE);
            }

            if (pls.getMinimumWidth() != 0) {
                pls.setMinimumWidth(0);
            }
            int safePadL = resolveSafeCutoutLeft(pls);
            if (pls.getPaddingLeft() != safePadL || pls.getPaddingRight() != 0) {
                pls.setPadding(safePadL, pls.getPaddingTop(), 0, pls.getPaddingBottom());
            }
            if (lp != null && lp.width != ViewGroup.LayoutParams.WRAP_CONTENT) {
                lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                pls.setLayoutParams(lp);
            }

            if (pls.getTag(TAG_PLS_LAYOUT_HOOKED) == null) {
                pls.setTag(TAG_PLS_LAYOUT_HOOKED, Boolean.TRUE);
                pls.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) ->
                        snapPaintLeftSettingWidthIfReady(ctx, pls));
            }
            snapPaintLeftSettingWidthIfReady(ctx, pls);
        } catch (Throwable ignored) {}
    }

    private static float clampFloat(float val, float min, float max) {
        if (val < min) return min;
        if (val > max) return max;
        return val;
    }

    private static float[] computeSliderSafeBounds(Context ctx, ViewGroup fullRoot, View slider) {
        int safeLeft = 0;
        int safeTop = 0;
        int safeRight = 0;
        int safeBottom = 0;
        if (Build.VERSION.SDK_INT >= 23) {
            try {
                WindowInsets wi = fullRoot.getRootWindowInsets();
                if (wi != null) {
                    if (Build.VERSION.SDK_INT >= 30) {
                        Insets insets = wi.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                        safeLeft = insets.left;
                        safeTop = insets.top;
                        safeRight = insets.right;
                        safeBottom = insets.bottom;
                    } else {
                        safeLeft = wi.getSystemWindowInsetLeft();
                        safeTop = wi.getSystemWindowInsetTop();
                        safeRight = wi.getSystemWindowInsetRight();
                        safeBottom = wi.getSystemWindowInsetBottom();
                    }
                }
            } catch (Throwable ignored) {}
        }
        int rootW = fullRoot.getWidth();
        int rootH = fullRoot.getHeight();
        int sliderW = slider.getWidth() > 0 ? slider.getWidth()
                : (sPromotedWidth > 0 ? sPromotedWidth : dpToPx(ctx, 44));
        int sliderH = slider.getHeight() > 0 ? slider.getHeight()
                : (sPromotedHeight > 0 ? sPromotedHeight : dpToPx(ctx, 220));
        boolean land = rootW > rootH;
        int edgePad = dpToPx(ctx, 6);
        float minX = safeLeft + edgePad;
        float barBottom = -1f;
        float barLeft = 0f;
        float barRight = 0f;
        try {
            View rootView = fullRoot.getRootView();
            if (rootView != null) {
                int[] rLoc = new int[2];
                int[] vLoc = new int[2];
                fullRoot.getLocationInWindow(rLoc);
                if (land) {
                    // Keep S/O clear of the real right edge of the left tool column (cutout included).
                    View leftCol = LibreResourceResolver.findViewByName(rootView, "rl_left_setting_root", 0);
                    if (leftCol != null && leftCol.isShown() && leftCol.getWidth() > 0) {
                        leftCol.getLocationInWindow(vLoc);
                        minX = Math.max(minX, vLoc[0] - rLoc[0] + leftCol.getWidth());
                    }
                }
                View bar = LibreResourceResolver.findViewByName(rootView, NAME_TOP_BAR, FALLBACK_TOP_BAR);
                if (bar != null && bar.isShown() && bar.getWidth() > 0 && bar.getHeight() > 0) {
                    bar.getLocationInWindow(vLoc);
                    barLeft = vLoc[0] - rLoc[0];
                    barRight = barLeft + bar.getWidth();
                    barBottom = vLoc[1] - rLoc[1] + bar.getHeight();
                }
            }
        } catch (Throwable ignored) {}
        float maxX = Math.max(minX, rootW - sliderW - safeRight - edgePad);
        int topFloorDp = land ? 8 : 36;
        float minY = Math.max(safeTop, dpToPx(ctx, topFloorDp));
        float maxY = Math.max(minY, rootH - sliderH - safeBottom);
        return new float[]{minX, maxX, minY, maxY, barBottom, barLeft, barRight, sliderW};
    }

    private static boolean isLandscapeRoot(View fullRoot) {
        return fullRoot != null && fullRoot.getWidth() > fullRoot.getHeight();
    }

    /**
     * Android 10+ edge back gesture protection: registers a system gesture exclusion rect ONLY on the
     * slider itself (never on fullRoot), and only when the slider sits within 40dp of a screen edge.
     */
    private static void updateSliderGestureExclusion(ViewGroup fullRoot, View slider) {
        if (Build.VERSION.SDK_INT < 29 || slider == null) return;
        try {
            if (fullRoot == null || slider.getParent() != fullRoot
                    || slider.getVisibility() != View.VISIBLE
                    || slider.getWidth() <= 0 || slider.getHeight() <= 0
                    || fullRoot.getWidth() <= 0) {
                slider.setSystemGestureExclusionRects(java.util.Collections.<Rect>emptyList());
                return;
            }
            Context ctx = slider.getContext();
            int edgeZone = dpToPx(ctx, 40);
            int sliderW = slider.getWidth();
            int sliderH = slider.getHeight();
            float left = slider.getX();
            float right = left + sliderW;
            boolean nearEdge = left <= edgeZone || (fullRoot.getWidth() - right) <= edgeZone;
            if (!nearEdge) {
                slider.setSystemGestureExclusionRects(java.util.Collections.<Rect>emptyList());
                return;
            }
            int centerY = sliderH / 2;
            View handle = slider.findViewWithTag(TAG_DRAG_HANDLE);
            if (handle != null && handle.getParent() == slider && handle.getHeight() > 0) {
                centerY = handle.getTop() + handle.getHeight() / 2;
            }
            int rectH = Math.min(dpToPx(ctx, 160), sliderH);
            int top = (int) clampFloat(centerY - rectH / 2f, 0f, Math.max(0, sliderH - rectH));
            int bottom = Math.min(sliderH, top + rectH);
            slider.setSystemGestureExclusionRects(
                    java.util.Collections.singletonList(new Rect(0, top, sliderW, bottom)));
        } catch (Throwable ignored) {}
    }

    /**
     * Clamps Y for a given X: when the slider horizontally overlaps the top toolbar, its top must
     * sit below the toolbar bottom, but never so low that the bottom of the slider leaves the screen.
     * bounds layout: {minX, maxX, minY, maxY, barBottom, barLeft, barRight, sliderW}.
     */
    private static float clampSliderY(float[] bounds, float x, float y) {
        float minY = bounds[2];
        float maxY = bounds[3];
        if (bounds[4] > 0f && x < bounds[6] && x + bounds[7] > bounds[5]) {
            minY = Math.max(minY, bounds[4]);
        }
        minY = Math.min(minY, maxY);
        return clampFloat(y, minY, maxY);
    }

    private static ViewGroup.LayoutParams createFreeSliderLp(ViewGroup fullRoot, int width) {
        if (fullRoot instanceof android.widget.RelativeLayout) {
            android.widget.RelativeLayout.LayoutParams lp =
                    new android.widget.RelativeLayout.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.addRule(android.widget.RelativeLayout.ALIGN_PARENT_TOP);
            lp.addRule(android.widget.RelativeLayout.ALIGN_PARENT_LEFT);
            lp.setMargins(0, 0, 0, 0);
            return lp;
        }
        android.widget.FrameLayout.LayoutParams lp =
                new android.widget.FrameLayout.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.TOP | Gravity.LEFT;
        lp.setMargins(0, 0, 0, 0);
        return lp;
    }

    /**
     * Places the slider at absolute (x, y) inside fullRoot. translation = target minus the laid-out
     * anchor (left/top), so left/top are never summed with a stale translation.
     */
    private static void placeSliderAt(ViewGroup fullRoot, View slider, float x, float y) {
        float baseL = fullRoot.getPaddingLeft();
        float baseT = fullRoot.getPaddingTop();
        if (slider.getParent() == fullRoot && slider.getWidth() > 0 && !slider.isLayoutRequested()) {
            baseL = slider.getLeft();
            baseT = slider.getTop();
        }
        slider.setTranslationX(x - baseL);
        slider.setTranslationY(y - baseT);
    }

    private static void hookSliderLayoutListener(final ViewGroup slider) {
        if (slider == null || slider.getTag(TAG_SLIDER_SELF_LISTENER) != null) return;
        slider.setTag(TAG_SLIDER_SELF_LISTENER, Boolean.TRUE);
        slider.addOnLayoutChangeListener((sv, l, t, r, b, ol, ot, or, ob) -> {
            try {
                ViewGroup cur = sPromotedSliderRef != null ? sPromotedSliderRef.get() : null;
                ViewGroup fr = sPromotedFullRootRef != null ? sPromotedFullRootRef.get() : null;
                if (sv != cur || fr == null || cur.getParent() != fr) return;
                if (sSliderTempRestored || sSliderDragging) return;
                int sw = r - l;
                int sh = b - t;
                if (sw > 0) sPromotedWidth = sw;
                if (sh > 0) sPromotedHeight = sh;
                if (sw != (or - ol) || sh != (ob - ot) || l != ol || t != ot) {
                    Context ctx = fr.getContext();
                    LibreConfig.init(ctx);
                    applyClampedNormalizedSliderPosition(ctx, fr, cur, LibreConfig.get(ctx));
                }
                updateSliderGestureExclusion(fr, cur);
            } catch (Throwable ignored) {}
        });
    }

    private static void installFullRootSliderListeners(final ViewGroup fullRoot) {
        if (fullRoot == null) return;
        if (fullRoot.getTag(TAG_SLIDER_LAYOUT_LISTENER) == null) {
            fullRoot.setTag(TAG_SLIDER_LAYOUT_LISTENER, Boolean.TRUE);
            fullRoot.addOnLayoutChangeListener((rv, l, t, r, b, ol, ot, or, ob) -> {
                try {
                    if (sSliderTempRestored || sSliderDragging) return;
                    final ViewGroup curSlider = sPromotedSliderRef != null ? sPromotedSliderRef.get() : null;
                    if (curSlider == null || curSlider.getParent() != fullRoot) return;
                    if (curSlider.getWidth() > 0) sPromotedWidth = curSlider.getWidth();
                    if (curSlider.getHeight() > 0) sPromotedHeight = curSlider.getHeight();
                    if ((r - l) != (or - ol) || (b - t) != (ob - ot)) {
                        final Context ctx = fullRoot.getContext();
                        LibreConfig.init(ctx);
                        applyClampedNormalizedSliderPosition(ctx, fullRoot, curSlider, LibreConfig.get(ctx));
                        if (curSlider.getHeight() <= 0) {
                            curSlider.post(() -> {
                                try {
                                    ViewGroup again = sPromotedSliderRef != null ? sPromotedSliderRef.get() : null;
                                    if (again == curSlider && again.getParent() == fullRoot
                                            && !sSliderTempRestored && !sSliderDragging) {
                                        if (again.getHeight() > 0) sPromotedHeight = again.getHeight();
                                        applyClampedNormalizedSliderPosition(ctx, fullRoot, again, LibreConfig.get(ctx));
                                    }
                                } catch (Throwable ignored) {}
                            });
                        }
                    }
                } catch (Throwable ignored) {}
            });
        }
        if (fullRoot.getTag(TAG_SLIDER_VIS_LISTENER) == null) {
            fullRoot.setTag(TAG_SLIDER_VIS_LISTENER, Boolean.TRUE);
            fullRoot.getViewTreeObserver().addOnGlobalLayoutListener(
                    new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
                private WeakReference<ViewGroup> mLastOrigParent = null;
                private int mLastOrigVis = -1;

                @Override
                public void onGlobalLayout() {
                    try {
                        if (sSliderTempRestored) return;
                        ViewGroup op = sPromotedOrigParentRef != null ? sPromotedOrigParentRef.get() : null;
                        ViewGroup sl = sPromotedSliderRef != null ? sPromotedSliderRef.get() : null;
                        if (op == null || sl == null || sl.getParent() != fullRoot) return;
                        ViewGroup last = mLastOrigParent != null ? mLastOrigParent.get() : null;
                        int vis = op.getVisibility();
                        if (last != op || vis != mLastOrigVis) {
                            mLastOrigParent = new WeakReference<>(op);
                            mLastOrigVis = vis;
                            if (sl.getVisibility() != vis) {
                                sl.setVisibility(vis);
                            }
                        }
                    } catch (Throwable ignored) {}
                }
            });
        }
    }

    private static void applyClampedNormalizedSliderPosition(
            Context ctx, ViewGroup fullRoot, View slider, LibreConfig config) {
        if (ctx == null || fullRoot == null || slider == null) return;
        if (fullRoot.getWidth() <= 0 || fullRoot.getHeight() <= 0) return;
        float[] bounds = computeSliderSafeBounds(ctx, fullRoot, slider);
        float minX = bounds[0];
        float maxX = bounds[1];
        float minY = bounds[2];
        float maxY = bounds[3];
        boolean land = isLandscapeRoot(fullRoot);

        float normX = config != null ? config.getSliderNormX(land, -1f) : -1f;
        float normY = config != null ? config.getSliderNormY(land, -1f) : -1f;
        if (normX < 0f || normY < 0f) {
            // No dedicated position for this orientation yet: inherit the ratio from the other one.
            float otherX = config != null ? config.getSliderNormX(!land, -1f) : -1f;
            float otherY = config != null ? config.getSliderNormY(!land, -1f) : -1f;
            if (otherX >= 0f && otherY >= 0f) {
                normX = otherX;
                normY = otherY;
            } else {
                float savedX = (!land && config != null) ? config.getSliderPosX(-1f) : -1f;
                float savedY = (!land && config != null) ? config.getSliderPosY(-1f) : -1f;
                if (savedX >= 0f && savedY >= 0f) {
                    normX = (savedX - minX) / Math.max(1f, maxX - minX);
                    normY = (savedY - minY) / Math.max(1f, maxY - minY);
                } else {
                    float baseX = sPromotedStartX >= 0f ? sPromotedStartX : minX;
                    float baseY = sPromotedStartY >= 0f ? sPromotedStartY : minY;
                    normX = (baseX - minX) / Math.max(1f, maxX - minX);
                    normY = (baseY - minY) / Math.max(1f, maxY - minY);
                }
            }
        }
        normX = clampFloat(normX, 0f, 1f);
        normY = clampFloat(normY, 0f, 1f);
        float targetX = clampFloat(minX + normX * (maxX - minX), minX, maxX);
        float targetY = clampSliderY(bounds, targetX, minY + normY * (maxY - minY));
        placeSliderAt(fullRoot, slider, targetX, targetY);
        updateSliderGestureExclusion(fullRoot, slider);
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
                if (attempt < 15 && activity.getWindow() != null && activity.getWindow().getDecorView() != null) {
                    activity.getWindow().getDecorView().postDelayed(
                            () -> attachFloatingSlidersWithRetry(activity, sliderContainer, attempt + 1),
                            100L);
                }
                return;
            }

            if (container.findViewWithTag(TAG_DRAG_HANDLE) != null) return;

            // Create full-width touch container holding a centered 2x2 dot rounded square handle
            final LinearLayout dragHandle = new LinearLayout(activity);
            dragHandle.setTag(TAG_DRAG_HANDLE);
            dragHandle.setContentDescription(TAG_DRAG_HANDLE);
            dragHandle.setOrientation(LinearLayout.VERTICAL);
            dragHandle.setGravity(Gravity.CENTER);
            dragHandle.setClickable(true);
            dragHandle.setFocusable(false);

            final int badgeSize = dpToPx(activity, 24);
            final int hitPadTop = dpToPx(activity, 8);
            final int hitPadBottom = dpToPx(activity, 12);
            dragHandle.setPadding(0, hitPadTop, 0, hitPadBottom);

            LinearLayout badge = new LinearLayout(activity);
            badge.setOrientation(LinearLayout.VERTICAL);
            badge.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(badgeSize, badgeSize);
            badge.setLayoutParams(badgeLp);

            GradientDrawable handleBg = new GradientDrawable();
            handleBg.setColor(Color.parseColor("#992B2B2E"));
            handleBg.setCornerRadius(dpToPx(activity, 7));
            badge.setBackground(handleBg);

            // 2x2 dot grip indicator
            int dotSize = Math.max(6, dpToPx(activity, 3));
            int dotGap = dpToPx(activity, 4);
            GradientDrawable dotBg = new GradientDrawable();
            dotBg.setShape(GradientDrawable.OVAL);
            dotBg.setColor(Color.parseColor("#E6F0F4F8"));

            for (int rowIdx = 0; rowIdx < 2; rowIdx++) {
                LinearLayout row = new LinearLayout(activity);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                if (rowIdx == 1) {
                    rowLp.topMargin = dotGap;
                }
                row.setLayoutParams(rowLp);
                for (int colIdx = 0; colIdx < 2; colIdx++) {
                    View dot = new View(activity);
                    LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dotSize, dotSize);
                    if (colIdx == 1) {
                        dotLp.leftMargin = dotGap;
                    }
                    dot.setLayoutParams(dotLp);
                    dot.setBackground(dotBg);
                    row.addView(dot);
                }
                badge.addView(row);
            }
            dragHandle.addView(badge);

            final int touchSlopSq;
            int slop = android.view.ViewConfiguration.get(activity).getScaledTouchSlop();
            touchSlopSq = slop * slop;

            // Attach drag gesture ONLY to the full-width center handle container with touchSlop guard
            dragHandle.setOnTouchListener(new View.OnTouchListener() {
                private float downRawX, downRawY;
                private float startX, startY;
                private boolean isDragging = false;
                private float[] mBounds = null;

                private void endGesture() {
                    isDragging = false;
                    sSliderDragging = false;
                    mBounds = null;
                }

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    // Always resolve the live promoted slider (never a stale captured container),
                    // because rotation can swap seekbars_view <-> rl_left_setting_root_seekbar.
                    ViewGroup target = sPromotedSliderRef != null ? sPromotedSliderRef.get() : null;
                    ViewGroup fullRoot = sPromotedFullRootRef != null ? sPromotedFullRootRef.get() : null;
                    boolean valid = target != null && fullRoot != null
                            && target.getParent() == fullRoot
                            && isViewDescendantOf(target, v);
                    int action = event.getActionMasked();
                    switch (action) {
                        case MotionEvent.ACTION_DOWN:
                            endGesture();
                            if (!valid) return true;
                            ViewParent p = target.getParent();
                            if (p != null) p.requestDisallowInterceptTouchEvent(true);
                            downRawX = event.getRawX();
                            downRawY = event.getRawY();
                            startX = target.getX();
                            startY = target.getY();
                            return true;

                        case MotionEvent.ACTION_MOVE: {
                            if (!valid) {
                                if (isDragging) endGesture();
                                return true;
                            }
                            float dx = event.getRawX() - downRawX;
                            float dy = event.getRawY() - downRawY;
                            if (!isDragging) {
                                if ((dx * dx + dy * dy) < touchSlopSq) {
                                    return true;
                                }
                                isDragging = true;
                                sSliderDragging = true;
                                mBounds = computeSliderSafeBounds(activity, fullRoot, target);
                            }
                            if (mBounds == null) {
                                mBounds = computeSliderSafeBounds(activity, fullRoot, target);
                            }
                            float newX = clampFloat(startX + dx, mBounds[0], mBounds[1]);
                            float newY = clampSliderY(mBounds, newX, startY + dy);
                            placeSliderAt(fullRoot, target, newX, newY);
                            return true;
                        }

                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            if (valid) {
                                ViewParent pUp = target.getParent();
                                if (pUp != null) pUp.requestDisallowInterceptTouchEvent(false);
                            }
                            if (isDragging && valid) {
                                float[] bounds = computeSliderSafeBounds(activity, fullRoot, target);
                                float curX = clampFloat(target.getX(), bounds[0], bounds[1]);
                                float curY = clampSliderY(bounds, curX, target.getY());
                                placeSliderAt(fullRoot, target, curX, curY);
                                float normX = clampFloat((curX - bounds[0]) / Math.max(1f, bounds[1] - bounds[0]), 0f, 1f);
                                float normY = clampFloat((curY - bounds[2]) / Math.max(1f, bounds[3] - bounds[2]), 0f, 1f);
                                config.setSliderNormPos(isLandscapeRoot(fullRoot), normX, normY);
                                updateSliderGestureExclusion(fullRoot, target);
                            }
                            endGesture();
                            return true;
                    }
                    return false;
                }
            });

            if (container instanceof LinearLayout) {
                LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                handleParams.setMargins(0, -hitPadTop, 0, 0);
                dragHandle.setLayoutParams(handleParams);

                int insertIdx = Math.min(1, container.getChildCount());
                int opacityFlId = LibreResourceResolver.resolveViewId(activity, "opacity_seekbar_fl", 0);
                for (int i = 0; i < container.getChildCount(); i++) {
                    View ch = container.getChildAt(i);
                    if (ch != null && opacityFlId != 0 && ch.getId() == opacityFlId) {
                        insertIdx = i;
                        break;
                    }
                }
                insertIdx = Math.max(0, Math.min(insertIdx, container.getChildCount()));
                container.addView(dragHandle, insertIdx);
            } else {
                int fallbackH = hitPadTop + badgeSize + hitPadBottom;
                container.setPadding(
                        container.getPaddingLeft(),
                        container.getPaddingTop() + fallbackH,
                        container.getPaddingRight(),
                        container.getPaddingBottom());
                ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        fallbackH);
                lp.topMargin = -fallbackH;
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
                    if (fullRoot == null || fullRoot.getWidth() <= 0 || fullRoot.getHeight() <= 0) return;

                    int[] cLoc = new int[2];
                    int[] rLoc = new int[2];
                    container.getLocationInWindow(cLoc);
                    fullRoot.getLocationInWindow(rLoc);

                    mPromoted = true;
                    container.removeOnLayoutChangeListener(this);
                    final int finalW = w;
                    final int finalH = h;
                    final int startX = Math.max(0, cLoc[0] - rLoc[0]);
                    final int startY = Math.max(0, cLoc[1] - rLoc[1]);

                    container.post(() -> {
                        try {
                            if (activity.isFinishing() || !container.isAttachedToWindow()) return;
                            if (container.getParent() != fullRoot && container.getParent() instanceof ViewGroup) {
                                ensureHandleBeforeOpacity(container, container.findViewWithTag(TAG_DRAG_HANDLE), LibreResourceResolver.findViewByName(container, "opacity_seekbar_fl", 0));
                                final ViewGroup origParent = (ViewGroup) container.getParent();
                                sPromotedOrigIndex = Math.max(0, origParent.indexOfChild(container));
                                sPromotedOrigLp = container.getLayoutParams();
                                sPromotedWidth = finalW;
                                sPromotedHeight = finalH;
                                sPromotedStartX = startX;
                                sPromotedStartY = startY;
                                sPromotedSliderRef = new WeakReference<>(container);
                                sPromotedOrigParentRef = new WeakReference<>(origParent);
                                sPromotedFullRootRef = new WeakReference<>(fullRoot);
                                sSliderTempRestored = false;

                                origParent.removeView(container);
                                container.setLayoutParams(createFreeSliderLp(fullRoot, finalW));
                                fullRoot.addView(container);

                                applyClampedNormalizedSliderPosition(activity, fullRoot, container, config);
                                syncPaintLeftSettingCompactWidth(activity, null, fullRoot);

                                installFullRootSliderListeners(fullRoot);
                                hookSliderLayoutListener(container);

                                final int initVis = origParent.getVisibility();
                                if (container.getVisibility() != initVis) {
                                    container.setVisibility(initVis);
                                }
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
            View brushFl = LibreResourceResolver.findViewByName(activity, "brush_seekbar_fl", 0);
            View opacityFl = LibreResourceResolver.findViewByName(activity, "opacity_seekbar_fl", 0);
            if (brushFl != null && opacityFl != null
                    && opacityFl.getParent() instanceof ViewGroup
                    && brushFl.getParent() == opacityFl.getParent()) {
                return (ViewGroup) opacityFl.getParent();
            }
        } catch (Throwable ignored) {}
        return null;
    }

    /**
     * Enforces an 8.0-degree two-finger rotation deadzone on the floating reference window
     * (NavigatorView.mRenderType == 1) so pinch-zoom gestures never cause unwanted tilt.
     *
     * @return null to keep native rotation delta, or Float override delta.
     */
    public static Float filterReferenceRotationDelta(Object navRunnable) {
        if (navRunnable == null || !isReferenceRotationEnabledFast()) {
            return null;
        }
        try {
            if (!sRefRotFieldsResolved) {
                Class<?> runClz = navRunnable.getClass();
                sRefRunNavField = LibreResourceResolver.findField(runClz, "OooOO0O", null, null);
                sRefRunGestureField = LibreResourceResolver.findField(runClz, "OooO0o", null, null);
                sRefRunX0Field = LibreResourceResolver.findField(runClz, "OooO0oO", null, null);
                sRefRunY0Field = LibreResourceResolver.findField(runClz, "OooO0oo", null, null);
                sRefRunX1Field = LibreResourceResolver.findField(runClz, "OooO", null, null);
                sRefRunY1Field = LibreResourceResolver.findField(runClz, "OooOO0", null, null);
                sRefRotFieldsResolved = true;
            }
            if (sRefRunNavField == null || sRefRunX0Field == null || sRefRunY0Field == null
                    || sRefRunX1Field == null || sRefRunY1Field == null) {
                return null;
            }
            Object navView = sRefRunNavField.get(navRunnable);
            if (navView == null) {
                return null;
            }
            if (sNavRenderTypeField == null) {
                sNavRenderTypeField = LibreResourceResolver.findField(
                        navView.getClass(), "mRenderType", int.class, null);
            }
            if (sNavRenderTypeField == null || sNavRenderTypeField.getInt(navView) != 1) {
                return null;
            }

            float x0 = sRefRunX0Field.getFloat(navRunnable);
            float y0 = sRefRunY0Field.getFloat(navRunnable);
            float x1 = sRefRunX1Field.getFloat(navRunnable);
            float y1 = sRefRunY1Field.getFloat(navRunnable);
            float curAngle = (float) Math.toDegrees(Math.atan2(y0 - y1, x0 - x1));

            Object gestureToken = sRefRunGestureField != null ? sRefRunGestureField.get(navRunnable) : null;
            if (gestureToken == null) {
                gestureToken = navRunnable;
            }

            if (gestureToken != sLastRefGestureRef.get()) {
                sLastRefGestureRef = new WeakReference<>(gestureToken);
                sRefRotUnlocked = false;
                sRefRotAccum = 0f;
                sRefRotLastAngle = curAngle;
                return Float.valueOf(0f);
            }

            float delta = curAngle - sRefRotLastAngle;
            if (delta > 180f) {
                delta -= 360f;
            } else if (delta < -180f) {
                delta += 360f;
            }
            sRefRotLastAngle = curAngle;

            if (!sRefRotUnlocked) {
                sRefRotAccum += delta;
                if (Math.abs(sRefRotAccum) < REF_ROTATION_DEADZONE_DEG) {
                    return Float.valueOf(0f);
                }
                sRefRotUnlocked = true;
                return Float.valueOf(0f);
            }
            return Float.valueOf(delta);
        } catch (Throwable ignored) {
            return null;
        }
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

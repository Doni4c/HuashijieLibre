package app.morphe.extension.huashijie;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Process;
import android.view.View;
import android.widget.CompoundButton;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Huashijie Libre global compatibility and telemetry decoupling controller.
 * Replaces domestic ShareSDK with standard Android share sheet,
 * intercepts and suppresses telemetry and crash SDKs, and provides safe app restarts.
 */
public final class GlobalCompatibilityController {
    private static final long PENDING_BUTTON_SHARE_TTL_MS = 45000L;
    private static volatile boolean sFileProviderResolved = false;
    private static volatile Method sFileProviderGetUriMethod = null;
    private static volatile boolean sHostUriHelperResolved = false;
    private static volatile Method sHostUriHelperMethod = null;
    private static volatile WeakReference<Activity> sPendingButtonShareActivity = new WeakReference<>(null);
    private static volatile long sPendingButtonShareAtMs = 0L;

    private static final class SystemShareClickListener implements View.OnClickListener {
        private final WeakReference<Object> mExportViewRef;

        SystemShareClickListener(Object exportPictureView) {
            mExportViewRef = new WeakReference<>(exportPictureView);
        }

        @Override
        public void onClick(View v) {
            triggerSystemShareExport(mExportViewRef.get());
        }
    }

    private GlobalCompatibilityController() {}

    private static Uri resolveShareUri(Context ctx, File targetFile) {
        if (ctx == null || targetFile == null) return null;
        LibreResourceResolver.init(ctx);
        try {
            if (!sFileProviderResolved) {
                try {
                    Class<?> fpClz = Class.forName("androidx.core.content.FileProvider");
                    sFileProviderGetUriMethod = LibreResourceResolver.findMethod(
                            fpClz, "getUriForFile", Uri.class, true,
                            Context.class, String.class, File.class);
                } catch (Throwable ignored) {}
                sFileProviderResolved = true;
            }
            if (sFileProviderGetUriMethod != null) {
                Uri uri = (Uri) sFileProviderGetUriMethod.invoke(
                        null, ctx, ctx.getPackageName() + ".fileprovider", targetFile);
                if (uri != null) {
                    return uri;
                }
            }
        } catch (Throwable ignored) {}

        try {
            if (!sHostUriHelperResolved) {
                try {
                    Class<?> helperClz = LibreResourceResolver.loadRegisteredClass("RxFileTool.class", null);
                    if (helperClz != null) {
                        sHostUriHelperMethod = LibreResourceResolver.findRegisteredMethod(
                                helperClz, "RxFileTool.getUriForFile", null, Uri.class, true,
                                Context.class, File.class);
                    }
                } catch (Throwable ignored) {}
                sHostUriHelperResolved = true;
            }
            if (sHostUriHelperMethod != null) {
                Uri uri = (Uri) sHostUriHelperMethod.invoke(null, ctx, targetFile);
                if (uri != null) {
                    return uri;
                }
            }
        } catch (Throwable ignored) {}

        if (Build.VERSION.SDK_INT < 24) {
            return Uri.fromFile(targetFile);
        }
        return null;
    }

    private static String resolveArtistMimeType(String nameLower) {
        if (nameLower == null) return "application/octet-stream";
        if (nameLower.endsWith(".png")) return "image/png";
        if (nameLower.endsWith(".jpg") || nameLower.endsWith(".jpeg")) return "image/jpeg";
        if (nameLower.endsWith(".webp")) return "image/webp";
        if (nameLower.endsWith(".gif")) return "image/gif";
        if (nameLower.endsWith(".tif") || nameLower.endsWith(".tiff")) return "image/tiff";
        if (nameLower.endsWith(".psd")) return "image/vnd.adobe.photoshop";
        if (nameLower.endsWith(".pdf")) return "application/pdf";
        if (nameLower.endsWith(".mp4")) return "video/mp4";
        if (nameLower.endsWith(".zip")) return "application/zip";
        if (nameLower.endsWith(".hsj")
                || nameLower.endsWith(".asbrush")
                || nameLower.endsWith(".brush")
                || nameLower.endsWith(".bru")
                || nameLower.endsWith(".lbru")
                || nameLower.endsWith(".abr")) {
            return "application/octet-stream";
        }
        return "application/octet-stream";
    }

    /**
     * Trampoline hook called from PaintActivity.o00000oO(Platform, String, int, boolean)V.
     * Intercepts ShareSDK export flow and redirects to standard Android System Share Sheet.
     *
     * @return true if intercepted and handled by Libre; false to let ShareSDK proceed.
     */
    public static boolean handleShare(Activity activity, String filePath) {
        if (activity == null || filePath == null || filePath.trim().isEmpty()) {
            return false;
        }

        try {
            LibreConfig.init(activity);
            boolean isReplay = activity.getClass().getName().endsWith(".ReplayActivity");
            String shareKey = isReplay ? LibreConfig.KEY_REPLAY_SYSTEM_SHARE : LibreConfig.KEY_SYSTEM_SHARE;
            boolean shareSheetActive = LibreConfig.isFeatureActive(shareKey);
            boolean fromButtonShare = consumePendingButtonSystemShare(activity);
            if (!shareSheetActive && !fromButtonShare) {
                return false;
            }

            File file = new File(filePath);
            if (!file.exists() || !file.canRead()) {
                return false;
            }

            Intent shareIntent;
            if (file.isDirectory()) {
                File[] children = file.listFiles();
                if (children == null || children.length == 0) {
                    return false;
                }
                Arrays.sort(children, Comparator.comparing(File::getName));
                ArrayList<Uri> uris = new ArrayList<>();
                ClipData clipData = null;
                for (File child : children) {
                    if (child != null && child.isFile() && child.canRead()) {
                        Uri u = resolveShareUri(activity, child);
                        if (u != null) {
                            uris.add(u);
                            if (Build.VERSION.SDK_INT >= 16) {
                                if (clipData == null) {
                                    clipData = ClipData.newRawUri(child.getName(), u);
                                } else {
                                    clipData.addItem(new ClipData.Item(u));
                                }
                            }
                        }
                    }
                }
                if (uris.isEmpty()) {
                    return false;
                }
                shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                shareIntent.setType("image/*");
                shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
                if (Build.VERSION.SDK_INT >= 16 && clipData != null) {
                    shareIntent.setClipData(clipData);
                }
            } else if (file.isFile()) {
                Uri fileUri = resolveShareUri(activity, file);
                if (fileUri == null) {
                    return false;
                }
                String mimeType = resolveArtistMimeType(filePath.toLowerCase());
                shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType(mimeType);
                shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                if (Build.VERSION.SDK_INT >= 16) {
                    shareIntent.setClipData(ClipData.newRawUri(file.getName(), fileUri));
                }
                shareIntent.putExtra(Intent.EXTRA_TITLE, file.getName());
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, file.getName());
            } else {
                return false;
            }

            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            String chooserTitle = TranslationEngine.get(
                    "libre_share_chooser_title", "\u753b\u4e16\u754c Libre\uff1a\u5206\u4eab\u4f5c\u54c1");
            Intent chooser = Intent.createChooser(shareIntent, chooserTitle);
            chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.startActivity(chooser);

            return true;
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    /**
     * Determines whether to block host native telemetry initialization in FlashActivity.o000o0o0()V.
     */
    public static boolean shouldBlockTelemetrySdk() {
        return LibreConfig.isFeatureActive(LibreConfig.KEY_BLOCK_TELEMETRY);
    }

    /**
     * Determines whether to block Umeng SDK init and preInit calls.
     */
    public static boolean shouldBlockUmengSdk() {
        return LibreConfig.isFeatureActive(LibreConfig.KEY_BLOCK_UMENG_SDK);
    }

    /**
     * Determines whether to block Tencent Bugly crash reporting SDK initialization.
     */
    public static boolean shouldBlockBuglySdk() {
        return LibreConfig.isFeatureActive(LibreConfig.KEY_BLOCK_BUGLY_SDK);
    }

    /**
     * Safe app restart flow that cleans up the task stack before terminating the process.
     */
    public static void restartApp(Context context) {
        if (context == null) return;
        try {
            LibreConfig.flushSync();
            String packageName = context.getPackageName();
            Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
            if (launchIntent != null) {
                Intent mainIntent = Intent.makeRestartActivityTask(launchIntent.getComponent());
                mainIntent.setPackage(packageName);
                context.startActivity(mainIntent);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            Process.killProcess(Process.myPid());
            System.exit(0);
        }
    }

    private static boolean isAllowedExportItem(Object item) {
        if (item == null) return false;
        String cls = item.getClass().getName();
        if (cls.contains("PlatformInfo")) return false;
        try {
            for (Field f : item.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                Object val = f.get(item);
                if (val instanceof String) {
                    String s = ((String) val).toLowerCase();
                    if (s.contains("qq") || s.contains("wechat") || s.contains("qzone")
                            || s.contains("moments") || s.contains("weibo") || s.contains("sina")
                            || s.contains("tencent") || s.contains("xhs") || s.contains("redbook")
                            || s.contains("xiaohongshu") || s.contains("\u5c0f\u7ea2\u4e66")
                            || s.contains("douyin") || s.contains("tiktok")) {
                        return false;
                    }
                }
            }
        } catch (Throwable ignored) {}
        return true;
    }

    public static boolean isHideExportSocialIconsActive() {
        return LibreConfig.isFeatureActive(LibreConfig.KEY_HIDE_EXPORT_SOCIAL_ICONS);
    }

    public static boolean isHideTimelapseExportSocialActive() {
        return LibreConfig.isFeatureActive(LibreConfig.KEY_HIDE_TIMELAPSE_EXPORT_SOCIAL);
    }

    public static boolean isExportShareButtonActive() {
        return LibreConfig.isFeatureActive(LibreConfig.KEY_EXPORT_SHARE_BUTTON);
    }

    private static void markPendingButtonSystemShare(Activity hostActivity) {
        sPendingButtonShareActivity = new WeakReference<>(hostActivity);
        sPendingButtonShareAtMs = android.os.SystemClock.uptimeMillis();
    }

    private static boolean consumePendingButtonSystemShare(Activity activity) {
        long atMs = sPendingButtonShareAtMs;
        if (atMs <= 0L) {
            return false;
        }
        long elapsed = android.os.SystemClock.uptimeMillis() - atMs;
        if (elapsed < 0L || elapsed > PENDING_BUTTON_SHARE_TTL_MS) {
            sPendingButtonShareAtMs = 0L;
            return false;
        }
        Activity pendingAct = sPendingButtonShareActivity != null ? sPendingButtonShareActivity.get() : null;
        if (pendingAct != null && activity != null && pendingAct != activity) {
            return false;
        }
        sPendingButtonShareAtMs = 0L;
        sPendingButtonShareActivity = new WeakReference<>(null);
        return true;
    }

    private static boolean hasSystemShareLogo(ArrayList list) {
        if (list == null) return false;
        for (Object item : list) {
            if (item == null) continue;
            try {
                Field listenerField = LibreResourceResolver.findField(
                        item.getClass(), "listener", View.OnClickListener.class, null);
                Object l = listenerField != null ? listenerField.get(item) : null;
                if (l instanceof SystemShareClickListener) {
                    return true;
                }
            } catch (Throwable ignored) {}
        }
        return false;
    }

    /**
     * Purges domestic Chinese social media platforms from export sheets when configured,
     * and optionally appends the standalone Android System Share button alongside Album/File.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void sanitizeExportLists(Object exportPictureView, ArrayList list0, ArrayList list4) {
        boolean hideSocial = isHideExportSocialIconsActive();
        boolean addShareButton = isExportShareButtonActive();
        if (!hideSocial && !addShareButton) return;
        if (list0 == null && list4 == null) return;
        try {
            if (hideSocial) {
                ArrayList keep = new ArrayList();
                if (list0 != null) {
                    for (Object item : list0) {
                        if (isAllowedExportItem(item)) {
                            keep.add(item);
                        }
                    }
                    list0.clear();
                }
                if (list4 != null) {
                    for (Object item : list4) {
                        if (isAllowedExportItem(item)) {
                            keep.add(item);
                        }
                    }
                    list4.clear();
                }

                if (list0 != null) {
                    list0.addAll(keep);
                }

                if (exportPictureView != null) {
                    hideExportField(exportPictureView, "ExportPictureView.socialRecyclerField");
                    hideExportField(exportPictureView, "ExportPictureView.socialHeaderField");
                    hideExportField(exportPictureView, "ExportPictureView.socialDividerField");
                }
            }

            if (addShareButton && list0 != null && exportPictureView != null && !hasSystemShareLogo(list0)) {
                Context ctx = resolveExportHostContext(exportPictureView);
                if (ctx != null) {
                    Object shareLogo = createSystemShareCustomerLogo(exportPictureView, ctx);
                    if (shareLogo != null) {
                        list0.add(shareLogo);
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    private static Activity resolveExportHostActivity(Object exportPictureView) {
        if (exportPictureView == null) return null;
        try {
            Method getActivity = LibreResourceResolver.findMethod(
                    exportPictureView.getClass(), "getActivity", null, false);
            if (getActivity != null) {
                Object act = getActivity.invoke(exportPictureView);
                if (act instanceof Activity) {
                    return (Activity) act;
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static Context resolveExportHostContext(Object exportPictureView) {
        Activity act = resolveExportHostActivity(exportPictureView);
        if (act != null) return act;
        if (exportPictureView == null) return null;
        try {
            Method getCtx = LibreResourceResolver.findMethod(
                    exportPictureView.getClass(), "getContext", null, false);
            if (getCtx != null) {
                Object ctx = getCtx.invoke(exportPictureView);
                if (ctx instanceof Context) {
                    return (Context) ctx;
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static Bitmap createSystemShareIconBitmap(Context ctx) {
        float density = ctx.getResources().getDisplayMetrics().density;
        int size = Math.max(48, Math.round(32 * density));
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeCap(Paint.Cap.ROUND);

        float s = (float) size;
        float r = s * 0.115f;
        float leftX = s * 0.25f;
        float leftY = s * 0.50f;
        float rightX = s * 0.72f;
        float topY = s * 0.24f;
        float botY = s * 0.76f;

        paint.setStrokeWidth(s * 0.085f);
        canvas.drawLine(leftX, leftY, rightX, topY, paint);
        canvas.drawLine(leftX, leftY, rightX, botY, paint);
        canvas.drawCircle(leftX, leftY, r, paint);
        canvas.drawCircle(rightX, topY, r, paint);
        canvas.drawCircle(rightX, botY, r, paint);
        return bmp;
    }

    private static Object createSystemShareCustomerLogo(final Object exportPictureView, Context ctx) {
        try {
            Class<?> logoClz = Class.forName("cn.sharesdk.onekeyshare.CustomerLogo");
            Constructor<?> ctor = logoClz.getDeclaredConstructor(
                    String.class, Bitmap.class, View.OnClickListener.class, int.class);
            ctor.setAccessible(true);
            String label = TranslationEngine.get("libre_export_system_share", "\u7cfb\u7edf\u5206\u4eab");
            Bitmap icon = createSystemShareIconBitmap(ctx);
            View.OnClickListener listener = new SystemShareClickListener(exportPictureView);
            return ctor.newInstance(label, icon, listener, 2);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void triggerSystemShareExport(Object epv) {
        if (epv == null) return;
        try {
            Class<?> epvClz = epv.getClass();
            Object cb = readObjField(epvClz, epv, "ExportPictureView.callbackField");
            if (cb == null) return;

            int format = readIntField(epvClz, epv, "ExportPictureView.formatField", 0);
            Object wmView = readObjField(epvClz, epv, "ExportPictureView.watermarkSwitchField");
            boolean wmChecked = (wmView instanceof CompoundButton) && ((CompoundButton) wmView).isChecked();
            boolean flag20 = readBoolField(epvClz, epv, "ExportPictureView.shareProgressField", false);
            Object sizeCb = readObjField(epvClz, epv, "ExportPictureView.sizeCallbackField");
            Object sizeType = readObjField(epvClz, epv, "ExportPictureView.sizeTypeField");
            int w = readIntField(epvClz, epv, "ExportPictureView.customWidthField", 0);
            int h = readIntField(epvClz, epv, "ExportPictureView.customHeightField", 0);

            Class<?> platformClz = Class.forName("cn.sharesdk.framework.Platform");
            Class<?> sizeCbClz = LibreResourceResolver.loadRegisteredClass(
                    "ExportPictureView.sizeCallbackClass", null);
            Class<?> sizeTypeClz = Class.forName("net.huanci.paintlib.enums.ExportPictureSizeType");
            if (sizeCbClz != null) {
                Method exportMethod = LibreResourceResolver.findRegisteredMethod(
                        cb.getClass(),
                        "ExportPictureView.callbackInvoke",
                        null,
                        void.class,
                        false,
                        int.class,
                        int.class,
                        platformClz,
                        boolean.class,
                        boolean.class,
                        sizeCbClz,
                        sizeTypeClz,
                        int.class,
                        int.class);
                if (exportMethod != null) {
                    markPendingButtonSystemShare(resolveExportHostActivity(epv));
                    Object platform = null;
                    try {
                        exportMethod.invoke(cb, 2, format, platform, wmChecked, flag20, sizeCb, sizeType, w, h);
                    } catch (Throwable primaryErr) {
                        try {
                            markPendingButtonSystemShare(resolveExportHostActivity(epv));
                            Class<?> wechatClz = Class.forName("cn.sharesdk.wechat.friends.Wechat");
                            platform = wechatClz.getDeclaredConstructor().newInstance();
                            exportMethod.invoke(cb, 2, format, platform, wmChecked, flag20, sizeCb, sizeType, w, h);
                        } catch (Throwable ignored) {}
                    }
                }
            }

            Method dismissMethod = LibreResourceResolver.findMethod(epvClz, "dismiss", void.class, false);
            if (dismissMethod != null) {
                dismissMethod.invoke(epv);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Rebinds item click listeners on CustomPlatformPageAdapter ViewHolders so injected
     * System Share entries invoke their custom listener while recycled Album/File slots
     * restore the adapter's mClickListener.
     */
    public static void bindCustomExportClick(Object adapter, View rootView) {
        if (rootView == null) return;
        try {
            Object tag = rootView.getTag();
            if (tag != null && "cn.sharesdk.onekeyshare.CustomerLogo".equals(tag.getClass().getName())) {
                Field listenerField = LibreResourceResolver.findField(
                        tag.getClass(), "listener", View.OnClickListener.class, null);
                Object listener = listenerField != null ? listenerField.get(tag) : null;
                if (listener instanceof View.OnClickListener) {
                    rootView.setOnClickListener((View.OnClickListener) listener);
                    return;
                }
            }
            if (adapter != null) {
                Field clickField = LibreResourceResolver.findField(
                        adapter.getClass(), "mClickListener", View.OnClickListener.class, null);
                Object origListener = clickField != null ? clickField.get(adapter) : null;
                if (origListener instanceof View.OnClickListener) {
                    rootView.setOnClickListener((View.OnClickListener) origListener);
                }
            }
        } catch (Throwable ignored) {}
    }

    private static Object readObjField(Class<?> cls, Object host, String registryKey) {
        try {
            Field f = LibreResourceResolver.findRegisteredField(cls, registryKey, null, null, null);
            return f != null ? f.get(host) : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static int readIntField(Class<?> cls, Object host, String registryKey, int fallback) {
        try {
            Field f = LibreResourceResolver.findRegisteredField(cls, registryKey, null, null, null);
            return f != null ? f.getInt(host) : fallback;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static boolean readBoolField(Class<?> cls, Object host, String registryKey, boolean fallback) {
        try {
            Field f = LibreResourceResolver.findRegisteredField(cls, registryKey, null, null, null);
            return f != null ? f.getBoolean(host) : fallback;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static void hideExportField(Object host, String registryKey) {
        try {
            Field f = LibreResourceResolver.findRegisteredField(
                    host.getClass(), registryKey, null, null, null);
            if (f != null) {
                Object val = f.get(host);
                if (val instanceof View) {
                    ((View) val).setVisibility(View.GONE);
                }
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Adjusts timelapse export resolution view: preserves 720p/1080p/2K/4K resolution choices
     * and hides VIP crown badges when offline profile privileges are active.
     */
    public static void sanitizeExportView3(View root) {
        if (root == null) return;
        try {
            if (!OfflineAccountController.isOfflineProfileActive()) {
                return;
            }
            hideViewIfFound(LibreResourceResolver.findViewByName(root, "d3_1080_vip", 0x7f090429));
            hideViewIfFound(LibreResourceResolver.findViewByName(root, "d3_2k_vip", 0x7f09042b));
            hideViewIfFound(LibreResourceResolver.findViewByName(root, "d3_4k_vip", 0x7f09042d));
        } catch (Throwable ignored) {}
    }

    private static void hideViewIfFound(View v) {
        if (v != null && v.getVisibility() != View.GONE) {
            v.setVisibility(View.GONE);
        }
    }

    /**
     * Generates a structured Markdown diagnostics report for troubleshooting.
     */
    public static String getDiagnosticsReport(Context context) {
        StringBuilder sb = new StringBuilder();
        sb.append("### HuashijieLibre: Diagnostics Report\n\n");

        sb.append("#### 1. Device & Runtime\n");
        sb.append("1. **Device:** ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append("\n");
        sb.append("2. **Android Version:** ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");
        sb.append("3. **Architecture:** ").append(Build.SUPPORTED_ABIS != null && Build.SUPPORTED_ABIS.length > 0 ? Build.SUPPORTED_ABIS[0] : "unknown").append("\n");
        sb.append("4. **Target Package:** `net.huanci.hsjpro` v3.4.6 (VersionCode: 313)\n\n");

        sb.append("#### 2. Module Status\n");
        LibreConfig cfg = LibreConfig.getInstance();
        String langFile = cfg != null ? cfg.getSelectedLanguageFile() : "vi.json";
        String fontFile = cfg != null ? cfg.getSelectedFontFile() : "";
        sb.append("1. **Dynamic i18n:** ").append(LibreConfig.isFeatureActive(LibreConfig.KEY_DYNAMIC_I18N) ? "ENABLED (" + langFile + ")" : "DISABLED").append("\n");
        sb.append("2. **UI Font Typeface:** ").append(fontFile.isEmpty() ? "SYSTEM DEFAULT" : fontFile).append("\n");
        sb.append("3. **Hide Community Tabs:** ").append(statusLabel(LibreConfig.KEY_HIDE_COMMUNITY_TABS)).append("\n");
        sb.append("4. **Instant Launch:** ").append(statusLabel(LibreConfig.KEY_INSTANT_LAUNCH)).append("\n");
        sb.append("5. **Hide Top Bar Cloud:** ").append(statusLabel(LibreConfig.KEY_HIDE_TOPBAR_CLOUD)).append("\n");
        sb.append("6. **Hide Canvas Popup Cloud:** ").append(statusLabel(LibreConfig.KEY_HIDE_CANVAS_POPUP_CLOUD)).append("\n");
        sb.append("7. **Hide Drawer VIP Banner:** ").append(statusLabel(LibreConfig.KEY_HIDE_DRAWER_VIP)).append("\n");
        sb.append("8. **Touch Outside Dismiss:** ").append(statusLabel(LibreConfig.KEY_TOUCH_OUTSIDE_DISMISS)).append("\n");
        sb.append("9. **Free Reference Rotation:** ").append(statusLabel(LibreConfig.KEY_FREE_REF_ROTATION)).append("\n");
        sb.append("10. **Floating Brush Sliders:** ").append(LibreConfig.isFeatureActive(LibreConfig.KEY_FLOATING_BRUSH_SLIDERS, false) ? "ENABLED" : "DISABLED").append("\n");
        sb.append("11. **Clean Speedpaint Export:** ").append(statusLabel(LibreConfig.KEY_CLEAN_SPEEDPAINT)).append("\n");
        sb.append("12. **Local Brush Studio:** ").append(statusLabel(LibreConfig.KEY_LOCAL_BRUSH_MANAGER)).append("\n");
        sb.append("13. **System Share Sheet:** ").append(statusLabel(LibreConfig.KEY_SYSTEM_SHARE)).append("\n");
        sb.append("14. **Block Native Telemetry:** ").append(statusLabel(LibreConfig.KEY_BLOCK_TELEMETRY)).append("\n");
        sb.append("15. **Block Umeng SDK:** ").append(statusLabel(LibreConfig.KEY_BLOCK_UMENG_SDK)).append("\n");
        sb.append("16. **Block Bugly SDK:** ").append(statusLabel(LibreConfig.KEY_BLOCK_BUGLY_SDK)).append("\n");
        sb.append("17. **Public Storage Preferred:** ").append(statusLabel(LibreConfig.KEY_PUBLIC_STORAGE)).append("\n\n");

        sb.append("#### 3. Storage Paths\n");
        File sdcard = Environment.getExternalStorageDirectory();
        sb.append("1. **Root:** `").append(new File(sdcard, "HuashijiePro").getAbsolutePath()).append("`\n");
        File langDir = TranslationEngine.getLanguageDirectory(context);
        if (langDir != null) {
            sb.append("2. **Lang:** `").append(langDir.getAbsolutePath()).append("`\n");
        }
        File fontDir = UiUxController.getFontsDirectory(context);
        if (fontDir != null) {
            sb.append("3. **Fonts:** `").append(fontDir.getAbsolutePath()).append("`\n");
        }
        sb.append("4. **Brushes:** `").append(new File(sdcard, "HuashijiePro/ASBrush").getAbsolutePath()).append("`\n");
        sb.append("5. **Drafts:** `").append(new File(sdcard, "HuashijiePro/Draft").getAbsolutePath()).append("`\n");

        return sb.toString();
    }

    private static String statusLabel(String key) {
        if (!LibreSettingsInjector.isToggleRegistered(key)) {
            return "NOT INSTALLED";
        }
        return LibreConfig.isFeatureActive(key) ? "ENABLED" : "DISABLED";
    }
}

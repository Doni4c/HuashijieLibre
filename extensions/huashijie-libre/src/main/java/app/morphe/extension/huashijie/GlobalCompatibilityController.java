package app.morphe.extension.huashijie;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Process;
import android.os.StrictMode;
import android.view.View;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;

/**
 * Huashijie Libre global compatibility and telemetry decoupling controller.
 * Replaces domestic ShareSDK with standard Android share sheet,
 * intercepts and suppresses telemetry and crash SDKs, and provides safe app restarts.
 */
public final class GlobalCompatibilityController {

    private GlobalCompatibilityController() {}

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
            if (!LibreConfig.isFeatureActive(shareKey)) {
                return false;
            }

            File file = new File(filePath);
            if (!file.exists() || !file.canRead()) {
                return false;
            }

            String lower = filePath.toLowerCase();
            String mimeType = "*/*";
            if (lower.endsWith(".png")) {
                mimeType = "image/png";
            } else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
                mimeType = "image/jpeg";
            } else if (lower.endsWith(".psd")) {
                mimeType = "image/vnd.adobe.photoshop";
            } else if (lower.endsWith(".mp4")) {
                mimeType = "video/mp4";
            } else if (lower.endsWith(".gif")) {
                mimeType = "image/gif";
            }

            try {
                StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                StrictMode.setVmPolicy(builder.build());
            } catch (Throwable ignored) {}

            Uri fileUri = Uri.fromFile(file);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType(mimeType);
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            String chooserTitle = TranslationEngine.get(
                    "libre_share_chooser_title", "\u753b\u4e16\u754c Libre\uff1a\u5206\u4eab\u4f5c\u54c1");
            Intent chooser = Intent.createChooser(shareIntent, chooserTitle);
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
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

    /**
     * Purges domestic Chinese social media platforms from export sheets,
     * preserving local file/album options and system share sheet.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void sanitizeExportLists(Object exportPictureView, ArrayList list0, ArrayList list4) {
        if (!isHideExportSocialIconsActive()) return;
        if (list0 == null && list4 == null) return;
        try {
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
                hideExportField(exportPictureView, "OooOOO");
                hideExportField(exportPictureView, "OooOOo0");
                hideExportField(exportPictureView, "OooOOoo");
            }
        } catch (Throwable ignored) {}
    }

    private static void hideExportField(Object host, String fieldName) {
        try {
            Field f = LibreResourceResolver.findField(host.getClass(), fieldName, null, null);
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
        sb.append("### Huashijie Libre: Diagnostics Report\n\n");

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

package app.morphe.extension.huashijie;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Huashijie Libre: Universal UI Typeface Manager
 * Manages custom font discovery from /sdcard/HuashijiePro/fonts/*.ttf,
 * safely hooks Activity lifecycle to inject fonts on view creation/resumption,
 * and guarantees fallback to Typeface.DEFAULT with zero IllegalStateExceptions.
 */
public final class UIFontManager {
    private static final String FONTS_DIR_NAME = "fonts";
    private static final int TAG_APPLIED_TYPEFACE = 0x7e098629;
    private static final ConcurrentHashMap<String, Typeface> sTypefaceCache = new ConcurrentHashMap<>();
    private static volatile boolean sInitialized = false;

    private UIFontManager() {}

    /**
     * Initializes the Typeface manager in DrawApplication.onCreate()V.
     */
    public static void init(Application app) {
        if (app == null || sInitialized) return;
        synchronized (UIFontManager.class) {
            if (sInitialized) return;
            sInitialized = true;

            app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
                @Override
                public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

                @Override
                public void onActivityStarted(Activity activity) {}

                @Override
                public void onActivityResumed(Activity activity) {
                    if (activity != null && !activity.isFinishing()) {
                        View decor = activity.getWindow().getDecorView();
                        decor.post(() -> applyCustomTypeface(decor));
                    }
                }

                @Override
                public void onActivityPaused(Activity activity) {}

                @Override
                public void onActivityStopped(Activity activity) {}

                @Override
                public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

                @Override
                public void onActivityDestroyed(Activity activity) {}
            });
        }
    }

    public static void applyCustomTypeface(View root) {
        if (root == null) return;
        try {
            Typeface tf = getActiveTypeface(root.getContext());
            if (tf == null || tf == Typeface.DEFAULT) return;
            applyTypefaceRecursive(root, tf);
        } catch (Throwable ignored) {}
    }

    /**
     * Restores the default system typeface and clears applied typeface tags so switching
     * back to a previously used custom font takes effect cleanly.
     */
    public static void resetDefaultTypeface(View root) {
        if (root == null) return;
        try {
            if (root instanceof TextView) {
                TextView tv = (TextView) root;
                Typeface current = tv.getTypeface();
                int style = current != null ? current.getStyle() : Typeface.NORMAL;
                tv.setTypeface(Typeface.DEFAULT, style);
                tv.setTag(TAG_APPLIED_TYPEFACE, null);
            } else if (root instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) root;
                int count = vg.getChildCount();
                for (int i = 0; i < count; i++) {
                    resetDefaultTypeface(vg.getChildAt(i));
                }
            }
        } catch (Throwable ignored) {}
    }

    private static void applyTypefaceRecursive(View v, Typeface tf) {
        if (v instanceof TextView) {
            applyToSingleTextView((TextView) v, tf);
        } else if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            int count = vg.getChildCount();
            for (int i = 0; i < count; i++) {
                applyTypefaceRecursive(vg.getChildAt(i), tf);
            }
        }
    }

    /**
     * Applies the active custom Typeface to a single TextView once per Typeface instance
     * so script-aware measurement runs against the real font metrics without overhead.
     */
    public static boolean applyToSingleTextView(TextView tv, Typeface tf) {
        if (tv == null || tf == null || tf == Typeface.DEFAULT) {
            return false;
        }
        if (tv.getTag(TAG_APPLIED_TYPEFACE) == tf) {
            return false;
        }
        Typeface current = tv.getTypeface();
        int style = current != null ? current.getStyle() : Typeface.NORMAL;
        tv.setTypeface(tf, style);
        tv.setTag(TAG_APPLIED_TYPEFACE, tf);
        return true;
    }

    public static int getActiveTypefaceIdentity(Context context) {
        return System.identityHashCode(getActiveTypeface(context));
    }

    public static Typeface getActiveTypeface(Context context) {
        LibreConfig config = LibreConfig.getInstance();
        if (config == null) return Typeface.DEFAULT;

        String fontFileName = config.getSelectedFontFile();
        if (fontFileName == null || fontFileName.trim().isEmpty()) {
            return Typeface.DEFAULT;
        }

        Typeface cached = sTypefaceCache.get(fontFileName);
        if (cached != null) return cached;

        File fontFile = findFontFile(context, fontFileName);
        if (fontFile != null && fontFile.exists() && fontFile.length() > 0 && fontFile.canRead()) {
            try {
                Typeface tf = Typeface.createFromFile(fontFile);
                if (tf != null) {
                    sTypefaceCache.put(fontFileName, tf);
                    return tf;
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        // Guaranteed safe fallback
        return Typeface.DEFAULT;
    }

    public static File getFontsDirectory(Context context) {
        File dir = new File(Environment.getExternalStorageDirectory(), "HuashijiePro/" + FONTS_DIR_NAME);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        if (!dir.canWrite() && context != null) {
            dir = new File(context.getExternalFilesDir(null), FONTS_DIR_NAME);
            if (!dir.exists()) dir.mkdirs();
        }
        return dir;
    }

    public static File findFontFile(Context context, String fontFileName) {
        File publicFile = new File(Environment.getExternalStorageDirectory(), "HuashijiePro/" + FONTS_DIR_NAME + "/" + fontFileName);
        if (publicFile.exists()) return publicFile;

        if (context != null) {
            File privateFile = new File(context.getExternalFilesDir(null), FONTS_DIR_NAME + "/" + fontFileName);
            if (privateFile.exists()) return privateFile;
        }
        return null;
    }

    public static List<File> getAvailableFontFiles(Context context) {
        List<File> list = new ArrayList<>();
        File dir = getFontsDirectory(context);
        if (dir != null && dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, name) -> {
                String lower = name.toLowerCase();
                return lower.endsWith(".ttf") || lower.endsWith(".otf");
            });
            if (files != null) {
                for (File f : files) {
                    if (f.isFile() && f.length() > 0) {
                        list.add(f);
                    }
                }
            }
        }
        return list;
    }
}

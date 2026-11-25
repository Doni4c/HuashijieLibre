package app.morphe.extension.huashijie;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Huashijie Libre local brush manager and studio controller.
 * Decouples brush editing from remote account login, allowing full offline
 * customization and managing custom brush packs in /sdcard/HuashijiePro/ASBrush/.
 */
public final class LocalBrushController {
    private static final String TAG = "LocalBrushController";
    private static final String SDCARD_DIR_NAME = "HuashijiePro";
    private static final String BRUSH_DIR_NAME = "ASBrush";
    private static final String EXPORT_BRUSH_DIR_NAME = "exportBrushes";

    private LocalBrushController() {}

    public static boolean isLocalBrushManagerActive() {
        return LibreConfig.isFeatureActive(LibreConfig.KEY_LOCAL_BRUSH_MANAGER);
    }

    public static boolean isExternalBrushImportActive() {
        return LibreConfig.isFeatureActive(LibreConfig.KEY_EXTERNAL_BRUSH_IMPORT);
    }

    /**
     * Hook called from ChooseBrushFragment.OoooOO0(BrushModel.Brush.Builder)Z.
     * Bypasses remote ownerId/login match check to allow full offline parameter editing.
     */
    public static boolean shouldAllowBrushEditing(boolean originalCanEdit) {
        if (isLocalBrushManagerActive()) {
            return true;
        }
        return originalCanEdit;
    }

    /**
     * Ensures local brush directories exist and returns all available custom brush files.
     */
    public static List<File> scanCustomBrushes(Context context) {
        List<File> brushes = new ArrayList<>();
        if (!isExternalBrushImportActive()) {
            return brushes;
        }
        try {
            File baseDir = new File(Environment.getExternalStorageDirectory(), SDCARD_DIR_NAME);
            File asBrushDir = new File(baseDir, BRUSH_DIR_NAME);
            File exportBrushDir = new File(baseDir, EXPORT_BRUSH_DIR_NAME);

            if (!asBrushDir.exists()) asBrushDir.mkdirs();
            if (!exportBrushDir.exists()) exportBrushDir.mkdirs();

            File[] files1 = asBrushDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".brush") || name.toLowerCase().endsWith(".asbrush"));
            if (files1 != null) {
                for (File f : files1) brushes.add(f);
            }

            File[] files2 = exportBrushDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".brush") || name.toLowerCase().endsWith(".asbrush"));
            if (files2 != null) {
                for (File f : files2) brushes.add(f);
            }

            Log.i(TAG, "Scanned " + brushes.size() + " custom brush definitions in " + baseDir.getAbsolutePath());
        } catch (Throwable t) {
            Log.w(TAG, "Failed scanning custom brushes", t);
        }
        return brushes;
    }

    /**
     * Ensures BrushPackage list is never null to prevent crash in PaintView.initBrush().
     */
    public static List<?> ensureBrushPackageList(Object list) {
        if (!isExternalBrushImportActive() && !StartupLifecycleController.isInstantLaunchActive()) {
            return list instanceof List ? (List<?>) list : java.util.Collections.emptyList();
        }
        if (list instanceof List && !((List<?>) list).isEmpty()) {
            return (List<?>) list;
        }
        try {
            Class<?> mgrClz = LibreResourceResolver.loadRegisteredClass("BrushPkgManager.class", null);
            Object mgr = LibreResourceResolver.getRegisteredStaticSingleton(
                    mgrClz, "BrushPkgManager.getInstance", null);
            if (mgr != null) {
                Method reload = LibreResourceResolver.findRegisteredMethod(
                        mgrClz, "BrushPkgManager.reload", null, void.class, false, boolean.class);
                if (reload != null) {
                    reload.invoke(mgr, false);
                }
                Field f = LibreResourceResolver.findRegisteredField(
                        mgrClz, "BrushPkgManager.listField", null, List.class, null);
                if (f != null) {
                    Object raw = f.get(mgr);
                    if (raw instanceof List<?>) {
                        List<?> res = (List<?>) raw;
                        if (!res.isEmpty()) {
                            return res;
                        }
                    }
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "Error ensuring brush package list", t);
        }
        return list instanceof List ? (List<?>) list : java.util.Collections.emptyList();
    }
}

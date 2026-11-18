package app.morphe.extension.huashijie;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.view.View;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dynamic resource and obfuscated reflection resolver for multi version resilience.
 * Resolves Android resource IDs by entry name first, validates fallback hex IDs
 * through getResourceEntryName before accepting them, and caches reflection lookups.
 */
public final class LibreResourceResolver {
    private static final ConcurrentHashMap<String, Integer> ID_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Object> REFLECT_CACHE = new ConcurrentHashMap<>();
    private static final Object MISSING = new Object();
    private static volatile Context sAppContext;

    private LibreResourceResolver() {}

    public static void init(Context context) {
        if (context == null) return;
        if (sAppContext == null) {
            Context appCtx = context.getApplicationContext();
            sAppContext = appCtx != null ? appCtx : context;
        }
    }

    public static int resolveViewId(Context context, String name, int fallbackHex) {
        return resolveId(context, "id", name, fallbackHex);
    }

    public static int resolveLayoutId(Context context, String name, int fallbackHex) {
        return resolveId(context, "layout", name, fallbackHex);
    }

    public static int resolveStringId(Context context, String name, int fallbackHex) {
        return resolveId(context, "string", name, fallbackHex);
    }

    public static int resolveId(Context context, String type, String name, int fallbackHex) {
        if (type == null || name == null) return 0;
        String cacheKey = type + "/" + name;
        Integer cached = ID_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        Context ctx = context != null ? context : sAppContext;
        if (ctx == null) {
            return fallbackHex;
        }
        init(ctx);

        try {
            Resources res = ctx.getResources();
            if (res == null) {
                return 0;
            }
            int id = res.getIdentifier(name, type, ctx.getPackageName());
            if (id != 0) {
                ID_CACHE.put(cacheKey, id);
                return id;
            }
            if (fallbackHex != 0) {
                try {
                    String entryName = res.getResourceEntryName(fallbackHex);
                    String typeName = res.getResourceTypeName(fallbackHex);
                    if (name.equals(entryName) && type.equals(typeName)) {
                        ID_CACHE.put(cacheKey, fallbackHex);
                        return fallbackHex;
                    }
                } catch (Resources.NotFoundException ignored) {}
            }
        } catch (Throwable ignored) {}

        ID_CACHE.put(cacheKey, 0);
        return 0;
    }

    public static View findViewByName(View root, String name, int fallbackHex) {
        if (root == null || name == null) return null;
        int id = resolveViewId(root.getContext(), name, fallbackHex);
        if (id == 0) return null;
        return root.findViewById(id);
    }

    public static View findViewByName(Activity activity, String name, int fallbackHex) {
        if (activity == null || name == null) return null;
        int id = resolveViewId(activity, name, fallbackHex);
        if (id == 0) return null;
        return activity.findViewById(id);
    }

    public static Field findField(
            Class<?> targetClass,
            String exactName,
            Class<?> exactType,
            String typeNameContains
    ) {
        if (targetClass == null) return null;
        String cacheKey = "F:" + targetClass.getName() + "#"
                + (exactName != null ? exactName : "") + ":"
                + (exactType != null ? exactType.getName() : "") + ":"
                + (typeNameContains != null ? typeNameContains : "");
        Object cached = REFLECT_CACHE.get(cacheKey);
        if (cached == MISSING) return null;
        if (cached instanceof Field) return (Field) cached;

        Field found = null;
        if (exactName != null && !exactName.isEmpty()) {
            for (Class<?> c = targetClass; c != null && c != Object.class; c = c.getSuperclass()) {
                try {
                    Field f = c.getDeclaredField(exactName);
                    f.setAccessible(true);
                    found = f;
                    break;
                } catch (Throwable ignored) {}
            }
        }

        if (found == null && (exactType != null || (typeNameContains != null && !typeNameContains.isEmpty()))) {
            Field uniqueTypeMatch = null;
            int matchCount = 0;
            for (Class<?> c = targetClass; c != null && c != Object.class; c = c.getSuperclass()) {
                try {
                    for (Field f : c.getDeclaredFields()) {
                        Class<?> ft = f.getType();
                        boolean matched = (exactType != null && exactType.isAssignableFrom(ft))
                                || (typeNameContains != null && !typeNameContains.isEmpty()
                                && ft.getName().contains(typeNameContains));
                        if (matched) {
                            uniqueTypeMatch = f;
                            matchCount++;
                        }
                    }
                } catch (Throwable ignored) {}
            }
            if (matchCount == 1 && uniqueTypeMatch != null) {
                uniqueTypeMatch.setAccessible(true);
                found = uniqueTypeMatch;
            } else if (typeNameContains != null && uniqueTypeMatch != null && exactType == null) {
                uniqueTypeMatch.setAccessible(true);
                found = uniqueTypeMatch;
            }
        }

        REFLECT_CACHE.put(cacheKey, found != null ? found : MISSING);
        return found;
    }

    public static Method findMethod(
            Class<?> targetClass,
            String exactName,
            Class<?> returnType,
            boolean requireStatic,
            Class<?>... paramTypes
    ) {
        if (targetClass == null) return null;
        StringBuilder sig = new StringBuilder();
        sig.append("M:").append(targetClass.getName()).append("#")
                .append(exactName != null ? exactName : "").append(":")
                .append(requireStatic ? "S:" : "I:")
                .append(returnType != null ? returnType.getName() : "*").append(":");
        if (paramTypes != null) {
            for (Class<?> p : paramTypes) {
                sig.append(p != null ? p.getName() : "?").append(",");
            }
        }
        String cacheKey = sig.toString();
        Object cached = REFLECT_CACHE.get(cacheKey);
        if (cached == MISSING) return null;
        if (cached instanceof Method) return (Method) cached;

        Method found = null;
        if (exactName != null && !exactName.isEmpty()) {
            try {
                Method m = targetClass.getMethod(exactName, paramTypes);
                m.setAccessible(true);
                found = m;
            } catch (Throwable ignored) {
                for (Class<?> c = targetClass; c != null && c != Object.class; c = c.getSuperclass()) {
                    try {
                        Method m = c.getDeclaredMethod(exactName, paramTypes);
                        m.setAccessible(true);
                        found = m;
                        break;
                    } catch (Throwable ignored2) {}
                }
            }
        }

        if (found == null) {
            Method uniqueMatch = null;
            int matchCount = 0;
            for (Class<?> c = targetClass; c != null && c != Object.class; c = c.getSuperclass()) {
                try {
                    for (Method m : c.getDeclaredMethods()) {
                        if (requireStatic && !Modifier.isStatic(m.getModifiers())) {
                            continue;
                        }
                        if (returnType != null && !returnType.equals(m.getReturnType())) {
                            continue;
                        }
                        if (!Arrays.equals(m.getParameterTypes(), paramTypes)) {
                            continue;
                        }
                        uniqueMatch = m;
                        matchCount++;
                    }
                } catch (Throwable ignored) {}
            }
            if (matchCount == 1 && uniqueMatch != null) {
                uniqueMatch.setAccessible(true);
                found = uniqueMatch;
            }
        }

        REFLECT_CACHE.put(cacheKey, found != null ? found : MISSING);
        return found;
    }

    public static Object getStaticSingleton(Class<?> targetClass, String exactGetterName) {
        if (targetClass == null) return null;
        try {
            Method m = findMethod(targetClass, exactGetterName, targetClass, true);
            if (m != null) {
                return m.invoke(null);
            }
        } catch (Throwable ignored) {}
        return null;
    }
}

package app.morphe.extension.huashijie;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dynamic resource and obfuscated reflection resolver for multi version resilience.
 * Resolves Android resource IDs by entry name first, validates fallback hex IDs
 * through getResourceEntryName before accepting them, and caches reflection lookups.
 */
public final class LibreResourceResolver {
    private static final ConcurrentHashMap<String, Integer> ID_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Object> REFLECT_CACHE = new ConcurrentHashMap<>();
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
        if (cached != null && cached != 0) {
            return cached;
        }

        Context ctx = context != null ? context : sAppContext;
        if (ctx == null) {
            return 0;
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
                        boolean exactMatched = matchesExactFieldType(ft, exactType);
                        boolean nameMatched = typeNameContains != null && !typeNameContains.isEmpty()
                                && ft.getName().contains(typeNameContains);
                        if (exactMatched || nameMatched) {
                            uniqueTypeMatch = f;
                            matchCount++;
                        }
                    }
                } catch (Throwable ignored) {}
            }
            if (matchCount == 1 && uniqueTypeMatch != null) {
                uniqueTypeMatch.setAccessible(true);
                found = uniqueTypeMatch;
            }
        }

        if (found != null) {
            REFLECT_CACHE.put(cacheKey, found);
        }
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
            List<Method> candidates = new ArrayList<>();
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
                        if (isOverriddenBySubclassMatch(m, candidates)) {
                            continue;
                        }
                        candidates.add(m);
                    }
                } catch (Throwable ignored) {}
            }
            if (candidates.size() > 1) {
                List<Method> deduplicated = new ArrayList<>(candidates.size());
                for (Method m : candidates) {
                    if (m.isBridge() && hasNonBridgeSibling(m, candidates)) {
                        continue;
                    }
                    deduplicated.add(m);
                }
                candidates = deduplicated;
            }
            if (candidates.size() == 1) {
                Method uniqueMatch = candidates.get(0);
                uniqueMatch.setAccessible(true);
                found = uniqueMatch;
            }
        }

        if (found != null) {
            REFLECT_CACHE.put(cacheKey, found);
        }
        return found;
    }

    private static boolean isOverlyBroadFieldType(Class<?> type) {
        return type.isPrimitive()
                || type == Object.class
                || type == View.class
                || type == ViewGroup.class
                || type == TextView.class
                || type == CharSequence.class
                || type == Number.class
                || type == Boolean.class
                || type == Integer.class
                || type == Long.class
                || type == Float.class
                || type == Double.class
                || type == Serializable.class
                || type == Cloneable.class;
    }

    private static boolean matchesExactFieldType(Class<?> fieldType, Class<?> exactType) {
        if (exactType == null || fieldType == null) {
            return false;
        }
        if (exactType.equals(fieldType)) {
            return true;
        }
        if (isOverlyBroadFieldType(exactType)) {
            return false;
        }
        return exactType.isAssignableFrom(fieldType);
    }

    private static boolean isOverriddenBySubclassMatch(Method candidate, List<Method> subclassMatches) {
        int candMod = candidate.getModifiers();
        if (Modifier.isStatic(candMod) || Modifier.isPrivate(candMod)) {
            return false;
        }
        Class<?> candClass = candidate.getDeclaringClass();
        String candName = candidate.getName();
        Class<?>[] candParams = candidate.getParameterTypes();
        for (Method sub : subclassMatches) {
            int subMod = sub.getModifiers();
            if (Modifier.isStatic(subMod) || Modifier.isPrivate(subMod)) {
                continue;
            }
            Class<?> subClass = sub.getDeclaringClass();
            if (subClass == candClass || !candClass.isAssignableFrom(subClass)) {
                continue;
            }
            if (!candName.equals(sub.getName()) || !Arrays.equals(candParams, sub.getParameterTypes())) {
                continue;
            }
            if (Modifier.isPublic(candMod) || Modifier.isProtected(candMod) || isSamePackage(subClass, candClass)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasNonBridgeSibling(Method bridgeCandidate, List<Method> allCandidates) {
        Class<?> owner = bridgeCandidate.getDeclaringClass();
        String name = bridgeCandidate.getName();
        Class<?>[] params = bridgeCandidate.getParameterTypes();
        for (Method other : allCandidates) {
            if (other != bridgeCandidate && !other.isBridge()
                    && other.getDeclaringClass() == owner
                    && name.equals(other.getName())
                    && Arrays.equals(params, other.getParameterTypes())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSamePackage(Class<?> a, Class<?> b) {
        Package pa = a.getPackage();
        Package pb = b.getPackage();
        String na = pa != null ? pa.getName() : "";
        String nb = pb != null ? pb.getName() : "";
        return na.equals(nb);
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

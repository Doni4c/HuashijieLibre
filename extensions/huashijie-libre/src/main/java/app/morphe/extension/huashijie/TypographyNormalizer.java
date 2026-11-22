package app.morphe.extension.huashijie;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Looper;
import android.text.BidiFormatter;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristic;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import java.util.Locale;

/**
 * Script-aware Unicode typography normalization and HarfBuzz measurement helpers.
 * Handles Indic/SE Asian/Arabic shaping protection, regional CJK locale tags,
 * BiDi ordering without full container mirroring, and StaticLayout building.
 */
public final class TypographyNormalizer {

    public static final int SCRIPT_LATIN_CYRILLIC_GREEK = 0;
    public static final int SCRIPT_CJK = 1;
    public static final int SCRIPT_COMPLEX_SHAPING_OR_TALL = 2;

    private static final TextPaint sMainThreadScratchPaint = new TextPaint();

    private TypographyNormalizer() {}

    /**
     * Reuses a single TextPaint on the main UI thread to prevent per-frame GC churn
     * during HarfBuzz measurement and layout stabilization passes.
     */
    public static TextPaint getScratchTextPaint(Paint source) {
        TextPaint tp = (Looper.myLooper() == Looper.getMainLooper())
                ? sMainThreadScratchPaint
                : new TextPaint();
        tp.reset();
        if (source instanceof TextPaint) {
            tp.set((TextPaint) source);
        } else if (source != null) {
            tp.set(source);
        }
        return tp;
    }

    public static boolean isShortAsciiOrDigitBadge(CharSequence cs) {
        if (cs == null) return false;
        int start = 0;
        int end = cs.length();
        while (start < end && cs.charAt(start) <= ' ') start++;
        while (end > start && cs.charAt(end - 1) <= ' ') end--;
        int trimmedLen = end - start;
        if (trimmedLen <= 0 || trimmedLen > 2) return false;
        for (int i = start; i < end; i++) {
            if (cs.charAt(i) > 127) return false;
        }
        return true;
    }

    public static boolean hasVisibleBackground(View v) {
        if (v == null) return false;
        Drawable bg = v.getBackground();
        if (bg == null) return false;
        if (bg instanceof ColorDrawable) {
            return (((ColorDrawable) bg).getColor() >>> 24) != 0;
        }
        return true;
    }

    public static float measureFontMetricsLineHeight(Paint paint) {
        if (paint == null) return 0f;
        Paint.FontMetrics fm = paint.getFontMetrics();
        if (fm == null) return paint.getTextSize();
        return Math.max(paint.getTextSize(), fm.bottom - fm.top);
    }

    public static int clamp(int value, int min, int max) {
        if (max < min) return min;
        return Math.max(min, Math.min(max, value));
    }

    public static float clamp(float value, float min, float max) {
        if (max < min) return min;
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Parses a BCP-47 or underscore-delimited locale tag into a Locale.
     */
    public static Locale parseLocaleTag(String rawTag, Locale fallback) {
        if (rawTag == null) {
            return fallback != null ? fallback : Locale.getDefault();
        }
        String clean = rawTag.trim();
        if (clean.isEmpty()) {
            return fallback != null ? fallback : Locale.getDefault();
        }
        String bcp = clean.replace('_', '-');
        if (Build.VERSION.SDK_INT >= 21) {
            try {
                Locale loc = Locale.forLanguageTag(bcp);
                if (loc != null && !loc.getLanguage().isEmpty()) {
                    return loc;
                }
            } catch (Throwable ignored) {}
        }
        String[] parts = bcp.split("-");
        if (parts.length == 1) {
            return new Locale(parts[0]);
        }
        if (parts.length == 2) {
            return new Locale(parts[0], parts[1]);
        }
        return new Locale(parts[0], parts[1], parts[2]);
    }

    /**
     * Checks whether a Unicode code point belongs to an RTL script block
     * (Hebrew, Arabic, Syriac, Thaana, NKo, Samaritan, Mandaic, Adlam).
     */
    public static boolean isRtlCodePoint(int cp) {
        return (cp >= 0x0590 && cp <= 0x05FF)
                || (cp >= 0x0600 && cp <= 0x06FF)
                || (cp >= 0x0700 && cp <= 0x07BF)
                || (cp >= 0x07C0 && cp <= 0x08FF)
                || (cp >= 0xFB1D && cp <= 0xFDFF)
                || (cp >= 0xFE70 && cp <= 0xFEFF)
                || (cp >= 0x10800 && cp <= 0x10FFF)
                || (cp >= 0x1E900 && cp <= 0x1E95F);
    }

    public static boolean containsRtlChars(CharSequence cs) {
        if (cs == null || cs.length() == 0) return false;
        int len = cs.length();
        for (int i = 0; i < len; ) {
            int cp = Character.codePointAt(cs, i);
            i += Character.charCount(cp);
            if (isRtlCodePoint(cp)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isRtlLocale(Locale locale) {
        if (locale == null || Build.VERSION.SDK_INT < 17) return false;
        try {
            return TextUtils.getLayoutDirectionFromLocale(locale) == View.LAYOUT_DIRECTION_RTL;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Classifies a text string by its Unicode script blocks so script invariants
     * are driven solely by character ranges rather than hardcoded language names.
     */
    public static int classifyScript(CharSequence cs) {
        if (cs == null || cs.length() == 0) {
            return SCRIPT_LATIN_CYRILLIC_GREEK;
        }
        boolean hasCjk = false;
        int len = cs.length();
        for (int i = 0; i < len; ) {
            int cp = Character.codePointAt(cs, i);
            i += Character.charCount(cp);
            if (isComplexShapingOrTallCodePoint(cp)) {
                return SCRIPT_COMPLEX_SHAPING_OR_TALL;
            }
            if (isCjkCodePoint(cp)) {
                hasCjk = true;
            }
        }
        return hasCjk ? SCRIPT_CJK : SCRIPT_LATIN_CYRILLIC_GREEK;
    }

    private static boolean isComplexShapingOrTallCodePoint(int cp) {
        if (isRtlCodePoint(cp)) {
            return true;
        }
        // Indic (Devanagari through Sinhala), Thai, Lao, Tibetan, Myanmar.
        if (cp >= 0x0900 && cp <= 0x109F) {
            return true;
        }
        // Tai Le, New Tai Lue, Khmer, Tai Tham, Sundanese, Balinese, Batak, Lepcha.
        if ((cp >= 0x1780 && cp <= 0x17FF)
                || (cp >= 0x1950 && cp <= 0x1AAF)
                || (cp >= 0x1B00 && cp <= 0x1C7F)
                || (cp >= 0xA9E0 && cp <= 0xAAFF)) {
            return true;
        }
        // Khmer Symbols, Myanmar Extended-A/B, Indic extensions.
        return (cp >= 0x19E0 && cp <= 0x19FF)
                || (cp >= 0x11000 && cp <= 0x117FF);
    }

    private static boolean isCjkCodePoint(int cp) {
        return (cp >= 0x3040 && cp <= 0x31FF)
                || (cp >= 0x3400 && cp <= 0x4DBF)
                || (cp >= 0x4E00 && cp <= 0x9FFF)
                || (cp >= 0xAC00 && cp <= 0xD7AF)
                || (cp >= 0x1100 && cp <= 0x11FF)
                || (cp >= 0xF900 && cp <= 0xFAFF)
                || (cp >= 0x20000 && cp <= 0x2FA1F);
    }

    public static boolean containsNonAscii(CharSequence cs) {
        if (cs == null) return false;
        int len = cs.length();
        for (int i = 0; i < len; i++) {
            if (cs.charAt(i) > 127) return true;
        }
        return false;
    }

    /**
     * Wraps mixed BiDi strings safely with BidiFormatter using ANYRTL_LTR when the string
     * contains RTL characters so numbers and technical units ("100%", "300 DPI") keep
     * proper visual reading order without mirroring parent view containers.
     */
    public static String wrapForCanvas(String text, Locale activeLocale) {
        if (text == null || text.isEmpty() || Build.VERSION.SDK_INT < 18) {
            return text;
        }
        if (!containsRtlChars(text)) {
            return text;
        }
        try {
            boolean rtlContext = isRtlLocale(activeLocale) || containsRtlChars(text);
            BidiFormatter formatter = BidiFormatter.getInstance(rtlContext);
            TextDirectionHeuristic heuristic = Build.VERSION.SDK_INT >= 18
                    ? TextDirectionHeuristics.ANYRTL_LTR
                    : null;
            return heuristic != null ? formatter.unicodeWrap(text, heuristic) : formatter.unicodeWrap(text);
        } catch (Throwable ignored) {
            return text;
        }
    }

    /**
     * Prepares a Paint/TextPaint before measuring or drawing on Canvas:
     * enforces active Locale, active custom Typeface, and script invariants.
     */
    public static void preparePaintForScript(Paint paint, CharSequence text, Locale activeLocale, Typeface activeTf) {
        if (paint == null) return;
        if (Build.VERSION.SDK_INT >= 17 && activeLocale != null) {
            try {
                Locale currentLoc = paint.getTextLocale();
                if (!activeLocale.equals(currentLoc)) {
                    paint.setTextLocale(activeLocale);
                }
            } catch (Throwable ignored) {}
        }
        if (activeTf != null && activeTf != Typeface.DEFAULT) {
            Typeface cur = paint.getTypeface();
            if (cur != activeTf) {
                int style = cur != null ? cur.getStyle() : Typeface.NORMAL;
                paint.setTypeface(style != Typeface.NORMAL ? Typeface.create(activeTf, style) : activeTf);
            }
        }
        int scriptGroup = classifyScript(text);
        if (scriptGroup == SCRIPT_COMPLEX_SHAPING_OR_TALL) {
            if (paint.getTextScaleX() != 1.0f) {
                paint.setTextScaleX(1.0f);
            }
            if (Build.VERSION.SDK_INT >= 21 && Math.abs(paint.getLetterSpacing()) > 0.001f) {
                paint.setLetterSpacing(0f);
            }
        } else if (scriptGroup == SCRIPT_CJK) {
            if (paint.getTextScaleX() != 1.0f) {
                paint.setTextScaleX(1.0f);
            }
        }
    }

    /**
     * Applies the mandatory pre-measurement pipeline to a TextView:
     * setTextLocale -> active UIFontManager Typeface -> script safety flags (fontPadding,
     * fallbackLineSpacing, BiDi paragraph direction without container mirroring, and
     * zero horizontal squish for Complex/CJK scripts).
     */
    public static boolean prepareTextViewForPipeline(
            TextView tv,
            CharSequence text,
            Locale activeLocale,
            Typeface activeTf,
            boolean dynamicI18nEnabled) {
        if (tv == null || text == null) return false;
        boolean mutated = false;
        if (Build.VERSION.SDK_INT >= 17 && dynamicI18nEnabled && activeLocale != null) {
            try {
                Locale curLoc = tv.getTextLocale();
                if (!activeLocale.equals(curLoc)) {
                    tv.setTextLocale(activeLocale);
                    mutated = true;
                }
            } catch (Throwable ignored) {}
        }
        if (UIFontManager.applyToSingleTextView(tv, activeTf)) {
            mutated = true;
        }
        int scriptGroup = classifyScript(text);
        boolean nonAscii = containsNonAscii(text);
        if (nonAscii || scriptGroup == SCRIPT_COMPLEX_SHAPING_OR_TALL) {
            if (!tv.getIncludeFontPadding()) {
                tv.setIncludeFontPadding(true);
                mutated = true;
            }
            if (Build.VERSION.SDK_INT >= 28 && !tv.isFallbackLineSpacing()) {
                tv.setFallbackLineSpacing(true);
                mutated = true;
            }
        }
        if (Build.VERSION.SDK_INT >= 17) {
            boolean hasRtl = containsRtlChars(text);
            int curDir = tv.getTextDirection();
            if (hasRtl) {
                if (curDir != View.TEXT_DIRECTION_ANY_RTL) {
                    tv.setTextDirection(View.TEXT_DIRECTION_ANY_RTL);
                    mutated = true;
                }
            } else if (curDir == View.TEXT_DIRECTION_ANY_RTL) {
                tv.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
                mutated = true;
            }
        }
        if (scriptGroup == SCRIPT_COMPLEX_SHAPING_OR_TALL) {
            if (Math.abs(tv.getTextScaleX() - 1.0f) > 0.005f) {
                tv.setTextScaleX(1.0f);
                mutated = true;
            }
            if (Build.VERSION.SDK_INT >= 21 && Math.abs(tv.getLetterSpacing()) > 0.001f) {
                tv.setLetterSpacing(0f);
                mutated = true;
            }
        } else if (scriptGroup == SCRIPT_CJK) {
            if (Math.abs(tv.getTextScaleX() - 1.0f) > 0.005f) {
                tv.setTextScaleX(1.0f);
                mutated = true;
            }
        }
        return mutated;
    }

    /**
     * Measures the single-line rendered width using Android's Layout/HarfBuzz shaper.
     */
    public static float measureHarfBuzzWidth(CharSequence str, TextPaint paint) {
        if (str == null || str.length() == 0 || paint == null) return 0f;
        try {
            return Layout.getDesiredWidth(str, paint);
        } catch (Throwable ignored) {
            return paint.measureText(str, 0, str.length());
        }
    }

    /**
     * Returns the widest line reported by a StaticLayout.
     */
    public static float getMaxLineWidth(Layout layout) {
        if (layout == null) return 0f;
        float maxW = 0f;
        int lc = layout.getLineCount();
        for (int i = 0; i < lc; i++) {
            float lw = layout.getLineWidth(i);
            if (lw > maxW) maxW = lw;
        }
        return maxW;
    }

    /**
     * Builds a HarfBuzz-shaped StaticLayout supporting balanced wrapping, fallback line spacing
     * on API 28+, no mid-word hyphenation, and ANYRTL_LTR BiDi detection.
     */
    public static StaticLayout buildStaticLayout(
            CharSequence str,
            TextPaint paint,
            int availWidth,
            Layout.Alignment align,
            float spacingMult,
            float spacingAdd,
            boolean includePad,
            int maxLines,
            TextUtils.TruncateAt ellipsize,
            boolean useAnyRtlHeuristic) {
        int safeW = Math.max(1, availWidth);
        CharSequence safeStr = str != null ? str : "";
        if (Build.VERSION.SDK_INT >= 23) {
            StaticLayout.Builder builder = StaticLayout.Builder.obtain(safeStr, 0, safeStr.length(), paint, safeW)
                    .setAlignment(align != null ? align : Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(spacingAdd, spacingMult)
                    .setIncludePad(includePad)
                    .setBreakStrategy(Layout.BREAK_STRATEGY_BALANCED)
                    .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
                    .setTextDirection(useAnyRtlHeuristic
                            ? TextDirectionHeuristics.ANYRTL_LTR
                            : TextDirectionHeuristics.FIRSTSTRONG_LTR);
            if (maxLines > 0) {
                builder.setMaxLines(maxLines);
            }
            if (ellipsize != null) {
                builder.setEllipsize(ellipsize);
                builder.setEllipsizedWidth(safeW);
            }
            if (Build.VERSION.SDK_INT >= 28) {
                builder.setUseLineSpacingFromFallbacks(true);
            }
            return builder.build();
        }
        return new StaticLayout(
                safeStr,
                paint,
                safeW,
                align != null ? align : Layout.Alignment.ALIGN_NORMAL,
                spacingMult,
                spacingAdd,
                includePad);
    }
}

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
import android.text.method.SingleLineTransformationMethod;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;
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

    public static final int ARCHETYPE_GENERIC_BOUNDED = 0;
    public static final int ARCHETYPE_ELASTIC_2D = 1;
    public static final int ARCHETYPE_VERTICAL_ELASTIC_STRIP = 2;
    public static final int ARCHETYPE_RIGID_1D_STRIP = 3;
    public static final int ARCHETYPE_CUSTOM_CANVAS = 4;

    public static final int TAG_BASELINE_SNAPSHOT = 0x7e098701;
    public static final int TAG_CONTAINER_HEIGHT_SNAP = 0x7e098702;
    public static final int TAG_LAST_SOLVER_SIG = 0x7e098703;
    public static final int TAG_DIALOG_TITLE_SNAP = 0x7e098704;
    public static final int TAG_ORIG_VISIBILITY = 0x7e098705;
    public static final int TAG_POPUP_ELASTIC_SNAP = 0x7e098706;
    public static final int TAG_PHASE2_MANAGED_EPOCH = 0x7e098707;

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

    /**
     * Immutable baseline snapshot captured once per TextView prior to any solver mutation
     * so toggling Auto-Fit OFF restores exact original geometry and typography.
     */
    public static final class BaselineSnapshot {
        public final float origTextSize;
        public final float origScaleX;
        public final int origPadL;
        public final int origPadT;
        public final int origPadR;
        public final int origPadB;
        public final int origMaxLines;
        public final boolean origSingleLine;
        public final TextUtils.TruncateAt origEllipsize;
        public final int origMaxWidth;
        public final int origGravity;
        public final int origTextAlignment;
        public final int origLpWidth;
        public final int origLpHeight;
        public final int origLpGravity;
        public final int origLpLeftMargin;
        public final int origLpRightMargin;
        public final int origLpMarginStart;
        public final int origLpMarginEnd;
        public final int origMinHeight;

        private BaselineSnapshot(TextView tv) {
            this.origTextSize = tv.getTextSize();
            this.origScaleX = tv.getTextScaleX();
            this.origPadL = tv.getPaddingLeft();
            this.origPadT = tv.getPaddingTop();
            this.origPadR = tv.getPaddingRight();
            this.origPadB = tv.getPaddingBottom();
            int maxL = tv.getMaxLines();
            this.origMaxLines = maxL;
            this.origSingleLine = isSingleLineViewSafe(tv, maxL);
            this.origEllipsize = tv.getEllipsize();
            this.origMaxWidth = tv.getMaxWidth();
            this.origGravity = tv.getGravity();
            this.origTextAlignment = Build.VERSION.SDK_INT >= 17 ? tv.getTextAlignment() : View.TEXT_ALIGNMENT_INHERIT;
            this.origMinHeight = Build.VERSION.SDK_INT >= 16 ? tv.getMinimumHeight() : 0;
            ViewGroup.LayoutParams lp = tv.getLayoutParams();
            if (lp != null) {
                this.origLpWidth = lp.width;
                this.origLpHeight = lp.height;
                if (lp instanceof LinearLayout.LayoutParams) {
                    this.origLpGravity = ((LinearLayout.LayoutParams) lp).gravity;
                } else if (lp instanceof FrameLayout.LayoutParams) {
                    this.origLpGravity = ((FrameLayout.LayoutParams) lp).gravity;
                } else {
                    this.origLpGravity = -1;
                }
                if (lp instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
                    this.origLpLeftMargin = mlp.leftMargin;
                    this.origLpRightMargin = mlp.rightMargin;
                    if (Build.VERSION.SDK_INT >= 17) {
                        this.origLpMarginStart = mlp.getMarginStart();
                        this.origLpMarginEnd = mlp.getMarginEnd();
                    } else {
                        this.origLpMarginStart = mlp.leftMargin;
                        this.origLpMarginEnd = mlp.rightMargin;
                    }
                } else {
                    this.origLpLeftMargin = 0;
                    this.origLpRightMargin = 0;
                    this.origLpMarginStart = 0;
                    this.origLpMarginEnd = 0;
                }
            } else {
                this.origLpWidth = ViewGroup.LayoutParams.WRAP_CONTENT;
                this.origLpHeight = ViewGroup.LayoutParams.WRAP_CONTENT;
                this.origLpGravity = -1;
                this.origLpLeftMargin = 0;
                this.origLpRightMargin = 0;
                this.origLpMarginStart = 0;
                this.origLpMarginEnd = 0;
            }
        }
    }

    /**
     * Snapshot for expanded fixed-height ancestors/containers so DeltaH restores cleanly on toggle OFF.
     */
    public static final class ContainerHeightSnapshot {
        public final int origLpWidth;
        public final int origLpHeight;
        public final int origMinHeight;
        public final int origPadL;
        public final int origPadT;
        public final int origPadR;
        public final int origPadB;
        public final int origGravity;
        public final boolean origClipChildren;
        public final boolean origClipToPadding;
        public final int origLeftMargin;
        public final int origTopMargin;
        public final int origRightMargin;
        public final int origLpGravity;
        public final float origLpWeight;

        private ContainerHeightSnapshot(View v) {
            ViewGroup.LayoutParams lp = v.getLayoutParams();
            this.origLpWidth = lp != null ? lp.width : ViewGroup.LayoutParams.WRAP_CONTENT;
            this.origLpHeight = lp != null ? lp.height : ViewGroup.LayoutParams.WRAP_CONTENT;
            this.origMinHeight = Build.VERSION.SDK_INT >= 16 ? v.getMinimumHeight() : 0;
            this.origPadL = v.getPaddingLeft();
            this.origPadT = v.getPaddingTop();
            this.origPadR = v.getPaddingRight();
            this.origPadB = v.getPaddingBottom();
            if (v instanceof LinearLayout) {
                this.origGravity = ((LinearLayout) v).getGravity();
            } else {
                this.origGravity = Gravity.NO_GRAVITY;
            }
            if (lp instanceof LinearLayout.LayoutParams) {
                LinearLayout.LayoutParams llp = (LinearLayout.LayoutParams) lp;
                this.origLpGravity = llp.gravity;
                this.origLpWeight = llp.weight;
            } else if (lp instanceof FrameLayout.LayoutParams) {
                this.origLpGravity = ((FrameLayout.LayoutParams) lp).gravity;
                this.origLpWeight = 0f;
            } else {
                this.origLpGravity = -1;
                this.origLpWeight = 0f;
            }
            if (v instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) v;
                this.origClipChildren = Build.VERSION.SDK_INT >= 18 ? vg.getClipChildren() : true;
                this.origClipToPadding = vg.getClipToPadding();
            } else {
                this.origClipChildren = true;
                this.origClipToPadding = true;
            }
            if (lp instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
                this.origLeftMargin = mlp.leftMargin;
                this.origTopMargin = mlp.topMargin;
                this.origRightMargin = mlp.rightMargin;
            } else {
                this.origLeftMargin = 0;
                this.origTopMargin = 0;
                this.origRightMargin = 0;
            }
        }
    }

    /**
     * Pure result of evaluating Candidate States 1..5 in memory without mutating UI Views.
     */
    public static final class CandidatePlan {
        public final int state;
        public final float targetPx;
        public final float targetScaleX;
        public final int targetLines;
        public final int neededDeltaH;
        public final boolean useEllipsis;
        public final boolean extremeOutlier;

        public CandidatePlan(
                int state,
                float targetPx,
                float targetScaleX,
                int targetLines,
                int neededDeltaH,
                boolean useEllipsis,
                boolean extremeOutlier) {
            this.state = state;
            this.targetPx = targetPx;
            this.targetScaleX = targetScaleX;
            this.targetLines = targetLines;
            this.neededDeltaH = neededDeltaH;
            this.useEllipsis = useEllipsis;
            this.extremeOutlier = extremeOutlier;
        }
    }

    /**
     * Input descriptor for one member of a Phase 2 static peer strip or menu group.
     */
    public static final class GroupMember {
        public final TextView tv;
        public final String text;
        public final int safeAvailW;
        public final int hardAvailH;
        public final int safePadL;
        public final int safePadR;
        public final boolean allowMultiLine;
        public final View slotView;
        public CandidatePlan plan;

        public GroupMember(
                TextView tv,
                String text,
                int safeAvailW,
                int hardAvailH,
                int safePadL,
                int safePadR,
                boolean allowMultiLine,
                View slotView) {
            this.tv = tv;
            this.text = text;
            this.safeAvailW = Math.max(1, safeAvailW);
            this.hardAvailH = hardAvailH;
            this.safePadL = Math.max(0, safePadL);
            this.safePadR = Math.max(0, safePadR);
            this.allowMultiLine = allowMultiLine;
            this.slotView = slotView;
        }
    }

    /**
     * Aggregated output of Phase 2 group solving (Poison-Pill Protected size + max DeltaH).
     */
    public static final class GroupSolveResult {
        public final float origPx;
        public final float groupPx;
        public final int maxDeltaH;
        public final boolean anyTwoLines;

        public GroupSolveResult(float origPx, float groupPx, int maxDeltaH, boolean anyTwoLines) {
            this.origPx = origPx;
            this.groupPx = groupPx;
            this.maxDeltaH = maxDeltaH;
            this.anyTwoLines = anyTwoLines;
        }
    }

    public static boolean isSingleLineViewSafe(TextView tv, int maxLines) {
        if (maxLines == 1) return true;
        try {
            return tv != null && tv.getTransformationMethod() instanceof SingleLineTransformationMethod;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static BaselineSnapshot captureBaseline(TextView tv) {
        if (tv == null) return null;
        Object existing = tv.getTag(TAG_BASELINE_SNAPSHOT);
        if (existing instanceof BaselineSnapshot) {
            return (BaselineSnapshot) existing;
        }
        BaselineSnapshot snap = new BaselineSnapshot(tv);
        tv.setTag(TAG_BASELINE_SNAPSHOT, snap);
        return snap;
    }

    public static ContainerHeightSnapshot captureContainerBaseline(View v) {
        if (v == null) return null;
        Object existing = v.getTag(TAG_CONTAINER_HEIGHT_SNAP);
        if (existing instanceof ContainerHeightSnapshot) {
            return (ContainerHeightSnapshot) existing;
        }
        ContainerHeightSnapshot snap = new ContainerHeightSnapshot(v);
        v.setTag(TAG_CONTAINER_HEIGHT_SNAP, snap);
        return snap;
    }

    public static boolean restoreBaseline(TextView tv) {
        if (tv == null) return false;
        Object obj = tv.getTag(TAG_BASELINE_SNAPSHOT);
        if (!(obj instanceof BaselineSnapshot)) return false;
        BaselineSnapshot b = (BaselineSnapshot) obj;
        boolean mutated = false;
        if (b.origTextSize > 0f && Math.abs(tv.getTextSize() - b.origTextSize) >= 0.35f) {
            tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, b.origTextSize);
            mutated = true;
        }
        if (Math.abs(tv.getTextScaleX() - b.origScaleX) > 0.005f) {
            tv.setTextScaleX(b.origScaleX);
            mutated = true;
        }
        if (tv.getPaddingLeft() != b.origPadL
                || tv.getPaddingTop() != b.origPadT
                || tv.getPaddingRight() != b.origPadR
                || tv.getPaddingBottom() != b.origPadB) {
            tv.setPadding(b.origPadL, b.origPadT, b.origPadR, b.origPadB);
            mutated = true;
        }
        if (isSingleLineViewSafe(tv, tv.getMaxLines()) != b.origSingleLine) {
            tv.setSingleLine(b.origSingleLine);
            mutated = true;
        }
        if (b.origMaxLines > 0 && tv.getMaxLines() != b.origMaxLines) {
            tv.setMaxLines(b.origMaxLines);
            mutated = true;
        }
        if (tv.getEllipsize() != b.origEllipsize) {
            tv.setEllipsize(b.origEllipsize);
            mutated = true;
        }
        if (tv.getMaxWidth() != b.origMaxWidth) {
            tv.setMaxWidth(b.origMaxWidth);
            mutated = true;
        }
        if (tv.getGravity() != b.origGravity) {
            tv.setGravity(b.origGravity);
            mutated = true;
        }
        if (Build.VERSION.SDK_INT >= 17 && tv.getTextAlignment() != b.origTextAlignment) {
            tv.setTextAlignment(b.origTextAlignment);
            mutated = true;
        }
        if (Build.VERSION.SDK_INT >= 16 && tv.getMinimumHeight() != b.origMinHeight) {
            tv.setMinimumHeight(b.origMinHeight);
            mutated = true;
        }
        ViewGroup.LayoutParams lp = tv.getLayoutParams();
        if (lp != null) {
            boolean lpChanged = false;
            if (lp.width != b.origLpWidth) {
                lp.width = b.origLpWidth;
                lpChanged = true;
            }
            if (lp.height != b.origLpHeight) {
                lp.height = b.origLpHeight;
                lpChanged = true;
            }
            if (lp instanceof LinearLayout.LayoutParams && ((LinearLayout.LayoutParams) lp).gravity != b.origLpGravity) {
                ((LinearLayout.LayoutParams) lp).gravity = b.origLpGravity;
                lpChanged = true;
            } else if (lp instanceof FrameLayout.LayoutParams && ((FrameLayout.LayoutParams) lp).gravity != b.origLpGravity) {
                ((FrameLayout.LayoutParams) lp).gravity = b.origLpGravity;
                lpChanged = true;
            }
            if (lp instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
                if (mlp.leftMargin != b.origLpLeftMargin || mlp.rightMargin != b.origLpRightMargin) {
                    mlp.leftMargin = b.origLpLeftMargin;
                    mlp.rightMargin = b.origLpRightMargin;
                    lpChanged = true;
                }
                if (Build.VERSION.SDK_INT >= 17
                        && (mlp.getMarginStart() != b.origLpMarginStart || mlp.getMarginEnd() != b.origLpMarginEnd)) {
                    mlp.setMarginStart(b.origLpMarginStart);
                    mlp.setMarginEnd(b.origLpMarginEnd);
                    lpChanged = true;
                }
            }
            if (lpChanged) {
                tv.setLayoutParams(lp);
                mutated = true;
            }
        }
        tv.setTag(TAG_LAST_SOLVER_SIG, null);
        tv.setTag(TAG_PHASE2_MANAGED_EPOCH, null);
        return mutated;
    }

    public static boolean restoreContainerBaseline(View v) {
        if (v == null) return false;
        Object obj = v.getTag(TAG_CONTAINER_HEIGHT_SNAP);
        if (!(obj instanceof ContainerHeightSnapshot)) return false;
        ContainerHeightSnapshot c = (ContainerHeightSnapshot) obj;
        boolean mutated = false;
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        if (lp != null) {
            boolean lpChanged = false;
            if (lp.width != c.origLpWidth) {
                lp.width = c.origLpWidth;
                lpChanged = true;
            }
            if (lp.height != c.origLpHeight) {
                lp.height = c.origLpHeight;
                lpChanged = true;
            }
            if (lp instanceof LinearLayout.LayoutParams) {
                LinearLayout.LayoutParams llp = (LinearLayout.LayoutParams) lp;
                if (llp.gravity != c.origLpGravity || Math.abs(llp.weight - c.origLpWeight) > 0.001f) {
                    llp.gravity = c.origLpGravity;
                    llp.weight = c.origLpWeight;
                    lpChanged = true;
                }
            } else if (lp instanceof FrameLayout.LayoutParams && ((FrameLayout.LayoutParams) lp).gravity != c.origLpGravity) {
                ((FrameLayout.LayoutParams) lp).gravity = c.origLpGravity;
                lpChanged = true;
            }
            if (lp instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
                if (mlp.leftMargin != c.origLeftMargin
                        || mlp.topMargin != c.origTopMargin
                        || mlp.rightMargin != c.origRightMargin) {
                    mlp.leftMargin = c.origLeftMargin;
                    mlp.topMargin = c.origTopMargin;
                    mlp.rightMargin = c.origRightMargin;
                    if (Build.VERSION.SDK_INT >= 17) {
                        mlp.setMarginStart(c.origLeftMargin);
                        mlp.setMarginEnd(c.origRightMargin);
                    }
                    lpChanged = true;
                }
            }
            if (lpChanged) {
                v.setLayoutParams(lp);
                mutated = true;
            }
        }
        if (Build.VERSION.SDK_INT >= 16 && v.getMinimumHeight() != c.origMinHeight) {
            v.setMinimumHeight(c.origMinHeight);
            mutated = true;
        }
        if (v.getPaddingLeft() != c.origPadL
                || v.getPaddingTop() != c.origPadT
                || v.getPaddingRight() != c.origPadR
                || v.getPaddingBottom() != c.origPadB) {
            v.setPadding(c.origPadL, c.origPadT, c.origPadR, c.origPadB);
            mutated = true;
        }
        if (v instanceof LinearLayout && ((LinearLayout) v).getGravity() != c.origGravity) {
            ((LinearLayout) v).setGravity(c.origGravity);
            mutated = true;
        }
        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            if (Build.VERSION.SDK_INT >= 18 && vg.getClipChildren() != c.origClipChildren) {
                vg.setClipChildren(c.origClipChildren);
                mutated = true;
            }
            if (vg.getClipToPadding() != c.origClipToPadding) {
                vg.setClipToPadding(c.origClipToPadding);
                mutated = true;
            }
        }
        return mutated;
    }

    /**
     * Expands a strip row and up to maxAncestorHops fixed-height ancestors by addDeltaPx
     * while preserving baseline snapshots and avoiding scroll/recycler containers.
     */
    public static boolean expandFixedHeightChain(View rowView, int addDeltaPx, int maxAncestorHops) {
        if (rowView == null || addDeltaPx <= 0) return false;
        boolean mutated = false;
        View cur = rowView;
        for (int hop = 0; hop <= maxAncestorHops && cur != null; hop++) {
            String cls = cur.getClass().getName();
            if (cls.endsWith("ScrollView")
                    || cls.endsWith("RecyclerView")
                    || cls.endsWith("ListView")
                    || cls.endsWith("DecorView")
                    || cur.getId() == android.R.id.content) {
                break;
            }
            ContainerHeightSnapshot snap = captureContainerBaseline(cur);
            if (snap.origLpHeight > 0) {
                int targetH = snap.origLpHeight + addDeltaPx;
                ViewGroup.LayoutParams lp = cur.getLayoutParams();
                if (lp != null && Math.abs(lp.height - targetH) > 1) {
                    lp.height = targetH;
                    cur.setLayoutParams(lp);
                    mutated = true;
                }
            }
            if (cur instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) cur;
                if (Build.VERSION.SDK_INT >= 18 && vg.getClipChildren()) {
                    vg.setClipChildren(false);
                    mutated = true;
                }
                if (vg.getClipToPadding()) {
                    vg.setClipToPadding(false);
                    mutated = true;
                }
            }
            ViewParent p = cur.getParent();
            cur = (p instanceof View) ? (View) p : null;
        }
        return mutated;
    }

    /**
     * Checks whether an unspaced string uses scripts (CJK, Thai, Lao, Myanmar, Khmer)
     * whose HarfBuzz/ICU break iterator separates words cleanly without ASCII spaces.
     */
    public static boolean allowsUnspacedLineBreak(CharSequence cs) {
        if (cs == null || cs.length() == 0) return false;
        int len = cs.length();
        for (int i = 0; i < len; ) {
            int cp = Character.codePointAt(cs, i);
            i += Character.charCount(cp);
            if (cp == ' ' || cp == '/' || cp == '-' || cp == '\n') {
                return false;
            }
            if ((cp >= 0x4E00 && cp <= 0x9FFF)
                    || (cp >= 0x3400 && cp <= 0x4DBF)
                    || (cp >= 0x3040 && cp <= 0x30FF)
                    || (cp >= 0x0E00 && cp <= 0x0EFF)
                    || (cp >= 0x1000 && cp <= 0x109F)
                    || (cp >= 0x1780 && cp <= 0x17FF)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether a string has an internal natural break token with non-space content on both sides.
     */
    public static boolean hasNaturalBreakToken(CharSequence cs) {
        if (cs == null) return false;
        int len = cs.length();
        boolean sawContentBefore = false;
        boolean sawBreakAfterContent = false;
        for (int i = 0; i < len; i++) {
            char c = cs.charAt(i);
            boolean isBreak = Character.isWhitespace(c) || c == '/' || c == '-' || c == '\u200b';
            if (isBreak) {
                if (sawContentBefore) {
                    sawBreakAfterContent = true;
                }
            } else {
                if (sawBreakAfterContent) {
                    return true;
                }
                sawContentBefore = true;
            }
        }
        return false;
    }

    /**
     * Prevents mid-word wrapping (e.g. Standar/dized) across spaced scripts including Latin, Cyrillic,
     * Greek, Arabic, and Devanagari, while respecting ICU syllabic breaks for CJK/Thai/Lao/Khmer/Myanmar.
     */
    public static boolean hasDisallowedWordSplit(StaticLayout sl, String str, int scriptGroup) {
        if (sl == null || str == null) return true;
        int lc = sl.getLineCount();
        if (lc <= 1) return false;
        if (lc > 2) return true;
        if (allowsUnspacedLineBreak(str)) {
            return false;
        }
        int len = str.length();
        for (int i = 0; i < lc - 1; i++) {
            int end = sl.getLineEnd(i);
            if (end <= 0 || end >= len) continue;
            char prev = str.charAt(end - 1);
            char next = str.charAt(end);
            boolean breakClean = Character.isWhitespace(prev)
                    || prev == '/'
                    || prev == '-'
                    || prev == '\u200b'
                    || Character.isWhitespace(next);
            if (!breakClean) {
                return true;
            }
        }
        return false;
    }

    public static float computeProtectedFloorPx(float origPx, DisplayMetrics dm) {
        float scaledDensity = (dm != null && dm.scaledDensity > 0f) ? dm.scaledDensity : 2f;
        float origSp = origPx / scaledDensity;
        float minSp = origSp < 9.95f ? 8.5f : 9.5f;
        float floorSpPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, minSp, dm);
        return Math.min(origPx, Math.max(origPx * 0.82f, floorSpPx));
    }

    /**
     * Evaluates the 5 deterministic candidate states purely in memory without mutating any View.
     */
    public static CandidatePlan evaluateCandidates(
            String str,
            float origPx,
            float baseScaleX,
            int scriptGroup,
            int archetype,
            boolean allowMultiLine,
            int safeAvailW,
            int hardAvailH,
            int maxGrowthPx,
            float spacingMult,
            float spacingAdd,
            boolean includePad,
            Paint basePaint,
            Locale locale,
            Typeface tf,
            DisplayMetrics dm) {
        int safeW = Math.max(1, safeAvailW);
        TextPaint tp = getScratchTextPaint(basePaint);
        preparePaintForScript(tp, str, locale, tf);

        boolean rtl = containsRtlChars(str);
        boolean multiAllowed = allowMultiLine && archetype != ARCHETYPE_RIGID_1D_STRIP;
        boolean canBreak = multiAllowed && (hasNaturalBreakToken(str) || allowsUnspacedLineBreak(str));
        float safeMult = spacingMult > 0.1f ? spacingMult : 1.0f;

        // State 1: 100% origTextSize, 1 line.
        tp.setTextSize(origPx);
        tp.setTextScaleX(baseScaleX);
        float w1 = measureHarfBuzzWidth(str, tp);
        float fm1 = measureFontMetricsLineHeight(tp);
        if (w1 <= safeW + 0.5f && (hardAvailH <= 0 || fm1 <= hardAvailH + 2f)) {
            return new CandidatePlan(1, origPx, baseScaleX, 1, 0, false, false);
        }

        // State 2: 100% origTextSize, balanced 2-line wrap (only with natural break or ICU unspaced script).
        if (canBreak) {
            tp.setTextSize(origPx);
            tp.setTextScaleX(1.0f);
            StaticLayout sl2 = buildStaticLayout(
                    str, tp, safeW, Layout.Alignment.ALIGN_NORMAL,
                    safeMult, spacingAdd, includePad, 0, null, rtl);
            int lc2 = sl2.getLineCount();
            float fm2 = lc2 * measureFontMetricsLineHeight(tp);
            float need2H = Math.max(fm2, sl2.getHeight());
            boolean vOk2 = (hardAvailH <= 0) || (need2H <= hardAvailH + maxGrowthPx + 2f);
            if (lc2 <= 2
                    && getMaxLineWidth(sl2) <= safeW + 0.5f
                    && !hasDisallowedWordSplit(sl2, str, scriptGroup)
                    && vOk2) {
                int deltaH = (hardAvailH > 0 && need2H > hardAvailH)
                        ? clamp((int) Math.ceil(need2H - hardAvailH + 1f), 0, maxGrowthPx)
                        : 0;
                return new CandidatePlan(2, origPx, 1.0f, Math.max(1, lc2), deltaH, false, false);
            }
        }

        float floorPx = computeProtectedFloorPx(origPx, dm);

        // State 3: Single-line smooth downscale inside [floorPx, origPx) with optional Latin scaleX >= 0.90f.
        if (floorPx < origPx - 0.1f) {
            float curPx = origPx;
            for (int step = 0; step < 8; step++) {
                float nextPx = (step == 7) ? floorPx : Math.max(floorPx, curPx * 0.955f);
                if (Math.abs(nextPx - curPx) < 0.15f && step < 7) {
                    nextPx = floorPx;
                }
                curPx = nextPx;
                tp.setTextSize(curPx);
                tp.setTextScaleX(baseScaleX);
                float mw = measureHarfBuzzWidth(str, tp);
                float mh = measureFontMetricsLineHeight(tp);
                boolean hOk = mw <= safeW + 0.5f;
                boolean vOk = (hardAvailH <= 0) || (mh <= hardAvailH + 2f);
                if (hOk && vOk) {
                    return new CandidatePlan(3, curPx, baseScaleX, 1, 0, false, false);
                }
                if (curPx <= floorPx + 0.05f) break;
            }
        }
        if (scriptGroup == SCRIPT_LATIN_CYRILLIC_GREEK) {
            tp.setTextSize(floorPx);
            tp.setTextScaleX(1.0f);
            float wFloor = measureHarfBuzzWidth(str, tp);
            float mhFloor = measureFontMetricsLineHeight(tp);
            if (wFloor > safeW + 0.5f && wFloor > 1f) {
                float candScale = Math.max(0.90f, Math.min(1.0f, safeW / wFloor));
                tp.setTextScaleX(candScale);
                float scaledW = measureHarfBuzzWidth(str, tp);
                if (scaledW <= safeW + 0.5f && (hardAvailH <= 0 || mhFloor <= hardAvailH + 2f)) {
                    return new CandidatePlan(3, floorPx, candScale, 1, 0, false, false);
                }
            }
        }

        // State 4: Balanced 2-line wrap + gentle downscale inside [floorPx, origPx).
        if (canBreak && floorPx < origPx - 0.1f) {
            float curPx = origPx * 0.94f;
            for (int step = 0; step < 6; step++) {
                if (step == 5 || curPx < floorPx) {
                    curPx = floorPx;
                }
                tp.setTextSize(curPx);
                tp.setTextScaleX(1.0f);
                StaticLayout sl4 = buildStaticLayout(
                        str, tp, safeW, Layout.Alignment.ALIGN_NORMAL,
                        safeMult, spacingAdd, includePad, 0, null, rtl);
                int lc4 = sl4.getLineCount();
                float fm4 = lc4 * measureFontMetricsLineHeight(tp);
                float need4H = Math.max(fm4, sl4.getHeight());
                boolean vOk4 = (hardAvailH <= 0) || (need4H <= hardAvailH + maxGrowthPx + 2f);
                if (lc4 <= 2
                        && getMaxLineWidth(sl4) <= safeW + 0.5f
                        && !hasDisallowedWordSplit(sl4, str, scriptGroup)
                        && vOk4) {
                    int deltaH = (hardAvailH > 0 && need4H > hardAvailH)
                            ? clamp((int) Math.ceil(need4H - hardAvailH + 1f), 0, maxGrowthPx)
                            : 0;
                    return new CandidatePlan(4, curPx, 1.0f, Math.max(1, lc4), deltaH, false, false);
                }
                if (curPx <= floorPx + 0.05f) break;
                curPx = Math.max(floorPx, curPx * 0.94f);
            }
        }

        // State 5: Final Zero-Collision Safety Net (floorPx + TruncateAt.END inside safeInset).
        tp.setTextSize(floorPx);
        tp.setTextScaleX(1.0f);
        float fmFloor = measureFontMetricsLineHeight(tp);
        int finalLines = 1;
        int finalDeltaH = 0;
        if (canBreak && (hardAvailH <= 0 || 2f * fmFloor <= hardAvailH + maxGrowthPx + 2f)) {
            finalLines = 2;
            if (hardAvailH > 0 && 2f * fmFloor > hardAvailH) {
                finalDeltaH = clamp((int) Math.ceil(2f * fmFloor - hardAvailH + 1f), 0, maxGrowthPx);
            }
        }
        return new CandidatePlan(5, floorPx, 1.0f, finalLines, finalDeltaH, true, true);
    }

    /**
     * Phase 2 Peer Synchronization with Poison-Pill Protection and monotonic single-pass convergence.
     */
    public static GroupSolveResult solvePeerGroup(
            List<GroupMember> members,
            int archetype,
            int maxGrowthPx,
            Locale locale,
            Typeface tf,
            DisplayMetrics dm) {
        if (members == null || members.isEmpty()) {
            return new GroupSolveResult(0f, 0f, 0, false);
        }
        float refOrigPx = 0f;
        float minMildPx = Float.MAX_VALUE;
        boolean hasMildDownscale = false;
        int maxDeltaH = 0;
        boolean anyTwoLines = false;

        for (int i = 0; i < members.size(); i++) {
            GroupMember m = members.get(i);
            BaselineSnapshot b = captureBaseline(m.tv);
            if (b == null || b.origTextSize <= 0f) continue;
            if (refOrigPx <= 0f) refOrigPx = b.origTextSize;
            int sg = classifyScript(m.text);
            float baseSx = (sg == SCRIPT_LATIN_CYRILLIC_GREEK) ? b.origScaleX : 1.0f;
            CandidatePlan plan = evaluateCandidates(
                    m.text,
                    b.origTextSize,
                    baseSx,
                    sg,
                    archetype,
                    m.allowMultiLine,
                    m.safeAvailW,
                    m.hardAvailH,
                    maxGrowthPx,
                    m.tv.getLineSpacingMultiplier(),
                    m.tv.getLineSpacingExtra(),
                    true,
                    m.tv.getPaint(),
                    locale,
                    tf,
                    dm);
            m.plan = plan;
            if (!plan.extremeOutlier && plan.targetPx < b.origTextSize - 0.1f) {
                hasMildDownscale = true;
                if (plan.targetPx < minMildPx) {
                    minMildPx = plan.targetPx;
                }
            }
            if (plan.neededDeltaH > maxDeltaH) {
                maxDeltaH = plan.neededDeltaH;
            }
            if (plan.targetLines >= 2) {
                anyTwoLines = true;
            }
        }

        float groupPx = hasMildDownscale ? minMildPx : refOrigPx;
        return new GroupSolveResult(refOrigPx, groupPx, maxDeltaH, anyTwoLines);
    }

    /**
     * Applies a solved CandidatePlan and optional Phase 2 uniform font size to a TextView
     * guarded by a 1-tick idempotency signature to prevent redundant layout passes.
     */
    public static boolean applySolvedPlanToTextView(
            TextView tv,
            CandidatePlan plan,
            float applyPx,
            int padL,
            int padR,
            boolean enforceTopCenter,
            boolean fillSlotWidth,
            int targetMaxWidth,
            int solverEpoch,
            String sigPrefix) {
        if (tv == null || plan == null) return false;
        BaselineSnapshot snap = captureBaseline(tv);
        if (snap == null) return false;

        String sig = sigPrefix
                + "|" + Math.round(applyPx * 4f)
                + "|" + Math.round(plan.targetScaleX * 100f)
                + "|" + plan.targetLines
                + "|" + (plan.useEllipsis ? 1 : 0)
                + "|" + padL + "," + padR
                + "|" + targetMaxWidth
                + "|" + tv.getWidth() + "x" + tv.getHeight();
        Object prevSig = tv.getTag(TAG_LAST_SOLVER_SIG);
        tv.setTag(TAG_PHASE2_MANAGED_EPOCH, Integer.valueOf(solverEpoch));
        if (sig.equals(prevSig)) {
            return false;
        }
        tv.setTag(TAG_LAST_SOLVER_SIG, sig);

        boolean mutated = false;
        boolean wantSingle = plan.targetLines <= 1;
        if (isSingleLineViewSafe(tv, tv.getMaxLines()) != wantSingle) {
            tv.setSingleLine(wantSingle);
            mutated = true;
        }
        if (tv.getMaxLines() != plan.targetLines) {
            tv.setMaxLines(plan.targetLines);
            mutated = true;
        }
        if (Build.VERSION.SDK_INT >= 23 && plan.targetLines >= 2) {
            if (tv.getBreakStrategy() != Layout.BREAK_STRATEGY_BALANCED) {
                tv.setBreakStrategy(Layout.BREAK_STRATEGY_BALANCED);
                mutated = true;
            }
            if (tv.getHyphenationFrequency() != Layout.HYPHENATION_FREQUENCY_NONE) {
                tv.setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE);
                mutated = true;
            }
        }
        TextUtils.TruncateAt wantEl = plan.useEllipsis ? TextUtils.TruncateAt.END : null;
        if (tv.getEllipsize() != wantEl) {
            tv.setEllipsize(wantEl);
            mutated = true;
        }
        if (tv.getPaddingLeft() != padL || tv.getPaddingRight() != padR) {
            tv.setPadding(padL, tv.getPaddingTop(), padR, tv.getPaddingBottom());
            mutated = true;
        }
        if (enforceTopCenter) {
            int wantGrav = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            if (tv.getGravity() != wantGrav) {
                tv.setGravity(wantGrav);
                mutated = true;
            }
            if (Build.VERSION.SDK_INT >= 17 && tv.getTextAlignment() != View.TEXT_ALIGNMENT_CENTER) {
                tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                mutated = true;
            }
        }
        ViewGroup.LayoutParams lp = tv.getLayoutParams();
        ViewParent parent = tv.getParent();
        boolean inVerticalLl = parent instanceof LinearLayout
                && ((LinearLayout) parent).getOrientation() == LinearLayout.VERTICAL;
        if (fillSlotWidth && inVerticalLl && lp != null) {
            boolean lpChanged = false;
            if (lp.width != ViewGroup.LayoutParams.MATCH_PARENT) {
                lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                lpChanged = true;
            }
            if (lp instanceof LinearLayout.LayoutParams) {
                LinearLayout.LayoutParams llp = (LinearLayout.LayoutParams) lp;
                if (llp.gravity != Gravity.CENTER_HORIZONTAL) {
                    llp.gravity = Gravity.CENTER_HORIZONTAL;
                    lpChanged = true;
                }
            }
            if (lpChanged) {
                tv.setLayoutParams(lp);
                mutated = true;
            }
        }
        int wantMaxW = targetMaxWidth > 0 ? targetMaxWidth : snap.origMaxWidth;
        if (tv.getMaxWidth() != wantMaxW) {
            tv.setMaxWidth(wantMaxW);
            mutated = true;
        }
        if (Math.abs(tv.getTextScaleX() - plan.targetScaleX) > 0.005f) {
            tv.setTextScaleX(plan.targetScaleX);
            mutated = true;
        }
        if (applyPx > 0f && Math.abs(tv.getTextSize() - applyPx) >= 0.4f) {
            tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, applyPx);
            mutated = true;
        }
        return mutated;
    }
}

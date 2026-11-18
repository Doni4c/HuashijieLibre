package app.morphe.patches.huashijie.core

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Field
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

/**
 * Shared strict cardinality structural resolvers for multi version resilience.
 * Every resolver fails loudly if matched count is zero or ambiguous (greater than 1).
 */
internal object MultiVersionResolvers {

    private val PRIMITIVE_SIGNATURES = setOf("V", "Z", "B", "S", "C", "I", "J", "F", "D")

    internal fun Iterable<ClassDef>.resolveSingleClass(
        label: String,
        predicate: (ClassDef) -> Boolean,
    ): ClassDef {
        val matches = filter(predicate)
        check(matches.size == 1) {
            "Expected exactly 1 class for $label, found ${matches.size}: ${matches.map { it.type }}"
        }
        return matches[0]
    }

    internal fun ClassDef.resolveSingleMethod(
        label: String,
        predicate: (Method) -> Boolean,
    ): Method {
        val matches = methods.filter(predicate)
        check(matches.size == 1) {
            "Expected exactly 1 method for $label in $type, found ${matches.size}: ${
                matches.map { "${it.name}(${it.parameterTypes.joinToString("")})${it.returnType}" }
            }"
        }
        return matches[0]
    }

    internal fun ClassDef.resolveSingleField(
        label: String,
        predicate: (Field) -> Boolean,
    ): Field {
        val matches = fields.filter(predicate)
        check(matches.size == 1) {
            "Expected exactly 1 field for $label in $type, found ${matches.size}: ${
                matches.map { "${it.name}:${it.type}" }
            }"
        }
        return matches[0]
    }

    internal fun Method.instructionsOrEmpty(): List<Instruction> =
        implementation?.instructions?.toList().orEmpty()

    internal fun Method.callsMethod(
        definingClass: String? = null,
        name: String? = null,
        returnType: String? = null,
    ): Boolean = instructionsOrEmpty().any { inst ->
        val ref = (inst as? ReferenceInstruction)?.reference as? MethodReference ?: return@any false
        (definingClass == null || ref.definingClass == definingClass) &&
            (name == null || ref.name == name) &&
            (returnType == null || ref.returnType == returnType)
    }

    internal fun Method.referencesTypeInCalls(typeName: String): Boolean =
        instructionsOrEmpty().any { inst ->
            val ref = (inst as? ReferenceInstruction)?.reference as? MethodReference ?: return@any false
            ref.definingClass == typeName ||
                ref.returnType == typeName ||
                ref.parameterTypes.any { it.toString() == typeName }
        }

    internal fun Method.referencesTypePrefixInCalls(prefix: String): Boolean =
        instructionsOrEmpty().any { inst ->
            val ref = (inst as? ReferenceInstruction)?.reference as? MethodReference ?: return@any false
            ref.definingClass.startsWith(prefix) ||
                ref.returnType.startsWith(prefix) ||
                ref.parameterTypes.any { it.toString().startsWith(prefix) }
        }

    internal fun Method.usesStringLiteral(literal: String): Boolean =
        instructionsOrEmpty().any { inst ->
            val ref = (inst as? ReferenceInstruction)?.reference as? StringReference
            ref?.string == literal
        }

    internal fun Method.paramsList(): List<String> = parameterTypes.map { it.toString() }

    internal fun resolveAssetsUtilClass(classes: Iterable<ClassDef>): ClassDef =
        classes.resolveSingleClass("AssetsUtil") {
            it.sourceFile == "AssetsUtil.java" && !it.type.contains('$')
        }

    internal fun resolveAssetsUtilGetString(assetsUtil: ClassDef): Method =
        assetsUtil.resolveSingleMethod("AssetsUtil.getString") { m ->
            m.returnType == "Ljava/lang/String;" && m.paramsList() == listOf("I")
        }

    internal fun resolveAssetsUtilGetStringArray(assetsUtil: ClassDef): Method =
        assetsUtil.resolveSingleMethod("AssetsUtil.getStringArray") { m ->
            m.returnType == "[Ljava/lang/String;" && m.paramsList() == listOf("I")
        }

    internal fun resolveStringFogClass(classes: Iterable<ClassDef>): ClassDef =
        classes.resolveSingleClass("StringFog") { c ->
            c.sourceFile == "StringFog.java" &&
                !c.type.contains('$') &&
                c.methods.any { m ->
                    m.returnType == "Ljava/lang/String;" &&
                        m.paramsList() == listOf("Ljava/lang/String;") &&
                        m.usesStringLiteral("hsjproapp")
                }
        }

    internal fun resolveStringFogDecrypt(stringFog: ClassDef): Method =
        stringFog.resolveSingleMethod("StringFog.decrypt") { m ->
            m.returnType == "Ljava/lang/String;" &&
                m.paramsList() == listOf("Ljava/lang/String;") &&
                m.usesStringLiteral("hsjproapp")
        }

    internal fun resolveLanguageLocaleUtilsClass(classes: Iterable<ClassDef>): ClassDef =
        classes.resolveSingleClass("LanguageLocaleUtils") {
            it.sourceFile == "LanguageLocaleUtils.java" && !it.type.contains('$')
        }

    internal fun resolveLanguageLocaleApply(localeUtils: ClassDef): Method =
        localeUtils.resolveSingleMethod("LanguageLocaleUtils.applyLocale") { m ->
            m.returnType == "V" &&
                m.paramsList() == listOf("Landroid/content/Context;", "Ljava/util/Locale;")
        }

    internal fun resolveBubblePopupClass(classes: Iterable<ClassDef>): ClassDef =
        classes.resolveSingleClass("BubblePopupWindow") {
            it.sourceFile == "BubblePopupWindow.java" &&
                !it.type.contains('$') &&
                it.superclass == "Landroid/widget/PopupWindow;"
        }

    internal fun resolveBubblePopupInit(bubblePopup: ClassDef): Method =
        bubblePopup.resolveSingleMethod("BubblePopupWindow.<init>(View, BubbleStyle)") { m ->
            m.name == "<init>" &&
                m.returnType == "V" &&
                m.paramsList() == listOf(
                    "Landroid/view/View;",
                    "Lnet/huanci/hsjpro/views/bubbleview/BubbleStyle;",
                )
        }

    internal fun resolveBubblePopupShowAnchored(bubblePopup: ClassDef): Method =
        bubblePopup.resolveSingleMethod("BubblePopupWindow.showAnchored") { m ->
            val p = m.paramsList()
            m.returnType == "V" &&
                p.size == 4 &&
                p[0] == "Landroid/view/View;" &&
                p[2] == "I" &&
                p[3] == "I"
        }

    internal fun resolveBubblePopupShowAtLocationCustom(bubblePopup: ClassDef): Method =
        bubblePopup.resolveSingleMethod("BubblePopupWindow.showAtLocationCustom") { m ->
            val p = m.paramsList()
            m.returnType == "V" &&
                p.size == 6 &&
                p[0] == "Landroid/view/View;" &&
                p[2] == "I" &&
                p[3] == "I" &&
                p[4] == "Z" &&
                p[5] == "I"
        }

    internal fun resolveAppModuleSwitchHelperClass(classes: Iterable<ClassDef>): ClassDef =
        classes.resolveSingleClass("AppModuleSwitchHelper") {
            it.sourceFile == "AppModuleSwitchHelper.kt" && !it.type.contains('$')
        }

    internal fun resolveAppModuleSwitchSingletonField(helper: ClassDef): Field =
        helper.resolveSingleField("AppModuleSwitchHelper.INSTANCE") { f ->
            AccessFlags.STATIC.isSet(f.accessFlags) && f.type == helper.type
        }

    internal fun resolveCommonDialogBuilderCreate(classes: Iterable<ClassDef>): Method {
        val builderClass = classes.resolveSingleClass("CommonDialog\$Builder") {
            it.type == "Lnet/huanci/hsjpro/dialog/CommonDialog\$Builder;"
        }
        return builderClass.resolveSingleMethod("CommonDialog\$Builder.create") { m ->
            m.returnType == "Lnet/huanci/hsjpro/dialog/CommonDialog;" && m.parameterTypes.isEmpty()
        }
    }

    internal fun resolvePaintActivityClass(classes: Iterable<ClassDef>): ClassDef =
        classes.resolveSingleClass("PaintActivity") {
            it.type == "Lnet/huanci/hsjpro/activities/PaintActivity;"
        }

    internal fun resolvePaintPopup1Menu(paintActivity: ClassDef, bubblePopupType: String): Method =
        paintActivity.resolveSingleMethod("PaintActivity.popup1") { m ->
            m.returnType == "V" &&
                m.paramsList() == listOf("Landroid/view/View;", "I") &&
                m.callsMethod(definingClass = bubblePopupType) &&
                m.callsMethod(definingClass = "Lcom/lihang/ShadowLayout;", name = "setRightShow")
        }

    internal fun resolvePaintPopup2Brush(paintActivity: ClassDef, bubblePopupType: String): Method =
        paintActivity.resolveSingleMethod("PaintActivity.popup2") { m ->
            m.returnType == "I" &&
                m.paramsList() == listOf("Landroid/view/View;", "Landroid/view/View;") &&
                m.callsMethod(definingClass = bubblePopupType) &&
                m.referencesTypeInCalls("Lnet/huanci/hsjpro/paint/views/BrushSeekbarView\$SettingType;")
        }

    internal fun resolvePaintPopup3Canvas(paintActivity: ClassDef, bubblePopupType: String): Method =
        paintActivity.resolveSingleMethod("PaintActivity.popup3") { m ->
            m.returnType == "I" &&
                m.paramsList() == listOf("Landroid/view/View;", "Landroid/view/View;") &&
                m.callsMethod(definingClass = bubblePopupType) &&
                m.referencesTypeInCalls("Lnet/huanci/hsjpro/clouddraft/CloudDraftStoreImpl;")
        }

    internal fun resolvePaintPopup4Ref(paintActivity: ClassDef, bubblePopupType: String): Method =
        paintActivity.resolveSingleMethod("PaintActivity.popup4") { m ->
            m.returnType == "I" &&
                m.paramsList() == listOf("Landroid/view/View;", "Landroid/view/View;") &&
                m.callsMethod(definingClass = bubblePopupType) &&
                m.callsMethod(definingClass = "Lcom/lihang/ShadowLayout;", name = "setLeftShow") &&
                !m.referencesTypeInCalls("Lnet/huanci/hsjpro/paint/views/BrushSeekbarView\$SettingType;") &&
                !m.referencesTypeInCalls("Lnet/huanci/hsjpro/clouddraft/CloudDraftStoreImpl;")
        }

    internal fun resolvePaintSeekbarsViewField(paintActivity: ClassDef): FieldReference {
        val initView = paintActivity.resolveSingleMethod("PaintActivity.initView") { m ->
            m.name == "initView" && m.returnType == "V" && m.parameterTypes.isEmpty()
        }
        val instrs = initView.instructionsOrEmpty()
        val matches = mutableListOf<FieldReference>()
        for (idx in instrs.indices) {
            if (instrs[idx].opcode != Opcode.IPUT_OBJECT) continue
            val fieldRef = (instrs[idx] as? ReferenceInstruction)?.reference as? FieldReference ?: continue
            if (fieldRef.definingClass != paintActivity.type || fieldRef.type != "Landroid/view/View;") {
                continue
            }
            val precededByLayoutChange = (maxOf(0, idx - 8) until idx).any { prevIdx ->
                val prevRef = (instrs[prevIdx] as? ReferenceInstruction)?.reference as? MethodReference
                prevRef?.name == "addOnLayoutChangeListener"
            }
            if (precededByLayoutChange) {
                matches += fieldRef
            }
        }
        val distinct = matches.distinctBy { "${it.definingClass}->${it.name}:${it.type}" }
        check(distinct.size == 1) {
            "Expected exactly 1 seekbars View field after addOnLayoutChangeListener in PaintActivity.initView, found ${distinct.size}: ${
                distinct.map { "${it.name}:${it.type}" }
            }"
        }
        return distinct[0]
    }

    internal fun resolveActivityShareCallback(activityClass: ClassDef): Method =
        activityClass.resolveSingleMethod("${activityClass.type}.shareCallback") { m ->
            m.returnType == "V" &&
                m.paramsList() == listOf("Lcn/sharesdk/framework/Platform;", "Ljava/lang/String;", "I", "Z")
        }

    internal fun resolveBaseFragmentHookTarget(classes: Iterable<ClassDef>): MethodReference {
        val baseFragment = classes.resolveSingleClass("BaseFragment") {
            it.sourceFile == "BaseFragment.java" &&
                it.type.startsWith("Lnet/huanci/hsjpro/") &&
                !it.type.contains('$')
        }
        val onCreateView = baseFragment.resolveSingleMethod("BaseFragment.onCreateView") { m ->
            m.name == "onCreateView" &&
                m.returnType == "Landroid/view/View;" &&
                m.paramsList() == listOf(
                    "Landroid/view/LayoutInflater;",
                    "Landroid/view/ViewGroup;",
                    "Landroid/os/Bundle;",
                )
        }
        val selfCalls = onCreateView.instructionsOrEmpty().mapNotNull { inst ->
            val ref = (inst as? ReferenceInstruction)?.reference as? MethodReference ?: return@mapNotNull null
            if (ref.definingClass == baseFragment.type &&
                ref.returnType == "V" &&
                ref.parameterTypes.map { it.toString() } == listOf("Landroid/view/View;")
            ) {
                ref
            } else {
                null
            }
        }.distinctBy { "${it.definingClass}->${it.name}" }
        check(selfCalls.size == 1) {
            "Expected exactly 1 BaseFragment(View)V call inside BaseFragment.onCreateView, found ${selfCalls.size}: ${
                selfCalls.map { it.name }
            }"
        }
        return selfCalls[0]
    }

    internal fun resolveMainFragment2Class(classes: Iterable<ClassDef>): ClassDef =
        classes.resolveSingleClass("MainFragment2") {
            it.sourceFile == "MainFragment2.java" &&
                it.type.startsWith("Lnet/huanci/hsjpro/") &&
                !it.type.contains('$')
        }

    internal fun resolveMainFragment2BindView(mf2: ClassDef): Method =
        mf2.resolveSingleMethod("MainFragment2.bindView") { m ->
            m.returnType == "V" &&
                m.paramsList() == listOf("Landroid/view/View;") &&
                m.callsMethod(name = "setErrorTips")
        }

    internal fun resolveMainFragment2Inflate(mf2: ClassDef): Method =
        mf2.resolveSingleMethod("MainFragment2.inflate") { m ->
            m.returnType == "V" &&
                m.parameterTypes.isEmpty() &&
                m.callsMethod(definingClass = "Landroid/view/LayoutInflater;", name = "inflate") &&
                m.callsMethod(definingClass = "Ljava/util/List;", name = "size")
        }

    internal fun resolveMainFragment2UpdateCloud(mf2: ClassDef): Method =
        mf2.resolveSingleMethod("MainFragment2.updateCloud") { m ->
            m.returnType == "V" &&
                m.parameterTypes.isEmpty() &&
                m.callsMethod(definingClass = "Landroid/view/View;", name = "setVisibility") &&
                m.callsMethod(definingClass = "Landroid/widget/ImageView;", name = "setVisibility") &&
                m.callsMethod(definingClass = "Landroid/view/View;", name = "getVisibility")
        }

    internal fun resolveSearchHelperClass(classes: Iterable<ClassDef>): ClassDef =
        classes.resolveSingleClass("SearchHelper") {
            it.sourceFile == "SearchHelper.kt" && !it.type.contains('$')
        }

    internal fun resolveSearchHelperShowDropdown(searchHelper: ClassDef): Method =
        searchHelper.resolveSingleMethod("SearchHelper.showDropdown") { m ->
            m.callsMethod(definingClass = "Landroid/widget/PopupWindow;", name = "showAsDropDown")
        }

    internal fun resolveFastKeyFragmentShowDropdown(classes: Iterable<ClassDef>): Method {
        val fastKey = classes.resolveSingleClass("FastKeyFragment") {
            it.sourceFile == "FastKeyFragment.java" &&
                it.type.startsWith("Lnet/huanci/hsjpro/") &&
                !it.type.contains('$')
        }
        return fastKey.resolveSingleMethod("FastKeyFragment.showDropdown") { m ->
            m.callsMethod(definingClass = "Landroid/widget/PopupWindow;", name = "showAsDropDown")
        }
    }

    internal fun resolveFlashActivityClass(classes: Iterable<ClassDef>): ClassDef =
        classes.resolveSingleClass("FlashActivity") {
            it.type == "Lnet/huanci/hsjpro/activities/FlashActivity;"
        }

    internal fun resolveFlashTelemetryInit(flashActivity: ClassDef): Method =
        flashActivity.resolveSingleMethod("FlashActivity.initTelemetry") { m ->
            m.returnType == "V" &&
                m.parameterTypes.isEmpty() &&
                m.referencesTypePrefixInCalls("Lxcrash/")
        }

    internal fun resolveFlashSplashJump(flashActivity: ClassDef): Method =
        flashActivity.resolveSingleMethod("FlashActivity.splashJump") { m ->
            m.returnType == "V" &&
                m.parameterTypes.isEmpty() &&
                m.callsMethod(definingClass = "Landroid/view/ViewGroup;", name = "setBackgroundColor") &&
                m.callsMethod(definingClass = "Landroid/widget/ImageView;", name = "setBackgroundColor")
        }

    internal fun resolveFlashCanLoadAd(flashActivity: ClassDef): Method =
        flashActivity.resolveSingleMethod("FlashActivity.canLoadAd") { m ->
            m.returnType == "Z" &&
                m.parameterTypes.isEmpty() &&
                m.callsMethod(
                    definingClass = "Lnet/huanci/hsjpro/model/LoginUser;",
                    name = "hasLogin",
                ) &&
                m.callsMethod(
                    definingClass = "Lnet/huanci/hsjpro/model/AdScopeUploadModel;",
                    name = "<init>",
                )
        }

    internal fun resolveDrawApplicationWatchdogField(classes: Iterable<ClassDef>): FieldReference {
        val baseActivity = classes.resolveSingleClass("BaseActivity") {
            it.sourceFile == "BaseActivity.java" &&
                it.type.startsWith("Lnet/huanci/hsjpro/") &&
                !it.type.contains('$')
        }
        val onCreate = baseActivity.resolveSingleMethod("BaseActivity.onCreate") { m ->
            m.name == "onCreate" &&
                m.returnType == "V" &&
                m.paramsList() == listOf("Landroid/os/Bundle;")
        }
        val refs = onCreate.instructionsOrEmpty().mapNotNull { inst ->
            if (inst.opcode != Opcode.IGET_BOOLEAN) return@mapNotNull null
            val f = (inst as? ReferenceInstruction)?.reference as? FieldReference ?: return@mapNotNull null
            if (f.definingClass == "Lnet/huanci/hsjpro/DrawApplication;") f else null
        }.distinctBy { "${it.definingClass}->${it.name}:${it.type}" }
        check(refs.size == 1) {
            "Expected exactly 1 DrawApplication boolean field read in BaseActivity.onCreate, found ${refs.size}: ${
                refs.map { it.name }
            }"
        }
        return refs[0]
    }

    internal fun resolveChooseBrushFragmentClass(classes: Iterable<ClassDef>): ClassDef =
        classes.resolveSingleClass("ChooseBrushFragment") {
            it.type == "Lnet/huanci/hsjpro/fragments/ChooseBrushFragment;"
        }

    internal fun resolveChooseBrushCreateView(chooseBrush: ClassDef): Method =
        chooseBrush.resolveSingleMethod("ChooseBrushFragment.createView") { m ->
            m.returnType == "Landroid/view/View;" &&
                m.paramsList() == listOf("Landroid/view/LayoutInflater;", "Landroid/view/ViewGroup;")
        }

    internal fun resolveChooseBrushCanEdit(chooseBrush: ClassDef): Method =
        chooseBrush.resolveSingleMethod("ChooseBrushFragment.canEdit") { m ->
            m.returnType == "Z" &&
                m.paramsList() == listOf("Lnet/huanci/paintlib/model/BrushModel\$Brush\$Builder;")
        }

    internal fun resolveBrushPkgManagerClass(classes: Iterable<ClassDef>): ClassDef =
        classes.resolveSingleClass("BrushPkgManager") {
            it.sourceFile == "BrushPkgManager.java" && !it.type.contains('$')
        }

    internal fun resolveBrushPkgManagerGetList(mgr: ClassDef): Method =
        mgr.resolveSingleMethod("BrushPkgManager.getList") { m ->
            m.returnType == "Ljava/util/List;" && m.paramsList() == listOf("Z")
        }

    internal fun resolveExportPictureViewPrepareAdapter(classes: Iterable<ClassDef>): Method {
        val epv = classes.resolveSingleClass("ExportPictureView") {
            it.type == "Lnet/huanci/hsjpro/views/ExportPictureView;"
        }
        return epv.resolveSingleMethod("ExportPictureView.prepareAdapter") { m ->
            m.returnType == "V" &&
                m.parameterTypes.isEmpty() &&
                m.instructionsOrEmpty().any { inst ->
                    inst.opcode == Opcode.NEW_INSTANCE &&
                        ((inst as? ReferenceInstruction)?.reference as? com.android.tools.smali.dexlib2.iface.reference.TypeReference)
                            ?.type == "Lcn/sharesdk/onekeyshare/themes/classic/CustomPlatformPageAdapter;"
                }
        }
    }

    internal fun resolveExportPictureView3BindView(classes: Iterable<ClassDef>): Method {
        val ep3 = classes.resolveSingleClass("ExportPictureView3") {
            it.type == "Lnet/huanci/hsjpro/views/ExportPictureView3;"
        }
        return ep3.resolveSingleMethod("ExportPictureView3.bindView") { m ->
            !AccessFlags.SYNTHETIC.isSet(m.accessFlags) &&
                m.returnType == "V" &&
                m.paramsList() == listOf("Landroid/view/View;") &&
                m.callsMethod(definingClass = "Landroid/view/View;", name = "findViewById") &&
                m.callsMethod(definingClass = "Landroid/widget/TextView;", name = "setText")
        }
    }

    internal fun resolveTimelapseWatermarkField(classes: Iterable<ClassDef>): FieldReference {
        val paintView = classes.resolveSingleClass("PaintView") {
            it.type == "Lnet/huanci/paintlib/views/PaintView;"
        }
        val setExport = paintView.resolveSingleMethod("PaintView.setExportVideoParams") { m ->
            m.name == "setExportVideoParams" &&
                m.returnType == "V" &&
                m.paramsList() == listOf("Z", "Z")
        }
        val firstBoolParamReg = setExport.implementation!!.registerCount - 2
        val fieldRefs = setExport.instructionsOrEmpty().mapNotNull { inst ->
            if (inst.opcode != Opcode.IPUT_BOOLEAN) return@mapNotNull null
            val twoReg = inst as? com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
                ?: return@mapNotNull null
            if (twoReg.registerA != firstBoolParamReg) return@mapNotNull null
            val ref = (inst as? ReferenceInstruction)?.reference as? FieldReference ?: return@mapNotNull null
            if (ref.definingClass == "Lnet/huanci/paintlib/views/PAINT_PARAMS;") ref else null
        }.distinctBy { "${it.definingClass}->${it.name}:${it.type}" }
        check(fieldRefs.size == 1) {
            "Expected exactly 1 PAINT_PARAMS boolean field in PaintView.setExportVideoParams, found ${fieldRefs.size}: ${
                fieldRefs.map { it.name }
            }"
        }
        return fieldRefs[0]
    }

    internal fun resolveGlVideoWatermarkRenderFrame(classes: Iterable<ClassDef>): Method {
        val eglBase = classes.resolveSingleClass("EglConfigBase") {
            it.sourceFile == "EglConfigBase.java" && !it.type.contains('$')
        }
        return eglBase.resolveSingleMethod("EglConfigBase.renderFrame") { m ->
            val p = m.paramsList()
            m.returnType == "V" &&
                p.size == 6 &&
                p[0] !in PRIMITIVE_SIGNATURES &&
                p.drop(1) == listOf("J", "I", "I", "I", "Z")
        }
    }

    internal fun BytecodePatchContext.allClasses(): List<ClassDef> {
        val list = mutableListOf<ClassDef>()
        classDefForEach { list += it }
        return list
    }

    internal fun BytecodePatchContext.toMutableMethod(method: Method): MutableMethod {
        val cls = classDefByOrNull(method.definingClass)
            ?: error("Defining class not found: ${method.definingClass}")
        val mutableCls = mutableClassDefBy(cls)
        val targetParams = method.parameterTypes.map { it.toString() }
        return mutableCls.methods.firstOrNull { m ->
            m.name == method.name &&
                m.returnType == method.returnType &&
                m.parameterTypes.map { it.toString() } == targetParams
        } ?: error("Mutable method not found: ${method.definingClass}->${method.name}")
    }

    internal fun BytecodePatchContext.toFingerprint(method: Method): Fingerprint =
        exactClassMethodFingerprint(
            method.definingClass,
            method.name,
            method.returnType,
            *method.parameterTypes.map { it.toString() }.toTypedArray(),
        )
}

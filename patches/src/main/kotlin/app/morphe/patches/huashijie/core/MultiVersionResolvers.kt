package app.morphe.patches.huashijie.core

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.util.addFreeRegisters
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Field
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import java.util.Base64

/**
 * Shared strict cardinality structural resolvers for multi version resilience.
 * Every resolver fails loudly if matched count is zero or ambiguous (greater than 1).
 */
internal object MultiVersionResolvers {

    internal const val LIBRE_RESOURCE_RESOLVER_CLASS =
        "Lapp/morphe/extension/huashijie/LibreResourceResolver;"
    private const val REGISTER_TARGET_SIG =
        "${LIBRE_RESOURCE_RESOLVER_CLASS}->registerTarget(Ljava/lang/String;Ljava/lang/String;)V"

    private val PRIMITIVE_SIGNATURES = setOf("V", "Z", "B", "S", "C", "I", "J", "F", "D")

    internal data class NavigatorTouchRunnableTargets(
        val runnableClass: ClassDef,
        val runMethod: Method,
        val navViewField: FieldReference,
        val gestureTokenField: FieldReference,
        val x0Field: FieldReference,
        val y0Field: FieldReference,
        val x1Field: FieldReference,
        val y1Field: FieldReference,
    )

    internal fun String.toJavaClassName(): String {
        val trimmed = trim()
        val unwrapped = if (trimmed.startsWith("L") && trimmed.endsWith(";") && trimmed.length > 2) {
            trimmed.substring(1, trimmed.length - 1)
        } else {
            trimmed
        }
        return unwrapped.replace('/', '.')
    }

    internal fun decryptStringFogLiteralOrNull(
        encoded: String,
        xorKey: String = "hsjproapp",
    ): String? = runCatching {
        val raw = Base64.getDecoder().decode(encoded)
        if (raw.isEmpty()) return@runCatching ""
        val keyBytes = xorKey.toByteArray(Charsets.UTF_8)
        val out = ByteArray(raw.size)
        for (i in raw.indices) {
            out[i] = (raw[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
        }
        String(out, Charsets.UTF_8)
    }.getOrNull()

    internal fun BytecodePatchContext.registerResolvedTarget(key: String, value: String) {
        val resolverClassDef = checkNotNull(classDefByOrNull(LIBRE_RESOURCE_RESOLVER_CLASS)) {
            "Extension class $LIBRE_RESOURCE_RESOLVER_CLASS not found. Ensure extendWith(\"extensions/huashijie-libre.mpe\") is declared."
        }
        val mutableResolver = mutableClassDefBy(resolverClassDef)
        var regMethod = checkNotNull(
            mutableResolver.methods.firstOrNull {
                it.name == "registerResolvedTargets" &&
                    it.parameters.isEmpty() &&
                    it.returnType == "V"
            },
        ) {
            "registerResolvedTargets()V not found in $LIBRE_RESOURCE_RESOLVER_CLASS"
        }

        val initialInstrs = requireNotNull(regMethod.implementation?.instructions) {
            "registerResolvedTargets()V has no implementation."
        }
        val regStrings = HashMap<Int, String>()
        for (inst in initialInstrs) {
            when (inst.opcode) {
                Opcode.CONST_STRING, Opcode.CONST_STRING_JUMBO -> {
                    val reg = (inst as? OneRegisterInstruction)?.registerA ?: continue
                    val str = ((inst as? ReferenceInstruction)?.reference as? StringReference)?.string ?: continue
                    regStrings[reg] = str
                }
                Opcode.INVOKE_STATIC, Opcode.INVOKE_STATIC_RANGE -> {
                    val ref = (inst as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                    val sig = "${ref.definingClass}->${ref.name}(${ref.parameterTypes.joinToString("")})${ref.returnType}"
                    if (sig == REGISTER_TARGET_SIG) {
                        val argRegs = invokeRegisterList(inst)
                        if (argRegs.size == 2) {
                            val existingKey = regStrings[argRegs[0]]
                            val existingVal = regStrings[argRegs[1]]
                            if (existingKey == key) {
                                check(existingVal == value) {
                                    "Conflicting target registration for $key: '$existingVal' vs '$value'"
                                }
                                return
                            }
                        }
                    }
                }
                else -> {}
            }
        }

        val curCount = regMethod.implementation!!.registerCount
        if (curCount < 2) {
            val (expanded, _) = regMethod.addFreeRegisters(2 - curCount)
            regMethod = expanded
        }

        val instrs = regMethod.implementation!!.instructions
        val retIdx = instrs.indexOfLast { it.opcode == Opcode.RETURN_VOID }
        check(retIdx >= 0) {
            "RETURN_VOID not found in $LIBRE_RESOURCE_RESOLVER_CLASS->registerResolvedTargets()V"
        }
        regMethod.addInstructionsPreservingLabels(
            retIdx,
            """
            const-string v0, "$key"
            const-string v1, "$value"
            invoke-static {v0, v1}, $REGISTER_TARGET_SIG
            """.trimIndent(),
        )
    }

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

    internal fun resolveExportPictureViewClass(classes: Iterable<ClassDef>): ClassDef =
        classes.resolveSingleClass("ExportPictureView") {
            it.type == "Lnet/huanci/hsjpro/views/ExportPictureView;"
        }

    internal fun resolveExportPictureViewPrepareAdapter(classes: Iterable<ClassDef>): Method =
        resolveExportPictureViewClass(classes).resolveSingleMethod("ExportPictureView.prepareAdapter") { m ->
            m.returnType == "V" &&
                m.parameterTypes.isEmpty() &&
                m.instructionsOrEmpty().any { inst ->
                    inst.opcode == Opcode.NEW_INSTANCE &&
                        ((inst as? ReferenceInstruction)?.reference as? TypeReference)
                            ?.type == "Lcn/sharesdk/onekeyshare/themes/classic/CustomPlatformPageAdapter;"
                }
        }

    internal fun resolveExportPictureCallbackInterface(classes: Iterable<ClassDef>): ClassDef =
        classes.resolveSingleClass("ExportPictureView.CallbackInterface") { c ->
            c.type.startsWith("Lnet/huanci/hsjpro/views/ExportPictureView\$") &&
                AccessFlags.INTERFACE.isSet(c.accessFlags) &&
                c.methods.any { m ->
                    val p = m.paramsList()
                    m.returnType == "V" &&
                        p.size == 9 &&
                        p[0] == "I" &&
                        p[1] == "I" &&
                        p[2] == "Lcn/sharesdk/framework/Platform;" &&
                        p[3] == "Z" &&
                        p[4] == "Z" &&
                        p[6] == "Lnet/huanci/paintlib/enums/ExportPictureSizeType;" &&
                        p[7] == "I" &&
                        p[8] == "I"
                }
        }

    internal fun resolveExportPictureCallbackInvoke(callbackInterface: ClassDef): Method =
        callbackInterface.resolveSingleMethod("ExportPictureView.Callback.invoke") { m ->
            val p = m.paramsList()
            m.returnType == "V" &&
                p.size == 9 &&
                p[0] == "I" &&
                p[1] == "I" &&
                p[2] == "Lcn/sharesdk/framework/Platform;" &&
                p[3] == "Z" &&
                p[4] == "Z" &&
                p[6] == "Lnet/huanci/paintlib/enums/ExportPictureSizeType;" &&
                p[7] == "I" &&
                p[8] == "I"
        }

    internal fun resolveExportPictureSizeCallbackClass(
        classes: Iterable<ClassDef>,
        callbackInvoke: Method? = null,
    ): ClassDef {
        val invokeMethod = callbackInvoke ?: resolveExportPictureCallbackInvoke(
            resolveExportPictureCallbackInterface(classes),
        )
        val sizeCbType = invokeMethod.paramsList()[5]
        return classes.resolveSingleClass("ExportPictureView.SizeCallback") {
            it.type == sizeCbType
        }
    }

    internal fun resolveExportPictureSocialRecyclerField(epv: ClassDef): Field {
        val prep = epv.resolveSingleMethod("ExportPictureView.prepareAdapter") { m ->
            m.returnType == "V" &&
                m.parameterTypes.isEmpty() &&
                m.instructionsOrEmpty().any { inst ->
                    inst.opcode == Opcode.NEW_INSTANCE &&
                        ((inst as? ReferenceInstruction)?.reference as? TypeReference)
                            ?.type == "Lcn/sharesdk/onekeyshare/themes/classic/CustomPlatformPageAdapter;"
                }
        }
        val instrs = prep.instructionsOrEmpty()
        val matches = mutableListOf<FieldReference>()
        for (idx in instrs.indices) {
            val inst = instrs[idx]
            if (inst.opcode != Opcode.IGET_OBJECT) continue
            val fieldRef = (inst as? ReferenceInstruction)?.reference as? FieldReference ?: continue
            if (fieldRef.definingClass != epv.type ||
                fieldRef.type != "Landroidx/recyclerview/widget/RecyclerView;"
            ) {
                continue
            }
            val followedBySetVis = (idx + 1..minOf(instrs.lastIndex, idx + 3)).any { nextIdx ->
                val ref = (instrs[nextIdx] as? ReferenceInstruction)?.reference as? MethodReference
                ref?.definingClass == "Landroid/view/ViewGroup;" && ref.name == "setVisibility"
            }
            if (followedBySetVis) {
                matches += fieldRef
            }
        }
        val distinct = matches.distinctBy { "${it.name}:${it.type}" }
        check(distinct.size == 1) {
            "Expected exactly 1 social RecyclerView field in ExportPictureView.prepareAdapter, found ${distinct.size}: ${
                distinct.map { it.name }
            }"
        }
        return epv.resolveSingleField("ExportPictureView.socialRecyclerField") {
            it.name == distinct[0].name && it.type == distinct[0].type
        }
    }

    internal fun resolveExportPictureSocialDividerFields(epv: ClassDef): Pair<Field, Field> {
        val prep = epv.resolveSingleMethod("ExportPictureView.prepareAdapter") { m ->
            m.returnType == "V" &&
                m.parameterTypes.isEmpty() &&
                m.instructionsOrEmpty().any { inst ->
                    inst.opcode == Opcode.NEW_INSTANCE &&
                        ((inst as? ReferenceInstruction)?.reference as? TypeReference)
                            ?.type == "Lcn/sharesdk/onekeyshare/themes/classic/CustomPlatformPageAdapter;"
                }
        }
        val instrs = prep.instructionsOrEmpty()
        val wechatMomentsIdx = instrs.indexOfFirst { inst ->
            inst.opcode == Opcode.SGET_OBJECT &&
                ((inst as? ReferenceInstruction)?.reference as? FieldReference)?.let {
                    it.definingClass == "Lcn/sharesdk/wechat/moments/WechatMoments;" && it.name == "NAME"
                } == true
        }
        check(wechatMomentsIdx >= 0) {
            "WechatMoments.NAME read not found in ExportPictureView.prepareAdapter"
        }
        val matches = mutableListOf<FieldReference>()
        for (idx in (wechatMomentsIdx + 1) until instrs.size) {
            val inst = instrs[idx]
            if (inst.opcode != Opcode.IGET_OBJECT) continue
            val fieldRef = (inst as? ReferenceInstruction)?.reference as? FieldReference ?: continue
            if (fieldRef.definingClass != epv.type || fieldRef.type != "Landroid/view/View;") {
                continue
            }
            val followedBySetVis = (idx + 1..minOf(instrs.lastIndex, idx + 3)).any { nextIdx ->
                val ref = (instrs[nextIdx] as? ReferenceInstruction)?.reference as? MethodReference
                ref?.definingClass == "Landroid/view/View;" && ref.name == "setVisibility"
            }
            if (followedBySetVis) {
                matches += fieldRef
            }
        }
        val distinct = matches.distinctBy { "${it.name}:${it.type}" }
        check(distinct.size == 2) {
            "Expected exactly 2 social View fields after WechatMoments.NAME in ExportPictureView.prepareAdapter, found ${distinct.size}: ${
                distinct.map { it.name }
            }"
        }
        val f0 = epv.resolveSingleField("ExportPictureView.socialHeaderField") {
            it.name == distinct[0].name && it.type == distinct[0].type
        }
        val f1 = epv.resolveSingleField("ExportPictureView.socialDividerField") {
            it.name == distinct[1].name && it.type == distinct[1].type
        }
        return f0 to f1
    }

    internal fun resolveExportPictureSocialHeaderField(epv: ClassDef): Field =
        resolveExportPictureSocialDividerFields(epv).first

    internal fun resolveExportPictureSocialDividerField(epv: ClassDef): Field =
        resolveExportPictureSocialDividerFields(epv).second

    internal fun resolveExportPictureCallbackField(epv: ClassDef, callbackInterface: ClassDef): Field =
        epv.resolveSingleField("ExportPictureView.callbackField") { f ->
            !AccessFlags.STATIC.isSet(f.accessFlags) && f.type == callbackInterface.type
        }

    internal fun resolveExportPictureFormatField(epv: ClassDef): Field {
        val setter = epv.resolveSingleMethod("ExportPictureView.setFormat") { m ->
            !AccessFlags.STATIC.isSet(m.accessFlags) &&
                !AccessFlags.SYNTHETIC.isSet(m.accessFlags) &&
                m.returnType == "V" &&
                m.paramsList() == listOf("I")
        }
        val writes = setter.instructionsOrEmpty().mapNotNull { inst ->
            if (inst.opcode != Opcode.IPUT) return@mapNotNull null
            val ref = (inst as? ReferenceInstruction)?.reference as? FieldReference ?: return@mapNotNull null
            if (ref.definingClass == epv.type && ref.type == "I") ref else null
        }.distinctBy { "${it.name}:${it.type}" }
        check(writes.size == 1) {
            "Expected exactly 1 int IPUT in ${epv.type}->${setter.name}(I)V, found ${writes.size}: ${
                writes.map { it.name }
            }"
        }
        return epv.resolveSingleField("ExportPictureView.formatField") {
            it.name == writes[0].name && it.type == writes[0].type
        }
    }

    internal fun resolveExportPictureWatermarkSwitchField(epv: ClassDef): Field =
        epv.resolveSingleField("ExportPictureView.watermarkSwitchField") { f ->
            !AccessFlags.STATIC.isSet(f.accessFlags) &&
                f.type == "Landroidx/appcompat/widget/SwitchCompat;"
        }

    internal fun resolveExportPictureShareProgressField(epv: ClassDef): Field {
        val onCreateView = epv.resolveSingleMethod("ExportPictureView.onCreateView") { m ->
            m.name == "onCreateView" &&
                m.returnType == "Landroid/view/View;" &&
                m.paramsList() == listOf(
                    "Landroid/view/LayoutInflater;",
                    "Landroid/view/ViewGroup;",
                    "Landroid/os/Bundle;",
                )
        }
        val instrs = onCreateView.instructionsOrEmpty()
        val getBoolIdx = instrs.indexOfFirst { inst ->
            val ref = (inst as? ReferenceInstruction)?.reference as? MethodReference ?: return@indexOfFirst false
            ref.definingClass == "Landroid/os/Bundle;" &&
                ref.name == "getBoolean" &&
                ref.parameterTypes.map { it.toString() } == listOf("Ljava/lang/String;", "Z")
        }
        check(getBoolIdx >= 0) {
            "Bundle.getBoolean not found in ExportPictureView.onCreateView"
        }
        val writes = (getBoolIdx + 1..minOf(instrs.lastIndex, getBoolIdx + 3)).mapNotNull { idx ->
            val inst = instrs[idx]
            if (inst.opcode != Opcode.IPUT_BOOLEAN) return@mapNotNull null
            val ref = (inst as? ReferenceInstruction)?.reference as? FieldReference ?: return@mapNotNull null
            if (ref.definingClass == epv.type && ref.type == "Z") ref else null
        }.distinctBy { "${it.name}:${it.type}" }
        check(writes.size == 1) {
            "Expected exactly 1 boolean IPUT_BOOLEAN after Bundle.getBoolean in ExportPictureView.onCreateView, found ${writes.size}"
        }
        return epv.resolveSingleField("ExportPictureView.shareProgressField") {
            it.name == writes[0].name && it.type == writes[0].type
        }
    }

    internal fun resolveExportPictureSizeCallbackField(epv: ClassDef, sizeCallbackClass: ClassDef): Field =
        epv.resolveSingleField("ExportPictureView.sizeCallbackField") { f ->
            !AccessFlags.STATIC.isSet(f.accessFlags) && f.type == sizeCallbackClass.type
        }

    internal fun resolveExportPictureSizeTypeField(epv: ClassDef): Field =
        epv.resolveSingleField("ExportPictureView.sizeTypeField") { f ->
            !AccessFlags.STATIC.isSet(f.accessFlags) &&
                f.type == "Lnet/huanci/paintlib/enums/ExportPictureSizeType;"
        }

    internal fun resolveExportPictureCustomSizeFields(epv: ClassDef): Pair<Field, Field> {
        val setter = epv.resolveSingleMethod("ExportPictureView.setSizeParams") { m ->
            !AccessFlags.STATIC.isSet(m.accessFlags) &&
                m.returnType == "V" &&
                m.paramsList() == listOf("Lnet/huanci/paintlib/enums/ExportPictureSizeType;", "I", "I")
        }
        val writes = setter.instructionsOrEmpty().mapNotNull { inst ->
            if (inst.opcode != Opcode.IPUT) return@mapNotNull null
            val ref = (inst as? ReferenceInstruction)?.reference as? FieldReference ?: return@mapNotNull null
            if (ref.definingClass == epv.type && ref.type == "I") ref else null
        }
        check(writes.size == 2) {
            "Expected exactly 2 int IPUT instructions in ${epv.type}->${setter.name}, found ${writes.size}: ${
                writes.map { it.name }
            }"
        }
        val widthField = epv.resolveSingleField("ExportPictureView.customWidthField") {
            it.name == writes[0].name && it.type == writes[0].type
        }
        val heightField = epv.resolveSingleField("ExportPictureView.customHeightField") {
            it.name == writes[1].name && it.type == writes[1].type
        }
        return widthField to heightField
    }

    internal fun resolveExportPictureCustomWidthField(epv: ClassDef): Field =
        resolveExportPictureCustomSizeFields(epv).first

    internal fun resolveExportPictureCustomHeightField(epv: ClassDef): Field =
        resolveExportPictureCustomSizeFields(epv).second

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

    internal fun resolveGlobalDataClass(classes: Iterable<ClassDef>): ClassDef =
        classes.resolveSingleClass("GlobalData") {
            it.sourceFile == "GlobalData.java" && !it.type.contains('$')
        }

    internal fun resolveGlobalDataUserField(globalData: ClassDef): Field =
        globalData.resolveSingleField("GlobalData.loginUser") { f ->
            AccessFlags.STATIC.isSet(f.accessFlags) &&
                f.type == "Lnet/huanci/hsjpro/model/LoginUser;"
        }

    internal fun resolveFileHelperClass(classes: Iterable<ClassDef>): ClassDef =
        classes.resolveSingleClass("FileHelper") {
            it.sourceFile == "FileHelper.java" &&
                it.type.startsWith("Lnet/huanci/hsjpro/utils/") &&
                !it.type.contains('$')
        }

    internal fun resolveFileHelperGetDraftDir(fileHelper: ClassDef): Method =
        fileHelper.resolveSingleMethod("FileHelper.getDraftDir") { m ->
            AccessFlags.STATIC.isSet(m.accessFlags) &&
                m.returnType == "Ljava/lang/String;" &&
                m.paramsList() == listOf("Z", "Ljava/lang/String;") &&
                m.callsMethod(definingClass = "Landroid/app/Application;", name = "getExternalFilesDir")
        }

    internal fun resolveFileHelperExportDraftsField(
        fileHelper: ClassDef,
        xorKey: String = "hsjproapp",
    ): FieldReference {
        val clinit = fileHelper.resolveSingleMethod("FileHelper.<clinit>") { m ->
            m.name == "<clinit>" && m.returnType == "V" && m.parameterTypes.isEmpty()
        }
        var pendingString: String? = null
        val matches = mutableListOf<FieldReference>()
        for (inst in clinit.instructionsOrEmpty()) {
            when (inst.opcode) {
                Opcode.CONST_STRING, Opcode.CONST_STRING_JUMBO -> {
                    val lit = ((inst as? ReferenceInstruction)?.reference as? StringReference)?.string
                    pendingString = if (lit != null) {
                        decryptStringFogLiteralOrNull(lit, xorKey) ?: lit
                    } else {
                        null
                    }
                }
                Opcode.SPUT_OBJECT -> {
                    val fieldRef = (inst as? ReferenceInstruction)?.reference as? FieldReference
                    if (fieldRef != null &&
                        fieldRef.definingClass == fileHelper.type &&
                        fieldRef.type == "Ljava/lang/String;" &&
                        pendingString == "exportDrafts"
                    ) {
                        matches += fieldRef
                    }
                    pendingString = null
                }
                else -> {}
            }
        }
        val distinct = matches.distinctBy { "${it.name}:${it.type}" }
        check(distinct.size == 1) {
            "Expected exactly 1 FileHelper static field initialized to 'exportDrafts', found ${distinct.size}: ${
                distinct.map { it.name }
            }"
        }
        return distinct[0]
    }

    internal fun resolveFileHelperGetExportDraftsDir(
        fileHelper: ClassDef,
        xorKey: String = "hsjproapp",
    ): Method {
        val exportField = resolveFileHelperExportDraftsField(fileHelper, xorKey)
        val fieldGetterNames = fileHelper.methods.filter { m ->
            AccessFlags.STATIC.isSet(m.accessFlags) &&
                m.returnType == "Ljava/lang/String;" &&
                m.parameterTypes.isEmpty() &&
                m.instructionsOrEmpty().none { inst ->
                    (inst as? ReferenceInstruction)?.reference is MethodReference
                } &&
                m.instructionsOrEmpty().any { inst ->
                    inst.opcode == Opcode.SGET_OBJECT &&
                        ((inst as? ReferenceInstruction)?.reference as? FieldReference)?.let {
                            it.definingClass == fileHelper.type && it.name == exportField.name
                        } == true
                }
        }.map { it.name }.toSet()

        return fileHelper.resolveSingleMethod("FileHelper.getExportDraftsDir") { m ->
            if (!AccessFlags.STATIC.isSet(m.accessFlags) ||
                m.returnType != "Ljava/lang/String;" ||
                m.parameterTypes.isNotEmpty()
            ) {
                return@resolveSingleMethod false
            }
            if (!m.callsMethod(
                    definingClass = "Landroid/os/Environment;",
                    name = "getExternalStorageDirectory",
                )
            ) {
                return@resolveSingleMethod false
            }
            val instrs = m.instructionsOrEmpty()
            val readsFieldDirectly = instrs.any { inst ->
                inst.opcode == Opcode.SGET_OBJECT &&
                    ((inst as? ReferenceInstruction)?.reference as? FieldReference)?.let {
                        it.definingClass == fileHelper.type && it.name == exportField.name
                    } == true
            }
            val callsFieldGetter = instrs.any { inst ->
                val ref = (inst as? ReferenceInstruction)?.reference as? MethodReference ?: return@any false
                ref.definingClass == fileHelper.type &&
                    ref.name in fieldGetterNames &&
                    ref.parameterTypes.isEmpty() &&
                    ref.returnType == "Ljava/lang/String;"
            }
            readsFieldDirectly || callsFieldGetter
        }
    }

    internal fun resolvePrefUtilClass(classes: Iterable<ClassDef>): ClassDef =
        classes.resolveSingleClass("PrefUtil") {
            it.sourceFile == "SharePrefrenceHelper.java" && !it.type.contains('$')
        }

    internal fun resolvePrefUtilPutBoolean(prefUtil: ClassDef): Method =
        prefUtil.resolveSingleMethod("PrefUtil.putBoolean") { m ->
            AccessFlags.STATIC.isSet(m.accessFlags) &&
                m.returnType == "V" &&
                m.paramsList() == listOf("Ljava/lang/String;", "Z")
        }

    internal fun resolveRxFileToolClass(classes: Iterable<ClassDef>): ClassDef =
        classes.resolveSingleClass("RxFileTool") {
            it.sourceFile == "RxFileTool.java" && !it.type.contains('$')
        }

    internal fun resolveRxFileToolGetUriForFile(rxFileTool: ClassDef): Method =
        rxFileTool.resolveSingleMethod("RxFileTool.getUriForFile") { m ->
            AccessFlags.STATIC.isSet(m.accessFlags) &&
                m.returnType == "Landroid/net/Uri;" &&
                m.paramsList() == listOf("Landroid/content/Context;", "Ljava/io/File;") &&
                m.usesStringLiteral(".fileprovider")
        }

    internal fun resolveDrawApplicationClass(classes: Iterable<ClassDef>): ClassDef =
        classes.resolveSingleClass("DrawApplication") {
            it.type == "Lnet/huanci/hsjpro/DrawApplication;"
        }

    internal fun resolveDrawApplicationGetInstance(drawApp: ClassDef): Method =
        drawApp.resolveSingleMethod("DrawApplication.getInstance") { m ->
            AccessFlags.STATIC.isSet(m.accessFlags) &&
                m.parameterTypes.isEmpty() &&
                m.returnType == drawApp.type
        }

    internal fun resolveBrushPkgManagerGetInstance(mgr: ClassDef): Method =
        mgr.resolveSingleMethod("BrushPkgManager.getInstance") { m ->
            AccessFlags.STATIC.isSet(m.accessFlags) &&
                m.parameterTypes.isEmpty() &&
                m.returnType == mgr.type
        }

    internal fun resolveBrushPkgManagerInit(mgr: ClassDef): Method =
        mgr.resolveSingleMethod("BrushPkgManager.init") { m ->
            !AccessFlags.STATIC.isSet(m.accessFlags) &&
                m.returnType == "V" &&
                m.paramsList() == listOf("Z") &&
                m.callsMethod(definingClass = "Landroid/content/Context;", name = "getAssets")
        }

    internal fun resolveBrushPkgManagerReload(mgr: ClassDef): Method =
        mgr.resolveSingleMethod("BrushPkgManager.reload") { m ->
            !AccessFlags.STATIC.isSet(m.accessFlags) &&
                m.returnType == "V" &&
                m.paramsList() == listOf("Z") &&
                m.callsMethod(
                    definingClass = "Lnet/huanci/paintlib/model/BrushModel\$BrushDiffs;",
                    name = "parseFrom",
                )
        }

    internal fun resolveBrushPkgManagerListField(mgr: ClassDef): Field {
        val getList = resolveBrushPkgManagerGetList(mgr)
        val instrs = getList.instructionsOrEmpty()
        check(instrs.isNotEmpty() && instrs[0].opcode == Opcode.IF_EQZ) {
            "Expected IF_EQZ entry branch in ${mgr.type}->${getList.name}(Z)Ljava/util/List;"
        }
        val listReads = instrs.mapNotNull { inst ->
            if (inst.opcode != Opcode.IGET_OBJECT) return@mapNotNull null
            val ref = (inst as? ReferenceInstruction)?.reference as? FieldReference ?: return@mapNotNull null
            if (ref.definingClass == mgr.type && ref.type == "Ljava/util/List;") ref else null
        }.distinctBy { "${it.name}:${it.type}" }
        check(listReads.size == 2) {
            "Expected exactly 2 List fields in ${mgr.type}->${getList.name}(Z)Ljava/util/List;, found ${listReads.size}: ${
                listReads.map { it.name }
            }"
        }
        // Index 0 is read when p1 == true (buildin list); index 1 is read when p1 == false (default list).
        return mgr.resolveSingleField("BrushPkgManager.listField") {
            it.name == listReads[1].name && it.type == listReads[1].type
        }
    }

    internal fun resolveColorSpaceManagerClass(classes: Iterable<ClassDef>): ClassDef =
        classes.resolveSingleClass("ColorSpaceManager") {
            it.sourceFile == "ColorSpaceManager.java" && !it.type.contains('$')
        }

    internal fun resolveColorSpaceManagerGetInstance(colorMgr: ClassDef): Method =
        colorMgr.resolveSingleMethod("ColorSpaceManager.getInstance") { m ->
            AccessFlags.STATIC.isSet(m.accessFlags) &&
                m.parameterTypes.isEmpty() &&
                m.returnType == colorMgr.type
        }

    internal fun resolveColorSpaceManagerInit(colorMgr: ClassDef): Method =
        colorMgr.resolveSingleMethod("ColorSpaceManager.init") { m ->
            m.name != "<init>" &&
                !AccessFlags.STATIC.isSet(m.accessFlags) &&
                m.returnType == "V" &&
                m.parameterTypes.isEmpty() &&
                m.callsMethod(
                    definingClass = "Ljava/util/concurrent/Executors;",
                    name = "newSingleThreadExecutor",
                )
        }

    internal fun resolveHistoryBrushManagerClass(classes: Iterable<ClassDef>): ClassDef =
        classes.resolveSingleClass("HistoryBrushManager") {
            it.sourceFile == "HistoryBrushManager.java" && !it.type.contains('$')
        }

    internal fun resolveHistoryBrushManagerGetInstance(histMgr: ClassDef): Method =
        histMgr.resolveSingleMethod("HistoryBrushManager.getInstance") { m ->
            AccessFlags.STATIC.isSet(m.accessFlags) &&
                m.parameterTypes.isEmpty() &&
                m.returnType == histMgr.type
        }

    internal fun resolveHistoryBrushManagerInit(histMgr: ClassDef): Method =
        histMgr.resolveSingleMethod("HistoryBrushManager.init") { m ->
            m.name != "<init>" &&
                !AccessFlags.STATIC.isSet(m.accessFlags) &&
                m.returnType == "V" &&
                m.parameterTypes.isEmpty() &&
                m.callsMethod(
                    definingClass = "Lnet/huanci/paintlib/model/BrushPackage;",
                    name = "<init>",
                )
        }

    internal fun resolvePaintUserIdField(paintActivity: ClassDef): Field {
        val setActiContentView = paintActivity.resolveSingleMethod("PaintActivity.setActiContentView") { m ->
            m.name == "setActiContentView" && m.returnType == "V" && m.parameterTypes.isEmpty()
        }
        val instrs = setActiContentView.instructionsOrEmpty()
        val getIdIdx = instrs.indexOfFirst { inst ->
            val ref = (inst as? ReferenceInstruction)?.reference as? MethodReference ?: return@indexOfFirst false
            ref.definingClass == "Lnet/huanci/hsjpro/model/result/user/BaseUser;" &&
                ref.name == "getId" &&
                ref.returnType == "I" &&
                ref.parameterTypes.isEmpty()
        }
        check(getIdIdx >= 0) {
            "BaseUser.getId() not found in PaintActivity.setActiContentView()V"
        }
        val writes = (getIdIdx + 1..minOf(instrs.lastIndex, getIdIdx + 3)).mapNotNull { idx ->
            val inst = instrs[idx]
            if (inst.opcode != Opcode.IPUT) return@mapNotNull null
            val ref = (inst as? ReferenceInstruction)?.reference as? FieldReference ?: return@mapNotNull null
            if (ref.definingClass == paintActivity.type && ref.type == "I") ref else null
        }.distinctBy { "${it.name}:${it.type}" }
        check(writes.size == 1) {
            "Expected exactly 1 int IPUT after BaseUser.getId() in PaintActivity.setActiContentView, found ${writes.size}"
        }
        return paintActivity.resolveSingleField("PaintActivity.userIdField") {
            it.name == writes[0].name && it.type == writes[0].type
        }
    }

    internal fun resolvePaintOutCloseViewField(paintActivity: ClassDef): Field =
        paintActivity.resolveSingleField("PaintActivity.outCloseViewField") { f ->
            !AccessFlags.STATIC.isSet(f.accessFlags) &&
                f.type == "Lnet/huanci/hsjpro/views/OutCloseView;"
        }

    internal fun resolvePaintXuanQuViewField(paintActivity: ClassDef): Field =
        paintActivity.resolveSingleField("PaintActivity.xuanQuViewField") { f ->
            !AccessFlags.STATIC.isSet(f.accessFlags) &&
                f.type == "Lnet/huanci/hsjpro/paint/views/XuanQuSettingView;"
        }

    internal fun resolvePaintXuanQuMoveViewField(paintActivity: ClassDef): Field =
        paintActivity.resolveSingleField("PaintActivity.xuanQuMoveViewField") { f ->
            !AccessFlags.STATIC.isSet(f.accessFlags) &&
                f.type == "Lnet/huanci/hsjpro/paint/views/XuanquMoveView;"
        }

    internal fun resolvePaintLeftSettingViewField(paintActivity: ClassDef): Field =
        paintActivity.resolveSingleField("PaintActivity.leftSettingViewField") { f ->
            !AccessFlags.STATIC.isSet(f.accessFlags) &&
                f.type == "Lnet/huanci/hsjpro/paint/views/PaintLeftSettingView;"
        }

    internal fun resolvePaintCanvasViewField(paintActivity: ClassDef): Field =
        paintActivity.resolveSingleField("PaintActivity.canvasViewField") { f ->
            !AccessFlags.STATIC.isSet(f.accessFlags) &&
                f.type == "Lnet/huanci/paintlib/views/PaintView;"
        }

    internal fun resolvePaintSwitchLeftToolMethod(paintActivity: ClassDef): Method =
        paintActivity.resolveSingleMethod("PaintActivity.switchLeftTool") { m ->
            !AccessFlags.STATIC.isSet(m.accessFlags) &&
                !AccessFlags.SYNTHETIC.isSet(m.accessFlags) &&
                m.returnType == "V" &&
                m.paramsList() == listOf(
                    "Lnet/huanci/hsjpro/enums/PaintLeftItemType;",
                    "Ljava/lang/Runnable;",
                )
        }

    private fun resolvePaintViewIsLayerSelectAreaEmpty(classes: Iterable<ClassDef>): Method {
        val paintView = classes.resolveSingleClass("PaintView") {
            it.type == "Lnet/huanci/paintlib/views/PaintView;"
        }
        return paintView.resolveSingleMethod("PaintView.isLayerSelectAreaEmpty") { m ->
            m.name == "isLayerSelectAreaEmpty" &&
                m.returnType == "Z" &&
                m.paramsList() == listOf("I")
        }
    }

    internal fun resolvePaintParamsLayerStateSparseField(classes: Iterable<ClassDef>): FieldReference {
        val method = resolvePaintViewIsLayerSelectAreaEmpty(classes)
        val matches = method.instructionsOrEmpty().mapNotNull { inst ->
            if (inst.opcode != Opcode.IGET_OBJECT) return@mapNotNull null
            val ref = (inst as? ReferenceInstruction)?.reference as? FieldReference ?: return@mapNotNull null
            if (ref.definingClass == "Lnet/huanci/paintlib/views/PAINT_PARAMS;" &&
                ref.type == "Landroid/util/SparseArray;"
            ) {
                ref
            } else {
                null
            }
        }.distinctBy { "${it.definingClass}->${it.name}:${it.type}" }
        check(matches.size == 1) {
            "Expected exactly 1 PAINT_PARAMS SparseArray field in PaintView.isLayerSelectAreaEmpty, found ${matches.size}"
        }
        return matches[0]
    }

    internal fun resolveLayerStateSelectionRectField(classes: Iterable<ClassDef>): FieldReference {
        val method = resolvePaintViewIsLayerSelectAreaEmpty(classes)
        val matches = method.instructionsOrEmpty().mapNotNull { inst ->
            if (inst.opcode != Opcode.IGET_OBJECT) return@mapNotNull null
            val ref = (inst as? ReferenceInstruction)?.reference as? FieldReference ?: return@mapNotNull null
            if (ref.type == "Landroid/graphics/Rect;") ref else null
        }.distinctBy { "${it.definingClass}->${it.name}:${it.type}" }
        check(matches.size == 1) {
            "Expected exactly 1 Rect field in PaintView.isLayerSelectAreaEmpty, found ${matches.size}"
        }
        return matches[0]
    }

    internal fun resolveLayerStateClass(classes: Iterable<ClassDef>): ClassDef {
        val rectRef = resolveLayerStateSelectionRectField(classes)
        return classes.resolveSingleClass("RenderAssist") {
            it.sourceFile == "RenderAssist.java" &&
                !it.type.contains('$') &&
                it.type == rectRef.definingClass
        }
    }

    internal fun resolveNavigatorTouchRunnableTargets(
        classes: Iterable<ClassDef>,
    ): NavigatorTouchRunnableTargets {
        val candidates = classes
            .filter { it.type.startsWith("Lnet/huanci/paintlib/views/NavigatorView\$") }
            .flatMap { cls ->
                cls.methods.filter { m ->
                    m.name == "run" &&
                        m.returnType == "V" &&
                        m.parameterTypes.isEmpty() &&
                        m.callsMethod(definingClass = "Ljava/lang/Math;", name = "atan2") &&
                        m.callsMethod(
                            definingClass = "Lnet/huanci/paintlib/views/NavigatorView;",
                            name = "access\$1100",
                            returnType = "Z",
                        ) &&
                        m.callsMethod(
                            definingClass = "Lnet/huanci/paintlib/views/NavigatorView;",
                            name = "access\$1702",
                            returnType = "F",
                        )
                }.map { m -> cls to m }
            }
        check(candidates.size == 1) {
            "Expected exactly 1 NavigatorView runnable calling Math.atan2 and access\$1702, found ${candidates.size}"
        }
        val (runnableClass, runMethod) = candidates[0]
        val initMethod = runnableClass.resolveSingleMethod("${runnableClass.type}.<init>") { m ->
            val p = m.paramsList()
            m.name == "<init>" &&
                m.returnType == "V" &&
                p.size == 6 &&
                p[0] == "Lnet/huanci/paintlib/views/NavigatorView;" &&
                p[1] !in PRIMITIVE_SIGNATURES &&
                p.drop(2) == listOf("F", "F", "F", "F")
        }
        val orderedFields = initMethod.instructionsOrEmpty().mapNotNull { inst ->
            if (inst.opcode != Opcode.IPUT_OBJECT && inst.opcode != Opcode.IPUT) {
                return@mapNotNull null
            }
            val ref = (inst as? ReferenceInstruction)?.reference as? FieldReference ?: return@mapNotNull null
            if (ref.definingClass == runnableClass.type) ref else null
        }
        check(
            orderedFields.size == 6 &&
                orderedFields[0].type == "Lnet/huanci/paintlib/views/NavigatorView;" &&
                orderedFields[1].type !in PRIMITIVE_SIGNATURES &&
                orderedFields.drop(2).all { it.type == "F" },
        ) {
            "Expected 6 ordered fields (NavigatorView, Object, F, F, F, F) in ${runnableClass.type}.<init>, found ${
                orderedFields.map { "${it.name}:${it.type}" }
            }"
        }
        return NavigatorTouchRunnableTargets(
            runnableClass = runnableClass,
            runMethod = runMethod,
            navViewField = orderedFields[0],
            gestureTokenField = orderedFields[1],
            x0Field = orderedFields[2],
            y0Field = orderedFields[3],
            x1Field = orderedFields[4],
            y1Field = orderedFields[5],
        )
    }

    internal fun resolvePsdHelperClass(classes: Iterable<ClassDef>): ClassDef =
        classes.resolveSingleClass("PsdHelper") {
            it.sourceFile == "PsdHelper.java" && !it.type.contains('$')
        }

    internal fun resolveShareBitmapWrapperClass(classes: Iterable<ClassDef>): ClassDef =
        classes.resolveSingleClass("ShareBitmapWrapper") {
            it.sourceFile == "ShareBitmapWrapper.java" && !it.type.contains('$')
        }

    internal fun resolveLocalImageActivityClass(classes: Iterable<ClassDef>): ClassDef =
        classes.resolveSingleClass("LocalImageActivity") {
            it.type == "Lnet/huanci/hsjpro/activities/LocalImageActivity;"
        }

    internal fun resolveLocalImageShowCloudMethod(
        classes: Iterable<ClassDef>,
        localImageActivity: ClassDef,
    ): Method {
        val pagerAdapterClasses = classes.filter { cls ->
            cls.type.startsWith("Lnet/huanci/hsjpro/activities/LocalImageActivity\$") &&
                cls.methods.any { m -> m.name == "getCount" && m.returnType == "I" && m.parameterTypes.isEmpty() }
        }
        check(pagerAdapterClasses.size == 1) {
            "Expected 1 LocalImageActivity FragmentPagerAdapter with getCount()I, found ${pagerAdapterClasses.size}"
        }
        val getCountMethod = pagerAdapterClasses[0].resolveSingleMethod("LocalImageActivity\$Adapter.getCount") {
            it.name == "getCount" && it.returnType == "I" && it.parameterTypes.isEmpty()
        }
        val calledShowCloudNames = getCountMethod.instructionsOrEmpty().mapNotNull { inst ->
            val ref = (inst as? ReferenceInstruction)?.reference as? MethodReference ?: return@mapNotNull null
            if (ref.definingClass == localImageActivity.type &&
                ref.returnType == "Z" &&
                ref.parameterTypes.map { p -> p.toString() } == listOf(localImageActivity.type)
            ) {
                ref.name
            } else {
                null
            }
        }.distinct()
        check(calledShowCloudNames.size == 1) {
            "Expected 1 static (LocalImageActivity)Z helper called by getCount()I, found $calledShowCloudNames"
        }
        val targetName = calledShowCloudNames[0]
        return localImageActivity.resolveSingleMethod("LocalImageActivity.showCloudMethod") { m ->
            AccessFlags.STATIC.isSet(m.accessFlags) &&
                m.name == targetName &&
                m.returnType == "Z" &&
                m.paramsList() == listOf(localImageActivity.type)
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

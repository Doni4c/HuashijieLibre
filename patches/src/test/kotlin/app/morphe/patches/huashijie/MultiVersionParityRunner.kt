package app.morphe.patches.huashijie

import app.morphe.patches.huashijie.core.MultiVersionResolvers
import com.android.tools.smali.dexlib2.DexFileFactory
import com.android.tools.smali.dexlib2.Opcodes
import java.io.File

private fun assertParity(label: String, actual: String, expected: String) {
    check(actual == expected) {
        "Target parity mismatch for $label: expected '$expected' but resolved '$actual'"
    }
    println("PARITY OK [$label] -> $actual")
}

fun main() {
    val apk = File("base.apk")
    require(apk.exists()) { "base.apk not found at ${apk.absolutePath}" }

    val container = DexFileFactory.loadDexContainer(apk, Opcodes.getDefault())
    val allClasses = container.dexEntryNames.flatMap { name ->
        container.getEntry(name)?.dexFile?.classes.orEmpty()
    }

    val assetsUtil = MultiVersionResolvers.resolveAssetsUtilClass(allClasses)
    assertParity("AssetsUtil.class", assetsUtil.type, "LOooOOO/OooO00o;")
    assertParity(
        "AssetsUtil.getString",
        MultiVersionResolvers.resolveAssetsUtilGetString(assetsUtil).name,
        "OooO0o",
    )
    assertParity(
        "AssetsUtil.getStringArray",
        MultiVersionResolvers.resolveAssetsUtilGetStringArray(assetsUtil).name,
        "OooO0oO",
    )

    val stringFog = MultiVersionResolvers.resolveStringFogClass(allClasses)
    assertParity("StringFog.class", stringFog.type, "Lo00OO00O/OooO0o;")
    assertParity(
        "StringFog.decrypt",
        MultiVersionResolvers.resolveStringFogDecrypt(stringFog).name,
        "OooO00o",
    )

    val localeUtils = MultiVersionResolvers.resolveLanguageLocaleUtilsClass(allClasses)
    assertParity("LanguageLocaleUtils.class", localeUtils.type, "Lnet/huanci/hsjpro/utils/o00OOO00;")
    assertParity(
        "LanguageLocaleUtils.applyLocale",
        MultiVersionResolvers.resolveLanguageLocaleApply(localeUtils).name,
        "OooO00o",
    )

    val bubblePopup = MultiVersionResolvers.resolveBubblePopupClass(allClasses)
    assertParity("BubblePopupWindow.class", bubblePopup.type, "Lnet/huanci/hsjpro/views/bubbleview/OooO0OO;")
    assertParity(
        "BubblePopupWindow.<init>",
        MultiVersionResolvers.resolveBubblePopupInit(bubblePopup).name,
        "<init>",
    )
    assertParity(
        "BubblePopupWindow.showAnchored",
        MultiVersionResolvers.resolveBubblePopupShowAnchored(bubblePopup).name,
        "OooOo0o",
    )
    assertParity(
        "BubblePopupWindow.showAtLocationCustom",
        MultiVersionResolvers.resolveBubblePopupShowAtLocationCustom(bubblePopup).name,
        "OooOoO0",
    )

    val switchHelper = MultiVersionResolvers.resolveAppModuleSwitchHelperClass(allClasses)
    assertParity("AppModuleSwitchHelper.class", switchHelper.type, "Lo00OO00O/OooO00o;")
    assertParity(
        "AppModuleSwitchHelper.INSTANCE",
        MultiVersionResolvers.resolveAppModuleSwitchSingletonField(switchHelper).name,
        "OooO00o",
    )

    assertParity(
        "CommonDialog\$Builder.create",
        MultiVersionResolvers.resolveCommonDialogBuilderCreate(allClasses).name,
        "OooOO0",
    )

    val paintActivity = MultiVersionResolvers.resolvePaintActivityClass(allClasses)
    assertParity(
        "PaintActivity.popup1",
        MultiVersionResolvers.resolvePaintPopup1Menu(paintActivity, bubblePopup.type).name,
        "oO000Oo",
    )
    assertParity(
        "PaintActivity.popup2",
        MultiVersionResolvers.resolvePaintPopup2Brush(paintActivity, bubblePopup.type).name,
        "oO00o00o",
    )
    assertParity(
        "PaintActivity.popup3",
        MultiVersionResolvers.resolvePaintPopup3Canvas(paintActivity, bubblePopup.type).name,
        "oO00OooO",
    )
    assertParity(
        "PaintActivity.popup4",
        MultiVersionResolvers.resolvePaintPopup4Ref(paintActivity, bubblePopup.type).name,
        "oO0O0OoO",
    )
    assertParity(
        "PaintActivity.seekbarsViewField",
        MultiVersionResolvers.resolvePaintSeekbarsViewField(paintActivity).name,
        "Oooo0o0",
    )
    assertParity(
        "PaintActivity.shareCallback",
        MultiVersionResolvers.resolveActivityShareCallback(paintActivity).name,
        "o00000oO",
    )

    val replayActivity = MultiVersionResolvers.resolveSingleClassFromTest(
        allClasses,
        "Lnet/huanci/hsjpro/activities/ReplayActivity;",
    )
    assertParity(
        "ReplayActivity.shareCallback",
        MultiVersionResolvers.resolveActivityShareCallback(replayActivity).name,
        "o00000oO",
    )

    assertParity(
        "BaseFragment.hookTarget",
        MultiVersionResolvers.resolveBaseFragmentHookTarget(allClasses).name,
        "OooOoo",
    )

    val mf2 = MultiVersionResolvers.resolveMainFragment2Class(allClasses)
    assertParity(
        "MainFragment2.bindView",
        MultiVersionResolvers.resolveMainFragment2BindView(mf2).name,
        "OooOoo",
    )
    assertParity(
        "MainFragment2.inflate",
        MultiVersionResolvers.resolveMainFragment2Inflate(mf2).name,
        "o00O0Ooo",
    )
    assertParity(
        "MainFragment2.updateCloud",
        MultiVersionResolvers.resolveMainFragment2UpdateCloud(mf2).name,
        "o000o000",
    )

    val searchHelper = MultiVersionResolvers.resolveSearchHelperClass(allClasses)
    assertParity("SearchHelper.class", searchHelper.type, "Lnet/huanci/hsjpro/paint/views/o00O0O00;")
    assertParity(
        "SearchHelper.showDropdown",
        MultiVersionResolvers.resolveSearchHelperShowDropdown(searchHelper).name,
        "OooO",
    )

    assertParity(
        "FastKeyFragment.showDropdown",
        MultiVersionResolvers.resolveFastKeyFragmentShowDropdown(allClasses).name,
        "o0OO00O",
    )

    val flash = MultiVersionResolvers.resolveFlashActivityClass(allClasses)
    assertParity(
        "FlashActivity.initTelemetry",
        MultiVersionResolvers.resolveFlashTelemetryInit(flash).name,
        "o000o0o0",
    )
    assertParity(
        "FlashActivity.splashJump",
        MultiVersionResolvers.resolveFlashSplashJump(flash).name,
        "o000Oo00",
    )
    assertParity(
        "FlashActivity.canLoadAd",
        MultiVersionResolvers.resolveFlashCanLoadAd(flash).name,
        "o000OOo0",
    )
    assertParity(
        "DrawApplication.watchdogField",
        MultiVersionResolvers.resolveDrawApplicationWatchdogField(allClasses).name,
        "OooO0o",
    )

    val chooseBrush = MultiVersionResolvers.resolveChooseBrushFragmentClass(allClasses)
    assertParity(
        "ChooseBrushFragment.createView",
        MultiVersionResolvers.resolveChooseBrushCreateView(chooseBrush).name,
        "Oooo000",
    )
    assertParity(
        "ChooseBrushFragment.canEdit",
        MultiVersionResolvers.resolveChooseBrushCanEdit(chooseBrush).name,
        "OoooOO0",
    )

    val brushMgr = MultiVersionResolvers.resolveBrushPkgManagerClass(allClasses)
    assertParity("BrushPkgManager.class", brushMgr.type, "Lnet/huanci/paintlib/manager/OooOOOO;")
    assertParity(
        "BrushPkgManager.getList",
        MultiVersionResolvers.resolveBrushPkgManagerGetList(brushMgr).name,
        "Oooo0oo",
    )

    assertParity(
        "ExportPictureView.prepareAdapter",
        MultiVersionResolvers.resolveExportPictureViewPrepareAdapter(allClasses).name,
        "Ooooo00",
    )
    assertParity(
        "ExportPictureView3.bindView",
        MultiVersionResolvers.resolveExportPictureView3BindView(allClasses).name,
        "OooooO0",
    )
    assertParity(
        "PAINT_PARAMS.timelapseWatermarkField",
        MultiVersionResolvers.resolveTimelapseWatermarkField(allClasses).name,
        "Oooo0OO",
    )

    val glRender = MultiVersionResolvers.resolveGlVideoWatermarkRenderFrame(allClasses)
    assertParity("EglConfigBase.class", glRender.definingClass, "Lo00o0o00/o000O0O0;")
    assertParity("EglConfigBase.renderFrame", glRender.name, "OooO0O0")

    println("ALL 32 STRUCTURAL TARGET PARITY CHECKS PASSED (1 TO 1 EXACT MATCH ON BASE.APK)")
}

private fun MultiVersionResolvers.resolveSingleClassFromTest(
    classes: Iterable<com.android.tools.smali.dexlib2.iface.ClassDef>,
    typeName: String,
): com.android.tools.smali.dexlib2.iface.ClassDef =
    classes.resolveSingleClass(typeName) { it.type == typeName }

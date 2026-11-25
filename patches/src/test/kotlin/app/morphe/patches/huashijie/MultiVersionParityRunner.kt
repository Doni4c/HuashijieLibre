package app.morphe.patches.huashijie

import app.morphe.patches.huashijie.core.MultiVersionResolvers
import com.android.tools.smali.dexlib2.DexFileFactory
import com.android.tools.smali.dexlib2.Opcodes
import java.io.File

private var parityChecks = 0

private fun assertParity(label: String, actual: String, expected: String) {
    check(actual == expected) {
        "Target parity mismatch for $label: expected '$expected' but resolved '$actual'"
    }
    parityChecks++
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

    val globalData = MultiVersionResolvers.resolveGlobalDataClass(allClasses)
    assertParity("GlobalData.class", globalData.type, "Lo00OO0o0/o0OOO0o;")
    assertParity(
        "GlobalData.userField",
        MultiVersionResolvers.resolveGlobalDataUserField(globalData).name,
        "OooO0oO",
    )

    val fileHelper = MultiVersionResolvers.resolveFileHelperClass(allClasses)
    assertParity("FileHelper.class", fileHelper.type, "Lnet/huanci/hsjpro/utils/o00O000o;")
    assertParity(
        "FileHelper.getDraftDir",
        MultiVersionResolvers.resolveFileHelperGetDraftDir(fileHelper).name,
        "Oooo0oo",
    )
    assertParity(
        "FileHelper.exportDraftsField",
        MultiVersionResolvers.resolveFileHelperExportDraftsField(fileHelper).name,
        "OooOo0",
    )
    assertParity(
        "FileHelper.getExportDraftsDir",
        MultiVersionResolvers.resolveFileHelperGetExportDraftsDir(fileHelper).name,
        "Oooo00o",
    )

    val prefUtil = MultiVersionResolvers.resolvePrefUtilClass(allClasses)
    assertParity("PrefUtil.class", prefUtil.type, "Lnet/huanci/hsjpro/utils/oo0oO0;")
    assertParity(
        "PrefUtil.putBoolean",
        MultiVersionResolvers.resolvePrefUtilPutBoolean(prefUtil).name,
        "o000",
    )

    val rxFileTool = MultiVersionResolvers.resolveRxFileToolClass(allClasses)
    assertParity("RxFileTool.class", rxFileTool.type, "Lo000OoOO/Oooo000;")
    assertParity(
        "RxFileTool.getUriForFile",
        MultiVersionResolvers.resolveRxFileToolGetUriForFile(rxFileTool).name,
        "Oooo00O",
    )

    val epv = MultiVersionResolvers.resolveExportPictureViewClass(allClasses)
    assertParity("ExportPictureView.class", epv.type, "Lnet/huanci/hsjpro/views/ExportPictureView;")
    assertParity(
        "ExportPictureView.socialRecyclerField",
        MultiVersionResolvers.resolveExportPictureSocialRecyclerField(epv).name,
        "OooOOO",
    )
    assertParity(
        "ExportPictureView.socialHeaderField",
        MultiVersionResolvers.resolveExportPictureSocialHeaderField(epv).name,
        "OooOOo0",
    )
    assertParity(
        "ExportPictureView.socialDividerField",
        MultiVersionResolvers.resolveExportPictureSocialDividerField(epv).name,
        "OooOOoo",
    )
    val epvCbInterface = MultiVersionResolvers.resolveExportPictureCallbackInterface(allClasses)
    assertParity(
        "ExportPictureView.callbackInterface",
        epvCbInterface.type,
        "Lnet/huanci/hsjpro/views/ExportPictureView\$OooO0o;",
    )
    val epvCbInvoke = MultiVersionResolvers.resolveExportPictureCallbackInvoke(epvCbInterface)
    assertParity("ExportPictureView.callbackInvoke", epvCbInvoke.name, "OooOoo0")
    val epvSizeCbClass = MultiVersionResolvers.resolveExportPictureSizeCallbackClass(allClasses, epvCbInvoke)
    assertParity(
        "ExportPictureView.sizeCallbackClass",
        epvSizeCbClass.type,
        "Lnet/huanci/hsjpro/views/ExportPictureView\$OooO0o\$OooO00o;",
    )
    assertParity(
        "ExportPictureView.callbackField",
        MultiVersionResolvers.resolveExportPictureCallbackField(epv, epvCbInterface).name,
        "OooOOOo",
    )
    assertParity(
        "ExportPictureView.formatField",
        MultiVersionResolvers.resolveExportPictureFormatField(epv).name,
        "OooOOOO",
    )
    assertParity(
        "ExportPictureView.watermarkSwitchField",
        MultiVersionResolvers.resolveExportPictureWatermarkSwitchField(epv).name,
        "OooOOo",
    )
    assertParity(
        "ExportPictureView.shareProgressField",
        MultiVersionResolvers.resolveExportPictureShareProgressField(epv).name,
        "OooOo0",
    )
    assertParity(
        "ExportPictureView.sizeCallbackField",
        MultiVersionResolvers.resolveExportPictureSizeCallbackField(epv, epvSizeCbClass).name,
        "OooOoO0",
    )
    assertParity(
        "ExportPictureView.sizeTypeField",
        MultiVersionResolvers.resolveExportPictureSizeTypeField(epv).name,
        "OooOo0O",
    )
    assertParity(
        "ExportPictureView.customWidthField",
        MultiVersionResolvers.resolveExportPictureCustomWidthField(epv).name,
        "OooOo0o",
    )
    assertParity(
        "ExportPictureView.customHeightField",
        MultiVersionResolvers.resolveExportPictureCustomHeightField(epv).name,
        "OooOo",
    )

    val drawApp = MultiVersionResolvers.resolveDrawApplicationClass(allClasses)
    assertParity("DrawApplication.class", drawApp.type, "Lnet/huanci/hsjpro/DrawApplication;")
    assertParity(
        "DrawApplication.getInstance",
        MultiVersionResolvers.resolveDrawApplicationGetInstance(drawApp).name,
        "OooO0oO",
    )

    assertParity(
        "BrushPkgManager.getInstance",
        MultiVersionResolvers.resolveBrushPkgManagerGetInstance(brushMgr).name,
        "OoooOOO",
    )
    assertParity(
        "BrushPkgManager.init",
        MultiVersionResolvers.resolveBrushPkgManagerInit(brushMgr).name,
        "OooooOo",
    )
    assertParity(
        "BrushPkgManager.reload",
        MultiVersionResolvers.resolveBrushPkgManagerReload(brushMgr).name,
        "OoooooO",
    )
    assertParity(
        "BrushPkgManager.listField",
        MultiVersionResolvers.resolveBrushPkgManagerListField(brushMgr).name,
        "OooO00o",
    )

    val colorMgr = MultiVersionResolvers.resolveColorSpaceManagerClass(allClasses)
    assertParity("ColorSpaceManager.class", colorMgr.type, "Lnet/huanci/paintlib/manager/o000oOoO;")
    assertParity(
        "ColorSpaceManager.getInstance",
        MultiVersionResolvers.resolveColorSpaceManagerGetInstance(colorMgr).name,
        "OooOoO",
    )
    assertParity(
        "ColorSpaceManager.init",
        MultiVersionResolvers.resolveColorSpaceManagerInit(colorMgr).name,
        "Oooo00o",
    )

    val histMgr = MultiVersionResolvers.resolveHistoryBrushManagerClass(allClasses)
    assertParity("HistoryBrushManager.class", histMgr.type, "Lnet/huanci/hsjpro/utils/o00OO00O;")
    assertParity(
        "HistoryBrushManager.getInstance",
        MultiVersionResolvers.resolveHistoryBrushManagerGetInstance(histMgr).name,
        "OooO0oO",
    )
    assertParity(
        "HistoryBrushManager.init",
        MultiVersionResolvers.resolveHistoryBrushManagerInit(histMgr).name,
        "OooOO0",
    )

    assertParity(
        "PaintActivity.userIdField",
        MultiVersionResolvers.resolvePaintUserIdField(paintActivity).name,
        "o0000oOO",
    )
    assertParity(
        "PaintActivity.outCloseViewField",
        MultiVersionResolvers.resolvePaintOutCloseViewField(paintActivity).name,
        "o000O0o0",
    )
    assertParity(
        "PaintActivity.xuanQuViewField",
        MultiVersionResolvers.resolvePaintXuanQuViewField(paintActivity).name,
        "Oooooo0",
    )
    assertParity(
        "PaintActivity.xuanQuMoveViewField",
        MultiVersionResolvers.resolvePaintXuanQuMoveViewField(paintActivity).name,
        "Ooooooo",
    )
    assertParity(
        "PaintActivity.leftSettingViewField",
        MultiVersionResolvers.resolvePaintLeftSettingViewField(paintActivity).name,
        "Oooo00O",
    )
    assertParity(
        "PaintActivity.canvasViewField",
        MultiVersionResolvers.resolvePaintCanvasViewField(paintActivity).name,
        "o0000O0",
    )
    assertParity(
        "PaintActivity.switchLeftTool",
        MultiVersionResolvers.resolvePaintSwitchLeftToolMethod(paintActivity).name,
        "o0ooo0oo",
    )

    assertParity(
        "PAINT_PARAMS.layerStateSparseField",
        MultiVersionResolvers.resolvePaintParamsLayerStateSparseField(allClasses).name,
        "OooO00o",
    )
    val renderAssist = MultiVersionResolvers.resolveLayerStateClass(allClasses)
    assertParity("RenderAssist.class", renderAssist.type, "Lnet/huanci/paintlib/views/o0OO0oO0;")
    assertParity(
        "RenderAssist.selectionRectField",
        MultiVersionResolvers.resolveLayerStateSelectionRectField(allClasses).name,
        "OooOo0",
    )

    val navRunTargets = MultiVersionResolvers.resolveNavigatorTouchRunnableTargets(allClasses)
    assertParity(
        "NavigatorTouchRunnable.class",
        navRunTargets.runnableClass.type,
        "Lnet/huanci/paintlib/views/NavigatorView\$o0ooOOo;",
    )
    assertParity("NavigatorTouchRunnable.run", navRunTargets.runMethod.name, "run")
    assertParity("NavigatorTouchRunnable.navViewField", navRunTargets.navViewField.name, "OooOO0O")
    assertParity("NavigatorTouchRunnable.gestureTokenField", navRunTargets.gestureTokenField.name, "OooO0o")
    assertParity("NavigatorTouchRunnable.x0Field", navRunTargets.x0Field.name, "OooO0oO")
    assertParity("NavigatorTouchRunnable.y0Field", navRunTargets.y0Field.name, "OooO0oo")
    assertParity("NavigatorTouchRunnable.x1Field", navRunTargets.x1Field.name, "OooO")
    assertParity("NavigatorTouchRunnable.y1Field", navRunTargets.y1Field.name, "OooOO0")

    val psdHelper = MultiVersionResolvers.resolvePsdHelperClass(allClasses)
    assertParity("PsdHelper.class", psdHelper.type, "Lo00o00oO/o00O0O0O;")
    val shareBitmapWrapper = MultiVersionResolvers.resolveShareBitmapWrapperClass(allClasses)
    assertParity("ShareBitmapWrapper.class", shareBitmapWrapper.type, "Lo00Oo0Oo/o00O00;")

    println("ALL $parityChecks STRUCTURAL TARGET PARITY CHECKS PASSED (1 TO 1 EXACT MATCH ON BASE.APK)")
}

private fun MultiVersionResolvers.resolveSingleClassFromTest(
    classes: Iterable<com.android.tools.smali.dexlib2.iface.ClassDef>,
    typeName: String,
): com.android.tools.smali.dexlib2.iface.ClassDef =
    classes.resolveSingleClass(typeName) { it.type == typeName }

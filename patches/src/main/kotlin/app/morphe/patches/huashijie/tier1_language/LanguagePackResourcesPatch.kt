package app.morphe.patches.huashijie.tier1_language

import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patches.huashijie.core.HUASHIJIE_COMPATIBILITY
import app.morphe.patches.huashijie.core.nativeSignatureBypassPatch
import java.io.File

private object LanguagePackResourceAnchor

@Suppress("unused")
val languagePackResourcesPatch = rawResourcePatch(
    name = "Language Pack Resources",
    description = "Bundles built-in Vietnamese, English, and Simplified Chinese JSON dictionaries inside the APK.",
    default = true,
) {
    dependsOn(nativeSignatureBypassPatch)
    compatibleWith(HUASHIJIE_COMPATIBILITY)

    execute {
        val langDir = get("assets/lang", copy = false)
        check(langDir.exists() || langDir.mkdirs()) {
            "Failed to create target asset directory: ${langDir.absolutePath}"
        }

        val fileNames = listOf("_template_zh.json", "vi.json", "en.json")
        val loaders = listOfNotNull(
            LanguagePackResourceAnchor::class.java.classLoader,
            javaClass.classLoader,
            Thread.currentThread().contextClassLoader,
        ).distinct()

        for (fileName in fileNames) {
            val resourcePath = "assets/lang/$fileName"
            val stream = loaders.firstNotNullOfOrNull { loader ->
                loader.getResourceAsStream(resourcePath)
            } ?: error("Bundled language pack resource not found in classpath: $resourcePath")

            stream.use { input ->
                File(langDir, fileName).outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }
}

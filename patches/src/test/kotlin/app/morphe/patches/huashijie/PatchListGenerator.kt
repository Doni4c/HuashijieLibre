package app.morphe.patches.huashijie

import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.loadPatchesFromJar
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import java.io.File
import java.net.URLClassLoader
import java.util.jar.Manifest

typealias PackageName = String
typealias VersionName = String

fun main(args: Array<String>) {
    require(args.size == 3) { "Expected arguments: <bundleFile> <outputJsonFile> <fallbackVersion>" }
    val patchFile = File(args[0])
    val outputFile = File(args[1])
    val fallbackVersion = args[2]
    require(patchFile.isFile) { "Compiled patch bundle not found: ${patchFile.absolutePath}" }

    val patchFiles = setOf(patchFile)
    val loadedPatches = loadPatchesFromJar(patchFiles)
    var manifestVersion: String? = null
    URLClassLoader(arrayOf(patchFile.toURI().toURL())).use { loader ->
        val manifests = loader.getResources("META-INF/MANIFEST.MF")
        while (manifests.hasMoreElements()) {
            val found = Manifest(manifests.nextElement().openStream())
                .mainAttributes
                .getValue("Version")
            if (!found.isNullOrBlank()) {
                manifestVersion = found
                break
            }
        }
    }

    val effectiveVersion = manifestVersion ?: fallbackVersion
    generatePatchList(effectiveVersion, loadedPatches, outputFile)
}

@Suppress("DEPRECATION")
private fun generatePatchList(version: String, patches: Set<Patch>, outputFile: File) {
    val appNames = patches
        .asSequence()
        .flatMap { it.compatibility.orEmpty().asSequence() }
        .mapNotNull { compat ->
            val pkg = compat.packageName ?: return@mapNotNull null
            val label = compat.name ?: return@mapNotNull null
            pkg to label
        }
        .toMap()

    val sortedPatches = patches.sortedBy { it.name ?: "" }.map { patch ->
        JsonPatch(
            name = patch.name,
            description = patch.description,
            category = patch.category,
            use = patch.default,
            dependencies = patch.dependencies
                .map { dep -> dep.name ?: dep.javaClass.simpleName }
                .distinct()
                .sorted(),
            compatiblePackages = patch.compatibility?.mapNotNull { it.legacyPackage() }?.toMap(),
            compatibility = patch.compatibility.orEmpty().map { it.toJsonCompatibility() },
            options = patch.options.values.map { option ->
                JsonPatch.Option(
                    key = option.key,
                    title = option.title,
                    description = option.description,
                    required = option.required,
                    type = option.type.toString(),
                    default = option.default,
                    values = option.values,
                )
            },
        )
    }

    val gson = GsonBuilder()
        .serializeNulls()
        .disableHtmlEscaping()
        .setPrettyPrinting()
        .create()

    val rootJson = JsonObject()
    rootJson.addProperty("version", "v$version")
    rootJson.add("appNames", gson.toJsonTree(appNames))
    rootJson.add("patches", gson.toJsonTree(sortedPatches))

    outputFile.writeText(gson.toJson(rootJson) + "\n", Charsets.UTF_8)
}

private fun Compatibility.legacyPackage(): Pair<PackageName, Set<VersionName>?>? {
    val pkg = packageName ?: return null
    val onlyExperimental = targets.none { !it.isExperimental }
    val counted = targets.filter { onlyExperimental || !it.isExperimental }
    if (counted.any { it.version == null }) return pkg to null
    return pkg to counted.mapNotNull { it.version }.toSet().ifEmpty { null }
}

private fun Compatibility.toJsonCompatibility() = JsonCompatibility(
    name = name ?: "",
    packageName = packageName ?: "",
    description = description?.takeUnless { it.isBlank() },
    appIconColor = appIconColor,
    signatures = signatures?.sorted(),
    targets = targets.map { it.toJsonAppTarget() },
)

private fun AppTarget.toJsonAppTarget() = JsonAppTarget(
    version = version ?: "",
    experimental = isExperimental,
    versionCodes = versionCodes?.entries?.sortedBy { it.key.name }?.associate { it.key.name to it.value },
)

@Suppress("unused")
private class JsonCompatibility(
    val name: String,
    val packageName: String,
    val description: String? = null,
    val appIconColor: Int?,
    val signatures: List<String>?,
    val targets: List<JsonAppTarget>?,
)

@Suppress("unused")
private class JsonAppTarget(
    val version: String,
    val experimental: Boolean = false,
    val versionCodes: Map<String, Int>? = null,
)

@Suppress("unused")
private class JsonPatch(
    val name: String? = null,
    val description: String? = null,
    val category: String? = null,
    val use: Boolean = true,
    val dependencies: List<String>,
    val compatiblePackages: Map<PackageName, Set<VersionName>?>? = null,
    val compatibility: List<JsonCompatibility>,
    val options: List<Option>,
) {
    class Option(
        val key: String,
        val title: String?,
        val description: String?,
        val required: Boolean,
        val type: String,
        val default: Any?,
        val values: Map<String, Any?>?,
    )
}

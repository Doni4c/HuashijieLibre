import groovy.json.JsonParserType
import groovy.json.JsonSlurper
import org.gradle.api.GradleException
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.jvm.tasks.Jar
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

patches {
    about {
        name = "HuashijieLibre Patches"
        description = "Open-source Morphe patches for HuashijiePro (net.huanci.hsjpro v3.4.6): dynamic localization, studio decluttering, offline artist profile, and UX ergonomics."
        source = "https://github.com/Doni4c/HuashijieLibre"
        author = "Doni4c"
        contact = "https://github.com/Doni4c/HuashijieLibre/issues"
        website = "https://github.com/Doni4c/HuashijieLibre"
        license = "GNU General Public License v3.0"
    }
}

dependencies {
    testImplementation(libs.gson)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

val langDir = rootProject.layout.projectDirectory.dir("lang")
val dictionaryNames = listOf("_template_zh.json", "vi.json", "en.json")
val validationStamp = layout.buildDirectory.file("reports/dictionaries-validated.txt")

val validateDictionaries = tasks.register("validateDictionaries") {
    group = "verification"
    description = "Validates JSON syntax, 100% key parity across dictionaries, and prohibited character rules."

    val dictionaryFiles = dictionaryNames.map { langDir.file(it) }
    inputs.files(dictionaryFiles)
    outputs.file(validationStamp)

    doLast {
        val forbiddenChars = mapOf(
            '\u2014' to "em-dash (U+2014)",
            '\u2013' to "en-dash (U+2013)",
            '\u2022' to "bullet (U+2022)",
        )
        val requiredSections = listOf("strings_by_id", "strings_by_text", "strings_by_value")
        val slurper = JsonSlurper().apply {
            type = JsonParserType.CHAR_BUFFER
        }

        fun scanProhibited(value: Any?, fileName: String, path: String) {
            when (value) {
                is String -> {
                    for ((ch, label) in forbiddenChars) {
                        if (value.indexOf(ch) >= 0) {
                            throw GradleException("Prohibited character $label found in $fileName at $path")
                        }
                    }
                }
                is Map<*, *> -> {
                    for ((k, v) in value) {
                        val keyStr = k?.toString() ?: ""
                        scanProhibited(keyStr, fileName, "$path.<key:$keyStr>")
                        scanProhibited(v, fileName, "$path.$keyStr")
                    }
                }
                is Iterable<*> -> {
                    value.forEachIndexed { idx, item ->
                        scanProhibited(item, fileName, "$path[$idx]")
                    }
                }
            }
        }

        val parsedDictionaries = linkedMapOf<String, Map<String, Any?>>()

        for (fileRef in dictionaryFiles) {
            val file = fileRef.asFile
            if (!file.exists() || !file.isFile) {
                throw GradleException("Missing required dictionary file: ${file.absolutePath}")
            }

            val rawText = file.readText(Charsets.UTF_8)
            rawText.lineSequence().forEachIndexed { lineIdx, line ->
                for ((ch, label) in forbiddenChars) {
                    if (line.indexOf(ch) >= 0) {
                        throw GradleException(
                            "Prohibited character $label found in raw text of ${file.name} at line ${lineIdx + 1}"
                        )
                    }
                }
            }

            val parsed = try {
                slurper.parseText(rawText)
            } catch (e: Exception) {
                throw GradleException("Invalid JSON syntax in ${file.name}: ${e.message}", e)
            }

            @Suppress("UNCHECKED_CAST")
            val rootMap = parsed as? Map<String, Any?>
                ?: throw GradleException("Dictionary ${file.name} must have a JSON object at the root.")

            scanProhibited(rootMap, file.name, "$")

            for (section in requiredSections) {
                if (rootMap[section] !is Map<*, *>) {
                    throw GradleException("Dictionary ${file.name} is missing JSON object section '$section'.")
                }
            }

            parsedDictionaries[file.name] = rootMap
        }

        val referenceName = "_template_zh.json"
        val referenceMap = parsedDictionaries.getValue(referenceName)

        for (section in requiredSections) {
            @Suppress("UNCHECKED_CAST")
            val refKeys = (referenceMap[section] as Map<String, Any?>).keys

            for ((fileName, targetMap) in parsedDictionaries) {
                if (fileName == referenceName) continue

                @Suppress("UNCHECKED_CAST")
                val targetKeys = (targetMap[section] as Map<String, Any?>).keys

                val missing = refKeys - targetKeys
                val extra = targetKeys - refKeys

                if (missing.isNotEmpty() || extra.isNotEmpty()) {
                    val details = buildString {
                        append("Key parity mismatch between $referenceName and $fileName in section '$section': ")
                        append("missing=${missing.size}, extra=${extra.size}.")
                        if (missing.isNotEmpty()) {
                            append(" Missing sample: ${missing.take(10)}")
                        }
                        if (extra.isNotEmpty()) {
                            append(" Extra sample: ${extra.take(10)}")
                        }
                    }
                    throw GradleException(details)
                }
            }
        }

        val stamp = validationStamp.get().asFile
        stamp.parentFile.mkdirs()
        val summary = buildString {
            appendLine("Validated ${parsedDictionaries.size} dictionaries successfully.")
            for (section in requiredSections) {
                val count = (referenceMap[section] as Map<*, *>).size
                appendLine("$section=$count")
            }
        }
        stamp.writeText(summary, Charsets.UTF_8)
        logger.lifecycle("validateDictionaries: passed 100% parity and character checks for $dictionaryNames")
    }
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(validateDictionaries)
    from(langDir) {
        include("_template_zh.json", "vi.json", "en.json")
        into("assets/lang")
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
}

tasks.withType<Test>().configureEach {
    failOnNoDiscoveredTests.set(false)
}

tasks.named("check") {
    dependsOn(validateDictionaries)
}

tasks.withType<Jar>().configureEach {
    archiveBaseName.set("patches")
    archiveExtension.set("mpp")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

val generatePatchesList = tasks.register<JavaExec>("generatePatchesList") {
    group = "documentation"
    description = "Generates patches-list.json at the repository root from the compiled .mpp patch bundle."
    dependsOn(tasks.named("jar"))
    val jarTask = tasks.named<Jar>("jar")
    val outputJson = rootProject.layout.projectDirectory.file("patches-list.json")
    inputs.file(jarTask.flatMap { it.archiveFile })
    outputs.file(outputJson)
    mainClass.set("app.morphe.patches.huashijie.PatchListGeneratorKt")
    classpath = sourceSets.test.get().runtimeClasspath + sourceSets.main.get().runtimeClasspath
    workingDir = rootProject.projectDir
    doFirst {
        val bundleFile = jarTask.get().archiveFile.get().asFile
        args(bundleFile.absolutePath, outputJson.asFile.absolutePath, project.version.toString())
    }
}

tasks.named("buildAndroid") {
    mustRunAfter(generatePatchesList)
}

tasks.named("build") {
    dependsOn(generatePatchesList, tasks.named("buildAndroid"))
}

tasks.register<JavaExec>("verifyTargetParity") {
    group = "verification"
    description = "Verifies 1 to 1 structural target resolution parity against base.apk."
    onlyIf {
        val hasApk = rootProject.file("base.apk").exists()
        if (!hasApk) {
            logger.lifecycle("verifyTargetParity: skipped because base.apk is not present in repository root.")
        }
        hasApk
    }
    mainClass.set("app.morphe.patches.huashijie.MultiVersionParityRunnerKt")
    classpath = sourceSets.test.get().runtimeClasspath + sourceSets.main.get().runtimeClasspath
    workingDir = rootProject.projectDir
}

import org.gradle.api.GradleException
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.jvm.tasks.Jar
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

patches {
    about {
        name = "HuashijieLibre Patches"
        description = "Morphe patches for 画世界Pro (net.huanci.hsjpro v3.4.6): JSON UI translations, custom fonts, offline access to login-gated tools, and canvas controls."
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
val validatorScript = rootProject.layout.projectDirectory.file("i18n/validate_dictionaries.py")
val validationStamp = layout.buildDirectory.file("reports/dictionaries-validated.txt")

val validateDictionaries = tasks.register("validateDictionaries") {
    group = "verification"
    description = "Validates JSON syntax, UTF-8/LF encoding, 100% 4-table parity, placeholders, and prohibited characters."

    val dictionaryTree = fileTree(langDir) {
        include("*.json")
    }
    inputs.files(dictionaryTree)
    inputs.file(validatorScript)
    outputs.file(validationStamp)

    doLast {
        val scriptFile = validatorScript.asFile
        if (!scriptFile.isFile) {
            throw GradleException("Missing shared dictionary validator script: ${scriptFile.absolutePath}")
        }
        val templateFile = langDir.file("_template_zh.json").asFile
        val stampFile = validationStamp.get().asFile

        val candidates = listOf(
            listOf("python3"),
            listOf("python"),
            listOf("py", "-3"),
        )
        var pythonCmd: List<String>? = null
        for (candidate in candidates) {
            try {
                val probe = ProcessBuilder(candidate + listOf("--version"))
                    .redirectErrorStream(true)
                    .start()
                if (probe.waitFor() == 0) {
                    pythonCmd = candidate
                    break
                }
            } catch (_: Exception) {
            }
        }
        if (pythonCmd == null) {
            throw GradleException("Python 3 is required to run :patches:validateDictionaries (${scriptFile.name}).")
        }

        val cmd = pythonCmd + listOf(
            scriptFile.absolutePath,
            "--lang-dir", langDir.asFile.absolutePath,
            "--template", templateFile.absolutePath,
            "--stamp", stampFile.absolutePath,
        )
        val process = ProcessBuilder(cmd)
            .directory(rootProject.projectDir)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader(Charsets.UTF_8).readText()
        val exitCode = process.waitFor()
        if (output.isNotBlank()) {
            logger.lifecycle(output.trimEnd())
        }
        if (exitCode != 0) {
            throw GradleException("Dictionary validation failed (exit code $exitCode). See validation output above.")
        }
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

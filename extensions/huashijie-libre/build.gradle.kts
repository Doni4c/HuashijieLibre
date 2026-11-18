android {
    namespace = "app.morphe.extension.huashijie"
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    compileOnly(libs.annotation)
    compileOnly(fileTree("dir" to "libs", "include" to listOf("*.jar")))
}

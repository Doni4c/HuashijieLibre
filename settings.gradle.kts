pluginManagement {
    val pluginGprUser = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
    val pluginGprKey = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
    repositories {
        mavenLocal()
        gradlePluginPortal()
        google()
        mavenCentral()
        if (!pluginGprUser.isNullOrBlank() && !pluginGprKey.isNullOrBlank()) {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/MorpheApp/registry")
                credentials {
                    username = pluginGprUser
                    password = pluginGprKey
                }
            }
        }
        maven { url = uri("https://jitpack.io") }
    }
}

plugins {
    id("app.morphe.patches") version "2.1.0"
}

rootProject.name = "huashijie-libre"

settings {
    extensions {
        defaultNamespace = "app.morphe.extension.huashijie"
    }
}

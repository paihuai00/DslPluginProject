pluginManagement {
    repositories {
        maven ("https://jitpack.io")
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven ("https://jitpack.io")
        google()
        mavenCentral()
    }
}

buildscript {
    repositories {
        maven ("https://jitpack.io")
        google()
        mavenCentral()
    }
    dependencies {
//        classpath("com.android.tools.build:gradle:7.0.0")

//        classpath("com.github.paihuai00:DslPluginProject:1.0.2")
    }
}

rootProject.name = "DslPluginProject"
include(":app")
//includeBuild("myPlugin")
include(":myPlugin")

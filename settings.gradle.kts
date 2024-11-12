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
//    dependencies {
//        classpath("com.github.paihuai00:DslPluginProject:1.0.1")
//    }
}

rootProject.name = "DslPluginProject"
include(":app")
//includeBuild("myPlugin")
include(":myPlugin")

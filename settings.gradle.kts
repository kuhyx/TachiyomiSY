pluginManagement {
    includeBuild("gradle/build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven(url = "https://www.jitpack.io")
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("mihonx") {
            from(files("gradle/mihon.versions.toml"))
        }
        create("sylibs") {
            from(files("gradle/sy.versions.toml"))
        }
    }

    @Suppress("UnstableApiUsage")
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    @Suppress("UnstableApiUsage")
    repositories {
        // SY fork (kuhy): artifacts JitPack can no longer build. JitPack
        // rebuilt com.github.arkon.FlexibleAdapter:flexible-adapter:c8013533
        // on 2026-09-12, the rebuild failed, and every fresh runner has
        // received a stub POM since; the AAR here is the last good build.
        // Listed first so the vendored copy wins over the broken one.
        maven(url = uri("gradle/vendored-m2")) {
            content { includeGroup("com.github.arkon.FlexibleAdapter") }
        }
        google()
        mavenCentral()
        maven(url = "https://www.jitpack.io")
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "TachiyomiSY"
include(":app")
include(":baseline-profile")
include(":core-metadata")
include(":core:common")
include(":data")
include(":domain")
include(":i18n")
// SY -->
include(":i18n-sy")
// SY <--
include(":presentation-core")
include(":presentation-widget")
include(":source-api")
include(":source-local")

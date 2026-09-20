plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.samWithReceiver)
    alias(libs.plugins.spotless)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
    `java-gradle-plugin`
}

// Configuration should be synced with [/gradle/build-logic/src/main/kotlin/mihon/gradle/configurations/Lint.kt]:
// this build cannot apply the convention plugin it compiles, so the same knobs are set here by hand.
detekt {
    allRules = true
    buildUponDefaultConfig = false
    parallel = true
    config.setFrom(fileTree("../../config/detekt") { include("*.yml") })
    source.setFrom(layout.projectDirectory.dir("src"))
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    jvmTarget = "17"
    reports {
        html.required.set(false)
        xml.required.set(false)
        sarif.required.set(false)
        md.required.set(false)
        txt.required.set(true)
    }
}

kover {
    useJacoco(libs.versions.jacoco.get())
    reports {
        verify {
            rule("line coverage") {
                minBound(100, kotlinx.kover.gradle.plugin.dsl.CoverageUnit.LINE)
            }
            rule("branch coverage") {
                minBound(100, kotlinx.kover.gradle.plugin.dsl.CoverageUnit.BRANCH)
            }
        }
    }
}

tasks.named("check") {
    dependsOn(tasks.withType<io.gitlab.arturbosch.detekt.Detekt>(), "koverVerify")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

kotlin {
    compilerOptions {
        allWarningsAsErrors.set(true)
        // As a free arg rather than explicitApi(): detekt's RedundantVisibilityModifierRule
        // only sees the mode through the compiler args it is handed.
        freeCompilerArgs.add("-Xexplicit-api=strict")
    }
}

// Configuration should be synced with [/gradle/build-logic/src/main/kotlin/PluginSpotless.kt]
val ktlintVersion = libs.ktlint.bom.get().version
val editorConfigFile = rootProject.file("../../.editorconfig")
spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint(ktlintVersion).setEditorConfigPath(editorConfigFile)
        trimTrailingWhitespace()
        endWithNewline()
    }

    kotlinGradle {
        target("*.kts")
        ktlint(ktlintVersion).setEditorConfigPath(editorConfigFile)
        trimTrailingWhitespace()
        endWithNewline()
    }
}

dependencies {
    compileOnly(gradleKotlinDsl())
    compileOnly(libs.android.gradle)
    compileOnly(libs.kotlin.compose.compiler.gradle)
    compileOnly(libs.kotlin.gradle)
    implementation(libs.spotless.gradle)
    implementation(libs.detekt.gradle)
    implementation(libs.kover.gradle)
    implementation(libs.tapmoc.gradle)
    testImplementation(gradleApi())
    testImplementation(gradleKotlinDsl())
    testImplementation(libs.android.gradle)
    testImplementation(libs.kotlin.gradle)
    testImplementation(libs.kotlin.compose.compiler.gradle)
    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.junit.platform.launcher)

    // These allow us to reference the dependency catalog inside our compiled plugins
    compileOnly(files(libs::class.java.superclass.protectionDomain.codeSource.location))
    compileOnly(files(mihonx::class.java.superclass.protectionDomain.codeSource.location))
    // The tests build the same accessors as mocks, so they need the generated classes at runtime.
    testImplementation(files(libs::class.java.superclass.protectionDomain.codeSource.location))
    testImplementation(files(mihonx::class.java.superclass.protectionDomain.codeSource.location))
}

samWithReceiver {
    annotation("org.gradle.api.HasImplicitReceiver")
}

gradlePlugin {
    plugins {
        register("android-application") {
            id = mihonx.plugins.android.application.get().pluginId
            implementationClass = "mihon.gradle.plugins.PluginAndroidApplication"
        }
        register("android-base") {
            id = mihonx.plugins.android.base.get().pluginId
            implementationClass = "mihon.gradle.plugins.PluginAndroidBase"
        }
        register("android-library") {
            id = mihonx.plugins.android.library.get().pluginId
            implementationClass = "mihon.gradle.plugins.PluginAndroidLibrary"
        }
        register("android-test") {
            id = mihonx.plugins.android.test.get().pluginId
            implementationClass = "mihon.gradle.plugins.PluginAndroidTest"
        }
        register("coverage") {
            id = mihonx.plugins.coverage.get().pluginId
            implementationClass = "mihon.gradle.plugins.PluginCoverage"
        }
        register("compose-android") {
            id = mihonx.plugins.compose.get().pluginId
            implementationClass = "mihon.gradle.plugins.PluginComposeAndroid"
        }
        register("kotlin-multiplatform") {
            id = mihonx.plugins.kotlin.multiplatform.get().pluginId
            implementationClass = "mihon.gradle.plugins.PluginKotlinMultiplatform"
        }
        register("lint") {
            id = mihonx.plugins.lint.get().pluginId
            implementationClass = "mihon.gradle.plugins.PluginLint"
        }
        register("sy-release") {
            id = mihonx.plugins.sy.release.get().pluginId
            implementationClass = "mihon.gradle.plugins.PluginSyRelease"
        }
        register("spotless") {
            id = mihonx.plugins.spotless.get().pluginId
            implementationClass = "mihon.gradle.plugins.PluginSpotless"
        }
    }
}

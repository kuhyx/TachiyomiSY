plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.spotless)
    alias(mihonx.plugins.lint)
    alias(mihonx.plugins.coverage)

    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.sqldelight)
}

android {
    namespace = "tachiyomi.data"

    sqldelight {
        databases {
            create("Database") {
                packageName.set("tachiyomi.data")
                dialect(libs.sqldelight.sqliteDialect338)
                schemaOutputDirectory.set(project.file("./src/main/sqldelight"))
                generateAsync.set(true)
            }
        }
    }
}

kover {
    reports {
        filters {
            // Coverage is measured on the hand-written code only. SQLDelight generates
            // the `Database`, `*Queries`, view and row classes into the same packages
            // (`tachiyomi.data`, `tachiyomi.view`), so the scope is an include list:
            // the hand-written sub-packages plus the hand-written top-level classes
            // of `tachiyomi.data`. Anything else in those two packages is generated.
            includes {
                packages(
                    "mihon.data",
                    "tachiyomi.data.category",
                    "tachiyomi.data.chapter",
                    "tachiyomi.data.history",
                    "tachiyomi.data.manga",
                    "tachiyomi.data.release",
                    "tachiyomi.data.source",
                    "tachiyomi.data.track",
                    "tachiyomi.data.updates",
                )
                classes(
                    "tachiyomi.data.*ColumnAdapter",
                    "tachiyomi.data.DatabaseAdapterKt",
                    "tachiyomi.data.LibraryQuery*",
                    "tachiyomi.data.QueryExtensionKt",
                    "tachiyomi.data.QueryPagingSource*",
                    "tachiyomi.data.RowReader",
                    "tachiyomi.data.UpdatesFilter",
                    "tachiyomi.data.UpdatesQuery*",
                )
            }
        }
    }
}

kotlin {
    compilerOptions {
        optIn.add("kotlinx.serialization.ExperimentalSerializationApi")
    }
}

dependencies {
    implementation(projects.sourceApi)
    implementation(projects.domain)
    implementation(projects.core.common)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.jsonOkio)
    implementation(libs.kotlinx.serialization.protobuf)

    api(libs.bundles.sqldelight)

    testImplementation(libs.bundles.test)
    testImplementation(libs.kotlinx.coroutines.test)
    // In-memory JDBC SQLite: the repositories only see `Database`, so the JVM
    // driver stands in for the androidx one without an emulator.
    testImplementation(libs.sqldelight.sqliteDriver)
    testRuntimeOnly(libs.junit.platform.launcher)
    // ExtensionStoreService and ReleaseServiceImpl are exercised against a local HTTP server.
    testImplementation(libs.okhttp.mockwebserver)
}

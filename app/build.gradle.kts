plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
    alias(libs.plugins.ktlint)
}

// versionName/versionCode come from the nearest `v1.2.3` tag (spec §17); untagged builds are 0.0.0 with code 1.
val gitVersion: String =
    providers
        .exec {
            commandLine("git", "describe", "--tags", "--match", "v[0-9]*", "--abbrev=0")
            isIgnoreExitValue = true
        }.standardOutput.asText
        .get()
        .trim()
        .removePrefix("v")
        .ifBlank { "0.0.0" }
val versionParts = gitVersion.split('.', '-').mapNotNull { it.toIntOrNull() } + listOf(0, 0, 0)

android {
    namespace = "nl.potentplum.mise"
    compileSdk = 37

    defaultConfig {
        applicationId = "nl.potentplum.mise"
        minSdk = 30
        targetSdk = 36
        versionCode = maxOf(1, versionParts[0] * 10_000 + versionParts[1] * 100 + versionParts[2])
        versionName = gitVersion
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Curated content lives in /data and is read in place (spec §4.3); there is no copy step.
    sourceSets {
        getByName("main") {
            assets.srcDir(rootProject.file("data/assets"))
            res.srcDir(rootProject.file("data/res"))
        }
        getByName("test") {
            resources.srcDir(rootProject.file("data/fixtures"))
            resources.srcDir(rootProject.file("data/assets"))
            // Room schemas for MigrationTestHelper under Robolectric.
            assets.srcDir(file("schemas"))
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    lint {
        error += listOf("HardcodedText", "ComposeHardcodedText")
        abortOnError = true
        checkReleaseBuilds = false
        lintConfig = file("lint.xml")
    }

    packaging {
        resources.excludes += listOf("/META-INF/{AL2.0,LGPL2.1}", "/META-INF/LICENSE*", "/META-INF/NOTICE*")
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

ktlint {
    version.set(libs.versions.ktlint)
}

dependencies {
    lintChecks(project(":lint-checks"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.datastore.preferences)
    implementation(libs.work.runtime.ktx)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)
    implementation(libs.jsoup)
    implementation(libs.coil.compose)
    implementation(libs.timber)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.junit)
    testImplementation(libs.room.testing)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
}

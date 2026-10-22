plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.android.lint)
    alias(libs.plugins.ktlint)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

ktlint {
    version.set(libs.versions.ktlint)
}

dependencies {
    compileOnly(libs.lint.api)
    compileOnly(libs.lint.checks)
    testImplementation(libs.lint.api)
    testImplementation(libs.lint.tests)
    testImplementation(libs.junit)
}

tasks.jar {
    manifest { attributes("Lint-Registry-v2" to "nl.potentplum.mise.lint.MiseIssueRegistry") }
}

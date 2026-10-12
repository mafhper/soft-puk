buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // AGP 9 tem Kotlin embutido; elevamos o KGP para casar com a era das libs
        // (Compose BOM 2026.09) — mecanismo documentado em migrate-to-built-in-kotlin.
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

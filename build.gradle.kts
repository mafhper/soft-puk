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
    // Dependabot (2026-10-12): o toolchain do AGP (bundletool, jetifier, sdklib)
    // traz transitivas com CVE conhecidas. Nenhuma embarca no APK (conferido no
    // grafo de runtime: zero ocorrências) — o build fica com as versões corrigidas.
    configurations.named("classpath") {
        resolutionStrategy {
            force(
                "org.bouncycastle:bcprov-jdk18on:1.85",
                "org.bouncycastle:bcpkix-jdk18on:1.85",
                "org.bouncycastle:bcutil-jdk18on:1.85",
                "org.bitbucket.b_c:jose4j:0.9.6",
                "org.jdom:jdom2:2.0.6.1",
                "org.apache.commons:commons-lang3:3.18.0",
                "org.apache.httpcomponents:httpclient:4.5.14",
            )
        }
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

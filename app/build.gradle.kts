plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.tanakhpoc.learner"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.tanakhpoc.learner"
        minSdk = 26
        targetSdk = 35
        versionCode = 17
        versionName = "0.5.1-dss-poc"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Stamp short SHA at configure time (matches git HEAD when building from a clean tree).
        val gitSha = providers.exec {
            commandLine("git", "rev-parse", "--short=12", "HEAD")
        }.standardOutput.asText.get().trim()
        buildConfigField("String", "GIT_SHA", "\"$gitSha\"")
    }

    // POC: release uses the local debug keystore so we can ship a
    // non-debuggable APK without a Play App Signing key. Documented in README.
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // Keep *.gz bytes intact when possible; PackRepository still falls back if
    // aapt2 gunzips and renames (known assets quirk).
    androidResources {
        noCompress += "gz"
    }
}

// Regression: packaged APK must contain catalog + glosses (gz or plain after aapt2).
fun verifyApkAssets(taskName: String, apkRel: String, assembleTask: String) {
    tasks.register(taskName) {
        group = "verification"
        description = "Assert catalog/glosses/book assets exist inside $apkRel"
        dependsOn(assembleTask)
        doLast {
            val apk = layout.buildDirectory.file(apkRel).get().asFile
            require(apk.isFile) { "Missing APK: $apk" }
            // Avoid java.* in Gradle Kotlin DSL (java = JavaPluginExtension).
            val proc = ProcessBuilder("unzip", "-Z1", apk.absolutePath)
                .redirectErrorStream(true)
                .start()
            val names = proc.inputStream.bufferedReader().readLines().filter { it.isNotBlank() }.toSet()
            val code = proc.waitFor()
            require(code == 0) { "unzip -Z1 failed ($code) on $apk" }
            fun has(path: String) = path in names
            require(has("assets/data/catalog.json")) { "APK missing assets/data/catalog.json" }
            val glossOk = has("assets/data/glosses.json.gz") || has("assets/data/glosses.json")
            require(glossOk) {
                "APK missing glosses (expected assets/data/glosses.json.gz or .json). Sample: " +
                    names.filter { it.startsWith("assets/data/") }.take(20)
            }
            val genOk = has("assets/data/books/Gen.json.gz") || has("assets/data/books/Gen.json")
            require(genOk) { "APK missing Genesis pack under assets/data/books/" }
            val glossLabel = if (has("assets/data/glosses.json.gz")) "glosses.json.gz" else "glosses.json"
            val genLabel = if (has("assets/data/books/Gen.json.gz")) "Gen.json.gz" else "Gen.json"
            logger.lifecycle("$taskName OK — glosses=$glossLabel gen=$genLabel")
        }
    }
}

verifyApkAssets(
    "verifyDebugApkAssets",
    "outputs/apk/debug/app-debug.apk",
    "assembleDebug"
)
verifyApkAssets(
    "verifyReleaseApkAssets",
    "outputs/apk/release/app-release.apk",
    "assembleRelease"
)

tasks.named("check") {
    dependsOn("verifyDebugApkAssets")
}


// Keep androidx.emoji2 on the classpath — Compose ui-text subclasses
// EmojiCompat.InitCallback; excluding the AAR causes NoClassDefFoundError at
// first Text composition (release won't launch). Argus control is manifesto
// tools:node="remove" on EmojiCompatInitializer (no auto GMS font fetch).
// Still exclude profileinstaller (exported receiver); strip via manifest too.
configurations.configureEach {
    exclude(group = "androidx.profileinstaller", module = "profileinstaller")
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.navigation:navigation-compose:2.8.4")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")


    testImplementation("junit:junit:4.13.2")
}

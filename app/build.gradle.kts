plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

import java.util.Properties
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val releaseSigningProperties = Properties().apply {
    val file = rootProject.file("key.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}
fun releaseSigningValue(propertyName: String, environmentName: String): String? {
    return releaseSigningProperties.getProperty(propertyName)?.takeIf { it.isNotBlank() }
        ?: providers.environmentVariable(environmentName).orNull?.takeIf { it.isNotBlank() }
}

val releaseSigningStoreFile = releaseSigningValue("storeFile", "LUNADESK_KEYSTORE_PATH")
val releaseSigningStorePassword = releaseSigningValue("storePassword", "LUNADESK_STORE_PASSWORD")
val releaseSigningKeyAlias = releaseSigningValue("keyAlias", "LUNADESK_KEY_ALIAS")
val releaseSigningKeyPassword = releaseSigningValue("keyPassword", "LUNADESK_KEY_PASSWORD")
val hasReleaseSigning = listOf(
    releaseSigningStoreFile,
    releaseSigningStorePassword,
    releaseSigningKeyAlias,
    releaseSigningKeyPassword
).all { !it.isNullOrBlank() }
val debugKeystoreFile = rootProject.file("keystore/lunadesk-debug.keystore")
val buildTag = providers.environmentVariable("LUNADESK_BUILD_TAG").orNull
    ?: LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"))

android {
    namespace = "com.example.lunadesk"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.lunadesk"
        minSdk = 26
        targetSdk = 35
        versionCode = 700_000_001
        versionName = "0.2.0"
        buildConfigField("String", "BUILD_TAG", "\"$buildTag\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        getByName("debug") {
            storeFile = debugKeystoreFile
            storePassword = "lunadeskdebug"
            keyAlias = "lunadesk-debug"
            keyPassword = "lunadeskdebug"
        }
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(releaseSigningStoreFile!!)
                storePassword = releaseSigningStorePassword
                keyAlias = releaseSigningKeyAlias
                keyPassword = releaseSigningKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
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
}

val releaseSigningRequired = gradle.startParameter.taskNames.any { taskPath ->
    val taskName = taskPath.substringAfterLast(':')
    taskName.contains("release", ignoreCase = true) ||
        taskName.equals("assemble", ignoreCase = true) ||
        taskName.equals("bundle", ignoreCase = true) ||
        taskName.equals("build", ignoreCase = true)
}
if (releaseSigningRequired && !hasReleaseSigning) {
    error(
        "缺少 LunaDesk release 签名。请配置 key.properties，" +
            "或提供 LUNADESK_KEYSTORE_PATH / LUNADESK_STORE_PASSWORD / " +
            "LUNADESK_KEY_ALIAS / LUNADESK_KEY_PASSWORD。"
    )
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")

    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.google.android.material:material:1.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.noties.markwon:core:4.6.2")
    implementation("io.noties.markwon:inline-parser:4.6.2")
    implementation("io.noties.markwon:ext-latex:4.6.2")

    testImplementation("junit:junit:4.13.2")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

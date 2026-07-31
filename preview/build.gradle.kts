import org.gradle.api.tasks.Sync
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

android {
    namespace = "com.mohdalyahri.essentialspreview"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.mohdalyahri.essentials.preview"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    sourceSets.getByName("main") {
        res.srcDir(layout.buildDirectory.dir("generated/previewResources"))
        assets.srcDir(layout.buildDirectory.dir("generated/previewAssets"))
    }
}

val preparePreviewResources by tasks.registering(Sync::class) {
    from(rootProject.file("app/src/main/res/values/strings.xml")) {
        into("values")
    }
    from(rootProject.file("app/src/main/res/values-ar-rSA/strings.xml")) {
        into("values-ar-rSA")
    }
    into(layout.buildDirectory.dir("generated/previewResources"))
}

val preparePreviewIndex by tasks.registering {
    val sourceFile = rootProject.file("app/src/main/res/values-ar-rSA/strings.xml")
    val outputDir = layout.buildDirectory.dir("generated/previewAssets")
    inputs.file(sourceFile)
    outputs.dir(outputDir)

    doLast {
        val destination = outputDir.get().asFile
        destination.mkdirs()
        val pattern = Regex("<string\\s+name=\"([^\"]+)\"")
        val reviewed = sourceFile.readLines()
            .take(704)
            .mapIndexedNotNull { index, line ->
                pattern.find(line)?.groupValues?.get(1)?.let { key -> "${index + 1}\t$key" }
            }
        destination.resolve("reviewed_keys.tsv").writeText(reviewed.joinToString("\n"))
    }
}

tasks.named("preBuild") {
    dependsOn(preparePreviewResources, preparePreviewIndex)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3.v150alpha24)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

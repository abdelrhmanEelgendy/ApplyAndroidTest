import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration
import org.jetbrains.dokka.gradle.DokkaTask
import java.io.FileInputStream
import java.util.Properties
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.dokka")

}
apply(from = "automation-tasks.gradle.kts")

android {
    namespace = "com.example.core_module"
    compileSdk = 33
    flavorDimensions += listOf("logging", "sslContext")

    signingConfigs {
        create("release") {
            val keyStoreProp = getProps("$rootDir/core_module/configs/keystore.properties")
            storeFile = file("configs/" + keyStoreProp.getProperty("storeFile"))
            storePassword = keyStoreProp.getProperty("storePassword")
            keyAlias = keyStoreProp.getProperty("keyAlias")
            keyPassword = keyStoreProp.getProperty("keyPassword")
        }
    }
    defaultConfig {
        minSdk = 30

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            signingConfig = signingConfigs["release"]
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
    }

    productFlavors {
        create("logCat") {
            dimension = "logging"
        }
        create("logWriter") {
            dimension = "logging"
        }
        create("production") {
            dimension = "logging"
        }
        create("withSSL") {
            dimension = "sslContext"
        }
        create("withoutSSL") {
            dimension = "sslContext"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    androidComponents {
        beforeVariants(selector().withBuildType("debug")) {
            if (it.flavorName == "productionWithoutSSL" || it.flavorName == "productionWithSSL")
                it.enable = false
        }

        beforeVariants(selector().withBuildType("release")) {
            it.enable =
                it.flavorName == "productionWithoutSSL" || it.flavorName == "productionWithSSL"
        }
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}


tasks.withType<DokkaTask>().configureEach {
    moduleName.set("Payment SDK")
    moduleVersion.set(project.version.toString())
    failOnWarning.set(false)
    suppressObviousFunctions.set(true)
    suppressInheritedMembers.set(true)
    offlineMode.set(true)

    dokkaSourceSets {
        configureEach {
//            documentedVisibilities.set(setOf(Visibility.PUBLIC))
            reportUndocumented.set(false)
            jdkVersion.set(17)
            noStdlibLink.set(true)
            noJdkLink.set(true)
            noAndroidSdkLink.set(true)
        }
    }
}

tasks.register<Jar>("dokkaHtmlJar") {
    dependsOn(tasks.dokkaHtml)
    from(tasks.dokkaHtml.flatMap { it.outputDirectory })
    archiveClassifier.set("html-docs")
}

tasks.register<Jar>("dokkaJavadocJar") {
    dependsOn(tasks.dokkaJavadoc)
    from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}

tasks.named("dokkaJavadoc").configure {
    mustRunAfter("renameAarFiles")
}

tasks.dokkaHtml {
    dokkaSourceSets.configureEach {
        pluginConfiguration<DokkaBase, DokkaBaseConfiguration> {
//        customAssets = listOf(file("my-image.png"))
//        customStyleSheets = listOf(file("my-styles.css"))
            footerMessage = "2023 - Specialized Business Solutions"
            separateInheritedMembers = false
//        templatesDir = file("dokka/templates")
            mergeImplicitExpectActualDeclarations = true
        }
    }
}

fun getProps(path: String): java.util.Properties {
    val props = Properties()
    props.load(FileInputStream(rootProject.file(path)))
    return props
}

// --------------------------------------------

tasks.named("assemble").configure {
    // Sign AAR release files using the payment keyStore file.
    finalizedBy("signAar").mustRunAfter("assembleRelease")

    // Calculate SHA-256 CheckSum for the release AAR files.
    finalizedBy("calculateAarChecksum")

    // Rename the generated AAR files for all build types and flavors.
    finalizedBy("renameAarFiles")

    // Generate the JavaDoc for the SDK.
    finalizedBy("dokkaJavadocJar")

    // Prepare the New release by moving the necessary files for the output dir.
    finalizedBy("moveFiles")

    // Package the new release files into a single ZIP file.
    finalizedBy("zipPackageFiles")
}

// Create a custom Gradle task to sign the AAR
tasks.register("signAar") {
    dependsOn("assembleRelease")
    description =
        "Sign all AAR files with different build types and product flavors using the production keyStore."

    doLast {
        val aarDir = project.buildDir.resolve("outputs/aar")
        val signingConfig = android.signingConfigs.getByName("release")
        val jarsignerPath = project.findProperty("org.gradle.java.home")?.toString()
            ?: System.getProperty("java.home")

        val pattern = Regex(".*-release.aar")
        val aarFiles = aarDir.listFiles { file -> pattern.matches(file.name) }

        aarFiles?.forEach { aarFile ->
            project.exec {
                commandLine = listOf(
                    "$jarsignerPath/bin/jarsigner",
                    "-sigalg",
                    "SHA256withRSA",
                    "-digestalg",
                    "SHA-256",
                    "-storepass",
                    signingConfig.storePassword,
                    "-keypass",
                    signingConfig.keyPassword,
                    "-keystore",
                    signingConfig.storeFile.toString(),
                    aarFile.toString(),
                    signingConfig.keyAlias
                )
            }
        }
    }
}

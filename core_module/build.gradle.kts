import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration
import org.jetbrains.dokka.gradle.DokkaTask
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.dokka")

}

android {
    namespace = "com.example.core_module"
    compileSdk = 33

    defaultConfig {
        minSdk = 30

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
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
            reportUndocumented.set(true)
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


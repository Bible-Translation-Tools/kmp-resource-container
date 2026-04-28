plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("maven-publish")
}

group = "org.bibletranslationtools"
version = "1.0.0"

kotlin {
    jvmToolchain(11)

    androidTarget {
        publishLibraryVariants("release")
    }
    jvm()

    sourceSets {
        commonMain {
            dependencies {
                implementation("net.mamoe.yamlkt:yamlkt:0.13.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
                implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.21.2")
                implementation("org.apache.commons:commons-compress:1.28.0")
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        jvmTest {
            dependencies {
                implementation(kotlin("test"))
                implementation("junit:junit:4.13.2")
            }
        }
    }
}

tasks.named<Test>("jvmTest") {
    useJUnit()
}

android {
    namespace = "org.bibletranslationtools.resourcecontainer"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        buildConfig = true
    }
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        groupId = group.toString()
        version = version.toString()
        pom {
            name = "Resource Container"
            description = "A utility for managing Door43 Resource Containers"
            inceptionYear = "2025"
            url = "https://github.com/bibletranslationtools/android-resource-container/"
            licenses {
                license {
                    name = "GNU GENERAL PUBLIC LICENSE, Version 2.0"
                    url = "http://www.gnu.org/licenses/gpl-2.0.en.html"
                    distribution = "repo"
                }
            }
            developers {
                developer {
                    id = "bibletranslationtools"
                    name = "BibleTranslationTools"
                    url = "https://github.com/bibletranslationtools/"
                }
            }
            scm {
                url = "https://github.com/bibletranslationtools/android-resource-container/"
                connection = "scm:git:git://github.com/bibletranslationtools/android-resource-container.git"
                developerConnection = "scm:git:ssh://git@github.com/bibletranslationtools/android-resource-container.git"
            }
            issueManagement {
                url = "https://github.com/bibletranslationtools/android-resource-container/issues"
                system = "github"
            }
        }
    }
    repositories {
        maven {
            name = "Nexus"
            url = uri(
                if (version.toString().endsWith("SNAPSHOT"))
                    "https://nexus-registry.walink.org/repository/maven-snapshots"
                else
                    "https://nexus-registry.walink.org/repository/maven-releases"
            )
            credentials {
                username = (project.findProperty("repoUser") as? String) ?: System.getenv("REPO_USER")
                password = (project.findProperty("repoPassword") as? String) ?: System.getenv("REPO_PASSWORD")
            }
        }
    }
}

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    jvm()
    js {
        browser()
        nodejs()
    }
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":socket-io"))
                implementation(libs.chrome.devtools.kotlin)
                implementation(libs.har.parser)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(libs.ktor.client.java)
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(libs.ktor.client.js)
            }
        }
    }
}

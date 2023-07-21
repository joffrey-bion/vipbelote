plugins {
    kotlin("multiplatform")
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
                implementation(project(":vipbelote-protocol"))
                implementation(libs.kotlinx.coroutines.core)
            }
        }
    }
}

plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":vipbelote-protocol"))
    implementation(libs.chrome.devtools.kotlin)
    implementation(libs.ktor.client.java)
    implementation(libs.slf4j.simple)
}

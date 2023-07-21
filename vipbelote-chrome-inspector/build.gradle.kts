plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":vipbelote-protocol"))
    implementation(project(":vipbelote-state"))
    implementation(libs.chrome.devtools.kotlin)
    implementation(libs.ktor.client.cio)
    implementation(libs.slf4j.simple)
}

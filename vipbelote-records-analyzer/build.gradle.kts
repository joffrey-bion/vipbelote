plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    implementation(project(":vipbelote-protocol"))
    implementation(project(":vipbelote-state"))
    implementation(libs.har.parser)
}

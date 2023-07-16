plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    implementation(project(":vipbelote-protocol"))
    implementation(libs.har.parser)
}

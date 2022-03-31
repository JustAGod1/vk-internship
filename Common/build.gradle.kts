
plugins {
    java
    `java-library`
}

group = "me.justagod"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    runtimeOnly("org.slf4j:slf4j-simple:2.0.0-alpha7")
    implementation("com.google.code.gson:gson:2.9.0")
    api("com.google.code.gson:gson:2.9.0")
    api("org.jetbrains:annotations:23.0.0")
}


plugins {
    java
    application
}

group = "me.justagod"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("ch.qos.logback:logback-classic:1.2.11")
    implementation("com.google.code.gson:gson:2.9.0")
    implementation("org.eclipse.jetty:jetty-servlet:11.0.8")
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("MainKt")
}
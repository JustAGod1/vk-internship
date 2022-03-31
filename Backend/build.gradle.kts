
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
    implementation("org.eclipse.jetty:jetty-servlet:11.0.8")
    implementation("org.xerial:sqlite-jdbc:3.36.0.3")
    implementation(project(":Common"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testImplementation("org.assertj:assertj-core:3.22.0")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.7.1")
    testImplementation("org.mockito:mockito-core:4.4.0")
    testImplementation("org.mockito:mockito-inline:4.4.0")

}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("MainKt")
}
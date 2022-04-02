
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
    testImplementation("com.google.guava:guava:31.1-jre")
    testImplementation(project(":Frontend"))
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("ru.justagod.vk.backend.Main")
}

tasks.create("runBackend", JavaExec::class) {
    group = "help"
    classpath = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).runtimeClasspath
    this.workingDir = rootProject.file("run/").absoluteFile
    mainClass.set("ru.justagod.vk.backend.Main")
}

rootProject.file("run").mkdirs()
plugins {
    java
    application
}

group = "me.justagod"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

allprojects {
    tasks.withType<JavaCompile> {
        targetCompatibility = "11"
        sourceCompatibility = "11"
    }
}

evaluationDependsOnChildren()
tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("MainKt")
}

tasks.create("runBackend") {
    dependsOn += project(":Backend").tasks.findByName("runBackend")!!
}
tasks.create("runFrontend") {
    dependsOn += project(":Frontend").tasks.findByName("runFrontend")!!
}

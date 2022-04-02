
plugins {
    java
}

group = "me.justagod"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.intellij:forms_rt:7.0.3")

    implementation(project(":Common"))
}

tasks.create("runFrontend", JavaExec::class) {
    group = "help"
    classpath = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).runtimeClasspath
    mainClass.set("ru.justagod.vk.frontend.Main")
}


import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    idea
    kotlin("jvm") version "1.9.0"
    id("xyz.jpenilla.run-paper") version "2.1.0"
}

group = "world.komq"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly(kotlin("stdlib"))
    compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")
    compileOnly("cloud.commandframework:cloud-paper:1.8.4")
    compileOnly("io.github.monun:tap-api:4.9.8")
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_17.toString()
    }
    processResources {
        filesMatching("**/*.yml") {
            expand(project.properties)
        }
    }
    jar {
        archiveBaseName.set(rootProject.name)
        archiveClassifier.set("")
        archiveVersion.set("")
    }
    runServer {
        minecraftVersion("1.20.1")
        jvmArgs = listOf("-Dcom.mojang.eula.agree=true")
    }
}

idea {
    module {
        excludeDirs.addAll(listOf(file("run"), file("out"), file(".idea")))
    }
}
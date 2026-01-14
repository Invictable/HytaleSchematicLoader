plugins {
    java
    id("com.gradleup.shadow") version "9.0.0-beta13"
}

group = "cc.invic"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
    maven("https://jitpack.io/")

}


dependencies {
    // Add Hytale Server as compileOnly dependency (not bundled in final JAR)
    compileOnly(files("HytaleServer.jar"))
    implementation("com.github.Querz:NBT:6.1")
}

tasks.jar {
    // Set the archive name
    archiveBaseName.set("schematic-loader")
    archiveVersion.set("1.0.0")

    // Handle duplicates (resources are already included by default)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

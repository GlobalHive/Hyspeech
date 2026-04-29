plugins {
    java
}

group = "gg.ngl.hyspeech"
version = "1.2.3"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {
    // Hytale Server API - compile only since it's provided at runtime
    compileOnly(files("../server/Server/HytaleServer.jar"))

    implementation("org.ow2.asm:asm:9.6")
    implementation("org.ow2.asm:asm-tree:9.6")
    implementation("org.ow2.asm:asm-commons:9.6")

    // Testing
    testImplementation(libs.junit)
}

tasks.jar {
    // Set the archive name
    archiveBaseName.set("Hyspeech")
}

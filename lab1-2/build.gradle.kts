plugins {
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "ru.example"
version = "1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

javafx {
    version = "21.0.2"
    modules = listOf("javafx.controls")
}

application {
    mainClass.set("ru.example.library.Main")
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

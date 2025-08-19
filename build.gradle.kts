import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.util.Properties

// Load properties
val properties = Properties()
file("gradle.properties").inputStream().use { properties.load(it) }

plugins {
    java
    id("com.gradleup.shadow") version "8.3.0"
}

group = properties.getProperty("pluginGroupId")
version = properties.getProperty("pluginVersion")

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withJavadocJar()
    withSourcesJar()
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://jitpack.io") // For VaultAPI
}

dependencies {
    // Compile-Only Dependencies (provided by server or other plugins)
    compileOnly("io.papermc.paper:paper-api:${properties.getProperty("paperApiVersion")}")
    compileOnly("com.github.milkbowl:VaultAPI:${properties.getProperty("vaultApiVersion")}")
    compileOnly("me.clip:placeholderapi:${properties.getProperty("placeholderApiVersion")}")
    compileOnly("mysql:mysql-connector-java:8.0.33") // For compiling against, not for bundling

    // Implementation Dependencies (to be shaded)
    implementation("com.zaxxer:HikariCP:${properties.getProperty("hikariVersion")}")
    implementation("net.kyori:adventure-text-minimessage:${properties.getProperty("miniMessageVersion")}")
    implementation("com.h2database:h2:${properties.getProperty("h2DriverVersion")}")
}

tasks {
    // Configure Java compilation
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-Xlint:all,-serial", "-parameters"))
        options.release.set(21)
    }

    // Configure the shadowJar task
    shadowJar {
        archiveClassifier.set("") // Produce a single jar without the '-all' suffix
        relocate("com.zaxxer.hikari", "com.minekarta.kec.libs.hikaricp")
        relocate("net.kyori.adventure", "com.minekarta.kec.libs.adventure")
        relocate("org.h2", "com.minekarta.kec.libs.h2")

        // Minimize the JAR file by removing unnecessary files
        minimize()
    }

    // Set shadowJar as the default build task
    build {
        dependsOn(shadowJar)
    }

    // Configure resource processing to replace placeholders
    processResources {
        filesMatching("plugin.yml") {
            expand("version" to project.version)
        }
    }
}

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.util.Properties

// Load properties
val properties = Properties()
file("gradle.properties").inputStream().use { properties.load(it) }

plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = properties.getProperty("pluginGroupId")
version = properties.getProperty("pluginVersion")

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    withJavadocJar()
    withSourcesJar()
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io") // For VaultAPI
}

dependencies {
    // Compile-Only Dependencies (provided by server or other plugins)
    compileOnly("io.papermc.paper:paper-api:${properties.getProperty("paperApiVersion")}")
    compileOnly("net.milkbowl.vault:VaultAPI:${properties.getProperty("vaultApiVersion")}")
    compileOnly("me.clip:placeholderapi:${properties.getProperty("placeholderApiVersion")}")
    compileOnly("mysql:mysql-connector-java:8.0.33") // For compiling against, not for bundling

    // Implementation Dependencies (to be shaded)
    implementation("com.zaxxer:HikariCP:${properties.getProperty("hikariVersion")}")
    implementation("net.kyori:adventure-text-minimessage:${properties.getProperty("miniMessageVersion")}")
    implementation("org.xerial:sqlite-jdbc:${properties.getProperty("sqliteDriverVersion")}")
}

tasks {
    // Configure Java compilation
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-Xlint:all,-serial", "-parameters"))
    }

    // Configure the shadowJar task
    shadowJar {
        archiveClassifier.set("") // Produce a single jar without the '-all' suffix
        relocate("com.zaxxer.hikari", "com.minekarta.kec.libs.hikaricp")
        relocate("net.kyori.adventure", "com.minekarta.kec.libs.adventure")
        relocate("org.sqlite", "com.minekarta.kec.libs.sqlite")

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

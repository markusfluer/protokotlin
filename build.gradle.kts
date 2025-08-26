plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
    application
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version "1.2.1"
}

group = "de.markusfluer.protokotlin"
version = "2.1.1"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.6.2")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
    implementation("com.github.ajalt.clikt:clikt:4.2.2")
    implementation("com.squareup:kotlinpoet:1.15.3")
    implementation(gradleApi())
    
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("com.protokotlin.MainKt")
}

// Fix duplicate handling for distribution tasks
tasks.withType<Tar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<Zip> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.protokotlin.MainKt"
    }
}

tasks.register<Jar>("fatJar") {
    archiveClassifier.set("all")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = "com.protokotlin.MainKt"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get())
}

gradlePlugin {
    website.set("https://github.com/markusfluer/protokotlin")
    vcsUrl.set("https://github.com/markusfluer/protokotlin.git")
    
    plugins {
        create("protokotlin") {
            id = "de.markusfluer.protokotlin.plugin"
            implementationClass = "com.protokotlin.gradle.ProtoKotlinPlugin"
            displayName = "ProtoKotlin Gradle Plugin"
            description = "Generate Kotlin DTOs from Protocol Buffer files compatible with kotlinx.serialization.protobuf. Supports proto3 syntax with proper nullable fields, @ProtoPacked annotations, and seamless build integration."
            tags.set(listOf("protobuf", "kotlin", "serialization", "code-generation", "dto", "proto3", "kotlinx-serialization"))
        }
    }
}

// Configure the plugin publication metadata
publishing {
    publications {
        withType<MavenPublication> {
            pom {
                name.set("ProtoKotlin")
                description.set("Generate Kotlin DTOs from Protocol Buffer files compatible with kotlinx.serialization.protobuf")
                url.set("https://github.com/markusfluer/protokotlin")
                
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                
                developers {
                    developer {
                        id.set("markusfluer")
                        name.set("Markus Fluer")
                    }
                }
                
                scm {
                    connection.set("scm:git:git://github.com/markusfluer/protokotlin.git")
                    developerConnection.set("scm:git:ssh://github.com/markusfluer/protokotlin.git")
                    url.set("https://github.com/markusfluer/protokotlin")
                }
            }
        }
    }
}
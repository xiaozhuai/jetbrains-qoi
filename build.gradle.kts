plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.5.2"
}

group = "io.github.xiaozhuai"
version = "1.0.8"

repositories {
    mavenCentral()
}

intellij {
    version.set("2021.2")
    type.set("IC")

    plugins.set(listOf())
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }

    patchPluginXml {
        sinceBuild.set("212")
        untilBuild.set("222.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("INTELLIJ_CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("INTELLIJ_PRIVATE_KEY"))
        password.set(System.getenv("INTELLIJ_PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("INTELLIJ_PUBLISH_TOKEN"))
    }
}

dependencies {
    // !!! Do not forget to change QOIMetadata.QOI_LIBRARY_VERSION when upgrade qoi-java !!!
    implementation("me.saharnooby:qoi-java:1.2.1")
}

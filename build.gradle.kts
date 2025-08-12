plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.7.1"
}

group = "io.github.xiaozhuai"
version = "1.3.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    // !!! Do not forget to change QOIPluginConstants.QOI_VERSION when upgrade qoi-java !!!
    implementation("me.saharnooby:qoi-java:1.2.1")
    intellijPlatform {
        create("IU", "2025.2")
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "212"
        }
    }
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }

    buildSearchableOptions{
        enabled = false
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

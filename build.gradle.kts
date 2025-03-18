plugins {
    id("java")
}

group = "me.mantou"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.ow2.asm:asm:9.7.1")
}

tasks.test {
    useJUnitPlatform()
}
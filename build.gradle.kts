buildscript {
    repositories {
        google()  // Asegúrate de que esté presente
        mavenCentral()
    }
    dependencies {
        classpath("com.google.gms:google-services:4.3.15")  // o la versión más reciente
    }
}

allprojects {
    repositories {
        google()  // Asegúrate de que esté presente
        mavenCentral()
    }
}


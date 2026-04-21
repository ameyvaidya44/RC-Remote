allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

val newBuildDir: Directory =
    rootProject.layout.buildDirectory
        .dir("../../build")
        .get()
rootProject.layout.buildDirectory.value(newBuildDir)

subprojects {
    val newSubprojectBuildDir: Directory = newBuildDir.dir(project.name)
    project.layout.buildDirectory.value(newSubprojectBuildDir)
}

subprojects {
    project.evaluationDependsOn(":app")
}

subprojects {
    project.plugins.withId("com.android.library") {
        val androidExtension = project.extensions.findByName("android")
        if (androidExtension != null) {
            val extensionClass = androidExtension.javaClass

            // Force all library subprojects (e.g. flutter_bluetooth_serial) to
            // compile against a modern SDK so AAPT can resolve attrs like lStar.
            try {
                val setCompileSdkMethod = extensionClass.getMethod("setCompileSdkVersion", Int::class.java)
                setCompileSdkMethod.invoke(androidExtension, 34)
            } catch (_: Exception) {
                try {
                    val setCompileSdkMethod = extensionClass.getMethod("compileSdkVersion", Int::class.java)
                    setCompileSdkMethod.invoke(androidExtension, 34)
                } catch (_: Exception) { /* ignore */ }
            }

            try {
                val namespaceMethod = extensionClass.getMethod("getNamespace")
                val namespace = namespaceMethod.invoke(androidExtension) as? String
                if (namespace.isNullOrEmpty()) {
                    val setNamespaceMethod = extensionClass.getMethod("setNamespace", java.lang.String::class.java)
                    val groupName = project.group.toString()
                    val newNamespace = if (groupName.isNotEmpty()) groupName else "com.example.${project.name.replace("-", "_")}"
                    setNamespaceMethod.invoke(androidExtension, newNamespace)
                    println("Injected namespace $newNamespace into project ${project.name}")
                }
            } catch (e: Exception) {
                // Ignore if getNamespace or setNamespace doesn't exist
            }
        }
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}


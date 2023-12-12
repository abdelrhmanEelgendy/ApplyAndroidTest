import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

tasks.register<Zip>("zipPackageFiles") {
    dependsOn("moveFiles")
    description = "Zip files within a package and output the result in the same package"

    val gitTag = getGitTag()
    // Define the output directory for the zip file
    val outputDir = project.buildDir.resolve("${project.name}-$gitTag-release")

    // Set the base name of the zip file (without the extension)
    archiveBaseName.set("package")

    // Set the output directory for the zip file (same as the package directory)
    destinationDirectory.set(outputDir)

    // Include all files within the package directory
    from(outputDir) {
        include("**/*")
    }

    // Specify the output zip file name
    archiveFileName.set("$outputDir.zip")
}

// ----------------------------------------- AAR Checksum ------------------------------------------

tasks.register("calculateAarChecksum") {
    dependsOn("signAar")
    doLast {
        val gitTag = getGitTag()
        val aarDir = project.buildDir.resolve("outputs/aar")
        val pattern = Regex(".*-release.aar")
        val aarFiles = aarDir.listFiles { file -> pattern.matches(file.name) }
        val outputTextFile =
            project.buildDir.resolve("outputs/aar/${project.name}-$gitTag-checksum.txt")

        // Ensure the file exists before attempting to append
        if (outputTextFile.exists().not())
            outputTextFile.createNewFile()

        aarFiles?.forEach { aarFile ->
            val sha256Checksum = calculateSHA256Checksum(aarFile)
            println("SHA-256 Checksum of file=${aarFile.name} is: $sha256Checksum")

            // Append checksum information to the file
            outputTextFile.appendText("${aarFile.name}= $sha256Checksum\n")
        }
    }
}

fun calculateSHA256Checksum(file: File): String {
    val messageDigest = MessageDigest.getInstance("SHA-256")
    val inputStream = file.inputStream()

    try {
        val byteArray = ByteArray(8192)
        var bytesRead: Int

        while (inputStream.read(byteArray).also { bytesRead = it } != -1) {
            messageDigest.update(byteArray, 0, bytesRead)
        }
    } finally {
        inputStream.close()
    }

    val digestBytes = messageDigest.digest()
    val hexString = StringBuffer()

    for (byte in digestBytes) {
        hexString.append(String.format("%02x", byte).toUpperCase())
    }

    return hexString.toString()
}

// ---------------------------------------- Output Renaming ----------------------------------------

tasks.register("renameAarFiles") {
    dependsOn("calculateAarChecksum")
    description = "Renames AAR files in the build directory."

    val outputDir = project.buildDir.resolve("outputs/aar")
    val pattern = Regex(".*.aar")
    val gitTag = getGitTag()

    doLast {
        val aarFiles = outputDir.listFiles { file -> pattern.matches(file.name) }

        aarFiles?.forEach { aarFile ->
            val newName = aarFile.name.replace("${project.name}", "${project.name}-${gitTag}")
            val newFile = outputDir.resolve(newName)

            if (aarFile.renameTo(newFile))
                logger.info("Renamed AAR file: ${aarFile.name} to ${newFile.name}")
            else
                logger.error("Failed to rename AAR file: ${aarFile.name}")
        }
    }
}

// -------------------------------------------- Moving ---------------------------------------------

tasks.register("moveFiles") {
    dependsOn("dokkaJavadocJar", "renameAarFiles")
    description = "Move the release files (aar, checksum, and dokkajar) to the output dir."

    doLast {
        val gitTag = getGitTag()
        // Define the output directory for the zip file
        val outputDir = project.buildDir.resolve("${project.name}-$gitTag-release")
        if (outputDir.exists().not()) {
            outputDir.mkdirs()
        }

        // Define the names of the aar release files.
        val pattern = Regex(".*-release.aar")
        val aarFiles =
            project.buildDir.resolve("outputs/aar").listFiles { file -> pattern.matches(file.name) }

        val inputFiles = mutableListOf<File>()

        // Add AAR files to the list
        inputFiles.addAll(aarFiles)

        // Define the names of the input files
        val checksumFile = file("$buildDir/outputs/aar/${project.name}-${gitTag}-checksum.txt")
        val dokkaJarFile = file("$buildDir/libs/${project.name}-javadoc.jar")
        inputFiles.addAll(listOf(checksumFile, dokkaJarFile))

        inputFiles.forEach { fileName ->
            // Move the input files to the output directory
            val sourceFile = fileName.toPath()
            val destFile = outputDir.resolve(fileName.name).toPath()

            if (Files.exists(sourceFile)) {
                Files.move(sourceFile, destFile, StandardCopyOption.REPLACE_EXISTING)
                logger.info("Moved $fileName to $outputDir")
            } else
                logger.error("File $fileName does not exist in the source directory.")
        }
    }
}

// ---------------------------------------------- Git ----------------------------------------------

fun getGitTag(): String? {
    val result = "git describe --tags --abbrev=0".execute()
    if (result.exitValue == 0) {
        return result.text.trim()
    }
    return null
}

fun String.execute(): ProcessResult {
    val command = split("\\s".toRegex())
    val process = ProcessBuilder(command)
        .directory(project.rootDir)
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.PIPE)
        .start()

    val output = process.inputStream.bufferedReader().readText()
    val errorOutput = process.errorStream.bufferedReader().readText()
    val exitValue = process.waitFor()
    return ProcessResult(output, errorOutput, exitValue)
}

data class ProcessResult(val text: String, val errorText: String, val exitValue: Int)
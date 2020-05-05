package io.github.mforlini.kmparse

import com.xenomachina.argparser.*
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Paths

class MetadataArgs(parser: ArgParser) {
    val allFiles by parser.flagging(
        "-a", "--all",
        help = "force parsing of all files even when input contains a smali directory"
    )
    val force by parser.flagging(
        "-f", "--force",
        help = "delete destination directory"
    )
    val source by parser.positional(
        "SOURCE",
        help = "source filename"
    ) {
        try {
            Paths.get(this)
        } catch (e: InvalidPathException) {
            throw SystemExitException("Source is an invalid path", 1)
        }
    }.addValidator {
        if (!Files.exists(value)) {
            throw InvalidArgumentException("Source file or directory does not exist")
        }
    }

    val destination by parser.positional(
        "DEST",
        help = "destination directory"
    ) {
        try {
            Paths.get(this)
        } catch (e: InvalidPathException) {
            throw SystemExitException("Destination is an invalid path", 1)
        }
    }.default(Paths.get("").toAbsolutePath())
        .addValidator {
            if (Files.exists(value) && !force) {
                throw InvalidArgumentException("Destination directory already exists. Use --force (or -f) to force overwrite")
            }
        }
}

fun main(args: Array<String>) {
    mainBody(PROGRAM_NAME) {
        ArgParser(args = args, helpFormatter = DefaultHelpFormatter(PROGRAM_DESCRIPTION)).parseInto(::MetadataArgs)
            .run {
                val metadataInfoDirectory = destination.resolve(DIRECTORY_NAME)
                val metadataTree =
                    if (!allFiles && Files.exists(source.resolve(SMALI))) createMetadataTreeFromSmaliDirectories(source) else createMetadataTreeFromPath(
                        source
                    )
                if (metadataTree == null) {
                    println("No Kotlin annotated smali files found")
                } else {
                    try {
                        if (force) metadataInfoDirectory.toFile().deleteRecursively()
                        Files.createDirectories(metadataInfoDirectory)
                        metadataTreeToFile(metadataTree, metadataInfoDirectory)
                        println("Finished")
                    } catch (e: Exception) {
                        throw SystemExitException("Destination is not valid", 1)
                    }
                }
            }
    }

}
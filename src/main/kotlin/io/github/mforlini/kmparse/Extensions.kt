package io.github.mforlini.kmparse

import kotlinx.metadata.jvm.*
import org.apache.commons.text.StringEscapeUtils
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path

inline fun <T> Path.useLines(charset: Charset = Charsets.UTF_8, block: (Sequence<String>) -> T): T =
    Files.newBufferedReader(this, charset).use { block(it.lineSequence()) }

val Path.extension: String
    get() = fileName.toString().substringAfterLast('.', "")

fun <T> List<T>.plusNotNull(element: T?): List<T> = element?.let { this + element } ?: this

fun String.hexToIntOrNull(): Int? = this.trim().substringAfter("x").toIntOrNull(16)

fun String.unescape(): String = StringEscapeUtils.unescapeJava(this)

fun Sequence<String>.subsequenceBetween(startMarker: String, endMarker: String): Sequence<String> =
    this.dropWhile { !it.contains(startMarker) }
        .drop(1)
        .takeWhile { !it.contains(endMarker) }

fun KotlinClassHeader.toStringBlock(): String {
    return when (val metadata = KotlinClassMetadata.read(this)) {
        is KotlinClassMetadata.Class -> {
            val klass = metadata.toKmClass()
            //TODO Add Flags
            """
                    |Type: Class
                    |Class Info:
                    |    Name: ${klass.name}
                    |    Supertypes: ${klass.supertypes.joinToString(", ") { it.classifier.toString() }}
                    |    Module Name: ${klass.moduleName}
                    |    Type Aliases: ${klass.typeAliases.joinToString(", ")}
                    |    Companion Object: ${klass.companionObject ?: ""}
                    |    Nested Classes:  ${klass.nestedClasses.joinToString(", ")}
                    |    Enum Entries: ${klass.enumEntries.joinToString(", ")}
                    |
                    |Constructors:${klass.constructors.joinToString(separator = INDENT, prefix = INDENT) { "${it.signature}, Arguments: ${it.valueParameters.joinToString(", ") { arg -> arg.name }}" }}
                    |
                    |Functions:${klass.functions.joinToString(separator = INDENT, prefix = INDENT) { "${it.signature}, Arguments: ${it.valueParameters.joinToString(", ") { arg -> arg.name }}" }}
                    |
                    |Properties:${klass.properties.joinToString(separator = INDENT, prefix = INDENT) { "${it.fieldSignature}" }}
        """.trimMargin()
        }
        is KotlinClassMetadata.FileFacade -> {
            val klass = metadata.toKmPackage()
            """
                    |Type: File Facade
                    |
                    |Functions:${klass.functions.joinToString(separator = INDENT, prefix = INDENT) { "${it.signature}, Arguments: ${it.valueParameters.joinToString(", ") { arg -> arg.name }}" }}
                    |
                    |Properties:${klass.properties.joinToString(separator = INDENT, prefix = INDENT) { "${it.fieldSignature}" }}
        """.trimMargin()
        }
        is KotlinClassMetadata.SyntheticClass -> {
            if (metadata.isLambda) {
                val klass = metadata.toKmLambda()
                """
                |Type: Synthetic Class
                |Is Kotlin Lambda: True
                |
                |Functions:
                |    ${klass?.function?.signature}, Arguments: ${klass?.function?.valueParameters?.joinToString(", ") { it.name }}
                
            """.trimMargin()

            } else {
                """
                |Type: Synthetic Class
                |Is Kotlin Lambda: False
            """.trimMargin()

            }
        }
        is KotlinClassMetadata.MultiFileClassFacade -> """
            |Type: Multi-File Class Facade
            |This multi-file class combines:
            |${metadata.partClassNames.joinToString(separator = INDENT, prefix = INDENT) { "Class: $it" }}
        """.trimMargin()
        is KotlinClassMetadata.MultiFileClassPart -> {
            val klass = metadata.toKmPackage()
            """
                    |Type: Multi-File Class Part
                    |Name: ${metadata.facadeClassName}
                    |
                    |Functions:${klass.functions.joinToString(separator = INDENT, prefix = INDENT) { "${it.signature}, Arguments: ${it.valueParameters.joinToString(", ") { arg -> arg.name }}" }}
                    |
                    |Properties:${klass.properties.joinToString(separator = INDENT, prefix = INDENT) { "${it.fieldSignature}" }}
        """.trimMargin()
        }
        is KotlinClassMetadata.Unknown -> """Type: Unknown"""
        null -> ""
    }
}

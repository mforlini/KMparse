package io.github.mforlini.kmparse

const val PROGRAM_NAME = "kmparse"
const val PROGRAM_DESCRIPTION ="Parses Kotlin metadata annotations from smali files into human readable class information"
const val DIRECTORY_NAME = "KotlinClassMetadata"

const val SMALI = "smali"
const val OUT_EXTENSION = "txt"

const val ANNOTATION_START = ".annotation runtime Lkotlin"
const val ANNOTATION_END = ".end annotation"
const val INDENT = "\n|    "

const val DEFAULT_KIND = 1
const val DEFAULT_EXTRA_INT = 0
val DEFAULT_BYTECODE_VERSION = sequenceOf("0x1", "0x1", "0x3")
val DEFAULT_METADATA_VERSION = sequenceOf("0x1", "0x1", "0xd")
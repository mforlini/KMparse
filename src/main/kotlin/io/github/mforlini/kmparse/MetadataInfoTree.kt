package io.github.mforlini.kmparse

import kotlinx.metadata.jvm.KotlinClassHeader
import java.nio.file.Files
import java.nio.file.Path

sealed class FileTreeNode {
    data class DirectoryNode(
        val name: String,
        val children: List<FileTreeNode>
    ) : FileTreeNode()

    data class KotlinClassInfoNode(
        val name: String,
        val kotlinClassHeader: KotlinClassHeader,
        val description: String
    ) : FileTreeNode()
}

fun createMetadataTreeFromPath(path: Path): FileTreeNode? =
    if (Files.isDirectory(path)) createDirectoryNodeFromPath(path) else createKotlinClassInfoNodeFromPath(path)

fun createMetadataTreeFromSmaliDirectories(path: Path): FileTreeNode.DirectoryNode? =
    path.listOfEntries()
        .filter { it.fileName.toString().startsWith(SMALI) }
        .map { createMetadataTreeFromPath(it) }
        .filterIsInstance<FileTreeNode.DirectoryNode>()
        .let { collapseDirectoryNodes(it, SMALI) }
        .let { mergeIdenticallyNamedDescendants(it) }

fun createKotlinClassInfoNodeFromPath(path: Path): FileTreeNode.KotlinClassInfoNode? =
    parseSmaliFile(path)?.let { createKotlinClassInfoNode(path.fileName.toString().removeSuffix(".$SMALI"), it) }

fun createKotlinClassInfoNode(name: String, kotlinClassHeader: KotlinClassHeader): FileTreeNode.KotlinClassInfoNode =
    FileTreeNode.KotlinClassInfoNode(name, kotlinClassHeader, kotlinClassHeader.toStringBlock())

fun createDirectoryNodeFromPath(root: Path): FileTreeNode.DirectoryNode? {
    val children = root.listOfEntries().fold(listOf<FileTreeNode>())
    { acc, path ->
        if (path.extension == SMALI) acc.plusNotNull(createKotlinClassInfoNodeFromPath(path))
        else if (Files.isDirectory(path)) acc.plusNotNull(createDirectoryNodeFromPath(path))
        else acc
    }
    return if (children.isEmpty()) null else FileTreeNode.DirectoryNode(root.fileName.toString(), children)

}

fun mergeIdenticallyNamedDescendants(directoryNode: FileTreeNode.DirectoryNode): FileTreeNode.DirectoryNode {
    val subdirectoryList = directoryNode.children.filterIsInstance<FileTreeNode.DirectoryNode>()
    val mergedSubdirectoriesList = subdirectoryList
        .groupBy { it.name }
        .map { collapseDirectoryNodes(it.value, it.key) }
    val recursivelyMergedSubdirectoriesList = mergedSubdirectoriesList.map { mergeIdenticallyNamedDescendants(it) }
    return directoryNode.copy(children = recursivelyMergedSubdirectoriesList + directoryNode.children.filterIsInstance<FileTreeNode.KotlinClassInfoNode>())
}

fun collapseDirectoryNodes(directoryNodes: List<FileTreeNode.DirectoryNode>, name: String): FileTreeNode.DirectoryNode =
    FileTreeNode.DirectoryNode(
        name,
        directoryNodes.fold(listOf<FileTreeNode>()) { acc, dirNode -> acc + dirNode.children })

fun directoryNodeToFile(node: FileTreeNode.DirectoryNode, path: Path) {
    Files.createDirectory(path.resolve(node.name))
}

fun kotlinClassInfoNodeToFile(node: FileTreeNode.KotlinClassInfoNode, path: Path) {
    Files.newBufferedWriter(path.resolve("${node.name}.$OUT_EXTENSION")).use { out -> out.write(node.description) }
}

fun metadataTreeToFile(node: FileTreeNode, path: Path) {
    when (node) {
        is FileTreeNode.DirectoryNode -> {
            directoryNodeToFile(node, path)
            node.children.forEach { metadataTreeToFile(it, path.resolve(node.name)) }
        }
        is FileTreeNode.KotlinClassInfoNode -> kotlinClassInfoNodeToFile(node, path)
    }
}

fun parseSmaliFile(path: Path): KotlinClassHeader? = getKotlinClassHeader(readMetadataAnnotation(path).asSequence())

fun readMetadataAnnotation(path: Path): List<String> = path.toFile().useLines {
    return it.subsequenceBetween(ANNOTATION_START, ANNOTATION_END).toList()
}

fun getKotlinClassHeader(metadataSequence: Sequence<String>): KotlinClassHeader? {
    if (metadataSequence.none()) {
        return null
    }
    val kind = parseMetadataAnnotationSingleValue(metadataSequence, "k").hexToIntOrNull() ?: DEFAULT_KIND
    val extraInt = parseMetadataAnnotationSingleValue(metadataSequence, "xi").hexToIntOrNull() ?: DEFAULT_EXTRA_INT
    val extraString = parseMetadataAnnotationSingleValue(metadataSequence, "xs")
    val packageName = parseMetadataAnnotationSingleValue(metadataSequence, "pn")
    val bytecodeVersion = parseMetadataAnnotationArrayValue(metadataSequence, "bv", DEFAULT_BYTECODE_VERSION) {
        parseLine(it).hexToIntOrNull()
    }.toIntArray()
    val metadataVersion = parseMetadataAnnotationArrayValue(metadataSequence, "mv", DEFAULT_METADATA_VERSION) {
        parseLine(it).hexToIntOrNull()
    }.toIntArray()
    val data1 = parseMetadataAnnotationArrayValue(metadataSequence, "d1") { parseLine(it).unescape() }
    val data2 = parseMetadataAnnotationArrayValue(metadataSequence, "d2") { parseLine(it) }
    if (data1.isEmpty()) {
        return null
    }
    return KotlinClassHeader(
        kind,
        metadataVersion,
        bytecodeVersion,
        data1,
        data2,
        extraString,
        packageName,
        extraInt
    )
}

private fun parseLine(line: String): String =
    line.trim().removeSuffix(",").removeSurrounding("\"")

private fun parseMetadataAnnotationSingleValue(sequence: Sequence<String>, annotation: String): String =
    sequence.find { it.trim().startsWith("$annotation =") }?.substringAfter("=")?.let { parseLine(it) } ?: ""

private inline fun <reified T : Any> parseMetadataAnnotationArrayValue(
    sequence: Sequence<String>,
    annotation: String,
    default: Sequence<String> = sequenceOf(),
    noinline elementTransform: (String) -> T?
): Array<T> =
    sequence.subsequenceBetween("$annotation = {", "}")
        .ifEmpty { default }
        .map(elementTransform)
        .toList()
        .filterNotNull()
        .toTypedArray()
package com.csx.plugin


import org.xmlunit.builder.DiffBuilder
import org.xmlunit.diff.Diff
import java.io.File

private val TAG = "XmlUtil"
//
// 使用 XMLUnit 比较两个 XML 文件是否相似
fun areXmlFilesSimilar(file1: File, file2: File): Boolean {
    val diff: Diff = DiffBuilder.compare(file1)
        .withTest(file2)
        .ignoreWhitespace() // 忽略空白符的差异
        .checkForSimilar()  // 检查相似性，而不是完全相同
        .build()

    return !diff.hasDifferences()
}

/**
 *  遍历文件并比较内容相似的 XML 文件
 *
 *  注：该方法会遍历所有文件对进行比较，时间根据文件的多少，会增加时间
 */
fun findAndPrintSimilarXmlFiles(directory: File) {
    val startTime = System.currentTimeMillis()
    // 存储已比较过的文件对
    val checkedPairs = mutableSetOf<Pair<String, String>>()

    // 获取所有 XML 文件
    val xmlFiles = directory.walkTopDown().filter { it.isFile && it.extension == "xml" }.toList()
    println("$TAG xmlFiles xmlFiles = ${xmlFiles.size}")

    // 存储相似文件的 Map，键为文件哈希，值为文件列表
    val similarFilesMap = mutableMapOf<Int, MutableList<File>>()

    // 遍历所有文件对进行比较
    for (i in xmlFiles.indices) {
        for (j in i + 1 until xmlFiles.size) {
            val file1 = xmlFiles[i]
            val file2 = xmlFiles[j]
            val filePair = file1.name to file2.name

            // 检查是否已经比较过这对文件
            if (filePair in checkedPairs || filePair.swap() in checkedPairs) continue

            // 标记为已比较
            checkedPairs.add(filePair)

            // 检查文件是否相似
            if (areXmlFilesSimilar(file1, file2)) {
                val key = file1.hashCode() // 简单的用第一个文件的 hashCode 作为键（或可以用更合适的键）

                // 将相似的文件加入到 Map 中
                val similarList = similarFilesMap.getOrPut(key) { mutableListOf() }
                if (!similarList.contains(file1)) similarList.add(file1)
                if (!similarList.contains(file2)) similarList.add(file2)
            }
        }
    }

    // 打印出相似的 XML 文件
    similarFilesMap.forEach { (_, files) ->
        if (files.size > 1) {
            println("Found similar XML files:")
            files.forEach { file -> println(" - ${file.name}") }
        }
    }

    println("findAndPrintSimilarXmlFiles directory = ${directory} 该方法耗时：${(System.currentTimeMillis() - startTime)/1000}s")
}

fun Pair<String, String>.swap() = second to first


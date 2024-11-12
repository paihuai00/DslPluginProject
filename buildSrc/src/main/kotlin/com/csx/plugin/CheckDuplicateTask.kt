package com.csx.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

/**
 *
 * Author: cuishuxiang
 * Date  : 2024/10/31
 *
 *
 * 资源重复检测 task
 */
abstract class CheckDuplicateTask : DefaultTask() {
    @TaskAction
    fun perfromCheck() {
        checkDuplicateResources()
    }

    private fun checkDuplicateResources() {
        val rootDir = project.rootProject.rootDir
        val resDir = File(rootDir, "app/src/main/res")
        val BASE_DIR = "drawable-xhdpi"
        val baseDrawableDir = File(resDir, BASE_DIR)
        val baseDrawableSet = hashSetOf<String>()
        val drawableDirList = arrayListOf<java.io.File>()

        // 所有 drawable 目录
        resDir.walkTopDown().forEach { drawableDir ->
            if (drawableDir.isDirectory && drawableDir.name.startsWith("drawable")) {
                drawableDirList.add(drawableDir)
            }
        }

        // 基准目录文件
        baseDrawableDir.walkTopDown().forEach { file ->
            if (file.isFile) {
                baseDrawableSet.add(file.name)
            }
        }

        println("基准目录为: $BASE_DIR 文件个数为：${baseDrawableSet.size}")

        println("扫描不同目录中同名文件")
        drawableDirList.forEach { drawableDir ->
//        if (drawableDir.name != baseDrawableDir.name && !excludeDirSet.contains(drawableDir.name)) {
//            checkDuplicateFileInDiffDir(baseDrawableSet, drawableDir)
//        }
            if (drawableDir.name != baseDrawableDir.name) {
                checkDuplicateFileInDiffDir(baseDrawableSet, drawableDir)
            }
        }

        println("扫描同目录中同 MD5 文件")
        drawableDirList.forEach { drawableDir ->
//        checkDuplicateFileByMd5(drawableDir, excludeFileMap[drawableDir.name])
            checkDuplicateFileByMd5(drawableDir, null)
        }
    }

    // 示例：检查不同目录中的同名文件
    fun checkDuplicateFileInDiffDir(baseDrawableSet: Set<String>, drawableDir: java.io.File) {
        drawableDir.walkTopDown().forEach { file ->
            if (file.isFile && baseDrawableSet.contains(file.name)) {
                println("发现同名文件: ${file.absolutePath}")
            }
        }
    }

    private fun checkDuplicateFileByMd5(dir: java.io.File, excludeFile: Set<String>?) {
        // key 为 MD5 值，value 为文件名
        val fileMd5Map = hashMapOf<String, String>()
        // key 为 MD5 值，value 为文件name 列表
        val dupFileMap = hashMapOf<String, MutableList<String>>()
        val needExclude = excludeFile != null && excludeFile.isNotEmpty()

        // 遍历目录中的所有文件
        dir.walkTopDown().forEach { file ->
            if (file.isFile) {

                val md5 = generateMD5(file)

                //
                if (fileMd5Map.containsKey(md5)) {
                    if (needExclude && excludeFile!!.contains(file.name)) {
                        return@forEach
                    }

                    val fileNameList = dupFileMap.getOrPut(md5) {
                        mutableListOf<String>().apply {
                            fileMd5Map[md5]?.let { add(it) }
                        }
                    }
                    fileNameList.add(file.name)
                } else if (!needExclude || excludeFile?.contains(file.name) == false) {
                    fileMd5Map[md5] = file.name
                }
            }
        }

        if (dupFileMap.isNotEmpty()) {
            println("\n在目录 ${dir.name} 有重复 MD5 文件")
            println("重复 MD5 数量为 ${dupFileMap.size}")

            var dupFileSize = 0L
            dupFileMap.forEach { (md5, list) ->
                val sb = StringBuilder("MD5: $md5 --> ")
                val fileName = fileMd5Map[md5]
                val file = java.io.File(dir, fileName)
                dupFileSize += file.length() * (list.size - 1)

                list.forEach { name -> sb.append("$name, ") }
                sb.setLength(sb.length - 2) // 移除最后的 ", "
                println(sb.toString())
            }
        } else {
            println("目录 ${dir.name} 未发现同 MD5 文件")
        }


        findAndPrintSimilarXmlFiles(dir)
    }

    // 计算文件的 MD5 值
    fun generateMD5(file: java.io.File): String {
        val buffer = ByteArray(1024) // 读取文件的缓冲区
        val md = MessageDigest.getInstance("MD5") // 创建 MD5 摘要实例

        // 逐块读取文件内容并更新 MD5 摘要
        FileInputStream(file).use { fis ->
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                md.update(buffer, 0, bytesRead)
            }
        }

        // 获取 MD5 哈希值并转换为十六进制字符串
        return md.digest().joinToString("") { "%02x".format(it) }
    }

}
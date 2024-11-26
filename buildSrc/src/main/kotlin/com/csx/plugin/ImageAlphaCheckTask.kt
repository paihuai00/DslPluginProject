package com.csx.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.imageio.ImageIO
/**
 *
 * Author: cuishuxiang
 * Date  : 2024/11/21
 */

open class ImageAlphaCheckTask : DefaultTask() {
    companion object {
        // 支持的图片格式
        private val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg")

        // 输出颜色
        private const val ANSI_RESET = "\u001B[0m"
        private const val ANSI_RED = "\u001B[31m"
        private const val ANSI_GREEN = "\u001B[32m"
        private const val ANSI_YELLOW = "\u001B[33m"
        private const val ANSI_BLUE = "\u001B[34m"
    }

    data class ImageInfo(
        val file: File,
        val hasAlpha: Boolean,
        val hasTransparency: Boolean,
        val fileSize: Long,
        val dimension: Pair<Int, Int>,
        val alphaPercentage: Double,
        val resourceDir: String, // 资源目录名称
        val compressionType: String,
        val metrics: ImageCompressionMetrics
    )

    @TaskAction
    fun checkImages() {
        println("\n${ANSI_BLUE}开始检查项目图片资源...${ANSI_RESET}")

        // 直接指定res目录路径
        val resDir = project.file("app/src/main/res")
        if (!resDir.exists()) {
            println("${ANSI_RED}错误: res目录不存在: ${resDir.absolutePath}${ANSI_RESET}")
            return
        }

        println("资源目录: ${resDir.absolutePath}")
        val results = scanResDirectory(resDir)

        if (results.isEmpty()) {
            println("\n${ANSI_YELLOW}未找到任何图片资源，当前支持的格式: ${IMAGE_EXTENSIONS.joinToString()}${ANSI_RESET}")
            return
        }

        generateReport(results)
    }

    private fun scanResDirectory(resDir: File): List<ImageInfo> {
        val results = mutableListOf<ImageInfo>()

        resDir.listFiles()?.forEach { dir ->
            if (dir.isDirectory && dir.name.startsWith("drawable")) {
                println("\n扫描目录: ${dir.name}")
                dir.listFiles()?.forEach { file ->
                    if (file.isFile && IMAGE_EXTENSIONS.contains(file.extension.toLowerCase())) {
                        try {
                            analyzeImage(file, dir.name)?.let { imageInfo ->
                                results.add(imageInfo)
                                printImageAnalysis(imageInfo)
                            }
                        } catch (e: Exception) {
                            println("${ANSI_RED}分析图片失败: ${file.name}")
                            println("错误: ${e.message}${ANSI_RESET}")
                        }
                    }
                }
            }
        }
        return results
    }

    private fun analyzeImage(file: File, dirName: String): ImageInfo? {
        val image = ImageIO.read(file) ?: return null
        val hasAlpha = image.colorModel.hasAlpha()

        var transparentPixels = 0
        var totalAlpha = 0L
        var midValuePixels = 0
        var gradientCount = 0
        var edgePixels = 0
        val totalPixels = image.width * image.height

        if (hasAlpha) {
            val raster = image.alphaRaster
            for (y in 0 until image.height) {
                for (x in 0 until image.width) {
                    val alpha = (image.getRGB(x, y) shr 24) and 0xff
                    if (alpha < 255) transparentPixels++
                    if (alpha in 77..198) midValuePixels++ // 0.3-0.7 的alpha值
                    totalAlpha += alpha

                    // 简单的边缘检测
//                    if (x > 0 && y > 0 && x < image.width - 1 && y < image.height - 1) {
//                        raster?.getPixel(x-1, y, FloatArray(1)[0])
//                        val neighbors = listOf(
//                            raster.getPixel(x-1, y, [0f])[0],
//                            raster.getPixel(x+1, y, [0f])[0],
//                            raster.getPixel(x, y-1, [0f])[0],
//                            raster.getPixel(x, y+1, [0f])
//                        )
//                        if (neighbors.maxOrNull()!! - neighbors.minOrNull()!! > 50) {
//                            edgePixels++
//                            if (alpha != neighbors.first()) gradientCount++
//                        }
//                    }
                }
            }
        }

        val metrics = ImageCompressionMetrics(
            width = image.width,
            height = image.height,
            alphaAreaRatio = transparentPixels.toDouble() / totalPixels,
            edgeComplexity = edgePixels.toDouble() / totalPixels,
            gradientCount = gradientCount,
            alphaMidValueRatio = midValuePixels.toDouble() / totalPixels
        )

        val shouldUseLossless = shouldUseLossless(metrics)

        return ImageInfo(
            file = file,
            hasAlpha = hasAlpha,
            hasTransparency = transparentPixels > 0,
            fileSize = file.length(),
            dimension = Pair(image.width, image.height),
            alphaPercentage = (transparentPixels * 100.0) / totalPixels,
            resourceDir = dirName,
            compressionType = if (shouldUseLossless) "无损" else "有损",
            metrics = metrics
        )
    }
    private fun printImageAnalysis(info: ImageInfo) {
        val status = if (info.hasAlpha) "${ANSI_YELLOW}含Alpha${ANSI_RESET}" else "${ANSI_GREEN}无Alpha${ANSI_RESET}"
        println("检查: ${info.file.name} - $status")
        if (info.hasAlpha) {
            println("├── 建议压缩方式: ${info.compressionType}")
            println("├── 透明区域占比: %.2f%%".format(info.metrics.alphaAreaRatio * 100))
            println("└── 边缘复杂度: %.2f%%".format(info.metrics.edgeComplexity * 100))
        }

    }

    private fun generateReport(results: List<ImageInfo>) {
        println("\n${ANSI_GREEN}=== 图片资源分析报告 ===${ANSI_RESET}")

        // 基础统计
        val totalImages = results.size
        val alphaImages = results.count { it.hasAlpha }
        val transparentImages = results.count { it.hasTransparency }
        val totalSize = results.sumOf { it.fileSize }

        println("\n基础统计:")
        println("└── 总图片数: $totalImages")
        println("└── 包含Alpha通道的图片: $alphaImages")
        println("└── 实际使用透明度的图片: $transparentImages")
        println("└── 总占用空间: ${formatFileSize(totalSize)}")

        // 按目录统计
        println("\n${ANSI_BLUE}各目录统计:${ANSI_RESET}")
        results.groupBy { it.resourceDir }
            .forEach { (dir, images) ->
                val dirAlphaCount = images.count { it.hasAlpha }
                val dirSize = images.sumOf { it.fileSize }
                println("\n$dir:")
                println("└── 图片数量: ${images.size}")
                println("└── 包含Alpha通道: $dirAlphaCount")
                println("└── 占用空间: ${formatFileSize(dirSize)}")
            }

        // Alpha通道图片详情
        if (alphaImages > 0) {
            println("\n${ANSI_YELLOW}包含Alpha通道的图片详情:${ANSI_RESET}")
            results.filter { it.hasAlpha }
                .sortedByDescending { it.fileSize }
                .forEach { info ->
                    println("\n文件名: ${info.file.name}")
                    println("├── 所在目录: ${info.resourceDir}")
                    println("├── 尺寸: ${info.dimension.first}x${info.dimension.second}")
                    println("├── 文件大小: ${formatFileSize(info.fileSize)}")
                    println("└── 透明像素占比: %.2f%%".format(info.alphaPercentage))

                    // 优化建议
                    if (info.file.extension.toLowerCase() == "webp" && info.hasTransparency) {
                        println("    ${ANSI_YELLOW}! 建议使用无损WebP格式${ANSI_RESET}")
                    }
                    if (info.alphaPercentage < 1.0) {
                        println("    ${ANSI_YELLOW}! Alpha通道可能不必要${ANSI_RESET}")
                    }
                    if (info.fileSize > 50 * 1024) { // 大于50KB
                        println("    ${ANSI_YELLOW}! 文件较大，建议优化${ANSI_RESET}")
                    }
                }
        }

        // 优化建议
        println("\n${ANSI_GREEN}优化建议:${ANSI_RESET}")
        println("1. 不需要透明效果的图片应移除Alpha通道")
        println("2. PNG转WebP时，带Alpha通道的图片需使用无损压缩")
        println("3. 文件大于50KB的图片建议进行压缩处理")
        val potentialSavings = results.filter { it.hasAlpha && it.alphaPercentage < 1.0 }
            .sumOf { it.fileSize }
        if (potentialSavings > 0) {
            println("4. 潜在优化空间: ${formatFileSize(potentialSavings)}")
        }
    }

    private fun formatFileSize(size: Long): String = when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "%.2f KB".format(size / 1024.0)
        else -> "%.2f MB".format(size / (1024.0 * 1024.0))
    }

    // 添加判断规则
    data class ImageCompressionMetrics(
        val width: Int,
        val height: Int,
        val alphaAreaRatio: Double,    // 透明区域占比 (0-1)
        val edgeComplexity: Double,    // 边缘复杂度 (0-1)
        val gradientCount: Int,        // 渐变数量
        val alphaMidValueRatio: Double // alpha值0.3-0.7的占比
    )

    private fun shouldUseLossless(metrics: ImageCompressionMetrics): Boolean {
        if (metrics.width > 1000 || metrics.height > 1000) return true
        if (metrics.alphaAreaRatio > 0.3) return true
        if (metrics.edgeComplexity > 0.7) return true
        if (metrics.gradientCount > 100) return true
        if (metrics.alphaMidValueRatio > 0.4) return true
        return false
    }
}
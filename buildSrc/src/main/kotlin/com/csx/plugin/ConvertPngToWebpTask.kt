package com.csx.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.ln
import kotlin.math.pow

/**
 *
 * Author: cuishuxiang
 * Date  : 2024/11/9
 */

abstract class ConvertPngToWebpTask : DefaultTask() {
    @get:Input
    var quality: Float = 0.75f  // 0.0f - 1.0f

    @get:Input
    var minSizeKB: Int = 0

    @PathSensitive(PathSensitivity.RELATIVE)
    @get:InputDirectory
    var sourceDir: File = project.file("src/main/res")

    @get:OutputDirectory
    var outputDir: File = project.file("src/main/res")

    @get:Input
    var shouldDeleteOriginal: Boolean = false  // 是否删除原始文件

    private data class ConversionResult(
        val originalFile: File,
        val originalSize: Long,
        val convertedSize: Long,
        val success: Boolean,
        val error: String? = null,
        var originalDeleted: Boolean = false
    ) {
        val savedBytes: Long = originalSize - convertedSize
        val savedPercentage: Double = if (originalSize > 0) {
            (savedBytes * 100.0) / originalSize
        } else 0.0
    }

    init {
        description = "Converts PNG and JPG images to WebP format"
        group = "custom"
    }

    @TaskAction
    fun convert() {
        var successCount = 0
        var skipCount = 0
        var errorCount = 0
        var totalSaved = 0L
        var deleteCount = 0

        println("\nStarting image to WebP conversion...")
        if (shouldDeleteOriginal) {
            println("Original image files will be deleted after successful conversion")
        }

        // 注册 WebP 支持
        try {
            Class.forName("com.luciad.imageio.webp.WebPWriteParam")
            println("WebP encoder successfully loaded")
        } catch (e: Exception) {
            throw IllegalStateException("""
                Failed to load WebP encoder.
                Please check if webp-imageio library is properly added to the dependencies.
                Error: ${e.message}
            """.trimIndent())
        }

        // 处理PNG和JPG文件
        project.fileTree(sourceDir).matching {
            include("**/*.png", "**/*.jpg", "**/*.jpeg")
            // 排除.9.png文件和其他不需要处理的文件
            exclude("**/*.9.png", "**/drawable-nodpi/**", "**/raw/**")
        }.forEach { imageFile ->
            try {
                // 跳过小于最小大小限制的文件
                if (imageFile.length() / 1024 < minSizeKB) {
                    println("Skipping ${imageFile.name} (below ${minSizeKB}KB)")
                    skipCount++
                    return@forEach
                }

                processFile(imageFile)?.let { result ->
                    when {
                        result.success -> {
                            successCount++
                            totalSaved += result.savedBytes

                            // 转换成功后尝试删除原文件
                            if (shouldDeleteOriginal) {
                                if (result.originalFile.delete()) {
                                    deleteCount++
                                    result.originalDeleted = true
                                    println("Deleted original file: ${result.originalFile.name}")
                                } else {
                                    println("Warning: Failed to delete file: ${result.originalFile.name}")
                                }
                            }

                            println("""
                                Converted: ${imageFile.name}
                                - Original: ${formatFileSize(result.originalSize)}
                                - WebP: ${formatFileSize(result.convertedSize)}
                                - Saved: ${formatFileSize(result.savedBytes)} (${String.format("%.1f", result.savedPercentage)}%)
                            """.trimIndent())
                        }
                        result.error != null -> {
                            errorCount++
                            System.err.println("Error converting ${imageFile.name}: ${result.error}")
                        }
                    }
                }
            } catch (e: Exception) {
                errorCount++
                System.err.println("Error processing ${imageFile.name}: ${e.message}")
            }
        }

        println("""
            
            ===== Conversion Summary =====
            Successfully converted: $successCount
            Skipped: $skipCount
            Failed: $errorCount
            ${if (shouldDeleteOriginal) "Original files deleted: $deleteCount" else ""}
            Total space saved: ${formatFileSize(totalSaved)}
            ===========================
        """.trimIndent())
    }

    private fun processFile(imageFile: File): ConversionResult? {
        val relativePath = sourceDir.toPath().relativize(imageFile.toPath()).toString()
        // 将任何图片扩展名替换为.webp
        val webpFile = File(outputDir, relativePath.replace(Regex("\\.(png|jpg|jpeg)$"), ".webp"))

        // 确保输出目录存在
        webpFile.parentFile.mkdirs()

        return try {
            // 读取图片
            val image = ImageIO.read(imageFile) ?: throw IllegalStateException("Failed to read image file")

            // 转换为WebP
            val success = ImageIO.write(image, "webp", webpFile)
            if (!success) {
                throw IllegalStateException("No appropriate writer found for WebP format")
            }

            ConversionResult(
                originalFile = imageFile,
                originalSize = imageFile.length(),
                convertedSize = webpFile.length(),
                success = true
            )
        } catch (e: Exception) {
            ConversionResult(
                originalFile = imageFile,
                originalSize = imageFile.length(),
                convertedSize = 0,
                success = false,
                error = e.message
            )
        }
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val exp = (ln(bytes.toDouble()) / ln(1024.0)).toInt()
        val unit = "KMGTPE"[exp - 1]
        return String.format("%.1f %sB", bytes / 1024.0.pow(exp), unit)
    }
}
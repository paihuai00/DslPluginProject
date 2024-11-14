package com.csx.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import kotlin.math.ln
import kotlin.math.pow
/**
 * png、jpg 转 webp
 * Author: cuishuxiang
 * Date  : 2024/11/9
 */

abstract class ConvertImageToWebpTask : DefaultTask() {
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

    @get:Input
    var compressionMode: CompressionMode = CompressionMode.LOSSLESS  // 新增压缩模式选择

    /**
     *  白名单配置
     *     whitelistFiles = setOf(
     *         "logo.png",                  // 完整文件名匹配
     *         "app_icon.png",
     *         "splash_screen.png"
     *     )
     */
    @get:Input
    var whitelistFiles: Set<String> = emptySet()  // 文件名白名单

    /**
     * 使用通配符的模式匹配，如
     * setOf(
     *         "ic_*_logo.png",
     *         "banner_*.png",
     *         "*_dont_convert.png"
     *     )
     */
    @get:Input
    var whitelistPatterns: Set<String> = emptySet()  // 文件名模式白名单（支持通配符）


    private data class ConversionResult(
        val originalFile: File,
        val originalSize: Long,
        val convertedSize: Long,
        val success: Boolean,
        val error: String? = null,
        var originalDeleted: Boolean = false,
        val skippedReason: String? = null  // 新增跳过原因

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
        var whitelistSkipCount = 0  // 新增白名单跳过计数
        var errorCount = 0
        var totalSaved = 0L
        var deleteCount = 0

        println("\nStarting image to WebP conversion...")
        println("Compression Mode: ${compressionMode.name}")

        if (shouldDeleteOriginal) {
            println("Original image files will be deleted after successful conversion")
        }

        // 打印白名单信息
        if (whitelistFiles.isNotEmpty() || whitelistPatterns.isNotEmpty()) {
            println("\nWhitelist configuration:")
            if (whitelistFiles.isNotEmpty()) {
                println("Whitelisted files: ${whitelistFiles.joinToString(", ")}")
            }
            if (whitelistPatterns.isNotEmpty()) {
                println("Whitelisted patterns: ${whitelistPatterns.joinToString(", ")}")
            }
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
                // 检查白名单
                if (isFileWhitelisted(imageFile)) {
                    println("Skipping ${imageFile.name} (whitelisted)")
                    whitelistSkipCount++
                    return@forEach
                }


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
            Compression Mode: ${compressionMode.name}
            Skipped (size): $skipCount
            Skipped (whitelist): $whitelistSkipCount
            Failed: $errorCount
            ${if (shouldDeleteOriginal) "Original files deleted: $deleteCount" else ""}
            Total space saved: ${formatFileSize(totalSaved)}
            ===========================
        """.trimIndent())
    }

    private fun processFile(imageFile: File): ConversionResult? {
        val relativePath = sourceDir.toPath().relativize(imageFile.toPath()).toString()
        val webpFile = File(outputDir, relativePath.replace(Regex("\\.(png|jpg|jpeg)$"), ".webp"))

        // 确保输出目录存在
        webpFile.parentFile.mkdirs()

        return try {
            // 1. 验证源文件
            if (!imageFile.exists() || imageFile.length() == 0L) {
                throw IllegalStateException("Source file is empty or does not exist")
            }

            // 2. 读取图片并验证
            val image = ImageIO.read(imageFile)
            if (image == null) {
                throw IllegalStateException("Failed to read image file - ImageIO.read returned null")
            }

            // 3. 打印图片信息用于调试
            println("""
            Processing ${imageFile.name}:
            - Size: ${formatFileSize(imageFile.length())}
            - Dimensions: ${image.width}x${image.height}
            - Color model: ${image.colorModel::class.java.simpleName}
            - Has alpha: ${image.colorModel.hasAlpha()}
        """.trimIndent())

            // 4. 获取WebP writer并验证
            val writers = ImageIO.getImageWritersByMIMEType("image/webp")
            if (!writers.hasNext()) {
                throw IllegalStateException("No WebP writer found")
            }
            val writer = writers.next()

            // 5. 确保在转换失败时删除可能创建的空文件
            try {
                webpFile.outputStream().use { outputStream ->
                    val imageOutput = ImageIO.createImageOutputStream(outputStream)
                    writer.output = imageOutput

                    val writeParam = writer.defaultWriteParam
                    writeParam.compressionMode = ImageWriteParam.MODE_EXPLICIT

                    when (compressionMode) {
                        CompressionMode.LOSSLESS -> {
                            writeParam.compressionType = "Lossless"
                            println("Using Lossless compression")
                        }
                        CompressionMode.LOSSY -> {
                            writeParam.compressionType = "Lossy"
                            writeParam.compressionQuality = quality
                            println("Using Lossy compression with quality: $quality")
                        }
                    }

                    // 6. 执行转换
                    writer.write(null, IIOImage(image, null, null), writeParam)
                    imageOutput.flush()
                }

                // 7. 验证输出文件
                if (!webpFile.exists() || webpFile.length() == 0L) {
                    throw IllegalStateException("WebP conversion failed - output file is empty")
                }

                ConversionResult(
                    originalFile = imageFile,
                    originalSize = imageFile.length(),
                    convertedSize = webpFile.length(),
                    success = true
                )
            } catch (e: Exception) {
                // 如果转换失败，删除可能创建的空文件
                if (webpFile.exists() && webpFile.length() == 0L) {
                    webpFile.delete()
                }
                throw e
            } finally {
                writer.dispose()
            }
        } catch (e: Exception) {
            // 详细错误日志
            System.err.println("""
            Error converting ${imageFile.name}:
            - Error type: ${e.javaClass.simpleName}
            - Error message: ${e.message}
            - Stack trace:
            ${e.stackTrace.take(5).joinToString("\n")}
        """.trimIndent())

            // 清理可能的残留文件
            if (webpFile.exists() && webpFile.length() == 0L) {
                webpFile.delete()
            }

            ConversionResult(
                originalFile = imageFile,
                originalSize = imageFile.length(),
                convertedSize = 0,
                success = false,
                error = "${e.javaClass.simpleName}: ${e.message}"
            )
        }
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val exp = (ln(bytes.toDouble()) / ln(1024.0)).toInt()
        val unit = "KMGTPE"[exp - 1]
        return String.format("%.1f %sB", bytes / 1024.0.pow(exp), unit)
    }


    private fun isFileWhitelisted(file: File): Boolean {
        val fileName = file.name

        // 检查完整文件名匹配
        if (fileName in whitelistFiles) {
            return true
        }

        // 检查模式匹配
        return whitelistPatterns.any { pattern ->
            fileName.matches(pattern.replace("*", ".*").toRegex())
        }
    }
}
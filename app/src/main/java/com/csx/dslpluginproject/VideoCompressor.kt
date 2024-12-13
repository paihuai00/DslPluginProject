package com.csx.dslpluginproject
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import android.media.MediaCodecInfo

/**
 *
 * Author: cuishuxiang
 * Date  : 2024/12/12
 *
 *
 * 该方法可以正常压缩视频，耗时放在子线程执行
 *
 * todo：
 *  1. 参数没有调优
 *  2. 视频旋转问题
 *  3. 调研过程中测试代码留存
 */

class VideoCompressor {
    companion object {
        private const val TAG = "VideoCompressor"
        private const val TIMEOUT_USEC = 10000L
        private const val VIDEO_MIME_TYPE = "video/avc" // H.264 Advanced Video Coding
        // 颜色格式为 YUV420
        private const val COLOR_FORMAT = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible

        // 目标参数
        private const val TARGET_LONG_EDGE = 1920
        private const val TARGET_SHORT_EDGE = 1080
        private const val TARGET_FRAMERATE = 60

        // 码率配置（根据分辨率调整）
        private const val HIGH_QUALITY_BITRATE = 40_000_000  // 40Mbps for 1080p60
        private const val MEDIUM_QUALITY_BITRATE = 30_000_000 // 30Mbps for 1080p30
        private const val BASE_QUALITY_BITRATE = 20_000_000   // 20Mbps for 720p

        private const val I_FRAME_INTERVAL = 1 // 关键帧间隔(秒)


        private const val QUALITY_PROFILE = MediaCodecInfo.CodecProfileLevel.AVCProfileHigh
        private const val QUALITY_LEVEL = MediaCodecInfo.CodecProfileLevel.AVCLevel51
    }

    private val isCompressing = AtomicBoolean(false)


    fun compressVideo(inputPath: String): String {
        if (isCompressing.get()) {
            Log.w(TAG, "已有压缩任务正在进行中")
            throw IllegalStateException("视频压缩任务正在进行中")
        }

        val startTime = System.currentTimeMillis()
        isCompressing.set(true)
        Log.i(TAG, "开始压缩视频: $inputPath")

        try {
            val inputFile = File(inputPath)
            val outputPath = generateOutputPath(inputPath)
            Log.d(TAG, "输出文件路径: $outputPath")

            val extractor = MediaExtractor()
            extractor.setDataSource(inputPath)

            val videoTrackIndex = findVideoTrack(extractor)
            if (videoTrackIndex < 0) {
                throw IllegalArgumentException("未找到视频轨道")
            }

            val inputFormat = extractor.getTrackFormat(videoTrackIndex)
            Log.d(TAG, "原始视频格式: $inputFormat")

            // 创建编码器以获取颜色格式支持
            val encoder = MediaCodec.createEncoderByType(VIDEO_MIME_TYPE)
            val outputParams = calculateOutputParams(inputFormat, encoder)
            val outputFormat = createOutputVideoFormat(outputParams)

            encoder.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

            val decoder = MediaCodec.createDecoderByType(inputFormat.getString(MediaFormat.KEY_MIME)!!).apply {
                configure(inputFormat, null, null, 0)
            }

            val muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4).apply {
                setOrientationHint(outputParams.rotation)
            }

            compressVideoInternal(extractor, decoder, encoder, muxer, videoTrackIndex)

            val endTime = System.currentTimeMillis()
            val duration = (endTime - startTime) / 1000f
            Log.i(TAG, "视频压缩完成，耗时: ${String.format("%.1f", duration)}秒")

            // 输出文件大小对比
            val inputSize = inputFile.length() / 1024f / 1024f
            val outputSize = File(outputPath).length() / 1024f / 1024f
            Log.i(TAG, "文件大小对比: 原始文件: ${String.format("%.2f", inputSize)}MB, " +
                    "压缩后: ${String.format("%.2f", outputSize)}MB, " +
                    "压缩率: ${String.format("%.1f", (1 - outputSize/inputSize) * 100)}%")

            return outputPath

        } catch (e: Exception) {
            Log.e(TAG, "视频压缩失败", e)
        } finally {
            isCompressing.set(false)
        }

        return ""
    }

    private fun generateOutputPath(inputPath: String): String {
        val inputFile = File(inputPath)
        val fileName = inputFile.nameWithoutExtension
        val extension = inputFile.extension
        return "${inputFile.parent}/${fileName}_compress.$extension"
    }

    private fun findVideoTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("video/") == true) {
                extractor.selectTrack(i)
                return i
            }
        }
        return -1
    }

    private fun createOutputVideoFormat(params: VideoParams): MediaFormat {
        return MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, params.width, params.height).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, params.bitRate)
            setInteger(MediaFormat.KEY_FRAME_RATE, params.frameRate)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)
            setInteger(MediaFormat.KEY_COLOR_FORMAT, params.colorFormat)

            // 保持原始视频方向
            setInteger(MediaFormat.KEY_ROTATION, params.rotation)

            // 设置颜色参数
            params.colorRange?.let {
                setInteger(MediaFormat.KEY_COLOR_RANGE, it)
            }
            params.colorStandard?.let {
                setInteger(MediaFormat.KEY_COLOR_STANDARD, it)
            }
            params.colorTransfer?.let {
                setInteger(MediaFormat.KEY_COLOR_TRANSFER, it)
            }

            // 设置编码质量参数
            setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh)
            setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel51)

            // 使用 CBR 模式可能有助于保持颜色稳定
            setInteger(MediaFormat.KEY_BITRATE_MODE,
                MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR)

            try {
                // 其他质量设置
                setInteger("quality", 100)
                setInteger("priority", 0)
                setFloat("quality-scale", 1.0f)
            } catch (e: Exception) {
                Log.w(TAG, "部分质量增强设置不支持: ${e.message}")
            }
        }

    }


    private fun compressVideoInternal(
        extractor: MediaExtractor,
        decoder: MediaCodec,
        encoder: MediaCodec,
        muxer: MediaMuxer,
        videoTrackIndex: Int
    ) {
        val startTime = System.currentTimeMillis()
        var isEncoderDone = false
        var isDecoderDone = false
        var muxerTrackIndex = -1
        var totalFramesProcessed = 0

        // 启动编解码器
        decoder.start()
        encoder.start()

        try {
            while (!isEncoderDone && !isDecoderDone) {
                // 1. 从输入文件读取数据并送入解码器
                if (!isDecoderDone) {
                    val inputBufferId = decoder.dequeueInputBuffer(TIMEOUT_USEC)
                    if (inputBufferId >= 0) {
                        val inputBuffer = decoder.getInputBuffer(inputBufferId)!!
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)

                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(inputBufferId, 0, 0, 0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            isDecoderDone = true
                            Log.d(TAG, "解码器输入结束")
                        } else {
                            decoder.queueInputBuffer(inputBufferId, 0, sampleSize,
                                extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                // 2. 处理解码器输出并送入编码器
                val decoderBufferInfo = MediaCodec.BufferInfo()
                val encoderBufferInfo = MediaCodec.BufferInfo()

                val outputBufferId = decoder.dequeueOutputBuffer(decoderBufferInfo, TIMEOUT_USEC)
                if (outputBufferId >= 0) {
                    val outputBuffer = decoder.getOutputBuffer(outputBufferId)!!

                    val inputBufferId = encoder.dequeueInputBuffer(TIMEOUT_USEC)
                    if (inputBufferId >= 0) {
                        val inputBuffer = encoder.getInputBuffer(inputBufferId)!!
                        inputBuffer.put(outputBuffer)

                        encoder.queueInputBuffer(inputBufferId, 0,
                            decoderBufferInfo.size, decoderBufferInfo.presentationTimeUs,
                            decoderBufferInfo.flags)
                    }

                    decoder.releaseOutputBuffer(outputBufferId, false)
                    totalFramesProcessed++

                    // 每处理100帧打印一次进度
                    if (totalFramesProcessed % 100 == 0) {
                        val currentTime = System.currentTimeMillis()
                        val elapsedSeconds = (currentTime - startTime) / 1000f
                        Log.d(TAG, "已处理 $totalFramesProcessed 帧，" +
                                "耗时: ${String.format("%.1f", elapsedSeconds)}秒")
                    }
                }

                // 3. 处理编码器输出并写入文件
                var encoderOutputAvailable = true
                while (encoderOutputAvailable) {
                    val encoderStatus = encoder.dequeueOutputBuffer(encoderBufferInfo, TIMEOUT_USEC)

                    if (encoderStatus >= 0) {
                        val encodedData = encoder.getOutputBuffer(encoderStatus)!!

                        if (encoderBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                            encoderBufferInfo.size = 0
                        }

                        if (encoderBufferInfo.size > 0) {
                            if (muxerTrackIndex == -1) {
                                val trackFormat = encoder.getOutputFormat()
                                muxerTrackIndex = muxer.addTrack(trackFormat)
                                muxer.start()
                                Log.d(TAG, "Muxer started")
                            }

                            encodedData.position(encoderBufferInfo.offset)
                            encodedData.limit(encoderBufferInfo.offset + encoderBufferInfo.size)

                            muxer.writeSampleData(muxerTrackIndex, encodedData, encoderBufferInfo)
                        }

                        encoder.releaseOutputBuffer(encoderStatus, false)

                        if (encoderBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            isEncoderDone = true
                            val endTime = System.currentTimeMillis()
                            val duration = (endTime - startTime) / 1000f
                            Log.d(TAG, "编码完成，共处理 $totalFramesProcessed 帧，" +
                                    "编码耗时: ${String.format("%.1f", duration)}秒")
                            break
                        }
                    } else if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        encoderOutputAvailable = false
                    } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (muxerTrackIndex >= 0) {
                            throw RuntimeException("格式已更改")
                        }
                        muxerTrackIndex = muxer.addTrack(encoder.getOutputFormat())
                        muxer.start()
                        Log.d(TAG, "Muxer started after format changed")
                    }
                }
            }
        } finally {
            // 清理资源
            decoder.stop()
            decoder.release()
            encoder.stop()
            encoder.release()
            extractor.release()
            muxer.stop()
            muxer.release()
        }
    }

    private fun calculateOutputParams(inputFormat: MediaFormat, encoder: MediaCodec): VideoParams {
        val inputWidth = inputFormat.getInteger(MediaFormat.KEY_WIDTH)
        val inputHeight = inputFormat.getInteger(MediaFormat.KEY_HEIGHT)
        val rotation = if (inputFormat.containsKey(MediaFormat.KEY_ROTATION)) {
            inputFormat.getInteger(MediaFormat.KEY_ROTATION)
        } else {
            0
        }

        // 获取颜色相关参数
        val colorFormat = getSupportedColorFormat(encoder, VIDEO_MIME_TYPE)
        val colorRange = if (inputFormat.containsKey(MediaFormat.KEY_COLOR_RANGE)) {
            inputFormat.getInteger(MediaFormat.KEY_COLOR_RANGE)
        } else null

        val colorStandard = if (inputFormat.containsKey(MediaFormat.KEY_COLOR_STANDARD)) {
            inputFormat.getInteger(MediaFormat.KEY_COLOR_STANDARD)
        } else null

        val colorTransfer = if (inputFormat.containsKey(MediaFormat.KEY_COLOR_TRANSFER)) {
            inputFormat.getInteger(MediaFormat.KEY_COLOR_TRANSFER)
        } else null

        // [尺寸和帧率计算逻辑保持不变...]

        // 输出参数信息
        Log.i(TAG, """
            视频编码参数:
            分辨率: ${inputWidth}x${inputHeight}
            旋转角度: $rotation°
            颜色格式: $colorFormat
            颜色范围: $colorRange
            颜色标准: $colorStandard
            颜色转换: $colorTransfer
        """.trimIndent())

        return VideoParams(
            width = inputWidth,
            height = inputHeight,
            frameRate = TARGET_FRAMERATE,
            bitRate = HIGH_QUALITY_BITRATE,
            rotation = rotation,
            colorFormat = colorFormat,
            colorRange = colorRange,
            colorStandard = colorStandard,
            colorTransfer = colorTransfer
        )
    }

    private fun getSupportedColorFormat(encoder: MediaCodec, mime: String): Int {
        val codecInfo = getCodecInfo(encoder) ?: return COLOR_FORMAT
        val capabilities = codecInfo.getCapabilitiesForType(mime)

        // 尝试查找最佳的颜色格式
        val supportedColorFormats = capabilities.colorFormats
        Log.d(TAG, "支持的颜色格式: ${supportedColorFormats.joinToString()}")

        return when {
            supportedColorFormats.contains(MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar) ->
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar
            supportedColorFormats.contains(MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar) ->
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar
            supportedColorFormats.contains(MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar) ->
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
            supportedColorFormats.contains(MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible) ->
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
            else -> COLOR_FORMAT
        }
    }
    private fun getCodecInfo(codec: MediaCodec): MediaCodecInfo? {
        return try {
            codec.codecInfo
        } catch (e: Exception) {
            Log.w(TAG, "无法获取编解码器信息: ${e.message}")
            null
        }
    }

    private data class VideoParams(
        val width: Int,
        val height: Int,
        val frameRate: Int,
        val bitRate: Int,
        val rotation: Int,
        val colorFormat: Int,
        val colorRange: Int?,
        val colorStandard: Int?,
        val colorTransfer: Int?
    )
}
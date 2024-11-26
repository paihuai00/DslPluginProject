// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.0.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
    id("org.jetbrains.kotlin.jvm") version "1.9.0" apply false
}



// 配置检查重复类的任务
task("CheckDuplicateTask", type = com.csx.plugin.CheckDuplicateTask::class){


}

task("ImageAlphaCheckTask", type = com.csx.plugin.ImageAlphaCheckTask::class){

}


tasks.register<com.csx.plugin.ConvertImageToWebpTask>("convertPngToWebp") {
    quality = 0.75f // 设置转换质量
    // 可选：自定义输入输出目录
    sourceDir = rootProject.project("app").file("src/main/res")
    outputDir = rootProject.project("app").file("src/main/res")
    minSizeKB = 50  // 只转换大于 50KB 的图片

    shouldDeleteOriginal = true // 是否删除原 png 文件

    // 有损、无损压缩（默认无损）
    compressionMode = com.csx.plugin.CompressionMode.LOSSLESS

    // 白名单配置
//    whitelistFiles = setOf(
//        "logo.png",                  // 完整文件名匹配
//        "app_icon.png",
//        "splash_screen.png"
//    )
//
//    whitelistPatterns = setOf(
//        "ic_*_logo.png",
//        "banner_*.png",
//        "*_dont_convert.png"
//    )
}
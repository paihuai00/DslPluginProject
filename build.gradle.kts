// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.0.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
    id("org.jetbrains.kotlin.jvm") version "1.9.0" apply false
}



// 配置检查重复类的任务
task("CheckDuplicateTask", type = com.csx.plugin.CheckDuplicateTask::class){


}


tasks.register<com.csx.plugin.ConvertImageToWebpTask>("convertPngToWebp") {
    quality = 0.75f // 设置转换质量
    // 可选：自定义输入输出目录
    sourceDir = rootProject.project("app").file("src/main/res")
    outputDir = rootProject.project("app").file("src/main/res")
    minSizeKB = 50  // 只转换大于 50KB 的图片

    shouldDeleteOriginal = true // 是否删除原 png 文件
}
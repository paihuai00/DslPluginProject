// buildSrc/build.gradle.kts
plugins {
    `kotlin-dsl` // 启用 Kotlin DSL 支持
}

repositories {
    maven {
        url = uri("https://maven.aliyun.com/repository/gradle-plugin") // 阿里云 Gradle 插件镜像
    }
    maven("https://maven.aliyun.com/repository/google")
    maven("https://maven.aliyun.com/repository/public")
    maven("https://maven.aliyun.com/repository/central")
    mavenCentral()
}

dependencies {
    // 添加 XMLUnit 依赖
    implementation("org.xmlunit:xmlunit-core:2.9.0")


    implementation("org.sejda.imageio:webp-imageio:0.1.6")
}

gradlePlugin{
//    plugins{
//        // create("simplePlugin") 表示创建一个名为 "simplePlugin" 的插件配置。
//        // 这里的 "simplePlugin" 是一个本地标识符，用于在 gradlePlugin 配置中区分多个插件。
//        create("simplePlugin"){
//            // 插件 ID:id 是插件的唯一标识符，它是插件被应用时使用的 ID。例如，其他项目可以使用以下方式应用此插件：
//            //  plugins {
//            //    id("com.csx.gradleplugin")
//            //  }
//            id = "com.csx.gradleplugin"
//            // 插件实现类:implementationClass
//            implementationClass = "com.csx.plugin.SimpleGradlePlugin"
//        }
//    }
}
// buildSrc/build.gradle.kts
plugins {
    `kotlin-dsl` // 启用 Kotlin DSL 支持
}

repositories {
    mavenCentral() // 添加所需的 Maven 仓库
}

dependencies {

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
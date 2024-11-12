plugins {
//    id("java-library")
//    id("org.jetbrains.kotlin.jvm")
    `kotlin-dsl` // 启用 Kotlin DSL 支持
    `maven-publish`

}
repositories {
    mavenCentral() // 添加所需的 Maven 仓库
}


gradlePlugin{
    plugins{
        // create("simplePlugin") 表示创建一个名为 "simplePlugin" 的插件配置。
        // 这里的 "simplePlugin" 是一个本地标识符，用于在 gradlePlugin 配置中区分多个插件。
        create("simplePlugin"){
            // 插件 ID:id 是插件的唯一标识符，它是插件被应用时使用的 ID。例如，其他项目可以使用以下方式应用此插件：
            //  plugins {
            //    id("com.csx.gradleplugin")
            //  }
            id = "com.csx.gradleplugin"
            // 插件实现类:implementationClass
            implementationClass = "com.csx.plugins.SimpleGradlePlugin"
        }
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.8.10")
}

publishing {
    publications {

        create<MavenPublication>("release") {
            from(components["kotlin"]) // 如果是 Kotlin 项目，可以改为 components["kotlin"]

            groupId = "com.github.paihuai00"
            artifactId = "csx_gradle_plugin"
            version = "1.0.0"

            // 可选：添加其他元数据
//            pom {
//                name.set("你的库的名称")
//                description.set("库的描述信息")
//                url.set("https://github.com/你的GitHub用户名/你的仓库名")
//
//                licenses {
//                    license {
//                        name.set("MIT License")
//                        url.set("https://opensource.org/licenses/MIT")
//                    }
//                }
//                developers {
//                    developer {
//                        id.set("你的GitHub用户名")
//                        name.set("你的名字")
//                        email.set("你的邮件")
//                    }
//                }
//                scm {
//                    connection.set("scm:git:github.com/你的GitHub用户名/你的仓库名.git")
//                    developerConnection.set("scm:git:ssh://github.com/你的GitHub用户名/你的仓库名.git")
//                    url.set("https://github.com/你的GitHub用户名/你的仓库名")
//                }
//            }
        }
    }

}
package com.csx.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 *
 * Author: cuishuxiang
 * Date  : 2024/11/12
 */
class SimpleGradlePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        println("hello gradle plugin ~! this is a simple gradle plugin !!   JavaLib")
    }

}
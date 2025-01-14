/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015, Minecrell <https://github.com/Minecrell>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.cadixdev.gradle.licenser

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import java.nio.file.Paths

class LicenserPluginFunctionalTest extends Specification {
    @Rule
    TemporaryFolder temporaryFolder = new TemporaryFolder()

    def "can run licenseCheck task"() {
        given:
        def projectDir = temporaryFolder.newFolder()
        new File(projectDir, "settings.gradle") << ""
        new File(projectDir, "build.gradle") << """
            plugins {
                id('org.cadixdev.licenser')
            }
        """.stripIndent()

        when:
        def runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("licenseCheck")
        runner.withProjectDir(projectDir)
        def result = runner.build()

        then:
        result.task(":checkLicenses").outcome == TaskOutcome.UP_TO_DATE
        result.task(":licenseCheck").outcome == TaskOutcome.UP_TO_DATE
    }

    def "skips existing headers in checkLicenses task"() {
        given:
        def projectDir = temporaryFolder.newFolder()
        def sourceDir = projectDir.toPath().resolve(Paths.get("src", "main", "java", "com", "example")).toFile()
        sourceDir.mkdirs()
        new File(projectDir, "header.txt") << "New copyright header"
        new File(projectDir, "settings.gradle") << ""
        new File(projectDir, "build.gradle") << """
            plugins {
                id('java')
                id('org.cadixdev.licenser')
            }
            
            license {
                header = project.file('header.txt')
                skipExistingHeaders = true
            }
        """.stripIndent()
        new File(sourceDir, "MyClass.java") << """
            /*
             * Existing copyright header
             */
            
            package com.example;
            
            class MyClass {}
        """.stripIndent()

        when:
        def runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("checkLicenses")
        runner.withProjectDir(projectDir)
        def result = runner.build()

        then:
        result.task(":checkLicenses").outcome == TaskOutcome.SUCCESS
    }

    def "skips existing headers in updateLicenses task"() {
        given:
        def projectDir = temporaryFolder.newFolder()
        def sourceDir = projectDir.toPath().resolve(Paths.get("src", "main", "java", "com", "example")).toFile()
        sourceDir.mkdirs()
        new File(projectDir, "header.txt") << "New copyright header"
        new File(projectDir, "settings.gradle") << ""
        new File(projectDir, "build.gradle") << """
            plugins {
                id('java')
                id('org.cadixdev.licenser')
            }
            
            license {
                header = project.file('header.txt')
                skipExistingHeaders = true
            }
        """.stripIndent()
        def sourceFileContent = """\
            /*
             * Existing copyright header
             */
            
            package com.example;
            
            class MyClass {}
        """.stripIndent()
        def sourceFile = new File(sourceDir, "MyClass.java") << sourceFileContent

        when:
        def runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("updateLicenses")
        runner.withProjectDir(projectDir)
        runner.debug = true
        def result = runner.build()

        then:
        result.task(":updateLicenses").outcome == TaskOutcome.UP_TO_DATE
        sourceFile.text == sourceFileContent
    }

    def "updates invalid headers in updateLicenses task when skipExistingHeaders=true"() {
        given:
        def projectDir = temporaryFolder.newFolder()
        def sourceDir = projectDir.toPath().resolve(Paths.get("src", "main", "java", "com", "example")).toFile()
        sourceDir.mkdirs()
        new File(projectDir, "header.txt") << "New copyright header"
        new File(projectDir, "settings.gradle") << ""
        new File(projectDir, "build.gradle") << """
            plugins {
                id('java')
                id('org.cadixdev.licenser')
            }
            
            license {
                header = project.file('header.txt')
                skipExistingHeaders = true
            }
        """.stripIndent()
        def sourceFileContent = """\
            //
            // Existing copyright header
            //
            
            package com.example;
            
            class MyClass {}
        """.stripIndent()
        def sourceFile = new File(sourceDir, "MyClass.java") << sourceFileContent

        when:
        def runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("updateLicenses")
        runner.withProjectDir(projectDir)
        runner.debug = true
        def result = runner.build()

        then:
        result.task(":updateLicenses").outcome == TaskOutcome.SUCCESS
        sourceFile.text == """\
            /*
             * New copyright header
             */
            
            //
            // Existing copyright header
            //
            
            package com.example;
            
            class MyClass {}
        """.stripIndent()
    }

    def "can run licenseFormat task"() {
        given:
        def projectDir = temporaryFolder.newFolder()
        new File(projectDir, "settings.gradle") << ""
        new File(projectDir, "build.gradle") << """
            plugins {
                id('org.cadixdev.licenser')
            }
        """.stripIndent()

        when:
        def runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("licenseFormat")
        runner.withProjectDir(projectDir)
        def result = runner.build()

        then:
        result.output.contains("Task :updateLicenses UP-TO-DATE")
        result.output.contains("Task :licenseFormat UP-TO-DATE")
        result.task(":updateLicenses").outcome == TaskOutcome.UP_TO_DATE
        result.task(":licenseFormat").outcome == TaskOutcome.UP_TO_DATE
    }

    def "supports custom source sets task"() {
        given:
        def projectDir = temporaryFolder.newFolder()
        new File(projectDir, "settings.gradle") << ""
        new File(projectDir, "build.gradle") << """
            plugins {
                id('org.cadixdev.licenser')
                id('java')
            }
            sourceSets {
                mySourceSet {}
            }
        """.stripIndent()

        when:
        def runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("licenseCheck")
        runner.withProjectDir(projectDir)
        def result = runner.build()

        then:
        result.task(":checkLicenseMySourceSet").outcome == TaskOutcome.NO_SOURCE
    }

    def "supports custom style"() {
        given:
        def projectDir = temporaryFolder.newFolder()
        def sourcesDir = new File(projectDir, "sources")
        sourcesDir.mkdirs()
        new File(projectDir, "settings.gradle") << ""
        new File(projectDir, "header.txt") << "Copyright header"
        def sourceFile = new File(sourcesDir, "source.c") << "TEST"
        new File(projectDir, "build.gradle") << """
            plugins {
                id('org.cadixdev.licenser')
            }
            
            license {
                header = project.file("header.txt")
                style {
                    c = 'BLOCK_COMMENT'
                }
                tasks {
                    sources {
                        files = project.files("sources")
                        include("**/*.c")
                    }
                }
            }
        """.stripIndent()

        when:
        def runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("updateLicenses")
        runner.withProjectDir(projectDir)
        def result = runner.build()

        then:
        result.task(":updateLicenseCustomSources").outcome == TaskOutcome.SUCCESS
        sourceFile.text == """\
            /*
             * Copyright header
             */
             
            TEST
            """.stripIndent()
    }
}

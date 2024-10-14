package net.biberius.gradle.plugin.patch

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.Copy

class PatchExtension {
    private final Project project

    File sourcesDir
    File diffsDir

    PatchExtension(final Project project) {
        this.project = project
    }

    def createPatchesTasks(String configuration, List<Map<String, String>> dataList, String patchDir = '') {
        project.extensions.getByType(JavaPluginExtension).sourceSets.each {
            it.java { sds -> sds.srcDir(sourcesDir) }
        }
        def tasks = project.tasks
        def provider = tasks.register(configuration)

        def cfg = project.configurations.findByName(configuration)
        if (cfg == null) {
            throw new IllegalArgumentException("Non existing configuration '$configuration'")
        }

        def sourceJars = this.project.with {
            p -> cfg.grep { it.name.indexOf('sources') >= 0 }.collect { p.zipTree(it) }
        }
        if (sourceJars.empty) {
            throw new IllegalStateException("No sources for configuration '$configuration'")
        }

        dataList.each {
            def originalFile = it.in
            def mm = (originalFile =~ /^.+\/([^\/]+)\.java$/)
            def baseName
            if (mm) {
                baseName = mm.group(1)
            } else {
                throw new IllegalStateException("Not matches: $originalFile")
            }

            def patchedFile = it.out ?: it.in
            mm = (patchedFile =~ /^.+\/([^\/]+)\.java$/)
            def destBaseName
            if (mm) {
                destBaseName = mm.group(1)
            } else {
                throw new IllegalStateException("Not matches: $patchedFile")
            }

            def diffFile = it.diff ?: "${destBaseName}.java.diff"

            if (!tasks.findByName("extract$baseName")) {
                tasks.register("extract$baseName", Copy) {
                    it.from sourceJars
                    it.into it.temporaryDir
                    it.include originalFile
                }
            }

            tasks.register("patch$destBaseName") {
                it.dependsOn "extract$baseName"
                def targetFile = new File(sourcesDir, patchedFile)
                it.outputs.file targetFile

                File patchFile = new File("$diffsDir/$patchDir", diffFile)
                it.inputs.file patchFile
                def original = tasks["extract${baseName}"].outputs.files.iterator().next()
                it.inputs.dir original

                sourcesDir.mkdirs()
                it.doLast {
                    ant.patch(
                            failonerror: true,
                            backups: false,
                            destfile: targetFile,
                            originalfile: "$original/$originalFile",
                            patchfile: patchFile
                    )
                }
            }
            provider.get().dependsOn("patch$destBaseName")
        }
        tasks.named('applyPatches').configure { it.dependsOn(configuration) }
    }
}

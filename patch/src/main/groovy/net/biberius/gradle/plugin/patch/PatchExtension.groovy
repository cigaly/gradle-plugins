package net.biberius.gradle.plugin.patch

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.Copy

class PatchExtension {
    private final Project project

    File sourcesDir
    File diffsDir

    PatchExtension(final Project project) {
        this.project = project
    }

    def createPatchesTasks(
            String configurationName,
            List<Map<String, String>> dataList,
            String patchDir = '',
            Object deps = []
    ) {
        apply([
                'configuration': configurationName,
                'list'         : dataList,
                'subdir'       : patchDir,
                'dependencies' : deps
        ])
    }

    def apply(Map<String, Object> data) {
        def badKeys = new HashSet<>(data.keySet())
        badKeys.removeAll(['configuration', 'list', 'subdir', 'dependencies'])
        if (!badKeys.empty) {
            throw new IllegalArgumentException("Unknown keys '$badKeys'")
        }
        String configurationName = data['configuration'] ?: "patch${UUID.randomUUID().toString().replaceAll('-', '')}"
        project.extensions.getByType(JavaPluginExtension).sourceSets
                .findByName('main')?.java { sds -> sds.srcDir(sourcesDir) }

        Configuration configuration = project.configurations.maybeCreate(configurationName)
        configuration.canBeConsumed = false
        def dependencies = listOfDependencies(data['dependencies'] ?: [])
        if (!dependencies.empty) {
            addDependencies(configuration, dependencies)
        }

        def tasks = project.tasks
        def provider = tasks.register(configurationName)

        def sourceJars = this.project.with {
            p -> configuration.grep { it.name.indexOf('sources') >= 0 }.collect { p.zipTree(it) }
        }
        if (sourceJars.empty) {
            throw new IllegalStateException("No sources for configurationName '$configurationName'")
        }

        data['list'].each {
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

                File patchFile = new File("$diffsDir/${data['subdir'] ?: ''}", diffFile)
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
        tasks.named('applyPatches').configure { it.dependsOn(configurationName) }
    }

    private static List listOfDependencies(dependencies) {
        if (dependencies instanceof String || dependencies instanceof GString) {
            return List.of(dependencies)
        } else if (dependencies.class.isArray()) {
            return Arrays.asList(dependencies)
        }
        if (!dependencies instanceof Collection) {
            throw new IllegalArgumentException('Dependencies parameter must be either colelction, or array, or string')
        }
        dependencies
    }

    private Configuration addDependencies(Configuration configuration, List dependencies) {
        project.dependencies.with { dependencyHandler ->
            configuration.withDependencies { set ->
                dependencies.each { dep ->
                    try {
                        def dependency = dependencyHandler.create(dep)
                        set.add(dependency)
                    } catch (Exception e) {
                        println e.toString()
                        println e.message
                        println dep
                        throw e
                    }
                }
            }
        }
    }
}

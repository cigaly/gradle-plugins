package net.biberius.gradle.plugin.patch


import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.TaskContainer

class PatchPlugin implements Plugin<Project> {

    private Set<Task> collectDependencies(Task task, Set<Task> collectedTasks, TaskContainer tasks) {
        collectedTasks.add(task)
        task.dependsOn.each { String name -> collectDependencies(tasks.named(name).get(), collectedTasks, tasks) }
        return collectedTasks
    }

    @Override
    void apply(final Project target) {
        def applyPatchesTask = target.tasks.register('applyPatches')

        target.tasks.named('compileJava').getOrNull()?.configure { it.dependsOn applyPatchesTask }

        target.extensions.create('patch', PatchExtension, target)

        target.tasks.register('removePatchedSources') {
            def mainSourcesSet = target.extensions.getByType(JavaPluginExtension).sourceSets
                    .named('main')
            def sourceDirs = mainSourcesSet.present
                    ? mainSourcesSet.get().java.sourceDirectories.files.collect { it.toURI() }
                    : []

            collectDependencies(applyPatchesTask.get(), new HashSet<Task>(), target.tasks)
                    .collectMany { it.outputs.files.files }
                    .collect { f -> f.toURI() }
                    .findAll { uout -> sourceDirs.any { uri -> !uri.relativize(uout).isAbsolute() } }
                    .each { new File(it).delete() }
        }
    }
}

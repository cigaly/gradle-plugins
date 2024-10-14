package net.biberius.gradle.plugin.patch

import org.gradle.api.Plugin
import org.gradle.api.Project

class PatchPlugin implements Plugin<Project> {

    @Override
    void apply(final Project target) {
        def task = target.tasks.register('applyPatches')

        target.tasks.findByName('compileJava')?.configure { it.dependsOn task }

        target.extensions.create('patch', PatchExtension, target)
    }
}

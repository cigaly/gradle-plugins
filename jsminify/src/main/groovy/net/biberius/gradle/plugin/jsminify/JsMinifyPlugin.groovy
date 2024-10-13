package net.biberius.gradle.plugin.jsminify

import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

@CompileStatic
@TypeChecked
class JsMinifyPlugin implements Plugin<Project> {
    private Configuration configuration

    @Override
    void apply(final Project target) {
        JsMinifyExtension extension = target.extensions.create('jsminify', JsMinifyExtension, target)

        configuration = target.configurations.create('jsminify')
        configuration.defaultDependencies { dependencies ->
            dependencies.add(target.dependencies.create("$extension.group:$extension.artifact:$extension.version"))
        }
    }
}

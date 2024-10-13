package net.biberius.gradle.plugin.jsminify

import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.StopExecutionException

import java.security.MessageDigest

import static TaskUtils.uptodate

@CompileStatic
@TypeChecked
class JsMinifyExtension {

    public static final String COMPILER_GROUP = 'com.google.javascript'
    public static final String COMPILER_ARTIFACT = 'closure-compiler'
    public static final String COMPILER_VERSION = 'v20240317'

    private final Project project
    String group = COMPILER_GROUP
    String artifact = COMPILER_ARTIFACT
    String version = COMPILER_VERSION

    JsMinifyExtension(final Project project) {
        this.project = project
    }

    String minify(File source, File target, String taskName = null, List<String> extraArgs = []) {
        if (!source.exists()) {
            println "Missing source $source"
            return null
        }
        if (!taskName) {
            def digest = MessageDigest.getInstance('sha1')
            digest.update(source.path.bytes)
            taskName = "minify-${new BigInteger(1, digest.digest()).toString(16)}"
        }
        project.tasks.register(taskName, JavaExec) {
            it.description = "Minify $source"
            it.classpath = project.files(project.configurations.named('jsminify'))

            it.args = extraArgs + ['--js', source.path, '--js_output_file', target.path]

            it.doFirst {
                if (uptodate(source, target)) {
                    throw new StopExecutionException()
                }
            }
            it.doLast {
                println "Minify $source to $target."
            }
        }
        return taskName
    }
}

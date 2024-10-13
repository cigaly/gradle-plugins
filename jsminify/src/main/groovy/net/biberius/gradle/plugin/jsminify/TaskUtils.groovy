package net.biberius.gradle.plugin.jsminify

import java.time.Instant

class TaskUtils {
    static String lastModified(f) {
        if (f instanceof String)
            return lastModified(new File(f))
        else
            return Instant.ofEpochMilli(f.lastModified())
    }

    static boolean uptodate(source, target, verbose = false, tolerance = 25) {
        if (source instanceof String) source = new File(source)
        if (target instanceof String) target = new File(target)
        if (verbose) {
            println "Source $source : ${lastModified(source)}"
            println "Target $target : ${lastModified(target)}"
        }
        return target.exists() && target.lastModified() + tolerance >= source.lastModified()
    }
}

package fr.coppernic.gradle.git

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository

import java.nio.file.Path
import java.util.regex.Matcher

/**
 * @author bastien
 */
public class GitUtils {

    final Git git;
    final Repository repository

    GitUtils(Path path) {
        git = Git.open(path.toFile())
        repository = git.getRepository()
    }

    int getVersionCode() {
        String code = getVersionCodeFromName(getVersionName())
        int ret = 1
        try {
            ret = code.toInteger()
        } catch (NumberFormatException ignore) {
            //println "Version code is ${code}, use $ret as version code"
        }
        return ret
    }

    static String getVersionCodeFromName(String name) {
        Matcher m = (name =~ '([0-9]+)[.]([0-9]+)[.]([0-9]+)')
        if (m.find()) {
            int n1 = Integer.parseInt(m.group(1))
            int n2 = Integer.parseInt(m.group(2))
            int n3 = Integer.parseInt(m.group(3))
            int n = (n1 * 10000) + (n2 * 100) + n3
            return "$n"
        } else {
            return name
        }
    }

    String getVersionNameSlow() {
        String name = repository.getBranch()
        def tags = repository.getTags()
        def revTags = [:]
        tags.reverseEach {
            revTags[it.key] = it.value

        }
        //println revTags
        revTags.find { String k, Ref v ->
            //println "${k} -> ${v.getObjectId().name()}"
            if (v.getObjectId().name() == name) {
                name = k
            }
        }
        return name
    }

    String getVersionName() {
        String name = repository.getBranch()
        String res = ""
        def tags = repository.getTags()
        //println tags
        tags.each { String k, Ref v ->
            // If several tags are matching the commit where we are, we are getting the tag that
            // has the less '-' in its name
            if (v.getObjectId().name() == name
                    && (res.isEmpty() || res.count("-") >= k.count("-"))) {
                res = k
            }
        }
        if (res.isEmpty()) {
            res = name
        }
        // Some tags contains a '/', we replace it by the next letter Uppercase
        return res.replaceAll(/\/\w/) { it[1].toUpperCase() }
    }
}

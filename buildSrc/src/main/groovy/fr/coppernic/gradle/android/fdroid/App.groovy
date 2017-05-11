package fr.coppernic.gradle.android.fdroid

import fr.coppernic.gradle.git.GitUtils
import org.gradle.api.Project
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Class representing an App that can be uploaded to fdroid server
 * @author Bastien Paul
 */
public class App {

    static final boolean DEBUG = false

    static Pattern sPatternAppName = ~/^([a-zA-Z0-9_.]+)(-\w+)?(-\d+\.\d+\.\d+)?(-\w+)?[.](\w+)$/
    static Pattern patAaptAppId = ~/name='([a-zA-Z0-9_.-]+)'/
    static Pattern patVersionName = ~/versionName='([a-zA-Z0-9_.-]+)'/
    static Pattern patVersion = ~/(\w+)?(-\d+\.\d+\.\d+)?(-\w+)?/

    /**
     * Base Name
     */
    final String name
    /**
     * Version prefix. It also contains branch name if it is the version
     */
    final String versionPrefix
    /**
     * Version base. It is of the form x.x.x where x is a number
     */
    final String versionBase
    /**
     * Version suffix
     */
    final String versionSuffix
    /**
     * Product name
     */
    final String productName
    /**
     * Extension
     */
    final String ext = "apk"
    /**
     * Application id
     */
    final String appId

    Logger logger = LoggerFactory.getLogger(this.getClass())

    /**
     * Create an App object from the full name gotten by ls linux command.
     * @param s Full apk name
     * @return App
     */
    static App fromLsLine(String s) {
        String aName, vPrefix, vBase, vSuffix
        Matcher m = sPatternAppName.matcher(s)
        if (m != null && m.find()) {
            aName = getGroupValue(m, 1)
            vPrefix = getGroupValue(m, 2)
            vBase = getGroupValue(m, 3)
            vSuffix = getGroupValue(m, 4)
        } else {
            aName = s
            vPrefix = ""
            vBase = ""
            vSuffix = ""
        }
        return new App(aName, vPrefix, vBase, vSuffix, "", "")
    }

    /**
     * Create an App object from a project. It is getting name, version and appId from project to
     * create the app.
     * @param project Gradle project
     * @param id app id
     * @return App
     */
    static App fromProject(Project project, String id) {
        String vName, vPrefix, vBase, vSuffix, aName
        aName = project.name
        vName = project.versionName
        Matcher m = patVersion.matcher(vName)
        if (m != null && m.find()) {
            vPrefix = getGroupValue(m, 1)
            vBase = getGroupValue(m, 2)
            vSuffix = getGroupValue(m, 3)
        } else {
            return null
        }
        new App(aName, vPrefix, vBase, vSuffix, id, "")
    }

    /**
     * Create an App from an aapt line.
     *
     * The aapt command to get the line is : "aapt dump badging {app} | grep package"
     *
     * @param appName Full app name
     * @param line Aapt line
     * @return App
     */
    static App fromAaptLine(String appName, String line, List<String> productNames) {
        String id, vName, vPrefix, vBase, vSuffix, name, pName
        Matcher m = patAaptAppId.matcher(line)
        if (m != null && m.find()) {
            id = getGroupValue(m, 1)
        } else {
            return null
        }
        m = patVersionName.matcher(line)
        if (m != null && m.find()) {
            vName = getGroupValue(m, 1)
        } else {
            return null
        }
        m = patVersion.matcher(vName)
        if (m != null && m.find()) {
            vPrefix = getGroupValue(m, 1)
            vBase = getGroupValue(m, 2)
            vSuffix = getGroupValue(m, 3)
        } else {
            return null
        }
        m = sPatternAppName.matcher(appName.trim())
        if (m != null && m.find()) {
            name = getGroupValue(m, 1)
            pName = getProductNameFromPrefix(getGroupValue(m, 2), productNames)
        } else {
            println "no match for '$appName'"
            return null
        }
        new App(name, vPrefix, vBase, vSuffix, id, pName)
    }

    /**
     * Sometime Apk name is like this one : CpcHdkConeSample-cone-1.0.0-EB1.apk. Here 'cone' is
     * the product name and not the version prefix. So we need to extract the product name to
     * construct the good apk name to remove the old version when updating app.
     * @param prefix Version prefix
     * @param productName List of available product names
     * @return Product name or empty string
     */
    static String getProductNameFromPrefix(String prefix, List<String> productName) {
        for (String s : prefix.split("-")) {
            if (!s.isEmpty()) {
                prefix = s
                break;
            }
        }
        for (String n : productName) {
            if (prefix == n) {
                return n
            }
        }
        return ""
    }

    private App(String n, String vP, String vB, String vS, String id, String pN) {
        name = n
        versionPrefix = prependSep(vP)
        versionBase = prependSep(vB)
        versionSuffix = prependSep(vS)
        appId = id
        productName = pN
    }

    /**
     * Put a "-" at start of string if there is not
     * @param s String
     * @return new String
     */
    private static String prependSep(String s) {
        if (s.isEmpty()) {
            return s
        } else {
            s.startsWith("-") ? s : "-$s"
        }
    }

    void log(String s) {
        if (DEBUG) {
            logger.quiet s
        } else {
            logger.debug s
        }
    }

    /**
     *
     * @return human readable string representation
     */
    @Override
    String toString() {
        return "${getFileName()} : $appId"
    }

    /**
     * @return Full version string
     */
    String getVersionString() {
        "$versionPrefix$versionBase$versionSuffix"
    }

    /**
     *
     * @return Full filename
     */
    String getFileName() {
        return "$name-$productName$versionPrefix$versionBase$versionSuffix.$ext"
    }

    /**
     * Function that hold the logic to tell if App needs to be updated
     * @param remoteApp App representing the one on Fdroid server
     * @return true if the App should be updated
     */
    boolean shouldUpdate(App remoteApp) {
        String remoteCode = GitUtils.getVersionCodeFromName("${remoteApp.getVersionString()}")
        String localCode = GitUtils.getVersionCodeFromName("${getVersionString()}")

        log "remote code : $remoteCode"
        log "localCode : $localCode"

        if (!remoteCode.isNumber()) {
            // Case where remote remoteApp is on a branch. We assume that we are compiling a more recent
            // version of the branch
            log "Case where remote remoteApp is on a branch. We assume that we are compiling a " +
                    "more recent version of the branch"
            return true
        } else if (remoteCode.isNumber() && !localCode.isNumber()) {
            // Case where local is on a branch and remote is released. We don't want to update.
            log "Case where local is on a branch and remote is released. We don't want to update."
            return false
        } else {
            int res = compareString(localCode, remoteCode)
            if (res == 0) {
                return shouldUpdateInternal(remoteApp)
            } else {
                return res > 0
            }
        }
    }

    boolean shouldUpdateInternal(App remoteApp) {
        if (compareString(remoteApp.versionPrefix, versionPrefix) != 0) {
            log "not the same branch, impossible to tell who is newer"
            return false
        } else if (compareString(versionBase, remoteApp.versionBase) != 0) {
            log "version base not equal"
            return compareString(versionBase, remoteApp.versionBase) > 0
        } else if (compareString(versionSuffix, remoteApp.versionSuffix) != 0) {
            log "suffix not equal"
            return compareString(versionSuffix, remoteApp.versionSuffix) > 0
        } else {
            log "exactly the same, do not update"
            return false
        }
    }

    /**
     * Match the app with name and AppId
     *
     * Name can be the same and App id different
     * @param remote App
     * @return true if they correspond
     */
    boolean doCorrespond(App remote) {
        this.name == remote.name && this.appId == remote.appId
    }

    /**
     * Compare string without taking the "-" into account
     *
     * "-master" and "master" are the same !
     * @param s1 first
     * @param s2 second
     * @return comparison result
     */
    static int compareString(String s1, String s2) {
        String one = s1.replaceAll(~/^-/, "")
        String two = s2.replaceAll(~/^-/, "")
        return one.compareTo(two)
    }

    static String getGroupValue(Matcher m, int i) {
        m.group(i) == null ? "" : m.group(i)
    }
}

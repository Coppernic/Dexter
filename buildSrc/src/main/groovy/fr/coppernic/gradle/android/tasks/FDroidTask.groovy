package fr.coppernic.gradle.android.tasks

import fr.coppernic.gradle.android.fdroid.App
import fr.coppernic.gradle.android.fdroid.UpdateData
import org.gradle.api.DefaultTask
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.TaskAction
import org.hidetake.groovy.ssh.Ssh
import org.hidetake.groovy.ssh.core.Service

import java.nio.file.Paths
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 */
public class FDroidTask extends DefaultTask {

    static final boolean DEBUG = false

    static Pattern sPatternApk = ~/^.+[.]apk$/

    static final String REPO = "repo"
    static final String METADATA = "metadata"
    static final String SEP = "@"

    private Service ssh
    private Logger logger
    List<App> appList = []
    List<String> productName = []

    @TaskAction
    public void action() {
        logger = project.logger
        ssh = Ssh.newService()
        ssh.settings.logging = 'none'

        if (DEBUG) {
            println project.name
            println project.versionCode
            println project.versionName
        }

        // Get available product names for Apk name resolution
        fillProductName()
        // Configure SSH object with some handy methods
        configureSsh()
        // Get the list of remote application alredy installed with their version and application id
        fillRemoteAppList()

        List<UpdateData> updateData = []
        // For each variant, we will create a corresponding App object associated with a remote
        // object if the app is already on fdroid. We will then look at them to see if an update
        // is accurate.
        project.android.applicationVariants.each { variant ->
            if (variant.buildType.name == "release") {
                variant.outputs.each { output ->
                    logger.debug "get corresponding remote app for ${project.name} " +
                            "and ${variant.applicationId}"

                    String appId = variant.applicationId
                    App local = App.fromProject(project, appId)
                    App remote = getCorrespondingRemoteApp(local)
                    File outputFile = output.outputFile

                    UpdateData data = new UpdateData(local, remote, outputFile, appId)
                    data.loadMetadata(project, variant)

                    updateData.add(data)
                }
            }
        }
        // Update FDroid with all data fetched before
        updateFDroid(updateData)
    }

    void configureSsh() {
        String repoPath = getFDroidRepoPath()
        String fDroidPath = getFDroidPath()
        ssh.settings {
            // Function that give the list of remote application with their version and
            // application id
            extensions.add getRemoteAppList: {
                List<App> remoteAppList = []
                ByteArrayOutputStream out = new ByteArrayOutputStream()

                String cmd = "find $repoPath -name \"*.apk\" -exec bash -c \"" +
                        "basename -z \"{}\" && " +
                        "echo -n \"${SEP}\" && " +
                        "/home/fdroid/android-sdk-linux/build-tools/24.0.2/aapt " +
                        "dump badging {} | grep package\" \\;"
                execute(cmd, outputStream: out)
                //println out.toString('UTF-8')
                out.toString('UTF-8').eachLine { String line ->
                    String[] array = line.split(SEP)
                    App app = App.fromAaptLine(array[0], array[1], productName)
                    if (app) {
                        remoteAppList.add(app)
                    } else {
                        println "Cannot create app for ${array[0]} and ${array[1]}"
                    }
                }
                return remoteAppList
            }
            // Method that copy an app and write its metadata
            extensions.add copyApp: { UpdateData data ->
                App local = data.local
                App remote = data.remote

                if (local) {
                    if (remote) {
                        String p = data.getRemoteFullPath(repoPath)
                        logger.debug "Remove ${p}"
                        remove p
                    } else if (DEBUG) {
                        println "No remote app found for $local"
                    }
                    // Copy apk
                    String target = data.getTargetOutputFilePath(repoPath)
                    logger.quiet "Copy from: $data.outputFile, into: ${target}"
                    put from: data.outputFile, into: target
                    // Copy metadata file
                    if (data.copyMetadata) {
                        String metaPath = data.getMetadataPath(getMetadataPath())
                        logger.quiet "Copy metadata, into: ${metaPath}"
                        put text: data.metadata.metadataFileContent, into: metaPath
                    }
                } else {
                    logger.quiet "No app to update"
                }
            }
            // Refresh fdroid server
            extensions.add refresh: {
                String script = """#!/bin/sh
cd ${fDroidPath}
fdroid update -c –v"""
                String temporaryPath = "/tmp/${UUID.randomUUID()}"

                try {
                    execute "mkdir -vp $temporaryPath"
                    put text: script, into: "$temporaryPath/script.sh"
                    execute "chmod +x $temporaryPath/script.sh"
                    def result = execute "$temporaryPath/script.sh"
                    println result
                } finally {
                    execute "rm -vfr $temporaryPath"
                }
            }

        }
    }

    void fillProductName() {
        project.rootProject.signingKeys.each {
            productName.add(it.name)
        }
    }

    void fillRemoteAppList() {
        ssh.run {
            session(project.rootProject.remotes.fdroid) {
                appList = getRemoteAppList()
            }
        }
    }

    void updateFDroid(List<UpdateData> updateData) {
        ssh.run {
            session(project.rootProject.remotes.fdroid) {
                updateData.each { UpdateData update ->
                    if (update.shouldUpdate()) {
                        // Call ssh extension configured in configureSsh()
                        copyApp(update)
                    } else {
                        logger.quiet "Remote app is already the most recent one for " +
                                "${update.getOutputFileName()}"
                    }
                }
                refresh()
            }
        }
    }

    static void printObj(Object obj) {
        def filtered = ['class', 'active']
        println obj.properties
                .sort { it.key }
                .collect { it }
                .findAll { !filtered.contains(it.key) }
                .join('\n')
    }

    static List<String> getAppList(String s) {
        List<String> list = []
        s.eachLine { String line ->
            Matcher m = sPatternApk.matcher(line)
            if (m != null && m.find()) {
                list.add(line)
            }
        }
        return list
    }

    static App getAppFromName(List<App> list, String name) {
        list.find { App app ->
            return app.name.equals(name)
        }
    }

    App getCorrespondingRemoteApp(App local) {
        appList.find { App app ->
            local.doCorrespond(app)
        }
    }

    String getFDroidRepoPath() {
        Paths.get(project.rootProject.remotes.fdroid.fdroidPath, REPO).toString()
    }

    String getMetadataPath() {
        Paths.get(project.rootProject.remotes.fdroid.fdroidPath, METADATA).toString()
    }

    String getFDroidPath() {
        Paths.get(project.rootProject.remotes.fdroid.fdroidPath).toString()
    }
}

package fr.coppernic.gradle.android.fdroid

import org.apache.commons.io.FilenameUtils
import org.gradle.api.Project

import java.nio.file.Paths;

/**
 * Created on 30/11/16
 *
 * @author bastien
 */

public class UpdateData {

    /**
     * Variable taht represent the local application to upload to fdroid server
     */
    final App local
    /**
     * App that represent the App in FDroid to be replaced.
     */
    final App remote
    /**
     * File to transfert to fdroid
     */
    final File outputFile
    /**
     * Metadata file to write in fdroid
     */
    final Metadata metadata = new Metadata()
    /**
     * Application id
     */
    final String appId

    boolean copyMetadata = true

    UpdateData(App local, App remote, File outputFile,
               String appId) {
        this.local = local
        this.remote = remote
        this.outputFile = outputFile
        this.appId = appId
    }

    /**
     * Load fdroid.properties file
     * @param project Gradle project
     * @param variant build variant
     */
    void loadMetadata(Project project, variant) {
        try {
            metadata.loadFile(project.file('fdroid.properties'))
            metadata.insertProjectData(project, variant)
        } catch (FileNotFoundException ignore) {
            copyMetadata = false
        }
    }

    boolean shouldUpdate() {
        remote ? local.shouldUpdate(remote) : true
    }

    String getOutputFileName() {
        FilenameUtils.getName(outputFile.toString())
    }

    String getRemoteFullPath(String repoPath) {
        assert repoPath
        Paths.get(repoPath, remote.getFileName()).toString()
    }

    String getTargetOutputFilePath(String repoPath) {
        assert repoPath
        Paths.get(repoPath, getOutputFileName()).toString()
    }

    String getMetadataPath(String metaPath) {
        assert metaPath
        Paths.get(metaPath, getMetadataFileName()).toString()
    }

    String getMetadataFileName() {
        appId + ".txt"
    }

}

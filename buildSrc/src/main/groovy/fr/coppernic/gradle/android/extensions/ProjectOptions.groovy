package fr.coppernic.gradle.android.extensions;

/**
 * Options for the project
 */
public class ProjectOptions {
    /**
     * Disable lint for all modules
     */
    boolean disableLint = false
    /**
     * Disable pre dex for all modules
     */
    boolean disablePreDex = false
    /**
     * Artifactory credentials
     */
    String artifactoryUser = ""
    String artifactoryPassword = ""
    String artifactoryContextUrl = ""
    /**
     * Build tools version for all modules
     */
    String androidBuildToolVersion = "22.0.1"
    /**
     * Sdk version for all modules
     */
    int androidCompileSdkVersion = 22
}

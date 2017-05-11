package fr.coppernic.gradle.android.extensions;

/**
 * Option for a module
 */
public class ModuleOptions {
    /**
     * True to configure all options
     */
    boolean configModule = true
    /**
     * True to configure build tools
     */
    boolean configBuildTools = true
    /**
     * True to configure sdk version
     */
    boolean configSdkVersion = true
    /**
     * True to configure source compatibilities
     */
    boolean configSourceCompat = true
    /**
     * True to configure version
     */
    boolean configVersion = true
    /**
     * True to config lint
     */
    boolean configLint = true
    /**
     * True to keep system product flavors
     */
    boolean system = false
    /**
     * True to keep user product flavors
     */
    boolean user = true
    /**
     * List of regex to match targets that shall be compiled
     */
    List<String> includeTarget = []
    /**
     * List of regex to match targets that shall be excluded from compilation
     */
    List<String> excludeTarget = []
}

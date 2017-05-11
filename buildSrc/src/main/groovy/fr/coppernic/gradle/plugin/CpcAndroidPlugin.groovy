package fr.coppernic.gradle.plugin

import fr.coppernic.gradle.android.extensions.*
import fr.coppernic.gradle.android.tasks.FDroidTask
import fr.coppernic.gradle.git.GitUtils
import org.apache.commons.io.FilenameUtils
import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.gradle.api.*
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.logging.Logger
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.javadoc.Javadoc

import java.nio.file.Paths
import java.security.InvalidParameterException
import java.util.regex.Matcher
import java.util.regex.Pattern
/**
 *
 * @author bastien
 */
class CpcAndroidPlugin implements Plugin<Project> {

    private static final TAG = "CpcAndroidPlugin"
    private static final DEBUG = false
    private static final String ARTI_CTX_URL = "http://arti-01:8081/artifactory"
    private static final String ARTI_REPO_KEY = "libs-coppernic-local"

    Logger logger

    void log(String s) {
        if (DEBUG) {
            logger.quiet s
        } else {
            logger.debug s
        }
    }

    void apply(final Project project) {
        configureInternal(project)
        configureRepositories(project)
        configureExtensions(project)
        configurePlugins(project)
        configureSubProjects(project)
    }

    void configureInternal(Project project) {
        // Protections to print error msg only once
        project.ext.errSigningPrinted = false
        project.ext.errKeyNotFound = false

        logger = project.getLogger()
    }

    void configureRepositories(Project project) {
        project.allprojects {
            repositories {
                def androidHome = System.getenv("ANDROID_HOME")
                maven { url "$androidHome/extras/android/m2repository/" }
                maven { url 'http://arti-01:8081/artifactory/plugins-release' }
                maven { url 'http://arti-01:8081/artifactory/libs-release' }
            }
        }
    }

    static void configureExtensions(Project project) {
        // Signing extension
        def signingKeys = project.container(SigningKey)
        project.extensions.signingKeys = signingKeys

        // Target extension
        project.extensions.create("targetDevice", TargetExtension)

        // Option extension
        project.extensions.create("options", ProjectOptions)


        project.extensions.remotes = createRemoteContainer(project)
        project.extensions.proxies = createProxyContainer(project)

        project.subprojects { Project sub ->
            sub.extensions.create("moduleOptions", ModuleOptions)
        }
    }

    static void configurePlugins(Project project) {
        project.subprojects { sub ->
            sub.plugins.whenPluginAdded { plugin ->
                if ("com.android.build.gradle.AppPlugin".equals(plugin.class.name)) {
                    sub.android.dexOptions.preDexLibraries = !project.options.disablePreDex
                    configureAndroidApp(sub)
                } else if ("com.android.build.gradle.LibraryPlugin".equals(plugin.class.name)) {
                    sub.android.dexOptions.preDexLibraries = !project.options.disablePreDex
                }
            }

            sub.apply plugin: 'maven-publish'
        }
    }

    void configureSubProjects(Project project) {
        project.subprojects {
            afterEvaluate { Project sub ->
                if (sub.hasProperty('android')) {
                    enableOptions(sub)
                    // must be done before publish
                    configureAndroidProject(sub)
                    configureSigning(project, sub)

                    // must done after signing
                    if (sub.android.hasProperty("applicationVariants")) {
                        sub.android.applicationVariants.all { variant ->
                            addJavadocTasksForVariant(sub, variant)
                            configureVariantOutputName(sub, variant)
                        }
                    } else if (sub.android.hasProperty("libraryVariants")) {
                        sub.android.libraryVariants.all { variant ->
                            addJavadocTasksForVariant(sub, variant)
                            configureVariantOutputName(sub, variant)
                            //must be the last
                            configureLibPublishing(sub, variant)
                        }
                    }
                }
            }
        }
    }

    /* ******* Options ******* */

    void enableOptions(Project project) {
        if (project.rootProject.options.disableLint) {
            project.android.lintOptions {
                abortOnError false
            }
        }

    }

    void configureAndroidProject(Project project) {
        // Get default config if needed
        ModuleOptions options = project.hasProperty("moduleOptions") ? project.moduleOptions : new ModuleOptions()
        if (!options.configModule) {
            log "Do not configure module ${project.name}"
            return
        }
        configureBuildTools(project, options)
        configureSdkVersion(project, options)
        configureSourceCompat(project, options)
        configureVersion(project, options)
        configureLint(project, options)
    }

    void configureBuildTools(Project project, ModuleOptions options) {
        if (!options.configBuildTools) {
            log "Do not set default build tools for module ${project.name}"
            return
        }
        project.android.buildToolsVersion project.options.androidBuildToolVersion
    }

    void configureSdkVersion(Project project, ModuleOptions options) {
        if (!options.configSdkVersion) {
            log "Do not set default sdk version for module ${project.name}"
            return
        }
        project.android.compileSdkVersion project.options.androidCompileSdkVersion
    }

    void configureSourceCompat(Project project, ModuleOptions options) {
        if (!options.configSourceCompat) {
            log "Do not set default source compat for module ${project.name}"
            return
        }

        // Ensure java version compatibility
        def javaVersion = JavaVersion.VERSION_1_7
        project.sourceCompatibility = javaVersion
        project.targetCompatibility = javaVersion // defaults to sourceCompatibility
    }

    void configureVersion(Project project, ModuleOptions options) {
        if (!options.configVersion) {
            log "Do not generate version for module ${project.name}"
            return
        }
        // Generate version name and version code
        println "Generate version name and version code"
        try {
            GitUtils git = new GitUtils(Paths.get(project.rootProject.projectDir.toString()))
            project.ext.versionCode = git.versionCode
            project.ext.versionName = git.versionName

            // Rewrite version info from git info
            project.android.defaultConfig {
                versionCode git.versionCode
                versionName git.versionName
            }
        } catch (RepositoryNotFoundException ignore) {
            println "$ignore"
            project.ext.versionCode = 1
            project.ext.versionName = "dev"
        }
    }

    void configureLint(Project project, ModuleOptions options){
        if(options.configLint) {
            project.android.lintOptions {
                warning 'GradleCompatible'
                warning 'MissingTranslation'
                warning 'InvalidPackage'
            }
        }
    }

    /* ******* Javadoc ******* */

    Task createJavadocTask(Project project, variant, String name) {
        Task t = project.task("generate${name.capitalize()}Javadoc", type: Javadoc) {
            description = "Generates Javadoc for $variant.name."
            group = "Documentation"
            source = variant.javaCompile.source
            project.ext.androidJar = "${project.android.sdkDirectory}/platforms/${project.android.compileSdkVersion}/android.jar"
            classpath = project.files(variant.javaCompile.classpath.files) + project.files(project.ext.androidJar)
            options.links("http://docs.oracle.com/javase/7/docs/api/");
            options.linksOffline("http://d.android.com/reference", "${project.android.sdkDirectory}/docs/reference");
            if (JavaVersion.current().isJava8Compatible()) {
                options.addStringOption('Xdoclint:none', '-quiet')
            }
            exclude '**/BuildConfig.java'
            exclude '**/R.java'
        }
        return t

    }

    Task createJavadocTask(Project project, variant) {
        createJavadocTask(project, variant, variant.name)
    }

    Task createJavadocArchiveTask(Project project, variant, String name) {
        Task t = project.task("generate${name.capitalize()}JavadocJar", type: Jar) {
            description = "Compress Javadoc for $variant.name."
            group = "Documentation"
            from project.buildDir.getPath() + "/docs/javadoc"
            into "${project.name}-javadoc"
            baseName = "${project.name}-javadoc"
            extension = "jar"
            manifest = null
        }
        return t
    }

    Task createJavadocArchiveTask(Project project, variant) {
        createJavadocArchiveTask(project, variant, variant.name)
    }

    void addJavadocTasksForVariant(Project project, variant) {
        if (!project.tasks.findByPath("generateReleaseJavadoc")) {
            if (variant.buildType.name == "release") {
                def t1 = createJavadocTask(project, variant, "release")
                def t2 = createJavadocArchiveTask(project, variant, "release")
                t2.dependsOn(t1)
            }
        }
    }

    /* ******* Publishing ******* */

    void configureLibPublishing(Project project, variant) {
        if (variant.name == 'release') {
            project.ext.appId = variant.applicationId

            configureMavenPublish(project, variant.outputs)
            configureArtifactoryPublish(project)
        }
    }

    void configureMavenPublish(Project project, outputs) {
        logger.info "configure maven publish for ${project.name}"

        project.publishing.publications {
            aar(MavenPublication) {
                // Artifact information
                groupId project.appId
                version = project.versionName
                artifactId project.getName()

                logger.info "$groupId:$artifactId:$version"

                // Set artifact
                outputs.each {
                    logger.info "Artifact : ${it.outputFile}"
                    artifact(it.outputFile)
                }

                // Customize pom with dependencies
                pom.withXml {
                    def dependenciesNode = asNode().appendNode('dependencies')

                    project.configurations.compile.allDependencies.each {
                        if (it instanceof ExternalModuleDependency
                                && it.group != null
                                && it.name != null) {

                            def dependencyNode = dependenciesNode.appendNode('dependency')
                            dependencyNode.appendNode('groupId', it.group)
                            dependencyNode.appendNode('artifactId', it.name)
                            dependencyNode.appendNode('version', it.version)

                            logger.info "dependency : ${it}\nartifact : ${it.getArtifacts()}"

                            //TODO remove this limitation of publishing only aar archive
                            if (it.group.startsWith('fr.coppernic')) {
                                dependencyNode.appendNode('type', 'aar')
                            }

                            //If there are any exclusions in dependency
                            if (it.excludeRules.size() > 0) {
                                def exclusionsNode = dependencyNode.appendNode('exclusions')
                                it.excludeRules.each { rule ->
                                    def exclusionNode = exclusionsNode.appendNode('exclusion')
                                    exclusionNode.appendNode('groupId', rule.group)
                                    exclusionNode.appendNode('artifactId', rule.module)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    static Map<String, String> getCredentials(Project project) {
        Map<String, String> cred = [artUser: "", artPassword: ""]

        if (project.properties.containsKey("artifactory_user")) {
            cred["artUser"] = project.artifactory_user.toString()
        }
        if (project.properties.containsKey("artifactory_password")) {
            cred["artPassword"] = project.artifactory_password.toString()
        }

        if (project.options.artifactoryUser) {
            cred["artUser"] = project.options.artifactoryUser.toString()
        }
        if (project.options.artifactoryPassword) {
            cred["artPassword"] = project.options.artifactoryPassword.toString()
        }

        if (!cred["artUser"] || !cred["artPassword"]) {
            println "No credentials set"
        }
        return cred
    }

    void configureArtifactoryPublish(Project project) {
        //log "Configure artifactoryPublish for ${project.name}"

        Map<String, String> cred = getCredentials(project)
        if (cred["artUser"] && cred["artPassword"]) {
            project.apply plugin: 'com.jfrog.artifactory'
            project.artifactory {
                contextUrl = ARTI_CTX_URL
                //The base Artifactory URL if not overridden by the publisher/resolver
                publish {
                    repository {
                        repoKey = ARTI_REPO_KEY
                        username = cred["artUser"]
                        password = cred["artPassword"]
                        maven = true
                    }
                    defaults {
                        publications('aar')
                        publishArtifacts = true
                        publishPom = true
                        publishBuildInfo = false
                    }
                }
            }
            project.artifactoryPublish.dependsOn(['build'])
            customizeArtifactoryPublish(project)
        } else {
            println "No credential set, task artifactoryPublish not configured"
        }
    }

    static void customizeArtifactoryPublish(final Project project) {

        String r = project.versionName
        String n = project.getName()
        String appId = project.appId.replaceAll(/[.]/, "/")
        String s = "$ARTI_CTX_URL/$ARTI_REPO_KEY/$appId/${n}/${r}/${n}-${r}.aar"

        Pattern rev = ~/(\d+)\.(\d+)\.(\d+)/
        Matcher m = rev.matcher(r)
        if (m != null && m.find()) {
            project.artifactoryPublish.onlyIf {
                boolean ret
                URL url = new URL(s)
                try {
                    ret = url.withReader { reader ->
                        try {
                            //noinspection GroovyUnusedAssignment
                            char c = reader.read()
                            project.getLogger().info "$s already exists in artifactory"
                            return false //do not execute this task
                        } catch (IOException ignore) {
                        }
                        return true //execute this task
                    }
                } catch (FileNotFoundException ignore) {
                    ret = true // Execute this task
                }
                project.getLogger().info "Publish to artifactory : " + ret
                return ret
            }
        }
    }

    /* ******* Signing ******* */

    void configureSigning(Project root, Project sub) {
        // Protection against empty projects (Folders like Sdk)
        if (!sub.hasProperty('android')) {
            logger.debug "${sub.name} is not an android project"
            return
        } else if (!sub.android.hasProperty('applicationVariants')) {
            logger.debug "${sub.name} is not an android application project"
            return
        }

        configureDefaultSigning(root, sub)
        configureSigningAndFlavor(root, sub)
        checkSigningConfig(root, sub)
    }

    void configureDefaultSigning(Project root, Project sub) {
        // Default config
        if (root.signingKeys.size()) {
            // Deactivate debug signing to get the good signing config for debug
            sub.android.buildTypes.debug.signingConfig null
        } else {
            // Default place holder when there are no keys
            sub.android.defaultConfig {
                manifestPlaceholders = [sharedUserId: "${sub.android.defaultConfig.applicationId}"]
            }
        }
    }

    void configureSigningAndFlavor(Project root, Project sub) {
        // Create signing key and corresponding flavor
        root.signingKeys.each { SigningKey sigKey ->
            // Filter signing keys
            if (excludeSigKey(root, sub, sigKey)) {
                return
            }
            addProductFlavor(sub, sigKey)
        }

        // Special case where no product flavors are specified, add keys that are not system
        if (sub.android.productFlavors.isEmpty()) {
            log "No signing key added, force adding non system keys."
            root.signingKeys.each { SigningKey sigKey ->
                if (!sigKey.system) {
                    addProductFlavor(sub, sigKey)
                }
            }
        }
    }

    void addProductFlavor(Project sub, SigningKey sigKey) {

        //println "${sub.name} -> ${sigKey.name}"

        sub.android.signingConfigs.create sigKey.name, {
            storeFile sigKey.storeFile
            storePassword sigKey.storePassword
            keyAlias sigKey.keyAlias
            keyPassword sigKey.keyPassword
        }

        // Configure product flavor
        sub.android.flavorDimensions "product"

        sub.android.productFlavors.create sigKey.name, {
            dimension "product"
            signingConfig sub.android.signingConfigs.getByName(sigKey.name)
            //println "sharedUserId:\"$defaultConfig.applicationId\""
            if (sigKey.system) {
                manifestPlaceholders = [sharedUserId: "android.uid.system"]
                applicationIdSuffix ".${sigKey.name}"
            } else {
                manifestPlaceholders = [sharedUserId: "${sub.android.defaultConfig.applicationId}"]
            }
        }
    }

    boolean excludeSigKey(Project root, Project sub, sigKey) {
        boolean ret = false
        def target = root.targetDevice?.name
        // Get default values if options are not set
        ModuleOptions options = sub.hasProperty("moduleOptions") ? sub.moduleOptions : new ModuleOptions()

        if (target && target != "all" && target != sigKey.name) {
            //skip config
            ret = true
        } else if (!options.system && sigKey.system) {
            logger.debug "skip ${sigKey.name} flavor because ${sub.name} is not system"
            ret = true
        } else if (!options.user && !sigKey.system) {
            log "skip ${sigKey.name} flavor because ${sub.name} is not user"
            ret = true
        } else if (!sigKey.storeFile.exists()) {
            logger.quiet "skip ${sigKey.name} flavor for ${sub.name} because " +
                    "${sigKey.storeFile} does not exist"
            ret = true
        } else {
            for (String s in options.excludeTarget) {
                if (sigKey.name ==~ /$s/) {
                    log "skip ${sigKey.name} flavor because it matches with ${s} filter " +
                            "for ${sub.name} project"
                    ret = true
                    break;
                }
            }
            if (!ret) {
                for (String s in options.includeTarget) {
                    if (!(sigKey.name ==~ /$s/)) {
                        log "skip ${sigKey.name} flavor because it doesn't match with " +
                                "${s} filter for ${sub.name} project"
                        ret = true
                        break;
                    }
                }
            }
        }
        return ret
    }

    static void checkSigningConfig(Project root, Project sub) {
        def target = root.targetDevice?.name
        // Protection against wrong target
        if (!sub.android.productFlavors.size()) {
            if (target) {
                throw new InvalidParameterException("Target '${target}' is invalid\nTarget name " +
                        "shall correspond to an existing signing config")
            }
        }
    }

    /* ******* Output Name ******* */

    void configureVariantOutputName(Project sub, variant) {
        variant.outputs.each { output ->
            String name = sub.name
            String appIdSuffix = variant.productFlavors[0]?.applicationIdSuffix
            String buildType = variant.variantData.variantConfiguration.buildType.name
            String version = sub.versionName
            String ext = FilenameUtils.getExtension("$output.outputFile")

            // Get appId suffix as flavor name -> standard does not have any special flavor name
            if (appIdSuffix) {
                //println appIdSuffix
                appIdSuffix = appIdSuffix.replace('.' as char, '-' as char)
            } else {
                appIdSuffix = ""
            }

            // Handle release
            if (buildType == "release") {
                buildType = ""
            } else {
                buildType = "-${buildType}"
            }

            String newName = "${name}${appIdSuffix}${buildType}-${version}.${ext}"

            logger.debug "${sub.name} output : $newName"

            output.outputFile = new File(output.outputFile.parent, newName)
        }
    }

    /* ******* FDroid ******* */

    static void configureAndroidApp(Project project) {
        project.task('fdroidPublish', type: FDroidTask/*, dependsOn: ['build']*/) {
            description = "Publish apk on fdroid server"
            group = "publishing"
            mustRunAfter 'build'
            mustRunAfter 'assemble'
        }
    }

    private static createRemoteContainer(Project project) {
        def remotes = project.container(RemoteOption)
        remotes.metaClass.mixin(RemoteContainerExtension)
        def parentRemotes = project.parent?.extensions?.findByName('remotes')
        if (parentRemotes instanceof NamedDomainObjectContainer<RemoteOption>) {
            remotes.addAll(parentRemotes)
        }
        remotes
    }

    private static createProxyContainer(Project project) {
        def proxies = project.container(Proxy)
        def parentProxies = project.parent?.extensions?.findByName('proxies')
        if (parentProxies instanceof NamedDomainObjectContainer<Proxy>) {
            proxies.addAll(parentProxies)
        }
        proxies
    }
}

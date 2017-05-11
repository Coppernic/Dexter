package fr.coppernic.gradle.android.fdroid

import fr.coppernic.gradle.android.extensions.SigningKey
import groovy.text.SimpleTemplateEngine
import org.gradle.api.Project

/**
 * Class holding metadata to be written on FDroid server
 *
 * Created on 30/11/16
 *
 * @author bastien
 */

public class Metadata {

    public final static String CATEGORIES = "Categories"
    public final static String LICENSE = "License"
    public final static String WEB_SITE = "Web Site"
    public final static String SOURCE_CODE = "Source Code"
    public final static String ISSUE_TRACKER = "Issue Tracker"
    public final static String SUMMARY = "Summary"
    public final static String DESCRIPTION = "Description"

    /**
     * Data to be written on FDroid server
     */
    final Map<String, String> data = ["Categories"   : "",
                                      "License"      : "Unknown",
                                      "Web Site"     : "",
                                      "Source Code"  : "",
                                      "Issue Tracker": "",
                                      "Summary"      : "",
                                      "Description"  : ""]

    /**
     * Table of correspondence between property keys and remote metadata keys
     */
    final static Map<String, String> propKeysToMetadataKeys = ["categories"  : CATEGORIES,
                                                               "license"     : LICENSE,
                                                               "webSite"     : WEB_SITE,
                                                               "sourceCode"  : SOURCE_CODE,
                                                               "issueTracker": ISSUE_TRACKER,
                                                               "summary"     : SUMMARY,
                                                               "description" : DESCRIPTION]

    /**
     * Load local property file.
     *
     * Properties key supported are listed in propKeysToMetadataKeys variable
     * @param f File
     */
    void loadFile(File f) {
        Properties props = new Properties()
        props.load(new FileInputStream(f))

        for (String key : props.stringPropertyNames()) {
            if (propKeysToMetadataKeys.containsKey(key)) {
                data.put(propKeysToMetadataKeys[key], props.getProperty(key).trim())
            } else {
                println "Key $key not supported"
            }
        }
    }

    /**
     * Get project data to be inserted in place of placeholders
     * @param project Gradle project
     * @param variant project build variant
     */
    void insertProjectData(Project project, variant) {
        Map<String, String> binding = [name: project.name]
        binding.putAll(getProductMapping(project, variant))

        SimpleTemplateEngine engine = new SimpleTemplateEngine()

        if (!data[SUMMARY]) {
            data[SUMMARY] = project.name
        } else {
            data[SUMMARY] = engine.createTemplate(data[SUMMARY]).make(binding).toString()
        }
        if (!data[DESCRIPTION]) {
            data[DESCRIPTION] = project.name
        } else {
            data[DESCRIPTION] = engine.createTemplate(data[DESCRIPTION]).make(binding).toString()
        }
        if (data[CATEGORIES]) {
            data[CATEGORIES] = engine.createTemplate(data[CATEGORIES]).make(binding).toString()
        }
    }

    void addCategory(String cat) {
        String previous = data[CATEGORIES]
        if (previous.isEmpty()) {
            data[CATEGORIES] = cat
        } else {
            data[CATEGORIES] = "$previous,$cat"
        }
    }

    /**
     * Get file content to be written on FDroid server
     *
     * @return String content
     */
    String getMetadataFileContent() {
        StringBuilder sb = new StringBuilder()
        data.each { String key, String value ->
            if (key == DESCRIPTION) {
                sb.append(key).append(':').append("\n")
                        .append(value).append("\n.\n")
            } else {
                sb.append(key).append(':').append(value).append("\n")
            }
        }
        sb.toString()
    }

    static Map<String, String> getProductMapping(Project project, variant) {
        Map<String, String> ret = [product: "", categories: ""]
        String productFlavor = variant.productFlavors[0]?.name
        if (productFlavor) {
            SigningKey key = project.rootProject.signingKeys.find {
                productFlavor == it.name
            }
            ret["product"] = key?.productName
            ret["categories"] = (key?.productCategories) ? key.productCategories : ret.product
        }
        return ret
    }

}

package fr.coppernic.gradle.android.extensions;

/**
 * @author bastien
 */
public class SigningKey {
    final String name
    File storeFile
    String storePassword = "android"
    String keyAlias = "AndroidDebugKey"
    String keyPassword = "android"
    boolean system = true
    String productName = ""
    String productCategories = ""


    SigningKey(String name) {
        this.name = name
    }
}

package fr.coppernic.gradle.plugin

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Created on 23/11/16
 * @author bastien
 */
class CpcAndroidPluginTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    private Project project
    private final def CpcAndroidPlugin buildPlugin = new CpcAndroidPlugin()

    @Before
    void setUp() {
        project = ProjectBuilder.builder()
                .withProjectDir(tempFolder.root)
                .build()
        buildPlugin.apply(project)
    }

    @Test
    void apply() {
        //buildPlugin.apply(project)

    }

}

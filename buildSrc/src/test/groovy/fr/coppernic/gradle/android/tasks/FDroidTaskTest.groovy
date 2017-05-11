package fr.coppernic.gradle.android.tasks

import fr.coppernic.gradle.android.fdroid.App
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FDroidTaskTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    Project project

    FDroidTask task

    String out1 = """
CpcTransparent-master.apk
CpcVoterDemo-1.1.0.apk
CpcZebraPassportDemo-1.0.1.apk
icons
"""

    @Before
    void setUp() {
        project = ProjectBuilder.builder()
                .withProjectDir(tempFolder.newFolder())
                .build()
        task = project.task('fdroidPublish', type: FDroidTask) {} as FDroidTask
    }

    @Test
    void checkParsing() {
        List<App> l = FDroidTask.getAppList(out1)
        List<String> list = []
        l.each {
            list.add(it.toString())
        }
        assert list == ["CpcTransparent-master.apk",
                        "CpcVoterDemo-1.1.0.apk",
                        "CpcZebraPassportDemo-1.0.1.apk"]
    }
}
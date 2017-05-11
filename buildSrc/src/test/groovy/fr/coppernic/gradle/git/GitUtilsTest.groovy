package fr.coppernic.gradle.git

import fr.coppernic.gradle.utils.ConditionalIgnoreRule
import org.junit.*
import org.junit.rules.TemporaryFolder

import java.nio.file.Path
import java.nio.file.Paths
/**
 */
class GitUtilsTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    @Rule
    public ConditionalIgnoreRule rule = new ConditionalIgnoreRule();


    Path cpcUtilsPath
    Path d2xxPath
    Path coolPath

    @Before
    void setUp() {
        cpcUtilsPath = Paths.get(tempFolder.root.absolutePath).resolve("CpcUtils")
        d2xxPath = Paths.get(tempFolder.root.absolutePath).resolve("D2xx")
        coolPath = Paths.get(tempFolder.root.absolutePath).resolve("Cool")
    }

    @After
    void tearDown() {
    }

    @Test
    @ConditionalIgnoreRule.ConditionalIgnore(condition = NotRunningOnJenkins.class)
    void getVersionNameCool() {
        assert 0 == executeOnShell("git clone git@gitlab-01.coppernic.local:test/RepoForUnitTest" +
                ".git " +
                "Cool", Paths.get(tempFolder.root.absolutePath))

        GitUtils utils = new GitUtils(coolPath)
        assert "master" == utils.getVersionName()

        executeOnShell("git checkout feature/cool", coolPath)
        assert "featureCool" == utils.getVersionName()
    }

    @Test
    @ConditionalIgnoreRule.ConditionalIgnore(condition = NotRunningOnJenkins.class)
    void getVersionName() {
        assert 0 == executeOnShell("git clone git@gitlab-01.coppernic.local:androidcpcsdk/cpcutilslib.git " +
                "CpcUtils", Paths.get(tempFolder.root.absolutePath))

        GitUtils utils = new GitUtils(cpcUtilsPath)
        assert "master" == utils.getVersionName()

        executeOnShell("git checkout dev", cpcUtilsPath)
        assert "dev" == utils.getVersionName()

        executeOnShell("git checkout 5.6.0", cpcUtilsPath)
        assert "5.6.0" == utils.getVersionName()

        executeOnShell("git checkout 5.8.0-RC4", cpcUtilsPath)
        assert "5.8.0-RC4" == utils.getVersionName()

        executeOnShell("git checkout 5.10.0", cpcUtilsPath)
        assert "5.10.0" == utils.getVersionName()

        executeOnShell("git checkout d1d1eb0cd2633cd00e947d81f309e5977560de16", cpcUtilsPath)
        assert "d1d1eb0cd2633cd00e947d81f309e5977560de16" == utils.getVersionName()
    }

    @Test
    @ConditionalIgnoreRule.ConditionalIgnore(condition = NotRunningOnJenkins.class)
    void getVersionNameD2xx() {
        assert 0 == executeOnShell("git clone git@gitlab-01.coppernic.local:external/d2xx.git " +
                "D2xx", Paths.get(tempFolder.root.absolutePath))

        GitUtils utils = new GitUtils(d2xxPath)
        executeOnShell("git checkout 2.4.3-RC3", d2xxPath)
        assert "2.4.3-RC3" == utils.getVersionName()
    }


    @Ignore
    @Test
    void getVersionNameD2xxProf() {
        assert 0 == executeOnShell("git clone git@gitlab-01.coppernic.local:external/d2xx.git " +
                "D2xx", Paths.get(tempFolder.root.absolutePath))

        GitUtils utils = new GitUtils(d2xxPath)
        String res

        executeOnShell("git checkout 2.4.3-RC3", d2xxPath)
        profile {
            res = utils.getVersionName()
        }.prettyPrint()
        assert "2.4.3-RC3" == res


        executeOnShell("git checkout 2.4.3-RC3", d2xxPath)
        profile {
            res = utils.getVersionNameSlow()
        }.prettyPrint()
        assert "2.4.3-RC3" == res
    }

    @Test
    @ConditionalIgnoreRule.ConditionalIgnore(condition = NotRunningOnJenkins.class)
    void getVersionCode() {
        assert 0 == executeOnShell("git clone git@gitlab-01.coppernic.local:androidcpcsdk/cpcutilslib.git " +
                "CpcUtils", Paths.get(tempFolder.root.absolutePath))

        GitUtils utils = new GitUtils(cpcUtilsPath)
        assert 1 == utils.getVersionCode()

        executeOnShell("git checkout dev", cpcUtilsPath)
        assert 1 == utils.getVersionCode()

        executeOnShell("git checkout 5.6.0", cpcUtilsPath)
        assert 50600 == utils.getVersionCode()

        executeOnShell("git checkout 5.8.0-RC4", cpcUtilsPath)
        assert 50800 == utils.getVersionCode()

        executeOnShell("git checkout d1d1eb0cd2633cd00e947d81f309e5977560de16", cpcUtilsPath)
        assert 1 == utils.getVersionCode()

    }

    private static int executeOnShell(String command, Path workingDir) {
        //println command
        def process = new ProcessBuilder(addShellPrefix(command))
                .directory(workingDir.toFile())
                .redirectErrorStream(true)
                .start()
        process.waitFor();
        def exit = process.exitValue()
        //if (!exit){
            process.inputStream.eachLine { println it }
        //}
        return exit
    }

    private static String[] addShellPrefix(String command) {
        def commandArray = new String[3]
        commandArray[0] = "sh"
        commandArray[1] = "-c"
        commandArray[2] = command
        return commandArray
    }

    public class NotRunningOnJenkins implements ConditionalIgnoreRule.IgnoreCondition {
        public boolean isSatisfied() {
            return !(System.getProperty("user.name") == "jenkins");
        }
    }

}

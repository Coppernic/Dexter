package fr.coppernic.gradle.android.fdroid

import fr.coppernic.gradle.android.tasks.FDroidTask
import org.junit.Ignore
import org.junit.Test

import java.util.regex.Matcher

/**
 * @author Bastien Paul
 */
class AppTest {

    @Test
    void compareString() {
        assert 0 == App.compareString("1.0.1", "1.0.1")
        assert 0 == App.compareString("-1.0.1", "1.0.1")
        assert 0 == App.compareString("1.0.1", "-1.0.1")
        assert 0 == App.compareString("-1.0.1", "-1.0.1")
        assert -1 == App.compareString("1.0.1", "1.0.2")
        assert -1 == App.compareString("-1.0.1", "1.0.2")
        assert -1 == App.compareString("1.0.1", "-1.0.2")
        assert -1 == App.compareString("-1.0.1", "-1.0.2")
        assert 2 == App.compareString("1.2.1", "1.0.2")
        assert 2 == App.compareString("-1.2.1", "1.0.2")
        assert 2 == App.compareString("1.2.1", "-1.0.2")
        assert 2 == App.compareString("-1.2.1", "-1.0.2")
        assert -3 == App.compareString("", "-RC1")
        assert 3 == App.compareString("RC1", "")
    }

    @Test
    void checkPattern() {
        Matcher m = FDroidTask.sPatternApk.matcher("CpcHidHfDemo-1.0.0.apk")
        assert m != null
        assert m.find()

        int i = 0
        m = App.sPatternAppName.matcher("CpcHidHfDemo-1.0.0.apk")
        assert m != null
        assert m.find()
        assert m.group(++i) == "CpcHidHfDemo"
        assert m.group(++i) == null
        assert m.group(++i) == "-1.0.0"
        assert m.group(++i) == null
        assert m.group(++i) == "apk"

        i = 0
        m = App.sPatternAppName.matcher("CpcHidIClassProxSample-1.0.0-RC1.apk")
        assert m != null
        assert m.find()
        assert m.group(++i) == "CpcHidIClassProxSample"
        assert m.group(++i) == null
        assert m.group(++i) == "-1.0.0"
        assert m.group(++i) == "-RC1"
        assert m.group(++i) == "apk"

        i = 0
        m = App.sPatternAppName.matcher("CpcTransparent-master.apk")
        assert m != null
        assert m.find()
        assert m.group(++i) == "CpcTransparent"
        assert m.group(++i) == "-master"
        assert m.group(++i) == null
        assert m.group(++i) == null
        assert m.group(++i) == "apk"

        i = 0
        m = App.sPatternAppName.matcher("CpcAgridentDemo-CpcAgrident-1.0.1-EB1.apk")
        assert m != null
        assert m.find()
        assert m.group(++i) == "CpcAgridentDemo"
        assert m.group(++i) == "-CpcAgrident"
        assert m.group(++i) == "-1.0.1"
        assert m.group(++i) == "-EB1"
        assert m.group(++i) == "apk"

        i = 0
        m = App.sPatternAppName.matcher("cpcftdidetails-debug.apk")
        assert m != null
        assert m.find()
        assert m.group(++i) == "cpcftdidetails"
        assert m.group(++i) == "-debug"
        assert m.group(++i) == null
        assert m.group(++i) == null
        assert m.group(++i) == "apk"

        i = 0
        m = App.sPatternAppName.matcher("CpcOcrDemo.apk")
        assert m != null
        assert m.find()
        assert m.group(++i) == "CpcOcrDemo"
        assert m.group(++i) == null
        assert m.group(++i) == null
        assert m.group(++i) == null
        assert m.group(++i) == "apk"

        i = 0
        m = App.sPatternAppName.matcher("CpcOcrDemo-cone-1.0.0.apk")
        assert m != null
        assert m.find()
        assert m.group(++i) == "CpcOcrDemo"
        assert m.group(++i) == "-cone"
        assert m.group(++i) == "-1.0.0"
        assert m.group(++i) == null
        assert m.group(++i) == "apk"
    }

    @Ignore
    @Test
    void checkPatternVersion() {
        int i = 0
        Matcher m = App.sPatternVersion.matcher("1.0.0")
        assert m != null
        assert m.find()
        println m.group(++i)
        assert m.group(i) == ""
        println m.group(++i)
        assert m.group(i) == "1.0.0"
        println m.group(++i)
        assert m.group(i) == ""

    }

    @Test
    void shouldUpdate() {
        //App local = new App("CpcHidHfDemo-1.0.0.apk")
        App local = App.fromLsLine("CpcHidHfDemo-1.0.0.apk")
        assert local.versionPrefix == ""
        assert local.versionSuffix == ""
        assert local.versionBase == "-1.0.0"

        App remote = App.fromLsLine("CpcHidHfDemo-1.0.0.apk")
        assert !local.shouldUpdateInternal(remote)

        remote = App.fromLsLine("CpcHidHfDemo-dev-1.0.0.apk")
        assert !local.shouldUpdateInternal(remote)

        remote = App.fromLsLine("CpcHidHfDemo-1.0.0-RC1.apk")
        assert !local.shouldUpdateInternal(remote)

        remote = App.fromLsLine("CpcHidHfDemo-2.0.0.apk")
        assert !local.shouldUpdateInternal(remote)

        remote = App.fromLsLine("CpcHidHfDemo-0.1.0.apk")
        assert local.shouldUpdateInternal(remote)

        // ---

        remote = App.fromLsLine("CpcHidHfDemo-1.0.0.apk")
        assert !local.shouldUpdate(remote)

        remote = App.fromLsLine("CpcHidHfDemo-dev-1.0.0.apk")
        assert !local.shouldUpdate(remote)

        remote = App.fromLsLine("CpcHidHfDemo-1.0.0-RC1.apk")
        assert !local.shouldUpdate(remote)

        remote = App.fromLsLine("CpcHidHfDemo-2.0.0.apk")
        assert !local.shouldUpdate(remote)

        remote = App.fromLsLine("CpcHidHfDemo-0.1.0.apk")
        assert local.shouldUpdate(remote)
    }

    @Test
    void aaptLine() {
        def list = ['cone', 'conedebug', 'intrabet', 'cizi', 'cfive']
        App app = App.fromAaptLine("CpcHidHfDemo-1.0.0.apk",
                "package: name='fr.coppernic.service.conedebug' versionCode='1' " +
                        "versionName='master' platformBuildVersionName='5.1.1-1819727'", list)
        assert app.name == "CpcHidHfDemo"
        assert app.appId == "fr.coppernic.service.conedebug"
        assert app.versionPrefix == "-master"
        assert app.versionBase == ""
        assert app.versionSuffix == ""
        assert app.productName == ""

        app = App.fromAaptLine("CpcHidHfDemo-1.0.0.apk",
                "package: name='fr.coppernic.service.conedebug' versionCode='1' " +
                        "versionName='CpcAgrident-1.0.1-EB1' platformBuildVersionName='5.1.1-1819727'", list)

        assert app.name == "CpcHidHfDemo"
        assert app.appId == "fr.coppernic.service.conedebug"
        assert app.versionPrefix == "-CpcAgrident"
        assert app.versionBase == "-1.0.1"
        assert app.versionSuffix == "-EB1"
        assert app.productName == ""

        app = App.fromAaptLine("CpcHidHfDemo-cone-1.0.0.apk",
                "package: name='fr.coppernic.service.conedebug' versionCode='1' " +
                        "versionName='CpcAgrident-1.0.1-EB1' platformBuildVersionName='5.1.1-1819727'", list)

        assert app.name == "CpcHidHfDemo"
        assert app.appId == "fr.coppernic.service.conedebug"
        assert app.versionPrefix == "-CpcAgrident"
        assert app.versionBase == "-1.0.1"
        assert app.versionSuffix == "-EB1"
        assert app.productName == "cone"

    }

    @Test
    void getProductFromPrefix() {
        def list = ['cone', 'conedebug', 'intrabet', 'cizi', 'cfive']
        assert App.getProductNameFromPrefix('-test', list) == ""
        assert App.getProductNameFromPrefix('-cone', list) == "cone"
        assert App.getProductNameFromPrefix('-conedebug', list) == "conedebug"
        assert App.getProductNameFromPrefix('-cone-cizi', list) == "cone"
        assert App.getProductNameFromPrefix('-cone-bidule', list) == "cone"
        assert App.getProductNameFromPrefix('cone', list) == "cone"
        assert App.getProductNameFromPrefix('cone-intrabet', list) == "cone"
    }

}

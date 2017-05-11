package fr.coppernic.gradle.android.fdroid;

import org.junit.Test;

/**
 * Created on 30/11/16
 *
 * @author bastien
 */
public class MetadataTest {

    static final File prop = new File("./src/test/resources/properties/fdroid.properties")

    @Test
    void load() {
        Metadata data = new Metadata()
        data.loadFile(prop)
        assert data.data["Categories"] == "cat"
        assert data.data["License"] == "lic"
        assert data.data["Web Site"] == "web"
        assert data.data["Source Code"] == "src"
        assert data.data["Issue Tracker"] == "issue"
        assert data.data["Summary"] == "sum up"
        assert data.data["Description"] == "this is a description"
    }

    @Test
    void addCat() {
        Metadata data = new Metadata()
        assert data.data["Categories"] == ""
        data.addCategory("C-One Ask")
        assert data.data["Categories"] == "C-One Ask"
        data.loadFile(prop)
        data.addCategory("C-One Ask")
        data.addCategory("C-five")
        assert data.data["Categories"] == "cat,C-One Ask,C-five"
    }

    @Test
    void getFileContent() {
        Metadata data = new Metadata()
        data.loadFile(prop)

        String s = """Categories:cat
License:lic
Web Site:web
Source Code:src
Issue Tracker:issue
Summary:sum up
Description:
this is a description
.
"""
        assert data.getMetadataFileContent() == s
    }
}
package fr.coppernic.gradle.android.extensions

import org.hidetake.groovy.ssh.core.Remote

/**
 * Created by bastien on 02/11/16.
 */

public class RemoteOption extends Remote {
    //String dirPath = ""
    String fdroidPath = ""

    RemoteOption(String name1) {
        super(name1)
    }

    RemoteOption(Map<String, Object> settingsMap) {
        super(settingsMap)
    }
}

package simonlee.elegant.d3algo;

import de.infsec.tpl.LibraryIdentifier;
import de.infsec.tpl.profile.LibProfile;
import de.infsec.tpl.utils.Utils;
import simonlee.elegant.Resources;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class D3LibScout extends D3AbstractWhiteList {

    // for lazy loading
    private static boolean libProfilesAreLoaded = false;
    // TODO - Yes, hard code here, don't touch it, should be a parameter passed to ELEGANT
    // libProfiles directory path
    private static String libProfilesDirPath = Resources.LIB_PROFILES_DIR;
    // library libProfiles
    private static List<LibProfile> libProfiles = null;

    // apk path
    private String apkPath;
    // android jars dir path
    private List<String> androidJarsPathes;

    // detected libraries' package prefixes
    private List<String> detectedLibPrefixes = new D3None().getWhiteList();

    public D3LibScout(String apkPath, List<String> androidJarsPathes) {
        this.apkPath = apkPath;
        this.androidJarsPathes = androidJarsPathes;
        this.detect();
    }

    @Override
    protected List<String> getWhiteList() {
        return detectedLibPrefixes;
    }

    // detect will do actual 3rd party libraries detection in a lazy way
    private void detect() {
        try {
            File apk = new File(apkPath);
            if (!apk.exists() || apk.isDirectory()) {
                throw new RuntimeException(apkPath + " is a directory or even not exists");
            }

            // lazily load library profiles
            if (!D3LibScout.libProfilesAreLoaded) {
                D3LibScout.loadLibraryProfiles();
                D3LibScout.libProfilesAreLoaded = true;
            }

            LibraryIdentifier identifier = new LibraryIdentifier(apk, this.androidJarsPathes);
            detectedLibPrefixes.addAll(identifier.identifyLibraries(D3LibScout.libProfiles));
        } catch (Exception e) {
            // do nothing
        }
    }

    // loadLibraryProfiles will load profiles into the system, load only once, so synchronized
    private static synchronized void loadLibraryProfiles() {
        D3LibScout.libProfiles = new ArrayList<>();

        File libProfilesDir = new File(D3LibScout.libProfilesDirPath);
        if (!libProfilesDir.exists() || !libProfilesDir.isDirectory()) {
            throw new RuntimeException(D3LibScout.libProfilesDirPath + " is a directory or even not exists");
        }

        try {
            // de-serialize library profiles, hard-code here, the extensions "lib" is embedded in LibScout
            for (File f : Utils.collectFiles(libProfilesDir, new String[]{ "lib" })) {
                LibProfile lp = (LibProfile) Utils.disk2Object(f);
                D3LibScout.libProfiles.add(lp);
            }

            Collections.sort(D3LibScout.libProfiles, LibProfile.comp);
        } catch (ClassNotFoundException e) {
            System.exit(1);
        }
    }
}

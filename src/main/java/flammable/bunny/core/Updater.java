package flammable.bunny.core;

import flammable.bunny.ui.UIUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Updater {
    public static final String CURRENT_VERSION = "1.0.0";

    private static int compareVersions(String v1) {
        String[] a1 = v1.replaceFirst("^v", "").split("\\.");
        String[] a2 = CURRENT_VERSION.replaceFirst("^v", "").split("\\.");
        int len = Math.max(a1.length, a2.length);
        for (int i = 0; i < len; i++) {
            int n1 = i < a1.length ? Integer.parseInt(a1[i]) : 0;
            int n2 = i < a2.length ? Integer.parseInt(a2[i]) : 0;
            if (n1 != n2) return Integer.compare(n1, n2);
        }
        return 0;
    }

    public static void checkForUpdates() {
        try {
            URL url = new URL("https://api.github.com/repos/Flammable-Bunny/Lingle/releases");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            String json;
            try (InputStream in = conn.getInputStream()) {
                json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            
            Pattern releasePattern = Pattern.compile("\\{[^}]*\"tag_name\"\\s*:\\s*\"([^\"]+)\"[^}]*\"browser_download_url\"\\s*:\\s*\"([^\"]*\\.jar)\"[^}]*}");
            Matcher releaseMatcher = releasePattern.matcher(json);

            if (!releaseMatcher.find()) return;

            String latest = releaseMatcher.group(1).trim();
            String downloadUrl = releaseMatcher.group(2);

            if (compareVersions(latest) <= 0) return;

            boolean choice = UIUtils.showDarkConfirm(
                    null,
                    "Lingle Update",
                    "A new version (" + latest + ") is available.\nUpdate now?"
            );
            if (choice) {
                downloadAndReplaceJar(downloadUrl);
            }
        } catch (Exception ignored) {}
    }

    private static void downloadAndReplaceJar(String downloadUrl) throws IOException {
        try {
            Path tmp = Files.createTempFile("lingle-update", ".jar");
            try (InputStream in = new URL(downloadUrl).openStream()) {
                Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            }

            String jarPath = new File(Updater.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).getAbsolutePath();

            Path updaterScript = Files.createTempFile("lingle-updater", ".sh");
            String scriptContent = String.format("""
                    #!/bin/bash
                    # Wait for the app to fully exit
                    sleep 3

                    # Replace the old jar with the new one
                    rm -f "%s"
                    mv "%s" "%s"

                    # Relaunch the app
                    java -jar "%s" &

                    # Clean up this script
                    rm -f "$0"
                    """, jarPath, tmp.toString(), jarPath, jarPath);

            Files.writeString(updaterScript, scriptContent, StandardCharsets.UTF_8);
            updaterScript.toFile().setExecutable(true);

            new ProcessBuilder("/bin/bash", updaterScript.toString()).start();
            System.exit(0);
        } catch (Exception e) {
            throw new IOException("Update failed", e);
        }
    }
}

package flammable.bunny.core;

import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Updater {
    public static final String CURRENT_VERSION = "0.5.6";

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
            URL url = new URL("https://api.github.com/repos/Flammable-Bunny/Lingle/releases/latest");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            String json;
            try (InputStream in = conn.getInputStream()) {
                json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }

            Matcher m = Pattern.compile("\"tag_name\"\\s*:\\s*\"(.*?)\"").matcher(json);
            if (!m.find()) return;
            String latest = m.group(1).trim();
            if (compareVersions(latest) <= 0) return;

            Matcher dl = Pattern.compile("\"browser_download_url\"\\s*:\\s*\"(.*?\\.jar)\"").matcher(json);
            if (!dl.find()) return;
            String downloadUrl = dl.group(1);

            int choice = JOptionPane.showConfirmDialog(
                    null,
                    "A new version (" + latest + ") is available.\nUpdate now?",
                    "Lingle Update",
                    JOptionPane.YES_NO_OPTION
            );
            if (choice == JOptionPane.YES_OPTION) {
                downloadAndReplaceJar(downloadUrl);
            }
        } catch (Exception ignored) {}
    }

    private static void downloadAndReplaceJar(String downloadUrl) throws IOException {
        Path tmp = Files.createTempFile("lingle-update", ".jar");
        try (InputStream in = new URL(downloadUrl).openStream()) {
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
        }

        String jarPath = new File(Updater.class.getProtectionDomain()
                .getCodeSource().getLocation().getPath()).getAbsolutePath();

        Path updaterScript = Files.createTempFile("lingle-updater", ".sh");
        String scriptContent = """
                #!/bin/bash
                oldjar="$1"
                newjar="$2"
                sleep 2
                rm -f "$oldjar"
                mv "$newjar" "$oldjar"
                exec java -jar "$oldjar" &
                """;
        Files.writeString(updaterScript, scriptContent, StandardCharsets.UTF_8);
        updaterScript.toFile().setExecutable(true);

        new ProcessBuilder(updaterScript.toString(), jarPath, tmp.toString()).start();
        System.exit(0);
    }
}

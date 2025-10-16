package flammable.bunny.core;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.List;

public class DiscordInstaller {

    private static final String APP_ASAR_URL = "https://github.com/GooseMod/OpenAsar/releases/latest/download/app.asar";

    private static final List<String> POSSIBLE_LOCATIONS = List.of(
        "/opt/discord/resources/app.asar",
        "/usr/lib/discord/resources/app.asar",
        "/usr/lib64/discord/resources/app.asar",
        "/usr/share/discord/resources/app.asar",
        "/var/lib/flatpak/app/com.discordapp.Discord/current/active/files/discord/resources/app.asar",
        System.getProperty("user.home") + "/.local/share/flatpak/app/com.discordapp.Discord/current/active/files/discord/resources/app.asar"
    );

    public static void installCustomAppAsar(String pkgManager) throws IOException, InterruptedException {
        // Download app.asar to temp location
        String tempDir = System.getProperty("java.io.tmpdir");
        Path tempAsar = Path.of(tempDir, "discord-app.asar");

        System.out.println("Downloading app.asar...");
        downloadFile(APP_ASAR_URL, tempAsar.toString());

        // Determine location based on package manager
        String targetLocation = null;
        if ("pacman".equals(pkgManager)) {
            targetLocation = "/opt/discord/resources/app.asar";
        } else if ("dnf".equals(pkgManager)) {
            targetLocation = "/usr/lib64/discord/resources/app.asar";
        } else {
            // For other distros, search for existing location because idk where they are on other distros
            for (String location : POSSIBLE_LOCATIONS) {
                if (Files.exists(Path.of(location))) {
                    targetLocation = location;
                    break;
                }
            }
        }

        if (targetLocation == null) {
            throw new IOException("Could not find Discord installation location");
        }

        // Backup existing app.asar
        Path target = Path.of(targetLocation);
        if (Files.exists(target)) {
            Files.copy(target, Path.of(targetLocation + ".backup"), StandardCopyOption.REPLACE_EXISTING);
        }

        // Replace with new app.asar
        ProcessBuilder pb = new ProcessBuilder("sudo", "cp", tempAsar.toString(), targetLocation);
        pb.inheritIO();
        int exitCode = pb.start().waitFor();
        if (exitCode != 0) {
            throw new IOException("Failed to replace app.asar with exit code: " + exitCode);
        }

        // Clean up temp file
        Files.deleteIfExists(tempAsar);

        System.out.println("Discord app.asar installed successfully at: " + targetLocation);
    }

    private static void downloadFile(String urlString, String destination) throws IOException {
        URL url = new URL(urlString);
        try (InputStream in = url.openStream();
             FileOutputStream out = new FileOutputStream(destination)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }
}

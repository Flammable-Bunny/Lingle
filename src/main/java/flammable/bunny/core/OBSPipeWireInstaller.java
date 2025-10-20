package flammable.bunny.core;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.util.Comparator;

public class OBSPipeWireInstaller {

    private static final String PLUGIN_URL = "https://github.com/dimtpap/obs-pipewire-audio-capture/releases/download/1.2.1/linux-pipewire-audio-1.2.1.tar.gz";
    private static final String OBS_CONFIG_DIR = System.getProperty("user.home") + "/.config/obs-studio";
    private static final String PLUGINS_DIR = OBS_CONFIG_DIR + "/plugins";

    public static void installPipeWireDependencies(String pkgManager) throws IOException, InterruptedException {
        System.out.println("Installing PipeWire dependencies...");

        if ("pacman".equals(pkgManager)) {
            runCommand("pacman -S --noconfirm wireplumber pipewire pipewire-pulse pipewire-alsa pipewire-jack");
        } else if ("dnf".equals(pkgManager)) {
            runCommand("dnf install -y wireplumber pipewire pipewire-pulseaudio pipewire-alsa pipewire-jack-audio-connection-kit");
        } else if ("apt".equals(pkgManager)) {
            runCommand("apt install -y wireplumber pipewire pipewire-pulse-session-manager pipewire-audio-client-libraries pipewire-jack");
        } else {
            throw new IOException("Unsupported package manager: " + pkgManager);
        }

        System.out.println("PipeWire dependencies installed successfully!");
    }

    public static void installOBSPipeWirePlugin() throws IOException, InterruptedException {
        System.out.println("Installing OBS PipeWire Audio Capture plugin...");

        // Ensure plugin directories exist
        Path pluginsDir = Path.of(PLUGINS_DIR);
        if (!Files.exists(pluginsDir)) {
            Files.createDirectories(pluginsDir);
            System.out.println("Created plugins directory: " + PLUGINS_DIR);
        }

        // Download plugin archive to temp location
        String tempDir = System.getProperty("java.io.tmpdir");
        Path tempArchive = Path.of(tempDir, "linux-pipewire-audio-1.2.1.tar.gz");
        Path tempExtractDir = Path.of(tempDir, "obs-pipewire-extract-" + System.nanoTime());
        Files.createDirectories(tempExtractDir);

        System.out.println("Downloading OBS PipeWire plugin...");
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(PLUGIN_URL))
                .GET()
                .build();

        HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(tempArchive));

        if (response.statusCode() != 200) {
            throw new IOException("Failed to download plugin: HTTP " + response.statusCode());
        }

        System.out.println("Extracting plugin archive...");
        ProcessBuilder pb = new ProcessBuilder("tar", "-xzf", tempArchive.toString(), "-C", tempExtractDir.toString());
        pb.inheritIO();
        int exitCode = pb.start().waitFor();

        if (exitCode != 0) {
            throw new IOException("Failed to extract plugin archive with exit code: " + exitCode);
        }

        Path candidate = tempExtractDir.resolve("linux-pipewire-audio");
        Path candidateUnderObs = tempExtractDir.resolve("obs-plugins").resolve("linux-pipewire-audio");
        Path sourceDir;
        if (Files.isDirectory(candidate)) {
            sourceDir = candidate;
        } else if (Files.isDirectory(candidateUnderObs)) {
            sourceDir = candidateUnderObs;
        } else {
            try (var stream = Files.walk(tempExtractDir)) {
                sourceDir = stream
                    .filter(Files::isDirectory)
                    .filter(p -> p.getFileName().toString().equals("linux-pipewire-audio"))
                    .findFirst()
                    .orElseThrow(() -> new IOException("Could not locate extracted linux-pipewire-audio directory"));
            }
        }

        Path targetDir = pluginsDir.resolve("linux-pipewire-audio");
        if (Files.exists(targetDir)) {
            deleteRecursively(targetDir);
        }
        copyRecursively(sourceDir, targetDir);

        Files.deleteIfExists(tempArchive);
        deleteRecursively(tempExtractDir);

        System.out.println("OBS PipeWire Audio Capture plugin installed successfully to " + targetDir);
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) return;
        try (var walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try { Files.deleteIfExists(p); } catch (IOException ignored) {}
            });
        }
    }

    private static void copyRecursively(Path source, Path target) throws IOException {
        Files.walk(source).forEach(p -> {
            try {
                Path rel = source.relativize(p);
                Path dest = target.resolve(rel);
                if (Files.isDirectory(p)) {
                    Files.createDirectories(dest);
                } else {
                    Files.createDirectories(dest.getParent());
                    Files.copy(p, dest, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    private static void runCommand(String command) throws IOException, InterruptedException {
        int exitCode = ElevatedInstaller.runElevatedBash(command);
        if (exitCode != 0) {
            throw new IOException("Command failed with exit code " + exitCode + ": " + command);
        }
    }
}

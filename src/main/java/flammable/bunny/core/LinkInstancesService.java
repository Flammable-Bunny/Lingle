package flammable.bunny.core;

import flammable.bunny.ui.UIUtils;
import javax.swing.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class LinkInstancesService {

    public static void symlinkInstances(List<String> instanceNames) throws IOException {
        Path home = Path.of(System.getProperty("user.home"));
        int idx = 1;
        for (String instance : instanceNames) {
            Path lingleDir = home.resolve("Lingle").resolve(String.valueOf(idx++));
            Files.createDirectories(lingleDir);

            Path savesPath = home.resolve(".local/share/PrismLauncher/instances")
                    .resolve(instance).resolve("minecraft/saves");

            if (Files.exists(savesPath)) {
                try (var stream = Files.walk(savesPath)) {
                    stream.sorted(Comparator.reverseOrder()).forEach(p -> {
                        try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                    });
                }
            }
            try { Files.createSymbolicLink(savesPath, lingleDir); }
            catch (FileAlreadyExistsException ignored) {}
        }
        LingleState.instanceCount = instanceNames.size();
        LingleState.saveState();
    }

    public static void linkPracticeMapsNow() throws IOException {
        if (LingleState.instanceCount <= 0 || LingleState.selectedPracticeMaps.isEmpty()) return;
        Path home = Path.of(System.getProperty("user.home"));
        Path savesDir = home.resolve(".local/share/lingle/saves");
        for (int k = 1; k <= LingleState.instanceCount; k++) {
            Path dstDir = home.resolve("Lingle").resolve(String.valueOf(k));
            Files.createDirectories(dstDir);
            for (String map : LingleState.selectedPracticeMaps) {
                Path target = savesDir.resolve(map);
                Path link = dstDir.resolve(map);
                if (Files.isSymbolicLink(link)) Files.deleteIfExists(link);
                try { Files.createSymbolicLink(link, target); }
                catch (FileAlreadyExistsException ignored) {}
            }
        }
    }

    public static void preparePracticeMapLinks() throws IOException {
        if (!LingleState.practiceMaps || !LingleState.enabled) return;
        Path home = Path.of(System.getProperty("user.home"));
        Path scriptsDir = home.resolve(".local/share/lingle/scripts");
        Files.createDirectories(scriptsDir);

        StringBuilder sb = new StringBuilder("#!/bin/bash\nset -e\n\n");
        sb.append("for k in {1..").append(LingleState.instanceCount).append("}\ndo\n")
                .append("  mkdir -p \"$HOME/Lingle/$k\"\n");
        for (String map : LingleState.selectedPracticeMaps) {
            sb.append("  ln -sf \"$HOME/.local/share/lingle/saves/Z_")
                    .append(map).append("\" \"$HOME/Lingle/$k/\"\n");
        }
        sb.append("done\n");

        Path linkScript = scriptsDir.resolve("link_practice_maps.sh");
        Files.writeString(linkScript, sb.toString(), StandardCharsets.UTF_8);
        linkScript.toFile().setExecutable(true);
        new ProcessBuilder("/bin/bash", linkScript.toString()).start();
    }

    public static void installCreateDirsService(JFrame parent) {
        try {
            preparePracticeMapLinks();

            Path home = Path.of(System.getProperty("user.home"));
            Path scriptsDir = home.resolve(".local/share/lingle/scripts");
            Path script = scriptsDir.resolve("link_practice_maps.sh");
            if (!Files.exists(script)) {
                UIUtils.showDarkMessage(parent, "Error", "Missing script: " + script);
                return;
            }

            try {
                Set<PosixFilePermission> perms = EnumSet.of(
                        PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE,
                        PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_EXECUTE,
                        PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_EXECUTE
                );
                Files.setPosixFilePermissions(script, perms);
            } catch (UnsupportedOperationException ignored) {}

            // Create user service instead of system service for better KDE compatibility
            String service = """
                    [Unit]
                    Description=Lingle Practice Map Linker
                    After=graphical-session.target

                    [Service]
                    Type=oneshot
                    ExecStart=%s
                    RemainAfterExit=yes

                    [Install]
                    WantedBy=default.target
                    """.formatted(script.toString());

            Path userServiceDir = home.resolve(".config/systemd/user");
            Files.createDirectories(userServiceDir);
            Path serviceFile = userServiceDir.resolve("lingle-startup.service");
            Files.writeString(serviceFile, service, StandardCharsets.UTF_8);

            // Enable and start user service (no root required)
            ProcessBuilder pb = new ProcessBuilder("systemctl", "--user", "daemon-reload");
            pb.redirectErrorStream(true);
            Process reloadProc = pb.start();
            reloadProc.waitFor();

            pb = new ProcessBuilder("systemctl", "--user", "enable", "--now", "lingle-startup.service");
            pb.redirectErrorStream(true);
            Process p = pb.start();

            // Capture output for debugging
            StringBuilder output = new StringBuilder();
            try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            int ec = p.waitFor();

            if (ec == 0) {
                UIUtils.showDarkMessage(parent, "Success", "Startup service installed and enabled.\n" +
                        "Directories will be created automatically on login.");
            } else {
                String errorMsg = "Failed to install/start service (exit " + ec + ")";
                if (output.length() > 0) {
                    errorMsg += "\n" + output.toString().trim();
                }
                UIUtils.showDarkMessage(parent, "Error", errorMsg);
            }

        } catch (Exception e) {
            UIUtils.showDarkMessage(parent, "Error", e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}

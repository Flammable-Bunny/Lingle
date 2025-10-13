package flammable.bunny.core;

import flammable.bunny.ui.UIUtils;
import javax.swing.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Comparator;
import java.util.List;

public class LinkManager {

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
            sb.append("  ln -sf \"$HOME/.local/share/lingle/saves/")
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
            String userHome = System.getProperty("user.home");
            String service = """
                    [Unit]
                    Description=Create Lingle instance directories on startup
                    After=graphical-session.target
                    Requires=graphical-session.target

                    [Service]
                    Type=oneshot
                    ExecStart=%h/.local/share/lingle/scripts/link_practice_maps.sh
                    RemainAfterExit=yes

                    [Install]
                    WantedBy=default.target
                    """;

            Path tmp = Files.createTempFile("lingle-tmpfs-service", ".service");
            Files.writeString(tmp, service, StandardCharsets.UTF_8);
            String target = userHome + "/.config/systemd/user/tmpfs.service";
            ProcessBuilder pb = new ProcessBuilder(
                    "bash", "-c",
                    "pkexec install -Dm644 " + tmp.toAbsolutePath() +
                            " /etc/systemd/system/tmpfs.service && " +
                            "sudo systemctl daemon-reload && " +
                            "sudo systemctl enable tmpfs.service"
            );
            int exit = pb.inheritIO().start().waitFor();
            if (exit == 0)
                UIUtils.showDarkMessage(parent, "Success", "User systemd service installed and enabled.");
            else
                UIUtils.showDarkMessage(parent, "Error", "Failed to install service. Exit code: " + exit);
        } catch (Exception e) {
            UIUtils.showDarkMessage(parent, "Error", e.getMessage());
        }
    }
}

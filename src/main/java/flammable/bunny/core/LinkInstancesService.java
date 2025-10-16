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

            String user = System.getProperty("user.name");
            String homeStr = home.toString();

            String service = """
                    [Unit]
                    Description=Lingle Practice Map Linker (System)
                    After=network-online.target
                    Wants=network-online.target

                    [Service]
                    Type=oneshot
                    User=%s
                    Environment=HOME=%s
                    ExecStart=%s
                    RemainAfterExit=yes

                    [Install]
                    WantedBy=multi-user.target
                    """.formatted(user, homeStr, script.toString());

            Path tmpService = Files.createTempFile("lingle-startup", ".service");
            Files.writeString(tmpService, service, StandardCharsets.UTF_8);

            String installCmd = String.join(" && ",
                    "install -D -m 0644 '" + tmpService + "' /etc/systemd/system/lingle-startup.service",
                    "systemctl daemon-reload",
                    "systemctl enable --now lingle-startup.service"
            );

            ProcessBuilder pb = new ProcessBuilder("pkexec", "bash", "-lc", installCmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            int ec = p.waitFor();

            if (ec == 0) {
                UIUtils.showDarkMessage(parent, "Success", "System service installed and enabled.");
            } else {
                String output = new String(p.getInputStream().readAllBytes());
                UIUtils.showDarkMessage(parent, "Error", "Failed to install/start service (exit " + ec + ").\n" +
                        "Output:\n" + output + "\n\nEnsure a polkit authentication agent is running.");
            }

        } catch (Exception e) {
            UIUtils.showDarkMessage(parent, "Error", e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}

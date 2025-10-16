package flammable.bunny.core;

import org.json.JSONObject;
import java.io.*;
import java.nio.file.*;
import java.util.List;

public final class DistroDetector {

    private static final Path CONFIG_PATH = Path.of(System.getProperty("user.home"))
            .resolve(".local/share/lingle/config.json");

    private DistroDetector() {}

    public static void detectAndSaveDistro() {
        String distro = detectDistro();
        String gpu = detectGPU();
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            JSONObject cfg = new JSONObject();
            if (Files.exists(CONFIG_PATH)) {
                try (BufferedReader r = Files.newBufferedReader(CONFIG_PATH)) {
                    cfg = new JSONObject(r.lines().reduce("", (a, b) -> a + b));
                } catch (Exception ignored) {}
            }
            cfg.put("distro", distro);
            cfg.put("gpu", gpu);
            try (BufferedWriter w = Files.newBufferedWriter(CONFIG_PATH)) {
                w.write(cfg.toString(2));
            }
        } catch (IOException ignored) {}
    }

    public static String getDistro() {
        String distro = readDistroFromConfig();
        if (distro == null || distro.equals("unknown")) {
            distro = detectDistro();
        }
        return distro;
    }

    public static String getPackageManager() {
        String distro = getDistro();
        return switch (distro.toLowerCase()) {
            case "arch", "endeavouros", "manjaro", "artix", "cachyos", "garuda" -> "pacman";
            case "debian", "ubuntu", "mint", "pop", "pop!_os", "kali", "elementary", "zorin" -> "apt";
            case "fedora", "rhel", "centos", "rocky", "almalinux" -> "dnf";
            case "opensuse", "suse", "opensuse-tumbleweed", "opensuse-leap" -> "zypper";
            case "alpine" -> "apk";
            case "nixos" -> "nix";
            case "void" -> "xbps";
            case "gentoo" -> "emerge";
            default -> null;
        };
    }

    private static String readDistroFromConfig() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                String json = Files.readString(CONFIG_PATH);
                JSONObject obj = new JSONObject(json);
                return obj.optString("distro", "unknown");
            }
        } catch (Exception ignored) {}
        return "unknown";
    }

    private static String detectDistro() {
        try {
            List<String> lines = Files.readAllLines(Path.of("/etc/os-release"));
            for (String line : lines) {
                if (line.startsWith("ID=")) {
                    return line.substring(3).replace("\"", "");
                }
            }
        } catch (IOException ignored) {}
        return "unknown";
    }

    public static String getGPU() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                String json = Files.readString(CONFIG_PATH);
                JSONObject obj = new JSONObject(json);
                return obj.optString("gpu", "unknown");
            }
        } catch (Exception ignored) {}
        return detectGPU();
    }

    private static String detectGPU() {
        try {
            Process p = new ProcessBuilder("lspci").start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                String lowerLine = line.toLowerCase();
                if (lowerLine.contains("vga") || lowerLine.contains("3d") || lowerLine.contains("display")) {
                    if (lowerLine.contains("nvidia")) {
                        return "nvidia";
                    } else if (lowerLine.contains("amd") || lowerLine.contains("radeon")) {
                        return "amd";
                    } else if (lowerLine.contains("intel")) {
                        return "intel";
                    }
                }
            }
        } catch (Exception ignored) {}
        return "unknown";
    }
}

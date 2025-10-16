package flammable.bunny.core;

import javax.swing.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.json.JSONObject;

import static flammable.bunny.ui.UIUtils.showDarkMessage;

public class DependencyInstaller {

    private static final Path CONFIG_PATH = Path.of(System.getProperty("user.home"))
            .resolve(".local/share/lingle/config.json");

    /** Checks jq, zip, and python3, installs if missing using pkexec. */
    public static boolean ensureDeps(JFrame parent) {
        List<String> missing = new ArrayList<>();

        if (!commandExists("jq")) missing.add("jq");
        if (!commandExists("zip")) missing.add("zip");
        if (!commandExists("python3")) missing.add("python3");

        if (missing.isEmpty()) return true;

        String deps = String.join(", ", missing);
        int choice = JOptionPane.showOptionDialog(
                parent,
                "You need the following dependencies:\n" + deps,
                "Missing Dependencies",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                new String[]{"Install", "Don't Install"},
                "Install"
        );

        if (choice == JOptionPane.YES_OPTION) {
            try {
                String pkgManager = detectPackageManager();
                if (pkgManager == null) {
                    showDarkMessage(parent, "Error", "Could not detect package manager.");
                    return false;
                }
                installPackages(pkgManager, missing, parent);
                showDarkMessage(parent, "Done", "Dependencies installed successfully.");
                return true;
            } catch (Exception e) {
                showDarkMessage(parent, "Error", "Failed to install dependencies:\n" + e.getMessage());
                return false;
            }
        }

        return false;
    }

    /** Detect package manager (prefers distro info from config.json). */
    private static String detectPackageManager() {
        String distro = readDistroFromConfig();
        if (distro == null || distro.equals("unknown")) {
            distro = detectDistroFallback();
        }

        // Match by known IDs
        return switch (distro.toLowerCase()) {
            case "arch", "endeavouros", "manjaro", "artix", "cachyos", "garuda" -> "pacman";
            case "debian", "ubuntu", "mint", "pop", "pop!_os", "kali", "elementary", "zorin" -> "apt";
            case "fedora", "rhel", "centos", "rocky", "almalinux" -> "dnf";
            case "opensuse", "suse", "opensuse-tumbleweed", "opensuse-leap" -> "zypper";
            case "alpine" -> "apk";
            case "nixos" -> "nix";
            case "void" -> "xbps";
            case "gentoo" -> "emerge";
            default -> detectDistroFallback();
        };
    }

    private static void installPackages(String mgr, List<String> pkgs, JFrame parent)
            throws IOException, InterruptedException {

        String joined = String.join(" ", pkgs);
        List<String> cmd = new ArrayList<>();
        cmd.add("pkexec");
        cmd.add("bash");
        cmd.add("-c");

        String installCmd = switch (mgr) {
            case "pacman" -> "pacman -S --noconfirm " + joined;
            case "apt" -> "apt update && apt install -y " + joined;
            case "dnf" -> "dnf install -y " + joined;
            case "zypper" -> "zypper install -y " + joined;
            case "apk" -> "apk add " + joined;
            case "nix" -> "nix-env -iA nixpkgs." + joined.replace(" ", " nixpkgs.");
            case "xbps" -> "xbps-install -Sy " + joined;
            case "emerge" -> "emerge " + joined;
            default -> throw new IOException("Unsupported package manager: " + mgr);
        };

        cmd.add(installCmd);
        new ProcessBuilder(cmd)
                .inheritIO()
                .start()
                .waitFor();
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

    private static String detectDistroFallback() {
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

    private static boolean commandExists(String cmd) {
        try {
            Process p = new ProcessBuilder("bash", "-c", "command -v " + cmd).start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}

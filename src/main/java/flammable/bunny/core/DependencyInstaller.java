package flammable.bunny.core;

import javax.swing.*;
import java.io.*;
import java.util.*;

import static flammable.bunny.ui.UIUtils.showDarkMessage;

public class DependencyInstaller {

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

    private static String detectPackageManager() {
        return DistroDetector.getPackageManager();
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

    private static boolean commandExists(String cmd) {
        try {
            Process p = new ProcessBuilder("bash", "-c", "command -v " + cmd).start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}

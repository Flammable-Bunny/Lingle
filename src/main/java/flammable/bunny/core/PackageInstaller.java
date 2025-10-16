package flammable.bunny.core;

import javax.swing.*;
import java.io.*;
import java.util.*;

import static flammable.bunny.ui.UIUtils.showDarkMessage;

public class PackageInstaller {

    private static final Map<String, Map<String, String>> PACKAGE_MAPPINGS = new HashMap<>();

    static {
        Map<String, String> git = new HashMap<>();
        git.put("pacman", "git");
        git.put("apt", "git");
        git.put("dnf", "git");
        PACKAGE_MAPPINGS.put("Git", git);

        Map<String, String> podman = new HashMap<>();
        podman.put("pacman", "podman");
        podman.put("apt", "podman");
        podman.put("dnf", "podman");
        PACKAGE_MAPPINGS.put("Podman", podman);

        Map<String, String> docker = new HashMap<>();
        docker.put("pacman", "docker");
        docker.put("apt", "docker.io");
        docker.put("dnf", "docker");
        PACKAGE_MAPPINGS.put("Docker", docker);

        Map<String, String> go = new HashMap<>();
        go.put("pacman", "go");
        go.put("apt", "golang");
        go.put("dnf", "golang");
        PACKAGE_MAPPINGS.put("Go", go);
    }

    public static void installPackages(List<String> packageNames, JFrame parent) {
        try {
            String pkgManager = detectPackageManager();
            if (pkgManager == null) {
                showDarkMessage(parent, "Error", "Could not detect package manager.");
                return;
            }

            List<String> toInstall = new ArrayList<>();
            boolean isWaywallGLFW = false;

            for (String pkgName : packageNames) {
                if (pkgName.equals("Waywall + GLFW")) {
                    if (!isSupportedDistro(pkgManager)) {
                        showDarkMessage(parent, "Unsupported Distro",
                            "Automatic Waywall + GLFW installation is only supported on:\n" +
                            "- Arch Linux (and derivatives)\n" +
                            "- Fedora (and derivatives)\n" +
                            "- Debian (and derivatives including Ubuntu)");
                        return;
                    }
                    isWaywallGLFW = true;
                    addPackageIfAvailable("Git", pkgManager, toInstall);
                    addPackageIfAvailable("Podman", pkgManager, toInstall);
                    addPackageIfAvailable("Docker", pkgManager, toInstall);
                    addPackageIfAvailable("Go", pkgManager, toInstall);
                } else {
                    addPackageIfAvailable(pkgName, pkgManager, toInstall);
                }
            }

            if (toInstall.isEmpty()) {
                showDarkMessage(parent, "Info", "No packages to install.");
                return;
            }

            executeInstall(pkgManager, toInstall, parent);

            if (isWaywallGLFW) {
                setupWaywallGLFW(parent);
            } else {
                showDarkMessage(parent, "Done", "Packages installed successfully.");
            }

        } catch (Exception e) {
            showDarkMessage(parent, "Error", "Failed to install packages:\n" + e.getMessage());
        }
    }

    private static boolean isSupportedDistro(String pkgManager) {
        return pkgManager.equals("pacman") || pkgManager.equals("dnf") || pkgManager.equals("apt");
    }

    private static void addPackageIfAvailable(String pkgName, String pkgManager, List<String> toInstall) {
        Map<String, String> mapping = PACKAGE_MAPPINGS.get(pkgName);
        if (mapping != null && mapping.containsKey(pkgManager)) {
            toInstall.add(mapping.get(pkgManager));
        }
    }

    private static String detectPackageManager() {
        return DistroDetector.getPackageManager();
    }

    private static void executeInstall(String mgr, List<String> pkgs, JFrame parent)
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

    private static void setupWaywallGLFW(JFrame parent) {
        try {
            showDarkMessage(parent, "Setup", "Installing pacur and configuring Docker images...\nThis may take several minutes.");

            String home = System.getProperty("user.home");

            ProcessBuilder goInstall = new ProcessBuilder("bash", "-c",
                "export PATH=$PATH:$HOME/go/bin && go install github.com/pacur/pacur@latest");
            goInstall.inheritIO();
            int exitCode = goInstall.start().waitFor();

            if (exitCode != 0) {
                showDarkMessage(parent, "Error", "Failed to install pacur via go install");
                return;
            }

            java.nio.file.Path pacurBase = java.nio.file.Path.of(home, "go", "pkg", "mod", "github.com", "pacur");
            if (!java.nio.file.Files.exists(pacurBase)) {
                showDarkMessage(parent, "Error", "Pacur directory not found at: " + pacurBase);
                return;
            }

            java.nio.file.Path pacurDir = java.nio.file.Files.list(pacurBase)
                .filter(p -> p.getFileName().toString().startsWith("pacur@"))
                .findFirst()
                .orElse(null);

            if (pacurDir == null) {
                showDarkMessage(parent, "Error", "Could not find pacur version directory");
                return;
            }

            java.nio.file.Path dockerDir = pacurDir.resolve("docker");

            // Remove all directories except archlinux, fedora-42, and debian-trixie
            List<String> dirsToRemove = new ArrayList<>();
            java.nio.file.Files.list(dockerDir)
                .filter(java.nio.file.Files::isDirectory)
                .filter(p -> {
                    String name = p.getFileName().toString();
                    return !name.equals("archlinux") && !name.equals("fedora-42") && !name.equals("debian-trixie");
                })
                .forEach(p -> dirsToRemove.add(p.toString()));

            if (!dirsToRemove.isEmpty()) {
                String rmCmd = "rm -rf " + String.join(" ", dirsToRemove);
                ProcessBuilder rmProcess = new ProcessBuilder("pkexec", "bash", "-c", rmCmd);
                rmProcess.inheritIO();
                int rmExitCode = rmProcess.start().waitFor();

                if (rmExitCode != 0) {
                    showDarkMessage(parent, "Warning", "Failed to remove some Docker directories");
                }
            }

            editScriptFile(dockerDir.resolve("update.sh"));
            editScriptFile(dockerDir.resolve("build.sh"));

            ProcessBuilder updateSh = new ProcessBuilder("bash", dockerDir.resolve("update.sh").toString());
            updateSh.directory(dockerDir.toFile());
            updateSh.inheritIO();
            exitCode = updateSh.start().waitFor();

            if (exitCode != 0) {
                showDarkMessage(parent, "Warning", "update.sh completed with exit code: " + exitCode);
            }

            ProcessBuilder buildSh = new ProcessBuilder("bash", dockerDir.resolve("build.sh").toString());
            buildSh.directory(dockerDir.toFile());
            buildSh.inheritIO();
            exitCode = buildSh.start().waitFor();

            if (exitCode != 0) {
                showDarkMessage(parent, "Warning", "build.sh completed with exit code: " + exitCode);
            }

            showDarkMessage(parent, "Done", "Waywall + GLFW setup completed successfully!");

        } catch (Exception e) {
            showDarkMessage(parent, "Error", "Setup failed:\n" + e.getMessage());
        }
    }

    private static void editScriptFile(java.nio.file.Path scriptPath) throws IOException {
        if (!java.nio.file.Files.exists(scriptPath)) return;

        String content = java.nio.file.Files.readString(scriptPath);
        content = content.replaceAll("su podman", "podman");
        content = content.replaceAll("sudo podman", "podman");
        java.nio.file.Files.writeString(scriptPath, content);
    }

    private static void deleteDirectory(java.nio.file.Path dir) throws IOException {
        if (java.nio.file.Files.isDirectory(dir)) {
            java.nio.file.Files.walk(dir)
                .sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        java.nio.file.Files.delete(p);
                    } catch (IOException ignored) {}
                });
        }
    }
}

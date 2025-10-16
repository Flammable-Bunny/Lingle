package flammable.bunny.core;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static flammable.bunny.ui.UIUtils.showDarkMessage;

public class PackageInstaller {

    // Error codes for Waywall + GLFW installation
    private static final int ERR_PKG_MGR_NOT_DETECTED = 1001;
    private static final int ERR_UNSUPPORTED_DISTRO = 1002;
    private static final int ERR_INSTALL_FAILED = 1003;
    private static final int ERR_PACUR_INSTALL_FAILED = 2001;
    private static final int ERR_PACUR_DIR_NOT_FOUND = 2002;
    private static final int ERR_PACUR_VERSION_NOT_FOUND = 2003;
    private static final int ERR_UPDATE_SCRIPT_FAILED = 2004;
    private static final int ERR_BUILD_SCRIPT_FAILED = 2005;
    private static final int ERR_WAYWALL_CLONE_FAILED = 2006;
    private static final int ERR_WAYWALL_BUILD_FAILED = 2007;
    private static final int ERR_PRISM_CONFIG_FAILED = 3001;
    private static final int ERR_DISCORD_DOWNLOAD_FAILED = 4001;
    private static final int ERR_DISCORD_INSTALL_FAILED = 4002;
    private static final int ERR_GENERAL_SETUP = 2999;

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

        Map<String, String> prism = new HashMap<>();
        prism.put("pacman", "prismlauncher jdk17-openjdk");
        prism.put("apt", "prismlauncher openjdk-17-jdk");
        prism.put("dnf", ""); // Special handling for Fedora COPR
        PACKAGE_MAPPINGS.put("Prism Launcher", prism);

        Map<String, String> obs = new HashMap<>();
        obs.put("pacman", "obs-studio");
        obs.put("apt", "obs-studio");
        obs.put("dnf", "obs-studio");
        PACKAGE_MAPPINGS.put("OBS Studio", obs);

        Map<String, String> discord = new HashMap<>();
        discord.put("pacman", "discord");
        discord.put("apt", "discord");
        discord.put("dnf", "discord");
        PACKAGE_MAPPINGS.put("Discord", discord);
    }

    public static void installPackages(List<String> packageNames, JFrame parent) {
        try {
            String pkgManager = detectPackageManager();
            if (pkgManager == null) {
                showDarkMessage(parent, "Error", formatError(ERR_PKG_MGR_NOT_DETECTED, "Could not detect package manager."));
                return;
            }

            List<String> toInstall = new ArrayList<>();
            boolean isWaywallGLFW = false;
            boolean isPrismLauncher = false;
            boolean isDiscord = false;

            for (String pkgName : packageNames) {
                if (pkgName.equals("Waywall + GLFW")) {
                    if (!isSupportedDistro(pkgManager)) {
                        showDarkMessage(parent, "Unsupported Distro",
                            formatError(ERR_UNSUPPORTED_DISTRO,
                                "Automatic Waywall + GLFW installation is only supported on:\n" +
                                "- Arch Linux (and derivatives)\n" +
                                "- Fedora (and derivatives)\n" +
                                "- Debian (and derivatives including Ubuntu)"));
                        return;
                    }
                    isWaywallGLFW = true;

                    // Install Fedora-specific dependencies first if needed
                    if ("dnf".equals(pkgManager)) {
                        try {
                            FedoraInstaller.installWaywallDependencies();
                        } catch (Exception e) {
                            showDarkMessage(parent, "Error", formatError(ERR_INSTALL_FAILED, "Failed to install Fedora dependencies:\n" + e.getMessage()));
                            return;
                        }
                    }

                    addPackageIfAvailable("Git", pkgManager, toInstall);
                    addPackageIfAvailable("Podman", pkgManager, toInstall);
                    addPackageIfAvailable("Docker", pkgManager, toInstall);
                    addPackageIfAvailable("Go", pkgManager, toInstall);
                } else if (pkgName.equals("Prism Launcher")) {
                    isPrismLauncher = true;

                    // Use COPR for Fedora
                    if ("dnf".equals(pkgManager)) {
                        try {
                            FedoraInstaller.installPrismLauncher();
                        } catch (Exception e) {
                            showDarkMessage(parent, "Error", formatError(ERR_INSTALL_FAILED, "Failed to install Prism Launcher:\n" + e.getMessage()));
                            return;
                        }
                    } else {
                        addPackageIfAvailable("Prism Launcher", pkgManager, toInstall);
                    }
                } else if (pkgName.equals("Discord")) {
                    isDiscord = true;
                    addPackageIfAvailable("Discord", pkgManager, toInstall);
                } else {
                    addPackageIfAvailable(pkgName, pkgManager, toInstall);
                }
            }

            if (toInstall.isEmpty()) {
                showDarkMessage(parent, "Info", "No packages to install.");
                return;
            }

            executeInstall(pkgManager, toInstall, parent);

            // Handle Discord app.asar replacement after Discord is installed
            if (isDiscord) {
                try {
                    DiscordInstaller.installCustomAppAsar(pkgManager);
                    showDarkMessage(parent, "Done", "Discord installed and configured with OpenAsar successfully!");
                } catch (Exception e) {
                    showDarkMessage(parent, "Warning", formatError(ERR_DISCORD_INSTALL_FAILED, "Discord installed but app.asar replacement failed:\n" + e.getMessage()));
                }
                return;
            }

            if (isWaywallGLFW) {
                setupWaywallGLFW(parent);
            } else if (isPrismLauncher) {
                configurePrismLauncher(pkgManager, parent);
            } else {
                showDarkMessage(parent, "Done", "Packages installed successfully.");
            }

        } catch (Exception e) {
            showDarkMessage(parent, "Error", formatError(ERR_INSTALL_FAILED, "Failed to install packages:\n" + e.getMessage()));
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

        ProcessBuilder pb = new ProcessBuilder("sudo", "bash", "-c", installCmd);
        pb.inheritIO().start().waitFor();
    }

    private static void setupWaywallGLFW(JFrame parent) {
        JDialog progressDialog = null;
        try {
            String home = System.getProperty("user.home");

            // Create progress dialog with progress bar
            progressDialog = new JDialog(parent, "Waywall + GLFW Setup", true);
            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.setBackground(new Color(43, 43, 43));
            panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

            JProgressBar progressBar = new JProgressBar(0, 7);
            progressBar.setStringPainted(true);
            progressBar.setString("Installing dependencies");
            progressBar.setForeground(new Color(106, 153, 85));
            progressBar.setBackground(new Color(60, 63, 65));

            panel.add(progressBar, BorderLayout.CENTER);
            progressDialog.add(panel);
            progressDialog.setSize(400, 100);
            progressDialog.setLocationRelativeTo(parent);
            progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

            JDialog finalProgressDialog = progressDialog;

            // Run installation in a separate thread
            Thread installThread = new Thread(() -> {
                try {
                    // Step 1: Installing dependencies (already done by previous steps)
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setValue(1);
                        progressBar.setString("Downloading pacur");
                    });

                    ProcessBuilder goInstall = new ProcessBuilder("bash", "-c",
                        "export PATH=$PATH:$HOME/go/bin && go install github.com/pacur/pacur@latest");
                    goInstall.inheritIO();
                    int exitCode = goInstall.start().waitFor();

                    if (exitCode != 0) {
                        SwingUtilities.invokeLater(() -> {
                            finalProgressDialog.dispose();
                            showDarkMessage(parent, "Error", formatError(ERR_PACUR_INSTALL_FAILED, "Failed to install pacur via go install"));
                        });
                        return;
                    }

                    java.nio.file.Path pacurBase = java.nio.file.Path.of(home, "go", "pkg", "mod", "github.com", "pacur");
                    if (!java.nio.file.Files.exists(pacurBase)) {
                        SwingUtilities.invokeLater(() -> {
                            finalProgressDialog.dispose();
                            showDarkMessage(parent, "Error", formatError(ERR_PACUR_DIR_NOT_FOUND, "Pacur directory not found at: " + pacurBase));
                        });
                        return;
                    }

                    java.nio.file.Path pacurDir = java.nio.file.Files.list(pacurBase)
                        .filter(p -> p.getFileName().toString().startsWith("pacur@"))
                        .findFirst()
                        .orElse(null);

                    if (pacurDir == null) {
                        SwingUtilities.invokeLater(() -> {
                            finalProgressDialog.dispose();
                            showDarkMessage(parent, "Error", formatError(ERR_PACUR_VERSION_NOT_FOUND, "Could not find pacur version directory"));
                        });
                        return;
                    }

                    // Step 2: Deleting unnecessary files and updating scripts
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setValue(2);
                        progressBar.setString("Preparing pacur environment");
                    });

                    java.nio.file.Path dockerDir = pacurDir.resolve("docker");

                    // Build list of directories to remove
                    StringBuilder dirsToRemove = new StringBuilder();
                    java.nio.file.Files.list(dockerDir)
                        .filter(java.nio.file.Files::isDirectory)
                        .filter(p -> {
                            String name = p.getFileName().toString();
                            return !name.equals("archlinux") && !name.equals("fedora-42") && !name.equals("debian-trixie");
                        })
                        .forEach(p -> {
                            if (dirsToRemove.length() > 0) dirsToRemove.append(" ");
                            dirsToRemove.append("'").append(p.toString()).append("'");
                        });

                    // Use sudo for root operations - sudo caches credentials for subsequent calls
                    String combinedCmd;
                    if (dirsToRemove.length() > 0) {
                        combinedCmd = String.format(
                            "rm -rf %s && cd '%s' && sed -i 's/su podman/podman/g; s/sudo podman/podman/g; s/sudo docker/docker/g' update.sh build.sh",
                            dirsToRemove.toString(),
                            dockerDir.toString()
                        );
                    } else {
                        combinedCmd = String.format(
                            "cd '%s' && sed -i 's/su podman/podman/g; s/sudo podman/podman/g; s/sudo docker/docker/g' update.sh build.sh",
                            dockerDir.toString()
                        );
                    }

                    ProcessBuilder combinedProcess = new ProcessBuilder("sudo", "bash", "-c", combinedCmd);
                    combinedProcess.inheritIO();
                    int combinedExitCode = combinedProcess.start().waitFor();

                    if (combinedExitCode != 0) {
                        SwingUtilities.invokeLater(() -> {
                            finalProgressDialog.dispose();
                            showDarkMessage(parent, "Error", formatError(ERR_UPDATE_SCRIPT_FAILED, "Failed to prepare pacur environment"));
                        });
                        return;
                    }

                    // Step 3: Running update and build scripts
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setValue(3);
                        progressBar.setString("Building pacur containers");
                    });

                    ProcessBuilder updateSh = new ProcessBuilder("bash", dockerDir.resolve("update.sh").toString());
                    updateSh.directory(dockerDir.toFile());
                    updateSh.inheritIO();
                    int updateExitCode = updateSh.start().waitFor();

                    if (updateExitCode != 0) {
                        final int finalUpdateExitCode = updateExitCode;
                        SwingUtilities.invokeLater(() -> {
                            finalProgressDialog.dispose();
                            showDarkMessage(parent, "Error", formatError(ERR_UPDATE_SCRIPT_FAILED, "update.sh failed with exit code: " + finalUpdateExitCode));
                        });
                        return;
                    }

                    ProcessBuilder buildSh = new ProcessBuilder("bash", dockerDir.resolve("build.sh").toString());
                    buildSh.directory(dockerDir.toFile());
                    buildSh.inheritIO();
                    int buildExitCode = buildSh.start().waitFor();

                    if (buildExitCode != 0) {
                        final int finalBuildExitCode = buildExitCode;
                        SwingUtilities.invokeLater(() -> {
                            finalProgressDialog.dispose();
                            showDarkMessage(parent, "Error", formatError(ERR_BUILD_SCRIPT_FAILED, "build.sh failed with exit code: " + finalBuildExitCode));
                        });
                        return;
                    }

                    // Step 4: Cloning waywall
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setValue(4);
                        progressBar.setString("Cloning waywall");
                    });

                    java.nio.file.Path waywallDir = java.nio.file.Path.of(home, "waywall");

                    // Remove existing waywall directory if it exists
                    if (java.nio.file.Files.exists(waywallDir)) {
                        deleteDirectory(waywallDir);
                    }

                    ProcessBuilder gitClone = new ProcessBuilder("git", "clone", "https://github.com/tesselslate/waywall", waywallDir.toString());
                    gitClone.inheritIO();
                    int cloneExitCode = gitClone.start().waitFor();

                    if (cloneExitCode != 0) {
                        SwingUtilities.invokeLater(() -> {
                            finalProgressDialog.dispose();
                            showDarkMessage(parent, "Error", formatError(ERR_WAYWALL_CLONE_FAILED, "Failed to clone waywall repository"));
                        });
                        return;
                    }

                    // Step 5: Running installation script
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setValue(5);
                        progressBar.setString("Running installation script");
                    });

                    java.nio.file.Path buildPackagesScript = waywallDir.resolve("build-packages.sh");
                    if (!java.nio.file.Files.exists(buildPackagesScript)) {
                        SwingUtilities.invokeLater(() -> {
                            finalProgressDialog.dispose();
                            showDarkMessage(parent, "Error", formatError(ERR_WAYWALL_BUILD_FAILED, "build-packages.sh not found at: " + buildPackagesScript));
                        });
                        return;
                    }

                    // Determine distro family flag
                    String pkgManager = detectPackageManager();
                    String distroFlag;
                    if (pkgManager != null) {
                        distroFlag = switch (pkgManager) {
                            case "pacman" -> "--arch";
                            case "dnf" -> "--fedora";
                            case "apt" -> "--debian";
                            default -> null;
                        };
                    } else {
                        distroFlag = null;
                    }

                    ProcessBuilder buildPackages;
                    if (distroFlag != null) {
                        buildPackages = new ProcessBuilder("bash", buildPackagesScript.toString(), distroFlag);
                    } else {
                        buildPackages = new ProcessBuilder("bash", buildPackagesScript.toString());
                    }
                    buildPackages.directory(waywallDir.toFile());
                    buildPackages.inheritIO();
                    int buildPackagesExitCode = buildPackages.start().waitFor();

                    if (buildPackagesExitCode != 0) {
                        final int finalBuildPackagesExitCode = buildPackagesExitCode;
                        SwingUtilities.invokeLater(() -> {
                            finalProgressDialog.dispose();
                            showDarkMessage(parent, "Error", formatError(ERR_WAYWALL_BUILD_FAILED, "build-packages.sh failed with exit code: " + finalBuildPackagesExitCode));
                        });
                        return;
                    }

                    // Step 6: Installing waywall and GLFW
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setValue(6);
                        progressBar.setString("Installing waywall and GLFW");
                    });

                    // Determine the package file to install
                    java.nio.file.Path buildDir = waywallDir.resolve("waywall-build");
                    String installPackageCmd = null;

                    if (pkgManager != null) {
                        switch (pkgManager) {
                            case "pacman" -> {
                                java.nio.file.Path pkgFile = buildDir.resolve("waywall-0.5-1-x86_64.pkg.tar.zst");
                                if (java.nio.file.Files.exists(pkgFile)) {
                                    installPackageCmd = "pacman -U --noconfirm " + pkgFile.toString();
                                }
                            }
                            case "dnf" -> {
                                java.nio.file.Path rpmFile = buildDir.resolve("waywall-0.5-1.fc42.x86_64.rpm");
                                if (java.nio.file.Files.exists(rpmFile)) {
                                    installPackageCmd = "dnf localinstall -y " + rpmFile.toString();
                                }
                            }
                            case "apt" -> {
                                java.nio.file.Path debFile = buildDir.resolve("waywall_0.5-1_amd64.deb");
                                if (java.nio.file.Files.exists(debFile)) {
                                    installPackageCmd = "dpkg -i " + debFile.toString();
                                }
                            }
                        }
                    }

                    if (installPackageCmd != null) {
                        ProcessBuilder installPkg = new ProcessBuilder("sudo", "bash", "-c", installPackageCmd);
                        installPkg.inheritIO();
                        int installExitCode = installPkg.start().waitFor();

                        if (installExitCode != 0) {
                            final int finalInstallExitCode = installExitCode;
                            SwingUtilities.invokeLater(() -> {
                                finalProgressDialog.dispose();
                                showDarkMessage(parent, "Error", formatError(ERR_WAYWALL_BUILD_FAILED, "Failed to install waywall package with exit code: " + finalInstallExitCode));
                            });
                            return;
                        }
                    } else {
                        SwingUtilities.invokeLater(() -> {
                            finalProgressDialog.dispose();
                            showDarkMessage(parent, "Error", formatError(ERR_WAYWALL_BUILD_FAILED, "Could not find waywall package file in " + buildDir));
                        });
                        return;
                    }

                    // Step 7: Complete
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setValue(7);
                        progressBar.setString("Complete");
                        finalProgressDialog.dispose();
                        showDarkMessage(parent, "Done", "Waywall + GLFW setup completed successfully!");
                    });

                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        finalProgressDialog.dispose();
                        showDarkMessage(parent, "Error", formatError(ERR_GENERAL_SETUP, "Setup failed:\n" + e.getMessage()));
                    });
                }
            });

            installThread.start();
            progressDialog.setVisible(true);

        } catch (Exception e) {
            if (progressDialog != null) {
                progressDialog.dispose();
            }
            showDarkMessage(parent, "Error", formatError(ERR_GENERAL_SETUP, "Setup failed:\n" + e.getMessage()));
        }
    }

    private static void editScriptFile(java.nio.file.Path scriptPath) throws IOException {
        if (!java.nio.file.Files.exists(scriptPath)) return;

        String content = java.nio.file.Files.readString(scriptPath);
        content = content.replaceAll("su podman", "podman");
        content = content.replaceAll("sudo podman", "podman");
        content = content.replaceAll("sudo docker", "docker");
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

    private static void configurePrismLauncher(String pkgManager, JFrame parent) {
        try {
            String home = System.getProperty("user.home");
            Path configFile = Path.of(home, ".local/share/PrismLauncher/prismlauncher.cfg");

            // Create directory if it doesn't exist
            Files.createDirectories(configFile.getParent());

            // Determine Java path based on distro
            String javaPath;
            if ("dnf".equals(pkgManager)) {
                javaPath = "/usr/lib/jvm/java-21-openjdk/bin/java";
            } else {
                javaPath = "/usr/lib/jvm/java-17-openjdk/bin/java";
            }

            // Read existing config or create new
            List<String> lines = new ArrayList<>();
            if (Files.exists(configFile)) {
                lines = Files.readAllLines(configFile);
            }

            // Settings to set/update
            Map<String, String> settings = new HashMap<>();
            settings.put("IgnoreJavaCompatibility", "true");
            settings.put("AutomaticJavaDownload", "false");
            settings.put("JavaDir", "java");
            settings.put("JavaPath", javaPath);
            settings.put("WrapperCommand", "waywall wrap --");
            settings.put("CustomGLFWPath", "/usr/local/lib64/waywall-glfw/libglfw.so");
            settings.put("UseNativeGLFW", "true");

            // Update or add settings
            for (Map.Entry<String, String> setting : settings.entrySet()) {
                String key = setting.getKey();
                String value = setting.getValue();
                boolean found = false;

                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    if (line.startsWith(key + "=")) {
                        lines.set(i, key + "=" + value);
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    lines.add(key + "=" + value);
                }
            }

            Files.write(configFile, lines);

            showDarkMessage(parent, "Done", "Prism Launcher installed and configured successfully!");

        } catch (Exception e) {
            showDarkMessage(parent, "Error", formatError(ERR_PRISM_CONFIG_FAILED, "Failed to configure Prism Launcher:\n" + e.getMessage()));
        }
    }

    private static String formatError(int errorCode, String message) {
        return String.format("[Error %d] %s", errorCode, message);
    }
}

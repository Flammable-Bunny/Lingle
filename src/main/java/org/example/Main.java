package org.example;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component;
import javax.swing.JCheckBox;


public class Main {

    private static volatile boolean enabled = false;
    private static JButton runButton;
    private static long lastClickTime = 0;

    public static void main(String[] args) {
        if (System.getenv("DISPLAY") == null && System.getenv("WAYLAND_DISPLAY") == null) {
            System.err.println("No DISPLAY/WAYLAND_DISPLAY found. This GUI requires a graphical session.");
            System.exit(1);
        }

        try {
            ensureScriptsPresent();
            detectCurrentState();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                    null,
                    "Failed to prepare scripts: " + e.getMessage(),
                    "Lingle",
                    JOptionPane.ERROR_MESSAGE
            );
            System.exit(1);
        }

        SwingUtilities.invokeLater(Main::createAndShowUI);
    }

    private static void createAndShowUI() {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        System.setProperty("sun.java2d.uiScale", "1.0");
        System.setProperty("awt.useSystemAAFontSettings", "off");
        System.setProperty("swing.useSystemExtensions", "false");

        JFrame frame = new JFrame("Lingle");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.setAlwaysOnTop(true);
        frame.setType(java.awt.Window.Type.UTILITY);
        frame.setResizable(false);
        frame.setUndecorated(true);

        javax.swing.JPanel titleBar = new javax.swing.JPanel(new BorderLayout());
        titleBar.setBackground(new Color(45, 45, 45));
        titleBar.setPreferredSize(new Dimension(0, 25));

        javax.swing.JLabel titleLabel = new javax.swing.JLabel("Lingle");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 5));

        javax.swing.JButton closeButton = new javax.swing.JButton("×");
        closeButton.setBackground(new Color(45, 45, 45));
        closeButton.setForeground(Color.WHITE);
        closeButton.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        closeButton.setFocusPainted(false);
        closeButton.addActionListener(e -> System.exit(0));
        closeButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                closeButton.setBackground(Color.RED);
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                closeButton.setBackground(new Color(45, 45, 45));
            }
        });

        titleBar.add(titleLabel, BorderLayout.WEST);
        titleBar.add(closeButton, BorderLayout.EAST);

        final java.awt.Point[] offset = new java.awt.Point[1];
        titleBar.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                offset[0] = e.getPoint();
            }
        });
        titleBar.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseDragged(java.awt.event.MouseEvent e) {
                java.awt.Point location = frame.getLocation();
                location.translate(e.getX() - offset[0].x, e.getY() - offset[0].y);
                frame.setLocation(location);
            }
        });

        javax.swing.JPanel navPanel = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 5, 5));
        navPanel.setBackground(new Color(64, 64, 64));
        navPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(100, 100, 100)));

        javax.swing.JButton tmpfsNavButton = new javax.swing.JButton("auToMPFS");
        javax.swing.JButton settingsNavButton = new javax.swing.JButton(" ");

        javax.swing.JButton[] navButtons = {tmpfsNavButton, settingsNavButton};
        for (javax.swing.JButton btn : navButtons) {
            btn.setBackground(new Color(80, 80, 80));
            btn.setForeground(Color.WHITE);
            btn.setBorder(BorderFactory.createLineBorder(new Color(150, 150, 150), 1));
            btn.setFocusPainted(false);
            btn.setFont(new Font("SansSerif", Font.PLAIN, 12));
            btn.setPreferredSize(new Dimension(80, 30));
            btn.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    btn.setBackground(new Color(70, 90, 130));
                    btn.setBorder(BorderFactory.createLineBorder(new Color(100, 150, 200), 1));
                }
                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    btn.setBackground(new Color(80, 80, 80));
                    btn.setBorder(BorderFactory.createLineBorder(new Color(150, 150, 150), 1));
                }
            });
        }

        navPanel.add(tmpfsNavButton);
        navPanel.add(settingsNavButton);

        java.awt.CardLayout cardLayout = new java.awt.CardLayout();
        javax.swing.JPanel contentPanel = new javax.swing.JPanel(cardLayout);
        contentPanel.setBackground(new Color(64, 64, 64));

        runButton = new JButton(buttonText());
        runButton.addActionListener(_ -> runToggleScriptAsync());

        runButton.setBackground(new Color(80, 80, 80));
        runButton.setForeground(Color.WHITE);
        runButton.setBorder(BorderFactory.createLineBorder(new Color(150, 150, 150), 2));
        runButton.setFocusPainted(false);
        runButton.setFont(new Font("SansSerif", Font.PLAIN, 14));

        runButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                runButton.setBackground(new Color(70, 90, 130));
                runButton.setBorder(BorderFactory.createLineBorder(new Color(100, 150, 200), 2));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                runButton.setBackground(new Color(80, 80, 80));
                runButton.setBorder(BorderFactory.createLineBorder(new Color(150, 150, 150), 2));
            }
        });

        javax.swing.JPanel tmpfsPanel = new javax.swing.JPanel(new BorderLayout());
        tmpfsPanel.setBackground(new Color(64, 64, 64));

        javax.swing.JPanel buttonSection = new javax.swing.JPanel(null);
        buttonSection.setBackground(new Color(64, 64, 64));
        buttonSection.setPreferredSize(new Dimension(0, 80));

        JLabel tmpfsLabel = new JLabel("Enable TMPFS");
        tmpfsLabel.setForeground(Color.WHITE);
        tmpfsLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        tmpfsLabel.setBounds(10, 10, 150, 25);
        buttonSection.add(tmpfsLabel);

        runButton.setBounds(10, 35, 100, 35);
        buttonSection.add(runButton);

        javax.swing.JPanel instancesSection = new javax.swing.JPanel(new BorderLayout());
        instancesSection.setBackground(new Color(64, 64, 64));

        javax.swing.JLabel instancesLabel = new javax.swing.JLabel("PrismLauncher Instances:");
        instancesLabel.setForeground(Color.WHITE);
        instancesLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        instancesLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));

        javax.swing.JPanel checkboxPanel = new javax.swing.JPanel();
        checkboxPanel.setLayout(new javax.swing.BoxLayout(checkboxPanel, javax.swing.BoxLayout.Y_AXIS));
        checkboxPanel.setBackground(new Color(64, 64, 64));

        Path home = Path.of(System.getProperty("user.home"));
        Path prismInstancesDir = home.resolve(".local").resolve("share").resolve("PrismLauncher").resolve("instances");

        try {
            if (Files.exists(prismInstancesDir) && Files.isDirectory(prismInstancesDir)) {
                Files.list(prismInstancesDir)
                        .filter(Files::isDirectory)
                        .sorted()
                        .forEach(instanceDir -> {
                            String instanceName = instanceDir.getFileName().toString();
                            javax.swing.JCheckBox checkbox = new javax.swing.JCheckBox(instanceName);
                            checkbox.setBackground(new Color(64, 64, 64));
                            checkbox.setForeground(Color.WHITE);
                            checkbox.setFont(new Font("SansSerif", Font.PLAIN, 12));
                            checkbox.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
                            checkboxPanel.add(checkbox);
                        });
            } else {
                javax.swing.JLabel noInstancesLabel = new javax.swing.JLabel("No PrismLauncher instances found");
                noInstancesLabel.setForeground(Color.LIGHT_GRAY);
                noInstancesLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));
                noInstancesLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                checkboxPanel.add(noInstancesLabel);
            }
        } catch (IOException e) {
            javax.swing.JLabel errorLabel = new javax.swing.JLabel("Error reading instances directory");
            errorLabel.setForeground(Color.RED);
            errorLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));
            errorLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            checkboxPanel.add(errorLabel);
        }

        javax.swing.JScrollPane scrollPane = new javax.swing.JScrollPane(checkboxPanel);
        scrollPane.setBackground(new Color(64, 64, 64));
        scrollPane.getViewport().setBackground(new Color(64, 64, 64));
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        instancesSection.add(instancesLabel, BorderLayout.NORTH);
        instancesSection.add(scrollPane, BorderLayout.CENTER);

        JButton symlinkButton = new JButton("Symlink Instances");
        symlinkButton.setBackground(new Color(80, 80, 80));
        symlinkButton.setForeground(Color.WHITE);
        symlinkButton.setBorder(BorderFactory.createLineBorder(new Color(150, 150, 150), 2));
        symlinkButton.setFocusPainted(false);
        symlinkButton.setFont(new Font("SansSerif", Font.PLAIN, 14));

        symlinkButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                symlinkButton.setBackground(new Color(70, 90, 130));
                symlinkButton.setBorder(BorderFactory.createLineBorder(new Color(100, 150, 200), 2));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                symlinkButton.setBackground(new Color(80, 80, 80));
                symlinkButton.setBorder(BorderFactory.createLineBorder(new Color(150, 150, 150), 2));
            }
        });

        symlinkButton.addActionListener(e -> {
            List<JCheckBox> selectedCheckboxes = new ArrayList<>();
            Component[] components = checkboxPanel.getComponents();
            for (Component component : components) {
                if (component instanceof JCheckBox && ((JCheckBox) component).isSelected()) {
                    selectedCheckboxes.add((JCheckBox) component);
                }
            }

            if (!selectedCheckboxes.isEmpty()) {
                int numInstances = selectedCheckboxes.size();
                String userHome = System.getProperty("user.home");

                try {
                    for (int i = 0; i < numInstances; i++) {
                        JCheckBox checkbox = selectedCheckboxes.get(i);
                        String instanceName = checkbox.getText();

                        Path lingleDir = Path.of(userHome).resolve("Lingle").resolve(String.valueOf(i + 1));
                        Files.createDirectories(lingleDir);

                        ProcessBuilder pb = new ProcessBuilder(
                                "sudo", "-n", "chown", userHome + ":" + lingleDir.toString()
                        );
                        pb.start().waitFor();
                        pb = new ProcessBuilder("sudo", "-n", "chmod", "700", lingleDir.toString());
                        pb.start().waitFor();

                        Path instancePathMinecraft = Path.of(userHome)
                                .resolve(".local/share/PrismLauncher/instances")
                                .resolve(instanceName).resolve("minecraft/saves");
                        if (Files.exists(instancePathMinecraft)) {
                            Files.walk(instancePathMinecraft)
                                    .sorted((a, b) -> b.compareTo(a))
                                    .forEach(path -> {
                                        try {
                                            Files.delete(path);
                                        } catch (IOException ex) {
                                        }
                                    });
                        }

                        Path instancePathMinecraftDot = Path.of(userHome)
                                .resolve(".local/share/PrismLauncher/instances")
                                .resolve(instanceName).resolve(".minecraft/saves");
                        if (Files.exists(instancePathMinecraftDot)) {
                            Files.walk(instancePathMinecraftDot)
                                    .sorted((a, b) -> b.compareTo(a))
                                    .forEach(path -> {
                                        try {
                                            Files.delete(path);
                                        } catch (IOException ex) {
                                        }
                                    });
                        }

                        Path symlinkTarget = instancePathMinecraft.toAbsolutePath();
                        if (!Files.exists(symlinkTarget)) {
                            symlinkTarget = instancePathMinecraftDot.toAbsolutePath();
                        }

                        Files.createSymbolicLink(
                                lingleDir.resolve("saves"),
                                symlinkTarget
                        );
                    }
                } catch (IOException | InterruptedException ex) {
                    JOptionPane.showMessageDialog(null, "Failed to create symbolic links: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(null, "Please select at least one instance.",
                        "No Selection", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        JPanel bottomSection = new JPanel();
        bottomSection.setLayout(new BorderLayout());
        bottomSection.add(instancesSection, BorderLayout.CENTER);
        bottomSection.add(symlinkButton, BorderLayout.SOUTH);

        tmpfsPanel.add(buttonSection, BorderLayout.NORTH);
        tmpfsPanel.add(bottomSection, BorderLayout.CENTER);

        javax.swing.JPanel settingsPanel = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER));
        settingsPanel.setBackground(new Color(64, 64, 64));

        javax.swing.JLabel emptyLabel = new javax.swing.JLabel("nothing here yet");
        emptyLabel.setForeground(Color.LIGHT_GRAY);
        emptyLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));
        settingsPanel.add(emptyLabel);

        contentPanel.add(tmpfsPanel, "auToMPFS");
        contentPanel.add(settingsPanel, " ");

        tmpfsNavButton.addActionListener(e -> cardLayout.show(contentPanel, "auToMPFS"));
        settingsNavButton.addActionListener(e -> cardLayout.show(contentPanel, " "));

        cardLayout.show(contentPanel, "auToMPFS");

        javax.swing.JPanel navAndContentPanel = new javax.swing.JPanel(new BorderLayout());
        navAndContentPanel.add(navPanel, BorderLayout.NORTH);
        navAndContentPanel.add(contentPanel, BorderLayout.CENTER);

        javax.swing.JPanel mainPanel = new javax.swing.JPanel(new BorderLayout());
        mainPanel.add(titleBar, BorderLayout.NORTH);
        mainPanel.add(navAndContentPanel, BorderLayout.CENTER);
        mainPanel.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100), 1));

        frame.getContentPane().add(mainPanel);
        frame.setSize(500, 300);
        frame.setLocationRelativeTo(null);

        frame.setVisible(true);
        frame.toFront();
        frame.requestFocus();
    }

    private static String buttonText() {
        return enabled ? "Disable" : "Enable";
    }

    private static void runToggleScriptAsync() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastClickTime < 3000) {
            return;
        }
        lastClickTime = currentTime;

        runButton.setEnabled(false);
        final boolean runDisable = enabled;

        new Thread(() -> {
            int exitCode = -1;
            try {
                ensureScriptsPresent();

                Path home = Path.of(System.getProperty("user.home"));
                Path scriptsDir = home.resolve(".local").resolve("share").resolve("lingle").resolve("scripts");
                Path scriptToRun = runDisable ?
                        scriptsDir.resolve("tmpfsdisable.sh") :
                        scriptsDir.resolve("tmpfsenable.sh");

                if (!Files.exists(scriptToRun)) {
                    throw new IOException("Script not found: " + scriptToRun);
                }

                ProcessBuilder pb = new ProcessBuilder("sudo", "/bin/bash", scriptToRun.toString());
                pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
                pb.redirectError(ProcessBuilder.Redirect.DISCARD);

                Process p = pb.start();
                exitCode = p.waitFor();

            } catch (IOException | InterruptedException ex) {
                exitCode = 1;
                Thread.currentThread().interrupt();
            }

            int finalExitCode = exitCode;
            SwingUtilities.invokeLater(() -> {
                if (finalExitCode == 0) {
                    enabled = !runDisable;
                    runButton.setText(buttonText());
                    saveCurrentState();
                } else {
                    JOptionPane.showMessageDialog(
                            null,
                            "Failed to execute task. Exit code: " + finalExitCode,
                            "Lingle",
                            JOptionPane.WARNING_MESSAGE
                    );
                }
                runButton.setEnabled(true);
            });
        }, "script-launcher").start();
    }

    private static String enableScriptContent() {
        return """
#!/bin/bash
set -euo pipefail

USER_NAME="$(logname 2>/dev/null || id -un)"
USER_HOME="$(getent passwd "${USER_NAME}" | cut -d: -f6)"
TARGET="${USER_HOME}/Lingle"
SIZE="4g"

COMMENT="# LINGLE tmpfs"
LINE="tmpfs ${TARGET} tmpfs defaults,size=${SIZE},mode=0700 0 0"

mkdir -p "${TARGET}"
chmod 0700 "${TARGET}"

if ! grep -qF "${LINE}" /etc/fstab; then
  echo "${COMMENT}" >> /etc/fstab
  echo "${LINE}" >> /etc/fstab
fi

if ! /usr/bin/mountpoint -q "${TARGET}"; then
  mount "${TARGET}"
fi

exit 0
""";
    }

    private static String disableScriptContent() {
        return """
#!/bin/bash
set -euo pipefail

USER_NAME="$(logname 2>/dev/null || id -un)"
USER_HOME="$(getent passwd "${USER_NAME}" | cut -d: -f6)"
TARGET="${USER_HOME}/Lingle"

COMMENT="# LINGLE tmpfs"
LINE="tmpfs ${TARGET} tmpfs defaults,size=4g,mode=0700 0 0"

if /usr/bin/mountpoint -q "${TARGET}"; then
  umount "${TARGET}"
fi

if grep -qF "${LINE}" /etc/fstab; then
  grep -v "${COMMENT}" /etc/fstab | grep -v "${LINE}" > /tmp/fstab.tmp
  mv /tmp/fstab.tmp /etc/fstab
fi

if [ -d "${TARGET}" ]; then
  chmod 000 "${TARGET}" 2>/dev/null || true
fi

exit 0
""";
    }

    private static void ensureScriptsPresent() throws IOException {
        Path home = Path.of(System.getProperty("user.home"));
        Path scriptsDir = home.resolve(".local").resolve("share").resolve("lingle").resolve("scripts");
        Path savesDir = home.resolve(".local").resolve("share").resolve("lingle").resolve("saves");
        Path enableSh = scriptsDir.resolve("tmpfsenable.sh");
        Path disableSh = scriptsDir.resolve("tmpfsdisable.sh");

        try {
            if (!Files.exists(scriptsDir)) {
                Files.createDirectories(scriptsDir);
            }

            if (!Files.exists(savesDir)) {
                Files.createDirectories(savesDir);
            }

            Files.writeString(enableSh, enableScriptContent(), StandardCharsets.UTF_8);
            enableSh.toFile().setExecutable(true, true);

            Files.writeString(disableSh, disableScriptContent(), StandardCharsets.UTF_8);
            disableSh.toFile().setExecutable(true, true);

        } catch (IOException e) {
            if (!Files.exists(home)) {
                throw new IOException("Home directory does not exist: " + home, e);
            }
            if (!Files.isWritable(home)) {
                throw new IOException("Cannot write to home directory: " + home, e);
            }

            throw new IOException("Failed to create scripts: " + e.getMessage(), e);
        }
    }

    private static void detectCurrentState() {
        Path home = Path.of(System.getProperty("user.home"));
        Path configDir = home.resolve(".local").resolve("share").resolve("lingle");
        Path configFile = configDir.resolve("config.json");

        try {
            if (Files.exists(configFile)) {
                String content = Files.readString(configFile).trim();
                enabled = content.contains("\"tmpfs\":\"enabled\"") || content.contains("\"tmpfs\": \"enabled\"");
            } else {
                enabled = false;
            }
        } catch (IOException e) {
            enabled = false;
        }
    }

    private static void saveCurrentState() {
        try {
            Path home = Path.of(System.getProperty("user.home"));
            Path configDir = home.resolve(".local").resolve("share").resolve("lingle");
            Path configFile = configDir.resolve("config.json");

            Files.createDirectories(configDir);

            String jsonContent = "{\n  \"tmpfs\": \"" + (enabled ? "enabled" : "disabled") + "\"\n}";

            Files.writeString(configFile, jsonContent);
        } catch (IOException e) {
        }
    }
}
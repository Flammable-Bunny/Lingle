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
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component;
import javax.swing.JCheckBox;
import javax.swing.*;
import java.awt.*;

public class Main {

    private static volatile boolean enabled = false;
    private static JButton runButton;
    private static long lastClickTime = 0;

    static void main() {
        if (System.getenv("DISPLAY") == null && System.getenv("WAYLAND_DISPLAY") == null) {
            System.err.println("No DISPLAY/WAYLAND_DISPLAY found. This GUI requires a graphical session.");
            System.exit(1);
        }

        try {
            ensureScriptsPresent();
            detectCurrentState();
            createInitialDirectories();
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

    private static void createInitialDirectories() throws IOException {
        Path home = Path.of(System.getProperty("user.home"));
        Path targetDir = home.resolve("Lingle");

        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "sudo", "-n",
                    "chown", System.getProperty("user.name") + ":" + System.getProperty("user.name"),
                    targetDir.toString()
            );
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("Failed to set ownership on directory: " + targetDir);
            }

            pb = new ProcessBuilder(
                    "sudo", "-n",
                    "chmod", "700", targetDir.toString()
            );
            process = pb.start();
            exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("Failed to set permissions on directory: " + targetDir);
            }
        } catch (InterruptedException e) {
            throw new IOException("Process interrupted while setting permissions", e);
        }
    }

    private static void createAndShowUI() {
        JFrame frame = new JFrame("Lingle");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.setAlwaysOnTop(true);
        frame.setType(java.awt.Window.Type.UTILITY);
        frame.setResizable(false);
        frame.setUndecorated(true);

        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(new Color(45, 45, 45));
        titleBar.setPreferredSize(new Dimension(0, 25));

        JLabel titleLabel = new JLabel("Lingle");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 5));

        JButton closeButton = new JButton("×");
        closeButton.setBackground(new Color(45, 45, 45));
        closeButton.setForeground(Color.WHITE);
        closeButton.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        closeButton.setFocusPainted(false);
        closeButton.addActionListener(_ -> System.exit(0));

        titleBar.add(titleLabel, BorderLayout.WEST);
        titleBar.add(closeButton, BorderLayout.EAST);

        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        navPanel.setBackground(new Color(64, 64, 64));
        navPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(100, 100, 100)));

        JButton tmpfsNavButton = new JButton("auToMPFS");
        JButton settingsNavButton = new JButton(" ");

        // Style nav buttons consistently
        JButton[] navButtons = {tmpfsNavButton, settingsNavButton};
        for (JButton btn : navButtons) {
            btn.setBackground(new Color(80, 80, 80));
            btn.setForeground(Color.WHITE);
            btn.setBorder(BorderFactory.createLineBorder(new Color(150, 150, 150), 1));
            btn.setFocusPainted(false);
            btn.setFont(new Font("SansSerif", Font.PLAIN, 12));
            btn.setPreferredSize(new Dimension(80, 35)); // consistent height
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

        CardLayout cardLayout = new CardLayout();
        JPanel contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(new Color(64, 64, 64));

        runButton = new JButton(buttonText());
        runButton.addActionListener(_ -> runToggleScriptAsync());
        compressed(runButton);
        runButton.setPreferredSize(new Dimension(100, 35)); // consistent height

        JPanel tmpfsPanel = new JPanel(new BorderLayout());
        tmpfsPanel.setBackground(new Color(64, 64, 64));

        JPanel buttonSection = new JPanel(null);
        buttonSection.setBackground(new Color(64, 64, 64));
        buttonSection.setPreferredSize(new Dimension(0, 80));

        JLabel tmpfsLabel = new JLabel("Enable TMPFS");
        tmpfsLabel.setForeground(Color.WHITE);
        tmpfsLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        tmpfsLabel.setBounds(10, 10, 150, 25);
        buttonSection.add(tmpfsLabel);

        runButton.setBounds(10, 35, 100, 35);
        buttonSection.add(runButton);

        // --- Instances Section ---
        JPanel checkboxPanel = new JPanel();
        checkboxPanel.setLayout(new BoxLayout(checkboxPanel, BoxLayout.Y_AXIS));
        checkboxPanel.setBackground(new Color(64, 64, 64));

        Path home = Path.of(System.getProperty("user.home"));
        Path prismInstancesDir = home.resolve(".local").resolve("share").resolve("PrismLauncher").resolve("instances");

        try {
            if (Files.exists(prismInstancesDir) && Files.isDirectory(prismInstancesDir)) Files.list(prismInstancesDir)
                    .filter(Files::isDirectory)
                    .sorted()
                    .forEach(instanceDir -> {
                        String instanceName = instanceDir.getFileName().toString();
                        JCheckBox checkbox = new JCheckBox(instanceName);
                        checkbox.setBackground(new Color(64, 64, 64));
                        checkbox.setForeground(Color.WHITE);
                        checkbox.setFont(new Font("SansSerif", Font.PLAIN, 12));
                        checkbox.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
                        checkboxPanel.add(checkbox);
                    });
            else {
                JLabel noInstancesLabel = new JLabel("No PrismLauncher instances found");
                noInstancesLabel.setForeground(Color.LIGHT_GRAY);
                noInstancesLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));
                checkboxPanel.add(noInstancesLabel);
            }
        } catch (IOException e) {
            JLabel errorLabel = new JLabel("Error reading instances directory");
            errorLabel.setForeground(Color.RED);
            errorLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));
            checkboxPanel.add(errorLabel);
        }

        JScrollPane scrollPane = new JScrollPane(checkboxPanel);
        scrollPane.setBackground(new Color(64, 64, 64));
        scrollPane.getViewport().setBackground(new Color(64, 64, 64));
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200)); // cap height for scrolling

        JButton symlinkButton = new JButton("Symlink Instances");
        compressed(symlinkButton);
        symlinkButton.setPreferredSize(new Dimension(150, 35)); // match Enable button height

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        buttonRow.setBackground(new Color(64, 64, 64));
        buttonRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonRow.add(symlinkButton);

        JLabel instancesLabel = new JLabel("PrismLauncher Instances:");
        instancesLabel.setForeground(Color.WHITE);
        instancesLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        instancesLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        instancesLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel instancesSection = new JPanel();
        instancesSection.setLayout(new BoxLayout(instancesSection, BoxLayout.Y_AXIS));
        instancesSection.setBackground(new Color(64, 64, 64));
        instancesSection.add(instancesLabel);
        instancesSection.add(scrollPane);
        instancesSection.add(buttonRow);

        JPanel instancesWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        instancesWrapper.setBackground(new Color(64, 64, 64));
        instancesWrapper.add(instancesSection);

        tmpfsPanel.add(buttonSection, BorderLayout.NORTH);
        tmpfsPanel.add(instancesWrapper, BorderLayout.CENTER);

        // --- Settings panel ---
        JPanel settingsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        settingsPanel.setBackground(new Color(64, 64, 64));
        JLabel emptyLabel = new JLabel("nothing here yet");
        emptyLabel.setForeground(Color.LIGHT_GRAY);
        settingsPanel.add(emptyLabel);

        contentPanel.add(tmpfsPanel, "auToMPFS");
        contentPanel.add(settingsPanel, " ");

        tmpfsNavButton.addActionListener(_ -> cardLayout.show(contentPanel, "auToMPFS"));
        settingsNavButton.addActionListener(_ -> cardLayout.show(contentPanel, " "));

        JPanel navAndContentPanel = new JPanel(new BorderLayout());
        navAndContentPanel.add(navPanel, BorderLayout.NORTH);
        navAndContentPanel.add(contentPanel, BorderLayout.CENTER);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(titleBar, BorderLayout.NORTH);
        mainPanel.add(navAndContentPanel, BorderLayout.CENTER);
        mainPanel.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100), 1));

        frame.getContentPane().add(mainPanel);
        frame.setSize(500, 600);
        frame.setLocationRelativeTo(null);

        frame.setVisible(true);
        frame.toFront();
        frame.requestFocus();
    }

    private static void compressed(JButton runButton) {
        runButton.setBackground(new Color(80, 80, 80));
        runButton.setForeground(Color.WHITE);
        runButton.setBorder(BorderFactory.createLineBorder(new Color(150, 150, 150), 2));
        runButton.setFocusPainted(false);
        runButton.setFont(new Font("SansSerif", Font.PLAIN, 14));
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
            int exitCode;
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
                    JOptionPane.showMessageDialog(null, "Failed to execute task. Exit code: " + finalExitCode);
                }
                runButton.setEnabled(true);
            });
        }).start();
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
        if (!Files.exists(scriptsDir)) Files.createDirectories(scriptsDir);
        Path enableSh = scriptsDir.resolve("tmpfsenable.sh");
        Path disableSh = scriptsDir.resolve("tmpfsdisable.sh");
        Files.writeString(enableSh, enableScriptContent(), StandardCharsets.UTF_8);
        enableSh.toFile().setExecutable(true, true);
        Files.writeString(disableSh, disableScriptContent(), StandardCharsets.UTF_8);
        disableSh.toFile().setExecutable(true, true);
    }

    private static void detectCurrentState() {
        enabled = false; // stub
    }

    private static void saveCurrentState() {
        // stub
    }
}

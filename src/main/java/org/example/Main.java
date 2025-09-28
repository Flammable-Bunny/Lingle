package org.example;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {

    private static volatile boolean enabled = false; // false -> next click runs enable; true -> next click runs disable
    private static JButton runButton;

    public static void main(String[] args) {
        if (System.getenv("DISPLAY") == null && System.getenv("WAYLAND_DISPLAY") == null) {
            System.err.println("No DISPLAY/WAYLAND_DISPLAY found. This GUI requires a graphical session.");
            System.exit(1);
        }

        try {
            ensureScriptsPresent();
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
        JFrame frame = new JFrame("Lingle");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        runButton = new JButton(buttonText());
        runButton.addActionListener(_ -> runToggleScriptAsync());

        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(runButton, BorderLayout.CENTER);
        frame.setMinimumSize(new Dimension(360, 120));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static String buttonText() {
        return enabled ? "Disable" : "Enable";
    }

    private static void runToggleScriptAsync() {
        runButton.setEnabled(false);
        final boolean runDisable = enabled; // snapshot current state

        new Thread(() -> {
            int exitCode = -1;
            try {
                Path home = Path.of(System.getProperty("user.home"));
                Path scriptsDir = home.resolve("Lingle").resolve("scripts");
                Path scriptToRun = runDisable ?
                    scriptsDir.resolve("tmpfsdisable.sh") :
                    scriptsDir.resolve("tmpfsenable.sh");

            if (!Files.exists(scriptToRun)) {
                throw new IOException("Script not found: " + scriptToRun);
            }

            // Run the script directly
            ProcessBuilder pb = new ProcessBuilder("/bin/bash", scriptToRun.toString());
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);

            Process p = pb.start();
            exitCode = p.waitFor();

        } catch (IOException | InterruptedException ex) {
            exitCode = 1;
            ex.printStackTrace();
            Thread.currentThread().interrupt();
        }

        int finalExitCode = exitCode;
        SwingUtilities.invokeLater(() -> {
            if (finalExitCode == 0) {
                enabled = !runDisable;
                runButton.setText(buttonText());
                JOptionPane.showMessageDialog(null, (runDisable ? "Disabled" : "Enabled") + " successfully.");
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

# Create the directory and set permissions (this should work without sudo)
mkdir -p "${TARGET}"
chmod 0700 "${TARGET}"

# Check if already mounted
if ! /usr/bin/mountpoint -q "${TARGET}"; then
  # Try to mount using user namespace if possible
  if mount -t tmpfs -o size=${SIZE},mode=0700 tmpfs "${TARGET}" 2>/dev/null; then
    echo "Mounted tmpfs at ${TARGET}"
  else
    echo "Note: Could not mount tmpfs. You may need to add this line to /etc/fstab manually:"
    echo "${LINE}"
    echo "Then run: sudo mount ${TARGET}"
  fi
fi
""";
}

private static String disableScriptContent() {
    return """
#!/bin/bash
set -euo pipefail

USER_NAME="$(logname 2>/dev/null || id -un)"
USER_HOME="$(getent passwd "${USER_NAME}" | cut -d: -f6)"
TARGET="${USER_HOME}/Lingle"
SIZE="4g"

COMMENT="# LINGLE tmpfs"
LINE="tmpfs ${TARGET} tmpfs defaults,size=${SIZE},mode=0700 0 0"

# Check if mounted and try to unmount
if /usr/bin/mountpoint -q "${TARGET}"; then
  if umount "${TARGET}" 2>/dev/null; then
    echo "Unmounted tmpfs from ${TARGET}"
  else
    echo "Note: Could not unmount tmpfs. You may need to run: sudo umount ${TARGET}"
  fi
fi

# Set restrictive permissions on the directory
if [ -d "${TARGET}" ]; then
  chmod 000 "${TARGET}" 2>/dev/null || echo "Could not change directory permissions"
fi
""";
}

    private static void ensureScriptsPresent() throws IOException {
        Path home = Path.of(System.getProperty("user.home"));
        Path lingleDir = home.resolve("Lingle");
        Path scriptsDir = lingleDir.resolve("scripts");
        Path enableSh = scriptsDir.resolve("tmpfsenable.sh");
        Path disableSh = scriptsDir.resolve("tmpfsdisable.sh");

        try {
            // Create directories if missing - handle all cases gracefully
            if (!Files.exists(lingleDir)) {
                Files.createDirectories(lingleDir);
            }
        
            if (!Files.exists(scriptsDir)) {
                Files.createDirectories(scriptsDir);
            }

            // Only create files if they don't exist
            if (!Files.exists(enableSh)) {
                Files.writeString(enableSh, enableScriptContent(), StandardCharsets.UTF_8);
                enableSh.toFile().setExecutable(true, true);
            }

            if (!Files.exists(disableSh)) {
                Files.writeString(disableSh, disableScriptContent(), StandardCharsets.UTF_8);
                disableSh.toFile().setExecutable(true, true);
            }
        
    } catch (IOException e) {
        // Check what specifically failed
        if (!Files.exists(home)) {
            throw new IOException("Home directory does not exist: " + home, e);
        }
        if (!Files.isWritable(home)) {
            throw new IOException("Cannot write to home directory: " + home, e);
        }
        if (Files.exists(lingleDir) && !Files.isDirectory(lingleDir)) {
            throw new IOException("Lingle path exists but is not a directory: " + lingleDir, e);
        }
        if (Files.exists(scriptsDir) && !Files.isDirectory(scriptsDir)) {
            throw new IOException("Scripts path exists but is not a directory: " + scriptsDir, e);
        }
        
        throw new IOException("Failed to create scripts: " + e.getMessage(), e);
    }
}

    private static void setupCapabilityWrapper() throws IOException {
        Path home = Path.of(System.getProperty("user.home"));
        Path binDir = home.resolve(".local/bin");
        Path wrapperScript = binDir.resolve("run-as-capabilities");

        // Create .local/bin directory if missing
        try { Files.createDirectories(binDir); } catch (FileAlreadyExistsException ignore) {}

        // Create and make executable the capability wrapper script
        String wrapperContent = """
#!/bin/bash
set -euo pipefail

# Add necessary capabilities for Lingle operations to execute file
CAPABILITIES="cap_dac_override,cap_fowner,cap_sys_admin=+ep"
setcap "$CAPABILITIES" "$1"

# Execute the script with elevated privileges
"$@"
""";

        try {
            if (!Files.exists(wrapperScript)) {
                Files.writeString(wrapperScript, wrapperContent, StandardCharsets.UTF_8);
            }

            // Ensure it's executable
            wrapperScript.toFile().setExecutable(true);

            // Add .local/bin to PATH environment variable if not already present
            String currentPath = System.getenv("PATH");
            if (!currentPath.contains(binDir.toString())) {
                System.setProperty("PATH", binDir + ":" + currentPath);
            }
        } catch (IOException e) {
            throw new IOException("Failed to set up capability wrapper script", e);
        }
    }
}
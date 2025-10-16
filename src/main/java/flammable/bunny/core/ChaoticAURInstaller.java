package flammable.bunny.core;

import java.io.*;
import java.nio.file.*;
import java.util.List;

public class ChaoticAURInstaller {


    public static void installOBS() throws IOException, InterruptedException {
        System.out.println("Setting up Chaotic-AUR for Arch Linux...");

        // Add Chaotic-AUR GPG keys
        runCommand("sudo pacman-key --recv-key 3056513887B78AEB --keyserver keyserver.ubuntu.com");
        runCommand("sudo pacman-key --lsign-key 3056513887B78AEB");

        // Install Chaotic-AUR keyrings and mirrorlist
        runCommand("sudo pacman -U --noconfirm 'https://cdn-mirror.chaotic.cx/chaotic-aur/chaotic-keyring.pkg.tar.zst'");
        runCommand("sudo pacman -U --noconfirm 'https://cdn-mirror.chaotic.cx/chaotic-aur/chaotic-mirrorlist.pkg.tar.zst'");

        // Add Chaotic-AUR repository to pacman.conf
        addChaoticAURToPacmanConf();

        // Refresh package databases
        runCommand("sudo pacman -Syu --noconfirm");

        // Install OBS Studio from Chaotic-AUR
        runCommand("sudo pacman -S --noconfirm chaotic-aur/obs-studio-stable");

        // Install OBS PipeWire audio capture plugin
        installOBSPipeWirePlugin();

        System.out.println("OBS Studio installed successfully from Chaotic-AUR!");
    }

    /**
     * Adds Chaotic-AUR repository to /etc/pacman.conf under multilib section
     */
    private static void addChaoticAURToPacmanConf() throws IOException, InterruptedException {
        Path pacmanConf = Path.of("/etc/pacman.conf");

        if (!Files.exists(pacmanConf)) {
            throw new IOException("pacman.conf not found at /etc/pacman.conf");
        }

        List<String> lines = Files.readAllLines(pacmanConf);
        boolean chaoticExists = false;

        // Check if Chaotic-AUR already exists
        for (String line : lines) {
            if (line.trim().equals("[chaotic-aur]")) {
                chaoticExists = true;
                System.out.println("Chaotic-AUR already exists in pacman.conf");
                return;
            }
        }

        if (!chaoticExists) {
            // Find multilib section and add Chaotic-AUR after it
            StringBuilder newContent = new StringBuilder();
            boolean foundMultilib = false;
            boolean addedChaotic = false;

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                newContent.append(line).append("\n");

                if (line.trim().equals("[multilib]")) {
                    foundMultilib = true;
                }

                if (foundMultilib && !addedChaotic) {
                    if (i + 1 < lines.size() && lines.get(i + 1).trim().startsWith("[") && !lines.get(i + 1).trim().equals("[multilib]")) {
                        newContent.append("\n[chaotic-aur]\n");
                        newContent.append("Include = /etc/pacman.d/chaotic-mirrorlist\n");
                        addedChaotic = true;
                    }
                }
            }

            // If we didn't find a good spot, add at the end
            if (!addedChaotic) {
                newContent.append("\n[chaotic-aur]\n");
                newContent.append("Include = /etc/pacman.d/chaotic-mirrorlist\n");
            }

            // Write to temp file first, then use sudo to copy
            Path tempFile = Files.createTempFile("pacman", ".conf");
            Files.writeString(tempFile, newContent.toString());

            ProcessBuilder pb = new ProcessBuilder("sudo", "cp", tempFile.toString(), "/etc/pacman.conf");
            pb.inheritIO();
            int exitCode = pb.start().waitFor();

            Files.deleteIfExists(tempFile);

            if (exitCode != 0) {
                throw new IOException("Failed to update pacman.conf with exit code: " + exitCode);
            }

            System.out.println("Added Chaotic-AUR to pacman.conf");
        }
    }

    private static void installOBSPipeWirePlugin() {
        try {
            // Try yay first
            System.out.println("Attempting to install obs-pipewire-audio-capture-git with yay...");
            ProcessBuilder pb = new ProcessBuilder("yay", "-S", "--noconfirm", "obs-pipewire-audio-capture-git");
            pb.inheritIO();
            int exitCode = pb.start().waitFor();

            if (exitCode == 0) {
                System.out.println("OBS PipeWire plugin installed successfully with yay!");
                return;
            }
        } catch (Exception e) {
            System.out.println("yay failed or not found, trying paru...");
        }

        try {
            // Try paru if yay failed
            System.out.println("Attempting to install obs-pipewire-audio-capture-git with paru...");
            ProcessBuilder pb = new ProcessBuilder("paru", "-S", "--noconfirm", "obs-pipewire-audio-capture-git");
            pb.inheritIO();
            int exitCode = pb.start().waitFor();

            if (exitCode == 0) {
                System.out.println("OBS PipeWire plugin installed successfully with paru!");
                return;
            }
        } catch (Exception e) {
            System.out.println("Warning: Could not install obs-pipewire-audio-capture-git. Neither yay nor paru succeeded.");
        }
    }

    private static void runCommand(String command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
        pb.inheritIO();
        int exitCode = pb.start().waitFor();
        if (exitCode != 0) {
            throw new IOException("Command failed with exit code " + exitCode + ": " + command);
        }
    }
}

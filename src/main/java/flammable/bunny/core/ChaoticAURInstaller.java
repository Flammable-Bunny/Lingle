package flammable.bunny.core;

import java.io.*;
import java.nio.file.*;
import java.util.List;

public class ChaoticAURInstaller {


    public static void installOBS() throws IOException, InterruptedException {
        System.out.println("Setting up Chaotic-AUR for Arch Linux...");

        ProcessBuilder checkObs = new ProcessBuilder("pacman", "-Q", "obs-studio-stable");
        checkObs.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        checkObs.redirectError(ProcessBuilder.Redirect.DISCARD);
        if (checkObs.start().waitFor() == 0) {
            System.out.println("OBS Studio already installed, skipping...");
            return;
        }

        ProcessBuilder checkKey = new ProcessBuilder("pacman-key", "-l");
        Process keyProcess = checkKey.start();
        String keyOutput = new String(keyProcess.getInputStream().readAllBytes());
        keyProcess.waitFor();
        
        if (!keyOutput.contains("3056513887B78AEB")) {
            runCommand("pacman-key --recv-key 3056513887B78AEB --keyserver keyserver.ubuntu.com");
            runCommand("pacman-key --lsign-key 3056513887B78AEB");
        } else {
            System.out.println("Chaotic-AUR GPG key already exists, skipping...");
        }

        runCommand("pacman -U --noconfirm --needed 'https://cdn-mirror.chaotic.cx/chaotic-aur/chaotic-keyring.pkg.tar.zst'");
        runCommand("pacman -U --noconfirm --needed 'https://cdn-mirror.chaotic.cx/chaotic-aur/chaotic-mirrorlist.pkg.tar.zst'");

        addChaoticAURToPacmanConf();

        runCommand("pacman -Sy");

        runCommand("pacman -S --noconfirm chaotic-aur/obs-studio-stable");

        System.out.println("OBS Studio installed successfully from Chaotic-AUR!");
    }


    private static void addChaoticAURToPacmanConf() throws IOException, InterruptedException {
        Path pacmanConf = Path.of("/etc/pacman.conf");

        if (!Files.exists(pacmanConf)) {
            throw new IOException("pacman.conf not found at /etc/pacman.conf");
        }

        List<String> lines = Files.readAllLines(pacmanConf);
        boolean chaoticExists = false;

        for (String line : lines) {
            if (line.trim().equals("[chaotic-aur]")) {
                chaoticExists = true;
                System.out.println("Chaotic-AUR already exists in pacman.conf");
                return;
            }
        }

        if (!chaoticExists) {
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

            if (!addedChaotic) {
                newContent.append("\n[chaotic-aur]\n");
                newContent.append("Include = /etc/pacman.d/chaotic-mirrorlist\n");
            }

            Path tempFile = Files.createTempFile("pacman", ".conf");
            Files.writeString(tempFile, newContent.toString());

            int exitCode = ElevatedInstaller.runElevatedBash("cp '" + tempFile.toString() + "' '/etc/pacman.conf'");

            Files.deleteIfExists(tempFile);

            if (exitCode != 0) {
                throw new IOException("Failed to update pacman.conf with exit code: " + exitCode);
            }

            System.out.println("Added Chaotic-AUR to pacman.conf");
        }
    }



    private static void runCommand(String command) throws IOException, InterruptedException {
        int exitCode = ElevatedInstaller.runElevatedBash(command);
        if (exitCode != 0) {
            throw new IOException("Command failed with exit code " + exitCode + ": " + command);
        }
    }
}

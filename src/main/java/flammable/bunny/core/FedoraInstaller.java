package flammable.bunny.core;

import java.io.IOException;

public class FedoraInstaller {

    public static void installWaywallDependencies() throws IOException, InterruptedException {
        // Step 1: Upgrade system
        runCommand("sudo dnf upgrade -y");

        // Step 2: Install RPM Fusion repositories
        String rpmFusionCmd = "sudo dnf install -y " +
            "https://mirrors.rpmfusion.org/free/fedora/rpmfusion-free-release-$(rpm -E %fedora).noarch.rpm " +
            "https://mirrors.rpmfusion.org/nonfree/fedora/rpmfusion-nonfree-release-$(rpm -E %fedora).noarch.rpm";
        runCommand(rpmFusionCmd);

        // Step 3: Install dependencies stuff
        String depsCmd = "sudo dnf install -y " +
            "xorg-x11-server-Xorg plasma-workspace-x11 egl-wayland " +
            "mesa-libEGL mesa-libGLES luajit libspng " +
            "libwayland-client libwayland-server libwayland-cursor libwayland-egl " +
            "libxcb libxkbcommon";
        runCommand(depsCmd);

        // Step 4: Install NVIDIA drivers if needed
        String gpu = DistroDetector.getGPU();
        if ("nvidia".equals(gpu)) {
            runCommand("sudo dnf install -y akmod-nvidia");
        }
    }


    public static void installPrismLauncher() throws IOException, InterruptedException {
        // Enable COPR repository
        runCommand("sudo dnf copr enable -y g3tchoo/prismlauncher");

        // Install Prism Launcher and Java 21 (Prism comes with Java 21, but we install it manually too for redundancy)
        runCommand("sudo dnf install -y prismlauncher java-21-openjdk");
    }

    private static void runCommand(String command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
        pb.inheritIO();
        int exitCode = pb.start().waitFor();
        if (exitCode != 0) {
            throw new IOException("Command failed with exit code: " + exitCode);
        }
    }
}

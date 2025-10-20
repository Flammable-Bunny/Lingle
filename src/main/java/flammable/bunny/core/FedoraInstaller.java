package flammable.bunny.core;

import java.io.IOException;

public class FedoraInstaller {

    public static void installWaywallDependencies() throws IOException, InterruptedException {
        runCommand("dnf upgrade -y");

        String rpmFusionCmd = "dnf install -y " +
            "https://mirrors.rpmfusion.org/free/fedora/rpmfusion-free-release-$(rpm -E %fedora).noarch.rpm " +
            "https://mirrors.rpmfusion.org/nonfree/fedora/rpmfusion-nonfree-release-$(rpm -E %fedora).noarch.rpm";
        runCommand(rpmFusionCmd);

        String depsCmd = "dnf install -y " +
            "xorg-x11-server-Xorg plasma-workspace-x11 egl-wayland " +
            "mesa-libEGL mesa-libGLES luajit libspng " +
            "libwayland-client libwayland-server libwayland-cursor libwayland-egl " +
            "libxcb libxkbcommon";
        runCommand(depsCmd);

        String gpu = DistroDetector.getGPU();
        if ("nvidia".equals(gpu)) {
            runCommand("dnf install -y akmod-nvidia");
        }
    }


    public static void installPrismLauncher() throws IOException, InterruptedException {
        runCommand("dnf copr enable -y g3tchoo/prismlauncher");
        runCommand("dnf install -y prismlauncher java-21-openjdk");
    }

    private static void runCommand(String command) throws IOException, InterruptedException {
        int exitCode = ElevatedInstaller.runElevatedBash(command);
        if (exitCode != 0) {
            throw new IOException("Command failed with exit code: " + exitCode);
        }
    }
}

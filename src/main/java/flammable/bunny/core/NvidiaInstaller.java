package flammable.bunny.core;

import java.io.IOException;

public class NvidiaInstaller {

    public static void installNvidiaDependencies(String pkgManager) throws IOException, InterruptedException {
        String gpu = DistroDetector.getGPU();
        if (gpu == null || !gpu.toLowerCase().contains("nvidia")) {
            throw new IOException("NVIDIA GPU not detected. This feature is only for NVIDIA users.");
        }

        String installCmd = switch (pkgManager) {
            case "pacman" -> "pacman -S --noconfirm nvidia-prime";
            case "dnf" -> "dnf install -y akmod-nvidia xorg-x11-drv-nvidia-cuda";
            case "apt" -> "apt update && apt install -y nvidia-prime";
            default -> throw new IOException("Unsupported package manager for NVIDIA dependencies");
        };

        int exitCode = ElevatedInstaller.runElevatedBash(installCmd);
        if (exitCode != 0) {
            throw new IOException("Failed to install NVIDIA dependencies");
        }
    }
}

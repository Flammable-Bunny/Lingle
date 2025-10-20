package flammable.bunny.core;

import java.io.IOException;
import java.util.List;

public final class JemallocInstaller {

    private JemallocInstaller() {}

    public static void installJemalloc() throws IOException, InterruptedException {
        String pkgManager = DistroDetector.getPackageManager();
        if (pkgManager == null) throw new IOException("Could not detect package manager");

        String cmd = switch (pkgManager) {
            case "pacman" -> "sudo pacman -S --noconfirm jemalloc";
            case "apt" -> "sudo apt update && sudo apt install -y libjemalloc2";
            case "dnf" -> "sudo dnf install -y jemalloc";
            case "zypper" -> "sudo zypper install -y jemalloc2";
            case "apk" -> "sudo apk add jemalloc";
            case "nix" -> "nix-env -iA nixpkgs.jemalloc";
            case "xbps" -> "sudo xbps-install -Sy jemalloc";
            case "emerge" -> "sudo emerge -av jemalloc";
            default -> throw new IOException("Unsupported package manager: " + pkgManager);
        };

        if (ElevatedInstaller.runElevatedBash(cmd) != 0)
            throw new IOException("Jemalloc installation failed");
    }
}

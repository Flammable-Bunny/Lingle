package flammable.bunny.core;

import java.io.IOException;
import java.util.List;

public final class JemallocInstaller {

    private JemallocInstaller() {}

    public static void installJemalloc() throws IOException, InterruptedException {
        String pkgManager = DistroDetector.getPackageManager();
        if (pkgManager == null) throw new IOException("Could not detect package manager");

        String cmd = switch (pkgManager) {
            case "pacman" -> "pacman -S --noconfirm jemalloc";
            case "apt" -> "apt update && apt install -y libjemalloc2";
            case "dnf" -> "dnf install -y jemalloc";
            case "zypper" -> "zypper install -y jemalloc";
            case "apk" -> "apk add jemalloc";
            case "xbps" -> "xbps-install -Sy jemalloc";
            case "emerge" -> "emerge jemalloc";
            default -> throw new IOException("Unsupported package manager: " + pkgManager);
        };

        if (ElevatedInstaller.runElevatedBash(cmd) != 0)
            throw new IOException("Jemalloc installation failed");
    }
}

package flammable.bunny.core;

import org.json.JSONObject;
import java.io.*;
import java.nio.file.*;
import java.util.List;

public final class DistroDetector {

    private static final Path CONFIG_PATH = Path.of(System.getProperty("user.home"))
            .resolve(".local/share/lingle/config.json");

    private DistroDetector() {}

    public static void detectAndSaveDistro() {
        String distro = detectDistro();
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            JSONObject cfg = new JSONObject();
            if (Files.exists(CONFIG_PATH)) {
                try (BufferedReader r = Files.newBufferedReader(CONFIG_PATH)) {
                    cfg = new JSONObject(r.lines().reduce("", (a, b) -> a + b));
                } catch (Exception ignored) {}
            }
            cfg.put("distro", distro);
            try (BufferedWriter w = Files.newBufferedWriter(CONFIG_PATH)) {
                w.write(cfg.toString(2));
            }
        } catch (IOException ignored) {}
    }

    private static String detectDistro() {
        try {
            List<String> lines = Files.readAllLines(Path.of("/etc/os-release"));
            for (String line : lines) {
                if (line.startsWith("ID=")) {
                    return line.substring(3).replace("\"", "");
                }
            }
        } catch (IOException ignored) {}
        return "unknown";
    }
}

package flammable.bunny.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class PrismConfigEditor {

    public static void configure(String pkgManager) throws Exception {
        String home = System.getProperty("user.home");
        Path configFile = Path.of(home, ".local/share/PrismLauncher/instances/instance/instance.cfg");
        Files.createDirectories(configFile.getParent());

        String javaPath = "dnf".equals(pkgManager)
                ? "/usr/lib/jvm/java-21-openjdk/bin/java"
                : "/usr/lib/jvm/java-17-openjdk/bin/java";

        List<String> lines = Files.exists(configFile)
                ? Files.readAllLines(configFile)
                : new ArrayList<>();

        Map<String, String> settings = Map.ofEntries(
                Map.entry("IgnoreJavaCompatibility", "true"),
                Map.entry("AutomaticJavaDownload", "false"),
                Map.entry("JavaDir", "java"),
                Map.entry("JavaPath", javaPath),
                Map.entry("CustomGLFWPath", "/usr/local/lib64/waywall-glfw/libglfw.so"),
                Map.entry("UseNativeGLFW", "true"),
                Map.entry("WrapperCommand", "waywall wrap --")
        );

        for (var entry : settings.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            boolean found = false;

            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).startsWith(key + "=")) {
                    lines.set(i, key + "=" + value);
                    found = true;
                    break;
                }
            }

            if (!found) lines.add(key + "=" + value);
        }

        Files.write(configFile, lines);
    }
}

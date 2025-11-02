package flammable.bunny.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class PrismConfigEditor {

    public static void configureInstances(List<String> instanceNames, String pkgManager, boolean hasNvidiaGPU) throws Exception {
        String home = System.getProperty("user.home");
        Path instancesDir = Path.of(home, ".local/share/PrismLauncher/instances");

        String javaPath = "dnf".equals(pkgManager)
                ? "/usr/lib/jvm/java-21-openjdk/bin/java"
                : "/usr/lib/jvm/java-17-openjdk/bin/java";

        int configuredCount = 0;
        List<String> skippedInstances = new ArrayList<>();

        for (String instanceName : instanceNames) {
            Path instanceDir = instancesDir.resolve(instanceName);
            Path configFile = instanceDir.resolve("instance.cfg");

            // Skip if instance directory doesn't exist
            if (!Files.exists(instanceDir) || !Files.isDirectory(instanceDir)) {
                skippedInstances.add(instanceName + " (directory not found)");
                continue;
            }

            // Read existing config or start with empty list
            List<String> lines = Files.exists(configFile)
                    ? new ArrayList<>(Files.readAllLines(configFile))
                    : new ArrayList<>();

            // Define settings to apply
            Map<String, String> settings = new LinkedHashMap<>();
            settings.put("IgnoreJavaCompatibility", "true");
            settings.put("AutomaticJavaDownload", "false");
            settings.put("JavaDir", "java");
            settings.put("JavaPath", javaPath);
            settings.put("CustomGLFWPath", "/usr/local/lib64/waywall-glfw/libglfw.so");
            settings.put("UseNativeGLFW", "true");
            settings.put("WrapperCommand", "waywall wrap --");

            // Add NVIDIA-specific settings if NVIDIA GPU is detected
            if (hasNvidiaGPU) {
                settings.put("OverrideEnv", "true");
                settings.put("Env", "@Variant(\\0\\0\\0\\b\\0\\0\\0\\x1\\0\\0\\0\\x36\\0_\\0_\\0G\\0L\\0_\\0T\\0H\\0R\\0E\\0\\x41\\0D\\0E\\0D\\0_\\0O\\0P\\0T\\0I\\0M\\0I\\0Z\\0\\x41\\0T\\0I\\0O\\0N\\0S\\0\\0\\0\\n\\0\\0\\0\\x2\\0\\x30)");
            }

            // Update or add each setting - FORCE OVERWRITE
            for (Map.Entry<String, String> entry : settings.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                boolean found = false;

                // Try to find and replace existing key
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    if (line.startsWith(key + "=")) {
                        // FORCE overwrite the existing line
                        lines.set(i, key + "=" + value);
                        found = true;
                        break;
                    }
                }

                // If key wasn't found, add it
                if (!found) {
                    lines.add(key + "=" + value);
                }
            }

            // Write the config file - FORCE WRITE
            Files.write(configFile, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            configuredCount++;
        }

        // Throw exception if no instances were configured
        if (configuredCount == 0) {
            if (!skippedInstances.isEmpty()) {
                throw new Exception("No instances could be configured. Skipped: " + String.join(", ", skippedInstances));
            } else {
                throw new Exception("No instances could be configured.");
            }
        }
    }
}

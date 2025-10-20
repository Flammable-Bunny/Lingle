package flammable.bunny.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class DebounceInstaller {

    public static void installDebounceScript() throws IOException, InterruptedException {
        String script = "#!/bin/sh\n" +
                "mkdir -p /etc/libinput\n" +
                "tee /etc/libinput/local-overrides.quirks >/dev/null <<'ENDHERE'\n" +
                "[Never Debounce]\n" +
                "MatchUdevType=mouse\n" +
                "ModelBouncingKeys=1\n" +
                "ENDHERE";

        Path tmpScript = Files.createTempFile("debounce-setup", ".sh");
        Files.writeString(tmpScript, script, StandardCharsets.UTF_8);
        tmpScript.toFile().setExecutable(true);

        int exitCode = ElevatedInstaller.runElevatedBash("bash " + tmpScript);
        Files.deleteIfExists(tmpScript);

        if (exitCode != 0) {
            throw new IOException("Failed to install debounce configuration");
        }
    }
}

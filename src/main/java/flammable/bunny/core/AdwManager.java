package flammable.bunny.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class AdwManager {
    private static Process adwProcess = null;

    public static void startAdwIfNeeded() {
        if (!LingleState.adwEnabled || !LingleState.enabled) return;
        stopAdwQuietly();

        try {
            Path home = Path.of(System.getProperty("user.home"));
            Path scriptsDir = home.resolve(".local/share/lingle/scripts");
            Files.createDirectories(scriptsDir);

            Path script = scriptsDir.resolve("auto_delete_worlds_tmpfs.sh");
            Files.writeString(script, generateScript(home), StandardCharsets.UTF_8);
            script.toFile().setExecutable(true);

            adwProcess = new ProcessBuilder("/bin/bash", script.toString())
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
        } catch (IOException ignored) {
            adwProcess = null;
        }
    }

    private static String generateScript(Path home) {
        long pid = ProcessHandle.current().pid();
        int X = Math.max(0, LingleState.instanceCount);
        int S = Math.max(1, LingleState.adwIntervalSeconds);
        Path cfg = home.resolve(".local/share/lingle/config.json");

        return "#!/bin/bash\nset -euo pipefail\n" +
                "APP_PID=" + pid + "\n" +
                "CFG=\"" + cfg + "\"\n" +
                "USER_HOME=\"" + home + "\"\n" +
                "X=" + X + "\nSLEEP_SECS=" + S + "\n" +
                "while true; do\n" +
                "  if [ ! -d /proc/${APP_PID} ]; then exit 0; fi\n" +
                "  if ! grep -q '\"adw\": true' \"$CFG\"; then exit 0; fi\n" +
                "  for i in $(seq 1 ${X}); do\n" +
                "    LDIR=\"${USER_HOME}/Lingle/${i}\"\n" +
                "    [ -d \"$LDIR\" ] || continue\n" +
                "    for save in $(ls \"$LDIR\" -t1 --ignore='Z*' 2>/dev/null | tail -n +7); do\n" +
                "      rm -rf \"${LDIR}/${save}\"\n" +
                "    done\n" +
                "  done\n" +
                "  sleep ${SLEEP_SECS}\n" +
                "done\n";
    }

    public static void stopAdwQuietly() {
        try {
            if (adwProcess != null) {
                adwProcess.destroy();
                adwProcess.waitFor();
            }
        } catch (Exception ignored) {}
        adwProcess = null;
    }
}

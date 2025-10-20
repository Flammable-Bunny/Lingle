package flammable.bunny.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class TmpfsScriptManager {

    public static void ensureScriptsPresent() throws IOException {
        Path home = Path.of(System.getProperty("user.home"));
        Path base = home.resolve(".local/share/lingle");
        Path scriptsDir = base.resolve("scripts");
        Path savesDir = base.resolve("saves");

        Files.createDirectories(scriptsDir);
        Files.createDirectories(savesDir);

        Path enableSh = scriptsDir.resolve("tmpfsenable.sh");
        Path disableSh = scriptsDir.resolve("tmpfsdisable.sh");

        Files.writeString(enableSh, enableScript(), StandardCharsets.UTF_8);
        enableSh.toFile().setExecutable(true);
        Files.writeString(disableSh, disableScript(), StandardCharsets.UTF_8);
        disableSh.toFile().setExecutable(true);
    }

    private static String enableScript() {
        return """
                #!/bin/bash
                set -euo pipefail

                USER_NAME="$(logname 2>/dev/null || id -un)"
                USER_HOME="$(getent passwd "${USER_NAME}" | cut -d: -f6)"
                USER_UID="$(id -u "${USER_NAME}")"
                USER_GID="$(id -g "${USER_NAME}")"
                TARGET="${USER_HOME}/Lingle"
                SIZE="4g"

                COMMENT="# LINGLE tmpfs"
                LINE="tmpfs ${TARGET} tmpfs defaults,size=${SIZE},uid=${USER_UID},gid=${USER_GID},mode=0700 0 0"

                # Add entry if not exists (using pkexec for both operations)
                if ! grep -qF "${LINE}" /etc/fstab; then
                  echo "${COMMENT}" | pkexec tee -a /etc/fstab >/dev/null
                  echo "${LINE}" | pkexec tee -a /etc/fstab >/dev/null
                fi

                # Mount with pkexec
                if ! mountpoint -q "${TARGET}"; then
                  pkexec mount -t tmpfs -o size=${SIZE},uid=${USER_UID},gid=${USER_GID},mode=700 tmpfs "${TARGET}"
                fi
                """;
    }

    private static String disableScript() {
        return """
                #!/bin/bash
                set -euo pipefail

                USER_NAME="$(logname 2>/dev/null || id -un)"
                USER_HOME="$(getent passwd "${USER_NAME}" | cut -d: -f6)"
                USER_UID="$(id -u "${USER_NAME}")"
                USER_GID="$(id -g "${USER_NAME}")"
                TARGET="${USER_HOME}/Lingle"
                SIZE="4g"

                COMMENT="# LINGLE tmpfs"
                LINE="tmpfs ${TARGET} tmpfs defaults,size=${SIZE},uid=${USER_UID},gid=${USER_GID},mode=0700 0 0"

                if mountpoint -q "${TARGET}"; then
                  pkexec umount "${TARGET}"
                fi

                if grep -qF "${LINE}" /etc/fstab; then
                  pkexec sed -i "/${COMMENT}/d" /etc/fstab
                  pkexec sed -i "\\|${LINE}|d" /etc/fstab
                fi
                """;
    }
}

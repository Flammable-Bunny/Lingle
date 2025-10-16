package flammable.bunny.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;

public final class PackagesforRunSubmissionZipper {

    private PackagesforRunSubmissionZipper() {}

    public static Path install(Path outDir) throws IOException {
        if (!DependencyInstaller.ensureDeps(null))
            throw new IOException("Missing required dependencies (jq, zip).");

        Path home = Path.of(System.getProperty("user.home"));
        Path scriptsDir = home.resolve(".local/share/lingle/scripts");
        Files.createDirectories(scriptsDir);

        Path scriptPath = scriptsDir.resolve("src_auto_zip.sh");

        String script = """
            #!/usr/bin/env bash
            set -euo pipefail

            OUT_DIR="${1:-}"
            [[ -n "$OUT_DIR" && -d "$OUT_DIR" ]] || exit 2

            SRIGT_HOME="$HOME/speedrunigt"
            JSON="$SRIGT_HOME/latest_world.json"
            [[ -f "$JSON" ]] || exit 4

            WORLD_PATH="$(/usr/bin/jq -r '.world_path' "$JSON")"
            [[ -n "$WORLD_PATH" && "$WORLD_PATH" != "null" ]] || exit 5

            MC_SAVES_DIR="$(dirname "$WORLD_PATH")"
            MC_DIR="$(dirname "$MC_SAVES_DIR")"
            WORLD_NAME="$(basename "$WORLD_PATH")"
            LATEST_DIR="$MC_SAVES_DIR/$WORLD_NAME"
            [[ -d "$LATEST_DIR" ]] || exit 6

            stat_mtime() { stat -c %Y "$1" 2>/dev/null || stat -f %m "$1"; }

            DATE="$(date '+%Y-%m-%d')"
            TIME="$(date '+%H-%M-%S')"
            OUT="SRC-Submission-${DATE}-${TIME}.zip"

            STAGE="$(mktemp -d)"
            trap 'rm -rf "$STAGE"' EXIT

            LATEST_MTIME="$(stat_mtime "$LATEST_DIR")"

            cd "$MC_SAVES_DIR"
            mkdir -p "$STAGE"

            /usr/bin/zip -r -q "$STAGE/${WORLD_NAME}.zip" "$WORLD_NAME"

            mapfile -t saves < <(find . -mindepth 1 -maxdepth 1 -type d -printf '%P\\n')
            declare -a older newer
            for save in "${saves[@]}"; do
              [[ "$save" == "$WORLD_NAME" ]] && continue
              mt="$(stat_mtime "$save")" || mt=0
              (( mt < LATEST_MTIME )) && older+=("$save")
              (( mt > LATEST_MTIME )) && newer+=("$save")
            done

            if ((${#older[@]})); then
              mkdir -p "$STAGE/Last5"
              for save in $(for s in "${older[@]}"; do
                               mt="$(stat_mtime "$s")"; echo "$((LATEST_MTIME - mt))|$s";
                             done | sort -n | head -5 | cut -d'|' -f2-); do
                /usr/bin/zip -r -q "$STAGE/Last5/${save}.zip" "$save"
              done
            fi

            if ((${#newer[@]})); then
              mkdir -p "$STAGE/Background"
              for save in "${newer[@]}"; do
                /usr/bin/zip -r -q "$STAGE/Background/${save}.zip" "$save"
              done
            fi

            MC_LOGS_DIR="$MC_DIR/logs"
            if [[ -d "$MC_LOGS_DIR" ]]; then
              mkdir -p "$STAGE/logs"
              find "$MC_LOGS_DIR" -maxdepth 1 -type f -printf '%T@ %p\\n' 2>/dev/null \
                | sort -nr | head -3 | cut -d' ' -f2- \
                | while IFS= read -r log; do cp -f "$log" "$STAGE/logs/"; done
            fi

            cd "$STAGE"
            /usr/bin/zip -r -q "$OUT" .
            mv -f "$OUT" "$OUT_DIR/$OUT"
            exit 0
            """;

        Files.writeString(
                scriptPath,
                script,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );

        try {
            Set<PosixFilePermission> perms = EnumSet.of(
                    PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE,
                    PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_EXECUTE,
                    PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_EXECUTE
            );
            Files.setPosixFilePermissions(scriptPath, perms);
        } catch (UnsupportedOperationException ignored) {}

        return scriptPath;
    }
}

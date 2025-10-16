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
            throw new IOException("Missing required dependencies (jq, zip, python3).");

        Path home = Path.of(System.getProperty("user.home"));
        Path scriptsDir = home.resolve(".local/share/lingle/scripts");
        Files.createDirectories(scriptsDir);

        Path scriptPath = scriptsDir.resolve("src_auto_zip.py");

        String script = """
            #!/usr/bin/env python3
            import json
            import os
            from sys import stderr, argv, exit
            from datetime import datetime
            import zipfile
            from zipfile import ZipFile
            import shutil

            if len(argv) < 2:
                print("Usage: src_auto_zip.py <output_directory>", file=stderr)
                exit(2)

            out_dir = argv[1]
            if not os.path.isdir(out_dir):
                print(f"Output directory does not exist: {out_dir}", file=stderr)
                exit(2)

            print("Zipping files...")

            home = os.environ.get("HOME")
            if home is None:
                print("$HOME is not defined.", file=stderr)
                exit(1)

            srigt_home = os.path.join(home, "speedrunigt")

            json_path = os.path.join(srigt_home, "latest_world.json")
            if not os.path.exists(json_path):
                print(f"JSON file not found: {json_path}", file=stderr)
                exit(4)

            with open(json_path) as file:
                latest_world = json.load(file)

            world_path = latest_world['world_path']
            path_components = world_path.split("/")

            # Remove last two components from "<mc_dir>/saves/<world_name>"
            mc_dir = "/".join(path_components[:-2:])

            # Remove last component from "<mc_saves_dir>/<world_name>"
            mc_saves_dir = "/".join(path_components[:-1:])

            # Has the last component form "<mc_saves_dir>/<world_name>"
            world_name = path_components[-1]

            today = datetime.now()
            str_date = today.strftime("%H-%M-%S")
            str_time = today.strftime("%Y-%m-%d")
            output_file_name = os.path.join(out_dir, f"LIGNLE-SRC-Submission-{str_time}-{str_date}.zip")
            zip = ZipFile(output_file_name, "w", zipfile.ZIP_DEFLATED)

            saves = os.listdir(mc_saves_dir)

            latest_world_write_time = None
            for save_dir in saves:
                if save_dir == world_name:
                    full_save_path = os.path.join(mc_saves_dir, save_dir)
                    zip_name = f"{save_dir}.zip"
                    print(f"Zipping {full_save_path} at the root")
                    shutil.make_archive(save_dir, 'zip', full_save_path)
                    zip.write(zip_name)
                    os.remove(zip_name)
                    latest_world_write_time = os.path.getmtime(full_save_path)
                    break
            else:
                print(f"No directory found in saves for world name: '{world_name}' in saves: '{mc_saves_dir}'", file=stderr)
                exit(1)


            def last5_sorter(save):
                if latest_world_write_time is None:
                    exit(1)

                return latest_world_write_time - os.path.getmtime(os.path.join(mc_saves_dir, save))


            def logs_sorter(log):
                return os.path.getmtime(os.path.join(mc_logs_dir, log))


            def last5_filter(save):
                if latest_world_write_time is None:
                    exit(1)

                return os.path.getmtime(os.path.join(mc_saves_dir, save)) < latest_world_write_time


            def background_worlds_filter(save):
                if latest_world_write_time is None:
                    exit(1)

                return os.path.getmtime(os.path.join(mc_saves_dir, save)) > latest_world_write_time


            last_five_worlds = sorted(filter(last5_filter, saves), key=last5_sorter)[:5]
            zip.mkdir("Last5")
            for save_dir in last_five_worlds:
                full_save_path = os.path.join(mc_saves_dir, save_dir)
                arcname = f"{os.path.join("Last5", save_dir)}.zip"
                zip_name = f"{save_dir}.zip"
                print(f"Zipping {full_save_path} into 'Last5' directory")
                shutil.make_archive(save_dir, 'zip', full_save_path)
                zip.write(zip_name, arcname)
                os.remove(zip_name)

            background_worlds = list(filter(background_worlds_filter, saves))
            if len(background_worlds) != 0:
                zip.mkdir("Background")
                for save_dir in background_worlds:
                    full_save_path = os.path.join(mc_saves_dir, save_dir)
                    arcname = f"{os.path.join("Background", save_dir)}.zip"
                    zip_name = f"{save_dir}.zip"
                    print(f"Zipping {full_save_path} into 'Background' directory")
                    shutil.make_archive(save_dir, 'zip', full_save_path)
                    zip.write(zip_name, arcname)
                    os.remove(zip_name)


            mc_logs_dir = os.path.join(mc_dir, "logs")
            logs = sorted(os.listdir(mc_logs_dir), key=logs_sorter, reverse=True)[:3]
            zip.mkdir("logs")
            for log_name in logs:
                full_log_path = os.path.join(mc_logs_dir, log_name)
                arcname = os.path.join("logs", log_name)
                print(f"Zipping {full_log_path} into 'logs' directory")
                zip.write(full_log_path, arcname)

            zip.close()

            print("Done")
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

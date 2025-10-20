package flammable.bunny.core;

import flammable.bunny.core.WorldBopperConfig.KeepWorldInfo;
import flammable.bunny.core.WorldBopperConfig.KeepCondition;

import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;

public class WorldBopperManager {

    public static void runOnce() {
        if (!LingleState.worldBopperEnabled) return;
        if (LingleState.WorldbopperSelectedInstances.isEmpty()) return;

        try {
            Path home = Path.of(System.getProperty("user.home"));
            Path instances = home.resolve(".local/share/PrismLauncher/instances");

            if (Files.isDirectory(instances)) {
                try (DirectoryStream<Path> insts = Files.newDirectoryStream(instances)) {
                    for (Path inst : insts) {
                        String instName = inst.getFileName().toString();
                        if (!LingleState.WorldbopperSelectedInstances.contains(instName)) continue;

                        Path saves = inst.resolve("minecraft").resolve("saves");
                        cleanDir(saves);
                    }
                }
            }

            if (LingleState.enabled) {
                int max = Math.max(1, LingleState.instanceCount);
                for (int i = 1; i <= max; i++) {
                    Path ldir = home.resolve("Lingle").resolve(String.valueOf(i));
                    cleanDir(ldir);
                }
            }

        } catch (IOException ignored) {}
    }

    private static void cleanDir(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) return;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
            for (Path p : ds) {
                if (Files.isDirectory(p) && shouldDeleteWorld(p)) {
                    deleteDir(p);
                }
            }
        }
    }

    private static boolean shouldDeleteWorld(Path worldDir) {
        String name = worldDir.getFileName().toString();

        KeepWorldInfo matchingConfig = null;
        for (KeepWorldInfo info : LingleState.boppableWorlds) {
            if (!info.prefix.isEmpty() && name.startsWith(info.prefix)) {
                matchingConfig = info;
                break;
            }
        }

        if (matchingConfig == null) return false;

        KeepCondition condition = matchingConfig.condition;

        switch (condition) {
            case ALWAYS_DELETE:
                return true;

            case REACHED_NETHER:
                return !hasNetherFolder(worldDir);

            case REACHED_BASTION:
                return !hasBastion(worldDir);

            case REACHED_FORTRESS:
                return !hasFortress(worldDir);

            case REACHED_STRONGHOLD:
                return !hasStronghold(worldDir);

            case REACHED_END:
                return !hasEndFolder(worldDir);

            case WORLD_SIZE:
                try {
                    long sizeBytes = getDirectorySize(worldDir);
                    long sizeMB = sizeBytes / (1024 * 1024);
                    return sizeMB < matchingConfig.minSizeMB;
                } catch (Exception e) {
                    return true;
                }

            default:
                return false;
        }
    }

    private static boolean hasNetherFolder(Path worldDir) {
        Path dim1 = worldDir.resolve("DIM-1");
        return Files.exists(dim1) && Files.isDirectory(dim1);
    }

    private static boolean hasEndFolder(Path worldDir) {
        Path dim1 = worldDir.resolve("DIM1");
        return Files.exists(dim1) && Files.isDirectory(dim1);
    }

    private static boolean hasBastion(Path worldDir) {
        return hasStructure(worldDir, "Bastion");
    }

    private static boolean hasFortress(Path worldDir) {
        return hasStructure(worldDir, "Fortress");
    }

    private static boolean hasStronghold(Path worldDir) {
        return hasStructure(worldDir, "Stronghold");
    }

    private static boolean hasStructure(Path worldDir, String structureName) {
        try {
            Path dataDir = worldDir.resolve("data");
            if (Files.exists(dataDir) && Files.isDirectory(dataDir)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(dataDir, "*.dat")) {
                    for (Path datFile : stream) {
                        String fileName = datFile.getFileName().toString().toLowerCase();
                        if (fileName.contains(structureName.toLowerCase())) {
                            return true;
                        }
                    }
                }
            }

            Path levelDat = worldDir.resolve("level.dat");
            if (Files.exists(levelDat)) {
                if (structureName.equals("Bastion")) {
                    return checkForStructureFiles(worldDir, "minecraft/structures/bastion");
                } else if (structureName.equals("Fortress")) {
                    return checkForStructureFiles(worldDir, "minecraft/structures/fortress");
                } else if (structureName.equals("Stronghold")) {
                    return checkForStructureFiles(worldDir, "data/Stronghold");
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    private static boolean checkForStructureFiles(Path worldDir, String structurePath) {
        try {
            Path structure = worldDir.resolve(structurePath);
            return Files.exists(structure);
        } catch (Exception e) {
            return false;
        }
    }

    private static long getDirectorySize(Path dir) throws IOException {
        try (var walk = Files.walk(dir)) {
            return walk
                .filter(Files::isRegularFile)
                .mapToLong(p -> {
                    try {
                        return Files.size(p);
                    } catch (IOException e) {
                        return 0;
                    }
                })
                .sum();
        }
    }

    private static void deleteDir(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (var walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try { Files.deleteIfExists(p); } catch (IOException ignored) {}
            });
        }
    }
}

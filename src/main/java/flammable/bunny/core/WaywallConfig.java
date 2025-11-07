package flammable.bunny.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WaywallConfig {
    private static final Path CONFIG_DIR = Path.of(System.getProperty("user.home"), ".config", "waywall");
    private static final Path CONFIG_LUA = CONFIG_DIR.resolve("config.lua");
    private static final Path INIT_LUA = CONFIG_DIR.resolve("init.lua");
    private static final Path REMAPS_LUA = CONFIG_DIR.resolve("remaps.lua");

    // ==================== TOGGLE METHODS ====================

    /**
     * Get a boolean toggle from config.lua
     */
    public static boolean getToggle(String key, boolean def) {
        try {
            if (!Files.exists(CONFIG_LUA)) return def;
            String s = Files.readString(CONFIG_LUA);
            Pattern p = Pattern.compile("(?m)^\\s*local\\s+" + Pattern.quote(key) + "\\s*=\\s*(true|false)");
            Matcher m = p.matcher(s);
            if (m.find()) {
                return Boolean.parseBoolean(m.group(1));
            }
        } catch (IOException ignored) {}
        return def;
    }

    /**
     * Set a boolean toggle in config.lua
     */
    public static void setToggle(String key, boolean value) throws IOException {
        Files.createDirectories(CONFIG_DIR);
        String content = Files.exists(CONFIG_LUA) ? Files.readString(CONFIG_LUA) : "";

        Pattern p = Pattern.compile("(?m)^(\\s*local\\s+" + Pattern.quote(key) + "\\s*=\\s*)(true|false)");
        Matcher m = p.matcher(content);

        String updated;
        if (m.find()) {
            updated = m.replaceAll("$1" + (value ? "true" : "false"));
        } else {
            // Add new toggle at the top of the file
            updated = "local " + key + " = " + (value ? "true" : "false") + "\n" + content;
        }

        // Special handling for res_1440 - update mirror positions
        if ("res_1440".equals(key) && value) {
            updated = update1440pMirrorPositions(updated);
        }

        Files.writeString(CONFIG_LUA, updated, StandardCharsets.UTF_8);
    }

    /**
     * Update mirror positions for 1440p resolution
     */
    private static String update1440pMirrorPositions(String content) {
        // Update e_count
        content = updateLuaTable(content, "e_count",
            Map.of("x", "1500", "y", "400", "size", "5", "colorkey", "false"));

        // Update thin_pie
        content = updateLuaTable(content, "thin_pie",
            Map.of("x", "1490", "y", "645", "size", "4", "colorkey", "false"));

        // Update thin_percent
        content = updateLuaTable(content, "thin_percent",
            Map.of("enabled", "false", "x", "1568", "y", "1050", "size", "6"));

        // Update tall_pie
        content = updateLuaTable(content, "tall_pie",
            Map.of("x", "1490", "y", "645", "size", "4", "colorkey", "false"));

        // Update tall_percent
        content = updateLuaTable(content, "tall_percent",
            Map.of("enabled", "false", "x", "1568", "y", "1050", "size", "6"));

        return content;
    }

    /**
     * Update a Lua table with new values
     */
    private static String updateLuaTable(String content, String tableName, Map<String, String> values) {
        // Find the table definition
        Pattern tablePattern = Pattern.compile(
            "(?m)^(\\s*local\\s+" + Pattern.quote(tableName) + "\\s*=\\s*\\{)([^}]+)(\\})",
            Pattern.DOTALL
        );
        Matcher m = tablePattern.matcher(content);

        if (m.find()) {
            String prefix = m.group(1);
            String tableContent = m.group(2);
            String suffix = m.group(3);

            // Update each value in the table
            for (Map.Entry<String, String> entry : values.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                Pattern keyPattern = Pattern.compile(
                    "(" + Pattern.quote(key) + "\\s*=\\s*)([^,\\s]+)"
                );
                Matcher keyMatcher = keyPattern.matcher(tableContent);

                if (keyMatcher.find()) {
                    tableContent = keyMatcher.replaceAll("$1" + value);
                }
            }

            String replacement = prefix + tableContent + suffix;
            return m.replaceFirst(Matcher.quoteReplacement(replacement));
        }

        return content;
    }

    // ==================== PATH METHODS ====================

    /**
     * Read paths from init.lua
     */
    public static Map<String, String> readPaths() {
        Map<String, String> out = new HashMap<>();
        try {
            if (!Files.exists(INIT_LUA)) return out;
            String s = Files.readString(INIT_LUA);

            // Look for path assignments in init.lua
            Pattern pathPattern = Pattern.compile(
                "(?m)^\\s*local\\s+(pacem_path|nb_path|overlay_path|bg_path|stretched_overlay_path|tall_overlay_path|thin_overlay_path|wide_overlay_path)\\s*=\\s*waywall_config_path\\s*\\.\\.\\s*\"resources/([^\"]+)\""
            );
            Matcher m = pathPattern.matcher(s);

            while (m.find()) {
                String varName = m.group(1);
                String filename = m.group(2);
                out.put(varName, "/resources/" + filename);
            }
        } catch (IOException ignored) {}
        return out;
    }

    /**
     * Set a path variable in init.lua
     */
    public static void setPathVar(String varName, String homeRelative) throws IOException {
        Files.createDirectories(CONFIG_DIR);
        String content = Files.exists(INIT_LUA) ? Files.readString(INIT_LUA) : "";

        // Extract just the filename from the path
        String filename = homeRelative;
        if (filename.startsWith("/")) filename = filename.substring(1);

        // Pattern to match the path assignment in init.lua
        Pattern p = Pattern.compile(
            "(?m)^(\\s*local\\s+" + Pattern.quote(varName) + "\\s*=\\s*waywall_config_path\\s*\\.\\.\\s*\"resources/)([^\"]+)(\")"
        );
        Matcher m = p.matcher(content);

        String updated;
        if (m.find()) {
            updated = m.replaceAll("$1" + Matcher.quoteReplacement(filename) + "$3");
        } else {
            // Add new path assignment
            Pattern sectionEnd = Pattern.compile("(?m)^(local keyboard_remaps = require)");
            Matcher sectionMatcher = sectionEnd.matcher(content);
            if (sectionMatcher.find()) {
                String newLine = "local " + varName + " = waywall_config_path .. \"resources/" + filename + "\"\n";
                updated = sectionMatcher.replaceFirst(newLine + "$1");
            } else {
                updated = content + "\nlocal " + varName + " = waywall_config_path .. \"resources/" + filename + "\"\n";
            }
        }

        Files.writeString(INIT_LUA, updated, StandardCharsets.UTF_8);
    }

    /**
     * Convert absolute path to home-relative format
     */
    public static String toHomeRelative(Path absolute) {
        Path home = Path.of(System.getProperty("user.home"));
        Path norm = absolute.toAbsolutePath().normalize();
        if (norm.startsWith(home)) {
            Path rel = home.relativize(norm);
            return "/" + rel.toString().replace('\\', '/');
        }
        return norm.toString();
    }

    // ==================== KEYBIND METHODS ====================

    /**
     * Set a keybind variable in config.lua
     */
    public static void setKeybindVar(String varName, String value, boolean withStar, boolean isPlaceholder) throws IOException {
        Files.createDirectories(CONFIG_DIR);
        String content = Files.exists(CONFIG_LUA) ? Files.readString(CONFIG_LUA) : "";

        String inner = (withStar ? "*-" : "") + value;

        // Handle table-based keybinds (thin, wide, tall)
        if (varName.equals("thin_key") || varName.equals("wide_key") || varName.equals("tall_key")) {
            String tableName = varName.replace("_key", "");
            Pattern tablePattern = Pattern.compile(
                "(?m)^(\\s*local\\s+" + Pattern.quote(tableName) + "\\s*=\\s*\\{\\s*key\\s*=\\s*\")([^\"]+)(\")",
                Pattern.DOTALL
            );
            Matcher m = tablePattern.matcher(content);

            if (m.find()) {
                content = m.replaceAll("$1" + Matcher.quoteReplacement(inner) + "$3");
            }
        } else {
            // Handle simple keybind variables
            Pattern p = Pattern.compile("(?m)^(\\s*local\\s+" + Pattern.quote(varName) + "\\s*=\\s*\")([^\"]+)(\")");
            Matcher m = p.matcher(content);

            if (m.find()) {
                content = m.replaceAll("$1" + Matcher.quoteReplacement(inner) + "$3");
            } else {
                // Add new keybind
                content = content + "\nlocal " + varName + " = \"" + inner + "\"\n";
            }
        }

        Files.writeString(CONFIG_LUA, content, StandardCharsets.UTF_8);
    }

    /**
     * Read keybinds from config.lua
     */
    public static void readKeybindsFromFile() throws IOException {
        if (!Files.exists(CONFIG_LUA)) {
            throw new IOException("Config file not found: " + CONFIG_LUA);
        }

        String content = Files.readString(CONFIG_LUA);

        // Pattern for table-based keybinds (thin, wide, tall)
        Pattern tablePattern = Pattern.compile("(?m)^\\s*local\\s+(thin|wide|tall)\\s*=\\s*\\{\\s*key\\s*=\\s*\"([^\"]+)\"");
        Matcher tableMatcher = tablePattern.matcher(content);

        while (tableMatcher.find()) {
            String name = tableMatcher.group(1);
            String value = tableMatcher.group(2);

            if (value.startsWith("*-")) value = value.substring(2);
            if (value.toLowerCase().contains("placeholder") || value.isBlank()) continue;

            if (name.equals("thin")) LingleState.setSetKeybind("Thin_Key", value);
            else if (name.equals("wide")) LingleState.setSetKeybind("Wide_Key", value);
            else if (name.equals("tall")) LingleState.setSetKeybind("Tall_Key", value);
        }

        // Pattern for simple keybind variables
        Pattern simplePattern = Pattern.compile("(?m)^\\s*local\\s+(launch_paceman_key|toggle_fullscreen_key|toggle_ninbot_key|toggle_remaps_key)\\s*=\\s*\"([^\"]+)\"");
        Matcher simpleMatcher = simplePattern.matcher(content);

        while (simpleMatcher.find()) {
            String varName = simpleMatcher.group(1);
            String value = simpleMatcher.group(2);

            if (value.startsWith("*-")) value = value.substring(2);
            if (value.toLowerCase().contains("placeholder") || value.isBlank()) continue;

            if (varName.equals("toggle_ninbot_key")) LingleState.setSetKeybind("NBB_Key", value);
            else if (varName.equals("toggle_fullscreen_key")) LingleState.setSetKeybind("Fullscreen_Key", value);
            else if (varName.equals("launch_paceman_key")) LingleState.setSetKeybind("Apps_Key", value);
            else if (varName.equals("toggle_remaps_key")) LingleState.setSetKeybind("Remaps_Key", value);
        }
    }

    // ==================== REMAPS METHODS ====================

    /**
     * Write remaps to remaps.lua
     */
    public static void writeRemapsFile(java.util.List<Remaps> remaps) throws IOException {
        Files.createDirectories(CONFIG_DIR);

        StringBuilder sb = new StringBuilder();
        sb.append("return {\n");
        sb.append("\tremapped_kb = {\n");
        sb.append("\t\t-- Add any playing remaps here\n");

        // Permanent remaps (always active)
        for (Remaps remap : remaps) {
            if (remap.isPermanent && !remap.fromKey.isEmpty() && !remap.toKey.isEmpty()) {
                sb.append("\t\t[\"").append(remap.fromKey).append("\"] = \"").append(remap.toKey).append("\",\n");
            }
        }

        sb.append("\n\t},\n\n");
        sb.append("\tnormal_kb = {\n");
        sb.append("\t\t-- Add any remaps you want to keep when disabling normal remaps (not necessary)\n");

        // Normal remaps (toggleable)
        for (Remaps remap : remaps) {
            if (!remap.isPermanent && !remap.fromKey.isEmpty() && !remap.toKey.isEmpty()) {
                sb.append("\t\t[\"").append(remap.fromKey).append("\"] = \"").append(remap.toKey).append("\",\n");
            }
        }

        sb.append("\n\t},\n\n");
        sb.append("}\n");

        Files.writeString(REMAPS_LUA, sb.toString(), StandardCharsets.UTF_8);
    }

    /**
     * Read remaps from remaps.lua
     */
    public static java.util.List<Remaps> readRemapsFile() {
        java.util.List<Remaps> remaps = new java.util.ArrayList<>();
        try {
            if (!Files.exists(REMAPS_LUA)) return remaps;

            String content = Files.readString(REMAPS_LUA);

            // Parse remapped_kb (permanent)
            Pattern remappedKbSection = Pattern.compile("remapped_kb\\s*=\\s*\\{([^}]+)\\}", Pattern.DOTALL);
            Matcher m1 = remappedKbSection.matcher(content);
            if (m1.find()) {
                String section = m1.group(1);
                Pattern entryPattern = Pattern.compile("\\[\"([^\"]+)\"\\]\\s*=\\s*\"([^\"]+)\"");
                Matcher entries = entryPattern.matcher(section);
                while (entries.find()) {
                    String from = entries.group(1);
                    String to = entries.group(2);
                    remaps.add(new Remaps(from, to, true));
                }
            }

            // Parse normal_kb (toggleable)
            Pattern normalKbSection = Pattern.compile("normal_kb\\s*=\\s*\\{([^}]+)\\}", Pattern.DOTALL);
            Matcher m2 = normalKbSection.matcher(content);
            if (m2.find()) {
                String section = m2.group(1);
                Pattern entryPattern = Pattern.compile("\\[\"([^\"]+)\"\\]\\s*=\\s*\"([^\"]+)\"");
                Matcher entries = entryPattern.matcher(section);
                while (entries.find()) {
                    String from = entries.group(1);
                    String to = entries.group(2);
                    remaps.add(new Remaps(from, to, false));
                }
            }

        } catch (IOException ignored) {}
        return remaps;
    }

    // ==================== GETTERS ====================

    public static Path getConfigDir() { return CONFIG_DIR; }
    public static Path getConfigFile() { return CONFIG_LUA; }
    public static Path getInitFile() { return INIT_LUA; }
    public static Path getRemapsFile() { return REMAPS_LUA; }

    // Legacy compatibility
    @Deprecated
    public static Path getTogglesFile() { return CONFIG_LUA; }
    @Deprecated
    public static Path getPathsFile() { return INIT_LUA; }
    @Deprecated
    public static Path getKeybindsFile() { return CONFIG_LUA; }
}

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
    private static final Path INIT_FILE = CONFIG_DIR.resolve("init.lua");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("config.lua");
    private static final Path REMAPS = CONFIG_DIR.resolve("remaps.lua");

    private static Pattern togglePattern(String key) {
        return Pattern.compile("(?m)^(\\s*local\\s+" + Pattern.quote(key) + "\\s*=\\s*)(true|false)(\\s*,?\\s*)$");
    }

    public static boolean getToggle(String key, boolean def) {
        try {
            if (!Files.exists(CONFIG_FILE)) return def;
            String s = Files.readString(CONFIG_FILE);
            Matcher m = togglePattern(key).matcher(s);
            if (m.find()) {
                return Boolean.parseBoolean(m.group(2));
            }
        } catch (IOException ignored) {}
        return def;
    }

    public static void setToggle(String key, boolean value) throws IOException {
        Files.createDirectories(CONFIG_DIR);
        String content = Files.exists(CONFIG_FILE) ? Files.readString(CONFIG_FILE) : "";
        Matcher m = togglePattern(key).matcher(content);
        String replacement = "$1" + (value ? "true" : "false") + "$3";
        String updated;
        if (m.find()) {
            updated = m.replaceAll(replacement);
        } else {
            // Need to add both the variable definition and the export entry
            // 1. Add variable definition before the export section
            String varDef = "local " + key + " = " + (value ? "true" : "false") + "\n";
            Pattern beforeExport = Pattern.compile("(?m)(\\n-- ======== EXPORT ========\\n)");
            Matcher m2 = beforeExport.matcher(content);
            if (m2.find()) {
                updated = m2.replaceFirst(varDef + "$1");
            } else {
                updated = content + (content.endsWith("\n") ? "" : "\n") + varDef;
            }

            // 2. Add to export table (before the closing brace)
            String exportEntry = "    " + key + " = " + key + ",\n";
            Pattern beforeClosingBrace = Pattern.compile("(?m)(\\n\\}\\n*$)");
            Matcher m3 = beforeClosingBrace.matcher(updated);
            if (m3.find()) {
                updated = m3.replaceFirst(exportEntry + "$1");
            }
        }
        Files.writeString(CONFIG_FILE, updated, StandardCharsets.UTF_8);
    }

    public static Map<String, String> readPaths() {
        Map<String, String> out = new HashMap<>();
        try {
            if (!Files.exists(INIT_FILE)) return out;
            String s = Files.readString(INIT_FILE);

            // Pattern to match variables in init.lua like: local pacem_path = waywall_config_path .. "resources/paceman.jar"
            Pattern pathPattern = Pattern.compile("(?m)^\\s*local\\s+(pacem_path|nb_path|overlay_path|lingle_path)\\s*=\\s*waywall_config_path\\s*\\.\\.\\s*\"([^\"]+)\"");
            Matcher m = pathPattern.matcher(s);
            while (m.find()) {
                String var = m.group(1);
                String path = m.group(2);
                out.put(var, path);
            }
        } catch (IOException ignored) {}
        return out;
    }

    public static void setPathVar(String varName, String homeRelative) throws IOException {
        if (!"lingle_path".equals(varName)) {
            // Only lingle_path should be edited in init.lua, others are fixed
            return;
        }

        Files.createDirectories(CONFIG_DIR);
        if (!Files.exists(INIT_FILE)) return;

        String content = Files.readString(INIT_FILE);

        // Pattern to find or add lingle_path in init.lua
        Pattern linglepathPattern = Pattern.compile("(?m)^(\\s*local\\s+lingle_path\\s*=\\s*waywall_config_path\\s*\\.\\.\\s*\")([^\"]*)(\"\\s*,?\\s*)$");
        Matcher m = linglepathPattern.matcher(content);

        String updated;
        if (m.find()) {
            // Replace existing lingle_path
            updated = m.replaceAll("$1" + Matcher.quoteReplacement(homeRelative) + "$3");
        } else {
            // Add lingle_path after waywall_config_path definition
            Pattern waywallConfigPathPattern = Pattern.compile("(?m)^(\\s*local\\s+waywall_config_path\\s*=.*\\n)");
            Matcher m2 = waywallConfigPathPattern.matcher(content);
            if (m2.find()) {
                String insertion = "local lingle_path = waywall_config_path .. \"" + homeRelative + "\"\n";
                updated = m2.replaceFirst("$1" + Matcher.quoteReplacement(insertion));
            } else {
                // Fallback: add at the beginning after imports
                updated = content.replaceFirst("(?m)(-- Other inits\\n)", "$1local lingle_path = waywall_config_path .. \"" + homeRelative + "\"\n");
            }
        }

        // Ensure lingle launcher code exists in init.lua
        updated = ensureLingleLauncherCode(updated);

        Files.writeString(INIT_FILE, updated, StandardCharsets.UTF_8);
    }

    private static String ensureLingleLauncherCode(String content) {
        // Check if lingle launcher code already exists
        if (content.contains("--*********************************************************************************************** LINGLE")) {
            return content;
        }

        // Add lingle launcher code after NINJABRAIN section
        String lingleCode = "\n" +
                "--*********************************************************************************************** LINGLE\n" +
                "local toggle_lingle = cfg.toggle_lingle or false\n" +
                "\n" +
                "local is_lingle_running = function()\n" +
                "    local handle = io.popen(\"pgrep -f 'lingle.*jar'\")\n" +
                "    local result = handle:read(\"*l\")\n" +
                "    handle:close()\n" +
                "    return result ~= nil\n" +
                "end\n" +
                "\n" +
                "local exec_lingle = function()\n" +
                "    if toggle_lingle and not is_lingle_running() then\n" +
                "        waywall.exec(\"java -jar \" .. lingle_path)\n" +
                "    end\n" +
                "end\n" +
                "\n" +
                "-- Auto-launch lingle when toggle_lingle is enabled\n" +
                "if toggle_lingle then\n" +
                "    exec_lingle()\n" +
                "end\n\n";

        // Insert after NINJABRAIN section
        Pattern ninjaSectionEnd = Pattern.compile("(?m)(local exec_ninb = function\\(\\).*?end\\n)");
        Matcher m = ninjaSectionEnd.matcher(content);
        if (m.find()) {
            return m.replaceFirst("$1" + Matcher.quoteReplacement(lingleCode));
        }

        // Fallback: add before MIRRORS section
        Pattern beforeMirrors = Pattern.compile("(?m)(\\n--\\*+\\s*MIRRORS)");
        Matcher m2 = beforeMirrors.matcher(content);
        if (m2.find()) {
            return m2.replaceFirst(Matcher.quoteReplacement(lingleCode) + "$1");
        }

        return content;
    }

    public static String toHomeRelative(Path absolute) {
        Path home = Path.of(System.getProperty("user.home"));
        Path norm = absolute.toAbsolutePath().normalize();
        if (norm.startsWith(home)) {
            Path rel = home.relativize(norm);
            return "/" + rel.toString().replace('\\', '/');
        }
        return norm.toString();
    }

    public static void setKeybindPlaceholder(String placeholderToken, String keybind) throws IOException {
        Files.createDirectories(CONFIG_DIR);
        String content = Files.exists(CONFIG_FILE) ? Files.readString(CONFIG_FILE) : "";
        Pattern quotedToken = Pattern.compile("(?m)(\")" + Pattern.quote(placeholderToken) + "(\")");
        Matcher m = quotedToken.matcher(content);
        String updated = m.replaceAll("$1" + Matcher.quoteReplacement(keybind) + "$2");
        if (updated.equals(content)) {
            Pattern linePattern = Pattern.compile("(?m)^(\\s*[A-Za-z_][A-Za-z0-9_]*\\s*=\\s*\")" + Pattern.quote(placeholderToken) + "\"(\\s*,?\\s*)$");
            Matcher m2 = linePattern.matcher(content);
            updated = m2.replaceAll("$1" + Matcher.quoteReplacement(keybind) + "\"$2");
        }
        if (updated.equals(content)) {
            updated = content + (content.endsWith("\n") ? "" : "\n") + "-- " + placeholderToken + " = '" + keybind + "'\n";
        }
        Files.writeString(CONFIG_FILE, updated, StandardCharsets.UTF_8);
    }


    public static void setKeybindVar(String varName, String value, boolean withStar, boolean isPlaceholder) throws IOException {
        Files.createDirectories(CONFIG_DIR);
        String content = Files.exists(CONFIG_FILE) ? Files.readString(CONFIG_FILE) : "";
        String inner = (withStar ? "*-" : "") + value;

        String updated;
        boolean wasAdded = false;

        // Check if this is a table-based keybind (thin, wide, tall)
        if (varName.equals("thin") || varName.equals("wide") || varName.equals("tall")) {
            // Pattern to match: local thin = { key = "*-Alt_L", f3_safe = false }
            Pattern tablePattern = Pattern.compile("(?m)^(\\s*local\\s+" + Pattern.quote(varName) + "\\s*=\\s*\\{[^}]*key\\s*=\\s*\")([^\"]*)(\"[^}]*\\})");
            Matcher m = tablePattern.matcher(content);
            if (m.find()) {
                updated = m.replaceAll("$1" + Matcher.quoteReplacement(inner) + "$3");
            } else {
                // Add new table entry before export section
                String varDef = "local " + varName + " = { key = \"" + inner + "\", f3_safe = false }\n";
                Pattern beforeExport = Pattern.compile("(?m)(\\n-- ======== EXPORT ========\\n)");
                Matcher m2 = beforeExport.matcher(content);
                if (m2.find()) {
                    updated = m2.replaceFirst(varDef + "$1");
                } else {
                    updated = content + (content.endsWith("\n") ? "" : "\n") + varDef;
                }
                wasAdded = true;
            }
        } else {
            // Simple string keybind: local toggle_ninbot_key = "*-apostrophe"
            Pattern stringPattern = Pattern.compile("(?m)^(\\s*local\\s+" + Pattern.quote(varName) + "\\s*=\\s*\")(.*?)(\"\\s*,?\\s*)$");
            Matcher m = stringPattern.matcher(content);
            if (m.find()) {
                updated = m.replaceAll("$1" + Matcher.quoteReplacement(inner) + "$3");
            } else {
                // Add new keybind before export section
                String varDef = "local " + varName + " = \"" + inner + "\"\n";
                Pattern beforeExport = Pattern.compile("(?m)(\\n-- ======== EXPORT ========\\n)");
                Matcher m2 = beforeExport.matcher(content);
                if (m2.find()) {
                    updated = m2.replaceFirst(varDef + "$1");
                } else {
                    updated = content + (content.endsWith("\n") ? "" : "\n") + varDef;
                }
                wasAdded = true;
            }
        }

        // Add to export table if this was a new variable
        if (wasAdded) {
            String exportEntry = "    " + varName + " = " + varName + ",\n";
            Pattern beforeClosingBrace = Pattern.compile("(?m)(\\n\\}\\n*$)");
            Matcher m3 = beforeClosingBrace.matcher(updated);
            if (m3.find()) {
                updated = m3.replaceFirst(exportEntry + "$1");
            }
        }

        Files.writeString(CONFIG_FILE, updated, StandardCharsets.UTF_8);
    }


    public static void readKeybindsFromFile() throws IOException {
        if (!Files.exists(CONFIG_FILE)) {
            throw new IOException("Existing Config file not found: " + CONFIG_FILE);
        }

        String content = Files.readString(CONFIG_FILE);

        // Pattern for simple string keybinds like: local toggle_ninbot_key = "*-apostrophe"
        Pattern stringVarPattern = Pattern.compile("(?m)^\\s*local\\s+(\\w+_key)\\s*=\\s*\"([^\"]+)\"\\s*,?\\s*$");
        Matcher m1 = stringVarPattern.matcher(content);

        while (m1.find()) {
            String varName = m1.group(1).toLowerCase();
            String value = m1.group(2).trim();

            if (value.startsWith("*-")) value = value.substring(2);
            if (value.toLowerCase().contains("placeholder") || value.isBlank()) continue;

            if (varName.contains("ninbot") || varName.contains("nbb") || varName.contains("show"))
                LingleState.setSetKeybind("NBB_Key", value);
            else if (varName.contains("fullscreen")) LingleState.setSetKeybind("Fullscreen_Key", value);
            else if ((varName.contains("launch") || varName.contains("paceman")))
                LingleState.setSetKeybind("Apps_Key", value);
            else if (varName.contains("remap")) LingleState.setSetKeybind("Remaps_Key", value);
        }

        // Pattern for table keybinds like: local thin = { key = "*-Alt_L", f3_safe = false }
        Pattern tableKeyPattern = Pattern.compile("(?m)^\\s*local\\s+(thin|wide|tall)\\s*=\\s*\\{[^}]*key\\s*=\\s*\"([^\"]+)\"");
        Matcher m2 = tableKeyPattern.matcher(content);

        while (m2.find()) {
            String varName = m2.group(1).toLowerCase();
            String value = m2.group(2).trim();

            if (value.startsWith("*-")) value = value.substring(2);
            if (value.toLowerCase().contains("placeholder") || value.isBlank()) continue;

            if (varName.equals("thin")) LingleState.setSetKeybind("Thin_Key", value);
            else if (varName.equals("wide")) LingleState.setSetKeybind("Wide_Key", value);
            else if (varName.equals("tall")) LingleState.setSetKeybind("Tall_Key", value);
        }
    }

    public static void writeRemapsFile(java.util.List<Remaps> remaps) throws IOException {
        Files.createDirectories(CONFIG_DIR);

        StringBuilder sb = new StringBuilder();
        sb.append("return {\n");
        sb.append("remapped_kb = {\n");

        // Permanent remaps (always active)
        for (Remaps remap : remaps) {
            if (remap.isPermanent && !remap.fromKey.isEmpty() && !remap.toKey.isEmpty()) {
                sb.append("\t[\"").append(remap.fromKey).append("\"] = \"").append(remap.toKey).append("\",\n");
            }
        }

        sb.append("\t-- remaps_placeholder\n");
        sb.append("},\n\n");
        sb.append("normal_kb = {\n");

        // Normal remaps (toggleable)
        for (Remaps remap : remaps) {
            if (!remap.isPermanent && !remap.fromKey.isEmpty() && !remap.toKey.isEmpty()) {
                sb.append("\t[\"").append(remap.fromKey).append("\"] = \"").append(remap.toKey).append("\",\n");
            }
        }

        sb.append("\t--disabled_placeholder\n");
        sb.append("\n},\n\n");
        sb.append("}\n");

        Files.writeString(REMAPS, sb.toString(), StandardCharsets.UTF_8);
    }

    public static java.util.List<Remaps> readRemapsFile() {
        java.util.List<Remaps> remaps = new java.util.ArrayList<>();
        try {
            if (!Files.exists(REMAPS)) return remaps;

            String content = Files.readString(REMAPS);

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

    public static Path getConfigDir() { return CONFIG_DIR; }
    public static Path getConfigFile() { return CONFIG_FILE; }
    public static Path getRemapsFile() { return REMAPS; }
}

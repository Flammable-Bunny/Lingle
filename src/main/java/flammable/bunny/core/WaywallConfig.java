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
    private static final Path TOGGLES = CONFIG_DIR.resolve("toggles.lua");
    private static final Path PATHS = CONFIG_DIR.resolve("paths.lua");
    private static final Path KEYBINDS = CONFIG_DIR.resolve("keybinds.lua");
    private static final Path REMAPS = CONFIG_DIR.resolve("remaps.lua");

    private static Pattern togglePattern(String key) {
        return Pattern.compile("(?m)^(\\s*" + Pattern.quote(key) + "\\s*=\\s*)(true|false)(\\s*,?\\s*)$");
    }

    private static Pattern pathVarPattern(String varName) {
        return Pattern.compile("(?m)^(\\s*" + Pattern.quote(varName) + "\\s*=\\s*home\\s*\\.\\.\\s*\")([^\"]*)(\"\\s*,?\\s*)$");
    }

    private static final Pattern BG_PATH_TOGGLED = Pattern.compile(
            "(?m)^\\s*bg_path\\s*=\\s*toggles\\.toggle_bg_picture\\s*and\\s*\\(\\s*home\\s*\\.\\.\\s*\"([^\"]*)\"\\s*\\)\\s*or\\s*nil\\s*,?\\s*$");

    public static boolean getToggle(String key, boolean def) {
        try {
            if (!Files.exists(TOGGLES)) return def;
            String s = Files.readString(TOGGLES);
            Matcher m = togglePattern(key).matcher(s);
            if (m.find()) {
                return Boolean.parseBoolean(m.group(2));
            }
        } catch (IOException ignored) {}
        return def;
    }

    public static void setToggle(String key, boolean value) throws IOException {
        Files.createDirectories(CONFIG_DIR);
        String content = Files.exists(TOGGLES) ? Files.readString(TOGGLES) : "";
        Matcher m = togglePattern(key).matcher(content);
        String replacement = "$1" + (value ? "true" : "false") + "$3";
        String updated;
        if (m.find()) {
            updated = m.replaceAll(replacement);
        } else {
            String line = key + " = " + (value ? "true" : "false") + ",\n";
            updated = content + (content.endsWith("\n") ? "" : "\n") + line;
        }
        Files.writeString(TOGGLES, updated, StandardCharsets.UTF_8);
    }

    public static Map<String, String> readPaths() {
        Map<String, String> out = new HashMap<>();
        try {
            if (!Files.exists(PATHS)) return out;
            String s = Files.readString(PATHS);
            for (String var : new String[]{"pacem_path", "nb_path", "overlay_path", "lingle_path"}) {
                Matcher m = pathVarPattern(var).matcher(s);
                if (m.find()) out.put(var, m.group(2));
            }
            Matcher bg = BG_PATH_TOGGLED.matcher(s);
            if (bg.find()) {
                out.put("bg_path", bg.group(1));
            }
        } catch (IOException ignored) {}
        return out;
    }

    public static void setPathVar(String varName, String homeRelative) throws IOException {
        Files.createDirectories(CONFIG_DIR);
        String content = Files.exists(PATHS) ? Files.readString(PATHS) : "";

        if ("bg_path".equals(varName)) {
            String toggledLine = "bg_path = toggles.toggle_bg_picture and (home .. \"" + homeRelative + "\") or nil,";
            String updated;
            Matcher mBg = BG_PATH_TOGGLED.matcher(content);
            if (mBg.find()) {
                updated = mBg.replaceAll(Matcher.quoteReplacement(toggledLine));
            } else {
                Matcher any = Pattern.compile("(?m)^\\s*bg_path\\s*=.*$").matcher(content);
                if (any.find()) {
                    updated = any.replaceAll(Matcher.quoteReplacement(toggledLine));
                } else {
                    updated = content + (content.endsWith("\n") ? "" : "\n") + toggledLine + "\n";
                }
            }
            Files.writeString(PATHS, updated, StandardCharsets.UTF_8);
            return;
        }

        Matcher m = pathVarPattern(varName).matcher(content);
        String updated;
        if (m.find()) {
            String replacement = "$1" + Matcher.quoteReplacement(homeRelative) + "$3";
            updated = m.replaceAll(replacement);
        } else {
            String cleanLine = varName + " = home .. \"" + homeRelative + "\",";
            Matcher anyAssign = Pattern.compile("(?m)^\\s*" + Pattern.quote(varName) + "\\s*=.*$").matcher(content);
            if (anyAssign.find()) {
                updated = anyAssign.replaceAll(Matcher.quoteReplacement(cleanLine));
            } else {
                String placeholderToken = switch (varName) {
                    case "pacem_path" -> "pacemanpathplaceholder";
                    case "nb_path" -> "ninjabrainbotpathplaceholder";
                    case "overlay_path" -> "measuringoverlaypathplaceholder";
                    case "lingle_path" -> "linglepathplaceholder";
                    default -> null;
                };
                if (placeholderToken != null && content.contains(placeholderToken)) {
                    updated = content.replace(placeholderToken, homeRelative);
                } else {
                    updated = content + (content.endsWith("\n") ? "" : "\n") + cleanLine + "\n";
                }
            }
        }
        Files.writeString(PATHS, updated, StandardCharsets.UTF_8);
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
        String content = Files.exists(KEYBINDS) ? Files.readString(KEYBINDS) : "";
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
        Files.writeString(KEYBINDS, updated, StandardCharsets.UTF_8);
    }


    public static void setKeybindVar(String varName, String value, boolean withStar, boolean isPlaceholder) throws IOException {
        Files.createDirectories(CONFIG_DIR);
        String content = Files.exists(KEYBINDS) ? Files.readString(KEYBINDS) : "";
        String inner = (withStar ? "*-" : "") + value;
        String line = varName + " = \"" + inner + "\",";
        Pattern varLine = Pattern.compile("(?m)^(\\s*" + Pattern.quote(varName) + "\\s*=\\s*\")(.*?)(\"\\s*,?\\s*)$");
        Matcher m = varLine.matcher(content);
        String updated;
        if (m.find()) {
            updated = m.replaceAll("$1" + Matcher.quoteReplacement(inner) + "$3");
        } else {
            updated = content + (content.endsWith("\n") ? "" : "\n") + line + "\n";
        }
        Files.writeString(KEYBINDS, updated, StandardCharsets.UTF_8);
    }


    public static void readKeybindsFromFile() throws IOException {
        if (!Files.exists(KEYBINDS)) {
            throw new IOException("Existing Config file not found: " + KEYBINDS);
        }

        String content = Files.readString(KEYBINDS);

        Pattern localVarPattern = Pattern.compile("(?m)^\\s*local\\s+(\\w+)\\s*=\\s*\"?([^\"\\n]+?)\"?\\s*,?\\s*$");
        Matcher m = localVarPattern.matcher(content);

        while (m.find()) {
            String varName = m.group(1).toLowerCase();
            String value = m.group(2).trim();

            if (value.startsWith("*-")) value = value.substring(2);
            if (value.toLowerCase().contains("placeholder") || value.isBlank()) continue;

            if (varName.contains("thin")) LingleState.setSetKeybind("Thin_Key", value);
            else if (varName.contains("wide")) LingleState.setSetKeybind("Wide_Key", value);
            else if (varName.contains("tall")) LingleState.setSetKeybind("Tall_Key", value);
            else if (varName.contains("ninbot") || varName.contains("nbb") || varName.contains("show"))
                LingleState.setSetKeybind("NBB_Key", value);
            else if (varName.contains("fullscreen")) LingleState.setSetKeybind("Fullscreen_Key", value);
            else if ((varName.contains("open") || varName.contains("apps")) && !varName.contains("remap"))
                LingleState.setSetKeybind("Apps_Key", value);
            else if (varName.contains("remap")) LingleState.setSetKeybind("Remaps_Key", value);
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
    public static Path getTogglesFile() { return TOGGLES; }
    public static Path getPathsFile() { return PATHS; }
    public static Path getKeybindsFile() { return KEYBINDS; }
    public static Path getRemapsFile() { return REMAPS; }
}

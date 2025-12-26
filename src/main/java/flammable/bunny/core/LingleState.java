package flammable.bunny.core;

import org.json.JSONObject;
import flammable.bunny.core.WorldBopperConfig.KeepWorldInfo;
import flammable.bunny.core.WorldBopperConfig.KeepCondition;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LingleState {
    public static boolean enabled = false;
    public static boolean practiceMaps = false;
    public static int instanceCount = 0;
    public static List<String> linkedInstances = new ArrayList<>();
    public static List<String> selectedPracticeMaps = new ArrayList<>();
    public static boolean adwEnabled = false;
    public static int adwIntervalSeconds = 300;
    public static List<String> WorldbopperSelectedInstances = new ArrayList<>();
    public static boolean worldBopperEnabled = false;
    public static List<KeepWorldInfo> boppableWorlds = new ArrayList<>();
    public static List<Remaps> remaps = new ArrayList<>();
    public static boolean configEditingEnabled = false;

    private static final String[] KEYBIND_NAMES = new String[] {
        "Thin_Key", "Wide_Key", "Tall_Key", "NBB_Key", "Fullscreen_Key", "Apps_Key", "Remaps_Key"
    };
    private static final String NOT_SET = "Not_Set_Yet";
    private static final LinkedHashMap<String, String> setKeybinds = new LinkedHashMap<>();

    private static void ensureDefaultKeybinds() {
        if (setKeybinds.isEmpty()) {
            for (String n : KEYBIND_NAMES) setKeybinds.put(n, NOT_SET);
        } else {
            for (String n : KEYBIND_NAMES) setKeybinds.putIfAbsent(n, NOT_SET);
        }
    }

    private static void ensureDefaultBoppableWorlds() {
        if (boppableWorlds.isEmpty()) {
            boppableWorlds.add(new KeepWorldInfo("Random Speedrun #", KeepCondition.REACHED_NETHER));
            boppableWorlds.add(new KeepWorldInfo("Benchmark Reset #", KeepCondition.ALWAYS_DELETE));
            boppableWorlds.add(new KeepWorldInfo("New World", KeepCondition.ALWAYS_DELETE));
        }
    }

        public static String getSetKeybind(String name) {
            ensureDefaultKeybinds();
            String v = setKeybinds.get(name);
            return (v == null || v.isBlank()) ? NOT_SET : v;
        }

        public static void setSetKeybind(String name, String value) {
            ensureDefaultKeybinds();
            if (name == null) return;
            boolean valid = false;
            for (String n : KEYBIND_NAMES) if (n.equals(name)) { valid = true; break; }
            if (!valid) return;
            setKeybinds.put(name, (value == null || value.isBlank()) ? NOT_SET : value);
            saveState();
        }

    private static Path getConfigFilePath() {
        String userHome = System.getProperty("user.home");
        return Path.of(userHome, ".local", "share", "lingle", "config.json");
    }

    public static void loadState() throws IOException {
        Path cfg = getConfigFilePath();
        ensureDefaultKeybinds();
        boolean needSave = false;
        String s = null;
        if (Files.exists(cfg)) {
            s = Files.readString(cfg);
            enabled = s.contains("\"tmpfs\"") && s.contains("\"enabled\"");
            practiceMaps = s.contains("\"practiceMaps\": true");
            Matcher m = Pattern.compile("\"instanceCount\"\\s*:\\s*(\\d+)").matcher(s);
            if (m.find()) instanceCount = Integer.parseInt(m.group(1));
            linkedInstances.clear();
            Matcher linkedArr = Pattern.compile("\"linkedInstances\"\\s*:\\s*\\[(.*?)]", Pattern.DOTALL).matcher(s);
            if (linkedArr.find()) {
                String inside = linkedArr.group(1);
                Matcher item = Pattern.compile("\"(.*?)\"").matcher(inside);
                while (item.find()) linkedInstances.add(item.group(1));
            }
            selectedPracticeMaps.clear();
            Matcher arr = Pattern.compile("\"selectedMaps\"\\s*:\\s*\\[(.*?)]", Pattern.DOTALL).matcher(s);
            if (arr.find()) {
                String inside = arr.group(1);
                Matcher item = Pattern.compile("\"(.*?)\"").matcher(inside);
                while (item.find()) selectedPracticeMaps.add(item.group(1));
            }
            adwEnabled = s.contains("\"adw\": true");
            Matcher im = Pattern.compile("\"adwInterval\"\\s*:\\s*(\\d+)").matcher(s);
            if (im.find()) adwIntervalSeconds = Math.max(1, Integer.parseInt(im.group(1)));
            WorldbopperSelectedInstances.clear();
            Matcher adwArr = Pattern.compile("\"WorldBopperInstances\"\\s*:\\s*\\[(.*?)]", Pattern.DOTALL).matcher(s);
            if (adwArr.find()) {
                String inside = adwArr.group(1);
                Matcher item = Pattern.compile("\"(.*?)\"").matcher(inside);
                while (item.find()) WorldbopperSelectedInstances.add(item.group(1));
            }
            worldBopperEnabled = s.contains("\"worldBopperEnabled\": true");
            configEditingEnabled = s.contains("\"configEditingEnabled\": true");

            // Load boppable worlds
            boppableWorlds.clear();
            try {
                JSONObject obj = new JSONObject(s);
                if (obj.has("boppableWorlds")) {
                    org.json.JSONArray bwArray = obj.getJSONArray("boppableWorlds");
                    for (int i = 0; i < bwArray.length(); i++) {
                        JSONObject bw = bwArray.getJSONObject(i);
                        String prefix = bw.optString("prefix", "");
                        String conditionStr = bw.optString("condition", "ALWAYS_DELETE");
                        int sizeMB = bw.optInt("minSizeMB", 10);

                        KeepCondition condition = KeepCondition.valueOf(conditionStr);
                        boppableWorlds.add(new KeepWorldInfo(prefix, condition, sizeMB));
                    }
                }
            } catch (Exception ignored) {}

            ensureDefaultBoppableWorlds();

            // Load remaps
            remaps.clear();
            try {
                JSONObject obj = new JSONObject(s);
                if (obj.has("remaps")) {
                    org.json.JSONArray remapsArray = obj.getJSONArray("remaps");
                    for (int i = 0; i < remapsArray.length(); i++) {
                        JSONObject remap = remapsArray.getJSONObject(i);
                        String from = remap.optString("from", "");
                        String to = remap.optString("to", "");
                        boolean perm = remap.optBoolean("permanent", false);
                        if (!from.isEmpty() && !to.isEmpty()) {
                            remaps.add(new Remaps(from, to, perm));
                        }
                    }
                }
            } catch (Exception ignored) {}

            try {
                JSONObject obj = new JSONObject(s);
                if (obj.has("Set_Keybinds") && obj.get("Set_Keybinds") instanceof org.json.JSONArray) {
                    org.json.JSONArray arr2 = obj.getJSONArray("Set_Keybinds");
                    for (int i = 0; i < arr2.length(); i++) {
                        String line = String.valueOf(arr2.get(i));
                        if (line == null) continue;
                        Matcher kv = Pattern.compile("^\\s*([A-Za-z0-9_]+)\\s*=\\s*(.+?)\\s*$").matcher(line);
                        if (kv.find()) {
                            String name = kv.group(1);
                            String val = kv.group(2);
                            setKeybinds.put(name, (val == null || val.isBlank()) ? NOT_SET : val);
                        }
                    }
                    ensureDefaultKeybinds();
                } else {
                    needSave = true;
                }
            } catch (Exception ignored) {
                needSave = true;
            }
        } else {
            needSave = true;
        }

        if (needSave) saveState();
    }

    public static void saveState() {
        try {
            Path cfg = getConfigFilePath();
            Files.createDirectories(cfg.getParent());

            String existing = Files.exists(cfg) ? Files.readString(cfg) : null;
            String gpu = null;
            String distro = null;
            if (existing != null && !existing.isBlank()) {
                try {
                    JSONObject prev = new JSONObject(existing);
                    gpu = prev.optString("gpu", null);
                    distro = prev.optString("distro", null);
                } catch (Exception ignored) {}
            }
            if (gpu == null || gpu.isBlank()) gpu = DistroDetector.getGPU();
            if (distro == null || distro.isBlank()) distro = DistroDetector.getDistro();

            Map<String, Object> ordered = new LinkedHashMap<>();
            if (gpu != null) ordered.put("gpu", gpu);
            if (distro != null) ordered.put("distro", distro);
            ordered.put("tmpfs", enabled ? "enabled" : "disabled");
            ordered.put("instanceCount", instanceCount);
            org.json.JSONArray linkedArr = new org.json.JSONArray();
            for (String inst : linkedInstances) linkedArr.put(inst);
            ordered.put("linkedInstances", linkedArr);
            ordered.put("practiceMaps", practiceMaps);
            org.json.JSONArray arr = new org.json.JSONArray();
            for (String s : selectedPracticeMaps) arr.put(s);
            ordered.put("selectedMaps", arr);
            ordered.put("adw", adwEnabled);
            ordered.put("adwInterval", Math.max(1, adwIntervalSeconds));
            org.json.JSONArray wbInstArr = new org.json.JSONArray();
            for (String inst : WorldbopperSelectedInstances) wbInstArr.put(inst);
            ordered.put("WorldBopperInstances", wbInstArr);
            ordered.put("worldBopperEnabled", worldBopperEnabled);
            ordered.put("configEditingEnabled", configEditingEnabled);

            // Save boppable worlds
            ensureDefaultBoppableWorlds();
            org.json.JSONArray bwArray = new org.json.JSONArray();
            for (KeepWorldInfo info : boppableWorlds) {
                JSONObject bw = new JSONObject();
                bw.put("prefix", info.prefix);
                bw.put("condition", info.condition.name());
                bw.put("minSizeMB", info.minSizeMB);
                bwArray.put(bw);
            }
            ordered.put("boppableWorlds", bwArray);

            // Save remaps
            org.json.JSONArray remapsArray = new org.json.JSONArray();
            for (Remaps remap : remaps) {
                JSONObject rm = new JSONObject();
                rm.put("from", remap.fromKey);
                rm.put("to", remap.toKey);
                rm.put("permanent", remap.isPermanent);
                remapsArray.put(rm);
            }
            ordered.put("remaps", remapsArray);

            ensureDefaultKeybinds();
            org.json.JSONArray keyArr = new org.json.JSONArray();
            for (String name : KEYBIND_NAMES) {
                String val = getSetKeybind(name);
                keyArr.put(name + " = " + val);
            }
            ordered.put("Set_Keybinds", keyArr);

            JSONObject out = new JSONObject(ordered);
            Files.writeString(cfg, out.toString(2), StandardCharsets.UTF_8);
        } catch (IOException ignored) {}
    }
}

package flammable.bunny.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LingleState {
    public static boolean enabled = false;
    public static boolean practiceMaps = false;
    public static int instanceCount = 0;
    public static List<String> selectedPracticeMaps = new ArrayList<>();
    public static boolean adwEnabled = false;
    public static int adwIntervalSeconds = 300;

    private static Path getConfigFilePath() {
        String userHome = System.getProperty("user.home");
        return Path.of(userHome, ".local", "share", "lingle", "config.json");
    }

    public static void loadState() throws IOException {
        Path cfg = getConfigFilePath();
        if (!Files.exists(cfg)) return;
        String s = Files.readString(cfg);
        enabled = s.contains("\"tmpfs\"") && s.contains("\"enabled\"");
        practiceMaps = s.contains("\"practiceMaps\": true");
        Matcher m = Pattern.compile("\"instanceCount\"\\s*:\\s*(\\d+)").matcher(s);
        if (m.find()) instanceCount = Integer.parseInt(m.group(1));
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
    }

    public static void saveState() {
        try {
            Path cfg = getConfigFilePath();
            Files.createDirectories(cfg.getParent());
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            sb.append("  \"tmpfs\": \"").append(enabled ? "enabled" : "disabled").append("\",\n");
            sb.append("  \"instanceCount\": ").append(instanceCount).append(",\n");
            sb.append("  \"practiceMaps\": ").append(practiceMaps).append(",\n");
            sb.append("  \"selectedMaps\": [");
            for (int i = 0; i < selectedPracticeMaps.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append("\"").append(selectedPracticeMaps.get(i)).append("\"");
            }
            sb.append("],\n");
            sb.append("  \"adw\": ").append(adwEnabled).append(",\n");
            sb.append("  \"adwInterval\": ").append(Math.max(1, adwIntervalSeconds)).append("\n");
            sb.append("}\n");
            Files.writeString(cfg, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {}
    }
}

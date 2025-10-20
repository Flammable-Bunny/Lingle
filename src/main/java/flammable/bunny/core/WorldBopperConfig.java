package flammable.bunny.core;

public class WorldBopperConfig {

    public enum KeepCondition {
        ALWAYS_DELETE("Always delete"),
        REACHED_NETHER("Reached Nether"),
        REACHED_BASTION("Reached Bastion"),
        REACHED_FORTRESS("Reached Fortress"),
        REACHED_STRONGHOLD("Reached Stronghold"),
        REACHED_END("Reached End"),
        WORLD_SIZE("World size (MB)");

        private final String displayName;

        KeepCondition(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static KeepCondition fromDisplayName(String name) {
            for (KeepCondition condition : values()) {
                if (condition.displayName.equals(name)) {
                    return condition;
                }
            }
            return ALWAYS_DELETE;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public static class KeepWorldInfo {
        public String prefix;
        public KeepCondition condition;
        public int minSizeMB;

        public KeepWorldInfo() {
            this.prefix = "";
            this.condition = KeepCondition.ALWAYS_DELETE;
            this.minSizeMB = 10;
        }

        public KeepWorldInfo(String prefix, KeepCondition condition) {
            this.prefix = prefix;
            this.condition = condition;
            this.minSizeMB = 10;
        }

        public KeepWorldInfo(String prefix, KeepCondition condition, int minSizeMB) {
            this.prefix = prefix;
            this.condition = condition;
            this.minSizeMB = minSizeMB;
        }
    }
}

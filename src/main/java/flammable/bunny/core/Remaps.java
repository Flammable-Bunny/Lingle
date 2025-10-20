package flammable.bunny.core;

public class Remaps {
    public String fromKey;
    public String toKey;
    public boolean isPermanent;

    public Remaps() {
        this.fromKey = "";
        this.toKey = "";
        this.isPermanent = false;
    }

    public Remaps(String fromKey, String toKey, boolean isPermanent) {
        this.fromKey = fromKey;
        this.toKey = toKey;
        this.isPermanent = isPermanent;
    }

    @Override
    public String toString() {
        return fromKey + " → " + toKey + (isPermanent ? " (always)" : "");
    }
}

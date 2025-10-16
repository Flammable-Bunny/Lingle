package flammable.bunny.core;

public class ErrorCodes {
    public static final int SUCCESS = 0;
    public static final int GENERAL_ERROR = 1;
    public static final int MISUSE = 2;
    public static final int CONFIG_ERROR = 3;
    public static final int IO_ERROR = 4;
    public static final int PERMISSION_ERROR = 5;
    public static final int NETWORK_ERROR = 6;
    public static final int DEPENDENCY_ERROR = 7;
    public static final int STATE_ERROR = 8;
    public static final int UPDATE_ERROR = 9;
    public static final int TMPFS_ERROR = 10;
    public static final int SYMLINK_ERROR = 11;
    public static final int ADW_ERROR = 12;
    public static final int INSTANCE_ERROR = 13;


    public static void exit(int code, String message) {
        if (code != SUCCESS) {
            System.err.println("[ERROR " + code + "] " + message);
        }
        System.exit(code);
    }


    public static void exitWithDialog(java.awt.Window parent, int code, String title, String message) {
        flammable.bunny.ui.UIUtils.showDarkMessage(parent, title, message + "\n\nError code: " + code);
        exit(code, title + ": " + message);
    }


    public static void showError(java.awt.Window parent, int code, String message) {
        flammable.bunny.ui.UIUtils.showDarkMessage(parent, "Error " + code, message);
        System.err.println("[ERROR " + code + "] " + message);
    }


    public static String getDescription(int code) {
        return switch (code) {
            case SUCCESS -> "Success";
            case GENERAL_ERROR -> "General error";
            case MISUSE -> "Invalid usage";
            case CONFIG_ERROR -> "Configuration error";
            case IO_ERROR -> "I/O error";
            case PERMISSION_ERROR -> "Permission denied";
            case NETWORK_ERROR -> "Network error";
            case DEPENDENCY_ERROR -> "Missing dependency";
            case STATE_ERROR -> "Invalid state";
            case UPDATE_ERROR -> "Update failed";
            case TMPFS_ERROR -> "tmpfs operation failed";
            case SYMLINK_ERROR -> "Symlink operation failed";
            case ADW_ERROR -> "Auto Delete World failed";
            case INSTANCE_ERROR -> "Instance management failed";
            default -> "Unknown error";
        };
    }
}

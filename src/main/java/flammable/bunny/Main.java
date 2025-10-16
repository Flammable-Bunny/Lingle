package flammable.bunny;

import flammable.bunny.core.*;
import flammable.bunny.ui.*;

import javax.swing.*;
import java.io.IOException;
import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.SwingUtilities;


public class Main {
    public static void main(String[] args) {

        FlatDarkLaf.setup();
        DistroDetector.detectAndSaveDistro();

        boolean nogui = args.length > 0 && "--nogui".equals(args[0]);

        if (!nogui && System.getenv("DISPLAY") == null && System.getenv("WAYLAND_DISPLAY") == null) {
            ErrorCodes.exit(ErrorCodes.MISUSE, "No DISPLAY/WAYLAND_DISPLAY found. This GUI requires a graphical session.");
        }

        try {
            TmpfsScriptManager.ensureScriptsPresent();
        } catch (IOException e) {
            String msg = "Failed to create scripts: " + e.getMessage();
            if (nogui) {
                ErrorCodes.exit(ErrorCodes.IO_ERROR, msg);
            } else {
                ErrorCodes.exitWithDialog(null, ErrorCodes.IO_ERROR, "Initialization Error", msg);
            }
        }

        try {
            LingleState.loadState();
        } catch (IOException e) {
            String msg = "Failed to load configuration: " + e.getMessage();
            if (nogui) {
                ErrorCodes.exit(ErrorCodes.CONFIG_ERROR, msg);
            } else {
                ErrorCodes.exitWithDialog(null, ErrorCodes.CONFIG_ERROR, "Configuration Error", msg);
            }
        }

        try {
            LinkInstancesService.preparePracticeMapLinks();
        } catch (IOException e) {
            String msg = "Failed to prepare practice map links: " + e.getMessage();
            if (!nogui) {
                ErrorCodes.showError(null, ErrorCodes.SYMLINK_ERROR, msg);
            } else {
                System.err.println("[WARNING] " + msg);
            }
        }

        Updater.checkForUpdates();

        try {
            AdwManager.startAdwIfNeeded();
        } catch (Exception e) {
            String msg = "Failed to start ADW service: " + e.getMessage();
            if (!nogui) {
                ErrorCodes.showError(null, ErrorCodes.ADW_ERROR, msg);
            } else {
                System.err.println("[WARNING] " + msg);
            }
        }

        Runtime.getRuntime().addShutdownHook(new Thread(AdwManager::stopAdwQuietly));

        if (nogui) {
            System.out.println("Running Lingle in nogui mode");
            try {
                Thread.currentThread().join();
            } catch (InterruptedException ignored) {}
            return;
        }

        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        SwingUtilities.invokeLater(LingleUI::new);
    }
}

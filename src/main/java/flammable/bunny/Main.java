package flammable.bunny;

import flammable.bunny.core.*;
import flammable.bunny.ui.*;

import javax.swing.*;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        boolean nogui = args.length > 0 && "--nogui".equals(args[0]);
        try {
            TmpfsScriptManager.ensureScriptsPresent();
            LingleState.loadState();
            LinkInstancesService.preparePracticeMapLinks();
            Updater.checkForUpdates();
            AdwManager.startAdwIfNeeded();
        } catch (IOException e) {
            if (nogui) {
                e.printStackTrace(System.err);
            } else {
                UIUtils.showDarkMessage(null, "Lingle", "Initial Error: " + e.getMessage());
            }
            System.exit(1);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(AdwManager::stopAdwQuietly));

        if (nogui) {
            System.out.println("Running Lingle in nogui mode");
            try {
                Thread.currentThread().join();
            } catch (InterruptedException ignored) {}
            return;
        }

        if (System.getenv("DISPLAY") == null && System.getenv("WAYLAND_DISPLAY") == null) {
            System.err.println("No DISPLAY/WAYLAND_DISPLAY found. This GUI requires a graphical session.");
            System.exit(1);
        }

        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        SwingUtilities.invokeLater(LingleUI::new);
    }
}

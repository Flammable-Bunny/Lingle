package flammable.bunny.core;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class ElevatedInstaller {

    private static Process rootShellProcess;
    private static BufferedWriter shellWriter;
    private static BufferedReader shellReader;
    private static final Object SHELL_LOCK = new Object();
    private static boolean initialized = false;

    private static void ensureRootShell() throws IOException {
        synchronized (SHELL_LOCK) {
            if (rootShellProcess != null && rootShellProcess.isAlive()) return;

            ProcessBuilder pb = new ProcessBuilder("pkexec", "bash", "-s");
            pb.redirectErrorStream(true);
            rootShellProcess = pb.start();
            shellWriter = new BufferedWriter(new OutputStreamWriter(rootShellProcess.getOutputStream(), StandardCharsets.UTF_8));
            shellReader = new BufferedReader(new InputStreamReader(rootShellProcess.getInputStream(), StandardCharsets.UTF_8));

            try {
                shellWriter.write("set -m\n");
                shellWriter.flush();
            } catch (IOException ignored) {}

            if (!initialized) {
                initialized = true;
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try {
                        if (shellWriter != null) {
                            shellWriter.write("exit\n");
                            shellWriter.flush();
                        }
                    } catch (IOException ignored) {
                    }
                    if (rootShellProcess != null) rootShellProcess.destroy();
                }));
            }
        }
    }


    public static int runElevatedBash(String command) throws IOException, InterruptedException {
        ensureRootShell();
        String marker = "__CMD_DONE__" + System.nanoTime();
        String exitMarker = "__EXIT_CODE__" + System.nanoTime();

        String wrapped = "{ " + command + "; }; echo " + exitMarker + ":$?; echo " + marker + "\n";

        synchronized (SHELL_LOCK) {
            shellWriter.write(wrapped);
            shellWriter.flush();

            int exitCode = -1;
            String line;
            while ((line = shellReader.readLine()) != null) {
                if (line.startsWith(exitMarker + ":")) {
                    try {
                        exitCode = Integer.parseInt(line.substring((exitMarker + ":").length()).trim());
                    } catch (NumberFormatException ignored) {
                        exitCode = -1;
                    }
                }
                if (line.equals(marker)) {
                    break;
                }
            }

            return exitCode;
        }
    }


    public static int runElevated(String... command) throws IOException, InterruptedException {
        String[] elevatedCommand = new String[command.length + 1];
        elevatedCommand[0] = "pkexec";
        System.arraycopy(command, 0, elevatedCommand, 1, command.length);
        ProcessBuilder pb = new ProcessBuilder(elevatedCommand);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        return p.waitFor();
    }
}

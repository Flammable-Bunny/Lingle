package flammable.bunny.core;

import javax.swing.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class LingleLogger {
    private static final List<String> logEntries = new ArrayList<>();
    private static final List<JTextArea> listeners = new ArrayList<>();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void log(String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        String logEntry = "[" + timestamp + "] " + message;
        
        synchronized (logEntries) {
            logEntries.add(logEntry);
        }
        
        // Update all registered text areas
        SwingUtilities.invokeLater(() -> {
            synchronized (listeners) {
                for (JTextArea listener : listeners) {
                    listener.append(logEntry + "\n");
                    listener.setCaretPosition(listener.getDocument().getLength());
                }
            }
        });
    }

    public static void logAction(String action) {
        log("ACTION: " + action);
    }

    public static void logSuccess(String message) {
        log("SUCCESS: " + message);
    }

    public static void logError(String message) {
        log("ERROR: " + message);
    }

    public static void logError(String message, Exception e) {
        log("ERROR: " + message + " - " + e.getClass().getSimpleName() + ": " + e.getMessage());
        // Log stack trace for detailed debugging
        StringBuilder stackTrace = new StringBuilder();
        for (StackTraceElement element : e.getStackTrace()) {
            stackTrace.append("    at ").append(element.toString()).append("\n");
        }
        if (stackTrace.length() > 0) {
            log("Stack trace:\n" + stackTrace.toString().trim());
        }
        // Log cause if present
        if (e.getCause() != null) {
            log("Caused by: " + e.getCause().getClass().getSimpleName() + ": " + e.getCause().getMessage());
        }
    }

    public static void logInfo(String message) {
        log("INFO: " + message);
    }

    public static void logCommand(String command) {
        log("COMMAND: " + command);
    }

    public static void logOutput(String output) {
        // Log output line by line, preserving terminal-like formatting
        if (output != null && !output.isEmpty()) {
            for (String line : output.split("\n")) {
                log("OUTPUT: " + line);
            }
        }
    }

    public static void registerListener(JTextArea textArea) {
        // Add existing logs to the new listener BEFORE adding to listeners list
        // This prevents double printing
        SwingUtilities.invokeLater(() -> {
            synchronized (logEntries) {
                for (String entry : logEntries) {
                    textArea.append(entry + "\n");
                }
                textArea.setCaretPosition(textArea.getDocument().getLength());
            }

            // Only add to listeners AFTER replay is complete
            synchronized (listeners) {
                listeners.add(textArea);
            }
        });
    }

    public static void unregisterListener(JTextArea textArea) {
        synchronized (listeners) {
            listeners.remove(textArea);
        }
    }

    public static String getAllLogs() {
        synchronized (logEntries) {
            return String.join("\n", logEntries);
        }
    }

    public static void clear() {
        synchronized (logEntries) {
            logEntries.clear();
        }
        
        SwingUtilities.invokeLater(() -> {
            synchronized (listeners) {
                for (JTextArea listener : listeners) {
                    listener.setText("");
                }
            }
        });
    }
}

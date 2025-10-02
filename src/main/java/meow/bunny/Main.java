package meow.bunny;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    private static volatile boolean enabled = false;
    private static boolean practiceMaps = false;
    private static int instanceCount = 0;
    private static List<String> selectedPracticeMaps = new ArrayList<>();

    private static boolean adwEnabled = false;
    private static int adwIntervalSeconds = 300;
    private static Process adwProcess = null;

    private static JButton runButton;
    private static long lastClickTime = 0;

    private static final Color BG = new Color(64, 64, 64);
    private static final Color BTN_BG = new Color(80, 80, 80);
    private static final Color BTN_BORDER = new Color(150, 150, 150);
    private static final Color BTN_HOVER = new Color(70, 90, 130);
    private static final Color BTN_HOVER_BORDER = new Color(100, 150, 200);
    private static final Color BTN_SELECTED = new Color(75, 95, 140);
    private static final Color TXT = Color.WHITE;
    private static final Font UI_FONT = new Font("SansSerif", Font.PLAIN, 13);
    private static final Font UI_FONT_BOLD = new Font("SansSerif", Font.BOLD, 14);
    private static final int ROW_H = 22;

    public static void main(String[] args) {
        boolean nogui = args.length > 0 && "--nogui".equals(args[0]);

        try {
            ensureScriptsPresent();
            detectCurrentState();
            createInitialDirectories();
            startAdwIfNeeded();
        } catch (IOException e) {
            if (nogui) {
                e.printStackTrace(System.err);
            } else {
                showDarkMessage(null, "Lingle", "Initial Error: " + e.getMessage());
            }
            System.exit(1);
        }


        Runtime.getRuntime().addShutdownHook(new Thread(Main::stopAdwQuietly));

        if (nogui) {
            System.out.println("Running Lingle in nogui mode");
            try {
                Thread.currentThread().join();
            } catch (InterruptedException ignored) {}
            return;
        }

        // GUI path
        if (System.getenv("DISPLAY") == null && System.getenv("WAYLAND_DISPLAY") == null) {
            System.err.println("No DISPLAY/WAYLAND_DISPLAY found. This GUI requires a graphical session.");
            System.exit(1);
        }

        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        SwingUtilities.invokeLater(Main::createAndShowUI);
    }


    private static void createInitialDirectories() throws IOException {
        Path home = Path.of(System.getProperty("user.home"));
        Path targetDir = home.resolve("Lingle");

        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }

        try {
            Files.setPosixFilePermissions(targetDir,
                    java.util.Set.of(
                            java.nio.file.attribute.PosixFilePermission.OWNER_READ,
                            java.nio.file.attribute.PosixFilePermission.OWNER_WRITE,
                            java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE
                    )
            );
        } catch (UnsupportedOperationException e) {
        }
    }


    private static void applyNormal(JButton b) {
        b.setBackground(BTN_BG);
        b.setBorder(BorderFactory.createLineBorder(BTN_BORDER, 2));
        b.setForeground(TXT);
        b.setFont(UI_FONT);
    }

    private static void applyHover(JButton b) {
        b.setBackground(BTN_HOVER);
        b.setBorder(BorderFactory.createLineBorder(BTN_HOVER_BORDER, 2));
        b.setForeground(TXT);
    }

    private static void applySelected(JButton b) {
        b.setBackground(BTN_SELECTED);
        b.setBorder(BorderFactory.createLineBorder(BTN_HOVER_BORDER, 2));
        b.setForeground(TXT);
    }

    private static void styleWithHover(JButton b) {
        applyNormal(b);
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { applyHover(b); }
            @Override public void mouseExited(MouseEvent e) { applyNormal(b); }
        });
    }

    private static void styleToggleWithHover(AbstractButton b, java.util.function.BooleanSupplier isSelected) {
        b.setFont(UI_FONT);
        if (isSelected.getAsBoolean()) {
            b.setBackground(BTN_SELECTED);
            b.setBorder(BorderFactory.createLineBorder(BTN_HOVER_BORDER, 2));
        } else {
            b.setBackground(BTN_BG);
            b.setBorder(BorderFactory.createLineBorder(BTN_BORDER, 2));
        }
        b.setForeground(TXT);
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { b.setBackground(BTN_HOVER); b.setBorder(BorderFactory.createLineBorder(BTN_HOVER_BORDER, 2)); }
            @Override public void mouseExited(MouseEvent e) {
                if (isSelected.getAsBoolean()) {
                    b.setBackground(BTN_SELECTED);
                    b.setBorder(BorderFactory.createLineBorder(BTN_HOVER_BORDER, 2));
                } else {
                    b.setBackground(BTN_BG);
                    b.setBorder(BorderFactory.createLineBorder(BTN_BORDER, 2));
                }
            }
        });
    }

    private static void createAndShowUI() {
        JFrame frame = new JFrame("Lingle");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setAlwaysOnTop(true);
        frame.setType(Window.Type.UTILITY);
        frame.setResizable(false);
        frame.setUndecorated(true);

        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(new Color(45, 45, 45));
        titleBar.setPreferredSize(new Dimension(0, 25));

        JLabel titleLabel = new JLabel("Lingle");
        titleLabel.setForeground(TXT);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 5));

        JButton closeButton = new JButton("×");
        closeButton.setBackground(new Color(45, 45, 45));
        closeButton.setForeground(TXT);
        closeButton.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        closeButton.setFocusPainted(false);
        closeButton.addActionListener(e -> System.exit(0));
        closeButton.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { closeButton.setBackground(Color.RED); }
            @Override public void mouseExited(MouseEvent e) { closeButton.setBackground(new Color(45, 45, 45)); }
        });

        titleBar.add(titleLabel, BorderLayout.WEST);
        titleBar.add(closeButton, BorderLayout.EAST);

        final Point[] dragOffset = new Point[1];
        titleBar.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { dragOffset[0] = e.getPoint(); }
        });
        titleBar.addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                Point p = frame.getLocation();
                p.translate(e.getX() - dragOffset[0].x, e.getY() - dragOffset[0].y);
                frame.setLocation(p);
            }
        });

        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        navPanel.setBackground(BG);
        navPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(100, 100, 100)));

        JButton tmpfsNavButton = new JButton("auToMPFS");
        JButton settingsNavButton = new JButton(" ");
        for (JButton btn : new JButton[]{tmpfsNavButton, settingsNavButton}) {
            btn.setBackground(BTN_BG);
            btn.setForeground(TXT);
            btn.setBorder(BorderFactory.createLineBorder(BTN_BORDER, 1));
            btn.setFocusPainted(false);
            btn.setFont(UI_FONT);
            btn.setPreferredSize(new Dimension(80, 35));
            btn.addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { btn.setBackground(BTN_HOVER); btn.setBorder(BorderFactory.createLineBorder(BTN_HOVER_BORDER, 1)); }
                @Override public void mouseExited(MouseEvent e) { btn.setBackground(BTN_BG); btn.setBorder(BorderFactory.createLineBorder(BTN_BORDER, 1)); }
            });
        }
        navPanel.add(tmpfsNavButton);
        navPanel.add(settingsNavButton);

        CardLayout cardLayout = new CardLayout();
        JPanel contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(BG);

        runButton = new JButton(buttonText());
        runButton.setFocusPainted(false);
        runButton.setFont(UI_FONT);
        runButton.setPreferredSize(new Dimension(100, 35));
        if (enabled) applySelected(runButton); else applyNormal(runButton);
        runButton.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { applyHover(runButton); }
            @Override public void mouseExited(MouseEvent e) { if (enabled) applySelected(runButton); else applyNormal(runButton); }
        });
        runButton.addActionListener(e -> runToggleScriptAsync());

        JPanel tmpfsPanel = new JPanel(new BorderLayout());
        tmpfsPanel.setBackground(BG);

        JPanel buttonSection = new JPanel(null);
        buttonSection.setBackground(BG);
        buttonSection.setPreferredSize(new Dimension(0, 80));

        JLabel tmpfsLabel = new JLabel("Enable TMPFS");
        tmpfsLabel.setForeground(TXT);
        tmpfsLabel.setFont(UI_FONT_BOLD);
        tmpfsLabel.setBounds(10, 10, 150, 25);
        buttonSection.add(tmpfsLabel);

        runButton.setBounds(10, 35, 100, 35);
        buttonSection.add(runButton);

        JPanel instancesChecks = new JPanel();
        instancesChecks.setLayout(new BoxLayout(instancesChecks, BoxLayout.Y_AXIS));
        instancesChecks.setBackground(BG);

        Path home = Path.of(System.getProperty("user.home"));
        Path prismInstancesDir = home.resolve(".local").resolve("share").resolve("PrismLauncher").resolve("instances");

        int instRows = 0;
        try {
            if (Files.exists(prismInstancesDir) && Files.isDirectory(prismInstancesDir)) {
                for (Path dir : (Iterable<Path>) Files.list(prismInstancesDir)
                        .filter(Files::isDirectory)
                        .sorted(Comparator.comparing(p -> p.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
                        ::iterator) {
                    JCheckBox cb = new JCheckBox(dir.getFileName().toString());
                    cb.setBackground(BG);
                    cb.setForeground(TXT);
                    cb.setFont(UI_FONT);
                    cb.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
                    cb.setAlignmentX(Component.LEFT_ALIGNMENT);
                    instancesChecks.add(cb);
                    instRows++;
                }
            }
            if (instRows == 0) {
                JLabel none = new JLabel("No instances found");
                none.setForeground(Color.LIGHT_GRAY);
                none.setFont(new Font("SansSerif", Font.ITALIC, 12));
                instancesChecks.add(none);
                instRows = 1;
            }
        } catch (IOException e) {
            JLabel err = new JLabel("Error reading instances directory");
            err.setForeground(Color.RED);
            err.setFont(new Font("SansSerif", Font.ITALIC, 12));
            instancesChecks.add(err);
            instRows = 1;
        }

        JScrollPane instancesScroll = new JScrollPane(instancesChecks);
        instancesScroll.setBackground(BG);
        instancesScroll.getViewport().setBackground(BG);
        instancesScroll.setBorder(BorderFactory.createEmptyBorder());
        int instPrefH = Math.min(instRows, 6) * ROW_H;
        Dimension instDim = new Dimension(Integer.MAX_VALUE, instPrefH);
        instancesScroll.setPreferredSize(instDim);
        instancesScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 6 * ROW_H));
        instancesScroll.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel instancesLabel = new JLabel("Instances:");
        instancesLabel.setForeground(TXT);
        instancesLabel.setFont(UI_FONT_BOLD);
        instancesLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        instancesLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton symlinkButton = new JButton("Symlink Instances");
        symlinkButton.setFocusPainted(false);
        symlinkButton.setFont(UI_FONT);
        symlinkButton.setPreferredSize(new Dimension(150, 35));
        styleWithHover(symlinkButton);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        buttonRow.setBackground(BG);
        buttonRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonRow.add(symlinkButton);

        symlinkButton.addActionListener(e -> {
            List<JCheckBox> selected = new ArrayList<>();
            for (Component c : instancesChecks.getComponents()) {
                if (c instanceof JCheckBox cb && cb.isSelected()) selected.add(cb);
            }
            if (selected.isEmpty()) {
                showDarkMessage(frame, "No Instances Selected", "Please select at least one instance.");
                return;
            }
            int choice = showConfirmDark(frame
            );
            if (choice != 0) return;

            try {
                int idx = 1;
                for (JCheckBox cb : selected) {
                    Path lingleDir = home.resolve("Lingle").resolve(String.valueOf(idx++));
                    if (!Files.exists(lingleDir)) Files.createDirectories(lingleDir);
                    String instance = cb.getText();
                    Path savesPath = home.resolve(".local/share/PrismLauncher/instances")
                            .resolve(instance).resolve("minecraft/saves");
                    if (Files.exists(savesPath)) {
                        try {
                            Files.walk(savesPath)
                                    .sorted(Comparator.reverseOrder())
                                    .forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
                        } catch (IOException ignored) {}
                    }
                    try {
                        Files.createSymbolicLink(savesPath, lingleDir);
                    } catch (FileAlreadyExistsException ignored) {}
                }
                instanceCount = selected.size();
                saveCurrentState();
            } catch (IOException ex) {
                showDarkMessage(frame, "Error Code 1", "Failed to create symbolic links: " + ex.getMessage());
            }
        });

        JLabel savesLabel = new JLabel("Practice Maps:");
        savesLabel.setForeground(TXT);
        savesLabel.setFont(UI_FONT_BOLD);
        savesLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        savesLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel savesChecks = new JPanel();
        savesChecks.setLayout(new BoxLayout(savesChecks, BoxLayout.Y_AXIS));
        savesChecks.setBackground(BG);

        Path savesDir = home.resolve(".local").resolve("share").resolve("lingle").resolve("saves");
        try { if (!Files.exists(savesDir)) Files.createDirectories(savesDir); } catch (IOException ignored) {}

        int saveRows = 0;
        try {
            if (Files.exists(savesDir) && Files.isDirectory(savesDir)) {
                for (Path dir : (Iterable<Path>) Files.list(savesDir)
                        .filter(Files::isDirectory)
                        .sorted(Comparator.comparing(p -> p.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
                        ::iterator) {
                    JCheckBox cb = new JCheckBox(dir.getFileName().toString());
                    cb.setBackground(BG);
                    cb.setForeground(TXT);
                    cb.setFont(UI_FONT);
                    cb.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
                    cb.setAlignmentX(Component.LEFT_ALIGNMENT);
                    savesChecks.add(cb);
                    saveRows++;
                }
            }
            if (saveRows == 0) {
                JLabel none = new JLabel("No practice maps found");
                none.setForeground(Color.LIGHT_GRAY);
                none.setFont(new Font("SansSerif", Font.ITALIC, 12));
                savesChecks.add(none);
                saveRows = 1;
            }
        } catch (IOException e) {
            JLabel err = new JLabel("Error reading practice maps directory");
            err.setForeground(Color.RED);
            err.setFont(new Font("SansSerif", Font.ITALIC, 12));
            savesChecks.add(err);
            saveRows = 1;
        }

        JScrollPane savesScroll = new JScrollPane(savesChecks);
        savesScroll.setBackground(BG);
        savesScroll.getViewport().setBackground(BG);
        savesScroll.setBorder(BorderFactory.createEmptyBorder());
        int savesPrefH = Math.min(saveRows, 11) * ROW_H;
        Dimension savesDim = new Dimension(Integer.MAX_VALUE, savesPrefH);
        savesScroll.setPreferredSize(savesDim);
        savesScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 11 * ROW_H));
        savesScroll.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton linkPracticeBtn = new JButton("Link Practice Maps");
        linkPracticeBtn.setFocusPainted(false);
        linkPracticeBtn.setFont(UI_FONT);
        linkPracticeBtn.setPreferredSize(new Dimension(170, 35));
        styleWithHover(linkPracticeBtn);

        JPanel mapsButtonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        mapsButtonRow.setBackground(BG);
        mapsButtonRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        mapsButtonRow.add(linkPracticeBtn);

        linkPracticeBtn.addActionListener(e -> {
            try {
                List<String> chosen = new ArrayList<>();
                for (Component c : savesChecks.getComponents()) {
                    if (c instanceof JCheckBox cb && cb.isSelected()) {
                        String name = cb.getText();
                        String withZ = name.startsWith("Z_") ? name : "Z_" + name;
                        if (!name.equals(withZ)) {
                            Path from = savesDir.resolve(name);
                            Path to = savesDir.resolve(withZ);
                            if (!Files.exists(to)) Files.move(from, to);
                            cb.setText(withZ);
                        }
                        chosen.add(withZ);
                    }
                }
                if (chosen.isEmpty()) {
                    showDarkMessage(frame, "No Selection", "Please select at least one practice map.");
                    return;
                }
                selectedPracticeMaps = chosen;
                practiceMaps = true;
                saveCurrentState();

                try {
                    linkPracticeMapsNow();
                } catch (IOException ex) {
                    showDarkMessage(frame, "Error", "Linking failed: " + ex.getMessage());
                }
            } catch (IOException ex) {
                showDarkMessage(frame, "Error", "Failed to prepare practice maps: " + ex.getMessage());
            }
        });

        JLabel adwLabel = new JLabel("Auto Delete Worlds:");
        adwLabel.setForeground(TXT);
        adwLabel.setFont(UI_FONT_BOLD);
        adwLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        adwLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JToggleButton adwToggle = new JToggleButton("Auto Delete Worlds");
        adwToggle.setSelected(adwEnabled);
        adwToggle.setFocusPainted(false);
        adwToggle.setPreferredSize(new Dimension(170, 35));
        styleToggleWithHover(adwToggle, adwToggle::isSelected);

        JTextField adwIntervalField = new JTextField(String.valueOf(Math.max(1, adwIntervalSeconds)), 6);
        adwIntervalField.setPreferredSize(new Dimension(70, 30));
        adwIntervalField.setFont(UI_FONT);

        JButton adwApplyBtn = new JButton("Apply");
        adwApplyBtn.setFocusPainted(false);
        adwApplyBtn.setPreferredSize(new Dimension(90, 30));
        styleWithHover(adwApplyBtn);

        JLabel secondsLbl = new JLabel("seconds");
        secondsLbl.setForeground(TXT);
        secondsLbl.setFont(UI_FONT);

        JPanel adwRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        adwRow.setBackground(BG);
        adwRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        adwRow.add(adwToggle);
        JLabel intervalLabel = new JLabel("Interval:");
        intervalLabel.setForeground(TXT);
        intervalLabel.setFont(UI_FONT);
        adwRow.add(intervalLabel);
        adwRow.add(adwIntervalField);
        adwRow.add(secondsLbl);
        adwRow.add(adwApplyBtn);

        adwToggle.addActionListener(e -> {
            adwEnabled = adwToggle.isSelected();
            if (adwEnabled) { adwToggle.setBackground(BTN_SELECTED); adwToggle.setBorder(BorderFactory.createLineBorder(BTN_HOVER_BORDER, 2)); }
            else { adwToggle.setBackground(BTN_BG); adwToggle.setBorder(BorderFactory.createLineBorder(BTN_BORDER, 2)); }
            int parsed = parsePositiveInt(adwIntervalField.getText(), adwIntervalSeconds);
            adwIntervalSeconds = Math.max(1, parsed);
            saveCurrentState();
            if (adwEnabled) startAdwIfNeeded(); else stopAdwQuietly();
        });

        adwApplyBtn.addActionListener(e -> {
            int parsed = parsePositiveInt(adwIntervalField.getText(), adwIntervalSeconds);
            adwIntervalSeconds = Math.max(1, parsed);
            saveCurrentState();
            if (adwEnabled) {
                stopAdwQuietly();
                startAdwIfNeeded();
            }
        });

        JPanel mainListContainer = new JPanel();
        mainListContainer.setLayout(new BoxLayout(mainListContainer, BoxLayout.Y_AXIS));
        mainListContainer.setBackground(BG);
        mainListContainer.add(instancesLabel);
        mainListContainer.add(instancesScroll);
        mainListContainer.add(buttonRow);
        mainListContainer.add(savesLabel);
        mainListContainer.add(savesScroll);
        mainListContainer.add(mapsButtonRow);
        mainListContainer.add(adwLabel);
        mainListContainer.add(adwRow);

        JPanel topStack = new JPanel();
        topStack.setLayout(new BoxLayout(topStack, BoxLayout.Y_AXIS));
        topStack.setBackground(BG);
        topStack.add(buttonSection);
        topStack.add(Box.createVerticalStrut(5));
        topStack.add(mainListContainer);

        tmpfsPanel.add(topStack, BorderLayout.NORTH);

        JPanel settingsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        settingsPanel.setBackground(BG);
        JLabel empty = new JLabel("nothing here yet");
        empty.setForeground(Color.LIGHT_GRAY);
        empty.setFont(new Font("SansSerif", Font.ITALIC, 24));
        settingsPanel.add(empty);

        contentPanel.add(tmpfsPanel, "auToMPFS");
        contentPanel.add(settingsPanel, " ");
        tmpfsNavButton.addActionListener(e -> cardLayout.show(contentPanel, "auToMPFS"));
        settingsNavButton.addActionListener(e -> cardLayout.show(contentPanel, " "));

        JPanel navAndContentPanel = new JPanel(new BorderLayout());
        navAndContentPanel.add(navPanel, BorderLayout.NORTH);
        navAndContentPanel.add(contentPanel, BorderLayout.CENTER);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(titleBar, BorderLayout.NORTH);
        mainPanel.add(navAndContentPanel, BorderLayout.CENTER);
        mainPanel.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100), 1));

        frame.setUndecorated(true);
        frame.setContentPane(mainPanel);
        frame.setSize(525, 750);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.toFront();
        frame.requestFocus();
    }

    private static int parsePositiveInt(String s, int fallback) {
        try {
            int v = Integer.parseInt(s.trim());
            return v > 0 ? v : fallback;
        } catch (Exception e) { return fallback; }
    }

    private static String buttonText() { return enabled ? "Disable" : "Enable"; }

    private static void runToggleScriptAsync() {
        long t = System.currentTimeMillis();
        if (t - lastClickTime < 3000) return;
        lastClickTime = t;

        runButton.setEnabled(false);
        final boolean runDisable = enabled;

        new Thread(() -> {
            int exitCode;
            try {
                ensureScriptsPresent();
                Path home = Path.of(System.getProperty("user.home"));
                Path scriptsDir = home.resolve(".local").resolve("share").resolve("lingle").resolve("scripts");
                Path scriptToRun = runDisable ? scriptsDir.resolve("tmpfsdisable.sh") : scriptsDir.resolve("tmpfsenable.sh");
                if (!Files.exists(scriptToRun)) throw new IOException("Script not found: " + scriptToRun);
                ProcessBuilder pb = new ProcessBuilder("sudo", "/bin/bash", scriptToRun.toString());
                Process p = pb.start();
                exitCode = p.waitFor();
            } catch (IOException | InterruptedException ex) {
                exitCode = 1;
                Thread.currentThread().interrupt();
            }

            int ec = exitCode;
            SwingUtilities.invokeLater(() -> {
                if (ec == 0) {
                    enabled = !runDisable;
                    runButton.setText(buttonText());
                    if (enabled) applySelected(runButton); else applyNormal(runButton);
                    saveCurrentState();
                    if (adwEnabled) startAdwIfNeeded(); else stopAdwQuietly();
                    if (enabled && practiceMaps) {
                        try { runPracticeMapLinkingIfNeeded(); } catch (IOException ignored) {}
                    }
                } else {
                    showDarkMessage(null, "Lingle", "Failed to execute task. Exit code: " + ec);
                }
                runButton.setEnabled(true);
            });
        }).start();
    }

    private static void linkPracticeMapsNow() throws IOException {
        if (instanceCount <= 0 || selectedPracticeMaps.isEmpty()) return;
        Path home = Path.of(System.getProperty("user.home"));
        Path savesDir = home.resolve(".local").resolve("share").resolve("lingle").resolve("saves");
        for (int k = 1; k <= instanceCount; k++) {
            Path dstDir = home.resolve("Lingle").resolve(String.valueOf(k));
            if (!Files.exists(dstDir)) Files.createDirectories(dstDir);
            for (String map : selectedPracticeMaps) {
                Path target = savesDir.resolve(map);
                Path link = dstDir.resolve(map);
                if (Files.exists(link)) {
                    if (Files.isSymbolicLink(link)) {
                        try { Files.deleteIfExists(link); } catch (IOException ignored) {}
                    } else {
                        continue;
                    }
                }
                try { Files.createSymbolicLink(link, target); } catch (FileAlreadyExistsException ignored) {}
            }
        }
    }

    private static void runPracticeMapLinkingIfNeeded() throws IOException {
        if (!practiceMaps || !enabled) return;
        if (instanceCount <= 0 || selectedPracticeMaps.isEmpty()) return;

        Path home = Path.of(System.getProperty("user.home"));
        Path scriptsDir = home.resolve(".local").resolve("share").resolve("lingle").resolve("scripts");
        if (!Files.exists(scriptsDir)) Files.createDirectories(scriptsDir);

        String userHome = home.toString();
        StringBuilder sb = new StringBuilder();
        sb.append("#!/bin/bash\nset -e\n\nfor k in {1..").append(instanceCount).append("}\ndo\n");
        for (String map : selectedPracticeMaps) {
            sb.append("  ln -s \"").append(userHome)
                    .append("/.local/share/lingle/saves/").append(map)
                    .append("\" \"").append(userHome).append("/Lingle/$k/\"\n");
        }
        sb.append("done\n");

        Path linkScript = scriptsDir.resolve("link_practice_maps.sh");
        Files.writeString(linkScript, sb.toString(), StandardCharsets.UTF_8);
        linkScript.toFile().setExecutable(true, true);

        for (int k = 1; k <= instanceCount; k++) {
            Path d = home.resolve("Lingle").resolve(String.valueOf(k));
            if (!Files.exists(d)) Files.createDirectories(d);
        }

        try {
            ProcessBuilder pb = new ProcessBuilder("/bin/bash", linkScript.toString());
            Process p = pb.start();
            p.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


    private static void startAdwIfNeeded() {
        if (!adwEnabled) return;
        stopAdwQuietly();

        try {
            Path home = Path.of(System.getProperty("user.home"));
            Path scriptsDir = home.resolve(".local").resolve("share").resolve("lingle").resolve("scripts");
            if (!Files.exists(scriptsDir)) Files.createDirectories(scriptsDir);

            long appPid = ProcessHandle.current().pid();
            int X = Math.max(0, instanceCount);
            int S = Math.max(1, adwIntervalSeconds);
            String userHome = home.toString();
            Path cfg = configFile();

            String sb = "#!/bin/bash\n" +
                    "set -euo pipefail\n" +
                    "APP_PID=" + appPid + "\n" +
                    "CFG=\"" + cfg + "\"\n" +
                    "USER_HOME=\"" + userHome + "\"\n" +
                    "X=" + X + "\n" +
                    "SLEEP_SECS=" + S + "\n" +
                    "IFS=$'\\n'\n" +
                    "while true; do\n" +
                    "  if [ ! -d \"/proc/${APP_PID}\" ]; then exit 0; fi\n" +
                    "  if ! grep -q '\"adw\": true' \"$CFG\"; then exit 0; fi\n" +
                    "  for i in $(seq 1 ${X}); do\n" +
                    "    LDIR=\"${USER_HOME}/Lingle/${i}\"\n" +
                    "    [ -d \"$LDIR\" ] || continue\n" +
                    "    for save in $(ls \"$LDIR\" -t1 --ignore='Z*' 2>/dev/null | tail -n +7); do\n" +
                    "      rm -rf \"${LDIR}/${save}\"\n" +
                    "    done\n" +
                    "  done\n" +
                    "  sleep ${SLEEP_SECS}\n" +
                    "done\n";

            Path adwScript = scriptsDir.resolve("auto_delete_worlds.sh");
            Files.writeString(adwScript, sb, StandardCharsets.UTF_8);
            adwScript.toFile().setExecutable(true, true);

            adwProcess = new ProcessBuilder("/bin/bash", adwScript.toString())
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();
        } catch (IOException ignored) {
            adwProcess = null;
        }
    }

    private static void stopAdwQuietly() {
        try {
            if (adwProcess != null) {
                adwProcess.destroy();
                try { adwProcess.waitFor(); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            }
        } catch (Exception ignored) { }
        adwProcess = null;
    }

    private static String enableScriptContent() {
        return """
#!/bin/bash
set -euo pipefail

USER_NAME="$(logname 2>/dev/null || id -un)"
USER_HOME="$(getent passwd "${USER_NAME}" | cut -d: -f6)"
USER_UID="$(id -u "${USER_NAME}")"
USER_GID="$(id -g "${USER_NAME}")"
TARGET="${USER_HOME}/Lingle"
SIZE="4g"

COMMENT="# LINGLE tmpfs"
LINE="tmpfs ${TARGET} tmpfs defaults,size=${SIZE},uid=${USER_UID},gid=${USER_GID},mode=0700 0 0"

if ! grep -qF "${LINE}" /etc/fstab; then
  echo "${COMMENT}" >> /etc/fstab
  echo "${LINE}" >> /etc/fstab
fi

if ! /usr/bin/mountpoint -q "${TARGET}"; then
  mount -t tmpfs -o size=4G,uid=$(id -u),gid=$(id -g),mode=700 tmpfs "${TARGET}"
fi

exit 0
""";
    }

    private static String disableScriptContent() {
        return """
#!/bin/bash
set -euo pipefail

USER_NAME="$(logname 2>/dev/null || id -un)"
USER_HOME="$(getent passwd "${USER_NAME}" | cut -d: -f6)"
TARGET="${USER_HOME}/Lingle"

COMMENT="# LINGLE tmpfs"
LINE="tmpfs ${TARGET} tmpfs defaults,size=${SIZE},uid=${USER_UID},gid=${USER_GID},mode=0700 0 0"

if /usr/bin/mountpoint -q "${TARGET}"; then
  umount "${TARGET}"
fi

if grep -qF "${LINE}" /etc/fstab; then
  grep -v "${COMMENT}" /etc/fstab | grep -v "${LINE}" > /tmp/fstab.tmp
  mv /tmp/fstab.tmp /etc/fstab
fi

exit 0
""";
    }

    private static void ensureScriptsPresent() throws IOException {
        Path home = Path.of(System.getProperty("user.home"));
        Path base = home.resolve(".local").resolve("share").resolve("lingle");
        Path scriptsDir = base.resolve("scripts");
        Path savesDir = base.resolve("saves");
        if (!Files.exists(scriptsDir)) Files.createDirectories(scriptsDir);
        if (!Files.exists(savesDir)) Files.createDirectories(savesDir);
        Path enableSh = scriptsDir.resolve("tmpfsenable.sh");
        Path disableSh = scriptsDir.resolve("tmpfsdisable.sh");
        Files.writeString(enableSh, enableScriptContent(), StandardCharsets.UTF_8);
        enableSh.toFile().setExecutable(true, true);
        Files.writeString(disableSh, disableScriptContent(), StandardCharsets.UTF_8);
        disableSh.toFile().setExecutable(true, true);
    }

    private static Path configFile() {
        Path home = Path.of(System.getProperty("user.home"));
        return home.resolve(".local").resolve("share").resolve("lingle").resolve("config.json");
    }

    private static void detectCurrentState() {
        Path cfg = configFile();
        if (!Files.exists(cfg)) {
            enabled = false;
            practiceMaps = false;
            instanceCount = 0;
            selectedPracticeMaps.clear();
            adwEnabled = false;
            adwIntervalSeconds = 300;
            return;
        }
        try {
            String s = Files.readString(cfg);
            enabled = s.contains("\"tmpfs\"") && s.contains("\"enabled\"");
            practiceMaps = s.contains("\"practiceMaps\"") && s.contains("\"practiceMaps\": true");
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
            else adwIntervalSeconds = 300;
        } catch (IOException ignored) {
            enabled = false;
            practiceMaps = false;
            instanceCount = 0;
            selectedPracticeMaps.clear();
            adwEnabled = false;
            adwIntervalSeconds = 300;
        }
    }

    private static void saveCurrentState() {
        try {
            Path cfg = configFile();
            Files.createDirectories(cfg.getParent());
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            sb.append("  \"tmpfs\": \"").append(enabled ? "enabled" : "disabled").append("\",\n");
            sb.append("  \"instanceCount\": ").append(instanceCount).append(",\n");
            sb.append("  \"practiceMaps\": ").append(practiceMaps ? "true" : "false").append(",\n");
            sb.append("  \"selectedMaps\": [");
            for (int i = 0; i < selectedPracticeMaps.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append("\"").append(selectedPracticeMaps.get(i)).append("\"");
            }
            sb.append("],\n");
            sb.append("  \"adw\": ").append(adwEnabled ? "true" : "false").append(",\n");
            sb.append("  \"adwInterval\": ").append(Math.max(1, adwIntervalSeconds)).append("\n");
            sb.append("}\n");
            Files.writeString(cfg, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {}
    }

    private static void showDarkMessage(Component parent, String title, String message) {
        Window owner = parent != null ? SwingUtilities.getWindowAncestor(parent) : null;
        JDialog d = new JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
        d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        d.setResizable(false);


        JPanel root = new JPanel(new BorderLayout(14, 14));
        root.setBackground(BG);
        root.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100,100,100), 1),
                BorderFactory.createEmptyBorder(18,18,18,18)
        ));

        JLabel header = new JLabel(title);
        header.setForeground(TXT);
        header.setFont(new Font("SansSerif", Font.BOLD, 16));
        header.setBorder(BorderFactory.createEmptyBorder(0,0,6,0));
        root.add(header, BorderLayout.NORTH);

        int wrapWidth = 460;
        JTextArea ta = new JTextArea(message);
        ta.setEditable(false);
        ta.setWrapStyleWord(true);
        ta.setLineWrap(true);
        ta.setOpaque(false);
        ta.setForeground(TXT);
        ta.setFont(new Font("SansSerif", Font.PLAIN, 14));
        ta.setBorder(BorderFactory.createEmptyBorder(6,8,6,8));
        ta.setSize(new Dimension(wrapWidth, Short.MAX_VALUE));
        Dimension pref = ta.getPreferredSize();
        ta.setPreferredSize(new Dimension(wrapWidth, pref.height));
        root.add(ta, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btns.setBackground(BG);
        JButton ok = new JButton("OK");
        styleWithHover(ok);
        ok.addActionListener(e -> d.dispose());
        btns.add(ok);
        root.add(btns, BorderLayout.SOUTH);

        d.setContentPane(root);
        d.pack();
        d.setMinimumSize(new Dimension(520, 180));
        d.setLocationRelativeTo(parent);
        d.setVisible(true);
    }

    private static int showConfirmDark(Component parent) {
        final int[] result = {-1};
        JDialog d = new JDialog(SwingUtilities.getWindowAncestor(parent), "Symlinking Confirmation", Dialog.ModalityType.APPLICATION_MODAL);
        d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        d.setResizable(false);

        JPanel root = new JPanel(new BorderLayout(14,14));
        root.setBackground(BG);
        root.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100,100,100), 1),
                BorderFactory.createEmptyBorder(18,18,18,18)
        ));

        JLabel header = new JLabel("WARNING");
        header.setForeground(TXT);
        header.setFont(new Font("SansSerif", Font.BOLD, 18));
        header.setBorder(BorderFactory.createEmptyBorder(0,0,6,0));
        root.add(header, BorderLayout.NORTH);

        int wrapWidth = 460;
        JTextArea ta = new JTextArea("WARNING: this will delete the selected instance's saves folders.\nIf there is anything important in any of them, please move them to a safe place.");
        ta.setEditable(false);
        ta.setWrapStyleWord(true);
        ta.setLineWrap(true);
        ta.setOpaque(false);
        ta.setForeground(TXT);
        ta.setFont(new Font("SansSerif", Font.PLAIN, 14));
        ta.setBorder(BorderFactory.createEmptyBorder(6,8,6,8));
        ta.setSize(new Dimension(wrapWidth, Short.MAX_VALUE));
        Dimension pref = ta.getPreferredSize();
        ta.setPreferredSize(new Dimension(wrapWidth, pref.height));
        root.add(ta, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btns.setBackground(BG);
        JButton yes = new JButton("Continue");
        JButton no = new JButton("Go Back");
        styleWithHover(yes);
        styleWithHover(no);
        yes.addActionListener(e -> { result[0] = 0; d.dispose(); });
        no.addActionListener(e -> { result[0] = 1; d.dispose(); });
        btns.add(no);
        btns.add(yes);
        root.add(btns, BorderLayout.SOUTH);

        d.setContentPane(root);
        d.pack();
        d.setMinimumSize(new Dimension(520, 200));
        d.setLocationRelativeTo(parent);
        d.setVisible(true);
        return result[0];
    }
}
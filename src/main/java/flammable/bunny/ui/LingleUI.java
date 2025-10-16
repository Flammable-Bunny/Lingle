package flammable.bunny.ui;

import flammable.bunny.core.*;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static flammable.bunny.ui.UIConstants.*;
import static flammable.bunny.ui.UIUtils.*;

public class LingleUI extends JFrame {

    private JButton runButton;
    private long lastClickTime;

    private static final int ROW_H = 22;

    public LingleUI() {
        super("Lingle");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setAlwaysOnTop(true);
        setType(Window.Type.UTILITY);
        setResizable(false);
        setUndecorated(true);

        // ===== Title bar =====
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(new Color(45, 45, 45));
        titleBar.setPreferredSize(new Dimension(0, 25));

        JLabel titleLabel = new JLabel("Lingle v" + Updater.CURRENT_VERSION);
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

        // Drag window
        final Point dragOffset = new Point();
        titleBar.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { dragOffset.setLocation(e.getPoint()); }
        });
        titleBar.addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                Point p = getLocation();
                p.translate(e.getX() - dragOffset.x, e.getY() - dragOffset.y);
                setLocation(p);
            }
        });

        // ===== Nav bar =====
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        navPanel.setBackground(BG);
        navPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(100, 100, 100)));

        JButton tmpfsNavButton = new JButton("auToMPFS");
        JButton settingsNavButton = new JButton("Utilities");
        for (JButton btn : new JButton[]{tmpfsNavButton, settingsNavButton}) {
            btn.setBackground(BTN_BG);
            btn.setForeground(TXT);
            btn.setBorder(new UIUtils.RoundedBorder(BTN_BORDER, 1, 8, false));
            btn.setFocusPainted(false);
            btn.setFont(UI_FONT);
            btn.setPreferredSize(new Dimension(80, 35));
            UIUtils.setupNavButton(btn);
            btn.addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) {
                    btn.setBorder(new UIUtils.RoundedBorder(BTN_HOVER_BORDER, 1, 8, true));
                    btn.repaint();
                }
                @Override public void mouseExited(MouseEvent e) {
                    btn.setBorder(new UIUtils.RoundedBorder(BTN_BORDER, 1, 8, false));
                    btn.repaint();
                }
            });
        }
        navPanel.add(settingsNavButton);
        navPanel.add(tmpfsNavButton);

        // ===== Main content card =====
        CardLayout cardLayout = new CardLayout();
        JPanel contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(BG);

        // ===== TMPFS + lists =====
        runButton = new JButton(LingleState.enabled ? "Disable" : "Enable");
        runButton.setFocusPainted(false);
        runButton.setFont(UI_FONT);
        runButton.setPreferredSize(new Dimension(100, 35));
        if (LingleState.enabled) applySelected(runButton); else applyNormal(runButton);
        runButton.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { applyHover(runButton); }
            @Override public void mouseExited(MouseEvent e) {
                if (LingleState.enabled) applySelected(runButton); else applyNormal(runButton);
            }
        });
        runButton.addActionListener(e -> toggleTmpfs());

        JPanel tmpfsPanel = new JPanel(new BorderLayout(0, 8));
        tmpfsPanel.setBackground(BG);

        JPanel buttonSection = new JPanel(null);
        buttonSection.setBackground(BG);
        buttonSection.setPreferredSize(new Dimension(0, 85));

        JLabel tmpfsLabel = new JLabel("Enable TMPFS");
        tmpfsLabel.setForeground(TXT);
        tmpfsLabel.setFont(UI_FONT_BOLD);
        tmpfsLabel.setBounds(10, 12, 150, 25);
        buttonSection.add(tmpfsLabel);

        runButton.setBounds(10, 40, 100, 35);
        buttonSection.add(runButton);

        // ===== Left column container (everything below TMPFS block) =====
        JPanel mainListContainer = new JPanel();
        mainListContainer.setLayout(new BoxLayout(mainListContainer, BoxLayout.Y_AXIS));
        mainListContainer.setBackground(BG);
        // indent to match the Enable button’s left edge
        mainListContainer.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));

        // ===== Instances =====
        JLabel instancesLabel = new JLabel("Instances:");
        instancesLabel.setForeground(TXT);
        instancesLabel.setFont(UI_FONT_BOLD);
        instancesLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 5, 0));
        instancesLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainListContainer.add(instancesLabel);

        JPanel instancesChecks = new JPanel();
        instancesChecks.setLayout(new BoxLayout(instancesChecks, BoxLayout.Y_AXIS));
        instancesChecks.setBackground(BG);
        instancesChecks.setAlignmentX(Component.LEFT_ALIGNMENT);

        Path home = Path.of(System.getProperty("user.home"));
        Path prismInstancesDir = home.resolve(".local/share/PrismLauncher/instances");

        int instRows = 0;
        try {
            if (Files.exists(prismInstancesDir) && Files.isDirectory(prismInstancesDir)) {
                for (Path dir : Files.list(prismInstancesDir)
                        .filter(Files::isDirectory)
                        .filter(p -> !p.getFileName().toString().equals(".tmp")) // hide .tmp
                        .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase()))
                        .toList()) {
                    JCheckBox cb = createStyledCheckBox(dir.getFileName().toString());
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

        JScrollPane instancesScroll = makeScroll(instancesChecks);
        int instVisible = Math.max(1, Math.min(instRows, 5));
        instancesScroll.setPreferredSize(new Dimension(Integer.MAX_VALUE, instVisible * ROW_H));
        instancesScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 5 * ROW_H));
        instancesScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainListContainer.add(instancesScroll);

        JButton symlinkButton = makeButton("Symlink Instances", 180);
        JPanel symlinkRow = leftRow();
        symlinkRow.add(symlinkButton);
        mainListContainer.add(Box.createVerticalStrut(15));
        mainListContainer.add(symlinkRow);

        // ===== Practice maps =====
        JLabel savesLabel = new JLabel("Practice Maps:");
        savesLabel.setForeground(TXT);
        savesLabel.setFont(UI_FONT_BOLD);
        savesLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 5, 0));
        savesLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainListContainer.add(savesLabel);

        JPanel savesChecks = new JPanel();
        savesChecks.setLayout(new BoxLayout(savesChecks, BoxLayout.Y_AXIS));
        savesChecks.setBackground(BG);
        savesChecks.setAlignmentX(Component.LEFT_ALIGNMENT);

        Path savesDir = home.resolve(".local/share/lingle/saves");
        int saveRows = 0;
        try {
            Files.createDirectories(savesDir);
            for (Path dir : Files.list(savesDir)
                    .filter(Files::isDirectory)
                    .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase()))
                    .toList()) {

                String raw = dir.getFileName().toString();

                if (raw.startsWith("_")) {
                    Path to = dir.getParent().resolve(raw.substring(1));
                    if (!Files.exists(to)) {
                        try { Files.move(dir, to); raw = to.getFileName().toString(); }
                        catch (IOException ignored) { /* fall through and just show it trimmed */ }
                    } else {
                        raw = raw.substring(1);
                    }
                }

                String display = raw.startsWith("Z_") ? raw.substring(2) : raw;

                JCheckBox cb = createStyledCheckBox(display);
                cb.setAlignmentX(Component.LEFT_ALIGNMENT);
                savesChecks.add(cb);
                saveRows++;
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

        JScrollPane savesScroll = makeScroll(savesChecks);
        int savesVisible = Math.max(1, Math.min(saveRows, 9));
        savesScroll.setPreferredSize(new Dimension(Integer.MAX_VALUE, savesVisible * ROW_H));
        savesScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 9 * ROW_H));
        savesScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainListContainer.add(savesScroll);

        JButton linkPracticeBtn = makeButton("Link Practice Maps", 220);
        JButton createDirsBtn = makeButton("Create Directories on Startup", 240);

        JPanel linkRow = leftRow();
        linkRow.add(linkPracticeBtn);
        JPanel dirsRow = leftRow();
        dirsRow.add(createDirsBtn);

        mainListContainer.add(Box.createVerticalStrut(15));
        mainListContainer.add(linkRow);
        mainListContainer.add(Box.createVerticalStrut(12));
        mainListContainer.add(dirsRow);
        mainListContainer.add(Box.createVerticalStrut(20));

        // ===== ADW =====
        JLabel adwLabel = new JLabel("Auto Delete Worlds:");
        adwLabel.setForeground(TXT);
        adwLabel.setFont(UI_FONT_BOLD);
        adwLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        adwLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JToggleButton adwToggle = new JToggleButton("Auto Delete Worlds");
        adwToggle.setSelected(LingleState.adwEnabled);
        adwToggle.setFocusPainted(false);
        adwToggle.setPreferredSize(new Dimension(180, 35));
        styleToggleWithHover(adwToggle, adwToggle::isSelected);

        JTextField adwInterval = new JTextField(String.valueOf(LingleState.adwIntervalSeconds), 6);
        adwInterval.setPreferredSize(new Dimension(70, 30));
        adwInterval.setFont(UI_FONT);

        JButton adwApply = makeButton("Apply", 90);
        JLabel secondsLbl = new JLabel("seconds");
        secondsLbl.setForeground(TXT);
        secondsLbl.setFont(UI_FONT);

        JPanel adwRow = leftRow();
        adwRow.add(adwToggle);
        adwRow.add(new JLabel("Interval:"));
        adwRow.add(adwInterval);
        adwRow.add(secondsLbl);
        adwRow.add(adwApply);

        mainListContainer.add(adwLabel);
        mainListContainer.add(adwRow);

        // ===== Wire actions =====
        symlinkButton.addActionListener(e -> {
            List<String> instanceNames = new ArrayList<>();
            for (Component c : instancesChecks.getComponents()) {
                if (c instanceof JCheckBox cb && cb.isSelected()) {
                    instanceNames.add(cb.getText());
                }
            }
            if (instanceNames.isEmpty()) {
                showDarkMessage(this, "No Instances Selected", "Please select at least one instance.");
                return;
            }
            int choice = new SymlinkConfirmationDialog(this).showDialog();
            if (choice != 0) return;
            try {
                LinkInstancesService.symlinkInstances(instanceNames);
                showDarkMessage(this, "Done", "Symlinks created.");
            } catch (IOException ex) {
                showDarkMessage(this, "Error", "Failed to create symlinks:\n" + ex.getMessage());
            }
        });

        linkPracticeBtn.addActionListener(e -> {
            List<String> chosen = new ArrayList<>();
            for (Component c : savesChecks.getComponents()) {
                if (c instanceof JCheckBox cb && cb.isSelected()) {
                    chosen.add(cb.getText());
                }
            }
            if (chosen.isEmpty()) {
                showDarkMessage(this, "No Maps Selected", "Please select at least one practice map.");
                return;
            }
            try {
                LingleState.selectedPracticeMaps = chosen;
                LingleState.practiceMaps = true;
                LingleState.saveState();
                LinkInstancesService.linkPracticeMapsNow();
                showDarkMessage(this, "Done", "Practice maps linked.");
            } catch (IOException ex) {
                showDarkMessage(this, "Error", ex.getMessage());
            }
        });

        createDirsBtn.addActionListener(e -> {
            int choice = new CreateDirsConfirmationDialog(this).showDialog();
            if (choice != 0) return;
            try {
                LinkInstancesService.preparePracticeMapLinks();
                LinkInstancesService.installCreateDirsService(this);
            } catch (IOException ex) {
                showDarkMessage(this, "Error", ex.getMessage());
            }
        });

        adwApply.addActionListener(e -> {
            LingleState.adwEnabled = adwToggle.isSelected();
            try {
                int v = Integer.parseInt(adwInterval.getText().trim());
                if (v > 0) LingleState.adwIntervalSeconds = v;
            } catch (NumberFormatException ignored) {}
            LingleState.saveState();
            if (LingleState.adwEnabled) AdwManager.startAdwIfNeeded();
            else AdwManager.stopAdwQuietly();
            showDarkMessage(this, "Updated", "Auto delete settings applied.");
        });

        // ===== Stack to top =====
        JPanel topStack = new JPanel();
        topStack.setLayout(new BoxLayout(topStack, BoxLayout.Y_AXIS));
        topStack.setBackground(BG);
        topStack.add(buttonSection);
        topStack.add(Box.createVerticalStrut(5));
        topStack.add(mainListContainer);

        JPanel alignWrapper = new JPanel(new BorderLayout());
        alignWrapper.setBackground(BG);
        alignWrapper.add(topStack, BorderLayout.NORTH);

        tmpfsPanel.add(alignWrapper, BorderLayout.CENTER);

        // ===== Utilities =====
        JPanel settingsPanel = new JPanel();
        settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
        settingsPanel.setBackground(BG);

        settingsPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        JButton packagesButton = makeButton("Packages for Run Submission", 240);
        packagesButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        settingsPanel.add(packagesButton);
        settingsPanel.add(Box.createVerticalGlue());

        packagesButton.addActionListener(e -> {
            if (!DependencyInstaller.ensureDeps(this)) return;

            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Select output folder for SRC zip");
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fc.setApproveButtonText("Use this folder");
            if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

            Path outDir = fc.getSelectedFile().toPath();


            // progress dialog
            JDialog progress = new JDialog(this, "Creating package", true);
            JPanel pp = new JPanel(new BorderLayout());
            pp.setBackground(BG);
            JLabel lbl = new JLabel("Creating submission package…");
            lbl.setForeground(TXT);
            lbl.setFont(UI_FONT);
            JProgressBar bar = new JProgressBar();
            bar.setIndeterminate(true);
            pp.add(lbl, BorderLayout.NORTH);
            pp.add(bar, BorderLayout.CENTER);
            pp.setBorder(BorderFactory.createEmptyBorder(10, 12, 12, 12));
            progress.setContentPane(pp);
            progress.pack();
            progress.setLocationRelativeTo(this);
            progress.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

            Thread worker = new Thread(() -> {
                int ec;
                try {
                    Path script = PackagesforRunSubmissionZipper.install(outDir);
                    Process p = new ProcessBuilder("python3", script.toString(), outDir.toString())
                            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                            .redirectError(ProcessBuilder.Redirect.DISCARD)
                            .start();
                    ec = p.waitFor();
                } catch (Exception ex) {
                    ec = 1;
                }
                final int exitCode = ec;
                SwingUtilities.invokeLater(() -> {
                    progress.dispose();
                    if (exitCode == 0) {
                        showDarkMessage(this, "Done", "Package created in:\n" + outDir);
                    } else {
                        showDarkMessage(this, "Error Code 14", "Packaging failed.");
                    }
                });
            });

            worker.start();
            progress.setVisible(true);
        });


        settingsPanel.add(Box.createVerticalGlue());
        settingsPanel.add(packagesButton);
        settingsPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        contentPanel.add(settingsPanel, "Utilities");
        contentPanel.add(tmpfsPanel, "auToMPFS");
        settingsNavButton.addActionListener(e -> cardLayout.show(contentPanel, "Utilities"));
        tmpfsNavButton.addActionListener(e -> cardLayout.show(contentPanel, "auToMPFS"));
        cardLayout.show(contentPanel, "Utilities");

        // ===== Frame =====
        JPanel navAndContent = new JPanel(new BorderLayout());
        navAndContent.add(navPanel, BorderLayout.NORTH);
        navAndContent.add(contentPanel, BorderLayout.CENTER);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(titleBar, BorderLayout.NORTH);
        mainPanel.add(navAndContent, BorderLayout.CENTER);
        mainPanel.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100), 1));

        setContentPane(mainPanel);
        setSize(530, 790);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private static JScrollPane makeScroll(JPanel body) {
        JScrollPane sp = new JScrollPane(body);
        sp.setBackground(BG);
        sp.getViewport().setBackground(BG);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getVerticalScrollBar().setUnitIncrement(14);
        sp.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                this.thumbColor = new Color(110, 110, 110);
                this.trackColor = BG;
            }
        });
        return sp;
    }

    private static JPanel leftRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row.setBackground(BG);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        return row;
    }

    private static JButton makeButton(String text, int w) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setFont(UI_FONT);
        b.setPreferredSize(new Dimension(w, 35));
        styleWithHover(b);
        return b;
    }

    private void toggleTmpfs() {
        long t = System.currentTimeMillis();
        if (t - lastClickTime < 3000) return;
        lastClickTime = t;
        runButton.setEnabled(false);
        final boolean runDisable = LingleState.enabled;

        new Thread(() -> {
            int exitCode;
            try {
                Path home = Path.of(System.getProperty("user.home"));
                Path script = home.resolve(".local/share/lingle/scripts/")
                        .resolve(runDisable ? "tmpfsdisable.sh" : "tmpfsenable.sh");
                if (!Files.exists(script)) throw new IOException("Script not found: " + script);
                Process p = new ProcessBuilder("sudo", "/bin/bash", script.toString()).start();
                exitCode = p.waitFor();
            } catch (Exception ex) {
                exitCode = 1;
            }

            final int ec = exitCode;
            SwingUtilities.invokeLater(() -> {
                if (ec == 0) {
                    LingleState.enabled = !runDisable;
                    runButton.setText(LingleState.enabled ? "Disable" : "Enable");
                    if (LingleState.enabled) applySelected(runButton); else applyNormal(runButton);
                    LingleState.saveState();
                    if (LingleState.adwEnabled) AdwManager.startAdwIfNeeded();
                    else AdwManager.stopAdwQuietly();
                    showDarkMessage(this, "Success", "TMPFS " + (LingleState.enabled ? "enabled." : "disabled."));
                } else {
                    showDarkMessage(this, "Lingle", "Failed to execute task. Exit code: " + ec);
                }
                runButton.setEnabled(true);
            });
        }).start();
    }
}

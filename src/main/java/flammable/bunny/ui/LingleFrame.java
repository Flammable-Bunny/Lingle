package flammable.bunny.ui;

import flammable.bunny.core.*;
import javax.swing.*;
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

public class LingleFrame extends JFrame {

    private JButton runButton;
    private long lastClickTime;

    public LingleFrame() {
        super("Lingle");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setAlwaysOnTop(true);
        setType(Window.Type.UTILITY);
        setResizable(false);
        setUndecorated(true);

        // Title bar
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

        // Window drag
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

        // Nav bar
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
                @Override public void mouseEntered(MouseEvent e) {
                    btn.setBackground(BTN_HOVER);
                    btn.setBorder(BorderFactory.createLineBorder(BTN_HOVER_BORDER, 1));
                }
                @Override public void mouseExited(MouseEvent e) {
                    btn.setBackground(BTN_BG);
                    btn.setBorder(BorderFactory.createLineBorder(BTN_BORDER, 1));
                }
            });
        }
        navPanel.add(tmpfsNavButton);
        navPanel.add(settingsNavButton);

        // Main content
        CardLayout cardLayout = new CardLayout();
        JPanel contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(BG);

        // TMPFS Section
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

        // Instances section
        JLabel instancesLabel = new JLabel("Instances:");
        instancesLabel.setForeground(TXT);
        instancesLabel.setFont(UI_FONT_BOLD);
        instancesLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        instancesLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel instancesChecks = new JPanel();
        instancesChecks.setLayout(new BoxLayout(instancesChecks, BoxLayout.Y_AXIS));
        instancesChecks.setBackground(BG);

        Path home = Path.of(System.getProperty("user.home"));
        Path prismInstancesDir = home.resolve(".local/share/PrismLauncher/instances");
        int instRows = 0;
        try {
            if (Files.exists(prismInstancesDir) && Files.isDirectory(prismInstancesDir)) {
                for (Path dir : Files.list(prismInstancesDir)
                        .filter(Files::isDirectory)
                        .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase()))
                        .toList()) {
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
            }
        } catch (IOException e) {
            JLabel err = new JLabel("Error reading instances directory");
            err.setForeground(Color.RED);
            err.setFont(new Font("SansSerif", Font.ITALIC, 12));
            instancesChecks.add(err);
        }

        JScrollPane instancesScroll = new JScrollPane(instancesChecks);
        instancesScroll.setBackground(BG);
        instancesScroll.getViewport().setBackground(BG);
        instancesScroll.setBorder(BorderFactory.createEmptyBorder());

        JButton symlinkButton = new JButton("Symlink Instances");
        symlinkButton.setFocusPainted(false);
        symlinkButton.setFont(UI_FONT);
        symlinkButton.setPreferredSize(new Dimension(180, 35));
        styleWithHover(symlinkButton);

        // Practice maps
        JLabel savesLabel = new JLabel("Practice Maps:");
        savesLabel.setForeground(TXT);
        savesLabel.setFont(UI_FONT_BOLD);
        savesLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        savesLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel savesChecks = new JPanel();
        savesChecks.setLayout(new BoxLayout(savesChecks, BoxLayout.Y_AXIS));
        savesChecks.setBackground(BG);

        Path savesDir = home.resolve(".local/share/lingle/saves");
        int saveRows = 0;
        try {
            Files.createDirectories(savesDir);
            for (Path dir : Files.list(savesDir)
                    .filter(Files::isDirectory)
                    .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase()))
                    .toList()) {
                JCheckBox cb = new JCheckBox(dir.getFileName().toString());
                cb.setBackground(BG);
                cb.setForeground(TXT);
                cb.setFont(UI_FONT);
                cb.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
                cb.setAlignmentX(Component.LEFT_ALIGNMENT);
                savesChecks.add(cb);
                saveRows++;
            }
        } catch (IOException e) {
            JLabel err = new JLabel("Error reading practice maps directory");
            err.setForeground(Color.RED);
            err.setFont(new Font("SansSerif", Font.ITALIC, 12));
            savesChecks.add(err);
        }

        JScrollPane savesScroll = new JScrollPane(savesChecks);
        savesScroll.setBackground(BG);
        savesScroll.getViewport().setBackground(BG);
        savesScroll.setBorder(BorderFactory.createEmptyBorder());
        savesScroll.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton linkPracticeBtn = new JButton("Link Practice Maps");
        linkPracticeBtn.setFocusPainted(false);
        linkPracticeBtn.setFont(UI_FONT);
        linkPracticeBtn.setPreferredSize(new Dimension(220, 35));
        styleWithHover(linkPracticeBtn);

        JButton createDirsBtn = new JButton("Create Directories on Startup");
        createDirsBtn.setFocusPainted(false);
        createDirsBtn.setFont(UI_FONT);
        createDirsBtn.setPreferredSize(new Dimension(240, 35));
        styleWithHover(createDirsBtn);

        // Layout correction
        JPanel mapsPanel = new JPanel();
        mapsPanel.setLayout(new BoxLayout(mapsPanel, BoxLayout.Y_AXIS));
        mapsPanel.setBackground(BG);
        mapsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mapsPanel.add(Box.createVerticalStrut(10));

        JPanel linkPracticeWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        linkPracticeWrapper.setBackground(BG);
        linkPracticeWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        linkPracticeWrapper.add(linkPracticeBtn);
        mapsPanel.add(linkPracticeWrapper);

        mapsPanel.add(Box.createVerticalStrut(12));

        JPanel createDirsWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        createDirsWrapper.setBackground(BG);
        createDirsWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        createDirsWrapper.add(createDirsBtn);
        mapsPanel.add(createDirsWrapper);

        // Auto delete
        JLabel adwLabel = new JLabel("Auto Delete Worlds:");
        adwLabel.setForeground(TXT);
        adwLabel.setFont(UI_FONT_BOLD);
        adwLabel.setBorder(BorderFactory.createEmptyBorder(15, 10, 5, 10));
        adwLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JToggleButton adwToggle = new JToggleButton("Auto Delete Worlds");
        adwToggle.setSelected(LingleState.adwEnabled);
        adwToggle.setFocusPainted(false);
        adwToggle.setPreferredSize(new Dimension(180, 35));
        styleToggleWithHover(adwToggle, adwToggle::isSelected);

        JTextField adwInterval = new JTextField(String.valueOf(LingleState.adwIntervalSeconds), 6);
        adwInterval.setPreferredSize(new Dimension(70, 30));
        adwInterval.setFont(UI_FONT);

        JButton adwApply = new JButton("Apply");
        styleWithHover(adwApply);
        adwApply.setPreferredSize(new Dimension(90, 30));
        JLabel secondsLbl = new JLabel("seconds");
        secondsLbl.setForeground(TXT);
        secondsLbl.setFont(UI_FONT);

        JPanel adwRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        adwRow.setBackground(BG);
        adwRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        adwRow.add(adwToggle);
        adwRow.add(new JLabel("Interval:"));
        adwRow.add(adwInterval);
        adwRow.add(secondsLbl);
        adwRow.add(adwApply);

        // Main assembly (left aligned)
        JPanel mainListContainer = new JPanel();
        mainListContainer.setLayout(new BoxLayout(mainListContainer, BoxLayout.Y_AXIS));
        mainListContainer.setBackground(BG);
        mainListContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        instancesLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        instancesScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        savesLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        savesScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        mapsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        adwLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        adwRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        mainListContainer.add(instancesLabel);
        mainListContainer.add(instancesScroll);
        mainListContainer.add(Box.createVerticalStrut(15));

        JPanel symlinkWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        symlinkWrapper.setBackground(BG);
        symlinkWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        symlinkWrapper.add(symlinkButton);
        mainListContainer.add(symlinkWrapper);

        mainListContainer.add(Box.createVerticalStrut(25));
        mainListContainer.add(savesLabel);
        mainListContainer.add(savesScroll);
        mainListContainer.add(Box.createVerticalStrut(15));
        mainListContainer.add(mapsPanel);
        mainListContainer.add(Box.createVerticalStrut(25));
        mainListContainer.add(adwLabel);
        mainListContainer.add(adwRow);

        JPanel topStack = new JPanel();
        topStack.setLayout(new BoxLayout(topStack, BoxLayout.Y_AXIS));
        topStack.setBackground(BG);
        topStack.setAlignmentX(Component.LEFT_ALIGNMENT);
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

        JPanel navAndContent = new JPanel(new BorderLayout());
        navAndContent.add(navPanel, BorderLayout.NORTH);
        navAndContent.add(contentPanel, BorderLayout.CENTER);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(titleBar, BorderLayout.NORTH);
        mainPanel.add(navAndContent, BorderLayout.CENTER);
        mainPanel.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100), 1));

        setContentPane(mainPanel);
        setSize(525, 775);
        setLocationRelativeTo(null);
        setVisible(true);
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
                    if (LingleState.enabled) applySelected(runButton);
                    else applyNormal(runButton);
                    LingleState.saveState();
                    if (LingleState.adwEnabled) AdwManager.startAdwIfNeeded();
                    else AdwManager.stopAdwQuietly();
                } else {
                    showDarkMessage(null, "Lingle", "Failed to execute task. Exit code: " + ec);
                }
                runButton.setEnabled(true);
            });
        }).start();
    }
}

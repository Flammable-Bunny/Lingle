package flammable.bunny.ui;

import javax.swing.*;
import java.awt.*;

public class CreateDirsConfirmationDialog extends JDialog {
    private int result = -1;

    public CreateDirsConfirmationDialog(Frame owner) {
        super(owner, "Create Directories Confirmation", true);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout(14, 14));
        root.setBackground(UIConstants.BG);
        root.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 100), 1),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)
        ));

        JLabel header = new JLabel("Confirmation");
        header.setForeground(UIConstants.TXT);
        header.setFont(new Font("SansSerif", Font.BOLD, 18));
        root.add(header, BorderLayout.NORTH);

        JTextArea ta = new JTextArea(
                "This feature ONLY works on systemd-based distributions (Arch, Fedora, Debian, etc.)\n" +
                        "It will NOT work on non-systemd distros (Artix, Gentoo, FreeBSD, etc.).\n" +
                        "Are you sure you want to continue?"
        );
        ta.setEditable(false);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setOpaque(false);
        ta.setForeground(UIConstants.TXT);
        ta.setFont(new Font("SansSerif", Font.PLAIN, 14));
        ta.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        root.add(ta, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btns.setBackground(UIConstants.BG);
        JButton yes = new JButton("Continue");
        JButton no = new JButton("Go Back");
        UIUtils.styleWithHover(yes);
        UIUtils.styleWithHover(no);
        yes.addActionListener(e -> { result = 0; dispose(); });
        no.addActionListener(e -> { result = 1; dispose(); });
        btns.add(no);
        btns.add(yes);
        root.add(btns, BorderLayout.SOUTH);

        setContentPane(root);
        pack();
        setMinimumSize(new Dimension(520, 200));
        setLocationRelativeTo(owner);
    }

    public int showDialog() {
        setVisible(true);
        return result;
    }
}

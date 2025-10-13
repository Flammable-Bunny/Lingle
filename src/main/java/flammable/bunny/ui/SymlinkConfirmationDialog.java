package flammable.bunny.ui;

import javax.swing.*;
import java.awt.*;

public class SymlinkConfirmationDialog extends JDialog {
    private int result = -1;

    public SymlinkConfirmationDialog(Frame owner) {
        super(owner, "Symlinking Confirmation", true);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout(14, 14));
        root.setBackground(UIConstants.BG);
        root.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 100), 1),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)
        ));

        JLabel header = new JLabel("WARNING");
        header.setForeground(UIConstants.TXT);
        header.setFont(new Font("SansSerif", Font.BOLD, 18));
        root.add(header, BorderLayout.NORTH);

        JTextArea ta = new JTextArea(
                "WARNING: This will delete the selected instance's saves folders.\n" +
                        "If there is anything important in any of them, please move them to a safe place."
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

package flammable.bunny.ui;

import java.awt.*;
import java.util.function.BooleanSupplier;
import javax.swing.*;
import javax.swing.border.Border;

import static flammable.bunny.ui.UIConstants.*;

public class UIUtils {

    public static void applyNormal(AbstractButton b) {
        b.setBackground(BTN_BG);
        b.setBorder(BorderFactory.createLineBorder(BTN_BORDER, 2));
        b.setForeground(TXT);
        b.setFont(UI_FONT);
    }

    public static void applyHover(AbstractButton b) {
        b.setBackground(BTN_HOVER);
        b.setBorder(BorderFactory.createLineBorder(BTN_HOVER_BORDER, 2));
    }

    public static void applySelected(AbstractButton b) {
        b.setBackground(BTN_SELECTED);
        b.setBorder(BorderFactory.createLineBorder(BTN_HOVER_BORDER, 2));
    }

    public static void styleWithHover(AbstractButton b) {
        applyNormal(b);
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { applyHover(b); }
            @Override public void mouseExited(java.awt.event.MouseEvent e) { applyNormal(b); }
        });
    }

    public static void styleToggleWithHover(AbstractButton b, BooleanSupplier isSelected) {
        b.setFont(UI_FONT);
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { applyHover(b); }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                if (isSelected.getAsBoolean()) applySelected(b);
                else applyNormal(b);
            }
        });
        if (isSelected.getAsBoolean()) applySelected(b); else applyNormal(b);
    }

    public static JCheckBox createStyledCheckBox(String text) {
        JCheckBox cb = new JCheckBox(text);
        cb.setBackground(BG);
        cb.setForeground(TXT);
        cb.setFont(UI_FONT);
        cb.setFocusPainted(false);
        cb.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        cb.setIconTextGap(6);

        // Use custom checkbox icon for modern look
        cb.setUI(new javax.swing.plaf.basic.BasicCheckBoxUI() {
            @Override
            public Icon getDefaultIcon() {
                return new Icon() {
                    @Override
                    public void paintIcon(Component c, Graphics g, int x, int y) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                        JCheckBox checkbox = (JCheckBox) c;
                        boolean selected = checkbox.isSelected();
                        boolean hovered = checkbox.getModel().isRollover();

                        // Draw checkbox background with rounded corners
                        g2.setColor(selected ? new Color(75, 95, 140) : new Color(50, 50, 50));
                        g2.fillRoundRect(x, y, 16, 16, 4, 4);

                        // Draw border
                        g2.setColor(hovered ? new Color(100, 150, 200) : new Color(100, 100, 100));
                        g2.setStroke(new BasicStroke(1.5f));
                        g2.drawRoundRect(x, y, 16, 16, 4, 4);

                        // Draw checkmark if selected
                        if (selected) {
                            g2.setColor(Color.WHITE);
                            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                            g2.drawLine(x + 4, y + 8, x + 7, y + 11);
                            g2.drawLine(x + 7, y + 11, x + 12, y + 5);
                        }

                        g2.dispose();
                    }

                    @Override
                    public int getIconWidth() { return 16; }

                    @Override
                    public int getIconHeight() { return 16; }
                };
            }
        });

        return cb;
    }

    public static void showDarkMessage(Window parent, String title, String message) {
        Window owner = parent != null ? SwingUtilities.getWindowAncestor(parent) : null;
        JDialog d = new JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
        d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        d.setResizable(false);

        JPanel root = new JPanel(new BorderLayout(14, 14));
        root.setBackground(BG);
        root.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 100), 1),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)
        ));

        JLabel header = new JLabel(title);
        header.setForeground(TXT);
        header.setFont(new Font("SansSerif", Font.BOLD, 16));
        root.add(header, BorderLayout.NORTH);

        JTextArea ta = new JTextArea(message);
        ta.setEditable(false);
        ta.setWrapStyleWord(true);
        ta.setLineWrap(true);
        ta.setOpaque(false);
        ta.setForeground(TXT);
        ta.setFont(new Font("SansSerif", Font.PLAIN, 14));
        ta.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        root.add(ta, BorderLayout.CENTER);

        JButton ok = new JButton("OK");
        styleWithHover(ok);
        ok.addActionListener(e -> d.dispose());

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btns.setBackground(BG);
        btns.add(ok);
        root.add(btns, BorderLayout.SOUTH);

        d.setContentPane(root);
        d.pack();
        d.setMinimumSize(new Dimension(520, 180));
        d.setLocationRelativeTo(parent);
        d.setVisible(true);
    }
}

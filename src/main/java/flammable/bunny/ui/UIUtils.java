package flammable.bunny.ui;

import java.awt.*;
import java.util.function.BooleanSupplier;
import javax.swing.*;

import static flammable.bunny.ui.UIConstants.*;

public class UIUtils {

    public static void applyNormal(AbstractButton b) {
        b.setBackground(BTN_BG);
        b.setBorder(new RoundedBorder(BTN_BORDER, 2, 8, false));
        b.setForeground(TXT);
        b.setFont(UI_FONT);
        setupButtonPainting(b);
    }

    public static void applyHover(AbstractButton b) {
        b.setBackground(BTN_BG);
        b.setBorder(new RoundedBorder(BTN_HOVER_BORDER, 2, 8, true));
        b.repaint();
    }

    public static void applySelected(AbstractButton b) {
        b.setBackground(BTN_SELECTED);
        b.setBorder(new RoundedBorder(BTN_HOVER_BORDER, 2, 8, false));
        setupButtonPainting(b);
    }

    private static void setupButtonPainting(AbstractButton b) {
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                AbstractButton button = (AbstractButton) c;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                // Paint background with rounded corners
                g2.setColor(button.getBackground());
                if (button.getBorder() instanceof RoundedBorder) {
                    RoundedBorder rb = (RoundedBorder) button.getBorder();
                    g2.fillRoundRect(0, 0, button.getWidth(), button.getHeight(), rb.radius, rb.radius);
                }

                // Paint border
                if (button.getBorder() instanceof RoundedBorder) {
                    RoundedBorder rb = (RoundedBorder) button.getBorder();

                    if (rb.glowEffect) {
                        // Draw outer glow for hover effect
                        g2.setColor(new Color(rb.borderColor.getRed(), rb.borderColor.getGreen(), rb.borderColor.getBlue(), 80));
                        g2.setStroke(new BasicStroke(rb.thickness + 1));
                        g2.drawRoundRect(2, 2, button.getWidth() - 4, button.getHeight() - 4, rb.radius, rb.radius);
                    }

                    // Draw main border
                    g2.setColor(rb.borderColor);
                    g2.setStroke(new BasicStroke(rb.thickness));
                    g2.drawRoundRect(rb.thickness/2, rb.thickness/2, button.getWidth() - rb.thickness - 1, button.getHeight() - rb.thickness - 1, rb.radius, rb.radius);
                }

                // Paint the text
                FontMetrics fm = g2.getFontMetrics(button.getFont());
                String text = button.getText();
                if (text != null && !text.isEmpty()) {
                    int textX = (button.getWidth() - fm.stringWidth(text)) / 2;
                    int textY = (button.getHeight() + fm.getAscent() - fm.getDescent()) / 2;

                    g2.setColor(button.getForeground());
                    g2.setFont(button.getFont());
                    g2.drawString(text, textX, textY);
                }

                g2.dispose();
            }
        });
    }

    public static void setupNavButton(AbstractButton b) {
        setupButtonPainting(b);
    }

    public static class RoundedBorder implements javax.swing.border.Border {
        final Color borderColor;
        final int thickness;
        final int radius;
        final boolean glowEffect;

        public RoundedBorder(Color borderColor, int thickness, int radius, boolean glowEffect) {
            this.borderColor = borderColor;
            this.thickness = thickness;
            this.radius = radius;
            this.glowEffect = glowEffect;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            // Border painting is now handled in the button UI
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(thickness + 2, thickness + 8, thickness + 2, thickness + 8);
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
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

                        g2.setColor(selected ? new Color(75, 95, 140) : new Color(50, 50, 50));
                        g2.fillRoundRect(x, y, 16, 16, 4, 4);

                        g2.setColor(hovered ? new Color(100, 150, 200) : new Color(100, 100, 100));
                        g2.setStroke(new BasicStroke(1.5f));
                        g2.drawRoundRect(x, y, 16, 16, 4, 4);

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

    public static boolean showDarkConfirm(Window parent, String title, String message) {
        Window owner = parent != null ? SwingUtilities.getWindowAncestor(parent) : null;
        JDialog d = new JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
        d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        d.setResizable(false);

        final boolean[] result = {false};

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

        JButton yes = new JButton("Yes");
        JButton no = new JButton("No");
        styleWithHover(yes);
        styleWithHover(no);

        yes.addActionListener(e -> {
            result[0] = true;
            d.dispose();
        });
        no.addActionListener(e -> d.dispose());

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btns.setBackground(BG);
        btns.add(no);
        btns.add(yes);
        root.add(btns, BorderLayout.SOUTH);

        d.setContentPane(root);
        d.pack();
        d.setMinimumSize(new Dimension(520, 180));
        d.setLocationRelativeTo(parent);
        d.setVisible(true);

        return result[0];
    }
}

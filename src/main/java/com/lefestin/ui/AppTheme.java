package com.lefestin.ui;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/**
 * AppTheme — single source of truth for all visual styling in Le Festin.
 *
 * Usage in any panel:
 *   setBackground(AppTheme.BG_PAGE);
 *   JButton btn = AppTheme.primaryButton("Save");
 *   label.setFont(AppTheme.FONT_BODY);
 *
 * Never hardcode Color or Font values in panels — always reference here.
 * To restyle the entire app, change values in this one file.
 */
public class AppTheme {
    //  COLOR PALETTE
    //
    //  Inspired by a warm bistro kitchen:
    //    - Deep charcoal sidebar (like cast iron)
    //    - Warm cream page backgrounds (like parchment / recipe cards)
    //    - Herb green for actions (like fresh basil)
    //    - Terracotta red for destructive actions (like paprika)
    //    - Saffron amber for highlights (like turmeric)
    //    - Clean white surfaces for cards and tables

    // Sidebar
    /** Deep charcoal — sidebar background */
    public static final Color SIDEBAR_BG         = new Color(249, 249, 245);
    /** Slightly lighter — active nav item */
    public static final Color SIDEBAR_ACTIVE     = new Color(252, 219, 109);;
    /** Hover state */
    public static final Color SIDEBAR_HOVER      = new Color(253, 235, 158);
    /** Nav item text — inactive */
    public static final Color SIDEBAR_FG         = new Color(249, 249, 245);
    /** Nav item text — active */
    public static final Color SIDEBAR_FG_ACTIVE  = new Color(249, 249, 245);

    // Header
    /** Dark header bar */
    public static final Color HEADER_BG         = new Color(252, 219, 109);
    /** App name text */
    public static final Color HEADER_FG         = Color.WHITE;
    /** Username / subtitle text */
    public static final Color HEADER_FG_MUTED   = new Color(160, 168, 180);

    // Page & surface backgrounds 
    /** Warm cream — outermost page background */
    public static final Color BG_PAGE           = new Color(249, 249, 245); 
    /** Clean white — cards, panels, table surfaces */
    public static final Color BG_SURFACE        = new Color(249, 249, 245);
    /** Very light warm gray — alternating table rows, input fields */
    public static final Color BG_SUBTLE         = new Color(252, 250, 247);
    /** Soft divider/border color */
    public static final Color BG_BORDER         = new Color(225, 220, 213);

    // Text
    /** Primary text — headings, table content */
    public static final Color TEXT_PRIMARY      = Color.BLACK;
    /** Secondary text — subtitles, labels */
    public static final Color TEXT_SECONDARY    = Color.BLACK;
    /** Muted/hint text — placeholders, counts */
    public static final Color TEXT_MUTED        = Color.BLACK;
    /** Inverted text — on dark backgrounds */
    public static final Color TEXT_INVERTED     = Color.BLACK;

    // Herb green — primary action color
    /** Primary button background */
    public static final Color GREEN_PRIMARY     = new Color(252, 219, 109);;
    /** Hover state */
    public static final Color GREEN_HOVER       = new Color(34, 100, 60);
    /** Light tint — filled meal slots, success states */
    public static final Color GREEN_TINT        = new Color(230, 247, 237);
    /** Text on green tint */
    public static final Color GREEN_TINT_TEXT   = new Color(22, 88, 48);

    // Terracotta — destructive / warning actions
    /** Delete / remove button background */
    public static final Color TERRA_PRIMARY     = new Color(186, 74, 48);
    /** Hover */
    public static final Color TERRA_HOVER       = new Color(160, 58, 36);
    /** Light tint */
    public static final Color TERRA_TINT        = new Color(252, 238, 233);
    /** Text on terracotta tint */
    public static final Color TERRA_TINT_TEXT   = new Color(140, 50, 30);

    // Saffron amber — highlights and today indicator
    /** Accent — today badge, match % bar */
    public static final Color AMBER_PRIMARY     = new Color(210, 140, 30);
    /** Light tint — today column header */
    public static final Color AMBER_TINT        = new Color(255, 248, 225);
    /** Text on amber tint */
    public static final Color AMBER_TINT_TEXT   = new Color(140, 88, 10);

    // Blue — selection and info states
    /** Table row selection background */
    public static final Color SELECTION_BG      = new Color(224, 238, 255);
    /** Table row selection foreground */
    public static final Color SELECTION_FG      = new Color(20, 40, 80);


    //  TYPOGRAPHY
    //
    //  Swing is limited to system fonts — no web font loading.
    //  We use a serif display font for headings to evoke a printed
    //  recipe card, and clean SansSerif for all body/UI text.
    /** Panel titles, section headings — serif for warmth */
    public static final Font FONT_TITLE         =
        new Font("Serif", Font.BOLD, 20);

    /** Card headings, dialog titles */
    public static final Font FONT_HEADING       =
        new Font("Serif", Font.BOLD, 16);

    /** Nav items, button labels, table content */
    public static final Font FONT_BODY          =
        new Font("SansSerif", Font.PLAIN, 14);

    /** Table headers, labels, badges */
    public static final Font FONT_LABEL         =
        new Font("SansSerif", Font.BOLD, 12);

    /** Subtitles, count labels, helper text */
    public static final Font FONT_SMALL         =
        new Font("SansSerif", Font.PLAIN, 12);

    /** Tiny badges, column headers */
    public static final Font FONT_TINY          =
        new Font("SansSerif", Font.BOLD, 10);

    /** App name in header */
    public static final Font FONT_APP_NAME      =
        new Font("Serif", Font.BOLD, 22);

    /** Monospaced — recipe procedure, grocery list quantities */
    public static final Font FONT_MONO          =
        new Font("Monospaced", Font.PLAIN, 13);

    
    //  BORDERS
    /** Standard divider line — bottom of header, toolbar */
    public static final Border BORDER_DIVIDER =
        BorderFactory.createMatteBorder(
            0, 0, 1, 0, BG_BORDER);

    /** Divider on top — above toolbars */
    public static final Border BORDER_DIVIDER_TOP =
        BorderFactory.createMatteBorder(
            1, 0, 0, 0, BG_BORDER);

    /** Standard card inset */
    public static final Border BORDER_CARD_PADDING =
        BorderFactory.createEmptyBorder(16, 20, 16, 20);

    /** Inner padding for toolbar strips */
    public static final Border BORDER_TOOLBAR_PADDING =
        BorderFactory.createEmptyBorder(10, 16, 10, 16);

    /** Rounded input field border */
    public static final Border BORDER_INPUT =
        BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BG_BORDER, 1, true),
            BorderFactory.createEmptyBorder(6, 10, 6, 10));


    //  COMPONENT FACTORIES
    //  Use these instead of constructing styled components inline.
    /**
     * Primary action button — herb green, white text.
     * Use for: Save, Add, Match Recipes, Auto-Generate.
     */
    public static JButton primaryButton(String text) {
        JButton btn = baseButton(text);
        btn.setBackground(GREEN_PRIMARY);
        btn.setForeground(TEXT_INVERTED);
        btn.addMouseListener(hoverEffect(
            btn, GREEN_PRIMARY, GREEN_HOVER));
        return btn;
    }

    /**
     * Secondary button — neutral surface, dark text.
     * Use for: Edit, Cancel, navigation arrows.
     */
    public static JButton secondaryButton(String text) {
        JButton btn = baseButton(text);
        btn.setBackground(BG_SUBTLE);
        btn.setForeground(TEXT_PRIMARY);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BG_BORDER, 1),
            BorderFactory.createEmptyBorder(7, 16, 7, 16)));
        btn.setBorderPainted(true);
        btn.addMouseListener(hoverEffect(
            btn, BG_SUBTLE, new Color(245, 242, 238)));
        btn.addChangeListener(e -> {
            if (!btn.isEnabled()) {
                btn.setBackground(new Color(240, 238, 235));
                btn.setForeground(TEXT_MUTED);
            } else {
                btn.setBackground(BG_SUBTLE);
                btn.setForeground(TEXT_PRIMARY);
            }
        });
        return btn;
    }

    /**
     * Danger button — terracotta, white text.
     * Use for: Delete, Remove, Clear Week.
     */
    public static JButton dangerButton(String text) {
        JButton btn = baseButton(text);
        btn.setBackground(TERRA_PRIMARY);
        btn.setForeground(TEXT_INVERTED);
        btn.addMouseListener(hoverEffect(
            btn, TERRA_PRIMARY, TERRA_HOVER));
        btn.addChangeListener(e -> {
            if (!btn.isEnabled()) {
                btn.setBackground(new Color(180, 120, 100));
                btn.setForeground(new Color(200, 180, 170));
            } else {
                btn.setBackground(TERRA_PRIMARY);
                btn.setForeground(TEXT_INVERTED);
            }
        });
        return btn;
    }

    /**
     * Ghost button — transparent with border.
     * Use for: Prev/Next week, low-priority actions.
     */
    public static JButton ghostButton(String text) {
        JButton btn = baseButton(text);
        btn.setBackground(BG_SURFACE);
        btn.setForeground(TEXT_SECONDARY);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BG_BORDER, 1),
            BorderFactory.createEmptyBorder(6, 14, 6, 14)));
        btn.setBorderPainted(true);
        return btn;
    }

    /** Panel title label — Serif bold, primary text color */
    public static JLabel titleLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_TITLE);
        lbl.setForeground(TEXT_PRIMARY);
        return lbl;
    }

    /** Section heading label */
    public static JLabel headingLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_HEADING);
        lbl.setForeground(TEXT_PRIMARY);
        return lbl;
    }

    /** Muted subtitle label below a title */
    public static JLabel subtitleLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_SMALL);
        lbl.setForeground(TEXT_MUTED);
        return lbl;
    }

    /** Styled search/text input field */
    public static JTextField inputField(String placeholder) {
        JTextField field = new JTextField();
        field.setFont(FONT_BODY);
        field.setBackground(BG_SURFACE);
        field.setForeground(TEXT_PRIMARY);
        field.setBorder(BORDER_INPUT);
        field.putClientProperty(
            "JTextField.placeholderText", placeholder);
        return field;
    }

    /** Styled combo box */
    public static JComboBox<String> comboBox(String[] items) {
        JComboBox<String> combo = new JComboBox<>(items);
        combo.setFont(FONT_BODY);
        combo.setBackground(BG_SURFACE);
        combo.setForeground(TEXT_PRIMARY);
        return combo;
    }

    /**
     * Applies the full Le Festin theme to a JTable.
     * Call this after constructing any JTable.
     */
    public static void styleTable(JTable table) {
        table.setFont(FONT_BODY);
        table.setRowHeight(38);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION);
        table.setBackground(BG_SURFACE);
        table.setForeground(TEXT_PRIMARY);
        table.setSelectionBackground(SELECTION_BG);
        table.setSelectionForeground(SELECTION_FG);
        table.getTableHeader().setFont(FONT_LABEL);
        table.getTableHeader().setBackground(BG_SUBTLE);
        table.getTableHeader().setForeground(TEXT_SECONDARY);
        table.getTableHeader().setBorder(BORDER_DIVIDER);
    }

    /**
     * Alternating row renderer — apply to every column's renderer.
     * Keeps rows readable without visible grid lines.
     */
    public static javax.swing.table.DefaultTableCellRenderer
    alternatingRowRenderer() {
        return new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value,
                    boolean isSelected, boolean hasFocus,
                    int row, int col) {
                super.getTableCellRendererComponent(
                    table, value, isSelected,
                    hasFocus, row, col);
                setBorder(BorderFactory.createEmptyBorder(
                    0, 12, 0, 12));
                if (!isSelected) {
                    setBackground(row % 2 == 0
                        ? BG_SURFACE : BG_SUBTLE);
                    setForeground(TEXT_PRIMARY);
                }
                return this;
            }
        };
    }

    /**
     * Installs the Nimbus look-and-feel with Le Festin color
     * overrides. Call once from Main.java before creating
     * any Swing components.
     */
    public static void install() {
        try {
            for (UIManager.LookAndFeelInfo info
                    : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // Nimbus not available — fall back to system L&F
            try {
                UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
        }

        // Override Nimbus defaults with Le Festin palette
        UIManager.put("control",            BG_PAGE);
        UIManager.put("info",               AMBER_TINT);
        UIManager.put("nimbusBase",         SIDEBAR_BG);
        UIManager.put("nimbusBlueGrey",     new Color(130, 125, 115));
        UIManager.put("nimbusDisabledText", TEXT_MUTED);
        UIManager.put("nimbusFocus",        GREEN_PRIMARY);
        UIManager.put("nimbusGreen",        GREEN_PRIMARY);
        UIManager.put("nimbusOrange",       AMBER_PRIMARY);
        UIManager.put("nimbusRed",          TERRA_PRIMARY);
        
        UIManager.put("nimbusSelectedText", TEXT_MUTED);

        UIManager.put("nimbusSelectionBackground", SELECTION_BG);
        UIManager.put("text",               TEXT_PRIMARY);
        UIManager.put("Table.background",   BG_SURFACE);
        UIManager.put("Table.alternateRowColor", BG_SUBTLE);
        UIManager.put("TextField.background", BG_SURFACE);
        UIManager.put("ComboBox.background",  BG_SURFACE);
        UIManager.put("ScrollPane.background", BG_PAGE);
        UIManager.put("Panel.background",    BG_PAGE);
    }

    // Private helpers 

    private static JButton baseButton(String text) {
        JButton btn = new StyledButton(text);
        btn.setFont(FONT_BODY);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(
            Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(
            8, 18, 8, 18));
        return btn;
    }

    private static java.awt.event.MouseAdapter hoverEffect(
            JButton btn, Color normal, Color hover) {
        return new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (btn.isEnabled())
                    btn.setBackground(hover);
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (btn.isEnabled())
                    btn.setBackground(normal);
            }
        };
    }
}

/**
 * Custom button that takes complete control of its appearance.
 * Prevents Nimbus L&F from painting unwanted borders/effects.
 */
class StyledButton extends JButton {
    public StyledButton(String text) {
        super(text);
        setUI(new CustomBasicButtonUI());
    }
}

/**
 * Custom BasicButtonUI that doesn't gray out text when disabled.
 * Paints text directly without shadow or disabled effects.
 */
class CustomBasicButtonUI extends javax.swing.plaf.basic.BasicButtonUI {
    @Override
    protected void paintText(java.awt.Graphics g, 
                           javax.swing.AbstractButton b, 
                           java.awt.Rectangle textRect, 
                           String text) {
        // Paint text directly with button's foreground color, no shadow/disabled effects
        g.setColor(b.getForeground());
        g.setFont(b.getFont());
        javax.swing.plaf.basic.BasicGraphicsUtils.drawString(
            g, text, b.getDisplayedMnemonicIndex(),
            textRect.x, textRect.y + g.getFontMetrics().getAscent());
    }
}
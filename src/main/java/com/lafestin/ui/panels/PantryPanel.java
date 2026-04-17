package com.lafestin.ui.panels;

import com.lafestin.dao.PantryDAO;
import com.lafestin.model.PantryItem;
import com.lafestin.ui.MainFrame;
import com.lafestin.ui.dialogs.AddEditIngredientDialog;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;
import java.util.List;

/**
 * PantryPanel — the virtual pantry browser.
 *
 */
public class PantryPanel extends JPanel {

    // Deps
    private final MainFrame frame;
    private final PantryDAO pantryDAO;

    // Table look
    private JTable            table;
    private DefaultTableModel tableModel;

    // Toolbar buttons
    private JButton addBtn;
    private JButton editBtn;
    private JButton removeBtn;
    private JLabel  countLabel;

    // Col indxs
    private static final int COL_INGREDIENT_ID = 0; // hidden
    private static final int COL_NAME = 1;
    private static final int COL_QUANTITY = 2;
    private static final int COL_UNIT = 3;

    public PantryPanel(MainFrame frame) {
        this.frame     = frame;
        this.pantryDAO = new PantryDAO();

        setLayout(new BorderLayout(0, 0));
        setBackground(new Color(245, 245, 245));

        add(buildHeader(),  BorderLayout.NORTH);
        add(buildTable(),   BorderLayout.CENTER);
        add(buildToolbar(), BorderLayout.SOUTH);
    }

    //  HEADER — title + Match Recipes button
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0,
                new Color(220, 220, 220)),
            BorderFactory.createEmptyBorder(16, 20, 16, 20)
        ));

        // Left: title + subtitle 
        JPanel titleGroup = new JPanel();
        titleGroup.setLayout(new BoxLayout(titleGroup, BoxLayout.Y_AXIS));
        titleGroup.setBackground(Color.WHITE);

        JLabel title = new JLabel("My Pantry");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(new Color(30, 30, 30));

        JLabel subtitle = new JLabel(
            "Ingredients you currently have at home");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitle.setForeground(new Color(140, 140, 140));

        titleGroup.add(title);
        titleGroup.add(Box.createVerticalStrut(2));
        titleGroup.add(subtitle);

        // Right: Match Recipes button
        JButton matchBtn = new JButton("Match Recipes");
        matchBtn.setFont(new Font("SansSerif", Font.PLAIN, 13));
        matchBtn.setBackground(new Color(34, 139, 87));
        matchBtn.setForeground(Color.WHITE);
        matchBtn.setFocusPainted(false);
        matchBtn.setBorderPainted(false);
        matchBtn.setOpaque(true);
        matchBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        matchBtn.setBorder(BorderFactory.createEmptyBorder(9, 18, 9, 18));
        matchBtn.setToolTipText(
            "Find recipes you can make with your current pantry");
        matchBtn.addActionListener(e -> navigateToSuggestions());

        header.add(titleGroup, BorderLayout.WEST);
        header.add(matchBtn,   BorderLayout.EAST);

        return header;
    }

    //  TABLE
    private JScrollPane buildTable() {
        String[] columns = { "ID", "Name", "Quantity", "Unit" };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
            @Override
            public Class<?> getColumnClass(int col) {
                if (col == COL_INGREDIENT_ID) return Integer.class;
                if (col == COL_QUANTITY)      return String.class;
                return String.class;
            }
        };

        table = new JTable(tableModel);
        table.setFont(new Font("SansSerif", Font.PLAIN, 14));
        table.setRowHeight(38);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setBackground(Color.WHITE);
        table.setSelectionBackground(new Color(232, 240, 254));
        table.setSelectionForeground(new Color(30, 30, 30));

        // Header styling
        table.getTableHeader().setFont(
            new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setBackground(
            new Color(250, 250, 250));
        table.getTableHeader().setForeground(
            new Color(100, 100, 100));
        table.getTableHeader().setBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0,
                new Color(220, 220, 220)));

        // Hide the ID column
        table.getColumnModel().getColumn(COL_INGREDIENT_ID)
            .setMinWidth(0);
        table.getColumnModel().getColumn(COL_INGREDIENT_ID)
            .setMaxWidth(0);
        table.getColumnModel().getColumn(COL_INGREDIENT_ID)
            .setWidth(0);

        // Column widths
        table.getColumnModel().getColumn(COL_NAME)
            .setPreferredWidth(300);
        table.getColumnModel().getColumn(COL_QUANTITY)
            .setPreferredWidth(120);
        table.getColumnModel().getColumn(COL_UNIT)
            .setPreferredWidth(120);

        // Capitalize first letter of ingredient name
        DefaultTableCellRenderer nameRenderer =
            new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object value, boolean isSelected,
                    boolean hasFocus, int row, int col) {
                if (value instanceof String s && !s.isEmpty()) {
                    value = Character.toUpperCase(s.charAt(0))
                        + s.substring(1);
                }
                super.getTableCellRendererComponent(
                    t, value, isSelected, hasFocus, row, col);
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                if (!isSelected) {
                    setBackground(row % 2 == 0
                        ? Color.WHITE
                        : new Color(250, 250, 252));
                }
                return this;
            }
        };

        // Quantity renderer — right-aligned
        DefaultTableCellRenderer qtyRenderer =
            new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object value, boolean isSelected,
                    boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(
                    t, value, isSelected, hasFocus, row, col);
                setHorizontalAlignment(SwingConstants.RIGHT);
                setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 20));
                if (!isSelected) {
                    setBackground(row % 2 == 0
                        ? Color.WHITE
                        : new Color(250, 250, 252));
                }
                return this;
            }
        };

        // Unit renderer — muted color
        DefaultTableCellRenderer unitRenderer =
            new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object value, boolean isSelected,
                    boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(
                    t, value, isSelected, hasFocus, row, col);
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                if (!isSelected) {
                    setForeground(new Color(120, 120, 120));
                    setBackground(row % 2 == 0
                        ? Color.WHITE
                        : new Color(250, 250, 252));
                } else {
                    setForeground(new Color(30, 30, 30));
                }
                return this;
            }
        };

        table.getColumnModel().getColumn(COL_NAME)
            .setCellRenderer(nameRenderer);
        table.getColumnModel().getColumn(COL_QUANTITY)
            .setCellRenderer(qtyRenderer);
        table.getColumnModel().getColumn(COL_UNIT)
            .setCellRenderer(unitRenderer);

        // Enable/disable Edit + Remove on selection change
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean selected = table.getSelectedRow() != -1;
                editBtn.setEnabled(selected);
                removeBtn.setEnabled(selected);
            }
        });

        // Double-click → open edit dialog
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2
                        && table.getSelectedRow() != -1) {
                    openEditDialog();
                }
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);

        return scroll;
    }

    //  TOOLBAR — Add / Edit / Remove + count
    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0,
                new Color(220, 220, 220)),
            BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));

        JPanel btnGroup = new JPanel(new FlowLayout(
            FlowLayout.LEFT, 8, 0));
        btnGroup.setBackground(Color.WHITE);

        addBtn    = makeButton("+ Add",
            new Color(34, 139, 87), Color.WHITE);
        editBtn   = makeButton("Edit",
            new Color(245, 245, 245), new Color(60, 60, 60));
        removeBtn = makeButton("Remove",
            new Color(245, 245, 245), new Color(60, 60, 60));

        editBtn.setEnabled(false);
        removeBtn.setEnabled(false);

        addBtn.addActionListener(   e -> openAddDialog());
        editBtn.addActionListener(  e -> openEditDialog());
        removeBtn.addActionListener(e -> removeSelectedItem());

        btnGroup.add(addBtn);
        btnGroup.add(editBtn);
        btnGroup.add(removeBtn);

        countLabel = new JLabel();
        countLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        countLabel.setForeground(new Color(140, 140, 140));

        bar.add(btnGroup,   BorderLayout.WEST);
        bar.add(countLabel, BorderLayout.EAST);

        return bar;
    }

    private JButton makeButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 13));
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(7, 16, 7, 16));
        return btn;
    }

    //  DATA
    public void loadPantry() {
        tableModel.setRowCount(0);

        try {
            List<PantryItem> items = pantryDAO.getPantryByUser(
                frame.getCurrentUserId());
            for (PantryItem item : items) {
                tableModel.addRow(new Object[]{
                    item.getIngredientId(),
                    item.getIngredientName(),
                    item.getFormattedQuantity(),
                    item.getUnit()
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Failed to load pantry: " + e.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE);
        }

        updateCountLabel();
    }

    private void updateCountLabel() {
        int count = tableModel.getRowCount();
        countLabel.setText(count + " ingredient"
            + (count == 1 ? "" : "s"));
    }

    //  ACTIONS
    private void openAddDialog() {
        AddEditIngredientDialog dialog =
            new AddEditIngredientDialog(frame, null);
        dialog.setVisible(true);
        if (dialog.isSaved()) loadPantry();
    }

    private void openEditDialog() {
        int viewRow = table.getSelectedRow();
        if (viewRow == -1) return;

        PantryItem item = getSelectedPantryItem();
        if (item == null) return;

        AddEditIngredientDialog dialog =
            new AddEditIngredientDialog(frame, item);
        dialog.setVisible(true);
        if (dialog.isSaved()) loadPantry();
    }

    private void removeSelectedItem() {
        int viewRow = table.getSelectedRow();
        if (viewRow == -1) return;

        int modelRow = table.convertRowIndexToModel(viewRow);
        String name  = (String) tableModel
            .getValueAt(modelRow, COL_NAME);

        int confirm = JOptionPane.showConfirmDialog(this,
            "Remove \"" + name + "\" from your pantry?",
            "Confirm Remove",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                int ingredientId = (int) tableModel
                    .getValueAt(modelRow, COL_INGREDIENT_ID);
                pantryDAO.deletePantryItem(
                    ingredientId, frame.getCurrentUserId());
                loadPantry();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this,
                    "Failed to remove item: " + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // Navigates to Suggestions panel and triggers a match
    private void navigateToSuggestions() {
        frame.navigateTo(MainFrame.CARD_SUGGESTIONS);
    }

    // Reconstructs a PantryItem from the selected row
    private PantryItem getSelectedPantryItem() {
        int viewRow  = table.getSelectedRow();
        if (viewRow == -1) return null;
        int modelRow = table.convertRowIndexToModel(viewRow);

        int    ingredientId = (int)    tableModel
            .getValueAt(modelRow, COL_INGREDIENT_ID);
        String name         = (String) tableModel
            .getValueAt(modelRow, COL_NAME);
        String qtyStr       = (String) tableModel
            .getValueAt(modelRow, COL_QUANTITY);
        String unit         = (String) tableModel
            .getValueAt(modelRow, COL_UNIT);

        double qty;
        try { qty = Double.parseDouble(qtyStr); }
        catch (NumberFormatException e) { qty = 0; }

        return new PantryItem(
            ingredientId,
            frame.getCurrentUserId(),
            qty,
            unit,
            name
        );
    }

    // Reload when panel becomes visible
    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) loadPantry();
    }
}
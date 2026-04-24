package com.lefestin.ui.panels;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;

import com.lefestin.dao.PantryDAO;
import com.lefestin.model.PantryItem;
import com.lefestin.ui.AppTheme;
import com.lefestin.ui.MainFrame;

import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;
import java.util.List;

/**
 * PantryPanel — the virtual pantry browser.
 * Now extends BaseListPanel to share header structure.
 */
public class PantryPanel extends BaseListPanel {

    // Deps
    private final PantryDAO pantryDAO;

    // Table look
    private JTable table;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;

    // Col indxs
    private static final int COL_INGREDIENT_ID = 0; // hidden
    private static final int COL_NAME = 1;
    private static final int COL_QUANTITY = 2;
    private static final int COL_UNIT = 3;

    public PantryPanel(MainFrame frame) {
        super(frame);
        this.pantryDAO = new PantryDAO();
    }

    @Override
    protected String getHeaderTitle() {
        return "My Pantry";
    }

    @Override
    protected String getHeaderDescription() {
        return "Ingredients you currently have at home";
    }

    @Override
    protected String getSearchPlaceholder() {
        return "Search ingredients...";
    }

    @Override
    protected JComponent buildHeaderRightControl() {
        return Box.createHorizontalBox(); // empty — no filter needed for pantry
    }
    

    @Override
    protected JComponent buildSearchRightControl() {
        JButton matchBtn = AppTheme.primaryButton("Match Recipes");
        matchBtn.setToolTipText("Find recipes you can make with your current pantry");
        matchBtn.addActionListener(e -> navigateToSuggestions());
        return matchBtn;
    }

    @Override
    protected JComponent buildTableContent() {
        return buildTable();
    }

    @Override
    protected JPanel buildToolbar() {
        return buildStandardToolbar();
    }
    
    @Override
    protected JButton createActionButton() {
        return AppTheme.dangerButton("Remove");
    }
    
    @Override
    protected void onActionClicked() {
        removeSelectedItem();
    }

    //  TABLE
    private JScrollPane buildTable() {
        // Set up search listener on inherited searchField
        searchField.getDocument().addDocumentListener(
            new javax.swing.event.DocumentListener() {
                public void insertUpdate (javax.swing.event.DocumentEvent e) { applyFilters(); }
                public void removeUpdate (javax.swing.event.DocumentEvent e) { applyFilters(); }
                public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilters(); }
            }
        );

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
        AppTheme.styleTable(table);

        // sorter — enables search filtering and column sorting
        sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        // hide ID column — keep as-is
        table.getColumnModel().getColumn(COL_INGREDIENT_ID).setMinWidth(0);
        table.getColumnModel().getColumn(COL_INGREDIENT_ID).setMaxWidth(0);
        table.getColumnModel().getColumn(COL_INGREDIENT_ID).setWidth(0);

        // column widths — keep as-is
        table.getColumnModel().getColumn(COL_NAME)    .setPreferredWidth(300);
        table.getColumnModel().getColumn(COL_QUANTITY).setPreferredWidth(120);
        table.getColumnModel().getColumn(COL_UNIT)    .setPreferredWidth(120);

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
                    setBackground(row % 2 == 0 ? AppTheme.BG_SURFACE : AppTheme.BG_SUBTLE);
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
                    setBackground(row % 2 == 0 ? AppTheme.BG_SURFACE : AppTheme.BG_SUBTLE);
                }
                return this;
            }
        };

        // Unit renderer — same color as other columns
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
                    setBackground(row % 2 == 0 ? AppTheme.BG_SURFACE : AppTheme.BG_SUBTLE);
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
                actionBtn.setEnabled(selected);
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(AppTheme.BG_SURFACE);

        return scroll;
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

        updateCountLabelDisplay();
    }

    private void updateCountLabelDisplay() {
        int visible = table.getRowCount();
        int total   = tableModel.getRowCount();
        String text = visible == total
            ? total + " ingredient" + (total == 1 ? "" : "s")
            : visible + " of " + total + " ingredients";
        updateCountLabel(text);
    }

    /**
     * Applies search text filter using TableRowSorter.
     * Filters ingredient names case-insensitively.
     */
    private void applyFilters() {
        String text = searchField.getText().trim();
        
        if (text.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            // Filter by ingredient name (column 1), case-insensitive
            sorter.setRowFilter(
                RowFilter.regexFilter("(?i)" + text, COL_NAME)
            );
        }
        
        updateCountLabelDisplay();
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

    @Override
    protected void onAddClicked() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'onAddClicked'");
    }

    @Override
    protected void onEditClicked() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'onEditClicked'");
    }
}
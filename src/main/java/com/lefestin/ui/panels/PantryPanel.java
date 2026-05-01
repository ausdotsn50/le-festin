package com.lefestin.ui.panels;

import java.awt.Component;
import java.sql.SQLException;
import java.util.List;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import com.lefestin.dao.PantryDAO;
import com.lefestin.model.PantryItem;
import com.lefestin.ui.AppTheme;
import com.lefestin.ui.MainFrame;
import com.lefestin.ui.dialogs.AddEditIngredientDialog;

/**
 * PantryPanel — the virtual pantry browser.
 */
public class PantryPanel extends BaseListPanel {

    private final PantryDAO pantryDAO;
    private JTable table;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;

    private static final int COL_INGREDIENT_ID = 0;
    private static final int COL_NAME = 1;
    private static final int COL_QUANTITY = 2;
    private static final int COL_UNIT = 3;

    public PantryPanel(MainFrame frame) {
        super(frame);
        this.pantryDAO = new PantryDAO();
    }

    // --- Header Configuration ---

    @Override
    protected String getHeaderTitle() { return "My Pantry"; }

    @Override
    protected String getHeaderDescription() { return "Ingredients you currently have at home"; }

    @Override
    protected String getSearchPlaceholder() { return "Search ingredients..."; }

    @Override
    protected JComponent buildHeaderRightControl() { return Box.createHorizontalBox(); }

    @Override
    protected JComponent buildSearchRightControl() {
        JButton matchBtn = AppTheme.primaryButton("Match Recipes");
        matchBtn.setToolTipText("Find recipes you can make with your current pantry");
        matchBtn.addActionListener(e -> navigateToSuggestions());
        return matchBtn;
    }

    // --- UI Structure ---

    @Override
    protected JComponent buildTableContent() { return buildTable(); }

    @Override
    protected JPanel buildToolbar() { return buildStandardToolbar(); }

    @Override
    protected JButton createActionButton() { return AppTheme.dangerButton("Remove"); }

    // --- Actions ---

    @Override
    protected void onAddClicked() {
        AddEditIngredientDialog dialog = new AddEditIngredientDialog(frame, null);
        dialog.setVisible(true);
        if (dialog.isSaved()) loadPantry();
    }

    @Override
    protected void onEditClicked() {
        PantryItem selectedItem = getSelectedPantryItem();
        if (selectedItem != null) {
            AddEditIngredientDialog dialog = new AddEditIngredientDialog(frame, selectedItem);
            dialog.setVisible(true);
            if (dialog.isSaved()) loadPantry();
        }
    }

    @Override
    protected void onActionClicked() { removeSelectedItem(); }

    // --- Table Construction ---

    private JScrollPane buildTable() {
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { applyFilters(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { applyFilters(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilters(); }
        });

        tableModel = new DefaultTableModel(new String[]{ "ID", "Name", "Quantity", "Unit" }, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
            @Override public Class<?> getColumnClass(int col) {
                return (col == COL_INGREDIENT_ID) ? Integer.class : String.class;
            }
        };

        table = new JTable(tableModel);
        AppTheme.styleTable(table);
        sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        configureTableColumns();
        configureRenderers();

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

    private void configureTableColumns() {
        table.getColumnModel().getColumn(COL_INGREDIENT_ID).setMinWidth(0);
        table.getColumnModel().getColumn(COL_INGREDIENT_ID).setMaxWidth(0);
        table.getColumnModel().getColumn(COL_INGREDIENT_ID).setWidth(0);

        table.getColumnModel().getColumn(COL_NAME).setPreferredWidth(300);
        table.getColumnModel().getColumn(COL_QUANTITY).setPreferredWidth(120);
        table.getColumnModel().getColumn(COL_UNIT).setPreferredWidth(120);
    }

    private void configureRenderers() {
        // Name Renderer (Capitalized)
        table.getColumnModel().getColumn(COL_NAME).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean h, int r, int c) {
                if (v instanceof String str && !str.isEmpty()) {
                    v = Character.toUpperCase(str.charAt(0)) + str.substring(1);
                }
                super.getTableCellRendererComponent(t, v, s, h, r, c);
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                if (!s) setBackground(r % 2 == 0 ? AppTheme.BG_SURFACE : AppTheme.BG_SUBTLE);
                return this;
            }
        });

        // Quantity Renderer (Right Aligned)
        table.getColumnModel().getColumn(COL_QUANTITY).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean h, int r, int c) {
                super.getTableCellRendererComponent(t, v, s, h, r, c);
                setHorizontalAlignment(SwingConstants.RIGHT);
                setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 20));
                if (!s) setBackground(r % 2 == 0 ? AppTheme.BG_SURFACE : AppTheme.BG_SUBTLE);
                return this;
            }
        });

        // Unit Renderer
        table.getColumnModel().getColumn(COL_UNIT).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean h, int r, int c) {
                super.getTableCellRendererComponent(t, v, s, h, r, c);
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                if (!s) setBackground(r % 2 == 0 ? AppTheme.BG_SURFACE : AppTheme.BG_SUBTLE);
                return this;
            }
        });
    }

    // --- Data Management ---

    public void loadPantry() {
        tableModel.setRowCount(0);
        try {
            List<PantryItem> items = pantryDAO.getPantryByUser(frame.getCurrentUserId());
            for (PantryItem item : items) {
                tableModel.addRow(new Object[]{
                    item.getIngredientId(),
                    item.getIngredientName(),
                    item.getFormattedQuantity(),
                    item.getUnit()
                });
            }
        } catch (SQLException e) {
            handleError("Failed to load pantry", e);
        }
        updateCountLabelDisplay();
    }

    private void applyFilters() {
        String text = searchField.getText().trim();
        sorter.setRowFilter(text.isEmpty() ? null : RowFilter.regexFilter("(?i)" + text, COL_NAME));
        updateCountLabelDisplay();
    }

    private void removeSelectedItem() {
        PantryItem item = getSelectedPantryItem();
        if (item == null) return;

        int confirm = JOptionPane.showConfirmDialog(this,
            "Remove \"" + item.getIngredientName() + "\" from your pantry?",
            "Confirm Remove", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                pantryDAO.deletePantryItem(item.getIngredientId(), frame.getCurrentUserId());
                loadPantry();
            } catch (SQLException e) {
                handleError("Failed to remove item", e);
            }
        }
    }

    private PantryItem getSelectedPantryItem() {
        int viewRow = table.getSelectedRow();
        if (viewRow == -1) return null;
        int modelRow = table.convertRowIndexToModel(viewRow);

        int id = (int) tableModel.getValueAt(modelRow, COL_INGREDIENT_ID);
        String name = (String) tableModel.getValueAt(modelRow, COL_NAME);
        String qtyStr = (String) tableModel.getValueAt(modelRow, COL_QUANTITY);
        String unit = (String) tableModel.getValueAt(modelRow, COL_UNIT);

        double qty;
        try { qty = Double.parseDouble(qtyStr); } catch (Exception e) { qty = 0; }

        return new PantryItem(id, frame.getCurrentUserId(), qty, unit, name);
    }

    // --- Helpers ---

    private void updateCountLabelDisplay() {
        int visible = table.getRowCount();
        int total = tableModel.getRowCount();
        String text = (visible == total) 
            ? total + " ingredient" + (total == 1 ? "" : "s")
            : visible + " of " + total + " ingredients";
        updateCountLabel(text);
    }

    private void navigateToSuggestions() { frame.navigateTo(MainFrame.CARD_SUGGESTIONS); }

    private void handleError(String message, Exception e) {
        JOptionPane.showMessageDialog(this, message + ": " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) loadPantry();
    }
}
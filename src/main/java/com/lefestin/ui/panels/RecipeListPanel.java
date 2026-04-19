package com.lefestin.ui.panels;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import com.lefestin.dao.RecipeDAO;
import com.lefestin.model.Recipe;
import com.lefestin.ui.AppTheme;
import com.lefestin.ui.MainFrame;
import com.lefestin.ui.dialogs.AddEditRecipeDialog;

import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;
import java.util.List;

/**
 * RecipeListPanel — the main recipe browser.
 * Now extends BaseListPanel to share header structure.
 */
public class RecipeListPanel extends BaseListPanel {
    // Dependencies
    private final RecipeDAO recipeDAO;

    // For the table layout view
    private JTable table;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;

    // Search filter components
    private JComboBox<String> categoryFilter;

    // Col indexes
    private static final int COL_ID = 0;
    private static final int COL_TITLE = 1;
    private static final int COL_CATEGORY = 2;
    private static final int COL_PREP = 3;

    // Constructor
    public RecipeListPanel(MainFrame frame) {
        super(frame);
        this.recipeDAO = new RecipeDAO();
    }

    @Override
    protected String getHeaderTitle() {
        return "Recipes";
    }

    @Override
    protected String getHeaderDescription() {
        return "Browse and manage your recipe collection";
    }

    @Override
    protected String getSearchPlaceholder() {
        return "Search recipes...";
    }

    @Override
    protected JComponent buildHeaderRightControl() {
        return Box.createHorizontalBox(); // empty — no header button
    }

    @Override
    protected JComponent buildSearchRightControl() {
        // Category dropdown next to search field
        categoryFilter = new JComboBox<>(new String[]{
            "All",
            Recipe.CATEGORY_BREAKFAST,
            Recipe.CATEGORY_LUNCH,
            Recipe.CATEGORY_DINNER,
        });
        categoryFilter.setFont(AppTheme.FONT_BODY);
        categoryFilter.setBackground(AppTheme.BG_SURFACE);
        categoryFilter.setForeground(AppTheme.TEXT_PRIMARY);
        categoryFilter.setPreferredSize(new Dimension(140, 36));
        
        // Renderer for consistent styling with theme colors
        categoryFilter.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
                
                if (isSelected) {
                    setBackground(AppTheme.SELECTION_BG);
                    setForeground(AppTheme.SELECTION_FG);
                } else {
                    setBackground(AppTheme.BG_SURFACE);
                    setForeground(AppTheme.TEXT_PRIMARY);
                }
                
                return this;
            }
        });
        
        categoryFilter.addActionListener(e -> applyFilters());

        return categoryFilter;
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
        return AppTheme.dangerButton("Delete");
    }
    
    @Override
    protected void onAddClicked() {
        openAddDialog();
    }
    
    @Override
    protected void onEditClicked() {
        openEditDialog();
    }
    
    @Override
    protected void onActionClicked() {
        deleteSelectedRecipe();
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

        // Column names — ID hidden but kept for selection lookups
        String[] columns = { "ID", "Title", "Category", "Prep Time" };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false; // read-only — edits go through the dialog
            }
            @Override
            public Class<?> getColumnClass(int col) {
                return col == COL_ID ? Integer.class : String.class;
            }
        };

        table = new JTable(tableModel);
        AppTheme.styleTable(table);

        // hide ID column — keep as-is
        table.getColumnModel().getColumn(COL_ID).setMinWidth(0);
        table.getColumnModel().getColumn(COL_ID).setMaxWidth(0);
        table.getColumnModel().getColumn(COL_ID).setWidth(0);

        // column widths — keep as-is
        table.getColumnModel().getColumn(COL_TITLE)   .setPreferredWidth(340);
        table.getColumnModel().getColumn(COL_CATEGORY).setPreferredWidth(120);
        table.getColumnModel().getColumn(COL_PREP)    .setPreferredWidth(100);

        // sorter — keep as-is
        sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        // Sorter — enables column header click-to-sort
        sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        // Enable/disable Edit + Delete based on selection
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean selected = table.getSelectedRow() != -1;
                editBtn.setEnabled(selected);
                actionBtn.setEnabled(selected);
                updateCountLabelDisplay();
            }
        });

        // Double-click row → open edit dialog
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() != -1) {
                    openEditDialog();
                }
            }
        });

        // Alternating row colors via custom renderer
        table.setDefaultRenderer(Object.class,
            AppTheme.alternatingRowRenderer());
        
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(AppTheme.BG_SURFACE);

        return scroll;
    }

    //  DATA — load, filter, refresh
    /**
     * Loads all recipes for the current user from the DB
     * and populates the table. Call this on panel show and after
     * any add/edit/delete operation.
     */

    public void loadRecipes() {
        tableModel.setRowCount(0); // clear existing rows

        try {
            List<Recipe> recipes = recipeDAO.getAllRecipes(
                frame.getCurrentUserId()
            );
            for (Recipe r : recipes) {
                tableModel.addRow(new Object[]{
                    r.getRecipeId(),
                    r.getTitle(),
                    r.getCategory(),
                    r.getFormattedPrepTime()
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Failed to load recipes: " + e.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE);
        }

        updateCountLabelDisplay();
    }

    /**
     * Applies search text + category filter simultaneously.
     * Uses TableRowSorter's RowFilter — no DB call needed.
     */
    private void applyFilters() {
        String  text     = searchField.getText().trim();
        String  category = (String) categoryFilter.getSelectedItem();
        boolean hasText  = !text.isEmpty();
        boolean hasCategory = !"All".equals(category);

        if (!hasText && !hasCategory) {
            sorter.setRowFilter(null);
        } else if (hasText && !hasCategory) {
            // Filter by title only (column 1), case-insensitive
            sorter.setRowFilter(
                RowFilter.regexFilter("(?i)" + text, COL_TITLE)
            );
        } else if (!hasText) {
            // Filter by category only (column 2), exact match
            sorter.setRowFilter(
                RowFilter.regexFilter("(?i)^" + category + "$", COL_CATEGORY)
            );
        } else {
            // Both filters active — AND them together
            sorter.setRowFilter(RowFilter.andFilter(List.of(
                RowFilter.regexFilter("(?i)" + text,               COL_TITLE),
                RowFilter.regexFilter("(?i)^" + category + "$",    COL_CATEGORY)
            )));
        }

        updateCountLabelDisplay();
    }

    // Updates the count label to reflect visible filtered rows
    private void updateCountLabelDisplay() {
        int visible = table.getRowCount();
        int total   = tableModel.getRowCount();
        String text = visible == total
            ? total + " recipe" + (total == 1 ? "" : "s")
            : visible + " of " + total + " recipes";
        updateCountLabel(text);
    }


    //  ACTIONS
    private void openAddDialog() {
        AddEditRecipeDialog dialog =
            new AddEditRecipeDialog(frame, null);
        dialog.setVisible(true);
        if (dialog.isSaved()) loadRecipes();  // refresh if saved
    }

    private void openEditDialog() {
        int viewRow  = table.getSelectedRow();
        if (viewRow == -1) return;
        int modelRow = table.convertRowIndexToModel(viewRow);
        int recipeId = (int) tableModel.getValueAt(modelRow, COL_ID);

        try {
            Recipe recipe = recipeDAO.getRecipeById(recipeId);
            AddEditRecipeDialog dialog =
                new AddEditRecipeDialog(frame, recipe);
            dialog.setVisible(true);
            if (dialog.isSaved()) loadRecipes();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Failed to load recipe: " + e.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    

    private void deleteSelectedRecipe() {
        int viewRow  = table.getSelectedRow();
        if (viewRow == -1) return;
        int modelRow = table.convertRowIndexToModel(viewRow);
        int recipeId = (int) tableModel.getValueAt(modelRow, COL_ID);
        String title = (String) tableModel.getValueAt(modelRow, COL_TITLE);

        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete \"" + title + "\"? This cannot be undone.",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                recipeDAO.deleteRecipe(recipeId);
                loadRecipes();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this,
                    "Failed to delete recipe: " + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // Reload when panel becomes visible
    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) loadRecipes();
    }
}
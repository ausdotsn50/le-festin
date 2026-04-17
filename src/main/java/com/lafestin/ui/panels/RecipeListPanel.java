package com.lafestin.ui.panels;

import com.lafestin.dao.RecipeDAO;
import com.lafestin.model.Recipe;
import com.lafestin.ui.MainFrame;
import com.lafestin.ui.dialogs.AddEditRecipeDialog;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;
import java.util.List;


/**
 * RecipeListPanel — the main recipe browser.
 *
 */
public class RecipeListPanel extends JPanel {

    // Dependencies
    private final MainFrame frame;
    private final RecipeDAO recipeDAO;

    // For the table layout view
    private JTable table;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;

    // Top bar components
    private JTextField searchField;
    private JComboBox<String> categoryFilter;

    // Toolbar components
    private JButton addBtn;
    private JButton editBtn;
    private JButton deleteBtn;
    private JLabel  countLabel;

    // Col indexes
    private static final int COL_ID = 0;
    private static final int COL_TITLE = 1;
    private static final int COL_CATEGORY = 2;
    private static final int COL_PREP = 3;

    // Constructor
    public RecipeListPanel(MainFrame frame) {
        this.frame     = frame;
        this.recipeDAO = new RecipeDAO();

        setLayout(new BorderLayout(0, 0));
        setBackground(new Color(245, 245, 245));

        add(buildTopBar(), BorderLayout.NORTH);
        add(buildTable(),  BorderLayout.CENTER);
        add(buildToolbar(),BorderLayout.SOUTH);
    }

    //  TOP BAR — search + category filter
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout(10, 0));
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 220, 220)),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        // Search field
        searchField = new JTextField();
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        searchField.putClientProperty("JTextField.placeholderText",
            "Search recipes...");

        // Live filter, searching on keystrokes
        searchField.getDocument().addDocumentListener(
            new javax.swing.event.DocumentListener() {
                public void insertUpdate (javax.swing.event.DocumentEvent e) { applyFilters(); }
                public void removeUpdate (javax.swing.event.DocumentEvent e) { applyFilters(); }
                public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilters(); }
            }
        );

        // Category dropdown
        categoryFilter = new JComboBox<>(new String[]{
            "All",
            Recipe.CATEGORY_BREAKFAST,
            Recipe.CATEGORY_LUNCH,
            Recipe.CATEGORY_DINNER,
        });
        categoryFilter.setFont(new Font("SansSerif", Font.PLAIN, 13));
        categoryFilter.setPreferredSize(new Dimension(140, 36));
        categoryFilter.addActionListener(e -> applyFilters());

        bar.add(searchField,    BorderLayout.CENTER);
        bar.add(categoryFilter, BorderLayout.EAST);

        return bar;
    }

    //  TABLE
    private JScrollPane buildTable() {
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
        table.setFont(new Font("SansSerif", Font.PLAIN, 14));
        table.setRowHeight(36);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setBackground(Color.WHITE);
        table.setSelectionBackground(new Color(232, 240, 254));
        table.setSelectionForeground(new Color(30, 30, 30));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(250, 250, 250));
        table.getTableHeader().setBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 220, 220))
        );

        // Hide the ID column — present in model, invisible in UI
        table.getColumnModel().getColumn(COL_ID).setMinWidth(0);
        table.getColumnModel().getColumn(COL_ID).setMaxWidth(0);
        table.getColumnModel().getColumn(COL_ID).setWidth(0);

        // Column widths
        table.getColumnModel().getColumn(COL_TITLE)   .setPreferredWidth(340);
        table.getColumnModel().getColumn(COL_CATEGORY).setPreferredWidth(120);
        table.getColumnModel().getColumn(COL_PREP)    .setPreferredWidth(100);

        // Sorter — enables column header click-to-sort
        sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        // Enable/disable Edit + Delete based on selection
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean selected = table.getSelectedRow() != -1;
                editBtn.setEnabled(selected);
                deleteBtn.setEnabled(selected);
                updateCountLabel();
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
            new javax.swing.table.DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(
                        JTable t, Object value, boolean isSelected,
                        boolean hasFocus, int row, int col) {
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
            }
        );

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);

        return scroll;
    }

    //  TOOLBAR — Add / Edit / Delete + count label
    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 220, 220)),
            BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));

        // Left: action buttons
        JPanel btnGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnGroup.setBackground(Color.WHITE);

        addBtn    = makeToolbarButton("+ Add",  new Color(34, 139, 87),  Color.WHITE);
        editBtn   = makeToolbarButton("Edit",   new Color(245, 245, 245),new Color(60, 60, 60));
        deleteBtn = makeToolbarButton("Delete", new Color(245, 245, 245),new Color(60, 60, 60));

        // Edit + Delete start disabled — enabled when a row is selected
        editBtn.setEnabled(false);
        deleteBtn.setEnabled(false);

        addBtn.addActionListener(   e -> openAddDialog());
        editBtn.addActionListener(  e -> openEditDialog());
        deleteBtn.addActionListener(e -> deleteSelectedRecipe());

        btnGroup.add(addBtn);
        btnGroup.add(editBtn);
        btnGroup.add(deleteBtn);

        // Right: row count
        countLabel = new JLabel();
        countLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        countLabel.setForeground(new Color(140, 140, 140));

        bar.add(btnGroup,   BorderLayout.WEST);
        bar.add(countLabel, BorderLayout.EAST);

        return bar;
    }

    // Styled toolbar button factory
    private JButton makeToolbarButton(String text, Color bg, Color fg) {
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

        updateCountLabel();
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

        updateCountLabel();
    }

    // Updates the count label to reflect visible filtered rows
    private void updateCountLabel() {
        int visible = table.getRowCount();
        int total   = tableModel.getRowCount();
        countLabel.setText(visible == total
            ? total + " recipe" + (total == 1 ? "" : "s")
            : visible + " of " + total + " recipes");
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

    // Called by MainFrame when this panel becomes visible
    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) System.out.print("Visible recipe list panel");// loadRecipes();
    }
}
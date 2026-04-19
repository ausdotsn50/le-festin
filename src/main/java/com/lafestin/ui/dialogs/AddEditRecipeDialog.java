package com.lafestin.ui.dialogs;

import com.lafestin.dao.IngredientDAO;
import com.lafestin.dao.RecipeDAO;
import com.lafestin.dao.RecipeIngredientDAO;
import com.lafestin.model.Ingredient;
import com.lafestin.model.Recipe;
import com.lafestin.model.RecipeIngredient;
import com.lafestin.ui.AppTheme;
import com.lafestin.ui.MainFrame;
import com.lafestin.helper.Helper;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * AddEditRecipeDialog — modal form for creating and editing recipes.
 */
public class AddEditRecipeDialog extends JDialog {
    private final MainFrame frame;
    private final Recipe existingRecipe; // null = add mode
    private final RecipeDAO recipeDAO;
    private final RecipeIngredientDAO riDAO;
    private final IngredientDAO ingredientDAO;

    // Result flag
    private boolean saved = false;

    // Form fields
    private JTextField titleField;
    private JComboBox<String> categoryCombo;
    private JSpinner prepTimeSpinner;
    private JTextArea procedureArea;

    // Ingredient table
    private JTable ingredientTable;
    private DefaultTableModel ingredientModel;

    // Available ingredients for dropdown
    private List<Ingredient> allIngredients = new ArrayList<>();

    // Column indexes in ingredient table
    private static final int COL_INGREDIENT = 0;
    private static final int COL_QUANTITY = 1;
    private static final int COL_UNIT = 2;

    public AddEditRecipeDialog(MainFrame frame, Recipe recipe) {
        super(frame,
            recipe == null ? "Add New Recipe" : "Edit Recipe",
            true); // modal

        this.frame = frame;
        this.existingRecipe = recipe;
        this.recipeDAO = new RecipeDAO();
        this.riDAO = new RecipeIngredientDAO();
        this.ingredientDAO = new IngredientDAO();
    
        allIngredients = Helper.loadAllIngredients(this, ingredientDAO);
        initComponents();
        prefillIfEditing();
        Helper.packAndCenter(frame, this, new Dimension(580, 680));
    }

    // Initialize dialog components
    private void initComponents() {
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(AppTheme.BG_PAGE);

        add(buildFormPanel(), BorderLayout.CENTER);
        add(buildButtonBar(), BorderLayout.SOUTH);

        // Escape key cancels
        getRootPane().registerKeyboardAction(
            e -> dispose(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    // Form panel
    private JPanel buildFormPanel() {
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(AppTheme.BG_PAGE);
        form.setBorder(BorderFactory.createEmptyBorder(20, 24, 12, 24));

        form.add(buildFieldRow("Title", buildTitleField()));
        form.add(Box.createVerticalStrut(12));
        form.add(buildFieldRow("Category", buildCategoryCombo()));
        form.add(Box.createVerticalStrut(12));
        form.add(buildFieldRow("Prep time", buildPrepTimeRow()));
        form.add(Box.createVerticalStrut(12));
        form.add(buildProcedureSection());
        form.add(Box.createVerticalStrut(16));
        form.add(buildIngredientSection());

        return form;
    }

    // Label + field row
    private JPanel buildFieldRow(String labelText, JComponent field) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setBackground(AppTheme.BG_PAGE);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel label = new JLabel(labelText);
        label.setFont(AppTheme.FONT_LABEL);
        label.setForeground(AppTheme.TEXT_SECONDARY);
        label.setPreferredSize(new Dimension(80, 28));

        row.add(label, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);

        return row;
    }

    // Title
    private JTextField buildTitleField() {
        titleField = new JTextField();
        titleField.setFont(AppTheme.FONT_BODY);
        titleField.setBackground(AppTheme.BG_SURFACE);
        titleField.setForeground(AppTheme.TEXT_PRIMARY);
        titleField.setBorder(AppTheme.BORDER_INPUT);
        return titleField;
    }

    private JComboBox<String> buildCategoryCombo() {
        categoryCombo = new JComboBox<>(new String[]{
            Recipe.CATEGORY_BREAKFAST,
            Recipe.CATEGORY_LUNCH,
            Recipe.CATEGORY_DINNER,
        });
        categoryCombo.setFont(AppTheme.FONT_BODY);
        categoryCombo.setBackground(AppTheme.BG_SURFACE);
        categoryCombo.setForeground(AppTheme.TEXT_PRIMARY);
        
        // Renderer for consistent styling with theme colors
        categoryCombo.setRenderer(new DefaultListCellRenderer() {
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
        
        return categoryCombo;
    }

    private JPanel buildPrepTimeRow() {
        prepTimeSpinner = new JSpinner(
            new SpinnerNumberModel(15, 1, 300, 1));
        prepTimeSpinner.setFont(AppTheme.FONT_BODY);
        ((JSpinner.DefaultEditor) prepTimeSpinner.getEditor())
            .getTextField().setBackground(AppTheme.BG_SURFACE);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setBackground(AppTheme.BG_PAGE);

        JLabel minsLabel = new JLabel(" mins");
        minsLabel.setFont(AppTheme.FONT_SMALL);
        minsLabel.setForeground(AppTheme.TEXT_MUTED);

        prepTimeSpinner.setPreferredSize(new Dimension(80, 32));
        row.add(prepTimeSpinner);
        row.add(minsLabel);

        return row;
    }

    // Procedure section for
    private JPanel buildProcedureSection() {
        JPanel section = new JPanel(new BorderLayout(0, 6));
        section.setBackground(AppTheme.BG_PAGE);
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

        JLabel label = new JLabel("Procedure");
        label.setFont(AppTheme.FONT_LABEL);
        label.setForeground(AppTheme.TEXT_SECONDARY);

        procedureArea = new JTextArea(5, 20);
        procedureArea.setFont(AppTheme.FONT_BODY);
        procedureArea.setBackground(AppTheme.BG_SURFACE);
        procedureArea.setForeground(AppTheme.TEXT_PRIMARY);
        procedureArea.setLineWrap(true);
        procedureArea.setWrapStyleWord(true);
        procedureArea.setBorder(
            BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JScrollPane scroll = new JScrollPane(procedureArea);
        scroll.setBorder(BorderFactory.createLineBorder(
            AppTheme.BG_BORDER, 1));

        section.add(label,  BorderLayout.NORTH);
        section.add(scroll, BorderLayout.CENTER);

        return section;
    }

    // Ingredients section
    // To do: debug this section
    private JPanel buildIngredientSection() {
        JPanel section = new JPanel(new BorderLayout(0, 8));
        section.setBackground(AppTheme.BG_PAGE);
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(AppTheme.BG_PAGE);

        JLabel label = new JLabel("Ingredients");
        label.setFont(AppTheme.FONT_LABEL);
        label.setForeground(AppTheme.TEXT_SECONDARY);

        JButton addRowBtn = AppTheme.secondaryButton("+ Add Row");
        addRowBtn.addActionListener(e -> addIngredientRow(
            null, 1.0, Helper.UNITS[0]));

        header.add(label,     BorderLayout.WEST);
        header.add(addRowBtn, BorderLayout.EAST);

        // Table
        ingredientModel = new DefaultTableModel(
            new String[]{"Ingredient", "Quantity", "Unit"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return true; // all cells editable
            }
        };

        // Ingredient table styles (needs fixing)
        ingredientTable = new JTable(ingredientModel);
        ingredientTable.setFont(AppTheme.FONT_BODY);
        ingredientTable.setRowHeight(34);
        ingredientTable.setBackground(AppTheme.BG_SURFACE);
        ingredientTable.setGridColor(AppTheme.BG_BORDER);
        ingredientTable.setShowGrid(true);
        
        // Selection config
        // Fix: to bad table row contrast colors
        ingredientTable.setSelectionBackground(AppTheme.SELECTION_BG);
        ingredientTable.setSelectionForeground(AppTheme.SELECTION_FG);
        ingredientTable.getTableHeader()
            .setFont(AppTheme.FONT_LABEL);
        ingredientTable.getTableHeader()
            .setBackground(AppTheme.BG_SUBTLE);

        // Ingredient column — JComboBox of all ingredients
        installIngredientCombo();

        // Install renderer for ingredient column to display names
        installIngredientRenderer();

        // Unit column — JComboBox of unit options
        installUnitCombo();

        // Column widths
        ingredientTable.getColumnModel()
            .getColumn(COL_INGREDIENT).setPreferredWidth(200);
        ingredientTable.getColumnModel()
            .getColumn(COL_QUANTITY).setPreferredWidth(80);
        ingredientTable.getColumnModel()
            .getColumn(COL_UNIT).setPreferredWidth(120);

        JScrollPane scroll = new JScrollPane(ingredientTable);
        scroll.setPreferredSize(new Dimension(0, 140));
        scroll.setBorder(BorderFactory.createLineBorder(
            AppTheme.BG_BORDER, 1));

        JButton removeRowBtn = AppTheme.dangerButton("Remove Row");
        removeRowBtn.addActionListener(e -> removeSelectedRow());

        JPanel footer = new JPanel(new FlowLayout(
            FlowLayout.RIGHT, 0, 0));
        footer.setBackground(AppTheme.BG_PAGE);
        footer.add(removeRowBtn);

        section.add(header, BorderLayout.NORTH);
        section.add(scroll, BorderLayout.CENTER);
        section.add(footer, BorderLayout.SOUTH);

        return section;
    }

    // Install ingredient JComboBox as cell editor
    private void installIngredientCombo() {
        JComboBox<Ingredient> combo = new JComboBox<>();

        for (Ingredient i : allIngredients) {
            combo.addItem(i);
        }

        // Display ingredient name, not the object toString()
        combo.setRenderer(new DefaultListCellRenderer() {
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
                
                if (value instanceof Ingredient) {
                    setText(Helper.capitalize(
                        ((Ingredient) value).getName()));
                }
                return this;
            }
        });

        combo.setFont(AppTheme.FONT_BODY);
        combo.setBackground(AppTheme.BG_SURFACE);
        combo.setForeground(AppTheme.TEXT_PRIMARY);
        ingredientTable.getColumnModel()
            .getColumn(COL_INGREDIENT)
            .setCellEditor(new DefaultCellEditor(combo));
    }

    // Install unit JComboBox as cell editor
    private void installUnitCombo() {
        JComboBox<String> combo = new JComboBox<>(Helper.UNITS);
        combo.setFont(AppTheme.FONT_BODY);
        combo.setBackground(AppTheme.BG_SURFACE);
        combo.setForeground(AppTheme.TEXT_PRIMARY);
        
        // Renderer for consistent white background with dark text
        combo.setRenderer(new DefaultListCellRenderer() {
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
        
        ingredientTable.getColumnModel()
            .getColumn(COL_UNIT)
            .setCellEditor(new DefaultCellEditor(combo));
    }

    // Install renderer for ingredient column to display names
    private void installIngredientRenderer() {
        ingredientTable.getColumnModel()
            .getColumn(COL_INGREDIENT)
            .setCellRenderer(new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(
                        JTable table, Object value, boolean isSelected,
                        boolean hasFocus, int row, int column) {
                    super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                    
                    if (value instanceof Ingredient) {
                        setText(Helper.capitalize(
                            ((Ingredient) value).getName()));
                    } else if (value != null) {
                        setText(value.toString());
                    }
                    
                    return this;
                }
            });
    }

    // Add one row to the ingredient table
    private void addIngredientRow(Ingredient ingredient,
                                   double quantity,
                                   String unit) {
        Ingredient selected = ingredient;

        // Default to first ingredient if none specified
        if (selected == null && !allIngredients.isEmpty()) {
            selected = allIngredients.get(0);
        }

        ingredientModel.addRow(new Object[]{
            selected,
            quantity,
            unit
        });
    }

    // Remove selected ingredient row
    private void removeSelectedRow() {
        // Stop any active cell editing first
        if (ingredientTable.isEditing()) {
            ingredientTable.getCellEditor().stopCellEditing();
        }

        int selectedRow = ingredientTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                "Select an ingredient row to remove.",
                "No Row Selected",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        ingredientModel.removeRow(selectedRow);
    }

    private JPanel buildButtonBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bar.setBackground(AppTheme.BG_SURFACE);
        bar.setBorder(BorderFactory.createCompoundBorder(
            AppTheme.BORDER_DIVIDER_TOP,
            BorderFactory.createEmptyBorder(12, 20, 12, 20)));

        JButton cancelBtn = AppTheme.secondaryButton("Cancel");
        JButton saveBtn   = AppTheme.primaryButton(
            existingRecipe == null ? "Save Recipe" : "Update Recipe");

        cancelBtn.addActionListener(e -> dispose());
        saveBtn.addActionListener(  e -> saveRecipe());

        // Enter key triggers save
        getRootPane().setDefaultButton(saveBtn);

        bar.add(cancelBtn);
        bar.add(saveBtn);

        return bar;
    }

    private void prefillIfEditing() {
        if (existingRecipe == null) return;

        titleField.setText(existingRecipe.getTitle());
        categoryCombo.setSelectedItem(existingRecipe.getCategory());
        prepTimeSpinner.setValue(existingRecipe.getPrepTime());
        procedureArea.setText(existingRecipe.getProcedure());
        procedureArea.setCaretPosition(0); // scroll to top

        // Load existing ingredient rows
        try {
            List<RecipeIngredient> existing =
                riDAO.getIngredientsByRecipeId(
                    existingRecipe.getRecipeId());

            for (RecipeIngredient ri : existing) {
                // Find the matching Ingredient object from allIngredients
                Ingredient match = findIngredientById(
                    ri.getIngredientId());
                addIngredientRow(match, ri.getQuantity(), ri.getUnit());
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Could not load existing ingredients: "
                + e.getMessage(),
                "Warning",
                JOptionPane.WARNING_MESSAGE);
        }
    }

    // Saving the recipe flow in comments
    private void saveRecipe() {
        // Stop any active cell editing
        if (ingredientTable.isEditing()) {
            ingredientTable.getCellEditor().stopCellEditing();
        }

        // Validate
        String validationError = validateForm();
        if (validationError != null) {
            JOptionPane.showMessageDialog(this,
                validationError,
                "Missing Information",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            // Build the recipe from form fields
            String title    = titleField.getText().trim();
            String category = (String) categoryCombo.getSelectedItem();
            int    prepTime = (int) prepTimeSpinner.getValue();
            String procedure= procedureArea.getText().trim();

            Recipe recipe;

            if (existingRecipe == null) {
                // ADD
                recipe = new Recipe(
                    frame.getCurrentUserId(),
                    title, category, prepTime, procedure);
                recipeDAO.addRecipe(recipe); // sets recipe.recipeId

            } else {
                // EDIT
                recipe = existingRecipe;
                recipe.setTitle(title);
                recipe.setCategory(category);
                recipe.setPrepTime(prepTime);
                recipe.setProcedure(procedure);
                recipeDAO.updateRecipe(recipe);
            }

            // Delete all existing rows first, then re-insert from table.
            // This handles add/remove/edit in one consistent pass.
            riDAO.deleteByRecipeId(recipe.getRecipeId());

            List<RecipeIngredient> ingredients = buildIngredientList(
                recipe.getRecipeId());
            riDAO.addAll(ingredients);

            saved = true;
            dispose();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Failed to save recipe: " + e.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private String validateForm() {
        if (titleField.getText().trim().isEmpty()) {
            return "Recipe title is required.";
        }
        if (titleField.getText().trim().length() > 150) {
            return "Title cannot exceed 150 characters.";
        }
        if (procedureArea.getText().trim().isEmpty()) {
            return "Procedure / instructions are required.";
        }
        if (ingredientModel.getRowCount() == 0) {
            return "Add at least one ingredient.";
        }
        // Check every ingredient row has a valid quantity
        for (int i = 0; i < ingredientModel.getRowCount(); i++) {
            Object qtyObj = ingredientModel.getValueAt(i, COL_QUANTITY);
            try {
                double qty = Double.parseDouble(
                    qtyObj.toString().trim());
                if (qty <= 0) {
                    return "Quantity on row " + (i + 1)
                        + " must be greater than 0.";
                }
            } catch (NumberFormatException e) {
                return "Invalid quantity on row " + (i + 1)
                    + ": \"" + qtyObj + "\"\n"
                    + "Enter a number (e.g. 2 or 0.5).";
            }
        }
        return null; // all valid
    }

    // Reads ingredient table into a List<RecipeIngredient>
    private List<RecipeIngredient> buildIngredientList(int recipeId) {
        List<RecipeIngredient> list = new ArrayList<>();

        for (int i = 0; i < ingredientModel.getRowCount(); i++) {
            Object ingObj  = ingredientModel.getValueAt(i, COL_INGREDIENT);
            Object qtyObj  = ingredientModel.getValueAt(i, COL_QUANTITY);
            Object unitObj = ingredientModel.getValueAt(i, COL_UNIT);

            if (ingObj == null) continue;

            int    ingredientId;
            String ingredientName;

            if (ingObj instanceof Ingredient) {
                ingredientId   = ((Ingredient) ingObj).getIngredientId();
                ingredientName = ((Ingredient) ingObj).getName();
            } else {
                // Fallback if cell value is a plain String
                Ingredient found = findIngredientByName(
                    ingObj.toString());
                if (found == null) continue;
                ingredientId   = found.getIngredientId();
                ingredientName = found.getName();
            }

            double qty;
            try {
                qty = Double.parseDouble(qtyObj.toString().trim());
            } catch (NumberFormatException e) {
                continue; // skip invalid rows
            }

            String unit = unitObj != null
                ? unitObj.toString() : Helper.UNITS[0];

            list.add(new RecipeIngredient(
                recipeId, ingredientId, qty, unit, ingredientName));
        }

        return list;
    }

    private Ingredient findIngredientById(int id) {
        for (Ingredient i : allIngredients) {
            if (i.getIngredientId() == id) return i;
        }
        return allIngredients.isEmpty() ? null : allIngredients.get(0);
    }

    private Ingredient findIngredientByName(String name) {
        for (Ingredient i : allIngredients) {
            if (i.getName().equalsIgnoreCase(name.trim())) return i;
        }
        return null;
    }
    
    /** Returns true if the user saved — RecipeListPanel uses this to refresh. */
    public boolean isSaved() { return saved; }
}
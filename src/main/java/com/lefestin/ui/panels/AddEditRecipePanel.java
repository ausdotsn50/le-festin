package com.lefestin.ui.panels;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;

import com.lefestin.dao.IngredientDAO;
import com.lefestin.dao.RecipeDAO;
import com.lefestin.dao.RecipeIngredientDAO;
import com.lefestin.helper.Helper;
import com.lefestin.model.Ingredient;
import com.lefestin.model.Recipe;
import com.lefestin.model.RecipeIngredient;
import com.lefestin.ui.AppTheme;
import com.lefestin.ui.MainFrame;

import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * AddEditRecipePanel — Panel form for creating and editing recipes.
 */
public class AddEditRecipePanel extends JPanel {
    private final MainFrame frame;
    private final Recipe existingRecipe; // null = add mode
    private final RecipeDAO recipeDAO;
    private final RecipeIngredientDAO riDAO;
    private final IngredientDAO ingredientDAO;

    // Form fields
    private JTextField titleField;
    private JSpinner prepTimeSpinner;
    private JSpinner servingsSpinner;
    private JTextArea procedureArea;

    // Ingredients
    private JPanel ingredientsContainer;
    private List<IngredientRowPanel> ingredientRows = new ArrayList<>();
    private List<Ingredient> allIngredients = new ArrayList<>();

    public AddEditRecipePanel(MainFrame frame, Recipe recipe) {
        this.frame = frame;
        this.existingRecipe = recipe;
        this.recipeDAO = new RecipeDAO();
        this.riDAO = new RecipeIngredientDAO();
        this.ingredientDAO = new IngredientDAO();

        allIngredients = Helper.loadAllIngredients(frame, ingredientDAO);
        initComponents();
        prefillIfEditing();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(AppTheme.BG_PAGE);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(AppTheme.BG_PAGE);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        contentPanel.add(buildTitleCard());
        contentPanel.add(Box.createVerticalStrut(15));
        contentPanel.add(Box.createVerticalStrut(15));
        contentPanel.add(buildBasicInfoCard());
        contentPanel.add(Box.createVerticalStrut(15));
        contentPanel.add(buildIngredientsCard());
        contentPanel.add(Box.createVerticalStrut(15));
        contentPanel.add(buildStepsCard());

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        add(buildButtonBar(), BorderLayout.SOUTH);
    }

    private JPanel buildTitleCard() {
        JPanel card = createCardPanel("Recipe title");
        titleField = createStyledTextField("Enter recipe name");
        titleField.setAlignmentX(Component.LEFT_ALIGNMENT);
         
        card.add(titleField);
        return card;
    }

    private JPanel buildBasicInfoCard() {
        JPanel card = createCardPanel("Basic Information");

        JPanel row1 = new JPanel(new BorderLayout());
        row1.setBackground(AppTheme.BG_SURFACE);
        row1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        // Force row 1 to align left
        row1.setAlignmentX(Component.LEFT_ALIGNMENT); 
        
        JLabel timeLabel = new JLabel("🕒 Total time");
        timeLabel.setForeground(AppTheme.TEXT_PRIMARY);
        timeLabel.setFont(AppTheme.FONT_BODY);
        
        prepTimeSpinner = new JSpinner(new SpinnerNumberModel(30, 1, 300, 5));
        styleSpinner(prepTimeSpinner);

        row1.add(timeLabel, BorderLayout.WEST);
        row1.add(prepTimeSpinner, BorderLayout.EAST);

        JPanel row2 = new JPanel(new BorderLayout());
        row2.setBackground(AppTheme.BG_SURFACE);
        row2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        row2.setAlignmentX(Component.LEFT_ALIGNMENT); 
        
        JLabel servingsLabel = new JLabel("👥 Servings");
        servingsLabel.setForeground(AppTheme.TEXT_PRIMARY);
        servingsLabel.setFont(AppTheme.FONT_BODY);

        servingsSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 50, 1));
        styleSpinner(servingsSpinner);

        row2.add(servingsLabel, BorderLayout.WEST);
        row2.add(servingsSpinner, BorderLayout.EAST);

        card.add(row1);
        card.add(Box.createVerticalStrut(10));
        card.add(row2);

        return card;
    }

    private JPanel buildIngredientsCard() {
        JPanel card = createCardPanel("Ingredients");

        ingredientsContainer = new JPanel();
        ingredientsContainer.setLayout(new BoxLayout(ingredientsContainer, BoxLayout.Y_AXIS));
        ingredientsContainer.setBackground(AppTheme.BG_SURFACE);

        JButton addBtn = new JButton("+ Add ingredient");
        addBtn.setForeground(new Color(255, 152, 0)); 
        addBtn.setContentAreaFilled(false);
        addBtn.setBorderPainted(false);
        addBtn.setFocusPainted(false);
        addBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        addBtn.addActionListener(e -> {
            addIngredientRow("", 1.0, Helper.UNITS[0]);
            revalidate();
            repaint();
        });

        if (existingRecipe == null) {
            addIngredientRow("", 1.0, Helper.UNITS[0]);
        }

        card.add(ingredientsContainer);
        card.add(Box.createVerticalStrut(10));
        card.add(addBtn);

        return card;
    }

    private JPanel buildStepsCard() {
        JPanel card = createCardPanel("Steps to follow");

        procedureArea = new JTextArea(8, 20);
        procedureArea.setFont(AppTheme.FONT_BODY);
        procedureArea.setBackground(AppTheme.BG_PAGE);
        procedureArea.setForeground(AppTheme.TEXT_PRIMARY);
        procedureArea.setLineWrap(true);
        procedureArea.setWrapStyleWord(true);
        procedureArea.setAlignmentX(Component.LEFT_ALIGNMENT);

        procedureArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppTheme.BG_BORDER, 1, true),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        card.add(procedureArea);
        return card;
    }

    private JPanel buildButtonBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        bar.setBackground(AppTheme.BG_SURFACE);
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, AppTheme.BG_BORDER));

        JButton cancelBtn = AppTheme.secondaryButton("Cancel");
        JButton saveBtn = AppTheme.primaryButton(existingRecipe == null ? "Save Recipe" : "Update Recipe");

        cancelBtn.addActionListener(e -> {
            // Tell MainFrame to show the list card
            frame.showRecipeList();
        });
        saveBtn.addActionListener(e -> saveRecipe());

        bar.add(cancelBtn);
        bar.add(saveBtn);

        return bar;
    }

    private JPanel createCardPanel(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(AppTheme.BG_SURFACE);
        
        Border line = BorderFactory.createLineBorder(AppTheme.BG_BORDER, 1, true);
        Border empty = BorderFactory.createEmptyBorder(15, 20, 20, 20);
        panel.setBorder(new CompoundBorder(line, empty));
        panel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(AppTheme.FONT_LABEL);
        titleLabel.setForeground(AppTheme.TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(15));

        return panel;
    }

    private JTextField createStyledTextField(String placeholder) {
        JTextField field = new JTextField();
        field.setFont(AppTheme.FONT_BODY);
        field.setBackground(AppTheme.BG_PAGE);
        field.setForeground(AppTheme.TEXT_PRIMARY);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        field.setPreferredSize(new Dimension(0, 40));
        
        Border line = BorderFactory.createLineBorder(AppTheme.BG_BORDER, 1, true);
        Border pad = BorderFactory.createEmptyBorder(5, 10, 5, 10);
        field.setBorder(new CompoundBorder(line, pad));
        return field;
    }

    private void styleSpinner(JSpinner spinner) {
        spinner.setFont(AppTheme.FONT_BODY);
        spinner.setPreferredSize(new Dimension(100, 35));
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField tf = ((JSpinner.DefaultEditor) editor).getTextField();
            tf.setBackground(AppTheme.BG_PAGE);
            tf.setForeground(AppTheme.TEXT_PRIMARY);
            tf.setBorder(BorderFactory.createLineBorder(AppTheme.BG_BORDER, 1, true));
        }
    }

    private void addIngredientRow(String ingredientName, double quantity, String unit) {
        IngredientRowPanel row = new IngredientRowPanel(ingredientName, quantity, unit);
        ingredientRows.add(row);
        ingredientsContainer.add(row);
    }

    private void removeIngredientRow(IngredientRowPanel row) {
        ingredientRows.remove(row);
        ingredientsContainer.remove(row);
        ingredientsContainer.revalidate();
        ingredientsContainer.repaint();
    }

    private class IngredientRowPanel extends JPanel {
        JTextField qtyField;
        JComboBox<String> unitCombo;
        JTextField ingredientNameField;

        public IngredientRowPanel(String initIngredientName, double initQty, String initUnit) {
            setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));
            setBackground(AppTheme.BG_SURFACE);
            setAlignmentX(Component.LEFT_ALIGNMENT);

            qtyField = new JTextField(String.valueOf(initQty), 4);
            qtyField.setFont(AppTheme.FONT_BODY);
            qtyField.setBackground(AppTheme.BG_PAGE);
            qtyField.setForeground(AppTheme.TEXT_PRIMARY);
            qtyField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BG_BORDER, 1, true),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
            ));

            unitCombo = new JComboBox<>(Helper.UNITS);
            unitCombo.setSelectedItem(initUnit);
            unitCombo.setFont(AppTheme.FONT_BODY);
            unitCombo.setBackground(AppTheme.BG_PAGE);
            unitCombo.setForeground(AppTheme.TEXT_PRIMARY);

            ingredientNameField = new JTextField(initIngredientName, 12);
            ingredientNameField.setFont(AppTheme.FONT_BODY);
            ingredientNameField.setBackground(AppTheme.BG_PAGE);
            ingredientNameField.setForeground(AppTheme.TEXT_PRIMARY);
            ingredientNameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BG_BORDER, 1, true),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
            ));

            JButton removeBtn = new JButton("✖");
            removeBtn.setForeground(AppTheme.TEXT_MUTED);
            removeBtn.setContentAreaFilled(false);
            removeBtn.setBorderPainted(false);
            removeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            removeBtn.addActionListener(e -> removeIngredientRow(this));

            add(qtyField);
            add(unitCombo);
            add(ingredientNameField);
            add(removeBtn);
        }

        public double getQuantity() throws NumberFormatException {
            return Double.parseDouble(qtyField.getText().trim());
        }

        public String getUnit() {
            return (String) unitCombo.getSelectedItem();
        }

        public String getIngredientName() {
            return ingredientNameField.getText().trim();
        }
    }

    private void prefillIfEditing() {
        if (existingRecipe == null) return;

        titleField.setText(existingRecipe.getTitle());
        prepTimeSpinner.setValue(existingRecipe.getPrepTime());
        procedureArea.setText(existingRecipe.getProcedure());
        procedureArea.setCaretPosition(0);

        try {
            List<RecipeIngredient> existing = riDAO.getIngredientsByRecipeId(existingRecipe.getRecipeId());
            
            ingredientsContainer.removeAll();
            ingredientRows.clear();

            for (RecipeIngredient ri : existing) {
                String name = ri.getIngredientName();
                if (name == null || name.trim().isEmpty()) {
                    Ingredient match = findIngredientById(ri.getIngredientId());
                    name = (match != null) ? match.getName() : "";
                }
                addIngredientRow(name, ri.getQuantity(), ri.getUnit());
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Could not load existing ingredients.", "Warning", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void saveRecipe() {
        String validationError = validateForm();
        if (validationError != null) {
            JOptionPane.showMessageDialog(this, validationError, "Missing Information", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            String title = titleField.getText().trim();
            int prepTime = (int) prepTimeSpinner.getValue();
            String procedure = procedureArea.getText().trim();

            Recipe recipe;

            if (existingRecipe == null) {
                recipe = new Recipe(frame.getCurrentUserId(), title, Recipe.CATEGORY_DINNER, prepTime, procedure);
                recipeDAO.addRecipe(recipe);
            } else {
                recipe = existingRecipe;
                recipe.setTitle(title);
                recipe.setPrepTime(prepTime);
                recipe.setProcedure(procedure);
                recipeDAO.updateRecipe(recipe);
            }

            riDAO.deleteByRecipeId(recipe.getRecipeId());
            List<RecipeIngredient> ingredients = buildIngredientList(recipe.getRecipeId());
            riDAO.addAll(ingredients);

            // Tell MainFrame to show the list card (which also refreshes the data)
            frame.showRecipeList();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to save recipe: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String validateForm() {
        if (titleField.getText().trim().isEmpty()) return "Recipe title is required.";
        if (procedureArea.getText().trim().isEmpty()) return "Procedure instructions are required.";
        if (ingredientRows.isEmpty()) return "Add at least one ingredient.";

        for (int i = 0; i < ingredientRows.size(); i++) {
            if (ingredientRows.get(i).getIngredientName().isEmpty()) {
                return "Ingredient name cannot be empty on row " + (i + 1) + ".";
            }
            try {
                double qty = ingredientRows.get(i).getQuantity();
                if (qty <= 0) return "Quantity on row " + (i + 1) + " must be greater than 0.";
            } catch (NumberFormatException e) {
                return "Invalid quantity on row " + (i + 1) + ". Enter a number.";
            }
        }
        return null;
    }

    private List<RecipeIngredient> buildIngredientList(int recipeId) throws SQLException {
        List<RecipeIngredient> list = new ArrayList<>();
        
        for (IngredientRowPanel row : ingredientRows) {
            String ingName = row.getIngredientName();
            if (ingName.isEmpty()) continue;

            Ingredient ingredient = ingredientDAO.findOrCreate(ingName);
            int ingId = ingredient.getIngredientId();

            if (findIngredientById(ingId) == null) {
                allIngredients.add(ingredient);
            }
            
            list.add(new RecipeIngredient(
                recipeId, 
                ingId, 
                row.getQuantity(), 
                row.getUnit(), 
                ingName
            ));
        }
        return list;
    }

    private Ingredient findIngredientById(int id) {
        for (Ingredient i : allIngredients) {
            if (i.getIngredientId() == id) return i;
        }
        return null;
    }
}
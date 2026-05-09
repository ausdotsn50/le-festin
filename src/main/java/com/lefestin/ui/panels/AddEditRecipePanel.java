package com.lefestin.ui.panels;

import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;

import com.lefestin.dao.*;
import com.lefestin.helper.Helper;
import com.lefestin.model.*;
import com.lefestin.ui.AppTheme;
import com.lefestin.ui.MainFrame;

public class AddEditRecipePanel extends JPanel {
    private final MainFrame frame;
    private final Recipe existingRecipe; 
    private final RecipeDAO recipeDAO = new RecipeDAO();
    private final RecipeIngredientDAO riDAO = new RecipeIngredientDAO();
    private final IngredientDAO ingredientDAO = new IngredientDAO();

    private JTextField titleField;
    private JSpinner prepTimeSpinner;
    private JComboBox<String> categoryCombo;
    private JTextArea procedureArea;
    private JPanel ingredientsContainer;

    private final List<IngredientRowPanel> ingredientRows = new ArrayList<>();
    private List<Ingredient> allIngredients = new ArrayList<>();

    public AddEditRecipePanel(MainFrame frame, Recipe recipe) {
        this.frame = frame;
        this.existingRecipe = recipe;
        this.allIngredients = Helper.loadAllIngredients(frame, ingredientDAO);
        
        setupLayout();
        prefillIfEditing();
    }

    private void setupLayout() {
        setLayout(new BorderLayout());
        setBackground(AppTheme.BG_PAGE);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(AppTheme.BG_PAGE);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        // Assemble sections
        contentPanel.add(buildTitleCard());
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

    // --- Card Builders ---

    private JPanel buildTitleCard() {
        JPanel card = createCardPanel("Recipe title");
        titleField = createStyledTextField();
        card.add(titleField);
        return card;
    }

    private JPanel buildBasicInfoCard() {
        JPanel card = createCardPanel("Basic Information");

        // Prep Time Row
        prepTimeSpinner = new JSpinner(new SpinnerNumberModel(30, 1, 300, 5));
        styleSpinner(prepTimeSpinner);
        card.add(createLabeledRow("🕒 Total time", prepTimeSpinner));
        
        card.add(Box.createVerticalStrut(10));

        // Category Row
        categoryCombo = new JComboBox<>(new String[]{
            Recipe.CATEGORY_BREAKFAST, Recipe.CATEGORY_LUNCH, Recipe.CATEGORY_DINNER
        });
        categoryCombo.setFont(AppTheme.FONT_BODY);
        categoryCombo.setPreferredSize(new Dimension(160, 35));
        card.add(createLabeledRow("📦 Category", categoryCombo));

        return card;
    }

    private JPanel buildIngredientsCard() {
        JPanel card = createCardPanel("Ingredients");

        ingredientsContainer = new JPanel();
        ingredientsContainer.setLayout(new BoxLayout(ingredientsContainer, BoxLayout.Y_AXIS));
        ingredientsContainer.setBackground(AppTheme.BG_SURFACE);

        JButton addBtn = createTransparentButton("+ Add ingredient", new Color(255, 152, 0));
        addBtn.addActionListener(e -> {
            addIngredientRow("", 1.0, Helper.UNITS[0]);
            refreshUI();
        });

        if (existingRecipe == null) addIngredientRow("", 1.0, Helper.UNITS[0]);

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
        procedureArea.setLineWrap(true);
        procedureArea.setWrapStyleWord(true);
        procedureArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane stepsScroll = new JScrollPane(procedureArea);
        stepsScroll.setBorder(BorderFactory.createLineBorder(AppTheme.BG_BORDER, 1, true));
        stepsScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));
        stepsScroll.setAlignmentX(LEFT_ALIGNMENT); 

        card.add(stepsScroll);  
        return card;
    }

    private JPanel buildButtonBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        bar.setBackground(AppTheme.BG_SURFACE);
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, AppTheme.BG_BORDER));

        JButton cancelBtn = AppTheme.secondaryButton("Cancel");
        JButton saveBtn = AppTheme.primaryButton(existingRecipe == null ? "Save Recipe" : "Update Recipe");

        cancelBtn.addActionListener(e -> frame.showRecipeList());
        saveBtn.addActionListener(e -> saveRecipe());

        bar.add(cancelBtn);
        bar.add(saveBtn);
        return bar;
    }

    // --- Helper UI Factory Methods ---

    private JPanel createCardPanel(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(AppTheme.BG_SURFACE);
        panel.setAlignmentX(LEFT_ALIGNMENT);
        panel.setBorder(new CompoundBorder(
            BorderFactory.createLineBorder(AppTheme.BG_BORDER, 1, true),
            BorderFactory.createEmptyBorder(15, 20, 20, 20)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(AppTheme.FONT_LABEL);
        titleLabel.setForeground(AppTheme.TEXT_PRIMARY);
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);
        
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(15));
        return panel;
    }

    private JPanel createLabeledRow(String labelText, JComponent component) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(AppTheme.BG_SURFACE);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        row.setAlignmentX(LEFT_ALIGNMENT);

        JLabel label = new JLabel(labelText);
        label.setFont(AppTheme.FONT_BODY);
        label.setForeground(AppTheme.TEXT_PRIMARY);

        row.add(label, BorderLayout.WEST);
        row.add(component, BorderLayout.EAST);
        return row;
    }

    private JTextField createStyledTextField() {
        JTextField field = new JTextField();
        field.setFont(AppTheme.FONT_BODY);
        field.setBackground(AppTheme.BG_PAGE);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        field.setAlignmentX(LEFT_ALIGNMENT);
        
        field.setBorder(new CompoundBorder(
            BorderFactory.createLineBorder(AppTheme.BG_BORDER, 1, true),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        return field;
    }

    private JButton createTransparentButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setForeground(color);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setAlignmentX(LEFT_ALIGNMENT);
        return btn;
    }

    private void styleSpinner(JSpinner spinner) {
        spinner.setFont(AppTheme.FONT_BODY);
        spinner.setPreferredSize(new Dimension(100, 35));
        if (spinner.getEditor() instanceof JSpinner.DefaultEditor editor) {
            JTextField tf = editor.getTextField();
            tf.setBackground(AppTheme.BG_PAGE);
            tf.setBorder(BorderFactory.createLineBorder(AppTheme.BG_BORDER, 1, true));
        }
    }

    private void addIngredientRow(String name, double qty, String unit) {
        IngredientRowPanel row = new IngredientRowPanel(name, qty, unit);
        ingredientRows.add(row);
        ingredientsContainer.add(row);
    }

    private void refreshUI() {
        revalidate();
        repaint();
    }

    // --- Logic & Data Handling ---

    private void prefillIfEditing() {
        if (existingRecipe == null) return;

        titleField.setText(existingRecipe.getTitle());
        prepTimeSpinner.setValue(existingRecipe.getPrepTime());
        categoryCombo.setSelectedItem(existingRecipe.getCategory());
        procedureArea.setText(existingRecipe.getProcedure());
        procedureArea.setCaretPosition(0);

        try {
            List<RecipeIngredient> existing = riDAO.getIngredientsByRecipeId(existingRecipe.getRecipeId());
            ingredientsContainer.removeAll();
            ingredientRows.clear();

            for (RecipeIngredient ri : existing) {
                String name = ri.getIngredientName();
                if (name == null || name.isBlank()) {
                    Ingredient match = findIngredientById(ri.getIngredientId());
                    name = (match != null) ? match.getName() : "";
                }
                addIngredientRow(name, ri.getQuantity(), ri.getUnit());
            }
        } catch (SQLException e) {
            showError("Could not load existing ingredients.");
        }
    }

    private void saveRecipe() {
        String error = validateForm();
        if (error != null) {
            JOptionPane.showMessageDialog(this, error, "Missing Information", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Recipe recipe = (existingRecipe == null) 
                ? new Recipe(frame.getCurrentUserId(), titleField.getText().trim(), (String) categoryCombo.getSelectedItem(), (int) prepTimeSpinner.getValue(), procedureArea.getText().trim())
                : existingRecipe;

            if (existingRecipe != null) {
                recipe.setTitle(titleField.getText().trim());
                recipe.setCategory((String) categoryCombo.getSelectedItem());
                recipe.setPrepTime((int) prepTimeSpinner.getValue());
                recipe.setProcedure(procedureArea.getText().trim());
                recipeDAO.updateRecipe(recipe);
            } else {
                recipeDAO.addRecipe(recipe);
            }

            riDAO.deleteByRecipeId(recipe.getRecipeId());
            riDAO.addAll(buildIngredientList(recipe.getRecipeId()));
            frame.showRecipeList();

        } catch (SQLException e) {
            showError("Failed to save recipe: " + e.getMessage());
        }
    }

    private String validateForm() {
        if (titleField.getText().trim().isEmpty()) return "Recipe title is required.";
        if (procedureArea.getText().trim().isEmpty()) return "Procedure instructions are required.";
        if (ingredientRows.isEmpty()) return "Add at least one ingredient.";

        for (int i = 0; i < ingredientRows.size(); i++) {
            IngredientRowPanel row = ingredientRows.get(i);
            if (row.getIngredientName().isEmpty()) return "Ingredient name empty on row " + (i + 1);
            try {
                if (row.getQuantity() <= 0) return "Invalid quantity of ingredient";
            } catch (NumberFormatException e) {
                return "Invalid quantity on row " + (i + 1);
            }
        }
        return null;
    }

    private List<RecipeIngredient> buildIngredientList(int recipeId) throws SQLException {
        List<RecipeIngredient> list = new ArrayList<>();
        for (IngredientRowPanel row : ingredientRows) {
            String name = row.getIngredientName();
            Ingredient ing = ingredientDAO.findOrCreate(name);
            if (findIngredientById(ing.getIngredientId()) == null) allIngredients.add(ing);
            
            list.add(new RecipeIngredient(recipeId, ing.getIngredientId(), row.getQuantity(), row.getUnit(), name));
        }
        return list;
    }

    private Ingredient findIngredientById(int id) {
        return allIngredients.stream().filter(i -> i.getIngredientId() == id).findFirst().orElse(null);
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // --- Inner Components ---

    private class IngredientRowPanel extends JPanel {
        private final JTextField qtyField, nameField;
        private final JComboBox<String> unitCombo;

        public IngredientRowPanel(String name, double qty, String unit) {
            setLayout(new GridBagLayout());
            setBackground(AppTheme.BG_SURFACE);
            setAlignmentX(LEFT_ALIGNMENT);

            qtyField = createRowTextField(String.valueOf(qty), 4);
            nameField = createRowTextField(name, 12);
            unitCombo = new JComboBox<>(Helper.UNITS);
            unitCombo.setSelectedItem(unit);
            unitCombo.setFont(AppTheme.FONT_BODY);

            JButton removeBtn = createTransparentButton("✖", AppTheme.TEXT_MUTED);
            removeBtn.addActionListener(e -> {
                ingredientRows.remove(this);
                ingredientsContainer.remove(this);
                refreshUI();
            });

            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(5, 5, 5, 5);
            
            add(qtyField, c);
            c.gridx = 1; add(unitCombo, c);
            c.gridx = 2; c.weightx = 1.0; c.fill = GridBagConstraints.HORIZONTAL; add(nameField, c);
            c.gridx = 3; c.weightx = 0; c.fill = GridBagConstraints.NONE; add(removeBtn, c);
        }

        private JTextField createRowTextField(String text, int cols) {
            JTextField f = new JTextField(text, cols);
            f.setFont(AppTheme.FONT_BODY);
            f.setBorder(new CompoundBorder(BorderFactory.createLineBorder(AppTheme.BG_BORDER, 1, true), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
            return f;
        }

        public double getQuantity() { return Double.parseDouble(qtyField.getText().trim()); }
        public String getUnit() { return (String) unitCombo.getSelectedItem(); }
        public String getIngredientName() { return nameField.getText().trim(); }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }
    }
}
package com.lefestin.ui.panels;

import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;

import com.lefestin.dao.RecipeDAO;
import com.lefestin.dao.RecipeIngredientDAO;
import com.lefestin.helper.Helper;
import com.lefestin.model.Recipe;
import com.lefestin.model.RecipeIngredient;
import com.lefestin.ui.AppTheme;
import com.lefestin.ui.MainFrame;

/**
 * RecipeDetailPanel — read-only view for a single recipe.
 */
public class RecipeDetailPanel extends JPanel {
    private final MainFrame frame;
    private final RecipeDAO recipeDAO = new RecipeDAO();
    private final RecipeIngredientDAO riDAO = new RecipeIngredientDAO();
    private final Recipe recipe;

    private JPanel ingredientsList;
    private JTextArea procedureArea;
    private JLabel categoryBadge;
    private JLabel prepTimeBadge;

    public RecipeDetailPanel(MainFrame frame, Recipe recipe) {
        this.frame = frame;
        this.recipe = recipe;

        setupLayout();
        initComponents();
        loadRecipeDetails();
    }

    private void setupLayout() {
        setLayout(new BorderLayout());
        setBackground(AppTheme.BG_PAGE);
        setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
    }

    private void initComponents() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(AppTheme.BG_PAGE);

        content.add(buildHeaderCard());
        content.add(Box.createVerticalStrut(15));
        content.add(buildIngredientsCard());
        content.add(Box.createVerticalStrut(15));
        content.add(buildStepsCard());

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(AppTheme.BG_PAGE);
        wrapper.add(content, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(wrapper);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(AppTheme.BG_PAGE);

        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel buildHeaderCard() {
        JPanel card = createCardPanel(12, 14);

        JPanel topRow = new JPanel(new BorderLayout(12, 0));
        topRow.setOpaque(false);
        topRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel leftSide = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftSide.setOpaque(false);
        leftSide.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel(recipe.getTitle());
        title.setFont(new Font("Serif", Font.BOLD, 24));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        prepTimeBadge = buildMetaBadge(recipe.getFormattedPrepTime());
        categoryBadge = buildMetaBadge(recipe.getCategory() != null ? recipe.getCategory() : "");

        leftSide.add(title);
        leftSide.add(prepTimeBadge);
        leftSide.add(categoryBadge);

        JButton backBtn = AppTheme.secondaryButton("Back");
        backBtn.addActionListener(e -> frame.showRecipeList());
        backBtn.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actions.setOpaque(false);
        actions.add(backBtn);

        topRow.add(leftSide, BorderLayout.WEST);
        topRow.add(actions, BorderLayout.EAST);

        card.add(topRow);
        return card;
    }

    private JPanel buildIngredientsCard() {
        JPanel card = createCardPanel(18, 20);

        JLabel heading = AppTheme.headingLabel("Ingredients");
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        ingredientsList = new JPanel();
        ingredientsList.setLayout(new BoxLayout(ingredientsList, BoxLayout.Y_AXIS));
        ingredientsList.setOpaque(false);
        ingredientsList.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(heading);
        card.add(Box.createVerticalStrut(12));
        card.add(ingredientsList);
        return card;
    }

    private JPanel buildStepsCard() {
        JPanel card = createCardPanel(18, 20);

        JLabel heading = AppTheme.headingLabel("Steps");
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);

        procedureArea = new JTextArea();
        procedureArea.setEditable(false);
        procedureArea.setFocusable(false);
        procedureArea.setLineWrap(true);
        procedureArea.setWrapStyleWord(true);
        procedureArea.setFont(AppTheme.FONT_BODY);
        procedureArea.setForeground(AppTheme.TEXT_PRIMARY);
        procedureArea.setBackground(AppTheme.BG_PAGE);
        procedureArea.setBorder(null);

        card.add(heading);
        card.add(Box.createVerticalStrut(12));
        card.add(procedureArea);
        return card;
    }

    private void loadRecipeDetails() {
        try {
            Recipe fullRecipe = recipeDAO.getRecipeById(recipe.getRecipeId());
            Recipe r = (fullRecipe != null) ? fullRecipe : recipe;

            prepTimeBadge.setText(r.getFormattedPrepTime());
            categoryBadge.setText(r.getCategory() != null ? r.getCategory() : "");
            procedureArea.setText(r.getProcedure());
            procedureArea.setCaretPosition(0);

            List<RecipeIngredient> ingredients = riDAO.getIngredientsByRecipeId(r.getRecipeId());
            ingredientsList.removeAll();

            if (ingredients.isEmpty()) {
                addIngredientLabel("No ingredients found.", AppTheme.TEXT_MUTED);
            } else {
                for (RecipeIngredient ri : ingredients) {
                    String text = "• " + Helper.formatQty(ri.getQuantity()) + " " + ri.getUnit() + " " + Helper.capitalize(ri.getIngredientName());
                    addIngredientLabel(text, AppTheme.TEXT_PRIMARY);
                    ingredientsList.add(Box.createVerticalStrut(8));
                }
            }
            ingredientsList.revalidate();
            ingredientsList.repaint();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Could not load recipe details: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addIngredientLabel(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(AppTheme.FONT_BODY);
        label.setForeground(color);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        ingredientsList.add(label);
    }

    private JPanel createCardPanel(int vPad, int hPad) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(AppTheme.BG_SURFACE);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        Border line = BorderFactory.createLineBorder(AppTheme.BG_BORDER, 1, true);
        Border empty = BorderFactory.createEmptyBorder(vPad, hPad, vPad, hPad);
        panel.setBorder(new CompoundBorder(line, empty));
        return panel;
    }

    private JLabel buildMetaBadge(String text) {
        JLabel label = new JLabel(text);
        label.setFont(AppTheme.FONT_SMALL);
        label.setForeground(AppTheme.TEXT_PRIMARY);
        label.setOpaque(true);
        label.setBackground(AppTheme.BG_PAGE);
        label.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppTheme.BG_BORDER, 1, true),
            BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        return label;
    }
}
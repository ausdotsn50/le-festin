package com.lefestin.ui.panels;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.sql.SQLException;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;

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
    private final RecipeDAO recipeDAO;
    private final RecipeIngredientDAO riDAO;
    private final Recipe recipe;

    private JPanel ingredientsList;
    private JTextArea procedureArea;

    public RecipeDetailPanel(MainFrame frame, Recipe recipe) {
        this.frame = frame;
        this.recipeDAO = new RecipeDAO();
        this.riDAO = new RecipeIngredientDAO();
        this.recipe = recipe;

        initComponents();
        loadRecipeDetails();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(AppTheme.BG_PAGE);
        setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

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

        leftSide.add(title);
        leftSide.add(buildMetaBadge(recipe.getFormattedPrepTime()));

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
        JPanel card = createCardPanel();

        JLabel heading = AppTheme.headingLabel("Ingredients");
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(heading);
        card.add(Box.createVerticalStrut(12));

        ingredientsList = new JPanel();
        ingredientsList.setLayout(new BoxLayout(ingredientsList, BoxLayout.Y_AXIS));
        ingredientsList.setOpaque(false);
        ingredientsList.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(ingredientsList);
        return card;
    }

    private JPanel buildStepsCard() {
        JPanel card = createCardPanel();

        JLabel heading = AppTheme.headingLabel("Steps");
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(heading);
        card.add(Box.createVerticalStrut(12));

        procedureArea = new JTextArea();
        procedureArea.setEditable(false);
        procedureArea.setFocusable(false);
        procedureArea.setLineWrap(true);
        procedureArea.setWrapStyleWord(true);
        procedureArea.setFont(AppTheme.FONT_BODY);
        procedureArea.setForeground(AppTheme.TEXT_PRIMARY);
        procedureArea.setBackground(AppTheme.BG_PAGE);
        procedureArea.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        JScrollPane stepsScroll = new JScrollPane(procedureArea);
        stepsScroll.setBorder(BorderFactory.createLineBorder(AppTheme.BG_BORDER, 1, true));
        stepsScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));
        stepsScroll.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(procedureArea);
        return card;
    }

    private JPanel createCardPanel() {
        return createCardPanel(18, 20);
    }

    private JPanel createCardPanel(int verticalPadding, int horizontalPadding) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(AppTheme.BG_SURFACE);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        Border line = BorderFactory.createLineBorder(AppTheme.BG_BORDER, 1, true);
        Border empty = BorderFactory.createEmptyBorder(verticalPadding, horizontalPadding, verticalPadding, horizontalPadding);
        panel.setBorder(new CompoundBorder(line, empty));
        return panel;
    }

    private JLabel buildMetaBadge(String text) {
        JLabel label = new JLabel(text);
        label.setFont(AppTheme.FONT_SMALL);
        label.setForeground(AppTheme.TEXT_PRIMARY);
        label.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppTheme.BG_BORDER, 1, true),
            BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        label.setOpaque(true);
        label.setBackground(AppTheme.BG_PAGE);
        return label;
    }

    private void loadRecipeDetails() {
        try {
            Recipe fullRecipe = recipeDAO.getRecipeById(recipe.getRecipeId());
            Recipe recipeToShow = fullRecipe != null ? fullRecipe : recipe;

            procedureArea.setText(recipeToShow.getProcedure());
            procedureArea.setCaretPosition(0);

            List<RecipeIngredient> ingredients =
                riDAO.getIngredientsByRecipeId(recipeToShow.getRecipeId());

            ingredientsList.removeAll();
            if (ingredients.isEmpty()) {
                JLabel empty = new JLabel("No ingredients found.");
                empty.setFont(AppTheme.FONT_BODY);
                empty.setForeground(AppTheme.TEXT_MUTED);
                empty.setAlignmentX(Component.LEFT_ALIGNMENT);
                ingredientsList.add(empty);
            } else {
                for (RecipeIngredient ingredient : ingredients) {
                    JLabel item = new JLabel("• " + Helper.formatQty(ingredient.getQuantity())
                        + " " + ingredient.getUnit() + " "
                        + Helper.capitalize(ingredient.getIngredientName()));
                    item.setFont(AppTheme.FONT_BODY);
                    item.setForeground(AppTheme.TEXT_PRIMARY);
                    item.setAlignmentX(Component.LEFT_ALIGNMENT);
                    ingredientsList.add(item);
                    ingredientsList.add(Box.createVerticalStrut(8));
                }
            }

            ingredientsList.revalidate();
            ingredientsList.repaint();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Could not load recipe details: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
}
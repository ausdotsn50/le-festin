package com.lefestin.ui.panels;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.lefestin.dao.impl.RecipeDAOImpl;
import com.lefestin.model.Recipe;
import com.lefestin.ui.AppTheme;
import com.lefestin.ui.MainFrame;

/**
 * RecipeListPanel — Card-based recipe browser extending BaseListPanel.
 */
public class RecipeListPanel extends BaseListPanel {
    private final RecipeDAOImpl recipeDAO;

    private List<Recipe> allRecipes = new ArrayList<>();
    private JPanel cardsContainer;

    public RecipeListPanel(MainFrame frame) {
        super(frame);
        this.recipeDAO = new RecipeDAOImpl();
    }

    @Override
    protected String getHeaderTitle() { return "My Recipes"; }
    @Override
    protected String getHeaderDescription() { return "Your saved recipes"; }
    @Override
    protected String getSearchPlaceholder() { return "Search recipes..."; }
    @Override
    protected JComponent buildHeaderRightControl() { return Box.createHorizontalBox(); }

    @Override
    protected JComponent buildSearchRightControl() {
        JButton addBtn = AppTheme.primaryButton("Add Recipe");
        addBtn.addActionListener(e -> openAddEditPanel(null));
        return addBtn;
    }

    @Override
    protected JComponent buildTableContent() {
        cardsContainer = new JPanel();
        cardsContainer.setLayout(new BoxLayout(cardsContainer, BoxLayout.Y_AXIS));
        cardsContainer.setBackground(AppTheme.BG_PAGE);
        cardsContainer.setAlignmentX(Component.LEFT_ALIGNMENT);

        JScrollPane scrollPane = new JScrollPane(cardsContainer);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(AppTheme.BG_PAGE);

        return scrollPane;
    }

    @Override
    protected JPanel buildToolbar() {
        // RecipeListPanel doesn't use a toolbar — keep it simple
        JPanel empty = new JPanel();
        empty.setBackground(AppTheme.BG_PAGE);
        return empty;
    }

    @Override
    protected JButton createActionButton() {
        return AppTheme.dangerButton("Delete");
    }

    @Override
    protected void onAddClicked() {
        openAddEditPanel(null);
    }

    @Override
    protected void onEditClicked() { }
    @Override
    protected void onActionClicked() { }

    private void renderCards(List<Recipe> recipes) {
        cardsContainer.removeAll();
        for (int i = 0; i < recipes.size(); i++) {
            cardsContainer.add(createRecipeCard(recipes.get(i)));
            if (i < recipes.size() - 1) {
                cardsContainer.add(Box.createVerticalStrut(12));
            }
        }
        cardsContainer.revalidate();
        cardsContainer.repaint();
    }

 private JPanel createRecipeCard(Recipe recipe) {
    // Keep your height at 90
    JPanel card = new JPanel(new BorderLayout(15, 0));
    card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
    card.setPreferredSize(new Dimension(300, 90));
    card.setBackground(AppTheme.BG_SURFACE);
    card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

    // Borders (Keep your logic)
    final Border defaultBorder = new CompoundBorder(
            BorderFactory.createLineBorder(AppTheme.BG_BORDER, 1, true),
            BorderFactory.createEmptyBorder(10, 20, 10, 20) // Reduced top/bottom padding to 10
    );
    final Border hoverBorder = new CompoundBorder(
            BorderFactory.createLineBorder(AppTheme.AMBER_PRIMARY, 1, true),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)
    );
    card.setBorder(defaultBorder);

    JPanel textContainer = new JPanel();
    textContainer.setLayout(new BoxLayout(textContainer, BoxLayout.Y_AXIS));
    textContainer.setOpaque(false);

    JLabel titleLbl = new JLabel("<html>" + recipe.getTitle() + "</html>");
    titleLbl.setFont(AppTheme.FONT_CARD_TITLE);
    titleLbl.setForeground(AppTheme.TEXT_SECONDARY);

    JLabel timeLbl = new JLabel("🕒 " + recipe.getFormattedPrepTime());
    timeLbl.setFont(AppTheme.FONT_SMALL);
    timeLbl.setForeground(AppTheme.TEXT_MUTED);

    textContainer.add(Box.createVerticalGlue()); 
    textContainer.add(titleLbl);
    textContainer.add(Box.createVerticalStrut(5));
    textContainer.add(timeLbl);
    textContainer.add(Box.createVerticalGlue());

    JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
    rightActions.setOpaque(false);
    JPanel rightWrapper = new JPanel(new GridBagLayout()); 
    rightWrapper.setOpaque(false);

    JButton editBtn = createIconButton("✎", "Edit Recipe", e -> openAddEditPanel(recipe));
    JButton deleteBtn = createIconButton("✖", "Delete Recipe", e -> deleteRecipe(recipe));

    rightActions.add(editBtn);
    rightActions.add(deleteBtn);
    rightWrapper.add(rightActions);

    // Assemble
    card.add(textContainer, BorderLayout.CENTER);
    card.add(rightWrapper, BorderLayout.EAST);

    // Interaction (Keep your logic)
    card.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) { openDetailPanel(recipe); }
        @Override
        public void mouseEntered(MouseEvent e) { card.setBorder(hoverBorder); }
        @Override
        public void mouseExited(MouseEvent e) { card.setBorder(defaultBorder); }
    });

    return card;
}

    public void loadRecipes() {
        try {
            allRecipes = recipeDAO.getAllRecipes(frame.getCurrentUserId());
            
            // Setup search listener
            searchField.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) { filterCards(); }
                public void removeUpdate(DocumentEvent e) { filterCards(); }
                public void changedUpdate(DocumentEvent e) { filterCards(); }
            });
            
            filterCards();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load recipes: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void filterCards() {
        String query = searchField.getText().toLowerCase().trim();
        if (query.isEmpty()) {
            renderCards(allRecipes);
        } else {
            List<Recipe> filtered = allRecipes.stream()
                .filter(r -> r.getTitle().toLowerCase().contains(query) || 
                             r.getCategory().toLowerCase().contains(query))
                .collect(Collectors.toList());
            renderCards(filtered);
        }
    }

    private void deleteRecipe(Recipe recipe) {
        String msg = "Delete \"" + recipe.getTitle() + "\"? This cannot be undone.";
        int confirm = JOptionPane.showConfirmDialog(this, msg, "Confirm Delete", 
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                recipeDAO.deleteRecipe(recipe.getRecipeId());
                loadRecipes();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Failed to delete: " + ex.getMessage());
            }
        }
    }

    // Styling helpers
    private JButton createIconButton(String icon, String tooltip, java.awt.event.ActionListener action) {
        JButton btn = new JButton(icon);
        btn.setFont(AppTheme.FONT_SMALL);
        btn.setForeground(AppTheme.TEXT_MUTED);
        btn.setToolTipText(tooltip);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(action);
        return btn;
    }

    private void openAddEditPanel(Recipe recipe) {
        frame.showAddEditRecipePanel(recipe);
    }

    private void openDetailPanel(Recipe recipe) {
        frame.showRecipeDetailPanel(recipe);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) loadRecipes();
    }
}
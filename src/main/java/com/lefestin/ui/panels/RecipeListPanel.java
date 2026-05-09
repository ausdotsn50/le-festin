package com.lefestin.ui.panels;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.lefestin.dao.RecipeDAO;
import com.lefestin.model.Recipe;
import com.lefestin.ui.AppTheme;
import com.lefestin.ui.MainFrame;

/**
 * RecipeListPanel — Card-based recipe browser.
 */
public class RecipeListPanel extends JPanel {
    private final MainFrame frame;
    private final RecipeDAO recipeDAO;

    private List<Recipe> allRecipes = new ArrayList<>();
    private JPanel cardsContainer;
    private JTextField searchField;

    public RecipeListPanel(MainFrame frame) {
        this.frame = frame;
        this.recipeDAO = new RecipeDAO();

        setupMainPanel();
        initComponents();
    }

    private void setupMainPanel() {
        setLayout(new BorderLayout());
        setBackground(AppTheme.BG_PAGE);
        setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
    }

    private void initComponents() {
        add(buildHeader(), BorderLayout.NORTH);

        cardsContainer = new JPanel();
        cardsContainer.setLayout(new BoxLayout(cardsContainer, BoxLayout.Y_AXIS));
        cardsContainer.setBackground(AppTheme.BG_PAGE);
        cardsContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardsContainer.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));

        JScrollPane scrollPane = new JScrollPane(cardsContainer);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(AppTheme.BG_PAGE);

        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(AppTheme.BG_PAGE);
        headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setBackground(AppTheme.BG_PAGE);
        
        topRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        topRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        JLabel titleLabel = new JLabel("My Recipes");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
        titleLabel.setForeground(AppTheme.TEXT_PRIMARY);

        JButton addBtn = new JButton("+");
        styleCircleButton(addBtn, AppTheme.GREEN_PRIMARY, Color.WHITE);
        addBtn.addActionListener(e -> openAddEditPanel(null));

        topRow.add(titleLabel, BorderLayout.WEST);
        topRow.add(addBtn, BorderLayout.EAST);

        searchField = createSearchField();
        
        headerPanel.add(topRow);
        headerPanel.add(Box.createVerticalStrut(15));
        headerPanel.add(searchField);
        headerPanel.add(Box.createVerticalStrut(10));

        return headerPanel;
    }

    private JTextField createSearchField() {
        JTextField field = new JTextField();
        field.setFont(AppTheme.FONT_BODY);
        field.setBackground(AppTheme.BG_SURFACE);
        field.setForeground(AppTheme.TEXT_PRIMARY);
        field.setCaretColor(AppTheme.TEXT_PRIMARY);
        
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        field.setPreferredSize(new Dimension(Integer.MAX_VALUE, 45));
       
        Border line = BorderFactory.createLineBorder(AppTheme.BG_BORDER, 1, true);
        Border pad = BorderFactory.createEmptyBorder(10, 15, 10, 15);
        field.setBorder(new CompoundBorder(line, pad));

        field.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterCards(); }
            public void removeUpdate(DocumentEvent e) { filterCards(); }
            public void changedUpdate(DocumentEvent e) { filterCards(); }
        });
        return field;
    }

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
            BorderFactory.createLineBorder(new Color(255, 152, 0), 1, true),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)
    );
    card.setBorder(defaultBorder);

    JPanel textContainer = new JPanel();
    textContainer.setLayout(new BoxLayout(textContainer, BoxLayout.Y_AXIS));
    textContainer.setOpaque(false);

    JLabel titleLbl = new JLabel("<html>" + recipe.getTitle() + "</html>");
    titleLbl.setFont(new Font("SansSerif", Font.BOLD, 15));
    titleLbl.setForeground(AppTheme.TEXT_PRIMARY);

    JLabel timeLbl = new JLabel("🕒 " + recipe.getFormattedPrepTime());
    timeLbl.setFont(AppTheme.FONT_SMALL);
    timeLbl.setForeground(new Color(255, 152, 0));

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

    // --- Styling Helpers ---

    private void styleCircleButton(JButton btn, Color bg, Color fg) {
        btn.setPreferredSize(new Dimension(40, 40));
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFont(new Font("SansSerif", Font.BOLD, 18));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

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
package com.lefestin.ui.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
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
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
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

        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(AppTheme.BG_PAGE);
        setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

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

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setBackground(AppTheme.BG_PAGE);

        JLabel titleLabel = new JLabel("My Recipes");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
        titleLabel.setForeground(AppTheme.TEXT_PRIMARY);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionPanel.setBackground(AppTheme.BG_PAGE);

        JButton addBtn = new JButton("+");
        styleCircleButton(addBtn, AppTheme.GREEN_PRIMARY, Color.WHITE); 
        addBtn.addActionListener(e -> openAddEditPanel(null));

        actionPanel.add(addBtn);

        topRow.add(titleLabel, BorderLayout.WEST);
        topRow.add(actionPanel, BorderLayout.EAST);

        searchField = new JTextField();
        searchField.setFont(AppTheme.FONT_BODY);
        searchField.setBackground(AppTheme.BG_SURFACE);
        searchField.setForeground(AppTheme.TEXT_PRIMARY);
        searchField.setCaretColor(AppTheme.TEXT_PRIMARY);
        
        Border line = BorderFactory.createLineBorder(AppTheme.BG_BORDER, 1, true);
        Border pad = BorderFactory.createEmptyBorder(10, 15, 10, 15);
        searchField.setBorder(new CompoundBorder(line, pad));

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { filterCards(); }
            @Override
            public void removeUpdate(DocumentEvent e) { filterCards(); }
            @Override
            public void changedUpdate(DocumentEvent e) { filterCards(); }
        });

        headerPanel.add(topRow);
        headerPanel.add(Box.createVerticalStrut(20));
        searchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        searchField.setAlignmentX(Component.LEFT_ALIGNMENT);   
        headerPanel.add(searchField);
        headerPanel.add(Box.createVerticalStrut(10));

        return headerPanel;
    }

    private void styleCircleButton(JButton btn, Color bg, Color fg) {
        btn.setPreferredSize(new Dimension(40, 40));
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFont(new Font("SansSerif", Font.BOLD, 18));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void renderCards(List<Recipe> recipes) {
        cardsContainer.removeAll();
        for (int i = 0; i < recipes.size(); i++) {
            cardsContainer.add(createRecipeCard(recipes.get(i)));
            if (i < recipes.size() - 1) {
                cardsContainer.add(Box.createVerticalStrut(12)); // 12px vertical gap
            }
        }
        cardsContainer.revalidate();
        cardsContainer.repaint();
    }

    private JPanel createRecipeCard(Recipe recipe) {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));     
        card.setPreferredSize(new Dimension(300, 130)); 
        card.setBackground(AppTheme.BG_SURFACE);
        
        Border lineBorder = BorderFactory.createLineBorder(AppTheme.BG_BORDER, 1, true);
        Border padding = BorderFactory.createEmptyBorder(15, 20, 15, 20);
        card.setBorder(new CompoundBorder(lineBorder, padding));

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        
        JButton deleteBtn = new JButton("✖");
        deleteBtn.setFont(AppTheme.FONT_SMALL);
        deleteBtn.setForeground(AppTheme.TEXT_MUTED);
        deleteBtn.setContentAreaFilled(false);
        deleteBtn.setBorderPainted(false);
        deleteBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        deleteBtn.setToolTipText("Delete Recipe");
        deleteBtn.addActionListener(e -> deleteRecipe(recipe));
        
        headerRow.add(deleteBtn, BorderLayout.EAST);

        JLabel titleLbl = new JLabel("<html>" + recipe.getTitle() + "</html>");
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 18));
        titleLbl.setForeground(AppTheme.TEXT_PRIMARY);
        titleLbl.setVerticalAlignment(SwingConstants.TOP);

        JPanel bottomRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        bottomRow.setOpaque(false);
        
        JLabel timeLbl = new JLabel("🕒 " + recipe.getFormattedPrepTime());
        timeLbl.setFont(AppTheme.FONT_SMALL);
        timeLbl.setForeground(new Color(255, 152, 0)); 

        bottomRow.add(timeLbl);

        card.add(headerRow, BorderLayout.NORTH);
        card.add(titleLbl, BorderLayout.CENTER);
        card.add(bottomRow, BorderLayout.SOUTH);

        card.setCursor(new Cursor(Cursor.HAND_CURSOR));
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openAddEditPanel(recipe);
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBorder(new CompoundBorder(
                    BorderFactory.createLineBorder(new Color(255, 152, 0), 1, true),
                    padding
                ));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                card.setBorder(new CompoundBorder(lineBorder, padding));
            }
        });

        return card;
    }

    public void loadRecipes() {
        try {
            allRecipes = recipeDAO.getAllRecipes(frame.getCurrentUserId());
            filterCards(); 
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load recipes: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete \"" + recipe.getTitle() + "\"? This cannot be undone.",
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                recipeDAO.deleteRecipe(recipe.getRecipeId());
                loadRecipes();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Failed to delete: " + ex.getMessage());
            }
        }
    }

    private void openAddEditPanel(Recipe recipe) {
        // Delegate to MainFrame so CardLayout handles the transition smoothly
        frame.showAddEditRecipePanel(recipe);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) loadRecipes();
    }
}
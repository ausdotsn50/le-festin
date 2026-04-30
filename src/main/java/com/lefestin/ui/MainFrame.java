package com.lefestin.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;

import com.lefestin.model.Recipe;
import com.lefestin.model.User;
import com.lefestin.ui.dialogs.LoginDialog;
import com.lefestin.ui.panels.AddEditRecipePanel;
import com.lefestin.ui.panels.GroceryListPanel;
import com.lefestin.ui.panels.PantryPanel;
import com.lefestin.ui.panels.RecipeDetailPanel;
import com.lefestin.ui.panels.RecipeListPanel;
import com.lefestin.ui.panels.RecipeSuggestionsPanel;
import com.lefestin.ui.panels.WeeklyPlannerPanel;

/**
 * MainFrame — the root JFrame for Le Festin.
 *
 * CardLayout swaps panels instantly with no DB re-query on switch —
 * each panel loads its data lazily when first shown.
 */
public class MainFrame extends JFrame {

    // For CARD LAYOUT switching
    public static final String CARD_RECIPES = "Recipes";
    public static final String CARD_PANTRY = "My Pantry";
    public static final String CARD_PLANNER = "Meal Planner";
    public static final String CARD_SUGGESTIONS = "Suggestions";
    public static final String CARD_GROCERY = "Grocery List";
    public static final String CARD_DETAIL = "Recipe Detail";
    public static final String CARD_ADD_EDIT = "Add Edit Recipe";

    private static final String[] NAV_ITEMS = {
        CARD_RECIPES,
        CARD_PANTRY,
        CARD_PLANNER,
        CARD_SUGGESTIONS,
        CARD_GROCERY
    };

    // Session
    private User currentUser;

    // Layout comps
    private CardLayout cardLayout;
    private JPanel contentArea;
    private JButton userMenuButton;

    // Nav buttons
    private JButton[] navButtons;

    private RecipeListPanel recipeListPanel;
    private PantryPanel pantryPanel;
    private WeeklyPlannerPanel plannerPanel;
    private RecipeSuggestionsPanel suggestionsPanel;
    private GroceryListPanel  groceryListPanel;
    private RecipeDetailPanel recipeDetailPanel;
    
    public MainFrame() {
        setTitle("Le Festin");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1024, 680));

        initComponents();
        pack();
        setLocationRelativeTo(null); // center on screen
    }

    // UI comp builder
    private void initComponents() {
        setLayout(new BorderLayout());

        add(buildHeader(),  BorderLayout.NORTH);
        add(buildSidebar(), BorderLayout.WEST);
        add(buildContent(), BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(AppTheme.HEADER_BG);
        header.setPreferredSize(new Dimension(0, 56));
        header.setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 24));

        JLabel appName = new JLabel("Le Festin");
        appName.setForeground(AppTheme.HEADER_FG);
        appName.setFont(AppTheme.FONT_APP_NAME);
        appName.setForeground(Color.BLACK);

        userMenuButton = buildUserMenuButton();

        header.add(appName, BorderLayout.WEST);
        header.add(userMenuButton, BorderLayout.EAST);
        return header;
    }

    private JButton buildUserMenuButton() {
        JButton btn = new JButton("Not logged in");
        btn.setForeground(AppTheme.HEADER_FG_MUTED);
        btn.setFont(AppTheme.FONT_SMALL);
        btn.setBackground(AppTheme.HEADER_BG);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btn.addActionListener(e -> showUserMenu(btn));

        return btn;
    }

    private void showUserMenu(JButton button) {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem logoutItem = new JMenuItem("Logout");
        logoutItem.addActionListener(e -> performLogout());
        menu.add(logoutItem);

        menu.show(button, 0, button.getHeight());
    }

    private void performLogout() {
        dispose();
        MainFrame newFrame = new MainFrame();
        LoginDialog loginDialog = new LoginDialog(newFrame);
        loginDialog.setVisible(true);
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(AppTheme.SIDEBAR_BG);
        
        // Narrower sidebar to fit the stacked icon design perfectly
        sidebar.setPreferredSize(new Dimension(120, 0));
        sidebar.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        navButtons = new JButton[NAV_ITEMS.length];

        for (int i = 0; i < NAV_ITEMS.length; i++) {
            JButton btn = buildNavButton(NAV_ITEMS[i]);
            navButtons[i] = btn;
            sidebar.add(btn);
            sidebar.add(Box.createVerticalStrut(8)); // slightly larger gap between items
        }

        // Push everything to the top
        sidebar.add(Box.createVerticalGlue());

        return sidebar;
    }

    private JButton buildNavButton(String label) {
        JButton btn = new JButton(label);
        
        // Compact dimensions to fit nicely in the narrow sidebar
        btn.setMaximumSize(new Dimension(120, 75));
        btn.setPreferredSize(new Dimension(120, 75));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        btn.setVerticalTextPosition(SwingConstants.BOTTOM);
        btn.setHorizontalTextPosition(SwingConstants.CENTER);
        btn.setHorizontalAlignment(SwingConstants.CENTER);
        btn.setIcon(new NavIcon(label)); 

        btn.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        // Derived a slightly smaller font size so it fits neatly under the icon
        btn.setFont(AppTheme.FONT_BODY.deriveFont(12f)); 
        
        btn.setBackground(AppTheme.SIDEBAR_BG);
        btn.setForeground(AppTheme.TEXT_PRIMARY);
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setBorderPainted(false);

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (!btn.getBackground().equals(AppTheme.SIDEBAR_ACTIVE))
                    btn.setBackground(AppTheme.SIDEBAR_HOVER);
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (!btn.getBackground().equals(AppTheme.SIDEBAR_ACTIVE))
                    btn.setBackground(AppTheme.SIDEBAR_BG);
            }
        });

        btn.addActionListener(e -> navigateTo(label));
        return btn;
    }

    // Content area: card layout
    private JPanel buildContent() {
        cardLayout  = new CardLayout();
        contentArea = new JPanel(cardLayout);
        contentArea.setBackground(AppTheme.BG_PAGE);

        // Instantiate all panels
        recipeListPanel    = new RecipeListPanel(this);
        pantryPanel        = new PantryPanel(this);
        plannerPanel       = new WeeklyPlannerPanel(this);
        suggestionsPanel   = new RecipeSuggestionsPanel(this);
        groceryListPanel   = new GroceryListPanel(this);
        recipeDetailPanel   = null;

        // Register each panel under its card name
        contentArea.add(recipeListPanel,    CARD_RECIPES);
        contentArea.add(pantryPanel,        CARD_PANTRY);
        contentArea.add(plannerPanel,       CARD_PLANNER);
        contentArea.add(suggestionsPanel,   CARD_SUGGESTIONS);
        contentArea.add(groceryListPanel,   CARD_GROCERY);

        // Show recipes by default
        navigateTo(CARD_RECIPES);

        return contentArea;
    }

    // For nav purposes
    public void navigateTo(String cardName) {
        cardLayout.show(contentArea, cardName);
        highlightActiveNavButton(cardName);
    }

    private void highlightActiveNavButton(String activeCard) {
        for (int i = 0; i < NAV_ITEMS.length; i++) {
            boolean isActive = NAV_ITEMS[i].equals(activeCard);
            
            // Change the background if it's active
            navButtons[i].setBackground(isActive
                ? AppTheme.SIDEBAR_ACTIVE
                : AppTheme.SIDEBAR_BG);
                
            // Lock the text to the default color no matter what
            navButtons[i].setForeground(AppTheme.TEXT_PRIMARY);
        }
    }

    // Session management
    public void setCurrentUser(User user) {
        this.currentUser = user;
        userMenuButton.setText(user != null ? user.getUsername() : "Not logged in");
        
        // Reload visible panels after authentication
        if (user != null) {
            refreshVisiblePanel();
        }
    }
    
    // Refresh the currently visible panel's data
    // Recipe panel bugfix (loading recipes on auth entry)
    private void refreshVisiblePanel() {
        if (recipeListPanel != null) recipeListPanel.loadRecipes();
        if (pantryPanel != null) pantryPanel.loadPantry();
    }

    public User getCurrentUser() { return currentUser; }

    public int getCurrentUserId() {
        return currentUser != null ? currentUser.getUserId() : -1;
    }

    public String getCurrentUsername() {
        return currentUser != null ? currentUser.getUsername() : "";
    }

    // Dynamically creates the editor panel, adds it to the CardLayout, and shows it
    public void showAddEditRecipePanel(Recipe r) {
        AddEditRecipePanel editor = new AddEditRecipePanel(this, r);
        contentArea.add(editor, CARD_ADD_EDIT);
        navigateTo(CARD_ADD_EDIT);
    }

    public void showRecipeDetailPanel(Recipe r) {
        recipeDetailPanel = new RecipeDetailPanel(this, r);
        contentArea.add(recipeDetailPanel, CARD_DETAIL);
        navigateTo(CARD_DETAIL);
    }

    // Switches back to the Recipe List and forces it to refresh the database
    public void showRecipeList() {
        navigateTo(CARD_RECIPES);
        if (recipeListPanel != null) {
            recipeListPanel.loadRecipes(); 
        }
    }

    /**
     * Custom Icon implementation that draws perfectly centered symbols
     * and automatically inherits the Button's foreground color.
     */
    private static class NavIcon implements Icon {
        private final String label;

        public NavIcon(String label) {
            this.label = label;
        }

        @Override
        public int getIconWidth() { return 30; }

        @Override
        public int getIconHeight() { return 34; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            
            String symbol = "📌"; 
            switch (label) {
                case CARD_RECIPES -> symbol = "📖";
                // Book / Bookmark
                case CARD_PANTRY -> symbol = "📦";
                // Pantry Box
                case CARD_PLANNER -> symbol = "📅";
                // Calendar
                case CARD_SUGGESTIONS -> symbol = "💡";
                // Idea / Suggestions
                case CARD_GROCERY -> symbol = "🛒";
                // Cart
            }

            g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24)); // Universally supported icon font
            g2.setColor(c.getForeground()); // This guarantees the icon turns active when the button does!
            
            FontMetrics fm = g2.getFontMetrics();
            int textX = x + (getIconWidth() - fm.stringWidth(symbol)) / 2;
            int textY = y + fm.getAscent() + (getIconHeight() - fm.getHeight()) / 2;
            
            g2.drawString(symbol, textX, textY);
            g2.dispose();
        }
    }
}
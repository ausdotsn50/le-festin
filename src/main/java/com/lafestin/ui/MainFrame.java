package com.lafestin.ui;

import com.lafestin.model.User;
import com.lafestin.ui.panels.*;

import javax.swing.*;
import java.awt.*;

/**
 * MainFrame — the root JFrame for La Festin.
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
    private JLabel usernameLabel;

    // Nav buttons
    private JButton[] navButtons;

    // To be created: panels
    private RecipeListPanel recipeListPanel;
    private PantryPanel pantryPanel;
    private WeeklyPlannerPanel plannerPanel;
    private RecipeSuggestionsPanel suggestionsPanel;
    private GroceryListPanel  groceryListPanel;

    public MainFrame() {
        setTitle("La Festin");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1024, 680));
        setPreferredSize(new Dimension(1280, 800));

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

        JLabel appName = new JLabel("La Festin");
        appName.setForeground(AppTheme.HEADER_FG);
        appName.setFont(AppTheme.FONT_APP_NAME);

        usernameLabel = new JLabel("Not logged in");
        usernameLabel.setForeground(AppTheme.HEADER_FG_MUTED);
        usernameLabel.setFont(AppTheme.FONT_SMALL);

        header.add(appName,       BorderLayout.WEST);
        header.add(usernameLabel, BorderLayout.EAST);
        return header;
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(AppTheme.SIDEBAR_BG);
        sidebar.setPreferredSize(new Dimension(200, 0));
        sidebar.setBorder(BorderFactory.createEmptyBorder(16, 0, 16, 0));

        navButtons = new JButton[NAV_ITEMS.length];

        for (int i = 0; i < NAV_ITEMS.length; i++) {
            JButton btn = buildNavButton(NAV_ITEMS[i]);
            navButtons[i] = btn;
            sidebar.add(btn);
            sidebar.add(Box.createVerticalStrut(4));
        }

        // Push everything to the top
        sidebar.add(Box.createVerticalGlue());

        return sidebar;
    }

    private JButton buildNavButton(String label) {
        JButton btn = new JButton(label);
        btn.setMaximumSize(new Dimension(200, 44));
        btn.setPreferredSize(new Dimension(200, 44));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setFont(AppTheme.FONT_BODY);
        btn.setBackground(AppTheme.SIDEBAR_BG);
        btn.setForeground(AppTheme.SIDEBAR_FG);
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

    // Highlight eff.
    private void highlightActiveNavButton(String activeCard) {
        for (int i = 0; i < NAV_ITEMS.length; i++) {
            boolean isActive = NAV_ITEMS[i].equals(activeCard);
            navButtons[i].setBackground(isActive
                ? AppTheme.SIDEBAR_ACTIVE
                : AppTheme.SIDEBAR_BG);
            navButtons[i].setForeground(isActive
                ? AppTheme.SIDEBAR_FG_ACTIVE
                : AppTheme.SIDEBAR_FG);
        }
    }

    // Session management
    public void setCurrentUser(User user) {
        this.currentUser = user;
        usernameLabel.setText(user != null ? user.getUsername() : "Not logged in");
    }

    public User getCurrentUser() { return currentUser; }

    public int getCurrentUserId() {
        return currentUser != null ? currentUser.getUserId() : -1;
    }

    public String getCurrentUsername() {
        return currentUser != null ? currentUser.getUsername() : "";
    }
}

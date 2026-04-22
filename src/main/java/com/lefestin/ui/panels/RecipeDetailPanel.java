package com.lefestin.ui.panels;

import com.lefestin.dao.RecipeDAO;
import com.lefestin.dao.RecipeIngredientDAO;
import com.lefestin.model.Recipe;
import com.lefestin.model.RecipeIngredient;
import com.lefestin.model.RecipeMatchResult;
import com.lefestin.service.RecipeMatchingService;
import com.lefestin.ui.AppTheme;
import com.lefestin.ui.MainFrame;
import com.lefestin.ui.dialogs.AddEditRecipeDialog;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * RecipeDetailPanel — full view of a single recipe.
 *
 * Layout:
 *   ┌──────────────────────────────────────────────────┐
 *   │  [← Back]                          [Edit Recipe] │  ← top bar
 *   ├──────────────────────────────────────────────────┤
 *   │  Pork Adobo                    [80% match badge] │  ← title row
 *   │  Dinner  ·  60 min                               │  ← subtitle
 *   ├──────────────────────────────────────────────────┤
 *   │  Ingredients                                     │  ← section
 *   │  Name           Quantity    Unit                 │
 *   │  Garlic         5           clove                │
 *   │  Pork belly     500         gram                 │
 *   │  ...                                             │
 *   ├──────────────────────────────────────────────────┤
 *   │  Procedure                                       │  ← section
 *   │  1. Cut pork belly into 2-inch cubes.            │
 *   │  2. Combine soy sauce, vinegar...                │
 *   └──────────────────────────────────────────────────┘
 */
public class RecipeDetailPanel extends JPanel {

    // ── Dependencies ──────────────────────────────────────────────────────
    private final MainFrame            frame;
    private final RecipeDAO            recipeDAO;
    private final RecipeIngredientDAO  riDAO;
    private final RecipeMatchingService matchingService;

    // ── Current recipe ────────────────────────────────────────────────────
    private Recipe currentRecipe;

    // ── UI components updated on loadRecipe() ─────────────────────────────
    private JLabel            titleLabel;
    private JLabel            subtitleLabel;
    private JLabel            matchBadge;
    private DefaultTableModel ingredientModel;
    private JTextArea         procedureArea;
    private JButton           editBtn;

    // ── Column indexes ────────────────────────────────────────────────────
    private static final int COL_NAME     = 0;
    private static final int COL_QUANTITY = 1;
    private static final int COL_UNIT     = 2;

    // ── Constructor ───────────────────────────────────────────────────────
    public RecipeDetailPanel(MainFrame frame) {
        this.frame           = frame;
        this.recipeDAO       = new RecipeDAO();
        this.riDAO           = new RecipeIngredientDAO();
        this.matchingService = new RecipeMatchingService();

        setLayout(new BorderLayout(0, 0));
        setBackground(AppTheme.BG_PAGE);

        add(buildTopBar(),   BorderLayout.NORTH);
        add(buildBody(),     BorderLayout.CENTER);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  TOP BAR — Back + Edit buttons
    // ══════════════════════════════════════════════════════════════════════

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(AppTheme.BG_SURFACE);
        bar.setBorder(BorderFactory.createCompoundBorder(
            AppTheme.BORDER_DIVIDER,
            BorderFactory.createEmptyBorder(10, 16, 10, 16)));

        JButton backBtn = AppTheme.ghostButton("← Back to Recipes");
        backBtn.addActionListener(
            e -> frame.navigateTo(MainFrame.CARD_RECIPES));

        editBtn = AppTheme.secondaryButton("Edit Recipe");
        editBtn.addActionListener(e -> openEditDialog());

        bar.add(backBtn, BorderLayout.WEST);
        bar.add(editBtn, BorderLayout.EAST);

        return bar;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  BODY — title, subtitle, match badge, ingredients, procedure
    // ══════════════════════════════════════════════════════════════════════

    private JScrollPane buildBody() {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(AppTheme.BG_PAGE);
        body.setBorder(BorderFactory.createEmptyBorder(24, 32, 32, 32));

        body.add(buildTitleSection());
        body.add(Box.createVerticalStrut(24));
        body.add(buildIngredientSection());
        body.add(Box.createVerticalStrut(24));
        body.add(buildProcedureSection());

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(AppTheme.BG_PAGE);
        scroll.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        return scroll;
    }

    // ── Title + subtitle + match badge ────────────────────────────────────
    private JPanel buildTitleSection() {
        JPanel section = new JPanel(new BorderLayout(16, 0));
        section.setBackground(AppTheme.BG_PAGE);
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        // Left: title + subtitle stacked
        JPanel textStack = new JPanel();
        textStack.setLayout(new BoxLayout(textStack, BoxLayout.Y_AXIS));
        textStack.setBackground(AppTheme.BG_PAGE);

        titleLabel = new JLabel("—");
        titleLabel.setFont(AppTheme.FONT_TITLE);
        titleLabel.setForeground(AppTheme.TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        subtitleLabel = new JLabel("—");
        subtitleLabel.setFont(AppTheme.FONT_SMALL);
        subtitleLabel.setForeground(AppTheme.TEXT_MUTED);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        textStack.add(titleLabel);
        textStack.add(Box.createVerticalStrut(4));
        textStack.add(subtitleLabel);

        // Right: match badge — colored pill
        matchBadge = new JLabel("—");
        matchBadge.setFont(AppTheme.FONT_LABEL);
        matchBadge.setOpaque(true);
        matchBadge.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
        matchBadge.setHorizontalAlignment(SwingConstants.CENTER);
        // Default neutral styling — updated in loadMatchBadge()
        matchBadge.setBackground(AppTheme.BG_SUBTLE);
        matchBadge.setForeground(AppTheme.TEXT_MUTED);

        JPanel badgeWrapper = new JPanel(
            new FlowLayout(FlowLayout.RIGHT, 0, 0));
        badgeWrapper.setBackground(AppTheme.BG_PAGE);
        badgeWrapper.add(matchBadge);

        section.add(textStack,    BorderLayout.CENTER);
        section.add(badgeWrapper, BorderLayout.EAST);

        return section;
    }

    // ── Ingredients section ───────────────────────────────────────────────
    private JPanel buildIngredientSection() {
        JPanel section = new JPanel(new BorderLayout(0, 10));
        section.setBackground(AppTheme.BG_PAGE);
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel heading = AppTheme.headingLabel("Ingredients");
        heading.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));

        // Table model — not editable
        ingredientModel = new DefaultTableModel(
            new String[]{"Name", "Quantity", "Unit"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        JTable table = new JTable(ingredientModel);
        AppTheme.styleTable(table);

        // Column widths
        table.getColumnModel().getColumn(COL_NAME)
            .setPreferredWidth(280);
        table.getColumnModel().getColumn(COL_QUANTITY)
            .setPreferredWidth(100);
        table.getColumnModel().getColumn(COL_UNIT)
            .setPreferredWidth(100);

        // Alternating rows
        table.setDefaultRenderer(Object.class,
            AppTheme.alternatingRowRenderer());

        // Fixed height — no need to scroll ingredients
        int rowHeight  = 38;
        int headerHeight = 28;
        int maxRows    = 8;
        table.setPreferredScrollableViewportSize(
            new Dimension(0, rowHeight * Math.min(maxRows,
                Math.max(3, ingredientModel.getRowCount()))
                + headerHeight));

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(
            AppTheme.BG_BORDER, 1));
        scroll.getViewport().setBackground(AppTheme.BG_SURFACE);

        section.add(heading, BorderLayout.NORTH);
        section.add(scroll,  BorderLayout.CENTER);

        return section;
    }

    // ── Procedure section ─────────────────────────────────────────────────
    private JPanel buildProcedureSection() {
        JPanel section = new JPanel(new BorderLayout(0, 10));
        section.setBackground(AppTheme.BG_PAGE);
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel heading = AppTheme.headingLabel("Procedure");
        heading.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));

        procedureArea = new JTextArea();
        procedureArea.setFont(AppTheme.FONT_MONO);
        procedureArea.setBackground(AppTheme.BG_SURFACE);
        procedureArea.setForeground(AppTheme.TEXT_PRIMARY);
        procedureArea.setLineWrap(true);
        procedureArea.setWrapStyleWord(true);
        procedureArea.setEditable(false);
        procedureArea.setBorder(
            BorderFactory.createEmptyBorder(12, 14, 12, 14));

        JScrollPane scroll = new JScrollPane(procedureArea);
        scroll.setBorder(BorderFactory.createLineBorder(
            AppTheme.BG_BORDER, 1));
        scroll.setPreferredSize(new Dimension(0, 200));

        section.add(heading, BorderLayout.NORTH);
        section.add(scroll,  BorderLayout.CENTER);

        return section;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  DATA LOADING
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Loads a recipe by ID and populates all UI components.
     * Called by MainFrame.showRecipeDetail(int recipeId).
     */
    public void loadRecipe(int recipeId) {
        // Use SwingWorker so DAO calls don't block the EDT
        new SwingWorker<RecipeLoadResult, Void>() {

            @Override
            protected RecipeLoadResult doInBackground() throws Exception {
                Recipe recipe = recipeDAO.getRecipeById(recipeId);
                if (recipe == null) return null;

                List<RecipeIngredient> ingredients =
                    riDAO.getIngredientsByRecipeId(recipeId);

                RecipeMatchResult matchResult =
                    matchingService.getMatchForRecipe(
                        frame.getCurrentUserId(), recipeId);

                return new RecipeLoadResult(recipe, ingredients, matchResult);
            }

            @Override
            protected void done() {
                try {
                    RecipeLoadResult result = get();
                    if (result == null) {
                        JOptionPane.showMessageDialog(
                            RecipeDetailPanel.this,
                            "Recipe not found.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                        frame.navigateTo(MainFrame.CARD_RECIPES);
                        return;
                    }
                    populateUI(result);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(
                        RecipeDetailPanel.this,
                        "Failed to load recipe: " + ex.getMessage(),
                        "Database Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // ── Populates every UI component from the loaded data ─────────────────
    private void populateUI(RecipeLoadResult result) {
        currentRecipe = result.recipe;

        // ── Title + subtitle ───────────────────────────────────────────────
        titleLabel.setText(result.recipe.getTitle());
        subtitleLabel.setText(
            result.recipe.getCategory()
            + "  ·  "
            + result.recipe.getFormattedPrepTime());

        // ── Match badge ────────────────────────────────────────────────────
        loadMatchBadge(result.matchResult);

        // ── Ingredients table ──────────────────────────────────────────────
        ingredientModel.setRowCount(0);
        for (RecipeIngredient ri : result.ingredients) {
            ingredientModel.addRow(new Object[]{
                capitalize(ri.getIngredientName()),
                formatQty(ri.getQuantity()),
                ri.getUnit()
            });
        }

        // ── Procedure ──────────────────────────────────────────────────────
        procedureArea.setText(result.recipe.getProcedure());
        procedureArea.setCaretPosition(0); // scroll to top

        // Repaint so changes are visible immediately
        revalidate();
        repaint();
    }

    // ── Colors the match badge based on percentage ────────────────────────
    private void loadMatchBadge(RecipeMatchResult matchResult) {
        if (matchResult == null) {
            matchBadge.setText("No pantry data");
            matchBadge.setBackground(AppTheme.BG_SUBTLE);
            matchBadge.setForeground(AppTheme.TEXT_MUTED);
            return;
        }

        int pct = matchResult.getMatchPercent();
        matchBadge.setText(matchResult.getMatchLabel());

        if (pct == 100) {
            // Ready to cook — herb green
            matchBadge.setBackground(AppTheme.GREEN_TINT);
            matchBadge.setForeground(AppTheme.GREEN_TINT_TEXT);
        } else if (pct >= 50) {
            // Partial match — saffron amber
            matchBadge.setBackground(AppTheme.AMBER_TINT);
            matchBadge.setForeground(AppTheme.AMBER_TINT_TEXT);
        } else {
            // Low match — terracotta
            matchBadge.setBackground(AppTheme.TERRA_TINT);
            matchBadge.setForeground(AppTheme.TERRA_TINT_TEXT);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  EDIT ACTION
    // ══════════════════════════════════════════════════════════════════════

    private void openEditDialog() {
        if (currentRecipe == null) return;

        AddEditRecipeDialog dialog =
            new AddEditRecipeDialog(frame, currentRecipe);
        dialog.setVisible(true);

        if (dialog.isSaved()) {
            // Reload this panel with fresh data after edit
            loadRecipe(currentRecipe.getRecipeId());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════════════

    /** Formats double quantity — strips trailing .0 for whole numbers. */
    private String formatQty(double qty) {
        return (qty == Math.floor(qty))
            ? String.valueOf((int) qty)
            : String.valueOf(qty);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // ── Internal data transfer object for SwingWorker ─────────────────────
    private static class RecipeLoadResult {
        final Recipe                 recipe;
        final List<RecipeIngredient> ingredients;
        final RecipeMatchResult      matchResult;

        RecipeLoadResult(Recipe recipe,
                          List<RecipeIngredient> ingredients,
                          RecipeMatchResult matchResult) {
            this.recipe      = recipe;
            this.ingredients = ingredients;
            this.matchResult = matchResult;
        }
    }
}
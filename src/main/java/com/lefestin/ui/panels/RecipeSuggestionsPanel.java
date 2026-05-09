package com.lefestin.ui.panels;

import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.*;

import com.lefestin.dao.MealEntryDAO;
import com.lefestin.helper.Helper;
import com.lefestin.model.*;
import com.lefestin.service.RecipeMatchingService;
import com.lefestin.ui.AppTheme;
import com.lefestin.ui.MainFrame;

/**
 * RecipeSuggestionsPanel — ranked recipe cards based on pantry match %.
 */
public class RecipeSuggestionsPanel extends JPanel {

    private final MainFrame frame;
    private final RecipeMatchingService matchingService = new RecipeMatchingService();
    private final MealEntryDAO mealEntryDAO = new MealEntryDAO();

    private static final String FILTER_ALL = "All";
    private static final String FILTER_READY = "Ready to Cook";
    private static final String FILTER_PARTIAL = "Partial Match";
    private String activeFilter = FILTER_ALL;

    private List<RecipeMatchResult> allResults = new ArrayList<>();

    private JPanel cardsPanel;
    private JLabel statusLabel;
    private JButton filterAllBtn, filterReadyBtn, filterPartialBtn;

    public RecipeSuggestionsPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        setBackground(AppTheme.BG_PAGE);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildCardsArea(), BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 8));
        header.setBackground(AppTheme.BG_SURFACE);
        header.setBorder(BorderFactory.createCompoundBorder(
            AppTheme.BORDER_DIVIDER,
            BorderFactory.createEmptyBorder(14, 20, 14, 20)));

        // Top row: title and status
        JPanel titleStack = new JPanel();
        titleStack.setLayout(new BoxLayout(titleStack, BoxLayout.Y_AXIS));
        titleStack.setBackground(AppTheme.BG_SURFACE);

        JLabel title = AppTheme.titleLabel("Suggestions");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        statusLabel = AppTheme.subtitleLabel("Loading...");
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        titleStack.add(title);
        titleStack.add(Box.createVerticalStrut(2));
        titleStack.add(statusLabel);

        // Bottom row: filter toggles
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filterRow.setBackground(AppTheme.BG_SURFACE);

        filterAllBtn = buildFilterButton(FILTER_ALL, true);
        filterReadyBtn = buildFilterButton(FILTER_READY, false);
        filterPartialBtn = buildFilterButton(FILTER_PARTIAL, false);

        filterAllBtn.addActionListener(e -> applyFilter(FILTER_ALL));
        filterReadyBtn.addActionListener(e -> applyFilter(FILTER_READY));
        filterPartialBtn.addActionListener(e -> applyFilter(FILTER_PARTIAL));

        filterRow.add(filterAllBtn);
        filterRow.add(filterReadyBtn);
        filterRow.add(filterPartialBtn);

        header.add(titleStack, BorderLayout.NORTH);
        header.add(filterRow, BorderLayout.SOUTH);

        return header;
    }

    private JButton buildFilterButton(String text, boolean active) {
        JButton btn = AppTheme.ghostButton(text);
        btn.setFont(AppTheme.FONT_SMALL);
        if (active) styleAsActiveFilter(btn);
        return btn;
    }

    private void applyFilter(String filter) {
        activeFilter = filter;
        resetFilterButtons();

        JButton active = switch (filter) {
            case FILTER_READY -> filterReadyBtn;
            case FILTER_PARTIAL -> filterPartialBtn;
            default -> filterAllBtn;
        };

        styleAsActiveFilter(active);
        renderCards(getFilteredResults());
    }

    private void resetFilterButtons() {
        for (JButton btn : new JButton[]{filterAllBtn, filterReadyBtn, filterPartialBtn}) {
            btn.setBackground(AppTheme.BG_SURFACE);
            btn.setForeground(AppTheme.TEXT_SECONDARY);
            btn.setBorderPainted(true);
            btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BG_BORDER, 1),
                BorderFactory.createEmptyBorder(6, 14, 6, 14)));
        }
    }

    private void styleAsActiveFilter(JButton btn) {
        btn.setBackground(AppTheme.GREEN_PRIMARY);
        btn.setForeground(AppTheme.TEXT_INVERTED);
        btn.setBorderPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(7, 15, 7, 15));
    }

    private List<RecipeMatchResult> getFilteredResults() {
        return switch (activeFilter) {
            case FILTER_READY -> allResults.stream().filter(RecipeMatchResult::isFullMatch).toList();
            case FILTER_PARTIAL -> allResults.stream().filter(r -> !r.isFullMatch() && r.getMatchPercent() > 0).toList();
            default -> allResults;
        };
    }

    private JScrollPane buildCardsArea() {
        cardsPanel = new JPanel();
        cardsPanel.setLayout(new BoxLayout(cardsPanel, BoxLayout.Y_AXIS));
        cardsPanel.setBackground(AppTheme.BG_PAGE);
        cardsPanel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JScrollPane scroll = new JScrollPane(cardsPanel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(AppTheme.BG_PAGE);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        return scroll;
    }

    private void loadSuggestions() {
        statusLabel.setText("Calculating matches...");
        showStateMessage("Checking your pantry...", AppTheme.TEXT_MUTED);

        new SwingWorker<List<RecipeMatchResult>, Void>() {
            @Override
            protected List<RecipeMatchResult> doInBackground() throws SQLException {
                return matchingService.getMatchedRecipes(frame.getCurrentUserId());
            }

            @Override
            protected void done() {
                try {
                    allResults = get();
                    renderCards(getFilteredResults());
                    updateStatusLabel();
                } catch (InterruptedException | ExecutionException ex) {
                    showStateMessage("Failed to load suggestions: " + ex.getMessage(), AppTheme.TERRA_PRIMARY);
                }
            }
        }.execute();
    }

    private void renderCards(List<RecipeMatchResult> results) {
        cardsPanel.removeAll();
        if (results.isEmpty()) {
            String msg = activeFilter.equals(FILTER_ALL)
                ? "No recipes found. Add recipes in the Recipes panel."
                : "No recipes match this filter.";
            showStateMessage(msg, AppTheme.TEXT_MUTED);
        } else {
            for (RecipeMatchResult result : results) {
                cardsPanel.add(buildRecipeCard(result));
                cardsPanel.add(Box.createVerticalStrut(12));
            }
        }
        cardsPanel.revalidate();
        cardsPanel.repaint();
    }

    private JPanel buildRecipeCard(RecipeMatchResult result) {
        Recipe recipe = result.getRecipe();
        int pct = result.getMatchPercent();

        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(AppTheme.BG_SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppTheme.BG_BORDER, 1),
            BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 999));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel topRow = new JPanel(new BorderLayout(10, 0));
        topRow.setOpaque(false);
        topRow.setAlignmentX(Component.LEFT_ALIGNMENT); 
        
        JLabel titleLabel = new JLabel(recipe.getTitle());
        titleLabel.setFont(AppTheme.FONT_HEADING);
        titleLabel.setForeground(AppTheme.TEXT_PRIMARY);
        topRow.add(titleLabel, BorderLayout.WEST);
        topRow.add(buildMatchBadge(pct, result.getMatchLabel()), BorderLayout.EAST);

        JLabel subtitleLabel = new JLabel(recipe.getFormattedPrepTime());
        subtitleLabel.setFont(AppTheme.FONT_SMALL);
        subtitleLabel.setForeground(AppTheme.TEXT_MUTED);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT); 

        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue(pct);
        bar.setPreferredSize(new Dimension(0, 6));
        bar.setBorderPainted(false);
        bar.setBackground(AppTheme.BG_SUBTLE);
        bar.setForeground(barColor(pct));
        bar.setAlignmentX(Component.LEFT_ALIGNMENT); 

        JPanel bottomSection = new JPanel();
        bottomSection.setLayout(new BoxLayout(bottomSection, BoxLayout.Y_AXIS));
        bottomSection.setOpaque(false);
        bottomSection.setAlignmentX(Component.LEFT_ALIGNMENT); 

        if (result.getMissingIngredients().isEmpty()) {
            JLabel allGood = new JLabel("All ingredients available in pantry");
            allGood.setFont(AppTheme.FONT_SMALL);
            allGood.setForeground(AppTheme.GREEN_PRIMARY);
            allGood.setAlignmentX(Component.LEFT_ALIGNMENT);
            bottomSection.add(allGood);
        } else {
            JLabel missingHeader = new JLabel("Still needed:");
            missingHeader.setFont(AppTheme.FONT_LABEL);
            missingHeader.setForeground(AppTheme.TEXT_SECONDARY);
            missingHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
            bottomSection.add(missingHeader);
            bottomSection.add(Box.createVerticalStrut(4));

            for (RecipeIngredient ri : result.getMissingIngredients()) {
                JLabel ing = new JLabel("  ·  " + Helper.capitalize(ri.getIngredientName())
                    + "  (" + Helper.formatQty(ri.getQuantity()) + " " + ri.getUnit() + ")");
                ing.setFont(AppTheme.FONT_SMALL);
                ing.setForeground(AppTheme.TERRA_PRIMARY);
                ing.setAlignmentX(Component.LEFT_ALIGNMENT); 
                bottomSection.add(ing);
            }
        }

        JButton assignBtn = AppTheme.primaryButton("Assign to Plan");
        assignBtn.addActionListener(e -> openAssignDialog(recipe));
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT); 
        btnRow.add(assignBtn);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.setAlignmentX(Component.LEFT_ALIGNMENT); 
        
        body.add(subtitleLabel);
        body.add(Box.createVerticalStrut(8));
        body.add(bar);
        body.add(Box.createVerticalStrut(8));
        body.add(bottomSection);
        body.add(Box.createVerticalStrut(10));
        body.add(btnRow);

        card.add(topRow, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private JLabel buildMatchBadge(int pct, String label) {
        JLabel badge = new JLabel(label);
        badge.setFont(AppTheme.FONT_LABEL);
        badge.setOpaque(true);
        badge.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));

        if (pct == 100) {
            badge.setBackground(AppTheme.GREEN_TINT);
            badge.setForeground(AppTheme.GREEN_TINT_TEXT);
        } else if (pct >= 50) {
            badge.setBackground(AppTheme.AMBER_TINT);
            badge.setForeground(AppTheme.AMBER_TINT_TEXT);
        } else {
            badge.setBackground(AppTheme.TERRA_TINT);
            badge.setForeground(AppTheme.TERRA_TINT_TEXT);
        }
        return badge;
    }

    private Color barColor(int pct) {
        if (pct == 100) return AppTheme.GREEN_PRIMARY;
        if (pct >= 50) return AppTheme.AMBER_PRIMARY;
        return AppTheme.TERRA_PRIMARY;
    }

    private void updateStatusLabel() {
        long ready = allResults.stream().filter(RecipeMatchResult::isFullMatch).count();
        String countText = allResults.size() + " recipe" + (allResults.size() == 1 ? "" : "s") + " scored";
        statusLabel.setText(countText + (ready > 0 ? "  ·  " + ready + " ready to cook" : ""));
    }

    private void showStateMessage(String text, Color color) {
        cardsPanel.removeAll();
        JLabel label = new JLabel(text);
        label.setFont(AppTheme.FONT_BODY);
        label.setForeground(color);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(40, 0, 0, 0));
        cardsPanel.add(label);
        cardsPanel.revalidate();
        cardsPanel.repaint();
    }

    private void openAssignDialog(Recipe recipe) {
        JPanel picker = new JPanel(new GridLayout(2, 2, 10, 8));
        picker.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));

        JSpinner dateSpinner = new JSpinner(new SpinnerDateModel());
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd"));
        ((JSpinner.DefaultEditor) dateSpinner.getEditor()).getTextField().setColumns(10);

        JComboBox<String> mealCombo = new JComboBox<>(new String[]{"Breakfast", "Lunch", "Dinner"});

        picker.add(new JLabel("Date:"));
        picker.add(dateSpinner);
        picker.add(new JLabel("Meal slot:"));
        picker.add(mealCombo);

        int choice = JOptionPane.showConfirmDialog(this,
            new Object[]{"Assign \"" + recipe.getTitle() + "\" to planner:", picker},
            "Assign to Plan", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (choice != JOptionPane.OK_OPTION) return;

        LocalDate date = ((java.util.Date) dateSpinner.getValue()).toInstant()
            .atZone(ZoneId.systemDefault()).toLocalDate();
        String mealType = (String) mealCombo.getSelectedItem();

        try {
            mealEntryDAO.addEntry(new MealEntry(recipe.getRecipeId(), frame.getCurrentUserId(), mealType, date, recipe.getTitle(), null));
            JOptionPane.showMessageDialog(this, "\"" + recipe.getTitle() + "\" assigned to " + mealType + " on " + date + ".", "Assigned", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to assign: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) loadSuggestions();
    }
}
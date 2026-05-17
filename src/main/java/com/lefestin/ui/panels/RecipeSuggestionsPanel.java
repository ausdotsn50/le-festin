package com.lefestin.ui.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.lefestin.dao.impl.MealEntryDAOImpl;
import com.lefestin.helper.Helper;
import com.lefestin.model.MealEntry;
import com.lefestin.model.Recipe;
import com.lefestin.model.RecipeIngredient;
import com.lefestin.model.RecipeMatchResult;
import com.lefestin.service.RecipeMatchingService;
import com.lefestin.ui.AppTheme;
import com.lefestin.ui.MainFrame;

/**
 * RecipeSuggestionsPanel — ranked recipe cards based on pantry match %.
 */
public class RecipeSuggestionsPanel extends BaseListPanel {

    private final RecipeMatchingService matchingService = new RecipeMatchingService();
    private final MealEntryDAOImpl mealEntryDAO = new MealEntryDAOImpl();

    private static final String FILTER_ALL = "All";
    private static final String FILTER_READY = "Ready to Cook";
    private static final String FILTER_PARTIAL = "Partial Match";
    private String activeFilter = FILTER_ALL;

    private List<RecipeMatchResult> allResults = new ArrayList<>();
    private JPanel cardsPanel;
    private JLabel statusLabel;
    private JButton filterAllBtn, filterReadyBtn, filterPartialBtn;

    public RecipeSuggestionsPanel(MainFrame frame) {
        super(frame);
    }

    // --- BaseListPanel contract ---

    @Override
    protected String getHeaderTitle() { return "Suggestions"; }

    @Override
    protected String getHeaderDescription() { return "Recipes ranked by your pantry match"; }

    @Override
    protected String getSearchPlaceholder() { return "Search recipes..."; }

    @Override
    protected JComponent buildHeaderRightControl() {
        statusLabel = new JLabel("Loading...");
        statusLabel.setFont(AppTheme.FONT_SMALL);
        statusLabel.setForeground(AppTheme.TEXT_MUTED);
        return statusLabel;
    }

    @Override
    protected JComponent buildSearchRightControl() {
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filterRow.setBackground(AppTheme.BG_SURFACE);

        filterAllBtn   = buildFilterButton(FILTER_ALL,     true);
        filterReadyBtn = buildFilterButton(FILTER_READY,   false);
        filterPartialBtn = buildFilterButton(FILTER_PARTIAL, false);

        filterAllBtn.addActionListener(e   -> applyFilter(FILTER_ALL));
        filterReadyBtn.addActionListener(e -> applyFilter(FILTER_READY));
        filterPartialBtn.addActionListener(e -> applyFilter(FILTER_PARTIAL));

        filterRow.add(filterAllBtn);
        filterRow.add(filterReadyBtn);
        filterRow.add(filterPartialBtn);

        return filterRow;
    }

    @Override
    protected JComponent buildTableContent() {
        cardsPanel = new JPanel();
        cardsPanel.setLayout(new BoxLayout(cardsPanel, BoxLayout.Y_AXIS));
        cardsPanel.setBackground(AppTheme.BG_PAGE);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { renderCards(getFilteredResults()); }
            public void removeUpdate(DocumentEvent e)  { renderCards(getFilteredResults()); }
            public void changedUpdate(DocumentEvent e) { renderCards(getFilteredResults()); }
        });

        JScrollPane scroll = new JScrollPane(cardsPanel);
        scroll.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        scroll.getViewport().setBackground(AppTheme.BG_PAGE);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        return scroll;
    }

    @Override
    protected JPanel buildToolbar() {
        JPanel empty = new JPanel();
        empty.setBackground(AppTheme.BG_PAGE);
        return empty;
    }

    @Override protected JButton createActionButton() { return AppTheme.dangerButton("Delete"); }
    @Override protected void onAddClicked()    {}
    @Override protected void onEditClicked()   {}
    @Override protected void onActionClicked() {}

    // --- Filter buttons ---

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
            case FILTER_READY   -> filterReadyBtn;
            case FILTER_PARTIAL -> filterPartialBtn;
            default             -> filterAllBtn;
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
        btn.setBackground(AppTheme.ACCENT_GOLD);
        btn.setForeground(AppTheme.TEXT_PRIMARY);
        btn.setBorderPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(7, 15, 7, 15));
    }

    // --- Data & rendering ---

    private void loadSuggestions() {
        if (statusLabel != null) statusLabel.setText("Calculating matches...");
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

    private List<RecipeMatchResult> getFilteredResults() {
        String query = searchField.getText().toLowerCase().trim();
        return allResults.stream()
            .filter(r -> query.isEmpty() || r.getRecipe().getTitle().toLowerCase().contains(query))
            .filter(r -> switch (activeFilter) {
                case FILTER_READY   -> r.isFullMatch();
                case FILTER_PARTIAL -> !r.isFullMatch() && r.getMatchPercent() > 0;
                default             -> true;
            })
            .toList();
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
            allGood.setForeground(AppTheme.GREEN_SUCCESS);
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
        if (pct == 100) return AppTheme.GREEN_SUCCESS;
        if (pct >= 50)  return AppTheme.ACCENT_GOLD;
        return AppTheme.TERRA_PRIMARY;
    }

    private void updateStatusLabel() {
        if (statusLabel == null) return;
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
            mealEntryDAO.addEntry(new MealEntry(recipe.getRecipeId(), frame.getCurrentUserId(),
                mealType, date, recipe.getTitle(), null));
            JOptionPane.showMessageDialog(this,
                "\"" + recipe.getTitle() + "\" assigned to " + mealType + " on " + date + ".",
                "Assigned", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            String sqlMsg = e.getMessage() != null ? e.getMessage() : "";
            if (sqlMsg.contains("Duplicate entry") || sqlMsg.toLowerCase().contains("duplicate")) {
                JOptionPane.showMessageDialog(this,
                    "Failed to assign: The selected date/meal slot is already filled.",
                    "Assign Failed", JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Failed to assign: " + sqlMsg,
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) loadSuggestions();
    }
}

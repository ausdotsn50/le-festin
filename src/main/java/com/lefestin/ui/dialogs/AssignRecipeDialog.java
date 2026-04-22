package com.lefestin.ui.dialogs;

import com.lefestin.dao.RecipeDAO;
import com.lefestin.model.Recipe;
import com.lefestin.ui.AppTheme;
import com.lefestin.ui.MainFrame;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * AssignRecipeDialog — recipe picker for a single meal slot.
 *
 * Called by WeeklyPlannerPanel.openAssignDialog(day, mealType).
 * After dialog closes, caller checks:
 *   if (dialog.getSelectedRecipeId() != -1) { ... }
 *
 * Layout:
 *   ┌─────────────────────────────────────────────┐
 *   │  Thursday, Apr 17 — Breakfast               │  ← header
 *   ├─────────────────────────────────────────────┤
 *   │  [Search recipes...........................] │  ← search bar
 *   │  Title              Category   Prep Time    │
 *   │  ─────────────────────────────────────────  │
 *   │  Classic Scrambled  Breakfast  10 min       │  ← table
 *   │  Garlic Fried Rice  Breakfast  15 min       │
 *   │  ...                                        │
 *   ├─────────────────────────────────────────────┤
 *   │               [Cancel]  [Select Recipe]     │  ← buttons
 *   └─────────────────────────────────────────────┘
 */
public class AssignRecipeDialog extends JDialog {

    // ── Dependencies ──────────────────────────────────────────────────────
    private final MainFrame frame;
    private final RecipeDAO recipeDAO;

    // ── Slot context ──────────────────────────────────────────────────────
    private final LocalDate date;
    private final String    mealType;

    // ── Result — read by WeeklyPlannerPanel after dialog closes ───────────
    private int    selectedRecipeId    = -1;
    private String selectedRecipeTitle = null;

    // ── Table ─────────────────────────────────────────────────────────────
    private JTable            table;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;

    // ── Search ────────────────────────────────────────────────────────────
    private JTextField searchField;

    // ── Buttons ───────────────────────────────────────────────────────────
    private JButton selectBtn;

    // ── Column indexes ────────────────────────────────────────────────────
    private static final int COL_ID       = 0; // hidden
    private static final int COL_TITLE    = 1;
    private static final int COL_CATEGORY = 2;
    private static final int COL_PREP     = 3;

    // ── Constructor ───────────────────────────────────────────────────────
    public AssignRecipeDialog(MainFrame frame,
                               LocalDate date,
                               String mealType) {
        super(frame,
            mealType + "  ·  "
                + date.format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
            true);

        this.frame     = frame;
        this.date      = date;
        this.mealType  = mealType;
        this.recipeDAO = new RecipeDAO();

        initComponents();
        loadRecipes();
        packAndCenter();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  INIT
    // ══════════════════════════════════════════════════════════════════════

    private void initComponents() {
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(AppTheme.BG_PAGE);

        add(buildHeader(),    BorderLayout.NORTH);
        add(buildTable(),     BorderLayout.CENTER);
        add(buildButtonBar(), BorderLayout.SOUTH);

        // Escape cancels
        getRootPane().registerKeyboardAction(
            e -> dispose(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW);

        // Enter selects if a row is highlighted
        getRootPane().registerKeyboardAction(
            e -> confirmSelection(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  HEADER
    // ══════════════════════════════════════════════════════════════════════

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.setBackground(AppTheme.BG_SURFACE);
        header.setBorder(BorderFactory.createCompoundBorder(
            AppTheme.BORDER_DIVIDER,
            BorderFactory.createEmptyBorder(14, 18, 14, 18)));

        // Instruction label
        JLabel instruction = AppTheme.subtitleLabel(
            "Choose a recipe to assign to this slot");
        header.add(instruction, BorderLayout.NORTH);

        // Search field
        searchField = new JTextField();
        searchField.setFont(AppTheme.FONT_BODY);
        searchField.setBackground(AppTheme.BG_SURFACE);
        searchField.setForeground(AppTheme.TEXT_PRIMARY);
        searchField.setBorder(AppTheme.BORDER_INPUT);
        searchField.putClientProperty(
            "JTextField.placeholderText", "Search recipes...");

        // Live filter on every keystroke
        searchField.getDocument().addDocumentListener(
            new javax.swing.event.DocumentListener() {
                public void insertUpdate(
                    javax.swing.event.DocumentEvent e) { applyFilter(); }
                public void removeUpdate(
                    javax.swing.event.DocumentEvent e) { applyFilter(); }
                public void changedUpdate(
                    javax.swing.event.DocumentEvent e) { applyFilter(); }
            });

        header.add(searchField, BorderLayout.SOUTH);
        return header;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  TABLE
    // ══════════════════════════════════════════════════════════════════════

    private JScrollPane buildTable() {
        tableModel = new DefaultTableModel(
            new String[]{"ID", "Title", "Category", "Prep Time"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
            @Override
            public Class<?> getColumnClass(int col) {
                return col == COL_ID ? Integer.class : String.class;
            }
        };

        table = new JTable(tableModel);
        AppTheme.styleTable(table);

        // Hide ID column
        table.getColumnModel().getColumn(COL_ID).setMinWidth(0);
        table.getColumnModel().getColumn(COL_ID).setMaxWidth(0);
        table.getColumnModel().getColumn(COL_ID).setWidth(0);

        // Column widths
        table.getColumnModel().getColumn(COL_TITLE)
            .setPreferredWidth(240);
        table.getColumnModel().getColumn(COL_CATEGORY)
            .setPreferredWidth(110);
        table.getColumnModel().getColumn(COL_PREP)
            .setPreferredWidth(90);

        // Row sorter — click header to sort
        sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        // Alternating rows
        table.setDefaultRenderer(Object.class,
            AppTheme.alternatingRowRenderer());

        // Enable Select button only when a row is selected
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectBtn.setEnabled(table.getSelectedRow() != -1);
            }
        });

        // Double-click confirms selection immediately
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2
                        && table.getSelectedRow() != -1) {
                    confirmSelection();
                }
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(AppTheme.BG_SURFACE);

        return scroll;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  BUTTON BAR
    // ══════════════════════════════════════════════════════════════════════

    private JPanel buildButtonBar() {
        JPanel bar = new JPanel(new FlowLayout(
            FlowLayout.RIGHT, 10, 0));
        bar.setBackground(AppTheme.BG_SURFACE);
        bar.setBorder(BorderFactory.createCompoundBorder(
            AppTheme.BORDER_DIVIDER_TOP,
            BorderFactory.createEmptyBorder(12, 18, 12, 18)));

        JButton cancelBtn = AppTheme.secondaryButton("Cancel");
        selectBtn         = AppTheme.primaryButton("Select Recipe");

        // Disabled until user picks a row
        selectBtn.setEnabled(false);

        cancelBtn.addActionListener(e -> dispose());
        selectBtn.addActionListener(e -> confirmSelection());

        bar.add(cancelBtn);
        bar.add(selectBtn);

        return bar;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  DATA
    // ══════════════════════════════════════════════════════════════════════

    private void loadRecipes() {
        tableModel.setRowCount(0);

        try {
            List<Recipe> recipes =
                recipeDAO.getAllRecipes(frame.getCurrentUserId());

            for (Recipe r : recipes) {
                tableModel.addRow(new Object[]{
                    r.getRecipeId(),
                    r.getTitle(),
                    r.getCategory(),
                    r.getFormattedPrepTime()
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Failed to load recipes: " + e.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Live search filter on title column ────────────────────────────────
    private void applyFilter() {
        String text = searchField.getText().trim();
        if (text.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(
                RowFilter.regexFilter("(?i)" + text, COL_TITLE));
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  CONFIRM SELECTION
    // ══════════════════════════════════════════════════════════════════════

    private void confirmSelection() {
        int viewRow = table.getSelectedRow();
        if (viewRow == -1) return;

        int modelRow = table.convertRowIndexToModel(viewRow);

        selectedRecipeId    = (int)    tableModel
            .getValueAt(modelRow, COL_ID);
        selectedRecipeTitle = (String) tableModel
            .getValueAt(modelRow, COL_TITLE);

        dispose();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PUBLIC API — read by WeeklyPlannerPanel after dialog closes
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Returns the selected recipe's ID, or -1 if the user cancelled.
     * WeeklyPlannerPanel checks: if (dialog.getSelectedRecipeId() != -1)
     */
    public int getSelectedRecipeId() {
        return selectedRecipeId;
    }

    /**
     * Returns the selected recipe's title for the MealEntry constructor,
     * or null if the user cancelled.
     */
    public String getSelectedRecipeTitle() {
        return selectedRecipeTitle;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════════════════

    private void packAndCenter() {
        setPreferredSize(new Dimension(500, 420));
        pack();
        setLocationRelativeTo(frame);
        setResizable(true);
    }
}
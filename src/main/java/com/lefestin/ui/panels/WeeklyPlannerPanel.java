package com.lefestin.ui.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import com.lefestin.dao.MealEntryDAO;
import com.lefestin.helper.Helper;
import com.lefestin.model.MealEntry;
import com.lefestin.ui.AppTheme;
import com.lefestin.ui.MainFrame;
import com.lefestin.ui.dialogs.AssignRecipeDialog;

/**
 * WeeklyPlannerPanel — 7-day meal planner grid.
 */
public class WeeklyPlannerPanel extends JPanel {
    private final MainFrame    frame;
    private final MealEntryDAO mealEntryDAO;

    // Week state 
    private LocalDate weekStart; // always a Monday

    // Grid cells: key = "yyyy-MM-dd|MealType", value = slot button
    private final Map<String, JButton> slotButtons = new HashMap<>();

    // Meal entries loaded for the current week 
    // key = "yyyy-MM-dd|MealType", value = MealEntry (null = empty slot)
    private final Map<String, MealEntry> weekEntries = new HashMap<>();

    // Header labels updated when week changes
    private JLabel weekRangeLabel;
    private JPanel gridPanel;

    // Formatters
    private static final DateTimeFormatter DAY_NAME_FMT =
        DateTimeFormatter.ofPattern("EEE");       // "Mon"
    private static final DateTimeFormatter DAY_DATE_FMT =
        DateTimeFormatter.ofPattern("MMM d");     // "Apr 14"
    private static final DateTimeFormatter WEEK_RANGE_FMT =
        DateTimeFormatter.ofPattern("MMM d");     // for header range

    // Constant colors
    private static final Color COL_DAY_BG = AppTheme.BG_SUBTLE;
    private static final Color COL_DAY_TODAY = AppTheme.AMBER_TINT;
    private static final Color COL_SLOT_EMPTY  = AppTheme.BG_SURFACE;
    private static final Color COL_SLOT_FILLED = AppTheme.GREEN_TINT;
    private static final Color COL_SLOT_BORDER = AppTheme.BG_BORDER;
    // private static final Color COL_MEAL_LABEL  = AppTheme.TEXT_MUTED;
    private static final Color COL_RECIPE_TEXT = AppTheme.GREEN_TINT_TEXT;

    public WeeklyPlannerPanel(MainFrame frame) {
        this.frame = frame;
        this.mealEntryDAO = new MealEntryDAO();
        this.weekStart = Helper.getMonday(LocalDate.now());

        setLayout(new BorderLayout(0, 0));
        setBackground(AppTheme.BG_PAGE);

        add(buildNavBar(), BorderLayout.NORTH);
        add(buildGrid(),   BorderLayout.CENTER);
    }

    //  NAV BAR — week range + prev/next + auto-generate + clear
    private JPanel buildNavBar() {
        JPanel nav = new JPanel(new BorderLayout());
        nav.setBackground(AppTheme.BG_SURFACE);
        nav.setBorder(BorderFactory.createCompoundBorder(
            AppTheme.BORDER_DIVIDER,
            BorderFactory.createEmptyBorder(12, 20, 12, 20)
        ));

        // Top row: Prev | week range label | Next
        JPanel topRow = new JPanel(new BorderLayout(12, 0));
        topRow.setBackground(AppTheme.BG_SURFACE);

        JButton prevBtn  = AppTheme.ghostButton("◀  Prev week");
        JButton nextBtn  = AppTheme.ghostButton("Next week  ▶");

        weekRangeLabel = new JLabel("", SwingConstants.CENTER);
        weekRangeLabel.setFont(AppTheme.FONT_HEADING);
        weekRangeLabel.setForeground(AppTheme.TEXT_PRIMARY);
        updateWeekRangeLabel();

        prevBtn.addActionListener(e -> shiftWeek(-1));
        nextBtn.addActionListener(e -> shiftWeek(+1));

        topRow.add(prevBtn,        BorderLayout.WEST);
        topRow.add(weekRangeLabel, BorderLayout.CENTER);
        topRow.add(nextBtn,        BorderLayout.EAST);

        // Bottom row: Auto-generate | spacer | Clear week
        JPanel bottomRow = new JPanel(new BorderLayout());
        bottomRow.setBackground(AppTheme.BG_SURFACE);
        bottomRow.setBorder(
            BorderFactory.createEmptyBorder(8, 0, 0, 0));

        JButton autoBtn  = AppTheme.primaryButton("Auto-Generate Week");
        JButton clearBtn = AppTheme.dangerButton("Clear Week");

        autoBtn.addActionListener( e -> autoGenerateWeek());
        clearBtn.addActionListener(e -> clearWeek());

        bottomRow.add(autoBtn,  BorderLayout.WEST);
        bottomRow.add(clearBtn, BorderLayout.EAST);

        nav.add(topRow,    BorderLayout.NORTH);
        nav.add(bottomRow, BorderLayout.SOUTH);

        return nav;
    }

    //  GRID — 7 columns (days) × 4 rows (header + 3 meal slots)
    private JPanel buildGrid() {
        gridPanel = new JPanel(new GridLayout(4, 7, 1, 1));
        gridPanel.setBackground(new Color(200, 200, 200)); // gap color
        gridPanel.setBorder(BorderFactory.createLineBorder(
            new Color(200, 200, 200), 1));

        rebuildGridCells();

        JScrollPane scroll = new JScrollPane(gridPanel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getHorizontalScrollBarPolicy() ;
        scroll.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(AppTheme.BG_PAGE);
        wrapper.setBorder(
            BorderFactory.createEmptyBorder(16, 16, 16, 16));
        wrapper.add(scroll, BorderLayout.CENTER);
        return wrapper;
    }

    /**
     * Clears and rebuilds all 28 cells (7 day headers + 21 meal slots).
     * Called on first load and whenever the week changes.
     */
    private void rebuildGridCells() {
        gridPanel.removeAll();
        slotButtons.clear();

        // Row 0: day headers
        for (int d = 0; d < 7; d++) {
            LocalDate day      = weekStart.plusDays(d);
            boolean   isToday  = day.equals(LocalDate.now());
            gridPanel.add(buildDayHeader(day, isToday));
        }

        // Rows 1–3: one row per meal type
        for (String mealType : MealEntry.MEAL_TYPES) {
            for (int d = 0; d < 7; d++) {
                LocalDate day = weekStart.plusDays(d);
                JButton   slot = buildSlotButton(day, mealType);
                slotButtons.put(Helper.slotKey(day, mealType), slot);
                gridPanel.add(slot);
            }
        }

        gridPanel.revalidate();
        gridPanel.repaint();
    }

    // Day column header
    private JPanel buildDayHeader(LocalDate day, boolean isToday) {
        JPanel cell = new JPanel();
        cell.setLayout(new BoxLayout(cell, BoxLayout.Y_AXIS));
        cell.setBackground(isToday ? COL_DAY_TODAY : COL_DAY_BG);
        cell.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));

        JLabel dayName = new JLabel(
            day.format(DAY_NAME_FMT).toUpperCase());
        dayName.setFont(new Font("SansSerif", Font.BOLD, 11));
        dayName.setForeground(isToday
            ? AppTheme.AMBER_PRIMARY
            : AppTheme.TEXT_MUTED);
        dayName.setFont(AppTheme.FONT_TINY);

        JLabel dayDate = new JLabel(day.format(DAY_DATE_FMT));
        dayDate.setFont(new Font("SansSerif", Font.PLAIN, 13));
        dayDate.setForeground(isToday
            ? AppTheme.AMBER_PRIMARY
            : AppTheme.TEXT_PRIMARY);
        dayDate.setFont(AppTheme.FONT_BODY);

        // "Today" badge
        if (isToday) {
            JLabel badge = new JLabel("Today");
            badge.setFont(AppTheme.FONT_TINY);
            badge.setForeground(AppTheme.AMBER_PRIMARY);
            badge.setAlignmentX(Component.CENTER_ALIGNMENT);
            cell.add(dayName);
            cell.add(Box.createVerticalStrut(2));
            cell.add(dayDate);
            cell.add(Box.createVerticalStrut(2));
            cell.add(badge);
        } else {
            cell.add(dayName);
            cell.add(Box.createVerticalStrut(4));
            cell.add(dayDate);
        }

        return cell;
    }

    // Individual meal slot button
    private JButton buildSlotButton(LocalDate day, String mealType) {
        JButton btn = new JButton();
        btn.setLayout(new BorderLayout(0, 2));
        btn.setBackground(COL_SLOT_EMPTY);
        btn.setOpaque(true);
        btn.setBorderPainted(true);
        btn.setBorder(BorderFactory.createLineBorder(
            COL_SLOT_BORDER, 1));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(120, 80));

        // Recipe name label (center)
        JLabel recipeLabel = new JLabel("", SwingConstants.CENTER);
        recipeLabel.setFont(AppTheme.FONT_SMALL);
        recipeLabel.setForeground(AppTheme.TEXT_MUTED);
        recipeLabel.setBorder(BorderFactory.createEmptyBorder(0, 6, 6, 6));

        btn.add(recipeLabel, BorderLayout.CENTER);

        // Hover highlight
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(new Color(240, 248, 255));
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                weekEntries.get(Helper.slotKey(day, mealType));
                btn.setBackground(AppTheme.SELECTION_BG);
            }
        });

        btn.addActionListener(
            e -> openSlotDialog(day, mealType, btn));

        return btn;
    }

    //  DATA — load and render
    public void loadWeek() {
        weekEntries.clear();

        try {
            LocalDate weekEnd = weekStart.plusDays(6);
            List<MealEntry> entries = mealEntryDAO.getEntriesByWeek(
                frame.getCurrentUserId(), weekStart, weekEnd);

            for (MealEntry entry : entries) {
                weekEntries.put(
                    Helper.slotKey(entry.getScheduledDate(),
                            entry.getMealType()),
                    entry);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Failed to load meal plan: " + e.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE);
        }

        renderSlots();
        updateWeekRangeLabel();
    }

    /**
     * Updates every slot button's appearance to match weekEntries.
     * Called after loadWeek() and after any single slot change.
     */
    private void renderSlots() {
        for (String mealType : MealEntry.MEAL_TYPES) {
            for (int d = 0; d < 7; d++) {
                LocalDate day   = weekStart.plusDays(d);
                String    key   = Helper.slotKey(day, mealType);
                JButton   btn   = slotButtons.get(key);
                MealEntry entry = weekEntries.get(key);

                if (btn == null) continue;

                // The recipe label is CENTER component
                JLabel recipeLabel = (JLabel) ((BorderLayout)
                    btn.getLayout()).getLayoutComponent(
                        BorderLayout.CENTER);

                if (entry != null && entry.getRecipeTitle() != null) {
                    // Filled slot — green tint, recipe name
                    btn.setBackground(COL_SLOT_FILLED);
                    recipeLabel.setText(
                        "<html><center>"
                        + wrapText(entry.getRecipeTitle(), 14)
                        + "</center></html>");
                    recipeLabel.setForeground(COL_RECIPE_TEXT);
                } else {
                    // Empty slot
                    btn.setBackground(COL_SLOT_EMPTY);
                    recipeLabel.setText("+ assign");
                    recipeLabel.setForeground(AppTheme.TEXT_MUTED);
                }
            }
        }
    }

    //  SLOT DIALOG — assign or clear a single slot
    private void openSlotDialog(LocalDate day,
                                String mealType, JButton btn) {
        String    key         = Helper.slotKey(day, mealType);
        MealEntry existing    = weekEntries.get(key);
        boolean   isOccupied  = existing != null;

        // Build options depending on whether slot is filled or empty
        String[] options = isOccupied
            ? new String[]{ "Change Recipe", "Clear Slot", "Cancel" }
            : new String[]{ "Assign Recipe", "Cancel" };

        String prompt = day.format(
            DateTimeFormatter.ofPattern("EEEE, MMM d"))
            + (isOccupied
                ? "\nCurrent: " + existing.getRecipeTitle()
                : "\nNo recipe assigned");

        int choice = JOptionPane.showOptionDialog(
            this, prompt,
            day.format(DAY_DATE_FMT),
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.PLAIN_MESSAGE,
            null, options, options[0]);

        if (choice == JOptionPane.CLOSED_OPTION) return;

        if (isOccupied) {
            if (choice == 0) openAssignDialog(day, mealType);
            else if (choice == 1) clearSlot(day, mealType);
        } else {
            if (choice == 0) openAssignDialog(day, mealType);
        }
    }

    private void openAssignDialog(LocalDate day, String mealType) {
        AssignRecipeDialog dialog =
            new AssignRecipeDialog(frame, day, mealType);
        dialog.setVisible(true);

        if (dialog.getSelectedRecipeId() != -1) {
            try {
                // Remove existing entry for this slot if any
                String key = Helper.slotKey(day, mealType);
                if (weekEntries.containsKey(key)) {
                    mealEntryDAO.deleteEntry(
                        frame.getCurrentUserId(), day, mealType);
                }

                MealEntry newEntry = new MealEntry(
                    dialog.getSelectedRecipeId(),
                    frame.getCurrentUserId(),
                    mealType, day,
                    dialog.getSelectedRecipeTitle(), null);

                mealEntryDAO.addEntry(newEntry);
                weekEntries.put(key, newEntry);
                renderSlots();

            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this,
                    "Failed to assign recipe: " + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void clearSlot(LocalDate day, String mealType) {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Clear " + mealType + " on "
                + day.format(DateTimeFormatter.ofPattern("MMM d")) + "?",
            "Clear Slot",
            JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                mealEntryDAO.deleteEntry(
                    frame.getCurrentUserId(), day, mealType);
                weekEntries.remove(Helper.slotKey(day, mealType));
                renderSlots();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this,
                    "Failed to clear slot: " + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    //  WEEK ACTIONS
    private void shiftWeek(int weeks) {
        weekStart = weekStart.plusWeeks(weeks);
        rebuildGridCells();
        loadWeek();
    }

    private void autoGenerateWeek() {
        int confirm = JOptionPane.showConfirmDialog(this, """
                                                          Auto-fill empty slots for this week?
                                                          Existing assignments will not be changed.""",
            "Auto-Generate",
            JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            JOptionPane.showMessageDialog(this,
                "Auto-generate coming in Week 3.",
                "Coming Soon",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void clearWeek() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Clear ALL meals for this week?\nThis cannot be undone.",
            "Clear Week",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                mealEntryDAO.clearWeek(
                    frame.getCurrentUserId(),
                    weekStart,
                    weekStart.plusDays(6));
                weekEntries.clear();
                renderSlots();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this,
                    "Failed to clear week: " + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    //  HELPER FUNCS
    // Consistent map key: "2026-04-17|Breakfast"
    private void updateWeekRangeLabel() {
        LocalDate end = weekStart.plusDays(6);
        weekRangeLabel.setText(
            weekStart.format(WEEK_RANGE_FMT)
            + " – "
            + end.format(WEEK_RANGE_FMT)
            + ", "
            + weekStart.getYear());
    }

    // Wraps long recipe titles at word boundaries for the slot label
    private String wrapText(String text, int maxLen) {
        if (text == null || text.length() <= maxLen) return text;
        int    cut  = text.lastIndexOf(' ', maxLen);
        if (cut == -1) cut = maxLen;
        return text.substring(0, cut) + "<br>"
             + text.substring(cut).trim();
    }

    // Reload when panel becomes visible
    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) loadWeek();
    }
}
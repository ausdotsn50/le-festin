package com.lefestin.ui.panels;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.*;
import javax.swing.border.CompoundBorder;

import com.lefestin.dao.*;
import com.lefestin.helper.Helper;
import com.lefestin.model.*;
import com.lefestin.service.CsvExportService;
import com.lefestin.ui.AppTheme;
import com.lefestin.ui.MainFrame;
import com.lefestin.ui.dialogs.AssignRecipeDialog;

/**
 * WeeklyPlannerPanel — 7-day meal planner grid.
 */
public class WeeklyPlannerPanel extends JPanel {
    private final MainFrame frame;
    private final MealEntryDAO mealEntryDAO = new MealEntryDAO();
    private final RecipeDAO recipeDAO = new RecipeDAO();
    private final CsvExportService csvService = new CsvExportService();

    private LocalDate weekStart;
    private final Map<String, JButton> slotButtons = new HashMap<>();
    private final Map<String, MealEntry> weekEntries = new HashMap<>();

    private JLabel weekRangeLabel;
    private JPanel gridPanel;
    private JButton exportBtn;

    // Formatters
    private static final DateTimeFormatter DAY_NAME_FMT = DateTimeFormatter.ofPattern("EEE");
    private static final DateTimeFormatter DAY_DATE_FMT = DateTimeFormatter.ofPattern("MMM d");
    private static final DateTimeFormatter WEEK_RANGE_FMT = DateTimeFormatter.ofPattern("MMM d");

    // Colors
    private static final Color COL_DAY_TODAY = AppTheme.AMBER_TINT;
    private static final Color COL_SLOT_EMPTY = AppTheme.BG_SURFACE;
    private static final Color COL_SLOT_FILLED = AppTheme.GREEN_TINT;
    private static final Color COL_RECIPE_TEXT = AppTheme.GREEN_TINT_TEXT;
    private static final DataFlavor SLOT_KEY_FLAVOR = DataFlavor.stringFlavor;

    public WeeklyPlannerPanel(MainFrame frame) {
        this.frame = frame;
        this.weekStart = Helper.getMonday(LocalDate.now());

        setLayout(new BorderLayout());
        setBackground(AppTheme.BG_PAGE);

        add(buildNavBar(), BorderLayout.NORTH);
        add(buildGridWrapper(), BorderLayout.CENTER);
    }

    // --- UI Builders ---

    private JPanel buildNavBar() {
        JPanel nav = new JPanel(new BorderLayout());
        nav.setBackground(AppTheme.BG_SURFACE);
        nav.setBorder(new CompoundBorder(AppTheme.BORDER_DIVIDER, BorderFactory.createEmptyBorder(12, 20, 12, 20)));

        // Navigation Row
        JPanel topRow = new JPanel(new BorderLayout(12, 0));
        topRow.setOpaque(false);

        JButton prevBtn = AppTheme.ghostButton("◀  Prev week");
        JButton nextBtn = AppTheme.ghostButton("Next week  ▶");
        weekRangeLabel = new JLabel("", SwingConstants.CENTER);
        weekRangeLabel.setFont(AppTheme.FONT_HEADING);
        weekRangeLabel.setForeground(AppTheme.TEXT_PRIMARY);

        prevBtn.addActionListener(e -> shiftWeek(-1));
        nextBtn.addActionListener(e -> shiftWeek(+1));

        topRow.add(prevBtn, BorderLayout.WEST);
        topRow.add(weekRangeLabel, BorderLayout.CENTER);
        topRow.add(nextBtn, BorderLayout.EAST);

        // Actions Row
        JPanel bottomRow = new JPanel(new BorderLayout());
        bottomRow.setOpaque(false);
        bottomRow.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        JButton autoBtn = AppTheme.primaryButton("Auto-Generate Week");
        autoBtn.addActionListener(e -> autoGenerateWeek());

        JPanel rightGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightGroup.setOpaque(false);
        
        exportBtn = AppTheme.secondaryButton("Export CSV");
        exportBtn.setEnabled(false);
        exportBtn.addActionListener(e -> exportMealPlan());

        JButton clearBtn = AppTheme.dangerButton("Clear Week");
        clearBtn.addActionListener(e -> clearWeek());

        rightGroup.add(exportBtn);
        rightGroup.add(clearBtn);

        bottomRow.add(autoBtn, BorderLayout.WEST);
        bottomRow.add(rightGroup, BorderLayout.EAST);

        nav.add(topRow, BorderLayout.NORTH);
        nav.add(bottomRow, BorderLayout.SOUTH);

        updateWeekRangeLabel();
        return nav;
    }

    private JPanel buildGridWrapper() {
        gridPanel = new JPanel(new GridLayout(4, 7, 1, 1));
        gridPanel.setBackground(new Color(200, 200, 200));
        gridPanel.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));

        rebuildGridCells();

        JScrollPane scroll = new JScrollPane(gridPanel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(AppTheme.BG_PAGE);
        wrapper.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        wrapper.add(scroll, BorderLayout.CENTER);
        return wrapper;
    }

    private void rebuildGridCells() {
        gridPanel.removeAll();
        slotButtons.clear();

        // Row 0: Headers
        for (int d = 0; d < 7; d++) {
            LocalDate day = weekStart.plusDays(d);
            gridPanel.add(buildDayHeader(day, day.equals(LocalDate.now())));
        }

        // Rows 1-3: Meal Slots
        for (String mealType : MealEntry.MEAL_TYPES) {
            for (int d = 0; d < 7; d++) {
                LocalDate day = weekStart.plusDays(d);
                JButton slot = buildSlotButton(day, mealType);
                slotButtons.put(Helper.slotKey(day, mealType), slot);
                gridPanel.add(slot);
            }
        }
        gridPanel.revalidate();
        gridPanel.repaint();
    }

    private JPanel buildDayHeader(LocalDate day, boolean isToday) {
        JPanel cell = new JPanel();
        cell.setLayout(new BoxLayout(cell, BoxLayout.Y_AXIS));
        cell.setBackground(isToday ? COL_DAY_TODAY : AppTheme.BG_SUBTLE);
        cell.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));

        JLabel dayName = new JLabel(day.format(DAY_NAME_FMT).toUpperCase());
        dayName.setFont(AppTheme.FONT_TINY);
        dayName.setForeground(isToday ? AppTheme.AMBER_PRIMARY : AppTheme.TEXT_MUTED);

        JLabel dayDate = new JLabel(day.format(DAY_DATE_FMT));
        dayDate.setFont(AppTheme.FONT_BODY);
        dayDate.setForeground(isToday ? AppTheme.AMBER_PRIMARY : AppTheme.TEXT_PRIMARY);

        cell.add(dayName);
        cell.add(Box.createVerticalStrut(isToday ? 2 : 4));
        cell.add(dayDate);

        if (isToday) {
            JLabel badge = new JLabel("Today");
            badge.setFont(AppTheme.FONT_TINY);
            badge.setForeground(AppTheme.AMBER_PRIMARY);
            badge.setAlignmentX(CENTER_ALIGNMENT);
            cell.add(Box.createVerticalStrut(2));
            cell.add(badge);
        }

        return cell;
    }

    private JButton buildSlotButton(LocalDate day, String mealType) {

        JButton btn = new JButton(); 
        btn.setLayout(new BorderLayout(0, 2)); 
        btn.setBackground(COL_SLOT_EMPTY);
        btn.setOpaque(true);
        btn.setBorder(BorderFactory.createLineBorder(AppTheme.BG_BORDER, 1));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(120, 80));
        btn.setToolTipText("Click to edit, drag to move or swap");
        
        String key = Helper.slotKey(day, mealType);
        btn.putClientProperty("slotKey", key);
        btn.setTransferHandler(new SlotTransferHandler());
        installDragSupport(btn);

        JLabel recipeLabel = new JLabel("", SwingConstants.CENTER);
        recipeLabel.setFont(AppTheme.FONT_SMALL);
        btn.add(recipeLabel, BorderLayout.CENTER);

        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setBackground(AppTheme.SELECTION_BG); }
            @Override public void mouseExited(MouseEvent e) { renderSingleSlot(key); }
        });

        btn.addActionListener(e -> openSlotDialog(day, mealType));
        return btn;
    }

    // --- Logic & Data ---

    public void loadWeek() {
        weekEntries.clear();
        try {
            List<MealEntry> entries = mealEntryDAO.getEntriesByWeek(frame.getCurrentUserId(), weekStart, weekStart.plusDays(6));
            entries.forEach(e -> weekEntries.put(Helper.slotKey(e.getScheduledDate(), e.getMealType()), e));
        } catch (SQLException e) {
            showError("Failed to load meal plan", e);
        }

        renderSlots();
        updateWeekRangeLabel();
        if (exportBtn != null) exportBtn.setEnabled(weekEntries.values().stream().anyMatch(Objects::nonNull));
    }

    private void renderSlots() {
        slotButtons.keySet().forEach(this::renderSingleSlot);
    }

    private void renderSingleSlot(String key) {
        JButton btn = slotButtons.get(key);
        if (btn == null) return;

        MealEntry entry = weekEntries.get(key);
        JLabel recipeLabel = (JLabel) ((BorderLayout) btn.getLayout()).getLayoutComponent(BorderLayout.CENTER);

        if (entry != null && entry.getRecipeTitle() != null) {
            btn.setBackground(COL_SLOT_FILLED);
            recipeLabel.setText("<html><center>" + wrapText(entry.getRecipeTitle(), 14) + "</center></html>");
            recipeLabel.setForeground(COL_RECIPE_TEXT);
        } else {
            btn.setBackground(COL_SLOT_EMPTY);
            recipeLabel.setText("+ assign");
            recipeLabel.setForeground(AppTheme.TEXT_MUTED);
        }
    }

    private void openSlotDialog(LocalDate day, String mealType) {
        MealEntry existing = weekEntries.get(Helper.slotKey(day, mealType));
        boolean occupied = existing != null;

        String[] options = occupied ? new String[]{"Change Recipe", "Clear Slot", "Cancel"} : new String[]{"Assign Recipe", "Cancel"};
        String current = occupied ? existing.getRecipeTitle() : "No recipe assigned";
        String prompt = day.format(DateTimeFormatter.ofPattern("EEEE, MMM d")) + "\nCurrent: " + current;

        int choice = JOptionPane.showOptionDialog(this, prompt, day.format(DAY_DATE_FMT),
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);

        if (choice == 0) openAssignDialog(day, mealType);
        else if (occupied && choice == 1) clearSlot(day, mealType);
    }

    private void openAssignDialog(LocalDate day, String mealType) {
        AssignRecipeDialog dialog = new AssignRecipeDialog(frame, day, mealType);
        dialog.setVisible(true);

        if (dialog.getSelectedRecipeId() != -1) {
            try {
                mealEntryDAO.deleteEntry(frame.getCurrentUserId(), day, mealType);
                MealEntry newEntry = new MealEntry(dialog.getSelectedRecipeId(), frame.getCurrentUserId(), 
                        mealType, day, dialog.getSelectedRecipeTitle(), null);

                mealEntryDAO.addEntry(newEntry);
                weekEntries.put(Helper.slotKey(day, mealType), newEntry);
                renderSlots();
            } catch (SQLException e) {
                showError("Failed to assign recipe", e);
            }
        }
    }

    private void clearSlot(LocalDate day, String mealType) {
        String msg = "Clear " + mealType + " on " + day.format(DAY_DATE_FMT) + "?";
        if (JOptionPane.showConfirmDialog(this, msg, "Clear Slot", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            try {
                mealEntryDAO.deleteEntry(frame.getCurrentUserId(), day, mealType);
                weekEntries.remove(Helper.slotKey(day, mealType));
                renderSlots();
            } catch (SQLException e) {
                showError("Failed to clear slot", e);
            }
        }
    }

    private void clearWeek() {
        String msg = "Clear ALL meals for this week?\nThis cannot be undone.";
        if (JOptionPane.showConfirmDialog(this, msg, "Clear Week", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
            try {
                mealEntryDAO.clearWeek(frame.getCurrentUserId(), weekStart, weekStart.plusDays(6));
                weekEntries.clear();
                renderSlots();
            } catch (SQLException e) {
                showError("Failed to clear week", e);
            }
        }
    }

    private void autoGenerateWeek() {
        String msg = "Auto-fill empty slots for this week?\nExisting assignments will not be changed.";
        if (JOptionPane.showConfirmDialog(this, msg, "Auto-Generate", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;

        try {
            int created = autoFillEmptyWeekSlots();
            renderSlots();
            String resultMsg = created == 0 ? "No empty slots to fill for this week." : "Added " + created + " meal" + (created == 1 ? "" : "s") + " to this week.";
            JOptionPane.showMessageDialog(this, resultMsg, "Auto-Generate", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            showError("Auto-generate failed", e);
        }
    }

    private int autoFillEmptyWeekSlots() throws SQLException {
        List<Recipe> recipes = recipeDAO.getAllRecipes(frame.getCurrentUserId());
        if (recipes.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Add at least one recipe before auto-generating a week.", "No Recipes", JOptionPane.INFORMATION_MESSAGE);
            return 0;
        }

        Map<Integer, Integer> recipeUseCount = new HashMap<>();
        weekEntries.values().stream().filter(Objects::nonNull).forEach(e -> recipeUseCount.merge(e.getRecipeId(), 1, Integer::sum));

        int created = 0;
        for (int d = 0; d < 7; d++) {
            LocalDate day = weekStart.plusDays(d);
            for (String mealType : MealEntry.MEAL_TYPES) {
                String key = Helper.slotKey(day, mealType);
                if (weekEntries.get(key) != null) continue;

                Recipe pick = pickRecipeForSlot(recipes, day, mealType, recipeUseCount);
                if (pick != null) {
                    MealEntry newEntry = new MealEntry(pick.getRecipeId(), frame.getCurrentUserId(), mealType, day, pick.getTitle(), pick.getCategory());
                    mealEntryDAO.addEntry(newEntry);
                    weekEntries.put(key, newEntry);
                    recipeUseCount.merge(pick.getRecipeId(), 1, Integer::sum);
                    created++;
                }
            }
        }
        return created;
    }

    private Recipe pickRecipeForSlot(List<Recipe> recipes, LocalDate day, String mealType, Map<Integer, Integer> usage) {
        Set<Integer> usedToday = weekEntries.values().stream()
            .filter(e -> e != null && day.equals(e.getScheduledDate()))
            .map(MealEntry::getRecipeId).collect(Collectors.toSet());

        List<Recipe> preferred = recipes.stream().filter(r -> mealType.equalsIgnoreCase(r.getCategory())).toList();
        Recipe chosen = chooseLeastUsed(preferred, usedToday, day, mealType, usage);
        return (chosen != null) ? chosen : chooseLeastUsed(recipes, usedToday, day, mealType, usage);
    }

    private Recipe chooseLeastUsed(List<Recipe> pool, Set<Integer> usedToday, LocalDate day, String mealType, Map<Integer, Integer> usage) {
        if (pool == null || pool.isEmpty()) return null;

        List<Recipe> allowed = pool.stream().filter(r -> !usedToday.contains(r.getRecipeId())).toList();
        if (allowed.isEmpty()) allowed = pool;

        int min = allowed.stream().mapToInt(r -> usage.getOrDefault(r.getRecipeId(), 0)).min().orElse(0);
        List<Recipe> leastUsed = allowed.stream().filter(r -> usage.getOrDefault(r.getRecipeId(), 0) == min)
                .sorted(Comparator.comparing(Recipe::getTitle, String.CASE_INSENSITIVE_ORDER)).toList();

        int idx = Math.floorMod((int) day.toEpochDay() + mealType.hashCode(), leastUsed.size());
        return leastUsed.get(idx);
    }

    // --- Helpers ---

    private void exportMealPlan() {
        LocalDate to = weekStart.plusDays(6);
        String name = String.format("meal_plan_%s_to_%s.csv", weekStart, to);

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(name));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".csv")) file = new File(file.getAbsolutePath() + ".csv");
            
            CsvExportService.ExportResult res = csvService.exportMealPlan(frame.getCurrentUserId(), weekStart, to, file);
            JOptionPane.showMessageDialog(this, res.getMessage(), "Export Status", 
                    res.isSuccess() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
        }
    }

    private void shiftWeek(int weeks) {
        weekStart = weekStart.plusWeeks(weeks);
        rebuildGridCells();
        loadWeek();
    }

    private void updateWeekRangeLabel() {
        weekRangeLabel.setText(weekStart.format(WEEK_RANGE_FMT) + " – " + weekStart.plusDays(6).format(WEEK_RANGE_FMT) + ", " + weekStart.getYear());
    }

    private String wrapText(String text, int max) {
        if (text == null || text.length() <= max) return text;
        int cut = text.lastIndexOf(' ', max);
        if (cut == -1) cut = max;
        return text.substring(0, cut) + "<br>" + text.substring(cut).trim();
    }

    private void showError(String msg, Exception e) {
        JOptionPane.showMessageDialog(this, msg + ": " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void installDragSupport(JButton btn) {
        MouseAdapter ml = new MouseAdapter() {
            private boolean active;
            @Override public void mousePressed(MouseEvent e) { active = false; }
            @Override public void mouseDragged(MouseEvent e) {
                if (!active && btn.getTransferHandler() != null) {
                    active = true;
                    btn.getTransferHandler().exportAsDrag(btn, e, TransferHandler.MOVE);
                }
            }
        };
        btn.addMouseListener(ml);
        btn.addMouseMotionListener(ml);
    }

    private class SlotTransferHandler extends TransferHandler {
        @Override protected Transferable createTransferable(JComponent c) {
            return new StringSelection(String.valueOf(c.getClientProperty("slotKey")));
        }
        @Override public int getSourceActions(JComponent c) { return MOVE; }
        @Override public boolean canImport(TransferSupport s) { return s.isDrop() && s.isDataFlavorSupported(SLOT_KEY_FLAVOR); }
        @Override public boolean importData(TransferSupport s) {
            try {
                String targetKey = String.valueOf(((JComponent) s.getComponent()).getClientProperty("slotKey"));
                String sourceKey = (String) s.getTransferable().getTransferData(SLOT_KEY_FLAVOR);
                if (sourceKey.equals(targetKey)) return false;

                MealEntry src = weekEntries.get(sourceKey);
                MealEntry tgt = weekEntries.get(targetKey);
                if (src == null) return false;

                String[] p = targetKey.split("\\|", 2);
                LocalDate d = LocalDate.parse(p[0]);
                if (tgt == null) mealEntryDAO.moveEntryToSlot(src, d, p[1]);
                else mealEntryDAO.swapEntries(src, tgt);

                loadWeek();
                return true;
            } catch (Exception e) {
                showError("Drag and Drop Error", e);
                return false;
            }
        }
    }

    @Override public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) loadWeek();
    }
}
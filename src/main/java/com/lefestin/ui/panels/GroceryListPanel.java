package com.lefestin.ui.panels;

import java.awt.*;
import java.awt.print.PrinterException;
import java.io.File;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import com.lefestin.model.RecipeIngredient;
import com.lefestin.service.CsvExportService;
import com.lefestin.service.GroceryListService;
import com.lefestin.ui.AppTheme;
import com.lefestin.ui.MainFrame;

/**
 * GroceryListPanel — Refactored read-only view for missing ingredients.
 */
public class GroceryListPanel extends JPanel {

    private final MainFrame frame;
    private final GroceryListService groceryService = new GroceryListService();
    private final CsvExportService csvService = new CsvExportService();

    private JSpinner fromSpinner, toSpinner;
    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel summaryLabel;
    private JButton exportBtn, printBtn;

    // Table Configuration
    private static final int COL_NAME = 0;
    private static final int COL_QUANTITY = 1;
    private static final int COL_UNIT = 2;
    private static final String DATE_FORMAT = "MMM d, yyyy";

    public GroceryListPanel(MainFrame frame) {
        this.frame = frame;
        setupMainLayout();
        initComponents();
    }

    private void setupMainLayout() {
        setLayout(new BorderLayout());
        setBackground(AppTheme.BG_PAGE);
    }

    private void initComponents() {
        add(buildHeader(), BorderLayout.NORTH);
        add(buildTableArea(), BorderLayout.CENTER);
    }

    // --- UI Assembly ---

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(AppTheme.BG_SURFACE);
        header.setBorder(AppTheme.BORDER_DIVIDER);

        header.add(buildTitleRow(), BorderLayout.NORTH);
        header.add(buildControlRow(), BorderLayout.SOUTH);
        return header;
    }

    private JPanel buildTitleRow() {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(AppTheme.BG_SURFACE);
        row.setBorder(BorderFactory.createEmptyBorder(16, 20, 10, 20));

        JPanel titleStack = new JPanel();
        titleStack.setLayout(new BoxLayout(titleStack, BoxLayout.Y_AXIS));
        titleStack.setBackground(AppTheme.BG_SURFACE);

        JLabel title = AppTheme.titleLabel("Grocery List");
        title.setAlignmentX(LEFT_ALIGNMENT);

        summaryLabel = AppTheme.subtitleLabel("Select a date range and click Generate List");
        summaryLabel.setAlignmentX(LEFT_ALIGNMENT);

        titleStack.add(title);
        titleStack.add(Box.createVerticalStrut(2));
        titleStack.add(summaryLabel);

        row.add(titleStack, BorderLayout.WEST);
        return row;
    }

    private JPanel buildControlRow() {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setBackground(AppTheme.BG_SURFACE);
        row.setBorder(new javax.swing.border.CompoundBorder(
                AppTheme.BORDER_DIVIDER,
                BorderFactory.createEmptyBorder(10, 20, 12, 20)));

        // Left Actions
        JPanel leftGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftGroup.setOpaque(false);

        LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        fromSpinner = buildDateSpinner(monday);
        toSpinner = buildDateSpinner(monday.plusDays(6));

        JButton generateBtn = AppTheme.primaryButton("Generate List");
        generateBtn.addActionListener(e -> executeGeneration());

        leftGroup.add(new JLabel("From") {{ setFont(AppTheme.FONT_LABEL); setForeground(AppTheme.TEXT_SECONDARY); }});
        leftGroup.add(fromSpinner);
        leftGroup.add(new JLabel("To") {{ setFont(AppTheme.FONT_LABEL); setForeground(AppTheme.TEXT_SECONDARY); }});
        leftGroup.add(toSpinner);
        leftGroup.add(generateBtn);

        // Right Actions
        JPanel rightGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightGroup.setOpaque(false);

        exportBtn = AppTheme.secondaryButton("Export CSV");
        printBtn = AppTheme.secondaryButton("Print");
        exportBtn.setEnabled(false);
        printBtn.setEnabled(false);

        exportBtn.addActionListener(e -> exportToCsv());
        printBtn.addActionListener(e -> printTable());

        rightGroup.add(exportBtn);
        rightGroup.add(printBtn);

        row.add(leftGroup, BorderLayout.WEST);
        row.add(rightGroup, BorderLayout.EAST);
        return row;
    }

    private JScrollPane buildTableArea() {
        tableModel = new DefaultTableModel(new String[]{"Ingredient", "Quantity", "Unit"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        table = new JTable(tableModel);
        AppTheme.styleTable(table);

        table.getColumnModel().getColumn(COL_NAME).setPreferredWidth(320);
        table.getColumnModel().getColumn(COL_QUANTITY).setPreferredWidth(120);
        table.getColumnModel().getColumn(COL_UNIT).setPreferredWidth(120);
        table.setDefaultRenderer(Object.class, AppTheme.alternatingRowRenderer());

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(AppTheme.BG_SURFACE);
        return scroll;
    }

    // --- Logic & SwingWorker ---

    private void executeGeneration() {
        LocalDate from = spinnerToLocalDate(fromSpinner);
        LocalDate to = spinnerToLocalDate(toSpinner);

        if (from.isAfter(to)) {
            showWarning("Start date must be before or equal to end date.", "Invalid Date Range");
            return;
        }

        resetUI();
        int userId = frame.getCurrentUserId();

        new SwingWorker<List<RecipeIngredient>, Void>() {
            @Override
            protected List<RecipeIngredient> doInBackground() throws SQLException {
                return groceryService.getGroceryList(userId, from, to);
            }

            @Override
            protected void done() {
                try {
                    updateTable(get());
                    updateSummary(userId, from, to, tableModel.getRowCount());
                } catch (InterruptedException | ExecutionException ex) {
                    summaryLabel.setText("Failed to generate list.");
                    showError("Failed to load grocery list: " + ex.getMessage(), "Database Error");
                }
            }
        }.execute();
    }

    private void updateTable(List<RecipeIngredient> items) {
        tableModel.setRowCount(0);
        for (RecipeIngredient item : items) {
            tableModel.addRow(new Object[]{
                capitalize(item.getIngredientName()),
                formatQty(item.getQuantity()),
                item.getUnit()
            });
        }
        boolean hasItems = !items.isEmpty();
        exportBtn.setEnabled(hasItems);
        printBtn.setEnabled(hasItems);
    }

    private void updateSummary(int userId, LocalDate from, LocalDate to, int itemCount) {
        try {
            summaryLabel.setText(groceryService.getSummary(userId, from, to));
        } catch (SQLException e) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM d");
            String countTxt = itemCount + " item" + (itemCount == 1 ? "" : "s");
            String rangeTxt = from.format(fmt) + (from.equals(to) ? "" : " – " + to.format(fmt));
            summaryLabel.setText(itemCount == 0 ? "Pantry covers all planned meals" : countTxt + " needed for " + rangeTxt);
        }
    }

    // --- IO & Utils ---

    private void exportToCsv() {
        LocalDate from = spinnerToLocalDate(fromSpinner);
        String suggestedName = "grocery_list_" + from.format(DateTimeFormatter.ISO_LOCAL_DATE) + ".csv";

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(suggestedName));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".csv")) {
                file = new File(file.getAbsolutePath() + ".csv");
            }

            CsvExportService.ExportResult result = csvService.exportGroceryList(frame.getCurrentUserId(), from, spinnerToLocalDate(toSpinner), file);
            if (result.isSuccess()) {
                showInfo(result.getMessage() + "\n\nSaved to: " + file.getAbsolutePath(), "Export Complete");
            } else {
                showError(result.getMessage(), "Export Failed");
            }
        }
    }

    private void printTable() {
        try {
            boolean printed = table.print(JTable.PrintMode.FIT_WIDTH, new MessageFormat("Grocery List"), new MessageFormat("Page {0}"));
            if (printed) showInfo("Grocery list sent to printer.", "Print Complete");
        } catch (PrinterException e) {
            showError("Failed to print: " + e.getMessage(), "Print Error");
        }
    }

    private JSpinner buildDateSpinner(LocalDate initial) {
        Date date = Date.from(initial.atStartOfDay(ZoneId.systemDefault()).toInstant());
        SpinnerDateModel model = new SpinnerDateModel(date, null, null, Calendar.DAY_OF_MONTH);
        JSpinner spinner = new JSpinner(model);
        spinner.setEditor(new JSpinner.DateEditor(spinner, DATE_FORMAT));
        spinner.setFont(AppTheme.FONT_BODY);
        spinner.setPreferredSize(new Dimension(130, 32));
        return spinner;
    }

    private LocalDate spinnerToLocalDate(JSpinner spinner) {
        return ((Date) spinner.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private void resetUI() {
        tableModel.setRowCount(0);
        exportBtn.setEnabled(false);
        printBtn.setEnabled(false);
    }

    private String capitalize(String s) {
        return (s == null || s.isEmpty()) ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String formatQty(double qty) {
        return (qty == Math.floor(qty)) ? String.valueOf((int) qty) : String.valueOf(qty);
    }

    private void showWarning(String msg, String title) { JOptionPane.showMessageDialog(this, msg, title, JOptionPane.WARNING_MESSAGE); }
    private void showInfo(String msg, String title) { JOptionPane.showMessageDialog(this, msg, title, JOptionPane.INFORMATION_MESSAGE); }
    private void showError(String msg, String title) { JOptionPane.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE); }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible && frame.getCurrentUserId() != -1) executeGeneration();
    }
}
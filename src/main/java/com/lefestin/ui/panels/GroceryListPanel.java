package com.lefestin.ui.panels;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.print.PrinterException;
import java.io.File;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

import com.lefestin.model.RecipeIngredient;
import com.lefestin.service.CsvExportService;
import com.lefestin.service.GroceryListService;
import com.lefestin.ui.AppTheme;
import com.lefestin.ui.MainFrame;

/**
 * GroceryListPanel — shows missing ingredients for a planned date range.
 */
public class GroceryListPanel extends JPanel {

    private final MainFrame frame;
    private final GroceryListService groceryService;
    private final CsvExportService   csvService;

    // Date range spinners
    private JSpinner fromSpinner;
    private JSpinner toSpinner;

    private JTable table;
    private DefaultTableModel tableModel;

    // Labels updated on load
    private JLabel summaryLabel;

    // Action buttons — disabled until list is generated
    private JButton exportBtn;
    private JButton printBtn;

    // Column indexes
    private static final int COL_NAME = 0;
    private static final int COL_QUANTITY = 1;
    private static final int COL_UNIT = 2;

    // Date formatter for spinners
    private static final String DATE_FORMAT = "MMM d, yyyy";

    public GroceryListPanel(MainFrame frame) {
        this.frame          = frame;
        this.groceryService = new GroceryListService();
        this.csvService     = new CsvExportService();

        setLayout(new BorderLayout(0, 0));
        setBackground(AppTheme.BG_PAGE);

        add(buildHeader(),  BorderLayout.NORTH);
        add(buildTable(),   BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 0));
        header.setBackground(AppTheme.BG_SURFACE);
        header.setBorder(BorderFactory.createCompoundBorder(
            AppTheme.BORDER_DIVIDER,
            BorderFactory.createEmptyBorder(0, 0, 0, 0)));

        header.add(buildTitleRow(),   BorderLayout.NORTH);
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
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        summaryLabel = AppTheme.subtitleLabel(
            "Select a date range and click Generate List");
        summaryLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        titleStack.add(title);
        titleStack.add(Box.createVerticalStrut(2));
        titleStack.add(summaryLabel);

        row.add(titleStack, BorderLayout.WEST);
        return row;
    }

    private JPanel buildControlRow() {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setBackground(AppTheme.BG_SURFACE);
        row.setBorder(BorderFactory.createCompoundBorder(
            AppTheme.BORDER_DIVIDER,
            BorderFactory.createEmptyBorder(10, 20, 12, 20)));

        // ── Left: date range pickers + Generate button ─────────────────────
        JPanel leftGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftGroup.setBackground(AppTheme.BG_SURFACE);

        // Default to current Monday–Sunday
        LocalDate monday = LocalDate.now()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);

        fromSpinner = buildDateSpinner(monday);
        toSpinner   = buildDateSpinner(sunday);

        JLabel fromLabel = new JLabel("From");
        fromLabel.setFont(AppTheme.FONT_LABEL);
        fromLabel.setForeground(AppTheme.TEXT_SECONDARY);

        JLabel toLabel = new JLabel("To");
        toLabel.setFont(AppTheme.FONT_LABEL);
        toLabel.setForeground(AppTheme.TEXT_SECONDARY);

        JButton generateBtn = AppTheme.primaryButton("Generate List");
        generateBtn.addActionListener(e -> generateGroceryList());

        leftGroup.add(fromLabel);
        leftGroup.add(fromSpinner);
        leftGroup.add(toLabel);
        leftGroup.add(toSpinner);
        leftGroup.add(generateBtn);

        // ── Right: Export CSV + Print ─────────────────────────────────────
        JPanel rightGroup = new JPanel(new FlowLayout(
            FlowLayout.RIGHT, 8, 0));
        rightGroup.setBackground(AppTheme.BG_SURFACE);

        exportBtn = AppTheme.secondaryButton("Export CSV");
        printBtn  = AppTheme.secondaryButton("Print");

        // Disabled until a list has been generated
        exportBtn.setEnabled(false);
        printBtn.setEnabled(false);

        exportBtn.addActionListener(e -> exportToCsv());
        printBtn.addActionListener( e -> printTable());

        rightGroup.add(exportBtn);
        rightGroup.add(printBtn);

        row.add(leftGroup,  BorderLayout.WEST);
        row.add(rightGroup, BorderLayout.EAST);

        return row;
    }

    // ── Styled date spinner ───────────────────────────────────────────────
    private JSpinner buildDateSpinner(LocalDate initial) {
        // Convert LocalDate → java.util.Date for SpinnerDateModel
        Date date = Date.from(initial
            .atStartOfDay(ZoneId.systemDefault()).toInstant());

        SpinnerDateModel model = new SpinnerDateModel(
            date, null, null, java.util.Calendar.DAY_OF_MONTH);

        JSpinner spinner = new JSpinner(model);
        spinner.setEditor(new JSpinner.DateEditor(spinner, DATE_FORMAT));
        spinner.setFont(AppTheme.FONT_BODY);
        spinner.setPreferredSize(new Dimension(130, 32));

        return spinner;
    }

    // ── Helper: read LocalDate from a date spinner ────────────────────────
    private LocalDate spinnerToLocalDate(JSpinner spinner) {
        Date date = (Date) spinner.getValue();
        return date.toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDate();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  TABLE
    // ══════════════════════════════════════════════════════════════════════

    private JScrollPane buildTable() {
        tableModel = new DefaultTableModel(
            new String[]{"Ingredient", "Quantity", "Unit"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        table = new JTable(tableModel);
        AppTheme.styleTable(table);

        // Column widths
        table.getColumnModel().getColumn(COL_NAME)
            .setPreferredWidth(320);
        table.getColumnModel().getColumn(COL_QUANTITY)
            .setPreferredWidth(120);
        table.getColumnModel().getColumn(COL_UNIT)
            .setPreferredWidth(120);

        // Alternating rows
        table.setDefaultRenderer(Object.class,
            AppTheme.alternatingRowRenderer());

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(AppTheme.BG_SURFACE);

        return scroll;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  GENERATE — SwingWorker keeps EDT responsive
    // ══════════════════════════════════════════════════════════════════════

    private void generateGroceryList() {
        LocalDate from = spinnerToLocalDate(fromSpinner);
        LocalDate to   = spinnerToLocalDate(toSpinner);

        if (from.isAfter(to)) {
            JOptionPane.showMessageDialog(this,
                "Start date must be before or equal to end date.",
                "Invalid Date Range",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Reset UI state while loading
        tableModel.setRowCount(0);
        exportBtn.setEnabled(false);
        printBtn.setEnabled(false);

        int userId = frame.getCurrentUserId();

        new SwingWorker<List<RecipeIngredient>, Void>() {

            @Override
            protected List<RecipeIngredient> doInBackground()
                    throws SQLException {
                return groceryService.getGroceryList(userId, from, to);
            }

            @Override
            protected void done() {
                try {
                    List<RecipeIngredient> items = get();

                    // Populate table
                    tableModel.setRowCount(0);
                    for (RecipeIngredient item : items) {
                        tableModel.addRow(new Object[]{
                            capitalize(item.getIngredientName()),
                            formatQty(item.getQuantity()),
                            item.getUnit()
                        });
                    }

                    // Update summary label
                    updateSummary(userId, from, to, items.size());

                    // Enable export + print only if list is non-empty
                    boolean hasItems = !items.isEmpty();
                    exportBtn.setEnabled(hasItems);
                    printBtn.setEnabled(hasItems);

                } catch (InterruptedException | ExecutionException ex) {
                    summaryLabel.setText("Failed to generate list.");
                    JOptionPane.showMessageDialog(
                        GroceryListPanel.this,
                        "Failed to load grocery list: " + ex.getMessage(),
                        "Database Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // Updates summary label after generation ────────────────────────────
    private void updateSummary(int userId,
                                LocalDate from,
                                LocalDate to,
                                int itemCount) {
        try {
            summaryLabel.setText(
                groceryService.getSummary(userId, from, to));
        } catch (SQLException e) {
            // Fallback — build summary from item count directly
            if (itemCount == 0) {
                summaryLabel.setText(
                    "Pantry covers all planned meals");
            } else {
                DateTimeFormatter fmt =
                    DateTimeFormatter.ofPattern("MMM d");
                summaryLabel.setText(
                    itemCount + " item"
                    + (itemCount == 1 ? "" : "s")
                    + " needed for "
                    + from.format(fmt)
                    + (from.equals(to)
                        ? "" : " – " + to.format(fmt)));
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  EXPORT CSV
    // ══════════════════════════════════════════════════════════════════════

    private void exportToCsv() {
        LocalDate from = spinnerToLocalDate(fromSpinner);
        LocalDate to   = spinnerToLocalDate(toSpinner);

        // Suggest a filename using the from date
        String suggestedName = "grocery_list_"
            + from.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            + ".csv";

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Grocery List");
        chooser.setSelectedFile(new File(suggestedName));

        int choice = chooser.showSaveDialog(this);
        if (choice != JFileChooser.APPROVE_OPTION) return;

        File outputFile = chooser.getSelectedFile();

        // Ensure .csv extension
        if (!outputFile.getName().toLowerCase().endsWith(".csv")) {
            outputFile = new File(outputFile.getAbsolutePath() + ".csv");
        }

        CsvExportService.ExportResult result =
            csvService.exportGroceryList(
                frame.getCurrentUserId(), from, to, outputFile);

        if (result.isSuccess()) {
            JOptionPane.showMessageDialog(this,
                result.getMessage()
                + "\n\nSaved to: " + outputFile.getAbsolutePath(),
                "Export Complete",
                JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this,
                result.getMessage(),
                "Export Failed",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PRINT
    // ══════════════════════════════════════════════════════════════════════

    private void printTable() {
        try {
            boolean printed = table.print(
                JTable.PrintMode.FIT_WIDTH,
                new MessageFormat("Grocery List"),
                new MessageFormat("Page {0}"));

            if (printed) {
                JOptionPane.showMessageDialog(this,
                    "Grocery list sent to printer.",
                    "Print Complete",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (PrinterException e) {
            JOptionPane.showMessageDialog(this,
                "Failed to print: " + e.getMessage(),
                "Print Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  AUTO-REFRESH ON SHOW
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        // Auto-generate for current week when panel becomes visible
        if (visible && frame.getCurrentUserId() != -1) {
            generateGroceryList();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════════════════

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String formatQty(double qty) {
        return (qty == Math.floor(qty))
            ? String.valueOf((int) qty)
            : String.valueOf(qty);
    }
}
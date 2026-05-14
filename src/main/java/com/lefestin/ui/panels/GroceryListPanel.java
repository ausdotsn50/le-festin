package com.lefestin.ui.panels;

import java.awt.BorderLayout;
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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import com.lefestin.model.RecipeIngredient;
import com.lefestin.service.CsvExportService;
import com.lefestin.service.GroceryListService;
import com.lefestin.ui.AppTheme;
import com.lefestin.ui.MainFrame;

/**
 * GroceryListPanel — read-only view for missing ingredients.
 */
public class GroceryListPanel extends BaseListPanel {

    private final GroceryListService groceryService = new GroceryListService();
    private final CsvExportService csvService = new CsvExportService();

    private JSpinner fromSpinner, toSpinner;
    private JTable table;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;
    private JLabel summaryLabel;
    private JButton exportBtn, printBtn;

    private static final int COL_NAME     = 0;
    private static final int COL_QUANTITY = 1;
    private static final int COL_UNIT     = 2;
    private static final int COL_NOTE     = 3;
    private static final String DATE_FORMAT = "MMM d, yyyy";

    public GroceryListPanel(MainFrame frame) {
        super(frame);
    }

    // --- BaseListPanel contract ---

    @Override
    protected String getHeaderTitle() { return "Grocery List"; }

    @Override
    protected String getHeaderDescription() { return "Missing ingredients from your meal plan"; }

    @Override
    protected String getSearchPlaceholder() { return "Search ingredients..."; }

    @Override
    protected JComponent buildHeaderRightControl() {
        summaryLabel = new JLabel("Generate to see results");
        summaryLabel.setFont(AppTheme.FONT_SMALL);
        summaryLabel.setForeground(AppTheme.TEXT_MUTED);
        return summaryLabel;
    }

    @Override
    protected JComponent buildSearchRightControl() {
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        controls.setBackground(AppTheme.BG_SURFACE);

        LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        fromSpinner = buildDateSpinner(monday);
        toSpinner   = buildDateSpinner(monday.plusDays(6));

        JButton generateBtn = AppTheme.primaryButton("Generate List");
        generateBtn.addActionListener(e -> executeGeneration());

        exportBtn = AppTheme.secondaryButton("Export CSV");
        printBtn  = AppTheme.secondaryButton("Print");
        exportBtn.setEnabled(false);
        printBtn.setEnabled(false);
        exportBtn.addActionListener(e -> exportToCsv());
        printBtn.addActionListener(e  -> printTable());

        controls.add(new JLabel("From") {{ setFont(AppTheme.FONT_LABEL); setForeground(AppTheme.TEXT_SECONDARY); }});
        controls.add(fromSpinner);
        controls.add(new JLabel("To")   {{ setFont(AppTheme.FONT_LABEL); setForeground(AppTheme.TEXT_SECONDARY); }});
        controls.add(toSpinner);
        controls.add(generateBtn);
        controls.add(exportBtn);
        controls.add(printBtn);

        return controls;
    }

    @Override
    protected JComponent buildTableContent() {
        tableModel = new DefaultTableModel(new String[]{"Ingredient", "Quantity", "Unit", "Note"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        table = new JTable(tableModel);
        AppTheme.styleTable(table);

        sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        table.getColumnModel().getColumn(COL_NAME).setPreferredWidth(280);
        table.getColumnModel().getColumn(COL_QUANTITY).setPreferredWidth(100);
        table.getColumnModel().getColumn(COL_UNIT).setPreferredWidth(100);
        table.getColumnModel().getColumn(COL_NOTE).setPreferredWidth(200);
        table.setDefaultRenderer(Object.class, AppTheme.alternatingRowRenderer());

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e)  { filterTable(); }
            @Override
            public void removeUpdate(DocumentEvent e)  { filterTable(); }
            @Override
            public void changedUpdate(DocumentEvent e) { filterTable(); }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
        scroll.getViewport().setBackground(AppTheme.BG_SURFACE);
        return scroll;
    }

    @Override
    protected JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        bar.setOpaque(false);
        return bar;
    }

    @Override protected JButton createActionButton() { return AppTheme.dangerButton("Remove"); }
    @Override protected void onAddClicked()    {}
    @Override protected void onEditClicked()   {}
    @Override protected void onActionClicked() {}

    // --- Logic & SwingWorker ---

    private void executeGeneration() {
        LocalDate from = spinnerToLocalDate(fromSpinner);
        LocalDate to   = spinnerToLocalDate(toSpinner);

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

    private void filterTable() {
        String text = searchField.getText().trim();
        sorter.setRowFilter(text.isEmpty() ? null : RowFilter.regexFilter("(?i)" + text, COL_NAME));
    }

    private void updateTable(List<RecipeIngredient> items) {
        tableModel.setRowCount(0);
        for (RecipeIngredient item : items) {
            String note = item.getNote() != null ? item.getNote() : "";
            tableModel.addRow(new Object[]{
                capitalize(item.getIngredientName()),
                formatQty(item.getQuantity()),
                item.getUnit(),
                note
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
            summaryLabel.setText(itemCount == 0
                ? "Pantry covers all planned meals"
                : countTxt + " needed for " + rangeTxt);
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
            if (!file.getName().toLowerCase().endsWith(".csv"))
                file = new File(file.getAbsolutePath() + ".csv");

            CsvExportService.ExportResult result = csvService.exportGroceryList(
                frame.getCurrentUserId(), from, spinnerToLocalDate(toSpinner), file);
            if (result.isSuccess()) {
                showInfo(result.getMessage() + "\n\nSaved to: " + file.getAbsolutePath(), "Export Complete");
            } else {
                showError(result.getMessage(), "Export Failed");
            }
        }
    }

    private void printTable() {
        try {
            boolean printed = table.print(JTable.PrintMode.FIT_WIDTH,
                new MessageFormat("Grocery List"), new MessageFormat("Page {0}"));
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
    private void showInfo(String msg, String title)    { JOptionPane.showMessageDialog(this, msg, title, JOptionPane.INFORMATION_MESSAGE); }
    private void showError(String msg, String title)   { JOptionPane.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE); }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible && frame.getCurrentUserId() != -1) executeGeneration();
    }
}

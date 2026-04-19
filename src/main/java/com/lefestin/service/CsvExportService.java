package com.lefestin.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import com.lefestin.dao.MealEntryDAO;
import com.lefestin.dao.RecipeDAO;
import com.lefestin.helper.Helper;
import com.lefestin.model.MealEntry;
import com.lefestin.model.Recipe;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * CsvExportService — exports meal plan data to CSV files.
 *
 * Uses Apache Commons CSV for proper quoting, escaping,
 * and header handling — never raw string concatenation.
 *
 * Output columns:
 *   Date | Meal Type | Recipe Title | Category | Prep Time (mins)
 *
 * Example output:
 *   Date,Meal Type,Recipe Title,Category,Prep Time (mins)
 *   2026-04-17,Breakfast,Garlic Fried Rice,Breakfast,15
 *   2026-04-17,Lunch,Garlic Butter Chicken,Lunch,25
 *   2026-04-18,Dinner,Pork Adobo,Dinner,60
 */
public class CsvExportService {

    // ── Column header constants ────────────────────────────────────────────
    // Defined as constants so they never drift between the header
    // row and the Javadoc above — one place to rename a column.
    public static final String COL_DATE      = "Date";
    public static final String COL_MEAL_TYPE = "Meal Type";
    public static final String COL_TITLE     = "Recipe Title";
    public static final String COL_CATEGORY  = "Category";
    public static final String COL_PREP_TIME = "Prep Time (mins)";

    // ── Date format written to CSV ─────────────────────────────────────────
    // ISO format (yyyy-MM-dd) — unambiguous, sorts correctly,
    // readable by Excel and Google Sheets without extra config
    private static final DateTimeFormatter DATE_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ── DAOs ──────────────────────────────────────────────────────────────
    private final MealEntryDAO mealEntryDAO;
    private final RecipeDAO    recipeDAO;

    // ── Constructor ───────────────────────────────────────────────────────
    public CsvExportService() {
        this.mealEntryDAO = new MealEntryDAO();
        this.recipeDAO    = new RecipeDAO();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PRIMARY METHOD
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Exports the meal plan for a date range to a CSV file.
     *
     * Flow:
     *  1. Fetch all meal entries in range via MealEntryDAO
     *     (entries already include recipeTitle + category from JOIN)
     *  2. For entries missing prep time, fetch Recipe via RecipeDAO
     *  3. Write header + one row per entry via CSVPrinter
     *  4. Flush and close — try-with-resources guarantees cleanup
     *
     * @param userId      the logged-in user
     * @param from        first day of range (inclusive)
     * @param to          last day of range  (inclusive)
     * @param outputFile  the File to write to — created if not exists,
     *                    overwritten if it already exists
     * @return ExportResult with success flag, row count, and message
     */
    public ExportResult exportMealPlan(int userId,
                                       LocalDate from,
                                       LocalDate to,
                                       File outputFile) {
        try {
            // ── Step 1: fetch entries ──────────────────────────────────────
            List<MealEntry> entries =
                mealEntryDAO.getEntriesByWeek(userId, from, to);

            if (entries.isEmpty()) {
                return ExportResult.fail(
                    "No meals planned for this period.\n"
                    + "Add meals to your planner before exporting."
                );
            }

            // ── Step 2 + 3: write CSV ──────────────────────────────────────
            int rowsWritten = writeCSV(entries, outputFile);

            return ExportResult.ok(
                rowsWritten,
                "Exported " + rowsWritten + " meal"
                    + (rowsWritten == 1 ? "" : "s")
                    + " to " + outputFile.getName()
            );

        } catch (SQLException e) {
            return ExportResult.fail(
                "Failed to load meal data from database.\n"
                + "Detail: " + e.getMessage()
            );
        } catch (IOException e) {
            return ExportResult.fail(
                "Failed to write CSV file.\n"
                + "Check that the destination folder is writable.\n"
                + "Detail: " + e.getMessage()
            );
        }
    }

    /**
     * Exports the grocery list for a date range to a CSV file.
     * Convenience export used by GroceryListPanel.
     *
     * Columns: Ingredient, Quantity, Unit
     *
     * @param userId      the logged-in user
     * @param from        start of range
     * @param to          end of range
     * @param outputFile  destination file
     * @return ExportResult with row count and message
     */
    public ExportResult exportGroceryList(int userId,
                                          LocalDate from,
                                          LocalDate to,
                                          File outputFile) {
        try {
            GroceryListService groceryService = new GroceryListService();
            List<com.lefestin.model.RecipeIngredient> items =
                groceryService.getGroceryList(userId, from, to);

            if (items.isEmpty()) {
                return ExportResult.fail(
                    "Grocery list is empty — your pantry covers "
                    + "all planned meals for this period."
                );
            }

            // ── Build CSVFormat with headers ───────────────────────────────
            CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader("Ingredient", "Quantity", "Unit")
                .setSkipHeaderRecord(false)
                .build();

            // ── Write with try-with-resources ──────────────────────────────
            // Both BufferedWriter and CSVPrinter implement AutoCloseable —
            // the inner try closes CSVPrinter first (flushes buffer),
            // then BufferedWriter is closed by the outer try.
            try (BufferedWriter writer =
                        new BufferedWriter(new FileWriter(outputFile));
                 CSVPrinter printer = new CSVPrinter(writer, format)) {

                for (com.lefestin.model.RecipeIngredient item : items) {
                    printer.printRecord(
                        Helper.capitalize(item.getIngredientName()),
                        formatQty(item.getQuantity()),
                        item.getUnit()
                    );
                }

                printer.flush();
            }

            return ExportResult.ok(
                items.size(),
                "Exported grocery list ("
                    + items.size() + " item"
                    + (items.size() == 1 ? "" : "s")
                    + ") to " + outputFile.getName()
            );

        } catch (SQLException e) {
            return ExportResult.fail(
                "Failed to load grocery data.\nDetail: " + e.getMessage());
        } catch (IOException e) {
            return ExportResult.fail(
                "Failed to write CSV file.\nDetail: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Writes meal entries to a CSV file and returns the row count.
     *
     * CSVFormat.DEFAULT handles:
     *  - Quoting values that contain commas or newlines
     *  - Escaping double quotes inside values
     *  - Writing CRLF line endings (RFC 4180 compliant)
     *
     * The builder pattern on CSVFormat is the recommended Commons CSV
     * 1.10.x API — the older withHeader() methods are deprecated.
     */
    private int writeCSV(List<MealEntry> entries, File outputFile)
            throws SQLException, IOException {

        // ── Build format with column headers ──────────────────────────────
        CSVFormat format = CSVFormat.DEFAULT.builder()
            .setHeader(
                COL_DATE,
                COL_MEAL_TYPE,
                COL_TITLE,
                COL_CATEGORY,
                COL_PREP_TIME)
            .setSkipHeaderRecord(false) // always write the header row
            .build();

        int rowCount = 0;

        // ── try-with-resources: both writer and printer auto-closed ────────
        try (BufferedWriter writer =
                    new BufferedWriter(new FileWriter(outputFile));
             CSVPrinter printer = new CSVPrinter(writer, format)) {

            for (MealEntry entry : entries) {

                // recipeTitle comes from the JOIN in MealEntryDAO —
                // it should always be populated, but guard just in case
                String title    = entry.getRecipeTitle() != null
                    ? entry.getRecipeTitle() : "(unknown)";
                String category = entry.getRecipeCategory() != null
                    ? entry.getRecipeCategory() : "";

                // prepTime is not on MealEntry — fetch from RecipeDAO
                int prepTime = fetchPrepTime(entry.getRecipeId());

                printer.printRecord(
                    entry.getScheduledDate().format(DATE_FMT), // "2026-04-17"
                    entry.getMealType(),                        // "Breakfast"
                    title,                                      // "Garlic Fried Rice"
                    category,                                   // "Breakfast"
                    prepTime                                    // 15
                );

                rowCount++;
            }

            // Explicit flush before close — ensures last buffer
            // is written even if close() is delayed
            printer.flush();
        }

        return rowCount;
    }

    /**
     * Fetches prep time for a recipe.
     * MealEntry doesn't carry prepTime — it needs a RecipeDAO lookup.
     * Returns 0 if recipe is not found (defensive fallback).
     */
    private int fetchPrepTime(int recipeId) throws SQLException {
        Recipe recipe = recipeDAO.getRecipeById(recipeId);
        return (recipe != null) ? recipe.getPrepTime() : 0;
    }

    /** Formats a double quantity — strips trailing .0 for whole numbers. */
    private String formatQty(double qty) {
        return (qty == Math.floor(qty))
            ? String.valueOf((int) qty)
            : String.valueOf(qty);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  RESULT WRAPPER
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Wraps the export outcome so the UI can show success or failure
     * without catching exceptions itself.
     *
     * Usage in GroceryListPanel or WeeklyPlannerPanel:
     *
     *   ExportResult result = csvService.exportMealPlan(...);
     *   if (result.isSuccess()) {
     *       JOptionPane.showMessageDialog(this, result.getMessage());
     *   } else {
     *       JOptionPane.showMessageDialog(this, result.getMessage(),
     *           "Export Failed", JOptionPane.ERROR_MESSAGE);
     *   }
     */
    public static class ExportResult {
        private final boolean success;
        private final int     rowsWritten;
        private final String  message;

        private ExportResult(boolean success,
                              int rowsWritten,
                              String message) {
            this.success     = success;
            this.rowsWritten = rowsWritten;
            this.message     = message;
        }

        public static ExportResult ok(int rows, String message) {
            return new ExportResult(true, rows, message);
        }

        public static ExportResult fail(String message) {
            return new ExportResult(false, 0, message);
        }

        public boolean isSuccess()    { return success;     }
        public int     getRowsWritten(){ return rowsWritten; }
        public String  getMessage()   { return message;     }
    }
}
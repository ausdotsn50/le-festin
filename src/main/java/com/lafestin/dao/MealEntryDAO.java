package com.lafestin.dao;

import com.lafestin.config.DBConnection;
import com.lafestin.model.MealEntry;

import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * MealEntryDAO — all SQL for the `meal_entry` table.
 *
 * meal_entry(recipe_id FK, user_id FK, meal_type, scheduled_date)
 * Composite PK: (user_id, scheduled_date, meal_type)
 *
 * All SELECT queries JOIN recipe so recipeTitle and recipeCategory
 * are populated — the UI never needs a second lookup.
 *
 * LocalDate ↔ SQL DATE conversion rule used throughout:
 *   Write:  java.sql.Date.valueOf(localDate)
 *   Read:   rs.getDate("col").toLocalDate()
 */
public class MealEntryDAO {

    private Connection conn() {
        return DBConnection.getInstance().getConnection();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  SHARED SQL FRAGMENTS
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Base SELECT + JOIN used by every read method.
     * Centralised so a schema change only needs one edit.
     */
    private static final String BASE_SELECT = """
        SELECT
            me.recipe_id,
            me.user_id,
            me.meal_type,
            me.scheduled_date,
            r.title    AS recipe_title,
            r.category AS recipe_category
        FROM meal_entry me
        JOIN recipe r ON me.recipe_id = r.recipe_id
        """;

    // ══════════════════════════════════════════════════════════════════════
    //  CREATE
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Inserts a new meal plan entry.
     *
     * The composite PK (user_id, scheduled_date, meal_type) enforces
     * one recipe per slot at the DB level. Attempting to assign a
     * second recipe to the same slot throws SQLException error 1062.
     * Callers should deleteEntry() first if replacing an existing slot.
     *
     * @param e  MealEntry with all fields set
     * @throws SQLException if INSERT fails or slot already occupied
     */
    public void addEntry(MealEntry e) throws SQLException {
        String sql = """
            INSERT INTO meal_entry
                (recipe_id, user_id, meal_type, scheduled_date)
            VALUES (?, ?, ?, ?)
            """;

        try (PreparedStatement stmt = conn().prepareStatement(sql)) {
            stmt.setInt(   1, e.getRecipeId());
            stmt.setInt(   2, e.getUserId());
            stmt.setString(3, e.getMealType());
            stmt.setDate(  4, Date.valueOf(e.getScheduledDate()));

            stmt.executeUpdate();
        }
    }

    /**
     * Replaces whatever is in a slot with a new recipe.
     * Deletes the existing entry (if any) then inserts the new one.
     * Use this in WeeklyPlannerPanel instead of addEntry() to avoid
     * duplicate key exceptions when reassigning a slot.
     *
     * @param e  MealEntry with the new recipe and slot details
     * @throws SQLException if either DELETE or INSERT fails
     */
    public void replaceEntry(MealEntry e) throws SQLException {
        deleteEntry(e.getUserId(), e.getScheduledDate(), e.getMealType());
        addEntry(e);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  READ
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Returns all meal entries for a single day.
     * Ordered by meal_type — Breakfast, Dinner, Lunch (MySQL ENUM order).
     *
     * @param userId  the logged-in user
     * @param date    the day to query
     * @return list of up to 3 MealEntry rows (one per slot),
     *         empty list if nothing planned
     * @throws SQLException if SELECT fails
     */
    public List<MealEntry> getEntriesByDate(int userId, LocalDate date)
            throws SQLException {
        String sql = BASE_SELECT + """
            WHERE me.user_id        = ?
              AND me.scheduled_date = ?
            ORDER BY me.meal_type
            """;

        return query(sql,
            stmt -> {
                stmt.setInt( 1, userId);
                stmt.setDate(2, Date.valueOf(date));
            }
        );
    }

    /**
     * Returns all meal entries for a date range (inclusive on both ends).
     * Used by WeeklyPlannerPanel to load the full week in one query.
     *
     * @param userId  the logged-in user
     * @param from    first day of range (Monday for weekly planner)
     * @param to      last day of range  (Sunday for weekly planner)
     * @return list of MealEntry rows ordered by date then meal_type
     * @throws SQLException if SELECT fails
     */
    public List<MealEntry> getEntriesByWeek(int userId,
                                             LocalDate from,
                                             LocalDate to)
            throws SQLException {
        String sql = BASE_SELECT + """
            WHERE me.user_id        = ?
              AND me.scheduled_date BETWEEN ? AND ?
            ORDER BY me.scheduled_date, me.meal_type
            """;

        return query(sql,
            stmt -> {
                stmt.setInt( 1, userId);
                stmt.setDate(2, Date.valueOf(from));
                stmt.setDate(3, Date.valueOf(to));
            }
        );
    }

    /**
     * Returns all meal entries for a calendar month.
     * Used by MonthlyOverviewPanel to count meals per day.
     *
     * Computes the first and last day of the month automatically
     * so the caller never needs to deal with month-length edge cases
     * (28/29/30/31 days, February in leap years, etc.).
     *
     * @param userId  the logged-in user
     * @param year    4-digit year  e.g. 2026
     * @param month   1-based month e.g. 4 for April
     * @return list of MealEntry rows ordered by date then meal_type
     * @throws SQLException if SELECT fails
     */
    public List<MealEntry> getEntriesByMonth(int userId,
                                              int year,
                                              int month)
            throws SQLException {
        YearMonth ym    = YearMonth.of(year, month);
        LocalDate first = ym.atDay(1);
        LocalDate last  = ym.atEndOfMonth();  // handles leap years

        String sql = BASE_SELECT + """
            WHERE me.user_id        = ?
              AND me.scheduled_date BETWEEN ? AND ?
            ORDER BY me.scheduled_date, me.meal_type
            """;

        return query(sql,
            stmt -> {
                stmt.setInt( 1, userId);
                stmt.setDate(2, Date.valueOf(first));
                stmt.setDate(3, Date.valueOf(last));
            }
        );
    }

    /**
     * Checks whether a specific meal slot already has a recipe assigned.
     * Use before addEntry() in cases where replaceEntry() is not wanted.
     *
     * @param userId    the logged-in user
     * @param date      the day to check
     * @param mealType  "Breakfast", "Lunch", or "Dinner"
     * @return true if a recipe is already assigned to this slot
     * @throws SQLException if SELECT fails
     */
    public boolean slotIsOccupied(int userId,
                                   LocalDate date,
                                   String mealType)
            throws SQLException {
        String sql = """
            SELECT COUNT(*)
            FROM meal_entry
            WHERE user_id        = ?
              AND scheduled_date = ?
              AND meal_type      = ?
            """;

        try (PreparedStatement stmt = conn().prepareStatement(sql)) {
            stmt.setInt(   1, userId);
            stmt.setDate(  2, Date.valueOf(date));
            stmt.setString(3, mealType);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }

        return false;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  DELETE
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Deletes one specific meal slot.
     * Identified by the composite PK: (user_id, scheduled_date, meal_type).
     *
     * @param userId    the logged-in user
     * @param date      the day of the slot
     * @param mealType  "Breakfast", "Lunch", or "Dinner"
     * @throws SQLException if DELETE fails
     */
    public void deleteEntry(int userId,
                            LocalDate date,
                            String mealType) throws SQLException {
        String sql = """
            DELETE FROM meal_entry
            WHERE user_id        = ?
              AND scheduled_date = ?
              AND meal_type      = ?
            """;

        try (PreparedStatement stmt = conn().prepareStatement(sql)) {
            stmt.setInt(   1, userId);
            stmt.setDate(  2, Date.valueOf(date));
            stmt.setString(3, mealType);

            stmt.executeUpdate();
        }
    }

    /**
     * Clears all meal slots for a single day.
     *
     * @param userId  the logged-in user
     * @param date    the day to clear
     * @throws SQLException if DELETE fails
     */
    public void clearDay(int userId, LocalDate date)
            throws SQLException {
        String sql = """
            DELETE FROM meal_entry
            WHERE user_id        = ?
              AND scheduled_date = ?
            """;

        try (PreparedStatement stmt = conn().prepareStatement(sql)) {
            stmt.setInt( 1, userId);
            stmt.setDate(2, Date.valueOf(date));

            stmt.executeUpdate();
        }
    }

    /**
     * Clears all meal slots in a date range (inclusive).
     * Used by WeeklyPlannerPanel "Clear Week" button.
     *
     * @param userId  the logged-in user
     * @param from    first day to clear
     * @param to      last day to clear
     * @throws SQLException if DELETE fails
     */
    public void clearWeek(int userId,
                          LocalDate from,
                          LocalDate to) throws SQLException {
        String sql = """
            DELETE FROM meal_entry
            WHERE user_id        = ?
              AND scheduled_date BETWEEN ? AND ?
            """;

        try (PreparedStatement stmt = conn().prepareStatement(sql)) {
            stmt.setInt( 1, userId);
            stmt.setDate(2, Date.valueOf(from));
            stmt.setDate(3, Date.valueOf(to));

            stmt.executeUpdate();
        }
    }

    /**
     * Clears all meal slots for a calendar month.
     * Used by MonthlyOverviewPanel "Clear Month" button.
     *
     * @param userId  the logged-in user
     * @param year    4-digit year
     * @param month   1-based month
     * @throws SQLException if DELETE fails
     */
    public void clearMonth(int userId, int year, int month)
            throws SQLException {
        YearMonth ym  = YearMonth.of(year, month);
        clearWeek(userId, ym.atDay(1), ym.atEndOfMonth());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Functional interface for binding PreparedStatement parameters.
     * Lets query() accept a lambda instead of duplicating
     * the try-with-resources + ResultSet loop in every method.
     */
    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement stmt) throws SQLException;
    }

    /**
     * Executes a SELECT query, binds parameters via lambda,
     * maps all rows, and returns the list.
     * Every read method delegates here.
     */
    private List<MealEntry> query(String sql, StatementBinder binder)
            throws SQLException {
        List<MealEntry> list = new ArrayList<>();

        try (PreparedStatement stmt = conn().prepareStatement(sql)) {
            binder.bind(stmt);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }

        return list;
    }

    /**
     * Maps one ResultSet row to a MealEntry.
     * Uses the 6-arg constructor to include recipeTitle + recipeCategory
     * from the JOIN so the UI never needs a second query.
     */
    private MealEntry mapRow(ResultSet rs) throws SQLException {
        return new MealEntry(
            rs.getInt(   "recipe_id"),
            rs.getInt(   "user_id"),
            rs.getString("meal_type"),
            rs.getDate(  "scheduled_date").toLocalDate(),
            rs.getString("recipe_title"),
            rs.getString("recipe_category")
        );
    }
}
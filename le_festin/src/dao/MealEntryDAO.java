package dao;

import java.time.LocalDate;
import java.util.List;

import model.MealEntry;

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
public interface MealEntryDAO {
    void addEntry(MealEntry e);
    void replaceEntry(MealEntry e);
    void moveEntryToSlot(MealEntry entry,
                                LocalDate newDate,
                                String newMealType);
    void swapEntries(MealEntry first, MealEntry second);
    List<MealEntry> getEntriesByDate(int userId, LocalDate date);
    List<MealEntry> getEntriesByWeek(int userId,
                                             LocalDate from,
                                             LocalDate to);
    List<MealEntry> getEntriesByMonth(int userId,
                                              int year,
                                              int month);
    boolean slotIsOccupied(int userId,
                                   LocalDate date,
                                   String mealType);

    void deleteEntry(int userId,
                            LocalDate date,
                            String mealType);
   
    void clearDay(int userId, LocalDate date);
    void clearWeek(int userId,
                          LocalDate from,
                          LocalDate to);
    void clearMonth(int userId, int year, int month);
}
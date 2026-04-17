package com.lafestin.dao;

import com.lafestin.model.MealEntry;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MealEntryDAO {
    public List<MealEntry> getEntriesByWeek(
            int userId, LocalDate from, LocalDate to)
            throws SQLException {
        return new ArrayList<>();
    }
    public void addEntry(MealEntry entry)
            throws SQLException {}
    public void deleteEntry(
            int userId, LocalDate date, String mealType)
            throws SQLException {}
    public void clearWeek(
            int userId, LocalDate from, LocalDate to)
            throws SQLException {}
}
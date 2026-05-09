package com.lefestin.helper;

import javax.swing.JDialog;

import java.awt.Component;
import java.awt.Dimension;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import com.lefestin.dao.IngredientDAO;
import com.lefestin.model.Ingredient;

// So far, the helper funs here are form ui/dialogs
// General helper function (unorganized)
public class Helper {
    // Unit options 
    public static final String[] UNITS = {
        "piece", "whole", "cup", "tablespoon", "teaspoon",
        "gram", "kilogram", "milliliter", "liter", "clove",
        "slice", "pinch"
    };

    public static final int COL_NAME = 0;
    public static final int COL_QUANTITY = 1;
    public static final int COL_UNIT = 2;

    public static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public static void packAndCenter(JFrame frame, JDialog dialog, Dimension dim) {
        dialog.setPreferredSize(dim);
        dialog.pack();
        dialog.setLocationRelativeTo(frame);
        dialog.setResizable(false);
    }

    public static List<Ingredient> loadAllIngredients(Component parentComponent, IngredientDAO ingredientDAO) {
    try {
        return ingredientDAO.getAllIngredients();
    } catch (SQLException e) {
        JOptionPane.showMessageDialog(parentComponent,
            "Could not load ingredients from database.\n"
            + e.getMessage(),
            "Warning",
            JOptionPane.WARNING_MESSAGE);
        return new ArrayList<>();
    }
}

    public static String formatQty(double qty) {
        return (qty == Math.floor(qty))
            ? String.valueOf((int) qty)
            : String.valueOf(qty);
    }

    public static String slotKey(LocalDate date, String mealType) {
        return date + "|" + mealType;
    }

    // Returns the Monday of the week containing the given date
    public static LocalDate getMonday(LocalDate date) {
        return date.with(
            java.time.temporal.TemporalAdjusters.previousOrSame(
                DayOfWeek.MONDAY));
    }

}

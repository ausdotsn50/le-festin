package com.lafestin.helper;

import javax.swing.JDialog;
import java.awt.Dimension;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import com.lafestin.dao.IngredientDAO;
import com.lafestin.model.Ingredient;

// So far, the helper funs here are form ui/dialogs
public class Helper {
    public static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public static void packAndCenter(JFrame frame, JDialog dialog) {
        dialog.setPreferredSize(new Dimension(580, 680));
        dialog.pack();
        dialog.setLocationRelativeTo(frame);
        dialog.setResizable(false);
    }

    public static void loadAllIngredients(JDialog dialog, List<Ingredient> allIngredients, IngredientDAO ingredientDAO) {
        try {
            allIngredients = ingredientDAO.getAllIngredients();
        } catch (SQLException e) {
            allIngredients = new ArrayList<>();
            JOptionPane.showMessageDialog(dialog,
                "Could not load ingredients from database.\n"
                + e.getMessage(),
                "Warning",
                JOptionPane.WARNING_MESSAGE);
        }
    }


    

}

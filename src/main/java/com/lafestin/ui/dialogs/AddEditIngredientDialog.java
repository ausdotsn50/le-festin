package com.lafestin.ui.dialogs;

import com.lafestin.model.PantryItem;
import com.lafestin.ui.MainFrame;
import javax.swing.*;

public class AddEditIngredientDialog extends JDialog {

    private boolean saved = false;

    public AddEditIngredientDialog(MainFrame frame, PantryItem item) {
        super(frame, item == null ? "Add Ingredient" : "Edit Ingredient",
            true);
        // TODO: build full form in Week 2
        setSize(400, 200);
        setLocationRelativeTo(frame);
        add(new JLabel("Dialog coming soon", SwingConstants.CENTER));
    }

    public boolean isSaved() { return saved; }
}
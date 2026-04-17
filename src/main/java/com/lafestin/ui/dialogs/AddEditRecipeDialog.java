package com.lafestin.ui.dialogs;

import com.lafestin.model.Recipe;
import com.lafestin.ui.MainFrame;
import javax.swing.*;

public class AddEditRecipeDialog extends JDialog {

    private boolean saved = false;

    public AddEditRecipeDialog(MainFrame frame, Recipe recipe) {
        super(frame, recipe == null ? "Add Recipe" : "Edit Recipe", true);
        // TODO: build full form
        setSize(400, 200);
        setLocationRelativeTo(frame);
        add(new JLabel("Dialog coming soon", SwingConstants.CENTER));
    }

    public boolean isSaved() { return saved; }
}
package com.lafestin.ui.panels;

import com.lafestin.ui.MainFrame;
import javax.swing.*;
import java.awt.*;

/**
 * GroceryListPanel — grocery list management (stub for now).
 * Extends BaseListPanel for consistent structure when fully implemented.
 */
public class GroceryListPanel extends BaseListPanel {

    public GroceryListPanel(MainFrame frame) {
        super(frame);
    }

    @Override
    protected String getHeaderTitle() {
        return "Grocery List";
    }

    @Override
    protected String getHeaderDescription() {
        return "Create and manage shopping lists";
    }

    @Override
    protected String getSearchPlaceholder() {
        return "Search items...";
    }

    @Override
    protected JComponent buildHeaderRightControl() {
        return Box.createHorizontalBox(); // empty for now
    }

    @Override
    protected JComponent buildSearchRightControl() {
        return Box.createHorizontalBox(); // empty for now
    }

    @Override
    protected JComponent buildTableContent() {
        // Placeholder for now
        JLabel placeholder = new JLabel("Grocery list — coming soon",
            SwingConstants.CENTER);
        placeholder.setFont(new Font("SansSerif", Font.PLAIN, 14));
        return placeholder;
    }

    @Override
    protected JPanel buildToolbar() {
        JPanel empty = new JPanel();
        empty.setBackground(new Color(245, 245, 245));
        return empty;
    }
    
    @Override
    protected JButton createActionButton() {
        return new JButton("Action");
    }
    
    @Override
    protected void onAddClicked() {
        // Placeholder — grocery list not yet implemented
    }
    
    @Override
    protected void onEditClicked() {
        // Placeholder — grocery list not yet implemented
    }
    
    @Override
    protected void onActionClicked() {
        // Placeholder — grocery list not yet implemented
    }
}

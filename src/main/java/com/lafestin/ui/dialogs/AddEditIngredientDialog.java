package com.lafestin.ui.dialogs;

import com.lafestin.dao.IngredientDAO;
import com.lafestin.dao.PantryDAO;
import com.lafestin.model.Ingredient;
import com.lafestin.model.PantryItem;
import com.lafestin.ui.AppTheme;
import com.lafestin.ui.MainFrame;
import com.lafestin.helper.Helper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * AddEditIngredientDialog — modal form for adding or editing a pantry item.
 *
 */
public class AddEditIngredientDialog extends JDialog {

    // Follows similar struct to AddEditRecipe
    private final MainFrame     frame;
    private final PantryItem    existingItem; // null = add mode
    private final PantryDAO     pantryDAO;
    private final IngredientDAO ingredientDAO;

    private boolean saved = false;

    private JComboBox<Ingredient> ingredientCombo; // add mode only
    private JTextField            quantityField;
    private JComboBox<String>     unitCombo;

    private List<Ingredient> allIngredients = new ArrayList<>();

    private static final String[] UNITS = {
        "piece", "whole", "cup", "tablespoon",
        "teaspoon", "gram", "kilogram", "clove",
        "milliliter", "liter", "slice", "pinch"
    };

    public AddEditIngredientDialog(MainFrame frame, PantryItem item) {
        super(frame,
            item == null ? "Add Ingredient to Pantry" : "Edit Pantry Item",
            true);

        this.frame        = frame;
        this.existingItem = item;
        this.pantryDAO    = new PantryDAO();
        this.ingredientDAO= new IngredientDAO();

        if (item == null) {
            allIngredients = Helper.loadAllIngredients(this, ingredientDAO); // only needed in add mode
        }

        initComponents();
        prefillIfEditing();
        Helper.packAndCenter(frame, this);
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(AppTheme.BG_PAGE);

        add(buildFormPanel(), BorderLayout.CENTER);
        add(buildButtonBar(), BorderLayout.SOUTH);

        // Escape cancels
        getRootPane().registerKeyboardAction(
            e -> dispose(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private JPanel buildFormPanel() {
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(AppTheme.BG_PAGE);
        form.setBorder(BorderFactory.createEmptyBorder(24, 28, 16, 28));

        form.add(buildIngredientRow());
        form.add(Box.createVerticalStrut(14));

        form.add(buildFieldRow("Quantity", buildQuantityField()));
        form.add(Box.createVerticalStrut(14));

        form.add(buildFieldRow("Unit", buildUnitCombo()));

        return form;
    }

    private JPanel buildFieldRow(String labelText, JComponent field) {
        JPanel row = new JPanel(new BorderLayout(14, 0));
        row.setBackground(AppTheme.BG_PAGE);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel(labelText);
        label.setFont(AppTheme.FONT_LABEL);
        label.setForeground(AppTheme.TEXT_SECONDARY);
        label.setPreferredSize(new Dimension(80, 28));

        row.add(label, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);

        return row;
    }

    private JPanel buildIngredientRow() {
        JPanel row = new JPanel(new BorderLayout(14, 0));
        row.setBackground(AppTheme.BG_PAGE);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel("Ingredient");
        label.setFont(AppTheme.FONT_LABEL);
        label.setForeground(AppTheme.TEXT_SECONDARY);
        label.setPreferredSize(new Dimension(80, 28));

        row.add(label, BorderLayout.WEST);

        if (existingItem == null) {
            // ADD mode: dropdown of all ingredients
            ingredientCombo = new JComboBox<>();
            for (Ingredient i : allIngredients) {
                ingredientCombo.addItem(i);
            }
            ingredientCombo.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(
                        JList<?> list, Object value, int index,
                        boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                    if (value instanceof Ingredient) {
                        setText(Helper.capitalize(
                            ((Ingredient) value).getName()));
                    }
                    return this;
                }
            });
            ingredientCombo.setFont(AppTheme.FONT_BODY);
            ingredientCombo.setBackground(AppTheme.BG_SURFACE);

            // When ingredient changes, auto-set a sensible default unit
            ingredientCombo.addActionListener(e -> suggestUnit());

            row.add(ingredientCombo, BorderLayout.CENTER);

        } else {
            // EDIT mode: locked label — ingredient cannot change 
            JPanel lockedPanel = new JPanel(new BorderLayout(8, 0));
            lockedPanel.setBackground(AppTheme.BG_PAGE);

            JLabel nameLabel = new JLabel(
                Helper.capitalize(existingItem.getIngredientName()));
            nameLabel.setFont(AppTheme.FONT_BODY);
            nameLabel.setForeground(AppTheme.TEXT_PRIMARY);

            JLabel lockedBadge = new JLabel("locked");
            lockedBadge.setFont(AppTheme.FONT_TINY);
            lockedBadge.setForeground(AppTheme.TEXT_MUTED);
            lockedBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BG_BORDER, 1),
                BorderFactory.createEmptyBorder(1, 6, 1, 6)));

            lockedPanel.add(nameLabel,   BorderLayout.WEST);
            lockedPanel.add(lockedBadge, BorderLayout.CENTER);

            row.add(lockedPanel, BorderLayout.CENTER);
        }

        return row;
    }

    // Quantity field
    private JTextField buildQuantityField() {
        quantityField = new JTextField();
        quantityField.setFont(AppTheme.FONT_BODY);
        quantityField.setBackground(AppTheme.BG_SURFACE);
        quantityField.setForeground(AppTheme.TEXT_PRIMARY);
        quantityField.setBorder(AppTheme.BORDER_INPUT);

        // Allow only digits, dot, and control keys
        quantityField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                String current = quantityField.getText();
                // Allow digits
                if (Character.isDigit(c)) return;
                // Allow one decimal point
                if (c == '.' && !current.contains(".")) return;
                // Allow control characters (backspace, delete, etc.)
                if (Character.isISOControl(c)) return;
                // Block everything else
                e.consume();
            }
        });

        return quantityField;
    }

    // Unit combo
    private JComboBox<String> buildUnitCombo() {
        unitCombo = new JComboBox<>(UNITS);
        unitCombo.setFont(AppTheme.FONT_BODY);
        unitCombo.setBackground(AppTheme.BG_SURFACE);
        return unitCombo;
    }

    // Auto-suggest a unit when ingredient selection changes
    // Maps common ingredient names to their natural unit.
    // Falls back to "piece" for unknown ingredients.
    private void suggestUnit() {
        if (ingredientCombo == null
                || ingredientCombo.getSelectedItem() == null) return;

        String name = ((Ingredient) ingredientCombo.getSelectedItem())
            .getName().toLowerCase();

        String suggested;
        if (name.contains("egg"))                         suggested = "whole";
        else if (name.contains("garlic"))                 suggested = "clove";
        else if (name.contains("flour")
              || name.contains("rice")
              || name.contains("milk"))                   suggested = "cup";
        else if (name.contains("butter")
              || name.contains("oil")
              || name.contains("sauce")
              || name.contains("vinegar")
              || name.contains("soy"))                    suggested = "tablespoon";
        else if (name.contains("salt")
              || name.contains("pepper")
              || name.contains("paprika")
              || name.contains("spice"))                  suggested = "teaspoon";
        else if (name.contains("beef")
              || name.contains("pork")
              || name.contains("chicken")
              || name.contains("fish")
              || name.contains("meat"))                   suggested = "gram";
        else                                              suggested = "piece";

        unitCombo.setSelectedItem(suggested);
    }


    private void prefillIfEditing() {
        if (existingItem == null) return;

        // Quantity — format whole numbers without .0
        double qty = existingItem.getQuantity();
        quantityField.setText(
            qty == Math.floor(qty)
                ? String.valueOf((int) qty)
                : String.valueOf(qty));

        // Unit — select matching unit in combo
        unitCombo.setSelectedItem(existingItem.getUnit());

        // If the unit from the DB isn't in our list, add it dynamically
        if (!existingItem.getUnit().equals(unitCombo.getSelectedItem())) {
            unitCombo.addItem(existingItem.getUnit());
            unitCombo.setSelectedItem(existingItem.getUnit());
        }
    }

    private JPanel buildButtonBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bar.setBackground(AppTheme.BG_SURFACE);
        bar.setBorder(BorderFactory.createCompoundBorder(
            AppTheme.BORDER_DIVIDER_TOP,
            BorderFactory.createEmptyBorder(12, 20, 12, 20)));

        JButton cancelBtn = AppTheme.secondaryButton("Cancel");
        JButton saveBtn   = AppTheme.primaryButton(
            existingItem == null ? "Add to Pantry" : "Update");

        cancelBtn.addActionListener(e -> dispose());
        saveBtn.addActionListener(  e -> savePantryItem());

        getRootPane().setDefaultButton(saveBtn);

        bar.add(cancelBtn);
        bar.add(saveBtn);

        return bar;
    }

    private void savePantryItem() {
        String error = validateForm();
        if (error != null) {
            JOptionPane.showMessageDialog(this,
                error, "Invalid Input",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        double qty  = Double.parseDouble(
            quantityField.getText().trim());
        String unit = (String) unitCombo.getSelectedItem();

        try {
            if (existingItem == null) {
                Ingredient selected =
                    (Ingredient) ingredientCombo.getSelectedItem();

                if (selected == null) {
                    JOptionPane.showMessageDialog(this,
                        "Please select an ingredient.",
                        "Invalid Input",
                        JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // Check if already in pantry — use addOrUpdate
                // to avoid duplicate PK exception
                PantryItem newItem = new PantryItem(
                    selected.getIngredientId(),
                    frame.getCurrentUserId(),
                    qty, unit,
                    selected.getName());

                pantryDAO.addOrUpdate(newItem);

            } else {
                // EDIT mode
                // Only quantity and unit can change — ingredient is locked
                pantryDAO.updateQuantityAndUnit(
                    existingItem.getIngredientId(),
                    frame.getCurrentUserId(),
                    qty, unit);
            }

            saved = true;
            dispose();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Failed to save pantry item: " + e.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private String validateForm() {
        String qtyText = quantityField.getText().trim();

        if (qtyText.isEmpty()) {
            return "Quantity is required.";
        }
        try {
            double qty = Double.parseDouble(qtyText);
            if (qty <= 0) {
                return "Quantity must be greater than 0.";
            }
            if (qty > 99999) {
                return "Quantity seems too large. Please check the value.";
            }
        } catch (NumberFormatException e) {
            return "Quantity must be a number (e.g. 3 or 0.5).";
        }
        if (existingItem == null && ingredientCombo.getItemCount() == 0) {
            return "No ingredients available.\n"
                + "Add ingredients to the database first.";
        }
        return null;
    }

    // ── Public API ────────────────────────────────────────────────────────
    public boolean isSaved() { return saved; }
}
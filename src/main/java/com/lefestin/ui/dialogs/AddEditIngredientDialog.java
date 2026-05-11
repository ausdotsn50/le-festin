package com.lefestin.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.SQLException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import com.lefestin.dao.impl.IngredientDAOImpl;
import com.lefestin.dao.impl.PantryDAOImpl;
import com.lefestin.helper.Helper;
import com.lefestin.model.Ingredient;
import com.lefestin.model.PantryItem;
import com.lefestin.service.ConversionService;
import com.lefestin.ui.AppTheme;
import com.lefestin.ui.MainFrame;

/**
 * AddEditIngredientDialog — modal form for adding or editing a pantry item.
 */
public class AddEditIngredientDialog extends JDialog {

    private final MainFrame     frame;
    private final PantryItem    existingItem; // null = add mode
    private final PantryDAOImpl     pantryDAO;
    private final IngredientDAOImpl ingredientDAO;
    private final ConversionService conversionService;

    private boolean saved = false;

    private JTextField        ingredientNameField; // Replaced JComboBox with JTextField
    private JTextField        quantityField;
    private JComboBox<String> unitCombo;

    public AddEditIngredientDialog(MainFrame frame, PantryItem item) {
        super(frame,
            item == null ? "Add Ingredient to Pantry" : "Edit Pantry Item",
            true);

        this.frame        = frame;
        this.existingItem = item;
        this.pantryDAO    = new PantryDAOImpl();
        this.ingredientDAO= new IngredientDAOImpl();
        this.conversionService = new ConversionService();

        initComponents();
        prefillIfEditing();
        Helper.packAndCenter(frame, this, new Dimension(350, 400));
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
            // ADD mode: Text input box for free typing
            ingredientNameField = new JTextField();
            ingredientNameField.setFont(AppTheme.FONT_BODY);
            ingredientNameField.setBackground(AppTheme.BG_SURFACE);
            ingredientNameField.setForeground(AppTheme.TEXT_PRIMARY);
            ingredientNameField.setBorder(AppTheme.BORDER_INPUT);

            // Auto-suggest unit when the user clicks out of the ingredient text box
            ingredientNameField.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    suggestUnit();
                }
            });

            row.add(ingredientNameField, BorderLayout.CENTER);

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
        unitCombo = new JComboBox<>(Helper.UNITS);
        unitCombo.setFont(AppTheme.FONT_BODY);
        unitCombo.setBackground(AppTheme.BG_SURFACE);
        unitCombo.setForeground(AppTheme.TEXT_PRIMARY);
        unitCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
                if (isSelected) {
                    setBackground(AppTheme.SELECTION_BG);
                    setForeground(AppTheme.SELECTION_FG);
                } else {
                    setBackground(AppTheme.BG_SURFACE);
                    setForeground(AppTheme.TEXT_PRIMARY);
                }
                return this;
            }
        });
        return unitCombo;
    }

    // Auto-suggest a unit based on typed string
    private void suggestUnit() {
        if (existingItem != null || ingredientNameField == null) return;
        
        String name = ingredientNameField.getText().trim().toLowerCase();
        if (name.isEmpty()) return;

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
        ConversionService.NormalizedAmount normalizedInput =
            conversionService.normalize(qty, unit);

        try {
            if (existingItem == null) {
                // Get name from text field
                String typedName = ingredientNameField.getText().trim();

                // Safely find the ingredient or create it in the database
                Ingredient selected = ingredientDAO.findOrCreate(typedName);

                // Check if already in pantry — add to existing quantity
                PantryItem existing = pantryDAO.getPantryItem(
                    selected.getIngredientId(),
                    frame.getCurrentUserId());

                double finalQty = normalizedInput.getQuantity();
                String finalUnit = normalizedInput.getUnit();

                if (existing != null) {
                    ConversionService.NormalizedAmount normalizedExisting =
                        conversionService.normalize(
                            existing.getQuantity(),
                            existing.getUnit());

                    if (!normalizedExisting.getUnit()
                            .equalsIgnoreCase(finalUnit)) {
                        JOptionPane.showMessageDialog(this,
                            "Existing pantry unit cannot be merged with the chosen unit.",
                            "Incompatible Units",
                            JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    finalQty += normalizedExisting.getQuantity();
                }

                PantryItem newItem = new PantryItem(
                    selected.getIngredientId(),
                    frame.getCurrentUserId(),
                    finalQty, finalUnit,
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
        // Validate ingredient name first if adding
        if (existingItem == null) {
            if (ingredientNameField.getText().trim().isEmpty()) {
                return "Ingredient name is required.";
            }
        }

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
        
        return null; // All valid
    }

    // ── Public API ────────────────────────────────────────────────────────
    public boolean isSaved() { return saved; }
}
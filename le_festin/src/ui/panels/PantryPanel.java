package ui.panels;

import dao.impl.PantryDAOImpl;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import model.PantryItem;
import ui.AppTheme;
import ui.MainFrame;
import ui.dialogs.AddEditIngredientDialog;

/**
 * PantryPanel — the virtual pantry browser utilizing a modern card system.
 */
public class PantryPanel extends BaseListPanel {

    private final PantryDAOImpl pantryDAO;
    private List<PantryItem> allItems = new ArrayList<>();
    private JPanel cardsContainer;

    public PantryPanel(MainFrame frame) {
        super(frame);
        this.pantryDAO = new PantryDAOImpl();
    }

    // --- Header Configuration ---
    @Override
    protected String getHeaderTitle() { return "My Pantry"; }
    @Override
    protected String getHeaderDescription() { return "Ingredients you currently have at home"; }
    @Override
    protected String getSearchPlaceholder() { return "Search ingredients..."; }
    
    @Override
    protected JComponent buildHeaderRightControl() {
        countLabel = new JLabel("0 ingredients");
        countLabel.setFont(AppTheme.FONT_SMALL);
        countLabel.setForeground(AppTheme.TEXT_MUTED);
        return countLabel;
    }
    
    @Override
    protected JComponent buildSearchRightControl() {
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        controls.setBackground(AppTheme.BG_SURFACE);

        JButton addButton = AppTheme.primaryButton("+ Add");
        addButton.setToolTipText("Add a new ingredient to your pantry");
        addButton.addActionListener(e -> onAddClicked());

        JButton matchBtn = AppTheme.secondaryButton("Match Recipes");
        matchBtn.setToolTipText("Find recipes you can make with your current pantry");
        matchBtn.addActionListener(e -> navigateToSuggestions());

        controls.add(addButton);
        controls.add(matchBtn);

        return controls;
    }

    // --- UI Structure ---
    @Override
    protected JComponent buildTableContent() {
        // Setup structural search document filtering hook
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterCards(); }
            public void removeUpdate(DocumentEvent e) { filterCards(); }
            public void changedUpdate(DocumentEvent e) { filterCards(); }
        });

        cardsContainer = new JPanel();
        cardsContainer.setLayout(new BoxLayout(cardsContainer, BoxLayout.Y_AXIS));
        cardsContainer.setBackground(AppTheme.BG_PAGE);
        cardsContainer.setAlignmentX(Component.LEFT_ALIGNMENT);

        JScrollPane scrollPane = new JScrollPane(cardsContainer);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(AppTheme.BG_PAGE);

        return scrollPane;
    }

    @Override
    protected JPanel buildToolbar() {
        JPanel empty = new JPanel();
        empty.setBackground(AppTheme.BG_PAGE);
        return empty;
    }

    @Override
    protected JButton createActionButton() {
        return AppTheme.dangerButton("Remove");
    }

    // --- Actions ---
    @Override
    protected void onAddClicked() {
        AddEditIngredientDialog dialog = new AddEditIngredientDialog(frame, null);
        dialog.setVisible(true);
        if (dialog.isSaved()) loadPantry();
    }

    @Override
    protected void onEditClicked() {
        // BaseListPanel abstract signature requirement placeholder
    }

    private void onEditCardItem(PantryItem item) {
        if (item != null) {
            AddEditIngredientDialog dialog = new AddEditIngredientDialog(frame, item);
            dialog.setVisible(true);
            if (dialog.isSaved()) loadPantry();
        }
    }

    @Override
    protected void onActionClicked() {
        // BaseListPanel abstract signature requirement placeholder
    }

    // --- Card Construction & Rendering ---
    private void renderCards(List<PantryItem> items) {
        cardsContainer.removeAll();
        for (int i = 0; i < items.size(); i++) {
            cardsContainer.add(createIngredientCard(items.get(i)));
            if (i < items.size() - 1) {
                cardsContainer.add(Box.createVerticalStrut(12));
            }
        }
        cardsContainer.revalidate();
        cardsContainer.repaint();
    }

    private JPanel createIngredientCard(PantryItem item) {
        JPanel card = new JPanel(new BorderLayout(15, 0));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        card.setPreferredSize(new Dimension(300, 90));
        card.setBackground(AppTheme.BG_SURFACE);
        card.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

        final Border defaultBorder = new CompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BG_BORDER, 1, true),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
        );
        final Border hoverBorder = new CompoundBorder(
                BorderFactory.createLineBorder(AppTheme.ACCENT_GOLD, 1, true),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
        );
        card.setBorder(defaultBorder);

        JPanel textContainer = new JPanel();
        textContainer.setLayout(new BoxLayout(textContainer, BoxLayout.Y_AXIS));
        textContainer.setOpaque(false);

        // Capitalized Item Name Processing
        String rawName = item.getIngredientName();
        String formattedName = (rawName != null && !rawName.isEmpty())
                ? Character.toUpperCase(rawName.charAt(0)) + rawName.substring(1)
                : "";

        JLabel titleLbl = new JLabel("<html>" + formattedName + "</html>");
        titleLbl.setFont(AppTheme.FONT_CARD_TITLE);
        titleLbl.setForeground(AppTheme.TEXT_SECONDARY);

        // Display Quantity & Unit
        JLabel quantityLbl = new JLabel(item.getFormattedQuantity() + " " + item.getUnit());
        quantityLbl.setFont(AppTheme.FONT_SMALL);
        quantityLbl.setForeground(AppTheme.TEXT_MUTED);

        textContainer.add(Box.createVerticalGlue());
        textContainer.add(titleLbl);
        textContainer.add(Box.createVerticalStrut(5));
        textContainer.add(quantityLbl);
        textContainer.add(Box.createVerticalGlue());

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        rightActions.setOpaque(false);
        JPanel rightWrapper = new JPanel(new BorderLayout());
        rightWrapper.setOpaque(false);

        JButton editBtn = AppTheme.compactSecondaryButton("Edit");
        editBtn.addActionListener(e -> onEditCardItem(item));

        JButton deleteBtn = AppTheme.compactDangerButton("Remove");
        deleteBtn.addActionListener(e -> removeCardItem(item));

        rightActions.add(editBtn);
        rightActions.add(deleteBtn);
        rightWrapper.add(rightActions, BorderLayout.NORTH);

        card.add(textContainer, BorderLayout.CENTER);
        card.add(rightWrapper, BorderLayout.EAST);

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { 
                if (e.getClickCount() == 2) {
                    onEditCardItem(item); 
                }
            }
            @Override
            public void mouseEntered(MouseEvent e) { card.setBorder(hoverBorder); }
            @Override
            public void mouseExited(MouseEvent e) { card.setBorder(defaultBorder); }
        });

        return card;
    }

    // --- Data Management & Filtering ---
    public void loadPantry() {
        try {
            allItems = pantryDAO.getPantryByUser(frame.getCurrentUserId());
            filterCards();
        } catch (SQLException e) {
            handleError("Failed to load pantry", e);
        }
    }

    private void filterCards() {
        String query = searchField.getText().toLowerCase().trim();
        List<PantryItem> filtered;
        
        if (query.isEmpty()) {
            filtered = allItems;
        } else {
            filtered = allItems.stream()
                .filter(item -> item.getIngredientName().toLowerCase().contains(query))
                .collect(Collectors.toList());
        }
        
        renderCards(filtered);
        updateCountLabelDisplay(filtered.size(), allItems.size());
    }

    private void removeCardItem(PantryItem item) {
        if (item == null) return;

        int confirm = JOptionPane.showConfirmDialog(this,
            "Remove \"" + item.getIngredientName() + "\" from your pantry?",
            "Confirm Remove", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                pantryDAO.deletePantryItem(item.getIngredientId(), frame.getCurrentUserId());
                loadPantry();
            } catch (SQLException e) {
                handleError("Failed to remove item", e);
            }
        }
    }

    // --- Helpers ---
    private void updateCountLabelDisplay(int visibleCount, int totalCount) {
        String text = (visibleCount == totalCount) 
            ? totalCount + " ingredient" + (totalCount == 1 ? "" : "s")
            : visibleCount + " of " + totalCount + " ingredients";
        updateCountLabel(text);
    }

    private void navigateToSuggestions() { frame.navigateTo(MainFrame.CARD_SUGGESTIONS); }

    private void handleError(String message, Exception e) {
        JOptionPane.showMessageDialog(this, message + ": " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) loadPantry();
    }
}
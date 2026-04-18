package com.lafestin.ui.panels;

import com.lafestin.ui.AppTheme;
import com.lafestin.ui.MainFrame;

import javax.swing.*;
import java.awt.*;

/**
 * BaseListPanel — abstract base class for list/table panels.
 *
 * Provides common structure: header (title + description) + search bar + right control
 * Subclasses extend this to customize the table, toolbar, and right-side control.
 */
public abstract class BaseListPanel extends JPanel {
    
    protected final MainFrame frame;
    protected JTextField searchField;

    public BaseListPanel(MainFrame frame) {
        this.frame = frame;
        
        setLayout(new BorderLayout(0, 0));
        setBackground(AppTheme.BG_PAGE);

        add(buildHeaderAndSearch(), BorderLayout.NORTH);
        add(buildTableContent(), BorderLayout.CENTER);
        add(buildToolbar(), BorderLayout.SOUTH);
    }

    /**
     * Builds header (title + description) and search bar combined.
     */
    private JPanel buildHeaderAndSearch() {
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(AppTheme.BG_SURFACE);
        
        // Header: title + description (top)
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(AppTheme.BG_SURFACE);
        header.setBorder(BorderFactory.createEmptyBorder(16, 20, 12, 20));
        
        // Title and description on the left
        JPanel titleGroup = new JPanel();
        titleGroup.setLayout(new BoxLayout(titleGroup, BoxLayout.Y_AXIS));
        titleGroup.setBackground(AppTheme.BG_SURFACE);
        
        JLabel title = new JLabel(getHeaderTitle());
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(new Color(30, 30, 30));
        
        JLabel subtitle = new JLabel(getHeaderDescription());
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitle.setForeground(new Color(140, 140, 140));
        
        titleGroup.add(title);
        titleGroup.add(Box.createVerticalStrut(2));
        titleGroup.add(subtitle);
        
        header.add(titleGroup, BorderLayout.WEST);
        header.add(buildHeaderRightControl(), BorderLayout.EAST);
        
        // Search bar (bottom)
        JPanel searchBar = new JPanel(new BorderLayout(10, 0));
        searchBar.setBackground(AppTheme.BG_SURFACE);
        searchBar.setBorder(BorderFactory.createEmptyBorder(0, 20, 16, 20));
        
        searchField = new JTextField();
        searchField.setFont(AppTheme.FONT_BODY);
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppTheme.BG_BORDER, 1, true),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        searchField.putClientProperty("JTextField.placeholderText", getSearchPlaceholder());
        
        searchBar.add(searchField, BorderLayout.CENTER);
        searchBar.add(buildSearchRightControl(), BorderLayout.EAST);
        
        container.add(header, BorderLayout.NORTH);
        container.add(searchBar, BorderLayout.SOUTH);
        container.setBorder(BorderFactory.createCompoundBorder(
            AppTheme.BORDER_DIVIDER_TOP,
            BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));
        
        return container;
    }

    /**
     * Abstract methods for subclasses to implement
     */
    
    /** Return the header title (e.g., "My Pantry", "Recipes") */
    protected abstract String getHeaderTitle();
    
    /** Return the header description (e.g., "Ingredients you currently have...") */
    protected abstract String getHeaderDescription();
    
    /** Return search placeholder text */
    protected abstract String getSearchPlaceholder();
    
    /** Build the right-side control in the header (e.g., "Match Recipes" button) */
    protected abstract JComponent buildHeaderRightControl();
    
    /** Build the right-side control next to search bar (e.g., category dropdown) */
    protected abstract JComponent buildSearchRightControl();
    
    /** Build the table/content area */
    protected abstract JComponent buildTableContent();
    
    /** Build the toolbar with buttons */
    protected abstract JPanel buildToolbar();
}

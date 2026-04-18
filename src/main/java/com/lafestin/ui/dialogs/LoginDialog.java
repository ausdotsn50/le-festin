package com.lafestin.ui.dialogs;

import com.lafestin.helper.Helper;
import com.lafestin.model.User;
import com.lafestin.service.AuthService;
import com.lafestin.service.AuthService.AuthResult;
import com.lafestin.ui.AppTheme;
import com.lafestin.ui.MainFrame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * LoginDialog — shown before MainFrame becomes visible.
 *
 * On login success  → frame.setCurrentUser(user), dispose()
 * On login failure  → JOptionPane error, fields stay open
 * On register click → RegisterPanel slides in replacing the form
 * On window close   → System.exit(0) — app cannot run without a user
 */
public class LoginDialog extends JDialog {
    private final MainFrame frame;
    private final AuthService authService;

    // Login form fields 
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginBtn;
    private JLabel errorLabel;

    // Register form fields
    private JTextField regUsernameField;
    private JPasswordField regPasswordField;
    private JPasswordField regConfirmField;
    private JLabel regErrorLabel;

    // Card panel that switches between login and register views
    private JScrollPane cardPanel;  // JScrollPane wrapper
    private JPanel cardContentPanel;  // Inner panel with CardLayout
    private CardLayout cardLayout;

    private static final String CARD_LOGIN = "login";
    private static final String CARD_REGISTER = "register";

    public LoginDialog(MainFrame frame) {
        super(frame, "La Festin", true);

        this.frame = frame;
        this.authService = new AuthService(); // Opens an auth service

        // Closing the dialog exits the app — no user = app can't run
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        initComponents();
        Helper.packAndCenter(frame, this, new Dimension(380, 460));
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(AppTheme.BG_PAGE);

        // Create inner content panel with CardLayout
        cardLayout = new CardLayout();
        cardContentPanel = new JPanel(cardLayout) {
            @Override
            public Dimension getPreferredSize() {
                // Get the preferred size of the current card
                Dimension d = super.getPreferredSize();
                // Ensure it fills the scroll pane width
                d.width = 380; // Match dialog width minus padding
                return d;
            }
            
            @Override
            public Dimension getMaximumSize() {
                return getPreferredSize();
            }
        };
        cardContentPanel.setBackground(AppTheme.BG_PAGE);

        cardContentPanel.add(buildLoginPanel(), CARD_LOGIN);
        cardContentPanel.add(buildRegisterPanel(), CARD_REGISTER);

        // Wrap in JScrollPane (vertical scrolling only)
        cardPanel = new JScrollPane(cardContentPanel,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        cardPanel.setBackground(AppTheme.BG_PAGE);
        cardPanel.getViewport().setBackground(AppTheme.BG_PAGE);
        cardPanel.setBorder(BorderFactory.createEmptyBorder());

        add(buildBrandingHeader(), BorderLayout.NORTH);
        add(cardPanel, BorderLayout.CENTER);

        cardLayout.show(cardContentPanel, CARD_LOGIN);
    }
    
    // La Festin
    // Your recipe companion - header component
    private JPanel buildBrandingHeader() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(AppTheme.HEADER_BG);
        header.setBorder(BorderFactory.createEmptyBorder(28, 0, 24, 0));

        JLabel appName = new JLabel("La Festin", SwingConstants.CENTER);
        appName.setFont(new Font("Serif", Font.BOLD, 28));
        appName.setForeground(AppTheme.HEADER_FG);
        appName.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel tagline = new JLabel(
            "Your recipe companion", SwingConstants.CENTER);
        tagline.setFont(AppTheme.FONT_SMALL);
        tagline.setForeground(AppTheme.HEADER_FG_MUTED);
        tagline.setAlignmentX(Component.CENTER_ALIGNMENT);

        header.add(appName);
        header.add(Box.createVerticalStrut(4));
        header.add(tagline);

        return header;
    }

    // To do: bug fix on buildLogin panel
    // Allows ctrl+ options (for JTextField)
    // Use ctrl+ instead of cmd+ on MacOS for JTextfield
    private JPanel buildLoginPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(AppTheme.BG_PAGE);
        panel.setBorder(BorderFactory.createEmptyBorder(28, 36, 20, 36));

        // Username
        panel.add(buildInputLabel("Username"));
        panel.add(Box.createVerticalStrut(4));
        usernameField = buildTextField("Enter username");
        panel.add(usernameField);
        panel.add(Box.createVerticalStrut(14));

        // Password
        panel.add(buildInputLabel("Password"));
        panel.add(Box.createVerticalStrut(4));
        passwordField = buildPasswordField("Enter password");
        panel.add(passwordField);
        panel.add(Box.createVerticalStrut(6));

        // Error label — hidden until login fails
        errorLabel = buildErrorLabel();
        panel.add(errorLabel);
        panel.add(Box.createVerticalStrut(16));

        // Login button
        loginBtn = AppTheme.primaryButton("Sign In");
        loginBtn.setPreferredSize(new Dimension(308, 40));
        loginBtn.addActionListener(e -> attemptLogin());
        JPanel loginBtnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        loginBtnPanel.setBackground(AppTheme.BG_PAGE);
        loginBtnPanel.add(loginBtn);
        panel.add(loginBtnPanel);
        panel.add(Box.createVerticalStrut(18));

        // Register link
        panel.add(buildRegisterLink());

        // Enter key triggers login from any field
        usernameField.addActionListener(e -> attemptLogin());
        passwordField.addActionListener(e -> attemptLogin());

        return panel;
    }

    private JPanel buildRegisterPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(AppTheme.BG_PAGE);
        panel.setBorder(BorderFactory.createEmptyBorder(24, 36, 20, 36));

        // Username 
        panel.add(buildInputLabel("Choose a username"));
        panel.add(Box.createVerticalStrut(4));
        regUsernameField = buildTextField("At least 3 characters");
        panel.add(regUsernameField);
        panel.add(Box.createVerticalStrut(12));

        // Password
        panel.add(buildInputLabel("Password"));
        panel.add(Box.createVerticalStrut(4));
        regPasswordField = buildPasswordField("At least 6 characters");
        panel.add(regPasswordField);
        panel.add(Box.createVerticalStrut(12));

        // Confirm pass
        panel.add(buildInputLabel("Confirm password"));
        panel.add(Box.createVerticalStrut(4));
        regConfirmField = buildPasswordField("Re-enter password");
        panel.add(regConfirmField);
        panel.add(Box.createVerticalStrut(6));

        // Error label
        regErrorLabel = buildErrorLabel();
        panel.add(regErrorLabel);
        panel.add(Box.createVerticalStrut(14));

        // Reg button
        JButton registerBtn = AppTheme.primaryButton("Create Account");
        registerBtn.setPreferredSize(new Dimension(308, 40));
        registerBtn.addActionListener(e -> attemptRegister());
        JPanel registerBtnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        registerBtnPanel.setBackground(AppTheme.BG_PAGE);
        registerBtnPanel.add(registerBtn);
        panel.add(registerBtnPanel);
        panel.add(Box.createVerticalStrut(14));

        // Back to login
        panel.add(buildBackToLoginLink());

        // Enter key triggers register
        regConfirmField.addActionListener(e -> attemptRegister());

        return panel;
    }

    // "Don't have an account? Register" footer
    private JPanel buildRegisterLink() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        row.setBackground(AppTheme.BG_PAGE);
        row.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel prompt = new JLabel("Don't have an account?");
        prompt.setFont(AppTheme.FONT_SMALL);
        prompt.setForeground(AppTheme.TEXT_MUTED);

        JButton link = buildLinkButton("Register");
        link.addActionListener(e -> showRegisterPanel());

        row.add(prompt);
        row.add(link);
        return row;
    }

    // "Already have an account? Sign in" footer
    private JPanel buildBackToLoginLink() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        row.setBackground(AppTheme.BG_PAGE);
        row.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel prompt = new JLabel("Already have an account?");
        prompt.setFont(AppTheme.FONT_SMALL);
        prompt.setForeground(AppTheme.TEXT_MUTED);

        JButton link = buildLinkButton("Sign in");
        link.addActionListener(e -> showLoginPanel());

        row.add(prompt);
        row.add(link);
        return row;
    }

    private void attemptLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        // Clear previous error
        showError(errorLabel, null);

        // Basic blank check before hitting the service
        if (username.isEmpty()) {
            showError(errorLabel, "Please enter your username.");
            usernameField.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            showError(errorLabel, "Please enter your password.");
            passwordField.requestFocus();
            return;
        }

        // Disable button during auth to prevent double-clicks
        loginBtn.setEnabled(false);
        loginBtn.setText("Signing in...");

        // Run on a background thread — BCrypt.checkpw takes ~300ms
        // and would freeze the EDT if called directly
        SwingWorker<AuthResult, Void> worker = new SwingWorker<>() {
            @Override
            protected AuthResult doInBackground() {
                return authService.login(username, password);
            }

            @Override
            protected void done() {
                loginBtn.setEnabled(true);
                loginBtn.setText("Sign In");

                try {
                    AuthResult result = get();
                    if (result.isSuccess()) {
                        onLoginSuccess(result.getUser());
                    } else {
                        showError(errorLabel, result.getMessage());
                        // Clear password field on failure
                        passwordField.setText("");
                        passwordField.requestFocus();
                    }
                } catch (Exception ex) {
                    showError(errorLabel,
                        "Unexpected error: " + ex.getMessage());
                }
            }
        };

        worker.execute();
    }

    private void attemptRegister() {
        String username = regUsernameField.getText().trim();
        String password = new String(regPasswordField.getPassword());
        String confirm  = new String(regConfirmField.getPassword());

        showError(regErrorLabel, null);

        // Client-side confirm check before hitting AuthService
        if (!password.equals(confirm)) {
            showError(regErrorLabel, "Passwords do not match.");
            regConfirmField.setText("");
            regConfirmField.requestFocus();
            return;
        }

        AuthResult result = authService.register(username, password);

        if (result.isSuccess()) {
            // Auto-login after successful registration
            onLoginSuccess(result.getUser());
        } else {
            showError(regErrorLabel, result.getMessage());
        }
    }

    // Called on both successful login and successful registration
    private void onLoginSuccess(User user) {
        frame.setCurrentUser(user);
        dispose();
    }

    // Card nav
    private void showRegisterPanel() {
        showError(errorLabel, null);
        clearRegisterFields();
        cardLayout.show(cardContentPanel, CARD_REGISTER);
        pack();
        setLocationRelativeTo(null);
        regUsernameField.requestFocus();
    }

    private void showLoginPanel() {
        showError(regErrorLabel, null);
        cardLayout.show(cardContentPanel, CARD_LOGIN);
        pack();
        setLocationRelativeTo(null);
        usernameField.requestFocus();
    }

    private JLabel buildInputLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(AppTheme.FONT_LABEL);
        label.setForeground(AppTheme.TEXT_SECONDARY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JTextField buildTextField(String placeholder) {
        JTextField field = new JTextField();
        field.setFont(AppTheme.FONT_BODY);
        field.setBackground(AppTheme.BG_SURFACE);
        field.setForeground(AppTheme.TEXT_PRIMARY);
        field.setBorder(AppTheme.BORDER_INPUT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.putClientProperty(
            "JTextField.placeholderText", placeholder);
        return field;
    }

    private JPasswordField buildPasswordField(String placeholder) {
        JPasswordField field = new JPasswordField();
        field.setFont(AppTheme.FONT_BODY);
        field.setBackground(AppTheme.BG_SURFACE);
        field.setForeground(AppTheme.TEXT_PRIMARY);
        field.setBorder(AppTheme.BORDER_INPUT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.putClientProperty(
            "JTextField.placeholderText", placeholder);
        return field;
    }

    private JLabel buildErrorLabel() {
        JLabel label = new JLabel(" "); // space keeps layout stable
        label.setFont(AppTheme.FONT_SMALL);
        label.setForeground(AppTheme.TERRA_PRIMARY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        label.setVisible(false);
        return label;
    }

    // Link-style button — no border, no background, colored text 
    private JButton buildLinkButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(AppTheme.FONT_SMALL);
        btn.setForeground(AppTheme.GREEN_PRIMARY);
        btn.setBackground(AppTheme.BG_PAGE);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        // Underline on hover
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setText("<html><u>" + text + "</u></html>");
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setText(text);
            }
        });

        return btn;
    }

    // Helper funcs LoginDialog.java
    /** Shows an error message inline, or hides the label if msg is null. */
    private void showError(JLabel label, String msg) {
        if (msg == null || msg.isBlank()) {
            label.setText(" ");
            label.setVisible(false);
        } else {
            // Wrap in HTML to enable text wrapping within max width
            label.setText("<html>" + msg + "</html>");
            label.setVisible(true);
        }
    }

    private void clearRegisterFields() {
        regUsernameField.setText("");
        regPasswordField.setText("");
        regConfirmField.setText("");
    }
}
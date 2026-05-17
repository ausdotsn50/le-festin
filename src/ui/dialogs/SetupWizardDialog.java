package com.lefestin.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;

import org.mindrot.jbcrypt.BCrypt;

import javax.swing.JFrame;

import com.lefestin.helper.Helper;
import com.lefestin.ui.AppTheme;

/**
 * SetupWizardDialog — first-run setup: DB credentials, schema init, optional seed data.
 * Shown by Main when resources/config.properties is absent or empty.
 * Owner may be null (centers on screen) so it can be shown before MainFrame is built.
 */
public class SetupWizardDialog extends JDialog {

    // Step 1 — credentials
    private JTextField     hostField;
    private JTextField     portField;
    private JTextField     databaseField;
    private JTextField     usernameField;
    private JPasswordField passwordField;
    private JLabel         connectionStatusLabel;
    private JButton        testBtn;
    private JButton        nextBtn;

    // Step 2 — initialization
    private JCheckBox seedCheck;
    private JTextArea logArea;
    private JButton   initBtn;
    private JButton   backBtn;

    // Navigation
    private JPanel     cards;
    private CardLayout cardLayout;
    private JLabel     stepIndicator;
    private static final String STEP1 = "step1";
    private static final String STEP2 = "step2";

    private boolean setupComplete = false;

    public SetupWizardDialog(JFrame owner) {
        super(owner, "Le Festin — First-Time Setup", true);
        initComponents();
        Helper.packAndCenter(owner, this, new Dimension(480, 500));
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(AppTheme.BG_PAGE);

        add(buildTopBar(), BorderLayout.NORTH);

        cardLayout = new CardLayout();
        cards = new JPanel(cardLayout);
        cards.setBackground(AppTheme.BG_PAGE);
        cards.add(buildStep1(), STEP1);
        cards.add(buildStep2(), STEP2);
        add(cards, BorderLayout.CENTER);

        getRootPane().registerKeyboardAction(
            e -> cancelSetup(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(AppTheme.BG_SURFACE);
        bar.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));

        JLabel title = new JLabel("Le Festin Setup");
        title.setFont(AppTheme.FONT_SUBTITLE);
        title.setForeground(AppTheme.TEXT_PRIMARY);

        stepIndicator = new JLabel("Step 1 of 2 — Database Connection");
        stepIndicator.setFont(AppTheme.FONT_SMALL);
        stepIndicator.setForeground(AppTheme.TEXT_MUTED);

        JPanel titleGroup = new JPanel();
        titleGroup.setLayout(new BoxLayout(titleGroup, BoxLayout.Y_AXIS));
        titleGroup.setBackground(AppTheme.BG_SURFACE);
        titleGroup.add(title);
        titleGroup.add(Box.createVerticalStrut(3));
        titleGroup.add(stepIndicator);

        bar.add(titleGroup, BorderLayout.WEST);
        return bar;
    }

    // ── Step 1: credentials ───────────────────────────────────────────────

    private JPanel buildStep1() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(AppTheme.BG_PAGE);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(AppTheme.BG_PAGE);
        form.setBorder(BorderFactory.createEmptyBorder(20, 28, 12, 28));

        JLabel hint = new JLabel("Enter your MySQL connection details.");
        hint.setFont(AppTheme.FONT_SMALL);
        hint.setForeground(AppTheme.TEXT_MUTED);
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(hint);
        form.add(Box.createVerticalStrut(18));

        hostField     = createField("localhost");
        portField     = createField("3306");
        databaseField = createField("le_festin");
        usernameField = createField("");
        passwordField = new JPasswordField();
        styleField(passwordField);

        form.add(buildRow("Host",     hostField));     form.add(Box.createVerticalStrut(10));
        form.add(buildRow("Port",     portField));     form.add(Box.createVerticalStrut(10));
        form.add(buildRow("Database", databaseField)); form.add(Box.createVerticalStrut(10));
        form.add(buildRow("Username", usernameField)); form.add(Box.createVerticalStrut(10));
        form.add(buildRow("Password", passwordField)); form.add(Box.createVerticalStrut(18));

        // Test connection row
        JPanel testRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        testRow.setBackground(AppTheme.BG_PAGE);
        testRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        testBtn = AppTheme.secondaryButton("Test Connection");
        connectionStatusLabel = new JLabel("");
        connectionStatusLabel.setFont(AppTheme.FONT_SMALL);
        connectionStatusLabel.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));
        testRow.add(testBtn);
        testRow.add(connectionStatusLabel);
        form.add(testRow);

        panel.add(form, BorderLayout.CENTER);
        panel.add(buildStep1ButtonBar(), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildStep1ButtonBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bar.setBackground(AppTheme.BG_SURFACE);
        bar.setBorder(BorderFactory.createCompoundBorder(
            AppTheme.BORDER_DIVIDER_TOP,
            BorderFactory.createEmptyBorder(12, 20, 12, 20)));

        JButton cancelBtn = AppTheme.secondaryButton("Cancel");
        nextBtn = AppTheme.primaryButton("Next");

        cancelBtn.addActionListener(e -> cancelSetup());
        testBtn.addActionListener(  e -> runTestConnection());
        nextBtn.addActionListener(  e -> goToStep2());

        getRootPane().setDefaultButton(nextBtn);

        bar.add(cancelBtn);
        bar.add(nextBtn);
        return bar;
    }

    // ── Step 2: initialization ────────────────────────────────────────────

    private JPanel buildStep2() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(AppTheme.BG_PAGE);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(AppTheme.BG_PAGE);
        form.setBorder(BorderFactory.createEmptyBorder(20, 28, 12, 28));

        JLabel hint = new JLabel("Set up the database tables and optional demo data.");
        hint.setFont(AppTheme.FONT_SMALL);
        hint.setForeground(AppTheme.TEXT_MUTED);
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(hint);
        form.add(Box.createVerticalStrut(16));

        JPanel schemaRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        schemaRow.setBackground(AppTheme.BG_PAGE);
        schemaRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel schemaIcon  = new JLabel("✓ ");
        schemaIcon.setFont(AppTheme.FONT_BODY);
        schemaIcon.setForeground(new Color(0x2E7D32));
        JLabel schemaLabel = new JLabel("Create database schema");
        schemaLabel.setFont(AppTheme.FONT_BODY);
        schemaLabel.setForeground(AppTheme.TEXT_PRIMARY);
        JLabel schemaNote  = new JLabel("  (always runs)");
        schemaNote.setFont(AppTheme.FONT_SMALL);
        schemaNote.setForeground(AppTheme.TEXT_MUTED);
        schemaRow.add(schemaIcon);
        schemaRow.add(schemaLabel);
        schemaRow.add(schemaNote);
        form.add(schemaRow);
        form.add(Box.createVerticalStrut(4));

        seedCheck = new JCheckBox(
            "<html>Load built-in seed data" +
            "<br><font size='-1' color='gray'>" +
            "&nbsp;&nbsp;First users: angela &nbsp;·&nbsp; carl &nbsp;·&nbsp; elizah" +
            "&nbsp;&nbsp;|&nbsp;&nbsp;password: <b>password123</b>" +
            "</font></html>");
        seedCheck.setSelected(true);
        seedCheck.setFont(AppTheme.FONT_BODY);
        seedCheck.setForeground(AppTheme.TEXT_PRIMARY);
        seedCheck.setBackground(AppTheme.BG_PAGE);
        seedCheck.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(seedCheck);
        form.add(Box.createVerticalStrut(16));

        logArea = new JTextArea(7, 36);
        logArea.setEditable(false);
        logArea.setFont(AppTheme.FONT_SMALL);
        logArea.setBackground(AppTheme.BG_SURFACE);
        logArea.setForeground(AppTheme.TEXT_PRIMARY);
        logArea.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createLineBorder(AppTheme.BG_BORDER, 1));
        logScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        logScroll.setPreferredSize(new Dimension(Integer.MAX_VALUE, 150));
        logScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        form.add(logScroll);

        panel.add(form, BorderLayout.CENTER);
        panel.add(buildStep2ButtonBar(), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildStep2ButtonBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bar.setBackground(AppTheme.BG_SURFACE);
        bar.setBorder(BorderFactory.createCompoundBorder(
            AppTheme.BORDER_DIVIDER_TOP,
            BorderFactory.createEmptyBorder(12, 20, 12, 20)));

        backBtn = AppTheme.secondaryButton("Back");
        JButton cancelBtn = AppTheme.secondaryButton("Cancel");
        initBtn = AppTheme.primaryButton("Initialize & Finish");

        backBtn.addActionListener(  e -> goToStep1());
        cancelBtn.addActionListener(e -> cancelSetup());
        initBtn.addActionListener(  e -> runInit());

        bar.add(backBtn);
        bar.add(cancelBtn);
        bar.add(initBtn);
        return bar;
    }

    // ── Navigation ────────────────────────────────────────────────────────

    private void goToStep2() {
        if (usernameField.getText().trim().isEmpty()) {
            setStatus("Username is required.", Color.RED);
            return;
        }
        if (!writeConfig()) return;
        stepIndicator.setText("Step 2 of 2 — Database Initialization");
        cardLayout.show(cards, STEP2);
        getRootPane().setDefaultButton(initBtn);
    }

    private void goToStep1() {
        stepIndicator.setText("Step 1 of 2 — Database Connection");
        cardLayout.show(cards, STEP1);
        getRootPane().setDefaultButton(nextBtn);
    }

    private void cancelSetup() {
        setupComplete = false;
        dispose();
    }

    // ── Actions ───────────────────────────────────────────────────────────

    private void runTestConnection() {
        testBtn.setEnabled(false);
        setStatus("Testing...", AppTheme.TEXT_MUTED);

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                // Connect to the server only (no database) — the target DB may not exist yet
                try (Connection c = DriverManager.getConnection(
                        buildServerUrl(),
                        usernameField.getText().trim(),
                        new String(passwordField.getPassword()))) {
                    return null;
                } catch (SQLException ex) {
                    return ex.getMessage();
                }
            }

            @Override
            protected void done() {
                testBtn.setEnabled(true);
                try {
                    String err = get();
                    if (err == null) {
                        setStatus("✓ Connected", new Color(0x2E7D32));
                    } else {
                        setStatus("✗ " + truncate(err, 60), Color.RED);
                    }
                } catch (InterruptedException | ExecutionException ex) {
                    setStatus("✗ " + truncate(ex.getMessage(), 60), Color.RED);
                }
            }
        }.execute();
    }

    private void runInit() {
        initBtn.setEnabled(false);
        backBtn.setEnabled(false);
        logArea.setText("");

        boolean loadSeed  = seedCheck.isSelected();
        String  serverUrl = buildServerUrl(); // no DB — used to create schema
        String  fullUrl   = buildUrl();       // with DB — used after schema creates it
        String  user      = usernameField.getText().trim();
        String  pass      = new String(passwordField.getPassword());

        new SwingWorker<String, String>() {
            @Override
            protected String doInBackground() {
                try {
                    // Connect without specifying the database so it can be created by the schema
                    publish("Connecting to MySQL server...");
                    try (Connection conn = DriverManager.getConnection(serverUrl, user, pass)) {
                        publish("Running schema...");
                        executeSqlFile(conn, "sql/le_festin_schema.sql");
                        publish("✓ Schema created.");
                    }

                    if (loadSeed) {
                        // Database now exists — reconnect with full URL
                        try (Connection conn = DriverManager.getConnection(fullUrl, user, pass)) {
                            publish("Loading seed data...");
                            executeSqlFile(conn, "sql/le_festin_seed.sql");
                            publish("✓ Seed data loaded.");

                            publish("Setting seed passwords...");
                            fixSeedPasswords(conn);
                            publish("✓ Passwords set (password123).");
                        }
                    }
                    return null;
                } catch (Exception ex) {
                    return ex.getMessage();
                }
            }

            @Override
            protected void process(List<String> chunks) {
                for (String line : chunks) logArea.append(line + "\n");
            }

            @Override
            protected void done() {
                try {
                    String err = get();
                    if (err == null) {
                        logArea.append("Setup complete.\n");
                        setupComplete = true;
                        dispose();
                    } else {
                        logArea.append("Error: " + err + "\n");
                        initBtn.setEnabled(true);
                        backBtn.setEnabled(true);
                    }
                } catch (InterruptedException | ExecutionException ex) {
                    logArea.append("Error: " + ex.getMessage() + "\n");
                    initBtn.setEnabled(true);
                    backBtn.setEnabled(true);
                }
            }
        }.execute();
    }

    // ── DB helpers ────────────────────────────────────────────────────────

    private void executeSqlFile(Connection conn, String filePath) throws Exception {
        String sql = new String(Files.readAllBytes(Paths.get(filePath)));
        try (Statement stmt = conn.createStatement()) {
            for (String s : sql.split(";")) {
                String trimmed = s.trim();
                if (!trimmed.isEmpty()) stmt.execute(trimmed);
            }
        }
    }

    private void fixSeedPasswords(Connection conn) throws SQLException {
        String hash = BCrypt.hashpw("password123", BCrypt.gensalt(12));
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE user SET password_hash = ? WHERE username = ?")) {
            for (String name : new String[]{"angela", "carl", "elizah"}) {
                ps.setString(1, hash);
                ps.setString(2, name);
                ps.executeUpdate();
            }
        }
    }

    private boolean writeConfig() {
        try {
            File resourcesDir = new File("resources");
            if (!resourcesDir.exists()) resourcesDir.mkdirs();

            File configFile = new File(resourcesDir, "config.properties");
            configFile.createNewFile(); // no-op if already exists

            try (FileWriter fw = new FileWriter(configFile)) {
                fw.write("db.url=" + buildUrl() + "\n");
                fw.write("db.user=" + usernameField.getText().trim() + "\n");
                fw.write("db.password=" + new String(passwordField.getPassword()) + "\n");
            }
            return true;
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                "Could not write config.properties:\n" + ex.getMessage(),
                "File Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private String buildUrl() {
        String host = hostField.getText().trim();
        String port = portField.getText().trim();
        String db   = databaseField.getText().trim();
        return "jdbc:mysql://" + host + ":" + port + "/" + db
            + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    }

    // URL without database name — used when the target DB may not exist yet
    private String buildServerUrl() {
        String host = hostField.getText().trim();
        String port = portField.getText().trim();
        return "jdbc:mysql://" + host + ":" + port
            + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    }

    // ── UI helpers ────────────────────────────────────────────────────────

    private JPanel buildRow(String labelText, JComponent field) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setBackground(AppTheme.BG_PAGE);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel(labelText);
        label.setFont(AppTheme.FONT_LABEL);
        label.setForeground(AppTheme.TEXT_SECONDARY);
        label.setPreferredSize(new Dimension(76, 28));

        row.add(label, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        return row;
    }

    private JTextField createField(String defaultText) {
        JTextField field = new JTextField(defaultText);
        styleField(field);
        return field;
    }

    private void styleField(JComponent field) {
        field.setFont(AppTheme.FONT_BODY);
        field.setBackground(AppTheme.BG_SURFACE);
        field.setForeground(AppTheme.TEXT_PRIMARY);
        field.setBorder(AppTheme.BORDER_INPUT);
    }

    private void setStatus(String text, Color color) {
        connectionStatusLabel.setText(text);
        connectionStatusLabel.setForeground(color);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    // ── Public API ────────────────────────────────────────────────────────

    public boolean isSetupComplete() { return setupComplete; }
}

package gui;

import manager.ClientController;
import network.Response;
import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;

public class MainFrame extends JFrame {
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel mainContainer = new JPanel(cardLayout);
    private final ClientController controller;
    private final LocaleManager lm = LocaleManager.getInstance();

    // Компоненты экрана логина — нужны для перевода
    private JLabel titleLabel;
    private JLabel userLabel, passLabel;
    private JButton loginBtn;
    private JComboBox<String> langCombo;

    private final PropertyChangeListener localeListener =
            evt -> rebuildLoginPanel();

    public MainFrame(ClientController controller) {
        this.controller = controller;
        lm.addChangeListener(localeListener);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);

        mainContainer.add(createLoginPanel(), "LOGIN");
        add(mainContainer);
        updateTitle();
    }

    private void updateTitle() {
        setTitle(lm.get("app.title"));
    }

    private void rebuildLoginPanel() {
        updateTitle();
        if (titleLabel != null) {
            titleLabel.setText(lm.get("login.title"));
            userLabel.setText(lm.get("login.username"));
            passLabel.setText(lm.get("login.password"));
            loginBtn.setText(lm.get("login.btn"));
        }
    }

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        titleLabel = new JLabel(lm.get("login.title"));
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));

        JTextField userField = new JTextField(15);
        JPasswordField passField = new JPasswordField(15);
        loginBtn = new JButton(lm.get("login.btn"));
        userLabel = new JLabel(lm.get("login.username"));
        passLabel = new JLabel(lm.get("login.password"));

        langCombo = new JComboBox<>(LocaleManager.LOCALE_LABELS);
        langCombo.addActionListener(e -> {
            int idx = langCombo.getSelectedIndex();
            lm.setLocale(LocaleManager.SUPPORTED_LOCALES[idx]);
        });

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1; gbc.gridx = 0; panel.add(new JLabel(lm.get("label.language")), gbc);
        gbc.gridx = 1; panel.add(langCombo, gbc);

        gbc.gridy = 2; gbc.gridx = 0; panel.add(userLabel, gbc);
        gbc.gridx = 1; panel.add(userField, gbc);

        gbc.gridy = 3; gbc.gridx = 0; panel.add(passLabel, gbc);
        gbc.gridx = 1; panel.add(passField, gbc);

        gbc.gridy = 4; gbc.gridx = 0; gbc.gridwidth = 2;
        panel.add(loginBtn, gbc);

        loginBtn.addActionListener(e -> {
            String u = userField.getText().trim();
            String p = new String(passField.getPassword()).trim();

            if (u.isEmpty() || p.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        lm.get("msg.error.empty_fields"),
                        lm.get("msg.error.label"), JOptionPane.ERROR_MESSAGE);
                return;
            }

            loginBtn.setEnabled(false);
            new Thread(() -> {
                controller.setCredentials(u, p);
                Response response = controller.sendCommand("login",
                        new String[]{p});
                SwingUtilities.invokeLater(() -> {
                    loginBtn.setEnabled(true);
                    if (response != null && response.isSuccess()) {
                        lm.removeChangeListener(localeListener);
                        mainContainer.add(new MainAppPanel(controller, u), "APP");
                        cardLayout.show(mainContainer, "APP");
                    } else {
                        String errorMsg = (response != null)
                                ? response.getMessage()
                                : lm.get("msg.error.no_server");
                        JOptionPane.showMessageDialog(this,
                                lm.get("msg.error.login", errorMsg),
                                lm.get("msg.error.label"), JOptionPane.ERROR_MESSAGE);
                    }
                });
            }).start();
        });

        return panel;
    }
}
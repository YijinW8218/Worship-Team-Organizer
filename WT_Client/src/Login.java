import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

public class Login extends JFrame {
    private final ClientConnection connection;
    private final JTextField usernameTextField = new JTextField(16);
    private final JPasswordField passwordTextField = new JPasswordField(16);
    private final JButton registerButton = new JButton("Create new user");
    private final JButton loginButton = new JButton("Log in");

    public Login(ClientConnection connection) {
        this.connection = connection;

        setTitle("Login");
        setSize(350, 220);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(createPanel());
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeConnection();
            }
        });

        loginButton.addActionListener(e -> submit("LOGIN"));
        registerButton.addActionListener(e -> submit("REGISTER"));
    }

    private JPanel createPanel() {
        JPanel outerPanel = new JPanel(new BorderLayout());
        outerPanel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Username:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(usernameTextField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        formPanel.add(new JLabel("Password:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(passwordTextField, gbc);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);

        outerPanel.add(formPanel, BorderLayout.CENTER);
        outerPanel.add(buttonPanel, BorderLayout.SOUTH);
        return outerPanel;
    }

    private void submit(String commandName) {
        String username = usernameTextField.getText().trim();
        String password = new String(passwordTextField.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter username and password.");
            return;
        }

        setButtonsEnabled(false);
        String command = commandName + "|" + username + "|" + password;

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws IOException {
                return connection.sendCommandForResponse(command);
            }

            @Override
            protected void done() {
                try {
                    handleServerResponse(commandName, username, get());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(Login.this, "Cannot connect to server: " + ex.getMessage());
                    setButtonsEnabled(true);
                }
            }
        }.execute();
    }

    private void handleServerResponse(String commandName, String username, String response) {
        if ("LOGIN_SUCCESS".equals(response) || "REGISTER_SUCCESS".equals(response)) {
            MainFrame mainFrame = new MainFrame(connection, username);
            mainFrame.setVisible(true);
            dispose();
            return;
        }

        String message;
        if ("LOGIN_FAIL".equals(response)) {
            message = "Username or password is incorrect.";
        } else if ("LOGIN_ALREADY_ONLINE".equals(response)) {
            message = "This account is already logged in on another client.";
        } else if ("REGISTER_FAIL".equals(response)) {
            message = "Username already exists.";
        } else {
            message = commandName + " failed. Server response: " + response;
        }

        JOptionPane.showMessageDialog(this, message);
        setButtonsEnabled(true);
    }

    private void setButtonsEnabled(boolean enabled) {
        SwingUtilities.invokeLater(() -> {
            loginButton.setEnabled(enabled);
            registerButton.setEnabled(enabled);
        });
    }

    private void closeConnection() {
        try {
            connection.close();
        } catch (IOException ignored) {
        }
    }
}

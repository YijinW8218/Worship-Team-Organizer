import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class login extends JFrame {
    private JPanel MainPanel;
    private JLabel welcomeLabel;
    private JTextField usernameTextField;
    private JTextField passwordTextField;
    private JButton loginButton;
    private JButton registerButton;
    private JLabel usernameLabel;
    private JLabel passwordLabel;

    public login(Client client) {
        setContentPane(MainPanel);
        setTitle("login page");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(500,400);
        setLocationRelativeTo(null);
        setVisible(true);

        welcomeLabel.setText("Welcome to Worship Team! Please log in or register.");
        usernameLabel.setText("username:");
        passwordLabel.setText("password:");
        loginButton.setText("Log in");
        registerButton.setText("Register");
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = usernameTextField.getText();
                String password = passwordTextField.getText();
                if (username.isEmpty()) {
                    JOptionPane.showMessageDialog(login.this, "username missing.");
                } else if (password.isEmpty()) {
                    JOptionPane.showMessageDialog(login.this, "password missing");
                } else {
                    client.logInGUI(username, password);
                    client.setConnectionCallback(msg -> {
                        if (msg.equals("Successfully logged in.")) {
                            JOptionPane.showMessageDialog(login.this, "Successfully logged in.");
                        } else if (msg.equals("Fail to log in. Try again.")) {
                            JOptionPane.showMessageDialog(login.this, "Fail to log in. Try again.");
                        }
                    });
                }
            }
        });
        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = usernameTextField.getText();
                String password = passwordTextField.getText();
                if (username.isEmpty()) {
                    JOptionPane.showMessageDialog(login.this, "username missing.");
                } else if (password.isEmpty()) {
                    JOptionPane.showMessageDialog(login.this, "password missing");
                } else {
                    client.registerGUI(username, password);
                    client.setConnectionCallback(msg -> {
                        if (msg.equals("Successfully register. Welcome.")) {
                            JOptionPane.showMessageDialog(login.this, "Successfully register. Welcome.");
                        } else if (msg.equals("Username already exists. Try again with another username.")) {
                            JOptionPane.showMessageDialog(login.this, "Username already exists. Try again with another username.");
                        }
                    });
                }
            }
        });
    }
}

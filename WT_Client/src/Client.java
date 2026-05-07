import javax.swing.*;
import java.io.*;
import java.net.Socket;

public class Client {
    private Socket socket = null;
    private InputStreamReader inputStreamReader = null;
    private OutputStreamWriter outputStreamWriter = null;
    private BufferedReader bufferedReader = null;
    private BufferedWriter bufferedWriter = null;
    private ConnectionCallback connection; //callback GUIs


    public void connect() {
        try{
            //establish socket connection to server
            socket = new Socket("localhost", 1234);
            //read and write with socket
            inputStreamReader = new InputStreamReader(socket.getInputStream());
            outputStreamWriter = new OutputStreamWriter(socket.getOutputStream());
            bufferedReader = new BufferedReader(inputStreamReader);
            bufferedWriter = new BufferedWriter(outputStreamWriter);
        }catch (IOException e) {e.printStackTrace();}
    }

    public void send(String msgToSend) {
        try {
            bufferedWriter.write(msgToSend);
            bufferedWriter.newLine();
            bufferedWriter.flush();

        }catch (IOException e){e.printStackTrace();}
    }

    public void listenServer() {
        new Thread(() -> {
            try{
                String msgRecieve;
                while ((msgRecieve = bufferedReader.readLine()) != null){
                    if (msgRecieve.startsWith("Events List:")) {
                        printEventList(msgRecieve);
                    } else if ((msgRecieve.startsWith("Songs List:"))) {
                        printSongList(msgRecieve);
                    }
                    //feedback login to GUI
                    else if (msgRecieve.equals("Successfully logged in.") || msgRecieve.equals("Fail to log in. Try again.")) {
                        final String msg = msgRecieve;
                        SwingUtilities.invokeLater(() -> {
                            if (connection != null) connection.onMessage(msg);
                        });}
                    //feedback register to GUI
                    else if (msgRecieve.equals("Successfully register. Welcome.") || msgRecieve.equals("Username already exists. Try again with another username.")) {
                        final String msg = msgRecieve;
                        SwingUtilities.invokeLater(() -> {
                            if (connection != null) connection.onMessage(msg);
                        });}

                    else {
                        System.out.println("Server:" + msgRecieve);
                    }
                }
            }catch (IOException e) {e.printStackTrace();}
        }).start();
    }

    private void printEventList(String msgRecieve) {
        String content = msgRecieve.substring("Events List:".length()); //skip the title of the data
        String[] eventStrings = content.split("\\|");

        System.out.println("Events List:");
        for (String e : eventStrings) {
            if (!e.isBlank()) {
                System.out.println(e.trim());
            }
        }
    }

    private void printSongList(String msgRecieve) {
        String content = msgRecieve.substring("Songs List:".length()); //skip the title of the data
        String[] songStrings = content.split("\\|");

        System.out.println("Songs List:");
        for (String s : songStrings) {
            if (!s.isBlank()) {
                System.out.println(s.trim());
            }
        }
    }


    //GUI request log in
    public void logInGUI(String username, String password) {
        try {
            String command_msg = "LOGIN|" + username + "|" + password;
            send(command_msg);
        }catch (Exception ex) {
            System.out.println("Try again.");
        }
    }

    //GUI request register
    public void registerGUI(String username, String password) {
        try {
            String command_msg = "REGISTER|" + username + "|" + password;
            send(command_msg);
        }catch (Exception ex) {
            System.out.println("Try again.");
        }
    }


    public void setConnectionCallback(ConnectionCallback connection) {
        this.connection = connection;
    }
}

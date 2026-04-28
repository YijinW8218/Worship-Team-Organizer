import java.util.Scanner;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {

    public static void main(String[] args) {
        Client client = new Client();
        ConsoleClient UI = new ConsoleClient();
        client.connect();
        client.listenServer();
        UI.start(client);

        new Thread(() -> {
            client.listenServer();
        }).start();


    }
}
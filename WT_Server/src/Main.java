import server.Server;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        // save json when server ends
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Server.shutdown();
        }));

        // start listen to client
        try {
            Server.listenClient(1234);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
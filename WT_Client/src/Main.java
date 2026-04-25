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

        // 处理控制台输入
        Scanner scanner = new Scanner(System.in);
        System.out.println("--- Connection Established ---");

        while (true) {
            System.out.print("press Enter to print: ");
            // 等待用户输入并按下回车
            String input = scanner.nextLine();

            // 调用 Client 的发送方法
            client.send(input);

            // 可选：添加退出指令
            if ("exit".equalsIgnoreCase(input)) {
                System.out.println("Closing connection...");
                break;
            }
        }

        scanner.close();

    }
}
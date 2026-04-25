import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Scanner;

public class ConsoleClient {
    private static final Scanner scanner = new Scanner(System.in);

    public static void start(Client client) {
        System.out.println("=== 敬拜团队排班系统（命令行版） ===");

        boolean running = true;

        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    addEventUI(client);
                    break;
                case "2":
                    listEventsUI();
                    break;
                case "3":
                    removeEventUI();
                    break;
                case "0":
                    running = false;
                    System.out.println("程序结束");
                    break;
                default:
                    System.out.println("无效输入，请重新选择");
            }
        }
    }
    private static void printMenu() {
        System.out.println();
        System.out.println("请选择操作：");
        System.out.println("1. 添加事件");
        System.out.println("2. 查看某天的事件");
        System.out.println("3. 删除事件");
        System.out.println("0. 退出");
        System.out.print("输入选项：");
    }

    // -------------------------
    // 1. 添加事件
    // -------------------------
    private static void addEventUI(Client client) {
        try {
            //Client send command to Server
            //dateString format:2026-4-21;    timeString format: 14:30
            System.out.print("输入日期 (YYYY-MM-DD): ");
            String dateString = scanner.nextLine().trim();
            System.out.print("输入时间 (HH:MM): ");
            String timeString = scanner.nextLine().trim();
            System.out.print("输入标题: ");
            String title = scanner.nextLine().trim();

            String command_msg = "ADD_EVENT|" + dateString + "|" + timeString + "|" + title;
            client.send(command_msg);

            System.out.println("已添加事件");

        } catch (Exception ex) {
            System.out.println("输入格式错误，请重试");
        }
    }
    // -------------------------
    // 2. 查看事件
    // -------------------------
    private static void listEventsUI(Client client) {
        try {
            System.out.print("输入日期 (YYYY-MM-DD): ");
            String dateString = scanner.nextLine().trim();

            String command_msg = "LIST_EVENT|" + dateString;
            client.send(command_msg);
            // Client will listen to server and print the list.

        } catch (Exception ex) {
            System.out.println("输入格式错误，请重试");
        }
    }

    // -------------------------
    // 3. 删除事件
    // -------------------------
    private static void removeEventUI(Client client) {
        try {
            System.out.print("输入要删除的事件 title: ");
            String title = scanner.nextLine().trim();

            String command_msg = "REMOVE_EVENT|" + title;
            client.send(command_msg);
            //server will receive and delete the event
        } catch (Exception ex) {
            System.out.println("输入格式错误，请重试");
        }
    }
}

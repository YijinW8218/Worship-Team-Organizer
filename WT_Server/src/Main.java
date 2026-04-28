import server.Server;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        // 1. 注册关闭钩子（程序退出时自动保存）
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Server.shutdown();
        }));

        // 2. 启动服务器监听
        try {
            Server.listenClient(1234);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
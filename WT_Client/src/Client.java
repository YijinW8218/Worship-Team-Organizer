import java.io.*;
import java.net.Socket;

public class Client {
    private Socket socket = null;
    private InputStreamReader inputStreamReader = null;
    private OutputStreamWriter outputStreamWriter = null;
    private BufferedReader bufferedReader = null;
    private BufferedWriter bufferedWriter = null;


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
                    }else {
                        System.out.println("Server:" + msgRecieve);
                    }
                }
            }catch (IOException e) {e.printStackTrace();}
        }).start();
    }

    private void printEventList(String msgRecieve) {
        String content = msgRecieve.substring("Events List:".length()); //skip the title of the data
        String[] eventStrings = content.split("\\|");

        System.out.println("=== Events List ===");
        for (String e : eventStrings) {
            if (!e.isBlank()) {
                System.out.println(e.trim());
            }
        }
    }
}

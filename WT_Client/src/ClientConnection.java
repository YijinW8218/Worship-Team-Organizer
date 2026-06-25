import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ClientConnection implements AutoCloseable {
    private static final String DISCONNECTED = "__DISCONNECTED__";
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int RESPONSE_TIMEOUT_SECONDS = 10;

    private final Socket socket;
    private final BufferedReader reader;
    private final BufferedWriter writer;
    private final BlockingQueue<String> responses = new LinkedBlockingQueue<>();
    private final List<ChangeListener> changeListeners = new CopyOnWriteArrayList<>();
    private volatile boolean closed;

    public ClientConnection(String host, int port) throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        startReaderThread();
    }

    public synchronized String sendCommandForResponse(String command) throws IOException {
        System.out.println("[CLIENT] Sending: " + command);
        sendCommand(command);
        try {
            String response = responses.poll(RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (response == null) {
                throw new IOException("Timed out waiting for server response to: " + command);
            }
            if (DISCONNECTED.equals(response)) {
                throw new IOException("Server disconnected.");
            }
            System.out.println("[CLIENT] Received: " + response);
            return response;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for server response.", e);
        }
    }

    public synchronized void sendCommand(String command) throws IOException {
        if (closed) {
            throw new IOException("Connection is closed.");
        }
        writer.write(command);
        writer.newLine();
        writer.flush();
    }

    public void addChangeListener(ChangeListener listener) {
        changeListeners.add(listener);
    }

    private void startReaderThread() {
        Thread readerThread = new Thread(() -> {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("DATA_CHANGED")) {
                        System.out.println("[CLIENT] Received broadcast: " + line);
                        notifyChangeListeners();
                    } else {
                        responses.offer(line);
                    }
                }
            } catch (IOException ignored) {
            } finally {
                closed = true;
                responses.offer(DISCONNECTED);
            }
        }, "client-server-reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    private void notifyChangeListeners() {
        for (ChangeListener listener : changeListeners) {
            listener.dataChanged();
        }
    }

    @Override
    public synchronized void close() throws IOException {
        if (!socket.isClosed()) {
            try {
                sendCommand("QUIT");
            } finally {
                socket.close();
            }
        }
    }

    public interface ChangeListener {
        void dataChanged();
    }
}

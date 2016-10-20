package laba_2;

import java.io.IOException;
import java.io.OutputStream;
import java.net.*;

/**
 * постоянно пишет на сервер (по 1024*1024 символу) после вызова start()
 */
public class Client {
    private int port;
    private InetAddress inetAddress;
    private Socket socket;

    public Client(InetAddress inetAddress, int port) {
        this.inetAddress = inetAddress;
        this.port = port;
    }
    public void start() throws IOException {
        try {
            socket = new Socket(inetAddress, port);

            OutputStream stream = socket.getOutputStream();
            byte[] bytes = new byte[1024*1024];
            for (byte b : bytes) {
                b = 111;
            }

            while (!socket.isClosed()) {
                stream.write(bytes);
                System.out.println("stream client");
            }
        } finally {
            socket.close();
        }
    }

    public static void main(String[] args) {
        try {
            //Client client = new Client(InetAddress.getLocalHost(), Laba_2.PORT);
            Client client = new Client(InetAddress.getByName("172.16.14.5"), 1234);
            client.start();
        } catch (IOException e) {
            System.err.println(e.toString());
        }
    }
}

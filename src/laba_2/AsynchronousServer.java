package laba_2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.concurrent.CompletableFuture;

/**
 * реализация не закончена
 */
public class AsynchronousServer {
    private ServerSocket server;
    private int timeout;
    private int port;

    public AsynchronousServer(int port, InetAddress inetAddress, int timeout) throws IOException {
        this.server = new ServerSocket(port, 0, inetAddress);
        this.timeout = timeout;
        this.port = port;
    }
    public void start() throws IOException {

        while(true){
            Socket socket = server.accept();
            socket.setSoTimeout(timeout);
            CompletableFuture.supplyAsync(()-> {
                try {
                    InputStreamReader br = new InputStreamReader(socket.getInputStream());
                    while(true) {
                        long lastTime;
                        long size;
                        for (lastTime = System.currentTimeMillis(), size = 0; socket.getKeepAlive() && System.currentTimeMillis() - lastTime < timeout; ++size) {
                            br.read();
                        }
                        System.out.printf("speed = %d mail/mlsec", size / timeout);
                    }
                } catch (IOException e) {
                    try {
                        socket.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    return 1;
                }
            });

        }



    }
}

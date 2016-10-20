package laba_2;

import java.net.Socket;

/**
 * не реализован
 */
public class ThreadServer extends Thread {
    Socket socket;
    int count;

    public ThreadServer(int count, Socket socket){
        this.count = count;
        this.socket = socket;
        setDaemon(true);
    }

    @Override
    public void run() {
        super.run();
    }
}

package laba_7;

import java.nio.channels.SocketChannel;

/**
 * Created by BODY on 22.12.2016.
 */
public class Rope {
    private SocketChannel socketClient;
    private SocketChannel socketServer;
    private boolean error;
    private boolean connect;

    public Rope(SocketChannel socketClient, SocketChannel socketServer) {
        this.socketClient = socketClient;
        this.socketServer = socketServer;
        error = false;
        connect = false;
    }

    public void setConnect (boolean connect) {
        this.connect = connect;
    }
    public void setError(boolean error) {
        this.error = error;
    }
    public boolean isConnect() {
        return connect;
    }
    public boolean isError() {
        return error;
    }
    public SocketChannel getOther(SocketChannel socketChannel) {
        if (socketChannel.equals(socketClient)) {
            return socketServer;
        }
        if (socketChannel.equals(socketServer)) {
            return  socketClient;
        }
        return null;
    }
}

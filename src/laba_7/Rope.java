package laba_7;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Created by BODY on 22.12.2016.
 */
public class Rope {
    private static final int SIZEBUF = 10;
    private SocketChannel socketClient;
    private SocketChannel socketServer;
    private boolean error;
    private boolean connect;
    private boolean writableClient;
    private boolean writableServer;
    private boolean mustWriteClient;
    private boolean mustWriteServer;
    private ByteBuffer bufferClient;
    private ByteBuffer bufferServer;

    public Rope(SocketChannel socketClient, SocketChannel socketServer) {
        this.socketClient = socketClient;
        this.socketServer = socketServer;
        error = false;
        connect = false;
        writableClient = false;
        writableServer = false;
        mustWriteClient = false;
        mustWriteServer = false;
        bufferClient = ByteBuffer.allocate(SIZEBUF);
        bufferServer = ByteBuffer.allocate(SIZEBUF);
    }
    public void setMustWrite(SocketChannel socketChannel, boolean mustWrite) {
        if (socketClient.equals(socketChannel)) {
            mustWriteClient = mustWrite;
        } else if (socketServer.equals(socketChannel)) {
            mustWriteServer = mustWrite;
        }
    }
    public void setWritable(SocketChannel socketChannel, boolean writable){
        if (socketClient.equals(socketChannel)) {
            writableClient = writable;
        } else if (socketServer.equals(socketChannel)) {
            writableServer = writable;
        }
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
    public boolean isWritable(SocketChannel socketChannel) {
        if (socketClient.equals(socketChannel)) {
            return writableClient;
        }
        if (socketServer.equals(socketChannel)) {
            return writableServer;
        }
        return false;
    }
    public boolean isMustWrite(SocketChannel socketChannel) {
        if (socketClient.equals(socketChannel)) {
            return mustWriteClient;
        }
        if (socketServer.equals(socketChannel)) {
            return mustWriteServer;
        }
        return false;
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
    public ByteBuffer getBufferOther(SocketChannel socketChannel) {
        if (socketClient.equals( socketChannel)) {
            return bufferServer;
        }
        if (socketServer.equals(socketChannel)) {
            return  bufferClient;
        }
        return null;
    }
    public ByteBuffer getBufferThis(SocketChannel socketChannel) {
        if (socketChannel.equals(socketClient)) {
            return bufferClient;
        }
        if (socketChannel.equals(socketServer)) {
            return  bufferServer;
        }
        return null;
    }
}

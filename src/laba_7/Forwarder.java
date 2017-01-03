package laba_7;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 *
 */
public class Forwarder {
    private Selector selector;
    private ServerSocketChannel ssc;
    private static final int TIMEOUT = 10000;
    public static final int PORT =  9999;
    private InetAddress rhost;
    private int rport;

    public Forwarder(int lport, InetAddress rhost, int rport) throws IOException {
        this.selector = Selector.open();
        ssc = ServerSocketChannel.open();
        ssc.bind(new InetSocketAddress(lport));
        ssc.configureBlocking( false );
        ssc.register( selector, SelectionKey.OP_ACCEPT );
        this.rhost = rhost;
        this.rport = rport;
        //ropes = new ArrayList<>();
    }

    public void start() throws IOException {
        /***************/
        while (true) {
            int numSelect = selector.select(TIMEOUT);
            System.err.println("numSelect =  " + numSelect);
            if (numSelect == 0) {
                continue;
            }

            Set<SelectionKey> selectedKeys = selector.selectedKeys();

            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

            while (keyIterator.hasNext()) {

                SelectionKey key = keyIterator.next();
                if (key.isAcceptable()) {
                    // a connection was accepted by a ServerSocketChannel.
                    SocketChannel sc = ssc.accept();
                    System.err.println("Accept: " + sc.toString());
                    sc.configureBlocking( false );
                    SocketChannel socketChannel = SocketChannel.open();
                    socketChannel.configureBlocking(false);
                    socketChannel.connect(new InetSocketAddress(rhost, rport));
                    Rope rope = new Rope(sc, socketChannel);
                    socketChannel.register(selector, SelectionKey.OP_CONNECT).attach(rope);
                    sc.register( selector, SelectionKey.OP_READ ).attach(rope); // Регистрируем его в Selector для чтения.
                } else if (key.isConnectable()) {
                    SocketChannel socketChannelKey = (SocketChannel) key.channel();
                    Rope ropeKey = (Rope)key.attachment();
                    // a connection was established with a remote server.
                    if (socketChannelKey.finishConnect()) {
                       connect(socketChannelKey, ropeKey);
                    } else {
                        System.err.println("Error connect: " + socketChannelKey.getRemoteAddress().toString());
                        ropeKey.setError(true);
                        key.cancel();
                    }
                } else if (key.isReadable()) {
                    SocketChannel socketChannelKey = (SocketChannel) key.channel();
                    Rope ropeKey = (Rope)key.attachment();
                    // a channel is ready for reading
                    if (ropeKey.isError()) {
                        System.err.println("Error: " + socketChannelKey.getRemoteAddress().toString());
                        //closeSockets(key); // мы же уже закрыли
                        key.cancel();
                    } else  {
                        try {
                            read(key);
                        } catch (IOException e) {
                            System.err.println("Error read: " + socketChannelKey.getRemoteAddress().toString());
                            ropeKey.setError(true);
                            closeSockets(key);
                            keyIterator.remove();
                            continue;
                        }
                    }
                } else if (key.isWritable()) {
                    write(key);
                }
                keyIterator.remove();
            }
        }
        /*********************/
    }

    private void connect(SocketChannel socketChannelKey, Rope ropeKey) throws IOException {
        ropeKey.setConnect(true);
        System.err.println("Connect: " + socketChannelKey.getRemoteAddress().toString());
        socketChannelKey.register(selector, SelectionKey.OP_READ).attach(ropeKey);

        if (ropeKey.isMustWrite(socketChannelKey)) {
            socketChannelKey.register(selector, SelectionKey.OP_WRITE | SelectionKey.OP_READ).attach(ropeKey);
            ropeKey.setWritable(socketChannelKey, true);
            ropeKey.setMustWrite(socketChannelKey, false);
        }
        if (ropeKey.isMustWrite(ropeKey.getOther(socketChannelKey))) {
            ropeKey.getOther(socketChannelKey).register(selector, SelectionKey.OP_WRITE | SelectionKey.OP_READ).attach(ropeKey);
            ropeKey.setWritable(ropeKey.getOther(socketChannelKey), true);
            ropeKey.setMustWrite(ropeKey.getOther(socketChannelKey), false);
        }
    }

    private void closeSockets(SelectionKey key) {

        try {
            System.err.println("Close: " + ((SocketChannel)key.channel()).getRemoteAddress().toString());
            key.channel().close();
        } catch (IOException e) {
            //
        }
        try {
            System.err.println("Close: " + ((Rope)key.attachment()).getOther((SocketChannel)key.channel()).getRemoteAddress().toString());
            ((Rope)key.attachment()).getOther((SocketChannel)key.channel()).close();
        } catch (IOException e) {
            //
        }
        key.cancel();
    }

    private boolean write(SelectionKey key) throws IOException {
        SocketChannel socketChannelKey = (SocketChannel) key.channel();
        Rope ropeKey = (Rope)key.attachment();
        return write(socketChannelKey, ropeKey);
    }

    private boolean write(SocketChannel socketChannelKey, Rope ropeKey) throws IOException {
        System.err.println("write: " + socketChannelKey.getRemoteAddress());
        ByteBuffer byteBuffer = ropeKey.getBufferThis(socketChannelKey);
        byteBuffer.flip();
        int size = socketChannelKey.write(byteBuffer);
        System.err.println("write  " + size);
        if (byteBuffer.position() == byteBuffer.limit()) {
            byteBuffer.clear();
            ropeKey.setWritable(socketChannelKey, false);
            socketChannelKey.register(selector, SelectionKey.OP_READ).attach(ropeKey);
            return true;
        } else {
            ByteBuffer newBuffer = byteBuffer.slice();
            byteBuffer.clear();
            byteBuffer.put(newBuffer);
            return false;
        }
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel socketChannelKey = (SocketChannel) key.channel();
        Rope ropeKey = (Rope)key.attachment();
        System.err.println(ropeKey.getBufferOther(socketChannelKey));
        int re = socketChannelKey.read(ropeKey.getBufferOther(socketChannelKey));
        System.err.println("Read: " + socketChannelKey.getRemoteAddress().toString());
        System.err.println("Read  " + re);
        if (!write(ropeKey.getOther(socketChannelKey), ropeKey)) {
            if (ropeKey.isConnect()) {
                if (!ropeKey.isWritable(ropeKey.getOther(socketChannelKey))) {
                    ropeKey.getOther(socketChannelKey).register(selector, SelectionKey.OP_WRITE | SelectionKey.OP_READ).attach(ropeKey);
                    ropeKey.setWritable(ropeKey.getOther(socketChannelKey), true);
                    ropeKey.setMustWrite(ropeKey.getOther(socketChannelKey), false);
                }
            } else {
                ropeKey.setMustWrite(ropeKey.getOther(socketChannelKey), true);
            }
        }
    }

    public static void main(String[] args) {
        try {
            Forwarder forwarder = new Forwarder(PORT, InetAddress.getByName("localhost"), TestEcho.PORT);
            forwarder.start();
        } catch (IOException e) {
            System.err.println(e.toString());
            System.exit(-1);
        }
    }
}
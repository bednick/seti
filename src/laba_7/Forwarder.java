package laba_7;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Created by BODY on 22.12.2016.
 */
public class Forwarder {
    private Selector selector;
    private ServerSocketChannel ssc;
    private static final int TIMEOUT = 10000;
    private static final int SIZEBUF = 1024;
    public static final int PORT =  9999;
    private InetAddress rhost;
    private int rport;
    //private List<Rope> ropes;

    public Forwarder (int lport, InetAddress rhost, int rport) throws IOException {
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
        ByteBuffer buffer = ByteBuffer.allocate(SIZEBUF);

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
                    //ropes.add(rope);
                } else if (key.isConnectable()) {
                    // a connection was established with a remote server.
                    if (((SocketChannel)key.channel()).finishConnect()) {
                        ((Rope)key.attachment()).setConnect(true);
                        SocketChannel sc = (SocketChannel)key.channel();
                        System.err.println("Connect: " + sc.getRemoteAddress().toString());
                        Rope rope = (Rope)key.attachment();
                        System.err.println("Read: " + sc.getRemoteAddress().toString());
                        sc.register(selector, SelectionKey.OP_READ).attach(rope);
                    } else {
                        System.err.println("Error connect: " + ((SocketChannel)key.channel()).getRemoteAddress().toString());
                        ((Rope)key.attachment()).setError(true);
                        key.cancel();
                    }
                } else if (key.isReadable()) {
                    // a channel is ready for reading
                    if (((Rope)key.attachment()).isError()) {
                        System.err.println("Error: " + ((SocketChannel)key.channel()).getRemoteAddress().toString());
                        closeSockets(key);
                    } else if (((Rope)key.attachment()).isConnect()) {
                        System.err.println("Connect: " + ((SocketChannel)key.channel()).getRemoteAddress().toString());
                        // передавать второму
                        try {
                            int re = ((SocketChannel) key.channel()).read(buffer);
                            System.err.println("Read: " + ((SocketChannel)key.channel()).getRemoteAddress().toString());
                            System.err.println("Read " + re);
                        } catch (IOException e) {
                            System.err.println("Error read: " + ((SocketChannel)key.channel()).getRemoteAddress().toString());
                            ((Rope) key.attachment()).setError(true);
                            closeSockets(key);
                            keyIterator.remove();
                            continue;
                        }
                        ByteBuffer byteBuffer = ByteBuffer.allocate(buffer.position());
                        byteBuffer.put(buffer.array(), 0, buffer.position());
                        byteBuffer.flip();
                        int pos = buffer.position();
                        //buffer.flip();
                        int size = 0;
                        try {
                            System.err.println("Wrire s: " + ((Rope) key.attachment()).getOther((SocketChannel) key.channel()).getRemoteAddress().toString());
                            while (size < pos) {
                                size += ((Rope) key.attachment()).getOther((SocketChannel) key.channel()).write(byteBuffer);
                                System.err.println("write: " + size);
                            }
                            buffer.clear();
                            System.err.println("Wrire f: " + ((Rope) key.attachment()).getOther((SocketChannel) key.channel()).getRemoteAddress().toString());
                        } catch (IOException e) {
                            System.err.println("Error wrire: " + ((Rope) key.attachment()).getOther((SocketChannel) key.channel()).getRemoteAddress().toString());
                            ((Rope) key.attachment()).setError(true);
                            closeSockets(key);
                            keyIterator.remove();
                            continue;
                        }
                    } else {
                        System.err.println("No connect: " + ((SocketChannel)key.channel()).getRemoteAddress().toString());
                        continue; //мы ещё не подключились, нужно ждать
                    }
                } else if (key.isWritable()) {

                }

                keyIterator.remove();
            }
        }

        /*********************/
    }

    private void closeSockets(SelectionKey key) {

        try {
            System.err.println("Close: " + ((SocketChannel)key.channel()).getRemoteAddress().toString());
            ((SocketChannel)key.channel()).close();
        } catch (IOException e) {
            //
        }
        try {
            ((Rope)key.attachment()).getOther((SocketChannel)key.channel()).close();
        } catch (IOException e) {
            //
        }
        key.cancel();
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

package laba_7;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by BODY on 22.12.2016.
 */
public class TestEcho {

    public static final int PORT = 10001;

    public static void main(String[] args) throws IOException {

        ByteBuffer buffer = ByteBuffer.allocate(1024);

        Selector selector = Selector.open();
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.bind(new InetSocketAddress(PORT));
        ssc.configureBlocking( false );
        ssc.register( selector, SelectionKey.OP_ACCEPT );

        while (true) {

            System.err.println("select " + selector.select(10000));

            Set<SelectionKey> selectedKeys = selector.selectedKeys();

            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

            while (keyIterator.hasNext()) {

                SelectionKey key = keyIterator.next();

                if (key.isAcceptable()) {
                    // a connection was accepted by a ServerSocketChannel.
                    SocketChannel sc = ssc.accept();
                    System.err.println("Accept: " + sc.toString());
                    sc.configureBlocking( false );
                    sc.register( selector, SelectionKey.OP_READ );
                } else if (key.isReadable()) {
                    System.err.println("Readable: ");
                    // a channel is ready for reading
                    try {
                        int size = ((SocketChannel) key.channel()).read(buffer);
                        System.err.println("Read " + size);
                        if (size == -1) {
                            System.err.println("Error read " + ((SocketChannel) key.channel()).getRemoteAddress().toString());
                            try {
                                ((SocketChannel) key.channel()).close();
                            } catch (IOException e) {
                                //
                            }
                            key.cancel();
                        }
                        //buffer.flip();
                        byte[] res = new byte[buffer.position()];
                        buffer.flip();
                        buffer.get(res);
                        String str = new String(res, Charset.forName("UTF-8"));
                        System.out.println("Echo: " + ((SocketChannel) key.channel()).getRemoteAddress().toString());
                        System.out.println("sizeStr=" + str.length() + " str=" + str);
                        System.err.println("write " + ((SocketChannel) key.channel()).write((ByteBuffer) ByteBuffer.allocate(res.length).put(res).flip()));
                        buffer.clear();
                    } catch (IOException e) {
                        System.err.println("Error: " + e.toString());
                        key.cancel();
                    }
                } else {
                    System.err.println("is not Readable");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        System.err.println(e.toString());
                    }
                }
                keyIterator.remove();
            }
        }
    }
}

package laba_3;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by BODY on 17.10.2016.
 */
public class SelectorServer {
    public static int PORT = 40040;
    private static final int SIZEBUF = 1024 * 1024;
    private static final int TIMEOUT = 5000;
    private Selector selector;
    private ServerSocketChannel ssc;

    public SelectorServer (int port) throws IOException {
        this.selector = Selector.open();
        ssc = ServerSocketChannel.open();
        ssc.bind(new InetSocketAddress(port));
        ssc.configureBlocking( false );
        ssc.register( selector, SelectionKey.OP_ACCEPT );
    }

    public void start() throws IOException {
        while(true) {
            int num = selector.select(TIMEOUT);
            if (num == 0) {
                continue;
            }
            Set keys = selector.selectedKeys();
            Iterator it = keys.iterator();
            while (it.hasNext()) {
                SelectionKey key = (SelectionKey) it.next();

                if ((key.readyOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) {
                    SocketChannel sc = ssc.accept();
                    sc.configureBlocking( false );
                    sc.register( selector, SelectionKey.OP_READ ).attach(new Handler());

                } else if ((key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
                    SocketChannel sc = (SocketChannel)key.channel();
                    long size;
                    try {
                        size = sc.read(((Handler)key.attachment()).buffer);
                    } catch (IOException e){
                        System.err.println("ошибка чтения " + key.toString());
                        key.cancel();
                        continue;
                    }
                    if(size >= 0){
                        //((Handler)key.attachment()). size += size;
                    } else {
                        key.cancel();
                    }
                }
            }
            keys.clear();
        }
    }

    private class Handler{
        ByteBuffer buffer;
        ByteBuffer allByte;
        public Handler(){
            this.buffer = ByteBuffer.allocate(SIZEBUF);
            this.allByte = ByteBuffer.allocate(1024*1024);
        }
        void work() {

        }
    }
}

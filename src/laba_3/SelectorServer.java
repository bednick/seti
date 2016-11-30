package laba_3;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
 * Created by BODY on 17.10.2016.
 */
public class SelectorServer {
    public static int PORT = 40040;
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
                    try {
                        if (((Handler)key.attachment()).work((SocketChannel)key.channel())){
                            System.out.println("close " + key);
                            ((SocketChannel) key.channel()).close();
                            key.cancel();
                        }
                    } catch (IOException e){
                        System.err.println("ошибка работы " + key.toString());
                        System.err.println(e.toString());
                        try {
                            ((SocketChannel) key.channel()).close();
                        } catch (IOException exc){
                            System.err.println(exc.toString());
                        }
                        key.cancel();
                    }
                }
            }
            keys.clear();
        }
    }

    private class Handler {
        private static final int SIZEBUF = 1024 * 1024;
        private static final float LIMITBUF = SIZEBUF * 0.5f;
        private static final String NAMEDIR = "uploads";
        private ByteBuffer buffer;
        private File fileOut = null;
        private FileOutputStream fileOutStream = null;
        private long fileSize;
        private long allReadBytes = 0;



        private Handler() {
            this.buffer = ByteBuffer.allocate(SIZEBUF);
            //this.allByte = ByteBuffer.allocate(1024*1024);
        }
        private boolean work(SocketChannel sc) throws IOException {
            int readBytes = sc.read(buffer);
            if (readBytes == -1) {
                writeData();
                ByteBuffer buffer = ByteBuffer.allocate(4);

                if (allReadBytes != fileSize) {
                    buffer.putInt(111);
                    buffer.flip();
                    while (buffer.hasRemaining()) {
                        sc.write(buffer);
                    }
                    System.err.println(allReadBytes);
                    System.err.println(fileSize);
                    throw new IOException("readBytes != fileSize");
                } else {
                    buffer.putInt(0);
                    buffer.flip();
                    while (buffer.hasRemaining()) {
                        sc.write(buffer);
                    }
                    //System.err.println("readBytes == fileSize");

                    return true;
                }

            } else if (buffer.position() > LIMITBUF) {
                writeData();
            }
            return false;
        }
        private void createFileOut(String nameFile) throws IOException {
            File dir = new File(NAMEDIR);
            if (!dir.canExecute()) {
                if(!dir.mkdir()){
                    throw new IOException("не удалось создать " + NAMEDIR);
                }
            }
            fileOut = new File(dir, nameFile);
        }
        private void writeData() throws IOException {
            int position = buffer.position();
            buffer.position(0);
            if (fileOut == null) {
                fileSize = buffer.getLong();
                int nameFileLength = buffer.getInt();
                byte[] nameFileByte = new byte[nameFileLength];
                buffer.get(nameFileByte);
                String nameFile = new String(nameFileByte, Charset.forName("UTF-8"));
                createFileOut(nameFile);
                fileOutStream = new FileOutputStream(fileOut);
            }
            fileOutStream.write(buffer.array(), buffer.position(), position - buffer.position());
            allReadBytes += position - buffer.position();
            buffer.clear();
        }
    }


    public static void main(String[] args) {
        try {
            SelectorServer selectorServer = new SelectorServer(SelectorServer.PORT);
            selectorServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

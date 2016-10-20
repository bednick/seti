package laba_2;

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
 * Created by BODY on 14.10.2016.
 */
public class SelectorServer {
    private Selector selector;
    private ServerSocketChannel ssc;
    private static final int TIMEOUT = 5000;
    private static final int SIZEBUF = 1024;

    public SelectorServer (int port) throws IOException {
        this.selector = Selector.open();
        ssc = ServerSocketChannel.open();
        ssc.bind(new InetSocketAddress(port));
        ssc.configureBlocking( false );
        ssc.register( selector, SelectionKey.OP_ACCEPT );
    }

    public void start() throws IOException {
        long lastTime = System.currentTimeMillis();
        ByteBuffer buffer = ByteBuffer.allocate(SIZEBUF);
        while (true) {

            // Проверяем, если ли какие-либо активности -
            // входящие соединения или входящие данные в
            // существующем соединении.
            int num = selector.select(TIMEOUT);

            //если прошло времени больше TIMEOUT
            //тогда выводим время в каждом из стоящих в очереди Channel'ов
            if(System.currentTimeMillis() - lastTime > TIMEOUT) {
                Set<SelectionKey> keys = selector.selectedKeys();
                for (SelectionKey key:keys) {
                    if(key.attachment() != null){
                        System.out.println("Client's " + ((SocketChannel) key.channel()).getRemoteAddress()
                                + " speed is " + ((Time) key.attachment()).getSpeed() + " Mb/s");
                    }
                }
                lastTime = System.currentTimeMillis();
            }
            // Если никаких активностей нет, выходим из цикла
            // и снова ждём.
            if (num == 0) {
                continue;
            }
            // Получим ключи, соответствующие активности,
            // которые могут быть распознаны и обработаны один за другим.
            Set keys = selector.selectedKeys();
            Iterator it = keys.iterator();

            while (it.hasNext()) {
                // Получим ключ, представляющий один из битов
                // активности ввода/вывода.
                SelectionKey key = (SelectionKey)it.next();
                // ... работаем с SelectionKey ...
                if ((key.readyOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) {
                    // Принимаем входящее соединение
                    // Необходимо сделать его неблокирующим,
                    // чтобы использовать Selector для него.
                    SocketChannel sc = ssc.accept();
                    sc.configureBlocking( false );
                    // Регистрируем его в Selector для чтения.
                    sc.register( selector, SelectionKey.OP_READ ).attach(new Time());
                } else if ((key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
                    // считать
                    SocketChannel sc = (SocketChannel)key.channel();
                    long size;
                    try {
                         size = sc.read(buffer);
                    } catch (IOException e){
                        key.cancel();
                        continue;
                    }
                    if(size > 0){
                        ((Time)key.attachment()). addByte(size);
                        buffer.clear();
                    } else {
                        key.cancel();
                    }
                }
            }
            // Удаляем выбранные ключи, поскольку уже отработали с ними.
            keys.clear();
        }
    }

    private class Time {
        long lastTime;
        long sizeByte;
        public Time(){
            lastTime = System.currentTimeMillis();
            sizeByte = 0;
        }
        public void addByte (long size){
            sizeByte += size;
        }
        public double getSpeed() {
            double speed = (sizeByte / (1024.0 * 1024.0)) / ((System.currentTimeMillis() - lastTime) / 1000.0);
            lastTime = System.currentTimeMillis();
            sizeByte  = 0;
            return speed;
        }
    }
}

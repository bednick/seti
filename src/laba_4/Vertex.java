package laba_4;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Чат-дерево
 * Узлы обмениваются UDP сообщениями
 *
 */
public class Vertex {
    private static final int MAXCOUNTSEND = 5;       // Максимальное количество повторных отправлений сообщений
    private static final int MAXSIZEMESSAGE = 1024;  // Максимальный размер сообщения, в байтах
    private static final int TIMESEND = 1000;        // Время через которое производим повторную отправку сообщений
    private static final int TIMEOUT = TIMESEND / 3; // Время блокирования сокета на reciive
    private static final int MAXWAITMESSAGES = 5;    // максивальное количество сообщений в очереди == (FOOMAX * childrens.size())

    private Node parent;
    private List<Node> childrens;
    private String thisName;
    private int thisPort;
    private DatagramSocket ds;

    private int loss;
    private Random random;

    private Map<Message, Long> waitMessages; // хранит все сообщения, ожидающие ответ(о их доставки до всех) и их время последнего отправления
    private Queue<Message> allMessages;      // хранит в себе определённое количество последних сообщений

    public Vertex(InetAddress parentIA, int parentPort, String thisName, int thisPort, int loss) {
        this.parent = new Node(parentIA, parentPort);
        this.thisName = thisName + ": ";
        this.thisPort = thisPort;
        this.ds = null;
        childrens = new ArrayList<>();
        waitMessages = new HashMap<>();
        allMessages = new LinkedList<>();
        this.loss = loss;
        random = new Random(System.currentTimeMillis());
    }
    public Vertex(String thisName, int thisPort, int loss) {
        this.parent = null;
        this.thisName = thisName + ": ";
        this.thisPort = thisPort;
        this.ds = null;
        childrens = new ArrayList<>();
        waitMessages = new HashMap<>();
        allMessages = new LinkedList<>();
        this.loss = loss;
        random = new Random(System.currentTimeMillis());
    }

    public void start() throws IOException {
        /* Основная логика программы
        * */
        try {
            ds = new DatagramSocket(thisPort);
            ds.setSoTimeout(TIMEOUT);
        } catch (SocketException e){
            System.err.println(e.toString());
            addLog("Error start new vetrex to port " + thisPort);
            return;
        }
        if (!connectParent(ds)) { // Если изначально ну удаётся подключиться к родителю, считаем это ошибкой
            System.err.println("error connect to parent");
            addLog("error connect to parent");
            return;
        }
        BufferedReader streamStdin = new BufferedReader(new InputStreamReader(System.in));
        DatagramPacket pack_in = new DatagramPacket(new byte[MAXSIZEMESSAGE], MAXSIZEMESSAGE);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            /* Обрабодчик, вызывающийся при завершении main потока
            * */
            @Override
            public void run() {
                waitMessages.clear();
                if (parent == null) {
                    if (childrens.size() > 0) {
                        parent = childrens.get(0);
                    } else {
                        return;
                    }
                } else {
                    sendMessage(new Message(Message.TYPE.DEAD, new byte[0], parent));
                }
                ByteBuffer buffer = ByteBuffer.allocate(parent.getInetAddress().getAddress().length + Integer.BYTES);
                buffer.putInt(parent.getPort()).put(parent.getInetAddress().getAddress()).flip();
                Message messageNewParent = new Message(Message.TYPE.PARENT, buffer.array());
                for (Node node: childrens) {
                    sendMessage(new Message(messageNewParent.getType(), messageNewParent.getData(), node));
                }
                DatagramPacket pack = new DatagramPacket(new byte[Message.SIZEHEAD + Long.BYTES], Message.SIZEHEAD + Long.BYTES);
                while (waitMessages.size() > 0) {
                    try {
                        ds.receive(pack);
                        Message newMessage = new Message(pack);
                        if (newMessage.getType() == Message.TYPE.ANSWER) {
                            receiveAnswer(newMessage);
                        }
                    } catch (SocketTimeoutException e) {
                        // вышли по таймауту
                    } catch (IOException ioe) {
                        System.err.println(ioe.toString());
                        return;
                    }
                    repeatSend();
                }
            }
        });

        while (true) {
            if (streamStdin.ready()) { // Есть ли данные на stdin (не блокирующий вызов)
                sendBroadcast(new Message(Message.TYPE.MESSAGE, (thisName + streamStdin.readLine()).getBytes(Charset.forName("UTF-8"))));
            }
            try {
                ds.receive(pack_in); // Ждём сообщений по сокету (блокирующий на TIMEOUT)
                if (random.nextInt(100) >= loss) { // имитация потерь
                    Message newMessage = new Message(pack_in);
                    receiveMessage(newMessage);
                    sendAnswer(newMessage);
                }
            } catch (SocketTimeoutException e){
                //вышли по таймауту, ничего не делаем
            }
            repeatSend();
        }
    }

    private void receiveMessage(Message message) throws IOException {
        /* Разборка логики приходящих сообщений в зависимости от его типа
        * */
        ByteBuffer byteBuffer;
        switch (message.getType()){
            case Message.TYPE.ANSWER:
                addLog("receiveAnswer: " + message.getGuid());
                receiveAnswer(message);
                break;
            case Message.TYPE.CONNECT:
                addLog("connect new children: " + message.getAddress().toString());
                childrens.add(message.getAddress());
                break;
            case Message.TYPE.DEAD:
                if (childrens.contains(message.getAddress())) {
                    addLog("dead children: " + message.getAddress().toString());
                    System.err.println("dead children: " + message.getAddress().toString());
                    childrens.remove(message.getAddress());
                }
                break;
            case Message.TYPE.MESSAGE:
                if (!allMessages.contains(message)) {
                    System.out.println(new String(message.getData(), Charset.forName("UTF-8")));
                    allMessages.add(message);
                } else {
                    addLog("follow-up letter " + message.getGuid());
                }
                sendBroadcast(message);
                break;
            case Message.TYPE.PARENT:
                if (message.getData().length == 0) {
                    parent = null;
                } else {
                    byteBuffer = ByteBuffer.allocate(Integer.BYTES + 4); // 4 bytes == sizeof (InetAddress)
                    byteBuffer.put(message.getData());
                    byteBuffer.flip();
                    int newPortParent = byteBuffer.getInt();
                    //byteBuffer.flip();
                    byte[] newInetAddressParentByte = new byte[message.getData().length - Integer.BYTES];
                    byteBuffer.get(newInetAddressParentByte, 0, message.getData().length - Integer.BYTES);
                    //InetAddress newInetAddressParent = null;
                    addLog("size bytes addr new Parent = " + (message.getData().length - Integer.BYTES));
                    try {
                        InetAddress newInetAddressParent = InetAddress.getByAddress(newInetAddressParentByte);
                        parent = new Node(newInetAddressParent, newPortParent);
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                        addLog("Передан не верный Inetaddress нового Paren");
                        parent = null;
                    }
                }
                if (parent != null) {
                    if (parent.getInetAddress().equals(InetAddress.getLocalHost()) && parent.getPort() == thisPort) {
                        parent = null;
                        addLog("set new parent null");
                        System.err.println("You become the apex of the tree");
                        break;
                    }
                    Message messageConnect = new Message(Message.TYPE.CONNECT, new byte[0], parent);
                    sendMessage(messageConnect);
                }
                addLog("set new parent " + parent);
                if (parent != null) {
                    System.err.println("New parent " + parent);
                } else {
                    System.err.println("You become the apex of the tree");
                }
                break;
        }
    }
    private void receiveAnswer(Message message) {
        /* Убирает из очереди на ожидание сообщение, ответ на который пришёл с помощью message
        * */
        if (message.getType() == Message.TYPE.ANSWER) {
            ByteBuffer byteBuffer = ByteBuffer.allocate(Long.BYTES);
            byteBuffer.put(message.getData());
            byteBuffer.flip();
            long guidAnswer = byteBuffer.getLong();
            for (Iterator<Map.Entry<Message, Long>> it = waitMessages.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<Message, Long> entry = it.next();
                addLog("quidAswer=" + guidAnswer);
                if (entry.getKey().getGuid() == guidAnswer && entry.getKey().getAddress().equals(message.getAddress())) {
                    it.remove();
                    addLog("не ждём более " + guidAnswer);
                    break;
                }
            }
        }
    }
    private boolean connectParent(DatagramSocket ds) throws IOException {
        /* Реализует изначальный коннект с родителем, при неудаче возвращает false
        * при удаче, или при parent == null возвращает true */
        if (parent == null) {
            return true;
        }
        boolean connected = false;
        Message messageConnect = new Message(Message.TYPE.CONNECT, new byte[0], parent);
        sendMessage(messageConnect);

        DatagramPacket pack = new DatagramPacket(new byte[Message.SIZEHEAD + Long.BYTES], Message.SIZEHEAD + Long.BYTES);
        for (int i = 0; i < MAXCOUNTSEND; ++i) {
            try {
                ds.receive(pack);
                if (pack.getAddress().equals(parent.getInetAddress()) && pack.getPort() == parent.getPort()) {
                    Message messageAnswer = new Message(pack);
                    if (messageAnswer.getType() == Message.TYPE.ANSWER) {
                        ByteBuffer byteBuffer = ByteBuffer.allocate(Long.BYTES);
                        byteBuffer.put(messageAnswer.getData());
                        byteBuffer.flip();
                        if (byteBuffer.getLong() == waitMessages.keySet().iterator().next().getGuid()) {
                            addLog("received answer parent");
                            connected = true;
                            waitMessages.clear();
                            break;
                        }
                    }
                }
            } catch (SocketTimeoutException e){
                repeatSend();
            }
        }
        return connected;
    }
    private void sendBroadcast(Message message)  {
        /* Производим рассылку всему своему окружению, кроме адреса указанного в message,
        * если адрес в message == null то производим рассылку всему окружению */
        if (message.getType() == Message.TYPE.ANSWER){
            return;
        }
        if (parent != null) {
            if (!parent.equals(message.getAddress())) {
                sendMessage(new Message(message.getType(), message.getData(), parent));
            }
        }
        for (Node node: childrens) {
            if (!node.equals(message.getAddress())) {
                sendMessage(new Message(message.getType(), message.getData(), node));
            }
        }
    }
    private void sendAnswer(Message message)  {
        /* Отправляем ответ на сообщение, при условии что это не отввет на наше сообщение
        * */
        if (message.getType() == Message.TYPE.ANSWER) {
            return;
        }
        ByteBuffer byteBuffer = ByteBuffer.allocate(Long.BYTES);
        byteBuffer.putLong(message.getGuid());
        byteBuffer.flip();
        Message messageAnswer = new Message(Message.TYPE.ANSWER, byteBuffer.array(), message.getAddress());
        addLog("send " + messageAnswer.getGuid() +" answer to " + message.getGuid());
        sendMessage(messageAnswer);
    }
    private void sendMessage(Message message)  {
        /* Отпавляет собщения по адресу, указаному в message.
        *  Добавляет в очередь на ожидание ответа.
        *  Увеличивает счётчик отправленки.
        * */
        try {

            ds.send(message.getPacket());
        } catch (IOException e) {
            //e.printStackTrace();
            addLog("error send message " + message.getGuid());
        }
        message.setCountSent(message.getCountSent() + 1);
        if (message.getType() != Message.TYPE.ANSWER) {
            if (waitMessages.size() <= childrens.size() * MAXWAITMESSAGES) {
                waitMessages.put(message, System.currentTimeMillis());
                addLog("wait guid " + message.getGuid());
            } else {
                addLog("limit size wait, don't wait message " + message.getGuid());
            }
        }
    }
    private void repeatSend() {
        /* Производит повторную отправку сообщений, находящихся в очереди.
        * Так же удаляет их из очереди, если они были отправлены  >= MAXCOUNTSEND раз.
        * Если при удалении это оказался CONNECT то мы считаем, что не удалось подключиться к родителю -> Мы новый корень дерева*/
        for (Iterator<Map.Entry<Message, Long>> it = waitMessages.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Message, Long> entry = it.next();
            if (System.currentTimeMillis() - entry.getValue() > TIMESEND) {
                if (entry.getKey().getCountSent() >= MAXCOUNTSEND) {
                    if (entry.getKey().getType() == Message.TYPE.CONNECT) {
                        parent = null;
                    }
                    it.remove();
                    continue;
                }
                sendMessage(entry.getKey());
            }
        }
    }
    private void addLog(String str) {
        //System.err.println(str);
    }

    public static void main(String[] args) {
        Vertex vertex;
        try {
            vertex = new Vertex("Vertex-Parent", 10000, 50);
            vertex = new Vertex(InetAddress.getLocalHost(), 10000, "Vertex-1", 10001, 50);
            //vertex = new Vertex(InetAddress.getLocalHost(), 10000, "Vertex-2", 10002, 10);
            //vertex = new Vertex(InetAddress.getLocalHost(), 10001, "Vertex-3", 10003, 10);
            //vertex = new Vertex(InetAddress.getLocalHost(), 10001, "Vertex-4", 10004, 10);
            //vertex = new Vertex(InetAddress.getLocalHost(), 10002, "Vertex-5", 10005, 10);
            //vertex = new Vertex(InetAddress.getLocalHost(), 10004, "Vertex-6", 10006, 10);
            //vertex = new Vertex(InetAddress.getLocalHost(), 10003, "Vertex-7", 10007, 10);
            vertex.start();
        } catch (IOException e) {
            System.err.println(e.toString());
            System.exit(-1);
        }
    }
}

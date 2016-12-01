package laba_4;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Created by BODY on 21.11.2016.
 */
public class Vertex {
    private static final int MAXCOUNTSEND = 5;
    private static final int MAXSIZEMESSAGE = 1024;
    private static final int TIMESEND = 1000;
    private static final int TIMEOUT = TIMESEND / 3;

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
        try {
            ds = new DatagramSocket(thisPort);
            ds.setSoTimeout(TIMEOUT);
        } catch (SocketException e){
            System.err.println(e.toString());
            System.err.println("Error start new vetrex to port " + thisPort);
            return;
        }
        if (!connectParent(ds)) {
            System.err.println("error connect to parent");
            return;
        }
        BufferedReader streamStdin = new BufferedReader(new InputStreamReader(System.in));
        DatagramPacket pack_in = new DatagramPacket(new byte[MAXSIZEMESSAGE], MAXSIZEMESSAGE);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.out.println("Shutdown hook ran!");
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
                    if (!node.equals(parent)) {
                        sendMessage(new Message(messageNewParent.getType(), messageNewParent.getData(), node));
                    }
                }
                DatagramPacket pack = new DatagramPacket(new byte[Message.SIZEHEAD + Long.BYTES], Message.SIZEHEAD + Long.BYTES);
                while (childrens.size() > 0) {
                    try {
                        ds.receive(pack);
                        Message newMessage = new Message(pack);
                        receiveMessage(newMessage);
                        sendAnswer(newMessage);
                        //ljltkfnm
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
            if (streamStdin.ready()) {
                sendBroadcast(new Message(Message.TYPE.MESSAGE, (thisName + streamStdin.readLine()).getBytes(Charset.forName("UTF-8"))));
            }
            try {
                ds.receive(pack_in);
                if (random.nextInt(100) >= loss) {
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
        /* Разборка логики приходящих сообщений
        * */
        System.err.println("receiveAnswer:");
        ByteBuffer byteBuffer;
        switch (message.getType()){
            case Message.TYPE.ANSWER:
                System.err.printf("receiveAnswer%s guid=%d sizeData=%d ",message.getAddress(), message.getGuid(), message.getData().length);
                byteBuffer = ByteBuffer.allocate(Long.BYTES);
                byteBuffer.put(message.getData());
                byteBuffer.flip();
                long guidAnswer = byteBuffer.getLong();
                for (Iterator<Map.Entry<Message, Long>> it = waitMessages.entrySet().iterator(); it.hasNext(); ) {
                    Map.Entry<Message, Long> entry = it.next();
                    System.err.println("quidAswer=" + guidAnswer);
                    if (entry.getKey().getGuid() == guidAnswer && entry.getKey().getAddress().equals(message.getAddress())) {
                        it.remove();
                        System.err.println("не ждём более "  + guidAnswer);
                        break;
                    }
                }
                break;
            case Message.TYPE.CONNECT:
                System.err.println("connect new children: " + message.getAddress().toString());
                childrens.add(message.getAddress());
                break;
            case Message.TYPE.DEAD:
                if (childrens.contains(message.getAddress())) {
                    System.err.println("dead children: " + message.getAddress().toString());
                    childrens.remove(message.getAddress());
                }
                break;
            case Message.TYPE.MESSAGE:
                if (!allMessages.contains(message)) {
                    System.out.println(new String(message.getData(), Charset.forName("UTF-8")));
                    allMessages.add(message);
                } else {
                    System.err.println("follow-up letter " + message.getGuid());
                }
                sendBroadcast(message);
                break;
            case Message.TYPE.PARENT:
                if (message.getData().length == 0) {
                    parent = null;
                } else {
                    byteBuffer = ByteBuffer.allocate(10);
                    byteBuffer.put(message.getData());
                    byteBuffer.flip();
                    int newPortParent = byteBuffer.getInt();
                    //byteBuffer.flip();
                    byte[] newInetAddressParentByte = new byte[message.getData().length - Integer.BYTES];
                    byteBuffer.get(newInetAddressParentByte, 0, message.getData().length - Integer.BYTES);
                    //InetAddress newInetAddressParent = null;
                    System.err.println("size bytes addr new Parent = " + (message.getData().length - Integer.BYTES));
                    try {
                        InetAddress newInetAddressParent = InetAddress.getByAddress(newInetAddressParentByte);
                        parent = new Node(newInetAddressParent, newPortParent);
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                        System.err.println("Передан не верный Inetaddress нового Paren");
                        parent = null;
                    }
                }
                System.err.println("set new parent " + parent);
                connectParent(ds);
                break;
        }
    }
    private boolean connectParent(DatagramSocket ds) throws IOException {
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
                            System.err.println("пришло подтверждение от родителя");
                            connected = true;
                            waitMessages.clear();
                            break;
                        }
                    }
                }
            } catch (SocketTimeoutException e){
                //вышли по таймауту
                //sendMessage(messageConnect);
                repeatSend();
            }
        }
        return connected;
    }
    private void sendBroadcast(Message message)  {
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
        if (message.getType() == Message.TYPE.ANSWER) {
            return;
        }
        ByteBuffer byteBuffer = ByteBuffer.allocate(Long.BYTES);
        byteBuffer.putLong(message.getGuid());
        byteBuffer.flip();
        Message messageAnswer = new Message(Message.TYPE.ANSWER, byteBuffer.array(), message.getAddress());
        sendMessage(messageAnswer);
    }
    private void sendMessage(Message message)  {
        /* отпавляет собщения по адресу, указаному в message
        *  Добавляет в очередь на ожидание ответа
        *  Увеличивает счётчик отправленки
        * */
        try {

            ds.send(message.getPacket());
        } catch (IOException e) {
            //e.printStackTrace();
            System.err.println("error send message ");
        }
        message.setCountSent(message.getCountSent() + 1);
        if (message.getType() != Message.TYPE.ANSWER) {
            waitMessages.put(message, System.currentTimeMillis());
            System.err.println("wait guid " + message.getGuid());
        }
    }
    private void repeatSend() {
        for (Iterator<Map.Entry<Message, Long>> it = waitMessages.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Message, Long> entry = it.next();
            if (System.currentTimeMillis() - entry.getValue() > TIMESEND) {
                sendMessage(entry.getKey());
                if (entry.getKey().getCountSent() >= MAXCOUNTSEND) {
                    it.remove();
                }
            }
        }
    }

    public static void main(String[] args) {
        Vertex vertex;
        try {
            //vertex = new Vertex("Vertex-Parent", 10000, 0);
            //vertex = new Vertex(InetAddress.getLocalHost(), 10000, "Vertex-1", 10001, 0);
            //vertex = new Vertex(InetAddress.getLocalHost(), 10000, "Vertex-2", 10002, 0);
            vertex = new Vertex(InetAddress.getLocalHost(), 10001, "Vertex-3", 10003, 0);
            //vertex = new Vertex(InetAddress.getLocalHost(), 10001, "Vertex-4", 10004, 0);
            //vertex = new Vertex(InetAddress.getLocalHost(), 10002, "Vertex-5", 10005, 0);
            //vertex = new Vertex(InetAddress.getLocalHost(), 10004, "Vertex-6", 10006, 0);
            vertex.start();
        } catch (IOException e) {
            System.err.println(e.toString());
            System.exit(-1);
        }
    }
}

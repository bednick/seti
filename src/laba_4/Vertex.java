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

    private Map<Message, Long> waitMessages; // хранит все сообщения, ожидающие ответ(о их доставки до всех) и их время последнего отправления
    //private Queue<Message> allMessages;      // хранит в себе определённое количество последних сообщений

    public Vertex(InetAddress parentIA, int parentPort, String thisName, int thisPort) {
        this.parent = new Node(parentIA, parentPort);
        this.thisName = thisName + ": ";
        this.thisPort = thisPort;
        this.ds = null;
        childrens = new ArrayList<>();
        waitMessages = new HashMap<>();
    }
    public Vertex(String thisName, int thisPort) {
        this.parent = null;
        this.thisName = thisName + ": ";
        this.thisPort = thisPort;
        this.ds = null;
        childrens = new ArrayList<>();
        waitMessages = new HashMap<>();
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
                // сообщить всем, что я умер
            }
        });

        while (true) {
            if (streamStdin.ready()) {
                sendBroadcast(new Message(Message.TYPE.MESSAGE, (thisName + streamStdin.readLine()).getBytes(Charset.forName("UTF-8"))));
            }
            try {
                ds.receive(pack_in);
                Message newMessage = new Message(pack_in);
                receiveMessage(newMessage);
                sendAnswer(newMessage);
            } catch (SocketTimeoutException e){
                //вышли по таймауту, ничего не делаем
            }
            for (Iterator<Map.Entry<Message, Long>> it = waitMessages.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<Message, Long> entry = it.next();
                if (System.currentTimeMillis() - entry.getValue() > TIMESEND) {
                    sendMessage(entry.getKey());
                    //waitMessages.replace(entry.getKey(), System.currentTimeMillis()); // Будет ли правильно работать? Вроде должна
                    if (entry.getKey().getCountSent() >= MAXCOUNTSEND) {
                        it.remove();
                    }
                }
            }
        }
    }

    private void receiveMessage(Message message) {
        /* Разборка логики приходящих сообщений
        * */
        switch (message.getType()){
            case Message.TYPE.ANSWER:
                System.err.printf("receiveAnswer%s guid=%d sizeData=%d\n",message.getAddress(), message.getGuid(), message.getData().length);
                for (Iterator<Map.Entry<Message, Long>> it = waitMessages.entrySet().iterator(); it.hasNext(); ) {
                    Map.Entry<Message, Long> entry = it.next();
                    ByteBuffer byteBuffer = ByteBuffer.allocate(Long.BYTES);
                    byteBuffer.put(message.getData());
                    byteBuffer.flip();
                    if (entry.getKey().getGuid() == byteBuffer.getLong() && entry.getKey().getAddress().equals(message.getAddress())) {
                        it.remove();
                        break;
                    }
                }
                break;
            case Message.TYPE.CONNECT:
                childrens.add(message.getAddress());
                break;
            case Message.TYPE.DEAD:
                if (childrens.contains(message.getAddress())) {
                    childrens.remove(message.getAddress());
                }
                break;
            case Message.TYPE.MESSAGE:
                System.out.println(new String(message.getData(), Charset.forName("UTF-8")));
                //sendBroadcast(message);
                break;
            case Message.TYPE.PARENT:
                if (message.getData().length == 0) {
                    parent = null;
                } else {
                    ByteBuffer byteBuffer = ByteBuffer.allocate(10);
                    byteBuffer.put(message.getData());
                    byteBuffer.flip();
                    int newPortParent = byteBuffer.getInt();
                    byteBuffer.flip();
                    byte[] newInetAddressParentByte = new byte[message.getData().length - Integer.BYTES];
                    byteBuffer.get(newInetAddressParentByte, 0, message.getData().length - Integer.BYTES);
                    //InetAddress newInetAddressParent = null;
                    try {
                        InetAddress newInetAddressParent = InetAddress.getByAddress(newInetAddressParentByte);
                        parent = new Node(newInetAddressParent, newPortParent);
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                        System.err.println("Передан не верный Inetaddress нового Paren");
                        parent = null;
                    }
                }
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
                sendMessage(messageConnect);
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

    public static void main(String[] args) {
        Vertex vertex;
        try {
            vertex = new Vertex("Vertex-Parent", 10001);
            vertex = new Vertex(InetAddress.getLocalHost(), 10001, "Vertex-1", 10002);
            vertex.start();
        } catch (IOException e) {
            System.err.println(e.toString());
            System.exit(-1);
        }
    }
}

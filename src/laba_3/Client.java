package laba_3;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Created by BODY on 17.10.2016.
 */
public class Client {
    private int port;
    private InetAddress inetAddress;
    private String nameFile;
    private static final int SIZEBUF = 1024 * 1024;
    private static final int LENGTH = (4 * 1024) + 8 + (4 * 2);
    private File file;

    public Client(InetAddress inetAddress, int port, String nameFile) {
        this.inetAddress = inetAddress;
        this.port = port;
        this.nameFile = nameFile;
    }
    public void start() throws IOException {
        OutputStream stream = null;
        InputStream streamIn = null;
        FileInputStream fileStream = null;
        Socket socket = null;
        try {
            socket = new Socket(inetAddress, port);
            fileStream = openFile();
            long fileSize = file.length();
            streamIn = socket.getInputStream();
            stream = socket.getOutputStream();


            ByteBuffer byteBuffer = ByteBuffer.allocate(LENGTH);//передать размер имени (int)
            byteBuffer.putLong(fileSize);//передать размер файла
            byteBuffer.putInt(file.getName().getBytes(Charset.forName("UTF-8")).length);//передать имя
            byteBuffer.put(file.getName().getBytes(Charset.forName("UTF-8")));
            stream.write(byteBuffer.array(), 0, byteBuffer.position());                        // отправляем
            int count;
            byte[] buf = new byte[SIZEBUF];
            while ( (count = fileStream.read(buf)) > 0) { //передать файл
                //System.out.println(new String(buf, "UTF-8"));
                stream.write(buf, 0 , count);
            }
            //stream.flush();
            socket.shutdownOutput();

            ByteBuffer resultBuffer = ByteBuffer.allocate(4);
            resultBuffer.putInt(1);
            resultBuffer.flip();
            int readBytes = 0;
            while (readBytes < 4) {
                //System.err.println("wait");
                count = streamIn.read(resultBuffer.array());
                if (count == -1) {
                    System.err.println("Ошибка получения результата");
                    System.exit(-1);
                }
                readBytes += count;
                //System.err.println(readBytes);
            }

            int result = resultBuffer.getInt();
            if (result == 0) {
                System.out.println("Файл доставлен");
            } else {
                System.err.println("ошибка при доставки файла: " + result);
            }

        } finally {
            if(stream != null){
                stream.close();
            }
            if(streamIn != null){
                streamIn.close();
            }
            if(fileStream != null) {
                fileStream.close();
            }
            if(socket != null){
                socket.close();
            }
        }
    }
    private FileInputStream openFile() throws IOException {
        file = new File(nameFile);
        if(file.isFile()){
            if(file.canRead()){
                return new FileInputStream(file);
            }
        }
        throw new IOException(nameFile + " is not file or can not read");
    }
    public void start(int port, InetAddress inetAddress, String nameFile) throws IOException {
        this.inetAddress = inetAddress;
        this.port = port;
        this.nameFile = nameFile;
        start();
    }

    public static void main(String[] args) {
        try {
            Client client = new Client(InetAddress.getLocalHost(), SelectorServer.PORT, "123");
            client.start();
        } catch (IOException e){
            System.err.println(e.toString());
        }
    }
}

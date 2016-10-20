package laba_3;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

/**
 * Created by BODY on 17.10.2016.
 */
public class Client {
    private int port;
    private InetAddress inetAddress;
    private String nameFile;
    private static final int SIZEBUF = 1024 * 1024;
    private File file;

    public Client(InetAddress inetAddress, int port, String nameFile) {
        this.inetAddress = inetAddress;
        this.port = port;
        this.nameFile = nameFile;
    }
    public void start() throws IOException {
        OutputStream stream = null;
        FileInputStream fileStream = null;
        Socket socket = null;
        try {
            socket = new Socket(inetAddress, port);
            fileStream = openFile();
            byte[] buf = new byte[SIZEBUF];
            byte[] byteNameFile = nameFile.getBytes("UTF-8");
            stream = socket.getOutputStream();
            ByteBuffer byteBuffer = ByteBuffer.allocate(byteNameFile.length);//передать размер имени (int)
            byteBuffer.put(byteNameFile);                                   //передать имя
            byteBuffer.putLong(file.length());                              //передать размер файла
            stream.write(byteBuffer.array());                               // отправляем
            int count;
            while ( (count = fileStream.read(buf)) > 0) { //передать файл
                System.out.println(new String(buf, "UTF-8"));
                stream.write(buf, 0 , count);
            }
            stream.flush();
        } finally {
            if(socket != null){
                socket.close();
            }
            if(stream != null){
                stream.close();
            }
            if(fileStream != null) {
                fileStream.close();
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
            System.out.println(e.toString());
        }
    }
}

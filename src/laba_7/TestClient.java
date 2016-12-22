package laba_7;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.charset.Charset;

/**
 * Created by BODY on 22.12.2016.
 */
public class TestClient {

    public static void main(String[] args) {

        try {
            Socket socket = new Socket(InetAddress.getByName("localhost"), Forwarder.PORT);

            BufferedReader streamStdin = new BufferedReader(new InputStreamReader(System.in));
            ByteBuffer buffer = ByteBuffer.allocate(1024);

            OutputStream streamOut = socket.getOutputStream();
            BufferedReader streamIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            while (true) {
                if (streamStdin.ready()) {
                    buffer.put(streamStdin.readLine().getBytes(Charset.forName("UTF-8")));
                    streamOut.write(buffer.array(), 0, buffer.position());
                    System.err.println("write :" + new String(buffer.array(), Charset.forName("UTF-8")));
                    buffer.clear();
                }
                if (streamIn.ready()) {
                    streamIn.read(buffer.asCharBuffer());
                    System.err.println("Echo: " + new String(buffer.array(), Charset.forName("UTF-8")));
                    buffer.clear();
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    //
                }
            }

        } catch (IOException e) {
            System.err.println(e.toString());
            System.exit(-1);
        }
    }
}

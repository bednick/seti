package laba_4;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;

/**
 * Created by BODY on 21.11.2016.
 */
public class Laba_4 {

    public static void main(String[] args) throws InterruptedException, IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(4 + 10);
        byte[] bytes = new byte[10];
        bytes[1] = -1;
        bytes[2] = 1;
        bytes[3] = 2;
        bytes[4] = 33;
        bytes[5] = 4;
        bytes[6] = 51;
        bytes[7] = 51;
        byteBuffer.putInt(12345);
        byteBuffer.put(bytes);
        byteBuffer.flip();
        byte[] newbytes = new byte[10];
        System.out.println(byteBuffer.getInt());
        byteBuffer.get(newbytes);
        System.out.println(newbytes[0]);
        System.out.println(newbytes[1]);
        System.out.println(newbytes[2]);
        System.out.println(newbytes[5]);

        /*System.out.println("START TEST SKANER");
        Scanner in = new Scanner(System.in);


        while (true) {
            System.err.println("start wait");
            Thread.sleep(1000);
            System.err.println("finish wait");
            if (in.hasNextLine()) {
                System.out.println("has next + " + in.nextLine());
            } else {
                System.out.println("! has next");
            }
        }*/


        System.out.println("Long.bytes " + Long.BYTES);
        System.out.println("Integer.SIZE " + Integer.SIZE);

        System.out.println("START TEST BR");
        BufferedReader bistream = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            System.err.println("start wait");
            Thread.sleep(2000);
            System.err.println("finish wait");
            if (bistream.ready()) {
                System.out.println(bistream.readLine());
            } else {
                System.out.println("not data stdin");
            }
        }
    }
}

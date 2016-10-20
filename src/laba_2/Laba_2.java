package laba_2;

import java.io.IOException;

/**
 * Created by BODY on 12.10.2016.
 */
public class Laba_2{
    public static final int PORT = 3128;

    public static void main(String[] args) {
        try {
            SelectorServer selectorServer = new SelectorServer(PORT);
            selectorServer.start();
        } catch (IOException e){
            System.err.println(e.toString());
            e.printStackTrace();
        }
    }

}

package laba_1;

import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Программа для подсчёта кочилества своих копий, запущенных в локальной сети
 */
public class Laba_1 {
    public static final int PORT =  40404;
    private static final int TIMEOUT = 1000;
    private static final int TIMELIVE = 5000;

    public static void main(String[] args) {
        DatagramSocket ds = null;
        try {
            try {
                ds = new DatagramSocket(PORT);
            } catch (SocketException e){
                System.err.println(e.toString());
                System.err.println("Не удалось запустить программу на машине. Возможно она уже на ней запущенна");
                return;
            }
            DatagramPacket pack_1 = new DatagramPacket(new byte[0], 0, InetAddress.getByName("255.255.255.255"), PORT);
            DatagramPacket pack_2 = new DatagramPacket(new byte[1], 1);
            HashMap<InetAddress, Long> environment = new HashMap<>();
            ds.setSoTimeout(TIMEOUT);
            long lastTime = System.currentTimeMillis();

            while(true){
                    if( System.currentTimeMillis() - lastTime > TIMEOUT ) {
                        lastTime = System.currentTimeMillis();
                        ds.send(pack_1);
                        if(checkEnvironment(environment)){
                            printEnvironment(environment, "Окружение уменьшилось");
                        }
                    }
                    try {
                        ds.receive(pack_2);
                        if ( !environment.containsKey(pack_2.getAddress()) ) {
                            environment.put(pack_2.getAddress(), System.currentTimeMillis());
                            printEnvironment( environment, "Окружение увеличилось" );
                        } else {
                            environment.put(pack_2.getAddress(), System.currentTimeMillis());
                        }
                    } catch (SocketTimeoutException e){
                        //вышли по таймауту, ничего не делаем
                    }
            }
        }  catch (IOException e) {
            System.err.println(e.toString());
            ds.close();
        }
    }
    private static boolean checkEnvironment(HashMap<InetAddress, Long> environment){
        boolean flag = false;
        for(Iterator<HashMap.Entry<InetAddress, Long>> it = environment.entrySet().iterator(); it.hasNext();){
            HashMap.Entry<InetAddress, Long> var = it.next();
            if(System.currentTimeMillis() - var.getValue() > TIMELIVE){
                it.remove();
                flag = true;
            }
        }
        return flag;
    }
    private static void printEnvironment(HashMap<InetAddress, Long> environment, String email){
        System.out.println();
        System.out.println(email);
        System.out.println("В сети "+ environment.size() + " копий программы:");
        for(Iterator<HashMap.Entry<InetAddress, Long>> it = environment.entrySet().iterator(); it.hasNext();){
            HashMap.Entry<InetAddress, Long> var = it.next();
            System.out.println(var.getKey());
        }
        System.out.println();
    }
}

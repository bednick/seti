package laba_4;

import java.net.InetAddress;

/**
 * Created by BODY on 22.11.2016.
 */
public class Node {
    private InetAddress inetAddress;
    private int port;

    public Node(InetAddress inetAddress, int port) {
        this.inetAddress = inetAddress;
        this.port = port;
    }
    public InetAddress getInetAddress(){
        return inetAddress;
    }
    public int getPort() {
        return port;
    }
    @Override
    public int hashCode() {
        return inetAddress.hashCode() | port;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Node other = (Node) obj;
        if (port != other.port)
            return false;
        if (!inetAddress.equals(other))
            return false;

        return true;
    }
    @Override
    public String toString() {
        return inetAddress.toString() + " " + port;
    }
}

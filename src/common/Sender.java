package common;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

//メッセージを送るやつ
public class Sender {

    public static void sendMessage(String message) {
        MulticastSocket socket = null;
        InetAddress mcastAddress;
        try {
            //マルチキャストソケットを作成
            socket = new MulticastSocket(Config.MCAST_PORT);
            mcastAddress = InetAddress.getByName(Config.MCAST_ADDRESS);
            byte[] buf = message.getBytes(Config.ENCODING);
            DatagramPacket sendPacket = new DatagramPacket(buf, buf.length, mcastAddress, Config.MCAST_PORT);
            socket.send(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }
}

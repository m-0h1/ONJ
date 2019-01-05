package sample;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import javafx.application.Platform;
import java.net.SocketTimeoutException;

public class Receiver extends Thread {
    MulticastSocket socket = null;
    InetAddress mcastAddress;

    Controller controller;
    boolean runnning = true;

    public void exit(){
        runnning = false;
    }

    public Receiver(Controller controller){
        this.controller = controller;
    }

    @Override
    public void run() {
        try {
            //マルチキャストソケットを作成
            socket = new MulticastSocket(Config.MCAST_PORT);
            mcastAddress = InetAddress.getByName(Config.MCAST_ADDRESS);
            //マルチキャストグループに参加
            socket.joinGroup(mcastAddress);
        }catch (IOException e){
            e.printStackTrace();
        }
        byte buf[] = new byte[Config.PACKET_SIZE];
        DatagramPacket receivePacket = new DatagramPacket(buf, buf.length);


        while (runnning) {
            //受信
            try {
                socket.setSoTimeout(1000); //1秒でいったん受信処理抜ける
                socket.receive(receivePacket);
                    String receiveMessage = new String(buf, 0, receivePacket.getLength(), Config.ENCODING);
                    Platform.runLater(() -> controller.receiveMessage(receiveMessage));
                    //controller.receiveMessage(receiveMessage);
                    //Applicationのスレッド外のからApplicationを操作することはできないのでお任せする
            } catch(IOException e){
              //e.printStackTrace();　タイムアウトしてもする～
                // タイムアウトしないとApplicationが終了しても受信処理(receive)を続けてしまう
            }
        }

        if (socket != null)
            socket.close();
    }
}

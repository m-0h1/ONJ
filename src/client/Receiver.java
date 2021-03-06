package client;

import common.Config;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import javafx.application.Platform;

public class Receiver extends Thread {

    private Controller controller;
    private boolean running = true;

    void exit(){
        running = false;
    }

    Receiver(Controller controller){
        this.controller = controller;
    }

    @Override
    public void run() {
        MulticastSocket socket = null;
        InetAddress mcastAddress;
        try {
            //マルチキャストソケットを作成
            socket = new MulticastSocket(Config.MCAST_PORT);
            mcastAddress = InetAddress.getByName(Config.MCAST_ADDRESS);
            //マルチキャストグループに参加
            socket.joinGroup(mcastAddress);

            ////}catch (IOException e){
            ////    e.printStackTrace();
            ////}
            byte buf[] = new byte[Config.PACKET_SIZE];
            DatagramPacket receivePacket = new DatagramPacket(buf, buf.length);

            //試し打ち…　必要っぽい？
            DatagramPacket sendPacket = new DatagramPacket(buf, buf.length, mcastAddress,
                    Config.MCAST_PORT);
            socket.send(sendPacket);


            while (running) {
                //受信
                try {
                    //System.out.println("socket is " + socket.isClosed());
                    socket.setSoTimeout(1000); //1秒でいったん受信処理抜ける
                    socket.receive(receivePacket);
                    String receiveMessage = new String(buf, 0, receivePacket.getLength(), Config.ENCODING);
                    Platform.runLater(() -> controller.receiveMessage(receiveMessage));
                    //controller.receiveMessage(receiveMessage);
                    //Applicationのスレッド外のからApplicationを操作することはできないのでお任せする
                    System.out.println("受信 >> " + receiveMessage);
                } catch (IOException e) {
                    //e.printStackTrace();　タイムアウトしてもする～
                    // タイムアウトしないとApplicationが終了しても受信処理(receive)を続けてしまう
                    //System.out.println("time out");
                }

            }
        } catch(IOException e){
            e.printStackTrace();
        }finally {
            if (socket != null)
                socket.close();
        }
    }
}

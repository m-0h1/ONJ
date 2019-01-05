package sample;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import javafx.scene.layout.AnchorPane;

//UDP通信用
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
//メッセージ受信用　thread的な奴
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.application.Platform;
/*
import javafx.fxml.Initializable;
import java.net.URL;
import java.util.ResourceBundle;
*/

public class Controller {

    /*
    //初期化はimplements Initializableして↓のメソッドをoverrideしてもOK
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        istView.setItems(list);
    }
    */

    //最初に呼び出される。ここでリストのインスタンスを作るなど
    @FXML
    void initialize() {
        messageListView.setItems(messageList);  //listに追加されたものはlistViewへ自動的に反映される
        memberListView.setItems(memberList);

        //メッセージ受信用のスレッド的なもの開始 これは結局使わない
        //ss.start();
    }


    @FXML
    private ListView<String> messageListView; //メッセージリスト表示部
    //メッセージリストの中身
    ObservableList<String> messageList = FXCollections.observableArrayList("a1","b2","c3");
    @FXML
    private TextArea messageTextArea;//メッセージ入力欄
    @FXML
    private Button messageSendButton;    //メッセージ送信ボタン
    @FXML
    private Label titleLabel;   //タイトル表示部
    @FXML
    private Label timerLabel;   //タイマー表示部
    @FXML
    private ListView<String> memberListView;    //メンバー表示部
    //メンバーリストの中身
    ObservableList<String> memberList = FXCollections.observableArrayList(
            "kasyukiyomitsu","iwatoshi","daihannyanagamitsu");
    @FXML
    private Label selectMemberLabel;    //選択したメンバーの表示部
    @FXML
    private Button memberSendButton;    //選択したメンバーの情報を送信する用ボタン

    @FXML
    private AnchorPane rootPane;        //がめん

    //メンバーリストで選択されたメンバーをselectMemberLabelに表示する
    public void memberSelected(){
        String member = memberListView.getSelectionModel().getSelectedItem();
        selectMemberLabel.setText(member + " を選択する");
    }

    //メッセージ送信ボタンをクリックすると呼び出されるメソッド
    public void messageSendButtonClick()
    {
        String sendMessage = messageTextArea.getText();
        //データをマルチキャスト送信
        Sender.sendMessage(sendMessage);
        messageTextArea.clear();
    }

    //サーバからメッセージを受信したときに追加する
    public void receiveMessage(String receiveMessage) {
        //構文解析！！！！
        //メッセージだったとき    MM::で始まる
        messageList.add(receiveMessage);
        //メンバー追加だったとき AM::で始まる
        memberList.add(name);
        //メンバー削除だったとき RM::で始まる
        memberList.remove(name);

        //他にもいろいろあるだろうな
    }


    //参加しているか？
    private boolean isAttend = false;
    //名前
    private String name = null;

    public boolean isAttend() {
        return isAttend;
    }

    @FXML
    private Button attendLeaveButton;   //参加or退席ボタン

    //参加or退席ボタンクリック
    public void attendLeaveButtonClick(){
        //参加していた
        if(isAttend){
            //退席処理
            isAttend = false;
            //メンバーリストから名前を消す
            Sender.sendMessage("RM::" + name);
            //memberList.remove(name);

            //メッセージ送信できないようになる
            messageSendButton.setDisable(true);
            messageTextArea.setText(name);
            //退席→参加にする
            attendLeaveButton.setText("参加");

            //いろいろ使えないようになる
            memberListView.setDisable(true);
            selectMemberLabel.setDisable(true);
            memberSendButton.setDisable(true);
        }else{
            //出席処理
            isAttend = true;
            //メンバーリストに名前を追加
            String name = messageTextArea.getText();

            //nameが改行あり状態なので改行をとりのぞくとかしたいが
            this.name = name;
            //memberList.add(name);
            Sender.sendMessage("AM::" + name);
            //サーバーにいったん送信して受信せなあかん

            //コメントが送信できるようになる
            messageSendButton.setDisable(false);
            messageTextArea.setPromptText("メッセージを入力");
            messageTextArea.clear();
            //参加→退席にする
            attendLeaveButton.setText("退席");

            //いろいろ使えるようになる
            memberListView.setDisable(false);
            selectMemberLabel.setDisable(false);
            memberSendButton.setDisable(false);
        }
    }




    /*　これ結局使わない
    //メッセージを受信する用のスレッド的なやつ、これを何度も繰り返す
    ScheduledService ss  = new ScheduledService(){
    @Override
        protected Task createTask(){
            Task task = new Task(){
                @Override
                protected Boolean call() throws Exception {
                    MulticastSocket socket = null;
                        try {
                        //マルチキャストソケットを作成
                        socket = new MulticastSocket(Config.MCAST_PORT);
                        InetAddress mcastAddress = InetAddress.getByName(Config.MCAST_ADDRESS);
                        //マルチキャストグループに参加
                        socket.joinGroup(mcastAddress);
                            byte buf[] = new byte[Config.PACKET_SIZE];
                            DatagramPacket receivePacket = new DatagramPacket(buf, buf.length);
                            //受信
                            if(!socket.isClosed()) {
                                socket.receive(receivePacket);
                                String receiveMessage = new String(buf, 0, receivePacket.getLength(), Config.ENCODING);
                                Platform.runLater(() -> messageList.add(receiveMessage));
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    finally {
                        if (socket != null) {
                            socket.close();
                        }
                    }
                    return true;
                };
            };
            return task;
        }
    };
    */

}

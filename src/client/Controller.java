package client;

import common.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;

//メッセージ受信用　thread的な奴
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
        attendanceListView.setItems(attendanceList);

        //メッセージ受信用のスレッド的なもの開始 これは結局使わない
        //ss.start();
    }

    @FXML
    private ListView<String> messageListView; //メッセージリスト表示部
    //メッセージリストの中身
    private ObservableList<String> messageList = FXCollections.observableArrayList();
    @FXML
    private TextArea messageTextArea;//メッセージ入力欄
    @FXML
    private Button messageSendButton;    //メッセージ送信ボタン
    @FXML
    private ImageView titleImageView;
    //private Label titleLabel;   //タイトル表示部
    @FXML
    private Label timerLabel;   //タイマー表示部
    @FXML
    private ListView<String> memberListView;    //メンバー表示部
    //メンバーリストの中身
    private ObservableList<String> memberList = FXCollections.observableArrayList();
    @FXML
    private Label selectMemberLabel;    //選択したメンバーの表示部
    @FXML
    private Button memberSendButton;    //選択したメンバーの情報を送信する用ボタン

    @FXML
    private AnchorPane rootPane;        //がめん

    @FXML
    private Label nameLabel;    //名前表示部
    @FXML
    private Label roleLabel;    //役職表示部

    @FXML
    private Button enterButton;   //再戦ボタン


    /////////////参加者（いる人）リスト！！！！！
    @FXML
    private ListView<String> attendanceListView;    //出席者（いる人）表示部
    //いる人リストの中身
    private ObservableList<String> attendanceList = FXCollections.observableArrayList();    ;


    //参戦ボタンクリック
    public void enterButtonClick(){    //ボタンの名前　gameButtonとかenterButtonとかにしたい

        if(isAttendGame){  //参加中　　　　やめる
            Sender.sendMessage("all::" + myName + "::RGM::" + myName); //キャンセル連絡
            isAttendGame=false;
            enterButton.setText("参戦");
            attendLeaveButton.setDisable(false);//退席できるようにする
        }else{             //参加してない　これからする
            //再戦希望を送る
            Sender.sendMessage("all::" + myName + "::EN::"+myName);
            //enterButton.setDisable(true);
            isAttendGame=true; //////////////////ゲーム参加中
            enterButton.setText("取消");
            attendLeaveButton.setDisable(true);//退席できないようにする
        }

    }


    //メンバーリストで選択されたメンバーをselectMemberLabelに表示する
    public void memberSelected(){
        String member = memberListView.getSelectionModel().getSelectedItem();
        selectMemberLabel.setText(member + " を選択する");
    }

    //選択メンバー送信ボタンをクリックしたときに呼び出されるメソッド
    public void memberSendButtonClick(){
        //選択されたメンバーを送信
        if(command == null){    //投票の時はcommand == VTです
            switch (myRole){
                case "占い師":
                    command = "FM";
                    break;
                case "怪盗":
                    command = "PM";
                    break;
            }
        }
        //Sender.sendMessage("server::" + myName + "::"+ command +"::" + selectMemberLabel.getText());
        Sender.sendMessage("server::" + myName + "::"+ command +"::" + memberListView.getSelectionModel().getSelectedItem());
        memberSendButton.setDisable(true); //使えなくなる
        command = null;
        messageList.add("あなたは【"+ memberListView.getSelectionModel().getSelectedItem() + "】を選択しました");
    }

    //メッセージ送信ボタンをクリックすると呼び出されるメソッド
    public void messageSendButtonClick() {
        String sendMessage = messageTextArea.getText();
        //データをマルチキャスト送信
        //コマンド付き！
        if(!sendMessage.equals("")) {   //空白はダメ～
            Sender.sendMessage("all::" + myName + "::CM::" + sendMessage);
            messageTextArea.clear();
        }
    }

    //外部からメッセージを受信したときに追加する
    void receiveMessage(String receiveMessage) {

        //構文解析！！！ public String[] split(String regex, int limit) 文字列の分割

        //receiveMessageは"宛先[0]::送信者[1]::コマンド[2]::本文[3]"という想定
        String[]  strings = receiveMessage.split("::");

        if(isAttend) {    //////////////////////////ざっくりガードしてみた

            if (strings[0].equals("all") || strings[0].equals(myName)) {

                switch (strings[2]) {
                    //メッセージだったとき    CM
                    case "CM":
                        messageList.add(strings[1] + ">" + strings[3]);
                        break;

                    //チャットメンバー追加だったとき ACM
                    case "ACM":
                        /////////////リスト別のに////////////////////////////////////////////
                        if(!myName.equals(strings[1])) { //自分の名前はいったん無視で
                            attendanceList.add(strings[1]);          /////////////リスト別のに
                        } //別のにした///////////////////
                        //////////////////////////////////////////////////////////////
                        //メッセージ表示
                        messageList.add(strings[1] + "がチャットに参加しました");
                        break;
                    //ゲームメンバー追加だったとき EN
                    case "EN":
                        memberList.add(strings[1]);
                        //メッセージ表示
                        messageList.add(strings[1] + "がゲームに参加しました");
                        break;

                    //ちゃっとメンバー削除だったとき RCM
                    case "RCM":
                        //memberList.remove(strings[1]);               /////リスト別のに///////////
                        attendanceList.remove(strings[1]);          //別のにした///////////////////
                        messageList.add(strings[1] + "が退席しました");
                        break;
                    //ゲームメンバー削除だったとき RGM
                    case "RGM":
                        memberList.remove(strings[1]);
                        messageList.add(strings[1] + "がゲーム参加をやめました");
                        break;
                    //参加時、チャットメンバーリストを渡されたとき
                    case "CML":
                        this.createChatMemberListView(strings[3]);//リストに入れてく
                        break;
                    //参加時、ゲームメンバーリストを渡されたとき
                    case "GML":
                        this.createGameMemberListView(strings[3]);//リストに入れてく
                        break;
                    //参加時、参戦可能か
                    case "NOW":
                        if (strings[3].equals("ok")) {
                            enterButton.setDisable(false); //参戦できる
                            timerLabel.setText("開始前");
                        } else { // 退出以外なんもできない
                            enterButton.setDisable(true);
                            messageTextArea.setDisable(true);
                            messageSendButton.setDisable(true);
                            roleLabel.setText("役職：" + "観戦者");

                        } //参戦できない
                        break;
                    //ゲーム開始
                    case "GS":
                        if (isAttendGame) //////////////////ゲーム参加中
                        {
                            //退席不可にする
                            attendLeaveButton.setDisable(true);
                            enterButton.setDisable(true);//キャンセル不可　”参戦”にはゲーム終了時にもどす

                        } else {  ///////////////////////////ゲーム不参加　参戦ボタンoff
                            enterButton.setDisable(true);
                            roleLabel.setText("役職：" + "観戦者");
                        }
                        //開始します
                        messageList.add(strings[3]);
                        //一時的にメッセージ送れません
                        messageTextArea.setDisable(true);
                        messageSendButton.setDisable(true);
                        break;

                    //役職決定
                    case "RD":
                        //役職表示
                        messageList.add("あなたは 【" + strings[3] + "】 です");
                        myRole = strings[3];
                        //役職ラベルに役職表示！！！！/////////////////////////////////////
                        roleLabel.setText("役職：" + strings[3]);
                        break;

                    //占い師の行動要請
                    case "FA":
                        //自分がもし占い師だったら
                        if (myRole.equals("占い師")) {
                            /////////////////自分の名前を消す/////////////////////////////////////////
                            myListIndex = memberList.indexOf(myName);
                            memberList.remove(myName);
                            //////////////////////////////////////////////////////////////////////////////////
                            //選択肢に"その他"を追加
                            memberList.add("その他");
                            /////////////////デフォがその他になるように///////////////////////////////
                            memberListView.getSelectionModel().clearSelection();
                            memberListView.getSelectionModel().select("その他");
                            ////////////////////////////////////////////////////////////////////////////////////
                            memberSendButton.setDisable(false);
                        }
                        messageList.add("占い師 > " + strings[3]);
                        break;

                    //占いの結果提示　占い師にだけ届く
                    case "FR":
                        messageList.add("占い師 > " + strings[3]);
                        //選択肢を削除
                        memberList.remove("その他");
                        /////////////////自分の名前をもどす/////////////////////////////////////////
                        memberList.add(myListIndex, myName);
                        ////////////////////////////////////////////////////////////////////////////////
                        break;

                    //人狼の確認　みんなに届く
                    case "WR":
                        if (myRole.equals("人狼")) {
                            messageList.add("人狼 > " + strings[3]);
                        }
                        break;

                    //怪盗の行動要請　みんなに届く
                    case "PA":
                        //自分がもし怪盗だったら
                        if (myRole.equals("怪盗")) {
                            /////////////////自分の名前を消す/////////////////////////////////////////
                            myListIndex = memberList.indexOf(myName);
                            memberList.remove(myName);
                            //////////////////////////////////////////////////////////////////////////////////
                            //選択肢に"なにもしない"を追加
                            memberList.add("なにもしない");
                            /////////////////デフォがなにもしないになるようにする///////////////////////////////
                            memberListView.getSelectionModel().clearSelection();
                            memberListView.getSelectionModel().select("なにもしない");
                            ////////////////////////////////////////////////////////////////////////////////////
                            memberSendButton.setDisable(false);
                        }
                        messageList.add("怪盗 > " + strings[3]);
                        break;

                    //怪盗の結果　怪盗にだけ届く
                    case "PR":
                        if (strings[3].equals("なにもしませんでした")) {
                            messageList.add("怪盗 > あなたは " + strings[3]);
                        } else {
                            myRole = strings[3];
                            //役職ラベル変更！！！！/////////////////////////////////////
                            roleLabel.setText("役職：" + strings[3]);
                            messageList.add("怪盗 > あなたは 【" + strings[3] + "】 になりました");
                        }
                        //選択肢を削除
                        memberList.remove("なにもしない");
                        /////////////////自分の名前もどす////////////////////////////////////////
                        memberList.add(myListIndex, myName);
                        //////////////////////////////////////////////////////////////////////////////////

                        break;

                    //話し合い開始
                    case "DS":
                        memberSendButton.setDisable(true); //決定ボタン使用不可
                        messageList.add(strings[3]);    //開始してください～
                        if (isAttendGame) {                 /////////////参加者だけ
                            messageTextArea.setDisable(false);
                            messageSendButton.setDisable(false);
                        }
                        break;
                    //投票時刻になりました
                    case "VS":
                        if (isAttendGame) {            /////////////参加者だけ
                            /////////////////自分の名前を消す（使わない）/////////////////////////////////////////
                            //memberListView.getSelectionModel().clearSelection();
                            //myListIndex = memberList.indexOf(myName);
                            //memberList.remove(myName);
                            //memberListView.getSelectionModel().selectFirst(); //デフォで先頭の人選択に
                            //////////////////////////////////////////////////////////////////////////////////
                            messageList.add(strings[3]);
                            command = "VT";
                            memberSendButton.setDisable(false);
                            //一時的にメッセージ送れません
                            messageTextArea.setDisable(true);
                            messageSendButton.setDisable(true);
                        }
                        break;
                    //結果発表
                    case "RS": //参加者だけ
                        /////////////////自分の名前をもどす/////////////////////////////////////////
                        // memberList.add(myListIndex, myName);
                        //////////////////////////////////////////////////////////////////////////////////
                        messageList.add(strings[3]);
                        break;
                    case "ARS": //参加者&参加者以外用結果発表 勝敗と役職
                        messageList.add(strings[3]);
                        break;
                    //再戦のお誘い
                    case "RG": //みんな
                        messageList.add(strings[3]);
                        //再戦ボタンを有効に/////////////////////////////////
                        enterButton.setDisable(false);
                        enterButton.setText("参戦");
                        isAttendGame = false;
                        memberList.clear();
                        //RSからうごかしました↓
                        //メッセージ送れます
                        messageTextArea.setDisable(false);
                        messageSendButton.setDisable(false);
                        //退席できます
                        attendLeaveButton.setDisable(false);
                        break;
                    //タイマー　時間の表示
                    case "TM":
                        timerLabel.setText("あと " + strings[3] + " 秒");
                        break;

                }
            }
        }
    }

    //参加時メンバーリストに名前を追加　その１　チャット
    private void createChatMemberListView(String listStr){
        String[] list = listStr.split(",");
        //memberList.addAll(list);               ///////////リスト別のに
        attendanceList.addAll(list);             ///////////////別のにした
    }
    //参加時メンバーリストに名前を追加　その２ ゲーム
    private void createGameMemberListView(String listStr){
        String[] list = listStr.split(",");
        memberList.addAll(list);
    }

    //参加しているか？
    private boolean isAttend = false;
    //名前
    private String myName = "";
    //役職
    private String myRole = "";
    //コマンド
    private String command = "";
    //自分のリスト上の位置
    private int myListIndex = 0;
    //ゲームに参加しているか？ ////////////////////////////////////////////
    private boolean isAttendGame=false; ///////////////////////////////////

    @FXML
    private Button attendLeaveButton;   //参加or退席ボタン

    //参加or退席ボタンクリック
    public void attendLeaveButtonClick(){
        //参加していた
        if(isAttend){
            //退席処理
            isAttend = false;
            //メンバーリストから名前を消す
            Sender.sendMessage("all::" + myName + "::RCM::" + myName);

            //メッセージ送信できないようになる
            messageSendButton.setDisable(true);
            messageTextArea.setText(myName);
            //退席→参加にする
            attendLeaveButton.setText("参加");

            ///////////////////////////////////////////////
            memberList.clear();    /////メンバーリストを空にする処理をいれる（みんなから、ではなく自分のリストを空っぽに。）
            //////////////////////////////////////////////
            ////removeAll()じゃなかったみたい

            /////////////////////////////////////////////
            ///とりあえずいろんなリスト空にするわ
            attendanceList.clear();
            messageList.clear();


            nameLabel.setText("名前：");
            timerLabel.setText("参加前");

            //いろいろ使えないようになる
            attendanceListView.setDisable(true);
            memberListView.setDisable(true);
            selectMemberLabel.setDisable(true);
            memberSendButton.setDisable(true);
            nameLabel.setDisable(true);
            roleLabel.setDisable(true);
            enterButton.setDisable(true);         ////////////////////////////////////参戦用　
        }else{
            String name = messageTextArea.getText();
            if(!name.equals("")){
                //出席処理
                isAttend = true;
                this.myName = name;
                Sender.sendMessage("all::" + name + "::ACM::" + name);

                //名前ラベルに名前表示！！！！//////////////////////////
                nameLabel.setText("名前：" + myName);

                //コメントが送信できるようになる
                messageSendButton.setDisable(false);
                messageTextArea.setPromptText("メッセージを入力");
                messageTextArea.clear();
                //参加→退席にする
                attendLeaveButton.setText("退席");

                //いろいろ使えるようになる
                attendanceListView.setDisable(false);
                memberListView.setDisable(false);
                selectMemberLabel.setDisable(false);
                nameLabel.setDisable(false);
                roleLabel.setDisable(false);
                enterButton.setDisable(false);

            }
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
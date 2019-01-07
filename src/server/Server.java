package server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.Random;
import static java.lang.Thread.sleep;

import common.*;

public class Server {

    //サーバーエントリーポイント
    public static void main(String args[]) {
        new Server();
    }

    //ゲーム参加者リスト
    private ArrayList<Member> members;

    //ゲーム参加者リスト
    private ArrayList<Member> chatMembers;

    //ゲーム管理用フラグ
    private Boolean gameStart = false;

    //ゲームの必要人数
    private static final int MEMBER_NUM = 4;

    //ゲームへの参加人数
    private int enterNum = 0;

    //役職リスト
    private static final String[] ROLES = {"占い師","怪盗","人狼","人狼","村人","村人"};

    private Sender sender;

    private Server() {

        members = new ArrayList<>();
        chatMembers = new ArrayList<>();

        MulticastSocket socket = null;
        InetAddress mcastAddress;

        try {
            //マルチキャストソケットを作成
            socket = new MulticastSocket(Config.MCAST_PORT);
            mcastAddress = InetAddress.getByName(Config.MCAST_ADDRESS);
            //マルチキャストグループに参加
            socket.joinGroup(mcastAddress);

            //バケット受け取り用
            byte buf[] = new byte[Config.PACKET_SIZE];
            DatagramPacket receivePacket = new DatagramPacket(buf, buf.length);

            //試し打ち…　必要っぽい
            DatagramPacket sendPacket = new DatagramPacket(buf, buf.length, mcastAddress,
                    Config.MCAST_PORT);
            socket.send(sendPacket);

            sender = new Sender();

            System.out.println("サーバー起動！");


            while (true) {


                //パケット受信
                socket.receive(receivePacket);
                String receiveMessage = new String(buf, 0, receivePacket.getLength(), Config.ENCODING);

                //送信元のアドレスを表示。
                //送信元のアドレスは、DatagramPacketクラスのgetSocketAddressメソッドで取得する。
                System.out.println(receivePacket.getSocketAddress() + " 受信: " + receiveMessage);

                //メッセージの形式は
                //宛先[0]::送信者[1]::コマンド[2]::本文[3]

                String[] line = receiveMessage.split("::");

                //System.out.println(line[0] +"/"+ line[1] +"/"+ line[2] +"/"+ line[3] );

                //メッセージの分類
                if (line[0].equals("server") || line[0].equals("all")) { //all or server宛なら
                    //コマンド
                    switch (line[2]) {
                        case "ACM":  //メンバーの参加　all::送信元::ACM::
                            //addMember(line[1]);
                            addChatMember(line[1]);// チャットの方だけ
                            break;
                        //////////ゲーム参加は "EN"
                        case "RGM":  //ゲームメンバーのキャンセル all::送信元::RGM::
                            //リストから削除
                            removeMember(line[1]);
                            break;
                        case "RCM":  //チャットメンバーの退室 all::送信元::RCM::
                            //チャットリストから削除
                            removeChatMember(line[1]);
                            break;
                        case "FM":  //占い師の行動 fortune-teller server::送信元::FM::占い先
                            //本文が占い先
                            checkCard(line[1], line[3]);
                            break;
                        case "PM":  //怪盗の行動 phantom thief server::送信元::PM::交換先
                            //本文が交換先
                            //交換&返却
                            changeCard(line[1], line[3]);
                            break;
                        //case "CM":  //チャット  all::送信元::CM::本文
                        //    break;
                        case "VT":  //投票
                            voting(line[1], line[3]);
                            break;
                        case "EN":  //再戦希望　// 参戦
                            addMember(line[1]); //ゲーム参加
                            //enterNum++;
                            break;
                        default:
                            break;
                    }//switch end
                }//if end

                //ゲーム開始人数に達した
                if(!gameStart) {

                    //参加意思確認
                    if (enterNum == 4) { //四人の開始同意があるかどうかで見るか？

                        System.out.println("ゲーム開始");
                        //開始の合図
                        sender.sendMessage("all::server::GS::ワンナイト人狼　開始します");
                        //配役
                        setting();
                        gameStart = true;
                        //タイマー起動
                        timerStart();
                    }
                }//if end


            }//while end
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }

    //ゲームの参加希望者の追加
    private void addMember(String name) {
        //リストに追加
        Member member = new Member(name);
        members.add(member);

        //参加者増加
        enterNum++;
        System.out.println("現在の参加人数:"+enterNum);
    }
    //チャット参加者　追加         ///////////チャットメンバーリスト.ゲームメンバーリスト.プレイ中かを送る
    private void addChatMember(String name) {
        //リストに追加
        Member member = new Member(name);
        chatMembers.add(member);
        //送信元に参加者リストを送る
        String body = null;
        for (Member cm : chatMembers) {
            if (body == null) { body = cm.getName(); }
            else { body = body.concat("," + cm.getName()); } //members,ほげ,ふが,ぴよ
        }
        //グループに送る　//新しい人にピンポイントで　//コマンドどっちかかえねば//////////////
        sender.sendMessage(name + "::server::CML::" + body);
        System.out.println("チャットメンバーリスト");
        System.out.println("送信 >> "+ name + "::server::CML::" + body);

        //チャット参加時にすでにゲーム参加者がいたら、そのリストも送る/////////////
        if(!members.isEmpty()) {                                          //////////
            String body2 = null;                                            //////
            for (Member m : members) {                                      //////
                if (body2 == null) { body2 = m.getName(); }                 ///////
                else { body2 = body2.concat("," + m.getName()); } //members,ほげ,ふが,ぴよ
            }                                                                               /////
            //グループに送る　//新しい人にピンポイントで　//コマンドどっちかかえねば        /////
            sender.sendMessage(name + "::server::GML::" + body2);                            /////
            System.out.println("ゲームメンバーリスト");                                   ///////
            System.out.println("送信 >> " + name + "::server::GML::" + body2);                /////
            /////////////////////////////////////////////////////////////////////////////////////
        }
        if(gameStart) {sender.sendMessage(name+"::server::NOW::"+"no" );}
        else{sender.sendMessage(name+"::server::NOW::ok" );}

        //チャットは何人でも
        //参加者増加
        //enterNum++;
        //System.out.println("現在の参加人数:"+enterNum);
    }


    //ゲームの参加　辞める
    private void removeMember(String name) {
        for (Member m : members) {
            if (m.getName().equals(name)) {
                members.remove(m);
                break;
            }
        }
        //参加者減少
        enterNum--;
        System.out.println("現在の参加人数:"+enterNum);
    }
    //チャット参加　やめる
    private void removeChatMember(String name) {
        for (Member cm : chatMembers)
            if (cm.getName().equals(name)) {
                chatMembers.remove(cm);
                break;
            }

        //参加者減少
        //enterNum--;
        //System.out.println("現在の参加人数:"+enterNum);
    }

    private void checkCard(String fortuneTeller, String target) {
        //占い結果(役職)を取得し返却する
        String body = "";
        if (target.equals("その他")) {
            String[] roles = ROLES.clone();           /////////////////ここでもclone()使うとは…
            System.out.println(roles[0]+","+roles[1]+","+roles[2]+","+roles[3]+roles[4]+","+roles[5]);
            String message = "";
            String message2 = "";
            for (Member m : members) {
                String s = m.getRole();
                for (int i = 0; i < roles.length; i++) {
                    if (s.equals(roles[i])) {
                        roles[i] = "0";
                        break;
                    }
                }
            }
            for(Member m:members){
                System.out.println(m.getName()+","+m.getRole());
            }
            System.out.println(roles[0]+","+roles[1]+","+roles[2]+","+roles[3]+roles[4]+","+roles[5]);
            for (int i = 0; i < roles.length; i++) {
                if (!roles[i].equals("0")) {
                    if (message.equals("")) {
                        message = roles[i];
                    } else {
                        message2 = " と " + roles[i];
                    }
                }
            }
            body = target + " の占い結果は 【" + message + message2 + "】 でした";
        } else
            for (Member m : members)
                if (m.getName().equals(target)) {
                    body = target + " の占い結果は 【" + m.getRole() + "】 でした";
                    break;
                }
        sender.sendMessage(fortuneTeller + "::server::FR::" + body);
        System.out.println("送信 >> " + fortuneTeller + "::server::FR::" + body);
        ftActEnd = true; //こうどうおわりました～
    }

    private void openWolf() {
        String body = null;
        for (Member m : members) {
            if (m.getRole().equals("人狼")) {
                if (body == null) body = m.getName();
                else body = body.concat(" と " + m.getName());
            }
        }
        if(body == null){ //人狼いないならなにも送らない
            return;
        }else{
            body = body.concat("】 です。");
        }
        sender.sendMessage("all::server::WR::人狼は 【" + body);
        System.out.println("送信 >> " + "all::server::WR::人狼は 【" + body);
    }

    private void changeCard(String phantomThief, String target) {
        String body = null;
        if (target.equals("なにもしない")) {
            body = "なにもしませんでした";
        } else {
            for (Member m : members) {
                if (m.getName().equals(target)) {
                    body = m.getRole();
                    m.setRole("怪盗");
                    break;
                }
            }
            for (Member m : members) {
                if (m.getName().equals(phantomThief)) {
                    m.setRole(body);
                    break;
                }
            }
        }
        sender.sendMessage(phantomThief + "::server::PR::" + body);
        System.out.println("送信 >> " + phantomThief + "::server::PR::" + body);
        ptActEnd = true; //こうどうおわりました～
    }

    private void setting(){
        String[] rolls = ROLES.clone();           /////////////////ここでもclone()使うとは…
        System.out.println(rolls[0]+","+rolls[1]+","+rolls[2]+","+rolls[3]+","+rolls[4]+","+rolls[5]);
        Random r = new Random();
        for(int i = 0; i < MEMBER_NUM + 2 ; i++){
            int u = r.nextInt( MEMBER_NUM + 2);
            String c = rolls[i];
            rolls[i] = rolls[u];
            rolls[u] = c;
        }
        System.out.println(rolls[0]+","+rolls[1]+","+rolls[2]+","+rolls[3]+rolls[4]+","+rolls[5]);
        for(int i = 0; i < MEMBER_NUM; i++){
            System.out.println(i);
            Member m = members.get(i);
            m.setRole(rolls[i]);
            System.out.println(m.getName() + "は" + m.getRole() +"になりました");
            sender.sendMessage(m.getName() + "::server::RD::" + m.getRole());
        }
    }

    //投票と集計
    private void voting(String voter, String destination){
        System.out.println(voter +" は "+destination+" に投票しました");
        for(Member m : members){
            //投票先
            if(m.getName().equals(voter))
                m.setVote(destination);
            //投票されてた
            if(m.getName().equals(destination))
                m.setVoteNum(m.getVoteNum() + 1);
        }
    }

    //処刑者決定
    private ArrayList<String> judgement(){
        System.out.println("名前,票数");            //////////////////////////
        for(Member m : members){                    //////////////////////////
            System.out.println(m.getName()+","+m.getVoteNum()); ////////////////////
        }
        //処刑される人リスト
        ArrayList<String> executed = new ArrayList<>();
        //最大投票数
        int maxVote = 0;
        //同数は全員処刑。全員同数のみ処刑なし
        for(Member m : members){
            if(maxVote < m.getVoteNum()) {  //最大投票数更新
                maxVote = m.getVoteNum();
                executed.clear();
                executed.add(m.getName());
                System.out.println("clear");                                    /////////////////////////
                System.out.println("KIll most:"+m.getName()+","+m.getVoteNum());////////////////////////////
            } else if(maxVote == m.getVoteNum()) {  //最大と同じだったら処刑
                maxVote = m.getVoteNum();
                executed.add(m.getName());
                System.out.println("puls :"+m.getName()+","+m.getVoteNum()); //////////////////////////

            }
        }
        if(executed.size() == MEMBER_NUM){  //全員同数だった場合
            executed.clear();  //誰も処刑されない
            System.out.println("none");                   ////////////////////////
        }
        return executed;
    }


    //開始時間
    private long startTime = 0;


    //タイマー生成&スタート
    private void timerStart(){
        startTime = System.currentTimeMillis();
        myTimer t = new myTimer();
        Thread th = new Thread(t);
        System.out.println("th:start()");
        th.start();
    }


    //占い師行動管理
    private boolean ftActEnd = false;
    //怪盗行動管理
    private boolean ptActEnd = false;


    //タイムキーパー
    private class myTimer implements Runnable{
        long time = 0;
        //お知らせ用
        boolean ftAct = false;
        boolean wwAct = false;
        boolean ptAct = false;
        boolean discussionStart = false;
        boolean votingStart = false;
        boolean resultsAnnounce = false;

        int iniPeriod = 10;                     //役職の確認時間
        int ftWwPeriod = 10 + iniPeriod;    //占い師&人狼の行動時間
        int ptPeriod = 10 + ftWwPeriod;     //怪盗の行動時間
        int disPeriod = 60 + ptPeriod;      //話し合い時間
        int votePeriod = 30 + disPeriod;    //投票時間

        //投票チェック
        boolean votingEnd = false;

        @Override
        public void run() {
            //ゲーム中は繰り返す
            while (gameStart) {
                //一秒まつ
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                //現在時刻
                long now = System.currentTimeMillis();

                //経過時間（秒）
                time = (now - startTime) / 1000;

                System.out.println("経過時間" + time + "秒");

                //役職確認
                if (time <= iniPeriod) {
                    sender.sendMessage("all::server::TM::" + (iniPeriod - time));
                }
                //占い師&人狼の時間が10秒
                else if (time <= ftWwPeriod) {
                    //占い師に行動要請
                    if (!ftAct) {
                        sender.sendMessage("all::server::FA::占う人を選択してください");
                        ftAct = true;
                    }
                    //人狼へ仲間周知
                    if (!wwAct) {
                        openWolf();
                        wwAct = true;
                    }
                    //残り時間を知らせる
                    sender.sendMessage("all::server::TM::" + (ftWwPeriod - time));
                }
                //怪盗の時間が10秒
                else if (time <= ptPeriod) {

                    //最初に占い師がタイムオーバーしてたら（なにもそうしんされてこなかったら）占い師にその他の結果をわたす
                    if(!ftActEnd){
                        for(Member m : members) {
                            if (m.getRole().equals("占い師"))
                                checkCard(m.getName(), "その他");
                        }
                    }
                    //おしらせ
                    if (!ptAct) {
                        sender.sendMessage("all::server::PA::盗む人を選択してください");
                        ptAct = true;
                    }
                    sender.sendMessage("all::server::TM::" + (ptPeriod - time));
                }
                //話し合い時間が180秒
                else if (time <= disPeriod) {

                    //最初に怪盗タイムオーバーしてたら…
                    if(!ptActEnd) {
                        for (Member m : members) {
                            if (m.getRole().equals("怪盗"))
                                changeCard(m.getName(), "なにもしない");
                        }
                    }

                    if (!discussionStart) {
                        sender.sendMessage("all::server::DS::話し合いを開始してください");
                        discussionStart = true;
                    }
                    sender.sendMessage("all::server::TM::" + (disPeriod - time));
                }
                //投票時間が20秒
                else if (time <= votePeriod) {
                    if (!votingStart) {
                        //投票要請
                        sender.sendMessage("all::server::VS::投票時刻になりました。投票する人を選択してください");
                        votingStart = true;
                    }
                    sender.sendMessage("all::server::TM::" + (votePeriod - time));
                }
                else{ //おわり

                    //////////////////////////未投票の人はどうするか？
                    //投票先が空欄の人は、自分に投票したことにする
                    if(!votingEnd) {
                        for (Member m : members) {
                            if (m.getVote() == null) {
                                voting(m.getName(), m.getName());
                            }
                        }
                        votingEnd = true;
                    }
                    if (!resultsAnnounce) {
                        //投票結果発表
                        ArrayList<String> exes = judgement();
                        String result = null;
                        if (exes.isEmpty()){
                            result = "誰もいません】";
                        }else{
                            for(String e : exes){
                                if(result == null){ result = e;}          //////////////////////
                                else { result = result.concat(" と " + e);} //////////////////// if(){}else{} の elseつけたよ
                            }
                            result = result.concat("】です");      //ですつきました
                        }
                        sender.sendMessage("all::server::RS::処刑される人は【" + result);

                        boolean humanIsVictory = false;            //memo 村人側：負け
                        System.out.println("はじめ：村人側　負け"); ///////////////////////

                        for(Member m: members) {
                            //誰も処刑せず人狼がいた場合
                            if(exes.isEmpty()) {
                                humanIsVictory = true;
                                System.out.println("村人側勝ちに決定 ver1"); ///////////////////////////
                                if (m.getRole().equals("人狼")) {                 ///////////// getName()からgetRol()にしたよ
                                    humanIsVictory = false;                        ////memo 村人側：負け
                                    System.out.println("と、思いきや、村人側負けに決定");/////////////////
                                    break;
                                }
                            }
                            else {//人狼を処刑できていた場合                  //memoだれかしら処刑　else {}の{} 好みでいれた
                                for (String s : exes) {
                                    if (m.getName().equals(s) && m.getRole().equals("人狼") ) {
                                        //m.getRoll().equals("人狼");               //memo 処刑された人が人狼の時
                                        humanIsVictory = true;                      ////memo 村人側：勝ち
                                        System.out.println("村人側勝ちに決定　ver2");//////
                                        System.out.println(s + "は処刑");////////////////////
                                        System.out.println(m.getName() + "は人狼");///////////
                                        break;
                                    }
                                }
                            }
                        }

                        System.out.println("終わり：村人側の勝ちは：" + humanIsVictory); ///////////////////////

                        String humanResult = "敗北しました...(村人側)";
                        String wolfResult = "勝利しました！(人狼)";
                        String nonPrayerResult="人狼の勝利です";

                        if(humanIsVictory){
                            humanResult = "勝利しました！(村人側)";
                            wolfResult = "敗北しました...(人狼)";
                            nonPrayerResult="村人側の勝利です";
                        }

                        sender.sendMessage("all::server::ARS::"+nonPrayerResult);///////////全員に勝敗通知
                        for(Member m: members){
                            if (m.getRole().equals("人狼")) {        /////////////// getName() から getRol()　にしたよ
                                result = wolfResult;
                            }else{
                                result = humanResult;
                            }
                            //勝敗を送信
                            sender.sendMessage(m.getName()+"::server::RS::あなたは "+result);

                        }
                        for(Member m: members) {                                             ///////各役職の発表
                            sender.sendMessage("all::server::ARS::" + m.getName() + "の役職は :" + m.getRole()); ////////////////
                        }                                                                   ////////////////////

                        resultsAnnounce = true;

                        //このあと、もう一戦できるようにしたい。
                        //setting() と timerの0を更新 ができればいいと思う
                        //フラグ管理。受信内でmenberが4人になったときと同じようなふうに（受信内にかく）

                        //いろいろリセット////////////////////////////////////////
                        enterNum = 0;
                        members.clear();//////////////////////ゲームメンバー初期化
                        //sender.sendMessage("all::server::RG::もう一度ゲームに参加する場合は【再戦】を押してください。");
                        sender.sendMessage("all::server::RG::ゲームに参加する場合は【参戦】を押してください。");
                        //占い師行動管理
                        ftActEnd = false; ///これぬけるとあかん
                        //怪盗行動管理
                        ptActEnd = false; ///これぬけるとあかん

                        gameStart = false; //終了。run end

                    }//if resultsAnnounce end

                }//try end

            }//while gameStart end
        }
    }

}//class end

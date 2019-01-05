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

    //ゲーム管理用フラグ
    private Boolean gameStart = false;

    //参加人数
    private static final int MEMBER_NUM = 4;
    //役職リスト
    private static final String[] ROLLS = {"占い師","怪盗","人狼","人狼","村人","村人"};

    public Server() {

        members = new ArrayList<Member>();

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
                        case "AM":  //メンバーの参加　all::送信元::AM::
                            addMember(line[1]);
                            break;
                        case "RM":  //メンバーの退室 all::送信元::RM::
                            //リストから削除
                            removeMember(line[1]);
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
                        case "CM":  //チャット  all::送信元::CM::本文
                            break;
                        case "VT":  //投票
                            voting(line[1], line[3]);
                            break;
                        default:
                            break;
                    }//switch end
                }//if end

                //ゲーム開始人数に達した
                if(!gameStart) {
                    if (members.size() == MEMBER_NUM) {
                        System.out.println("ゲーム開始");
                        //開始の合図
                        Sender.sendMessage("all::server::GS::ワンナイト人狼　開始します");
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

    private void addMember(String name) {
        //リストに追加
        Member member = new Member(name);
        members.add(member);
        //送信元に参加者リストを送る
        String body = null;
        for (Member m : members) {
            if (body == null) { body = m.getName(); }
            else { body = body.concat("," + m.getName()); } //members,ほげ,ふが,ぴよ
        }
        //グループに送る
        Sender.sendMessage(name + "::server::ML::" + body);
        System.out.println("送信 >> "+ name + "::server::ML::" + body);
    }

    private void removeMember(String name) {
        for (Member m : members)
            if (m.getName().equals(name)) {
                members.remove(m);
                break;
            }
    }

    private void checkCard(String fortuneTeller, String target) {
        //占い結果(役職)を取得し返却する
        String body = "";
        if (target.equals("その他")) {
            String[] rolls = ROLLS;
            String message = "";
            String message2 = "";
            for (Member m : members) {
                String s = m.getRoll();
                for (int i = 0; i < rolls.length; i++) {
                    if (s.equals(rolls[i])) {
                        rolls[i] = "0";
                        break;
                    }
                }
            }
            for (int i = 0; i < rolls.length; i++) {
                if (rolls[i] != "0") {
                    if (message.equals("")) {
                        message = rolls[i];
                    } else {
                        message2 = "と" + rolls[i];
                    }
                }
            }
            body = target + " の占い結果は " + message2 + " でした";
        } else
            for (Member m : members)
                if (m.getName().equals(target)) {
                    body = target + " の占い結果は " + m.getRoll() + " でした";
                    break;
                }
        Sender.sendMessage(fortuneTeller + "::server::FR::" + body);
    }

    private void openWolf() {
        String body = null;
        for (Member m : members) {
            if (m.getRoll().equals("人狼")) {
                if (body == null) body = m.getName();
                else body = body.concat(" と " + m.getName());
            }
        }
        if(body == null){ //人狼いない
            return;
        }else{
            body = body.concat(" です。");
        }
        Sender.sendMessage("all::server::WR::人狼は " + body);
    }

    private void changeCard(String phantomThief, String target) {
        String body = "あなたは ";
        String roll;
        if (target.equals("なにもしない")) {
            body = body.concat("なにもしませんでした");
        } else
            for (Member m : members) {
                if (m.getName().equals(target)) {
                    roll = m.getRoll();
                    m.setRoll("怪盗");
                    break;
                }
            }
        Sender.sendMessage(phantomThief + "::server::PR::" + body);
    }

    private void setting(){
        String[] rolls = ROLLS;
        Random r = new Random();
        for(int i = 0; i < MEMBER_NUM + 2 ; i++){
            int u = r.nextInt( MEMBER_NUM + 2);
            String c = rolls[i];
            rolls[i] = rolls[u];
            rolls[u] = c;
        }
        for(int i = 0; i < MEMBER_NUM; i++){
            System.out.println(i);
            Member m = members.get(i);
            m.setRoll(rolls[i]);
            System.out.println(m.getName() + "は" + m.getRoll() +"になりました");
            Sender.sendMessage(m.getName() + "::server::RD::" + m.getRoll());
        }
    }

    //投票と集計
    private void voting(String voter, String destination){
        System.out.println(voter +"は"+destination+"に投票しました");
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
        //処刑される人リスト
        ArrayList<String> executed = new ArrayList<String>();
        //最大投票数
        int maxVote = 0;
        //同数は全員処刑。全員同数のみ処刑なし
        for(Member m : members){
            if(maxVote < m.getVoteNum()) {  //最大投票数更新
                maxVote = m.getVoteNum();
                executed.clear();
                executed.add(m.getName());
            } else if(maxVote == m.getVoteNum()) {  //最大と同じだったら処刑
                maxVote = m.getVoteNum();
                executed.add(m.getName());
            }
        }
        if(executed.size() == MEMBER_NUM){  //全員同数だった場合
            executed.clear();  //誰も処刑されない
        }
        return executed;
    }

    //開始時間
    private long startTime = 0;

    private void timerStart(){
        startTime = System.currentTimeMillis();
        myTimer t = new myTimer();
        Thread th = new Thread(t);
        System.out.println("th:start()");
        th.start();
    }

    //タイムキーパー
    private class myTimer implements Runnable{
        long time = 0;
        //お知らせ用
        Boolean ftAct = false;
        Boolean wwAct = false;
        Boolean ptAct = false;
        Boolean discussionStart = false;
        Boolean votingStart = false;
        Boolean resultsAnnounce = false;

        int iniPeriod = 10;
        int ftWwPeriod = 20 + iniPeriod;
        int ptPeriod = 20 + ftWwPeriod;
        int disPeriod = 20 + ptPeriod;
        int votePeriod = 30 + disPeriod;

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
                    Sender.sendMessage("all::server::TM::" + (iniPeriod - time));
                }
                //占い師&人狼の時間が10秒
                else if (time <= ftWwPeriod) {
                    //占い師に行動要請
                    if (!ftAct) {
                        Sender.sendMessage("all::server::FA::占う人を選択してください");
                        ftAct = true;
                    }
                    //人狼へ仲間周知
                    if (!wwAct) {
                        openWolf();
                        wwAct = true;
                    }
                    //残り時間を知らせる
                    Sender.sendMessage("all::server::TM::" + (ftWwPeriod - time));
                }
                //怪盗の時間が10秒
                else if (time <= ptPeriod) {
                    //怪盗へ行動要請
                    if (!ptAct) {
                        Sender.sendMessage("all::server::PA::盗む人を選択してください");
                        ptAct = true;
                    }
                    Sender.sendMessage("all::server::TM::" + (ptPeriod - time));
                }
                //話し合い時間が180秒
                else if (time <= disPeriod) {
                    if (!discussionStart) {
                        Sender.sendMessage("all::server::DS::話し合いを開始してください");
                        discussionStart = true;
                    }
                    Sender.sendMessage("all::server::TM::" + (disPeriod - time));
                }
                //投票時間が20秒
                else if (time <= votePeriod) {
                    if (!votingStart) {
                        //投票要請
                        Sender.sendMessage("all::server::VS::投票時刻になりました。投票する人を選択してください");
                        votingStart = true;
                    }
                    Sender.sendMessage("all::server::TM::" + (votePeriod - time));
                }
                else{ //おわり
                    if (!resultsAnnounce) {
                        //投票結果発表
                         ArrayList<String> exes = judgement();
                         String result = null;
                        if (exes.isEmpty()){
                            result = "誰もいません";
                         }else{
                            for(String e : exes){
                                if(result == null) result = e;
                                result = result.concat(" と " + e);
                            }
                             result.concat(" です");
                         }
                        Sender.sendMessage("all::server::RS::処刑される人は " + result);

                        boolean humanIsVictory = false;

                        outside:for(Member m: members) {
                            //誰も処刑せず人狼がいた場合
                            if(exes.isEmpty()) {
                                humanIsVictory = true;
                                if (m.getName().equals("人狼")) {
                                    humanIsVictory = false;
                                    break outside;
                                }
                            }
                            else //人狼を処刑できていた場合
                            for (String s : exes) {
                                if (m.getName().equals(s)) {
                                    m.getRoll().equals("人狼");
                                    humanIsVictory = true;
                                    break outside;
                                }
                            }
                        }

                        String humanResult = "敗北しました...";
                        String wolfResult = "勝利しました！";

                        if(humanIsVictory){
                            humanResult = "勝利しました！";
                            wolfResult = "敗北しました...";
                        }
                        for(Member m: members){
                            if (m.getName().equals("人狼")) {
                                result = wolfResult;
                            }else{
                                result = humanResult;
                            }
                            //勝敗を送信
                            Sender.sendMessage(m.getName()+"::server::RS::あなたは "+result);
                        }

                        resultsAnnounce = true;
                    }

                }
            }
        }
    }

}//class end

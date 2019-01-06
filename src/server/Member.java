package server;

public class Member {
    private String name = null;     //名前
    private String role = null;     //役職
    private String vote = null;     //投票先
    private int voteNum = 0;        //被投票数

    int getVoteNum() {
        return voteNum;
    }

    void setVoteNum(int voteNum) {
        this.voteNum = voteNum;
    }

    Member(String name){
        this.setName(name);
    }
    String getName() {
        return name;
    }
    private void setName(String name) {
        this.name = name;
    }
    String getRole() {
        return role;
    }
    void setRole(String role) {
        this.role = role;
    }
    public String getVote() {
        return vote;
    }
    void setVote(String vote) {
        this.vote = vote;
    }
}

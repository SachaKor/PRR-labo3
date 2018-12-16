public class Message {
    private int id;
    private byte nodeId;
    private int aptitude;

    public Message(int id, byte nodeId, int aptitude) {
        this.id = id;
        this.nodeId = nodeId;
        this.aptitude = aptitude;
    }

    public int getId() {
        return id;
    }

    public byte getNodeId() {
        return nodeId;
    }

    public int getAptitude() {
        return aptitude;
    }

    public String toString() {
        return "[" + id + "] " + "(" + nodeId + "," + aptitude + ")";
    }
}

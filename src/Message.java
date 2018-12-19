public class Message {
    private byte nodeId;
    private int aptitude;

    public Message(byte nodeId, int aptitude) {
        this.nodeId = nodeId;
        this.aptitude = aptitude;
    }

    public byte getNodeId() {
        return nodeId;
    }

    public int getAptitude() {
        return aptitude;
    }

    public String toString() {
        return "(" + nodeId + "," + aptitude + ")";
    }
}

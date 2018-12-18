public class Message {
    private int id;
    private byte nodeId;
    private int aptitude;
    private MessageType messageType;

    public Message(MessageType messageType, int id, byte nodeId, int aptitude) {
        this.messageType = messageType;
        this.id = id;
        this.nodeId = nodeId;
        this.aptitude = aptitude;
    }

    public MessageType getMessageType() {
        return messageType;
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
        return "[" + id + "] " + messageType.name() + ": (" + nodeId + "," + aptitude + ")";
    }
}

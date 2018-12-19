import java.util.List;

public class MessageList {
    private MessageType messageType;
    private List<Message> messages;
    private int id;

    public MessageList(int id, MessageType messageType, List<Message> messages) {
        this.id = id;
        this.messageType = messageType;
        this.messages = messages;
    }

    public int getId() {
        return id;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public int size() {
        return messages.size();
    }

    public void setId(int id) {
        this.id = id;
    }

    public String toString() {
        String str = "[" + id + "] " + messageType.name() + " ";
        str += "[";
        for (int i = 0; i < messages.size(); i++) {
            str += messages.get(i);
            str += (i == messages.size()-1) ? "" : ",";
        }
        str += "]";
        return str;
    }

    public void add(Message m) {
        messages.add(m);
    }

    /**
     * Returns the id of the sender node
     * The id of the sender node is the one of the last entry of the list
     * @return the id of the sender node
     */
    public int getSenderId() {
        return messages.get(messages.size()-1).getNodeId();
    }
}

import sun.plugin2.message.Message;

public enum MessageType {
    ANNOUNCEMENT((byte) 0),
    RESULT((byte) 1),
    ACKNOWLEDGEMENT((byte) 2),
    HELLO((byte) 3),
    OLLEH((byte) 4);

    private final byte id;

    MessageType(byte id) {
        this.id = id;
    }

    public byte getId() {
        return id;
    }

    public static MessageType getById(byte id) {
        for (MessageType v : values()) {
            if (v.id == id) {
                return v;
            }
        }
        return null;
    }
}

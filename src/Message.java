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

    /**
     * Determines if the {@link Message} equals to another.
     * Note: this method is used in the contains() method
     * @param obj the {@link Message} we compare the current one to
     * @return true if the two {@link Message}s are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (!Message.class.isAssignableFrom(obj.getClass())) {
            return false;
        }

        final Message other = (Message) obj;
        if (this.nodeId != other.nodeId || this.aptitude != other.aptitude) {
            return false;
        }
        return true;
    }
}

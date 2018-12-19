import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Node {
    private static final Logger LOG  = Logger.getLogger(Node.class.getName());

    private Map<Byte, InetSocketAddress> nodes;
    private int aptitude = -1;
    private InetSocketAddress localAddress;
    private byte id;
    private int nbNodes;

    public Node(List<InetSocketAddress> addresses, int index) {
        this.localAddress = addresses.get(index);
        nbNodes = addresses.size();
        nodes = new HashMap<>();
        aptitude = calculateAptitude(localAddress);
        evaluateIds(addresses);
    }

    private InetSocketAddress getNextNodeAddress(byte id) {
        int nextId = (id+1)%nbNodes;
        return nodes.get((byte) nextId);
    }

    private InetSocketAddress getPreviousNodeAddress(byte id) {
        byte prevIndex = (byte) (id == 0 ? nodes.size()-1 : id-1);
        return nodes.get(prevIndex);
    }

    public void connectToNextNode() {
        InetSocketAddress nextNode = getNextNodeAddress(id);
        DatagramSocket socket = null;
        int helloId = 0;
        byte[] buffer;
        DatagramPacket packet = null;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        ByteArrayInputStream byteArrayInputStream;
        DataInputStream dataInputStream;
        try {
            while (true) {
                // send HELLO message to the next node
                socket = new DatagramSocket();
                dataOutputStream.writeByte((byte) MessageType.HELLO.ordinal());
                dataOutputStream.writeInt(helloId++);
                buffer = byteArrayOutputStream.toByteArray();
                packet = new DatagramPacket(buffer, buffer.length, nextNode.getAddress(), nextNode.getPort());
                socket.send(packet);
                LOG.log(Level.INFO, () -> MessageType.HELLO + " message sent to " + nextNode);

                socket.setSoTimeout(Constants.HELLO_TIMEOUT);

                // receive the HELLO or ELLOH
                buffer = new byte[Constants.BUFFER_SIZE];
                packet = new DatagramPacket(buffer, buffer.length);
                buffer = packet.getData();
                byteArrayInputStream = new ByteArrayInputStream(buffer);
                dataInputStream = new DataInputStream(byteArrayInputStream);
                try {
                    socket.receive(packet);
                    int msgType = (int)dataInputStream.readByte();
                    if (msgType == MessageType.OLLEH.ordinal()) {
                        int msgId = dataInputStream.readInt();
                        if (msgId == helloId) {
                            LOG.log(Level.INFO, () -> MessageType.OLLEH.name() + " received, id: " + msgId);
                        }
                    } else if (msgType == MessageType.HELLO.ordinal()) {

                    }
                    break;
                } catch (SocketTimeoutException e) {
                    LOG.log(Level.WARNING, () -> "Failed to connect to the neighbour node: " + nextNode + "; " +
                            "retry in " + Constants.HELLO_TIMEOUT + " milliseconds");
                }
            }
        } catch (IOException e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
        } finally {
            if(socket != null) {
                socket.close();
            }
        }
    }

    public void start(boolean launchElection) {
        new Thread(new ElectionManager(launchElection)).start();
    }

    /**
     * Attributes an id to every {@link Node} of the system.
     * Node ids are of the Byte type (since there can be maximum 4 nodes) and set according to the increasing
     * order of their aptitudes, starting with 0.
     * @param addresses
     */
    private void evaluateIds(List<InetSocketAddress> addresses) {
        LOG.log(Level.INFO, "Evaluating node IDs");
        Collections.sort(addresses, (o1, o2) -> calculateAptitude(o1) - calculateAptitude(o2));
        for (int i = 0; i < addresses.size(); ++i) {
            nodes.put((byte) i , addresses.get(i));
            if (addresses.get(i).equals(localAddress)) {
                id = (byte) i;
            }
        }
        LOG.log(Level.INFO, () -> "Resulting IDs: " + nodes);
    }

    private int calculateAptitude(InetSocketAddress address) {
        byte[] ipAdr = address.getAddress().getAddress();
        int aptitude =  ipAdr[3]*10000 + address.getPort();
        return aptitude;
    }

    private class ElectionManager implements Runnable {

        private DatagramSocket socket;
        private boolean shouldRun;
        private boolean launchElection;

        // streams and a buffer and a datagram packet needed to communicate to other nodes
        byte[] buffer;
        DatagramPacket packet;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        ByteArrayInputStream byteArrayInputStream;
        DataInputStream dataInputStream;

        ElectionManager(boolean launchElection) {
            try {
                socket = new DatagramSocket(localAddress.getPort(), localAddress.getAddress());
                shouldRun = true;
                this.launchElection = launchElection;
            } catch (SocketException e) {
                LOG.log(Level.SEVERE, e.getMessage(), e);
            }
        }

        /**
         * Sends a {@link MessageList} to the node
         * First sends the id and the {@link MessageType} id of the list, then the size of the list,
         * followed by the {@link Message}s, represented by the (senderId, aptitude) pair
         * @param messageList a {@link MessageList} to send
         * @param receiver the receiver node
         * @throws IOException
         */
        private void sendMessageList(MessageList messageList, InetSocketAddress receiver) throws IOException {
            dataOutputStream.writeInt(messageList.getId());
            dataOutputStream.writeByte(messageList.getMessageType().getId());
            dataOutputStream.writeInt(messageList.size());
            for (Message m : messageList.getMessages()) {
                dataOutputStream.writeByte(m.getNodeId());
                dataOutputStream.writeInt(m.getAptitude());
            }
            buffer = byteArrayOutputStream.toByteArray();
            packet = new DatagramPacket(buffer, buffer.length, receiver.getAddress(), receiver.getPort());
            socket.send(packet);
            LOG.log(Level.INFO, () -> messageList + " sent to " + receiver);
            byteArrayOutputStream.reset();
        }

        /**
         * Receives the ACKNOWLEDGEMENT from another {@link Node}
         * @param ackId id of the {@link MessageList} to be acknowledged
         * @return true if the id of the acknowledgement received corresponds to the one passed as an argument,
         * false otherwise
         * @throws IOException
         */
        private boolean receiveAcknowledgement(int ackId) throws IOException {
            // wait for acknowledgement
            buffer = new byte[Constants.BUFFER_SIZE];
            packet = new DatagramPacket(buffer, buffer.length);
            buffer = packet.getData();
            byteArrayInputStream = new ByteArrayInputStream(buffer);
            dataInputStream = new DataInputStream(byteArrayInputStream);
            socket.receive(packet);
            byte msgType = dataInputStream.readByte();
            if (msgType == MessageType.ACKNOWLEDGEMENT.getId()) {
                final int msgId = dataInputStream.readInt();
                if (msgId == ackId) {
                    LOG.log(Level.INFO, () -> "[" + msgId + "] " + MessageType.ACKNOWLEDGEMENT.name() + " received from "
                            + packet.getAddress() + ":" + packet.getPort());
                    return true;
                }
            }
            return false;
        }

        /**
         * Receives the {@link MessageList} from another node
         * First reads the id , then the {@link MessageType} of the list, represented by its id,
         * then the size of the list, followed by the {@link Message}s contained in the list,
         * represented by (senderId, senderAptitude) tuples
         * @return {@link MessageList} received
         * @throws IOException
         */
        private MessageList receiveMessageList() throws IOException {
            buffer = new byte[Constants.BUFFER_SIZE];
            packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            buffer = packet.getData();
            byteArrayInputStream = new ByteArrayInputStream(buffer);
            dataInputStream = new DataInputStream(byteArrayInputStream);
            int listId = dataInputStream.readInt();
            byte messageType = dataInputStream.readByte();
            int size = dataInputStream.readInt();
            List<Message> messages = new ArrayList<>(size);
            for (int i = 0; i < size; ++i) {
                byte senderId = dataInputStream.readByte();
                int senderAptitude = dataInputStream.readInt();
                messages.add(new Message(senderId, senderAptitude));
            }
            MessageList messageList = new MessageList(listId, MessageType.getById(messageType), messages);
            LOG.log(Level.INFO, () -> messageList + " received from " + packet.getAddress() + ":" + packet.getPort());
            return messageList;
        }

        /**
         * Sends an ACKNOWLEDGEMENT to the last {@link Node} which sent the message to the current {@link Node}
         * @param ackId the id of the ACKNOWLEDGEMENT (id of the {@link MessageList} received)
         * @throws IOException
         */
        private void sendAcknowledgement(int ackId) throws IOException {
            InetAddress senderAddress = packet.getAddress();
            int senderPort = packet.getPort();
            dataOutputStream.writeByte(MessageType.ACKNOWLEDGEMENT.getId());
            dataOutputStream.writeInt(ackId);
            byte[] response = byteArrayOutputStream.toByteArray();
            packet = new DatagramPacket(response, response.length, senderAddress, senderPort);
            socket.send(packet);
            byteArrayOutputStream.reset();
            LOG.log(Level.INFO, () -> "[" + ackId + "] " + MessageType.ACKNOWLEDGEMENT.name() + " sent to "
                    + packet.getAddress() + ":" + packet.getPort());
        }

        private boolean sendListWithAckToNextActiveNode(MessageList messageList) {
            boolean activeNodeFound = false;
            int nextNodeId = (id+1)%nbNodes;
            InetSocketAddress nextNodeAddress = nodes.get((byte)nextNodeId);
            while (!activeNodeFound) {
                try {
                    sendMessageList(messageList, nextNodeAddress);
                    try {
                        socket.setSoTimeout(Constants.ACK_TIMEOUT);
                        receiveAcknowledgement(messageList.getId());
                        activeNodeFound = true;
                        socket.setSoTimeout(0);
                    } catch (SocketTimeoutException e) {
                        InetSocketAddress nextAdr = nextNodeAddress;
                        LOG.log(Level.WARNING, () -> "[" + messageList.getId() + "] "
                                + MessageType.ACKNOWLEDGEMENT.name() + " from " + nextAdr + " timed out");
                        nextNodeId = (nextNodeId+1)%nbNodes;
                        if (nextNodeId == id) {
                            return false;
                        }
                        nextNodeAddress = nodes.get((byte)nextNodeId);
                    }
                } catch (IOException e) {
                    LOG.log(Level.SEVERE, e.getMessage(), e);
                }
            }
            return true;
        }

        @Override
        public void run() {
            try {
                int messageId = 1;
                if (launchElection) {
                    List<Message> messages = new ArrayList<>(1);
                    messages.add(new Message(id, aptitude));
                    MessageList messageList = new MessageList(messageId, MessageType.ANNOUNCEMENT, messages);
                    // TODO: elect the current node if it is the only active one
                    sendListWithAckToNextActiveNode(messageList);
                }
                LOG.log(Level.INFO, () -> "Listening on " + localAddress);
                while (shouldRun) {
                    // receive a message
                    MessageList messageList = receiveMessageList();
                    // send an ACKNOWLEDGEMENT
                    sendAcknowledgement(messageList.getId());
                    messageId = messageList.getId() + 1;
                    messageList.add(new Message(id, aptitude));
                    messageList.setId(messageId);
                    sendListWithAckToNextActiveNode(messageList);
                }
            } catch (IOException e) {
                LOG.log(Level.SEVERE, e.getMessage(), e);
            }
        }
    }

    /**
     * Main program
     * @param args
     * If N is the number of arguments
     *  - args[0]..args[N-2] are the addresses of the Nodes of the system.
     *    If all the nodes are launched from the localhost, the addresses only contain the ports
     *    Otherwise, every address is represented like this: ip:port
     *  - args[N-1] is the index of the current node
     */
    public static void main(String...args) {
        // parse arguments
        // addresses list
        List<InetSocketAddress> addresses = new ArrayList<>(args.length-1);
        for(int i = 0; i < args.length-1; i++) {
            String[] splitAdr = args[i].split(":");
            if (splitAdr.length == 2) {
                addresses.add(new InetSocketAddress(splitAdr[0].trim(), Integer.parseInt(splitAdr[1].trim())));
            } else if (splitAdr.length == 1) {
                addresses.add(new InetSocketAddress("localhost", Integer.parseInt(splitAdr[0].trim())));
            } else {
                LOG.log(Level.SEVERE, "Bad input format");
                return;
            }
        }

        Scanner scanner = new Scanner(System.in);
        boolean launchOption = false;
        boolean launch = false;
        while (!launchOption) {
            System.out.println("Launch the election? [y/n]");
            String input = scanner.next();
            switch (Character.toUpperCase(input.charAt(0))) {
                case Constants.YES: {
                    launch = true;
                    launchOption = true;
                    break;
                }
                case Constants.NO: {
                    launchOption = true;
                    break;
                }
                default:
                    System.out.println("Unknown option");
            }
        }
        // current node index
        int currentNodeAdrIndex = Integer.parseInt(args[args.length-1]);

        // create a new node
        Node node = new Node(addresses, currentNodeAdrIndex);
        node.start(launch);
    }
}

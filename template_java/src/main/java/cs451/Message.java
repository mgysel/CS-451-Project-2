package cs451;

import java.util.ArrayList;

enum MessageType {
    BROADCAST,
    ACK,
    FORWARD
}

public class Message implements Comparable<Message> {
    private MessageType type;
    private int sequenceNumber;
    private Host from;
    private String content;
    private ArrayList<Integer> VC;

    public Message(MessageType type, int sequenceNumber, Host from, String content, ArrayList<Integer> VC) {
        this.type = type;
        this.sequenceNumber = sequenceNumber;
        this.from = from;
        this.content = content;
        this.VC = VC;
    }

    // public Message(MessageType type, int sequenceNumber, Host from, String content, boolean receivedAck) {
    //     this.sequenceNumber = sequenceNumber;
    //     this.type = type;
    //     this.from = from;
    //     this.content = content;
    //     this.receivedAck = receivedAck;
    // }

    public Message(String message, Hosts hosts, Host me) {
        String[] messageComponents = message.split("/");
        if (messageComponents.length == 4 || messageComponents.length == 5) {
            // Type
            if (messageComponents[0].equals("A")) {
                this.type = MessageType.ACK;
            } else if (messageComponents[0].equals("B")) {
                this.type = MessageType.BROADCAST;
            } else if (messageComponents[0].equals("F")) {
                this.type = MessageType.FORWARD;
            }
            // Sequence Number
            try {
                int sequenceNumber = Integer.parseInt(messageComponents[1]);
                this.sequenceNumber = sequenceNumber;
            } catch (NumberFormatException e) {
                System.out.printf("Cannot convert message because ID is not an integer: ", e);
            } catch (NullPointerException e) {
                System.out.printf("Cannot convert message because ID is a null pointer: ", e);
            }
            // From
            try {
                Integer id = Integer.parseInt(messageComponents[2]);
                this.from = hosts.getHostById(id);
            } catch (NumberFormatException e) {
                System.out.printf("Cannot convert message because ID is not an integer: ", e);
            } catch (NullPointerException e) {
                System.out.printf("Cannot convert message because ID is a null pointer: ", e);
            }
            // Content
            this.content = messageComponents[3];
            // VectorClock
            ArrayList<Integer> vcComponents = new ArrayList<Integer>();
            if (messageComponents.length == 5) {
                String vcString = messageComponents[4];
                String[] vcSplit = vcString.split(",");
                
                for (String vcComponentString: vcSplit) {
                    try {
                        Integer vcComponentInt = Integer.parseInt(vcComponentString);
                        vcComponents.add(vcComponentInt);
                    } catch (NumberFormatException e) {
                        System.out.printf("Cannot convert message because ID is not an integer: ", e);
                    } catch (NullPointerException e) {
                        System.out.printf("Cannot convert message because ID is a null pointer: ", e);
                    }
                }
            }
            this.VC = vcComponents;
        }
    }

    public static boolean isValidMessage(String message) {
        String[] messageComponents = message.split("/");
        if (messageComponents.length == 5) {
            return true;
        }
        return false;
    }

    // Compare Message objects
    @Override
    public boolean equals(Object o) {
        // If the object is compared with itself then return true 
        if (o == this) {
            return true;
        }

        // Check if instance of Message
        if (!(o instanceof Message)) {
            return false;
        }
        
        // typecast o to Message so that we can compare data members
        Message m = (Message) o;
        
        // Compare the data members and return accordingly
        if (m.getType() == this.getType() && m.getSequenceNumber() == this.getSequenceNumber() && m.getFrom().equals(this.getFrom()) && m.getContent().equals(this.getContent())) {
            return true;
        }

        return false;
    }

    @Override
    public String toString() {
        String output = "";
        if (this != null) {
            if (this.type == MessageType.BROADCAST) {
                output += "B";
            } else if (this.type == MessageType.ACK) {
                output += "A";
            } else if (this.type == MessageType.FORWARD) {
                output += "F";
            }
            String vcString = "";
            for (int i=0; i < this.VC.size(); i++) {
                if (i != 0) {
                    vcString += ",";
                }
                vcString += String.format("%s", Integer.toString(this.VC.get(i)));
            }
            output = String.format("%s/%d/%d/%s/%s", output, this.getSequenceNumber(), this.from.getId(), this.content, vcString);
        }

        return output;
    }

    @Override
    public int compareTo(Message m) {
        Integer thisInt = this.getSequenceNumber();
        Integer mInt = m.getSequenceNumber();
        return thisInt.compareTo(mInt);
    }

    public Message getClone() {
        int sequenceNumber = this.getSequenceNumber();
        MessageType type = this.getType();
        Host from = this.getFrom();
        String content = new String(this.getContent());
        ArrayList<Integer> vcClone = new ArrayList<Integer>();
        for (Integer i: this.getVC()) {
            vcClone.add(i);
        }

        Message clone = new Message(type, sequenceNumber, from, content, vcClone);
        return clone;
    }

    public int getSequenceNumber() {
        return this.sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public MessageType getType() {
        return this.type;
    }

    public Host getFrom() {
        return this.from;
    }

    public String getContent() {
        return this.content;
    }

    public ArrayList<Integer> getVC() {
        return this.VC;
    }
}

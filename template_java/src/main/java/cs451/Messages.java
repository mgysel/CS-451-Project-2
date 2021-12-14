package cs451;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Messages {
    private static final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private static final Lock readLock = rwLock.readLock();
    private static final Lock writeLock = rwLock.writeLock();
    
    /**
     * Adds message to host's messageList
     * @param h
     * @param m
     * @return
    */
    public static boolean addMessageToMap(Host h, Message m, ConcurrentHashMap<Host, ArrayList<Message>> map) {
        readLock.lock();
        ArrayList<Message> msgList = map.get(h);

        // If h does not have any messages, create new message list
        if (msgList == null) {
            ArrayList<Message> newMsgList = new ArrayList<Message>();
            newMsgList.add(m);
            
            readLock.unlock();
            writeLock.lock();
            map.put(h, newMsgList);
            writeLock.unlock();
            return true;
        }
        
        // If h has msgList, add m
        int index = msgList.indexOf(m);
        if(index == -1) {
            readLock.unlock();
            writeLock.lock();
            msgList.add(m);
            writeLock.unlock();
            return true;
        }

        // If m already in h's msgList, return false
        readLock.unlock();
        return false;
    }

    /**
     * Adds host to message's host map
     * @param h
     * @param m
     * @param map
     * @return
     */
    public static boolean addHostToMap(Host h, Message m, ConcurrentHashMap<Message, ArrayList<Host>> map) {
        readLock.lock();
        for (ConcurrentHashMap.Entry<Message, ArrayList<Host>> entry : map.entrySet()) {
            Message message = entry.getKey();
            if (message.equals(m)) {
                ArrayList<Host> hostList = entry.getValue();
                // If m does not have any hosts, create new host list
                if (hostList == null) {
                    System.out.println("hostList is null");
                    ArrayList<Host> newHostList = new ArrayList<Host>();
                    newHostList.add(h);
                    
                    readLock.unlock();
                    writeLock.lock();
                    map.put(message, newHostList);
                    writeLock.unlock();
                    return true;
                }
                
                int index = hostList.indexOf(h);
                if(index == -1) {
                    // If h has msgList, add m
                    readLock.unlock();
                    writeLock.lock();
                    hostList.add(h);
                    writeLock.unlock();
                    return true;
                } else {
                    // If msgList already contains m, return false
                    readLock.unlock();
                    return false;
                }
            }
        }

        // If m not in map, add
        ArrayList<Host> newHostList = new ArrayList<Host>();
        newHostList.add(h);
        
        readLock.unlock();
        writeLock.lock();
        map.put(m, newHostList);
        writeLock.unlock();
        return true;
    }

    public static Message isMessageInMap(Host h, Message m, ConcurrentHashMap<Host, ArrayList<Message>> map) {
        Message msg = null;

        readLock.lock();
        ArrayList<Message> msgList = map.get(h);
        if (msgList != null) {
            int index = msgList.indexOf(m);
            if (index != -1) {
                msg = msgList.get(index);
                readLock.unlock();
                return msg;
            }
        }

        readLock.unlock();
        return msg;
    }

    public static boolean isMessageInList(Message m, ArrayList<Message> list) {
        readLock.lock();
        for (Message message: list) {
            if (message.equals(m)) {
                readLock.unlock();
                return true;
            }
        }
        readLock.unlock();
        return false;
    }

    public static boolean removeMessageFromMap(Host h, Message m, ConcurrentHashMap<Host, ArrayList<Message>> map) {
        readLock.lock();
        ArrayList<Message> msgList = map.get(h);
        if (msgList != null) {
            int index = msgList.indexOf(m);
            if (index != -1) {
                readLock.unlock();
                writeLock.lock();
                msgList.remove(index);
                writeLock.unlock();
                return true;
            }
        }

        readLock.unlock();
        return false;
    }

    /**
     * Gets Clone of a Messages Map
     * @param map
     * @return
     */
    public static ConcurrentHashMap<Host, ArrayList<Message>> getMapClone(ConcurrentHashMap<Host, ArrayList<Message>> map) {
        ConcurrentHashMap<Host, ArrayList<Message>> mapClone = new ConcurrentHashMap<Host, ArrayList<Message>>();

        readLock.lock();
        for (ConcurrentHashMap.Entry<Host, ArrayList<Message>> entry : map.entrySet()) {
            Host key = entry.getKey();

            ArrayList<Message> newValue = new ArrayList<Message>();
            for (Message msg: entry.getValue()) {
                Message clone = msg.getClone();
                newValue.add(clone);
            }
            mapClone.put(key, newValue);
        }
        readLock.unlock();

        return mapClone;
    }

    public static ArrayList<Message> getListClone(ArrayList<Message> list) {
        ArrayList<Message> listClone = new ArrayList<Message>();
        readLock.lock();
        for (Message m: list) {
            Message mClone = m.getClone();
            listClone.add(mClone);
        }
        readLock.unlock();

        return listClone;
    }

    public static boolean addMessageToList(Message m, ArrayList<Message> list) {
        readLock.lock();
        int index = list.indexOf(m);
        if (index == -1) {
            readLock.unlock();
            writeLock.lock();
            list.add(m);
            writeLock.unlock();
            return true;
        } 

        readLock.unlock();
        return false;
    }

    /**
     * Removes message from list
     * @param m
     * @param list
     * @return true if removed, false if not in list
     */
    public static boolean removeMessageFromList(Message m, ArrayList<Message> list) {
        readLock.lock();
        int index = list.indexOf(m);
        readLock.unlock();

        if (index != -1) {
            writeLock.lock();
            list.remove(m);
            writeLock.unlock();
            return true;
        }

        return false;
    }

    public static boolean isMajorityInMap(int numHosts, Message m, ConcurrentHashMap<Message, ArrayList<Host>> map) {
        readLock.lock();
        for (ConcurrentHashMap.Entry<Message, ArrayList<Host>> entry : map.entrySet()) {
            Message message = entry.getKey();
            if (message.equals(m)) {
                ArrayList<Host> hostList = entry.getValue();
                if (hostList != null) {
                    double numAcks = (double) hostList.size();
                    double total = (double) numHosts;
                    double perAcks = numAcks / total;

                    if (perAcks > 0.5) {
                        readLock.unlock();
                        return true;
                    } 
                }

                readLock.unlock();
                return false;
            }
        }

        readLock.unlock();
        return false;
    }

    /**
     * Prints a map - useful for debugging
     * @param map
     */
    public static void printHostMessageMap(ConcurrentHashMap<Host, ArrayList<Message>> map) {
        readLock.lock();
        System.out.println("***** Print Map");
        for (Map.Entry<Host, ArrayList<Message>> entry : map.entrySet()) {
            Host host = entry.getKey();
            ArrayList<Message> hostDelivered = entry.getValue();

            System.out.printf("* Host: %d\n", host.getId());
            for (Message m: hostDelivered) {
                System.out.printf("* Message: %s\n", m.toString());
            }
        }
        readLock.unlock();
    }

    /**
     * Prints a map - useful for debugging
     * @param map
     */
    public static void printMessageHostMap(ConcurrentHashMap<Message, ArrayList<Host>> map) {
        readLock.lock();
        System.out.println("***** Print Map");
        for (Map.Entry<Message, ArrayList<Host>> entry : map.entrySet()) {
            Message m = entry.getKey();
            ArrayList<Host> hostDelivered = entry.getValue();

            System.out.printf("* Message: %s\n", m.toString());
            for (Host h: hostDelivered) {
                System.out.printf("* Host: %d\n", h.getId());
            }
        }
        readLock.unlock();
    }

        /**
     * Prints a map - useful for debugging
     * @param map
     */
    public static void printMessageList(ArrayList<Message> list) {
        readLock.lock();
        System.out.println("\n***** Print Message list");
        int i = 1;
        for (Message m: list) {
            System.out.printf("%d: %s\n", i, m.toString());
            i++;
        }
        readLock.unlock();
    }
}

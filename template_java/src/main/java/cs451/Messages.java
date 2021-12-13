package cs451;

import java.util.ArrayList;
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
        ArrayList<Host> hostList = map.get(m);

        // If h does not have any messages, create new message list
        if (hostList == null) {
            ArrayList<Host> newHostList = new ArrayList<Host>();
            newHostList.add(h);
            
            readLock.unlock();
            writeLock.lock();
            map.put(m, newHostList);
            writeLock.unlock();
            return true;
        }
        
        // If h has msgList, add m
        int index = hostList.indexOf(h);
        if(index == -1) {
            readLock.unlock();
            writeLock.lock();
            hostList.add(h);
            writeLock.unlock();
            return true;
        }

        // If m already in h's msgList, return false
        readLock.unlock();
        return false;
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
        int index = list.indexOf(m);
        readLock.unlock();
        if (index != -1) {
            return true;
        }
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

    public static boolean isMajorityInMap(int numHosts, Message m, ConcurrentHashMap<Message, ArrayList<Host>> map) {
        readLock.lock();
        ArrayList<Host> hostList = map.get(m);
        if (hostList != null) {
            double numAcks = (double) hostList.size();
            double total = (double) numHosts;
            double perAcks = numAcks / total;

            if (perAcks > 0.5) {
                readLock.unlock();
                readLock.unlock();
                return true;
            }
        }

        readLock.unlock();
        return false;
    }
}

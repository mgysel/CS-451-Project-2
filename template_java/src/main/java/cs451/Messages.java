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
            readLock.unlock();
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
}

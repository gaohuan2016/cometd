package org.cometd.bayeux.server;


import java.util.Queue;

import org.cometd.bayeux.Message;
import org.cometd.bayeux.Session;


/**
 * @version $Revision$ $Date: 2009-12-08 09:42:45 +1100 (Tue, 08 Dec 2009) $
 */
public interface ServerSession extends Session
{
    void batch(Runnable batch);
    void deliver(ServerSession from, ServerMessage msg);

    void disconnect();
    void endBatch();
    void startBatch();

    boolean isLocalSession();
    LocalSession getLocalSession();
    
    Queue<ServerMessage> getQueue();


    interface ServerSessionListener extends Session.SessionListener
    {}


    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /** Queue a message listener
     * <p>
     * Listener that allows per session customization of messages
     */
    public interface QueueListener extends ServerSessionListener
    {
        public ServerMessage onQueue(Session from, ServerSession session, ServerMessage message);
    };

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    public interface DeQueueListener extends ServerSessionListener
    {
        /* ------------------------------------------------------------ */
        /**
         * callback to notify that the queue is about to be sent to the
         * client.  This is the last chance to process the queue and remove
         * duplicates or merge messages.
         */
        public void deQueue(ServerSession session);
    };


    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    public interface MaxQueueListener extends ServerSessionListener
    {
        /* ------------------------------------------------------------ */
        /**
         * Call back to notify if a message for a client will result in the
         * message queue exceeding {@link Session#getMaxQueue()}.
         * This is called with the client instance locked, so it is safe for the
         * handler to manipulate the queue returned by {@link Session#getQueue()}, but
         * action in the callback that may result in another Client instance should be
         * avoided as that would risk deadlock.
         * @param from Client message is published from
         * @param to Client message is being delivered to
         * @param message
         * @return true if the message should be added to the client queue
         */
        public boolean queueMaxed(Session from, ServerSession to, Message message);
    }
}

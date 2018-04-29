package sk.wearClient.helpers;

import com.google.gson.GsonBuilder;

import org.apache.commons.collections4.queue.CircularFifoQueue;

public class SampleMessage {

    public static String CLIENT_ID = "pali@michalek.it";
    private CircularFifoQueue<Float> x;
    private CircularFifoQueue<Float> y;
    private CircularFifoQueue<Float> z;

    private long messageId;
    private String clientId;


    private static SampleMessage instance = null;

    private SampleMessage() {
        this.x = new CircularFifoQueue<>(100);
        this.y = new CircularFifoQueue<>(100);
        this.z = new CircularFifoQueue<>(100);

        this.clientId = CLIENT_ID;
    }

    // singleton
    public static SampleMessage getInstance() {
        if(instance == null) {
            instance = new SampleMessage();
        }
        return instance;
    }


    public void insertValues(Float x, Float y, Float z) {

        this.x.offer(x);
        this.y.offer(y);
        this.z.offer(z);
    }

    public long getMessageId() {
        return messageId;
    }

    public void setMessageId(long messageId) {
        this.messageId = messageId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    public void setClientId() {
        this.clientId = CLIENT_ID;
    }

    public Float getLastX() {
        return x.peek();
    }
    public Float getLastY() {
        return y.peek();
    }
    public Float getLastZ() {
        return z.peek();
    }

    @Override
    public String toString() {
        return new GsonBuilder().create().toJson(this, SampleMessage.class);
    }

    public void clear() {
        this.x.clear();
        this.y.clear();
        this.z.clear();
    }
}

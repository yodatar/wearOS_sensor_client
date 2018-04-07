package sk.wearClient.helpers;



import com.google.gson.GsonBuilder;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;


public class SampleMessage {
    private List<Float> x;
    private List<Float> y;
    private List<Float> z;
    private List<Long> time;
    private long id;
    private String clientId;

    public SampleMessage() {
        this.x = new ArrayList<>();
        this.y = new ArrayList<>();
        this.z = new ArrayList<>();
        this.time = new ArrayList<>();
        //this.clientId = MqttHelper.clientId;
    }

    public void setValues(Float x, Float y, Float z, long timestamp, long id, String clientId) {

        this.x.add(x);
        this.y.add(y);
        this.z.add(z);
        this.time.add(timestamp);
        this.id = id;
        this.clientId = clientId;
    }

/*
    public setSampleMessage(String msg) {
        if (msg != null){
            clear();

            new GsonBuilder().

            this.x.add(x);
            this.y.add(y);
            this.z.add(z);
            this.time.add(timestamp);
            this.id = id;
            this.clientId = clientId;
        }

    }
*/

    @Override
    public String toString() {
        return new GsonBuilder().create().toJson(this, SampleMessage.class);
    }

    public void clear() {
        this.x.clear();
        this.y.clear();
        this.z.clear();
        this.time.clear();
        this.id = 0;
    }

    public long getId() {
        return id;
    }
}

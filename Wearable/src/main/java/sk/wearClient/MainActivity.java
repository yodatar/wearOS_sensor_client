/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sk.wearClient;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.wearable.view.DotsPageIndicator;
import android.support.wearable.view.FragmentGridPagerAdapter;
import android.support.wearable.view.GridViewPager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sk.wearClient.fragments.AssetFragment;



/**
 * The main activity with a view pager, containing three pages:<p/>
 * <ul>
 * <li>
 * Page 1: shows a list of DataItems received from the phone application
 * </li>
 * <li>
 * Page 2: shows the photo that is sent from the phone application
 * </li>
 * <li>
 * Page 3: includes two buttons to show the connected phone and watch devices
 * </li>
 * </ul>
 */
public class MainActivity extends Activity implements
        DataClient.OnDataChangedListener,
        MessageClient.OnMessageReceivedListener,
        CapabilityClient.OnCapabilityChangedListener {

    private static final String TAG = "MainActivity";
    private static final String CAPABILITY_1_NAME = "capability_1";
    private static final String CAPABILITY_2_NAME = "capability_2";
    private static final String DATA_ITEM_RECEIVED_PATH = "/data-item-received";

    private GridViewPager mPager;
    private wearClient.fragments.DataFragment mDataFragment;
    private AssetFragment mAssetFragment;

    GoogleApiClient mGoogleApiClient;
    private SensorManager sensorManager;
    private List<Sensor> sensors;
    private SensorEventListener listener;
    private Sensor sensor;
    private HashSet<Node> nodes;
    private Node node;
    boolean isCaptured = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setupViews();

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        //write out to the log all the sensors the device has.
        sensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
        if (sensors.size() < 1) {
            Toast.makeText(this, "No sensors returned from getSensorList", Toast.LENGTH_SHORT).show();
            Log.wtf(TAG,"No sensors returned from getSensorList");
        }
        Sensor[] sensorArray = sensors.toArray(new Sensor[sensors.size()]);
        for (int i = 0; i < sensorArray.length; i++) {
            Log.d(TAG,"Found sensor " + i + " " + sensorArray[i].toString());
        }


        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();

    }

    @Override
    protected void onResume() {
        super.onResume();


        // Instantiates clients without member variables, as clients are inexpensive to create and
        // won't lose their listeners. (They are cached and shared between GoogleApi instances.)
        Wearable.getDataClient(this).addListener(this);
        Wearable.getMessageClient(this).addListener(this);
        Wearable.getCapabilityClient(this)
                .addListener(
                        this, Uri.parse("wear://"), CapabilityClient.FILTER_REACHABLE);

        registerSensor();

        findNodes();

    }

    @Override
    protected void onPause() {
        super.onPause();

        Wearable.getDataClient(this).removeListener(this);
        Wearable.getMessageClient(this).removeListener(this);
        Wearable.getCapabilityClient(this).removeListener(this);

        unregisterSensor();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(TAG, "onDataChanged(): " + dataEvents);

    /*    if (!mGoogleApiClient.isConnected() || !mGoogleApiClient.isConnecting()) {
            ConnectionResult connectionResult = mGoogleApiClient
                    .blockingConnect(30, TimeUnit.SECONDS);
            if (!connectionResult.isSuccess()) {
                Log.e(TAG, "DataLayerListenerService failed to connect to GoogleApiClient, "
                        + "error code: " + connectionResult.getErrorCode());
                return;
            }
        }*/


        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                String path = event.getDataItem().getUri().getPath();

                if (DataLayerListenerService.IMAGE_PATH.equals(path)) {

                } else if (DataLayerListenerService.COUNT_PATH.equals(path)) {
                    Log.d(TAG, "Data Changed for COUNT_PATH");
                    mDataFragment.appendItem("DataItem Changed", event.getDataItem().toString());


                    byte[] payload = event.getDataItem().getUri().toString().getBytes();

                    if(node != null) {
                       // Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), DATA_ITEM_RECEIVED_PATH, payload);
                    }


                } else {
                    Log.d(TAG, "Unrecognized path: " + path);
                }

            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                mDataFragment.appendItem("DataItem Deleted", event.getDataItem().toString());
            } else {
                mDataFragment.appendItem("Unknown data event type", "Type = " + event.getType());
            }
        }
    }

    public void onClicked(View view) {
        switch (view.getId()) {
            case R.id.capability_2_btn:
                showNodes(CAPABILITY_2_NAME);
                break;
            case R.id.capabilities_1_and_2_btn:
                showNodes(CAPABILITY_1_NAME, CAPABILITY_2_NAME);
                break;
            default:
                Log.e(TAG, "Unknown click event registered");
        }
    }

    @Override
    public void onMessageReceived(MessageEvent event) {
        Log.d(TAG, "onMessageReceived: " + event);
        mDataFragment.appendItem("Message", event.toString());
    }

    @Override
    public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
        Log.d(TAG, "onCapabilityChanged: " + capabilityInfo);
        mDataFragment.appendItem("onCapabilityChanged", capabilityInfo.toString());
    }

    /**
     * Find the connected nodes that provide at least one of the given capabilities
     */
    private void findNodes() {
        Task<Map<String, CapabilityInfo>> capabilitiesTask =
                Wearable.getCapabilityClient(this)
                        .getAllCapabilities(CapabilityClient.FILTER_REACHABLE);

        capabilitiesTask.addOnSuccessListener(new OnSuccessListener<Map<String, CapabilityInfo>>() {
            @Override
            public void onSuccess(Map<String, CapabilityInfo> capabilityInfoMap) {
                nodes = new HashSet<>();

                CapabilityInfo capabilityInfo = capabilityInfoMap.get(CAPABILITY_1_NAME);
                if (capabilityInfo != null) {
                    nodes.addAll(capabilityInfo.getNodes());
                }

                setNode(nodes);
            }
        });
    }

    private void setNode(HashSet<Node> nodes) {
        for (Node node : nodes) {
            this.node = node;
        }
    }

    private void showNodes(final String... capabilityNames) {

        Task<Map<String, CapabilityInfo>> capabilitiesTask =
                Wearable.getCapabilityClient(this)
                        .getAllCapabilities(CapabilityClient.FILTER_REACHABLE);

        capabilitiesTask.addOnSuccessListener(new OnSuccessListener<Map<String, CapabilityInfo>>() {
            @Override
            public void onSuccess(Map<String, CapabilityInfo> capabilityInfoMap) {
                nodes = new HashSet<>();

                if (capabilityInfoMap.isEmpty()) {
                    showDiscoveredNodes(nodes);
                    return;
                }
                for (String capabilityName : capabilityNames) {
                    CapabilityInfo capabilityInfo = capabilityInfoMap.get(capabilityName);
                    if (capabilityInfo != null) {
                        nodes.addAll(capabilityInfo.getNodes());
                    }
                }
                showDiscoveredNodes(nodes);
            }
        });
    }

    private void showDiscoveredNodes(Set<Node> nodes) {
        List<String> nodesList = new ArrayList<>();
        for (Node node : nodes) {
            nodesList.add(node.getDisplayName());
            this.node = node;
        }
        Log.w(TAG, "Connected Nodes: " + (nodesList.isEmpty()
                ? "No connected device was found for the given capabilities"
                : TextUtils.join(",", nodesList)));
        String msg;
        if (!nodesList.isEmpty()) {
            msg = getString(R.string.connected_nodes,
                    TextUtils.join(", ", nodesList));
        } else {
            msg = getString(R.string.no_device);
        }
        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
    }

    private void setupViews() {
        mPager = (GridViewPager) findViewById(R.id.pager);
        mPager.setOffscreenPageCount(2);
        DotsPageIndicator dotsPageIndicator = (DotsPageIndicator) findViewById(R.id.page_indicator);
        dotsPageIndicator.setDotSpacing((int) getResources().getDimension(R.dimen.dots_spacing));
        dotsPageIndicator.setPager(mPager);
        mDataFragment = new wearClient.fragments.DataFragment();
        mAssetFragment = new AssetFragment();
        wearClient.fragments.DiscoveryFragment discoveryFragment = new wearClient.fragments.DiscoveryFragment();
        List<Fragment> pages = new ArrayList<>();
        pages.add(mDataFragment);
        pages.add(mAssetFragment);
        pages.add(discoveryFragment);
        final MyPagerAdapter adapter = new MyPagerAdapter(getFragmentManager(), pages);
        mPager.setAdapter(adapter);
    }

    private class MyPagerAdapter extends FragmentGridPagerAdapter {

        private List<Fragment> mFragments;

        public MyPagerAdapter(FragmentManager fm, List<Fragment> fragments) {
            super(fm);
            mFragments = fragments;
        }

        @Override
        public int getRowCount() {
            return 1;
        }

        @Override
        public int getColumnCount(int row) {
            return mFragments == null ? 0 : mFragments.size();
        }

        @Override
        public Fragment getFragment(int row, int column) {
            return mFragments.get(column);
        }

    }

    private void sendSample(String payload) {
        if(node != null && !isCaptured) {
            Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), DATA_ITEM_RECEIVED_PATH,
                    payload.getBytes());
            isCaptured = true;
            Log.wtf(TAG,"isCaptured: " + payload);
        }
    }

    void registerSensor() {


        if (sensorManager == null)
            sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);

        sensors = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
        if(sensors.size() > 0)
            sensor = sensors.get(0);

        listener = new SensorEventListener() {

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                // I have no desire to deal with the accuracy events

            }
            @Override
            public void onSensorChanged(SensorEvent event) {
                //just set the values to a textview so they can be displayed.
                if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    String msg = " x: "+String.valueOf(event.values[0]) +
                            "\n y: "+String.valueOf(event.values[1]) +
                            "\n z: "+String.valueOf(event.values[2]) +
                            "\n 3: " + String.valueOf(event.values[3]) +    //for the TYPE_ROTATION_VECTOR these 2 exist.
                            "\n 4: " + String.valueOf(event.values[4]);
                    mAssetFragment.setText(msg);

                    sendSample(msg);
                }
            }
        };

        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL);


/*
        sensors = sensorManager.getSensorList(Sensor.TYPE_SIGNIFICANT_MOTION);
        if(sensors.size() > 0)
            mSigMotion = sensors.get(0);

        final TriggerEventListener mListener = new TriggerListener() {
            @Override
            public void onTrigger(TriggerEvent triggerEvent) {
                Log.wtf(TAG,"triggerEvent");
                Log.wtf(TAG, String.valueOf(triggerEvent.values[0]));
            }
        };
        mSensorManager.requestTriggerSensor(mListener, mSigMotion);
*/

    }
    void unregisterSensor() {
        if (sensorManager != null && listener != null) {
            sensorManager.unregisterListener(listener);
        }
        //clean up and release memory.
        sensorManager = null;
        listener = null;
    }



}
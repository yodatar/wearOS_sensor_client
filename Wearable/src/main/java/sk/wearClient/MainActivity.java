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
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.wearable.view.DotsPageIndicator;
import android.support.wearable.view.FragmentGridPagerAdapter;
import android.support.wearable.view.GridViewPager;
import android.util.Log;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import sk.wearClient.fragments.AssetFragment;
import sk.wearClient.fragments.DataFragment;
import sk.wearClient.fragments.DiscoveryFragment;
import sk.wearClient.helpers.SampleMessage;


public class MainActivity extends Activity implements
        DataClient.OnDataChangedListener,
        MessageClient.OnMessageReceivedListener,
        CapabilityClient.OnCapabilityChangedListener {

    private static final String TAG = "MainActivity";
    private static final String CAPABILITY_1_NAME = "capability_1";
    private static final String DATA_SEND = "/data-send";

    private GridViewPager mPager;
    private sk.wearClient.fragments.DataFragment mDataFragment;
    private static AssetFragment mAssetFragment;
    private DiscoveryFragment discoveryFragment;


    public static AssetFragment getmAssetFragment() {
        return mAssetFragment;
    }

    private static GoogleApiClient mGoogleApiClient;
    private HashSet<Node> nodes;
    private static Node node;

    static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private AccelerometerManager accelerometerManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setupViews();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();

        accelerometerManager = new AccelerometerManager();
    }

    @Override
    protected void onResume() {
        super.onResume();

        Wearable.getDataClient(this).addListener(this);
        Wearable.getMessageClient(this).addListener(this);
        Wearable.getCapabilityClient(this)
                .addListener(
                        this, Uri.parse("wear://"), CapabilityClient.FILTER_REACHABLE);

        accelerometerManager.registerSensor(this);

        findNodes();
    }

    @Override
    protected void onPause() {
        super.onPause();

        Wearable.getDataClient(this).removeListener(this);
        Wearable.getMessageClient(this).removeListener(this);
        Wearable.getCapabilityClient(this).removeListener(this);

        accelerometerManager.unregisterSensor();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(TAG, "onDataChanged(): " + dataEvents);

        // loop through events received
        for (DataEvent event : dataEvents) {

            // TYPE_CHANGED
            if (event.getType() == DataEvent.TYPE_CHANGED) {

                String path = event.getDataItem().getUri().getPath();

             if (DataLayerListenerService.SET_CLIENT_ID.equals(path)) {
                    /*
                    byte[] payload = event.getDataItem().getUri().toString().getBytes();

                    if(node != null) {
                        Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), DATA_SEND, payload);
                    }
                    */

                } else {
                    Log.d(TAG, "Unrecognized path: " + path);
                }
            } else {
                mDataFragment.appendItem("Unknown data event type", "Type = " + event.getType());
            }
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
            Log.wtf("node", node.getDisplayName());

            MainActivity.node = node;
        }
    }

    private void setupViews() {
        mPager = (GridViewPager) findViewById(R.id.pager);
        mPager.setOffscreenPageCount(2);
        DotsPageIndicator dotsPageIndicator = (DotsPageIndicator) findViewById(R.id.page_indicator);
        dotsPageIndicator.setDotSpacing((int) getResources().getDimension(R.dimen.dots_spacing));
        dotsPageIndicator.setPager(mPager);
        mAssetFragment = new AssetFragment();
        mDataFragment = new DataFragment();
        discoveryFragment = new DiscoveryFragment();
        List<Fragment> pages = new ArrayList<>();
        pages.add(mAssetFragment);
        pages.add(mDataFragment);
        pages.add(discoveryFragment);
        final MyPagerAdapter adapter = new MyPagerAdapter(getFragmentManager(), pages);
        mPager.setAdapter(adapter);
    }

    public static void sendSample(String payload, Long messageId) {
        Log.wtf("sendSample", sdf.format(new Date(messageId)));

        // display on UI
        mAssetFragment.setMessageId(sdf.format(new Date(messageId)));

       sendMessage(payload);
    }

    private static void sendMessage(String payload) {

        if(node != null) {
            // publish sample
            Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), DATA_SEND, payload.getBytes());
            Log.wtf("DATA_SEND", node.getId());
        }
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











}
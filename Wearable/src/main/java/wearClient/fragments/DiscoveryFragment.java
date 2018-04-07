

package wearClient.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.android.wearable.datalayer.R;


/**
 * A simple fragment with two buttons to show connected phones and watches
 */
public class DiscoveryFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.discovery_fragment, container, false);
    }
}

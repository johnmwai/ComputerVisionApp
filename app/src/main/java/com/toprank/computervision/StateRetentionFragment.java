package com.toprank.computervision;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.os.Bundle;

import java.util.HashMap;
import java.util.Objects;

/**
 * Created by john on 7/8/15.
 */
public class StateRetentionFragment extends Fragment {

    private HashMap map = new HashMap();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retain this fragment across configuration changes.
        setRetainInstance(true);
    }

    public Object get(String name, Object def){
        Object res = map.get(name);
        return res == null ? def : res;
    }

    public void set(String name, Object val){
        map.put(name, val);
    }
}

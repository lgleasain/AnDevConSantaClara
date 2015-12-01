package com.polyglotprogramminginc.andevcon;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.mbientlab.metawear.AsyncOperation;
import com.mbientlab.metawear.Message;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.RouteManager;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.module.MultiChannelTemperature;

import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class ThermistorFragment extends Fragment {


    private MetaWearBoard metaWearBoard;
    private ListView temperatureList;
    private ArrayList temperatureItemList;

    public ThermistorFragment() {
        // Required empty public constructor
    }

    public void setMetaWearBoard(MetaWearBoard metaWearBoard) {
        this.metaWearBoard = metaWearBoard;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_thermistor, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        temperatureList = (ListView) getView().findViewById(R.id.temperatureList);
        temperatureItemList = new ArrayList<String>();
        // Create The Adapter with passing ArrayList as 3rd parameter
        ArrayAdapter<String> arrayAdapter =
                new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, temperatureItemList);
        // Set The Adapter
        temperatureList.setAdapter(arrayAdapter);

        ((Button) getView().findViewById(R.id.downloadTemperature)).setOnClickListener(
                new Button.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        readTemperature();
                    }
                }
        );
    }

    public void readTemperature() {
        try {
            final MultiChannelTemperature mcTempModule = metaWearBoard.getModule(MultiChannelTemperature.class);
            final List<MultiChannelTemperature.Source> tempSources = mcTempModule.getSources();
            mcTempModule.routeData()
                    .fromSource(tempSources.get(MultiChannelTemperature.MetaWearRChannel.NRF_DIE)).stream("temp_nrf_stream")
                    .commit().onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                @Override
                public void success(RouteManager result) {
                    result.subscribe("temp_nrf_stream", new RouteManager.MessageHandler() {
                        @Override
                        public void process(Message msg) {
                            final Float temperatureReading = msg.getData(Float.class);
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                    //((TextView) getView().findViewById(R.id.temperature_reading)).setText(String.valueOf(temperatureReading));
                                }
                            });
                            Log.i("MainActivity", String.format("Ext thermistor: %.3fC",
                                    temperatureReading));
                        }
                    });

                    // Read temperature from the NRF soc chip
                    mcTempModule.readTemperature(tempSources.get(MultiChannelTemperature.MetaWearRChannel.NRF_DIE));
                }
            });
        } catch (UnsupportedModuleException e) {
            Log.e("Thermistor Fragment", e.toString());
        }
    }
}

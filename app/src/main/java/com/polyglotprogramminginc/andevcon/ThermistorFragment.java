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
import com.mbientlab.metawear.module.Logging;
import com.mbientlab.metawear.module.MultiChannelTemperature;
import com.mbientlab.metawear.module.Timer;

import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class ThermistorFragment extends Fragment {


    private MetaWearBoard metaWearBoard;
    private ListView temperatureList;
    private ArrayList temperatureItemList;
    private MultiChannelTemperature mcTempModule;
    private ArrayAdapter<String> temperatureArrayAdapter;
    private Logging loggingModule;

    public ThermistorFragment() {
        // Required empty public constructor
    }

    private final RouteManager.MessageHandler loggingMessageHandler = new RouteManager.MessageHandler() {
        @Override
        public void process(Message msg) {
            Log.i("MainActivity", String.format("Ext thermistor: %.3fC",

                    msg.getData(Float.class)));
            java.sql.Date date = new java.sql.Date(msg.getTimestamp().getTimeInMillis());
            temperatureItemList.add(String.valueOf(msg.getData(Float.class) + "    " + msg.getTimestampAsString()));
        }
    };


    private final AsyncOperation.CompletionHandler<RouteManager> temperatureHandler = new AsyncOperation.CompletionHandler<RouteManager>() {
        @Override
        public void success(RouteManager result) {
            result.setLogMessageHandler("mystream", loggingMessageHandler);

            // Read temperature from the NRF soc chip
            try {
                AsyncOperation<Timer.Controller> taskResult = metaWearBoard.getModule(Timer.class)
                        .scheduleTask(new Timer.Task() {
                            @Override
                            public void commands() {
                                mcTempModule.readTemperature(mcTempModule.getSources().get(MultiChannelTemperature.MetaWearRChannel.NRF_DIE));
                            }
                        }, 30000, false);
                taskResult.onComplete(new AsyncOperation.CompletionHandler<Timer.Controller>() {
                    @Override
                    public void success(Timer.Controller result) {
                        // start executing the task
                        result.start();
                    }
                });
            } catch (UnsupportedModuleException e) {
                Log.e("Temperature Fragment", e.toString());
            }

        }
    };


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

    public void startTemperatureLogging() {
        try {
            if (mcTempModule == null) {
                mcTempModule = metaWearBoard.getModule(MultiChannelTemperature.class);
                List<MultiChannelTemperature.Source> tempSources = mcTempModule.getSources();

                MultiChannelTemperature.Source tempSource = tempSources.get(MultiChannelTemperature.MetaWearRChannel.NRF_DIE);
                mcTempModule.routeData().fromSource(tempSource).log("mystream")
                        .commit().onComplete(temperatureHandler);
            }

            loggingModule = metaWearBoard.getModule(Logging.class);
            loggingModule.startLogging();
        } catch (UnsupportedModuleException e) {
            Log.e("Thermistor Fragment", e.toString());
        }
    }

    public void readTemperature() {
        temperatureItemList = new ArrayList<String>();

        loggingModule.downloadLog((float) 0.1, new Logging.DownloadHandler() {
                    @Override
                    public void onProgressUpdate(int nEntriesLeft, int totalEntries) {
                        Log.i("Thermistor", String.format("Progress= %d / %d", nEntriesLeft,
                                totalEntries));

                        if (nEntriesLeft == 0) {
                            final ArrayAdapter<String> arrayAdapter =
                                    new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, temperatureItemList);
                            // Set The Adapter
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                    temperatureList.setAdapter(arrayAdapter);
                                }
                            });
                        }
                    }
                }
        );

    }
}

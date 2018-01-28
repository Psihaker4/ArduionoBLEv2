package com.example.psycho.arduionoble;

import android.arch.lifecycle.ViewModelProviders;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ThemedSpinnerAdapter;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private SpinnerAdapter spinnerAdapter;
    private BLeViewModel bLeViewModel;

    private int command;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        Spinner spinner = findViewById(R.id.spinner);
        spinnerAdapter = new SpinnerAdapter(toolbar.getContext());
        spinner.setAdapter(spinnerAdapter);


        bLeViewModel = ViewModelProviders.of(this).get(BLeViewModel.class);
        bLeViewModel.getDevices().observe(this, bluetoothDevices -> {
            for (BluetoothDevice device : bluetoothDevices) spinnerAdapter.addDevice(device);
        });

        bLeViewModel.getConnected2().observe(this, b ->
                Toast.makeText(this,b?"Connected":"Disconnected",Toast.LENGTH_LONG).show());

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                BluetoothDevice device = spinnerAdapter.getDevice(position);
                if (device != null) bLeViewModel.choose(device);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        ControlCircleLayout controlLayout = findViewById(R.id.controls);
        controlLayout.setupLayout();
        controlLayout.setOnCommandListener(c -> {
            if (c == command) return;
            bLeViewModel.send(c);
            command = c;
            Log.d("MainActivity", "onCreate: " + c);
        });
    }

    private class SpinnerAdapter extends ArrayAdapter<String> implements ThemedSpinnerAdapter {

        private final ThemedSpinnerAdapter.Helper dropDownHelper;

        List<BluetoothDevice> bluetoothDevices = new ArrayList<>();

        public SpinnerAdapter(Context context) {
            super(context, R.layout.spinner_list_item);
            dropDownHelper = new ThemedSpinnerAdapter.Helper(context);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {

            View view;

            if (convertView == null) {
                LayoutInflater inflater = dropDownHelper.getDropDownViewInflater();
                view = inflater.inflate(R.layout.double_spinner_list_item, parent, false);
            } else {
                view = convertView;
            }

            if(bluetoothDevices.get(position)!= null) {

                TextView nameView = view.findViewById(R.id.text_name);
                String name = bluetoothDevices.get(position).getName();
                nameView.setText(name==null?"null":name);

                TextView addressView = view.findViewById(R.id.text_address);
                addressView.setText(bluetoothDevices.get(position).getAddress());

            }
            return view;
        }

        @Override
        public Resources.Theme getDropDownViewTheme() {
            return dropDownHelper.getDropDownViewTheme();
        }

        @Override
        public void setDropDownViewTheme(Resources.Theme theme) {
            dropDownHelper.setDropDownViewTheme(theme);
        }

        public void addDevice(BluetoothDevice device) {
            if (!bluetoothDevices.contains(device)) {
                bluetoothDevices.add(device);
                if(device.getName() == null) add("nulLL");
                else add(device.getName());
                Log.d("SpinnerAdapter", "addDevice: ");
            }
        }

        public BluetoothDevice getDevice(int position){
            return bluetoothDevices.get(position);
        }

        public void clear() {
            super.clear();
            bluetoothDevices.clear();
            notifyDataSetChanged();
        }
    }

}

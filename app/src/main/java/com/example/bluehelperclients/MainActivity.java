package com.example.bluehelperclients;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.Serializable;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final int REQUEST_ENABLE_BT = 1;
    private final int REQUEST_LOCATION_PERMISSION = 2;
    private EditText startPointEditText;
    private EditText endPointEditText;
    private TextView nearestBeaconLabel;
    private CheckBox discoveryCheckBox;
    private Button start;
    public static String baseUrl = "http://t999640p.beget.tech";
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private SharedPreferences sharedPreferences;
    private boolean canUseTTS = false;
    private TextToSpeech tts;
    private Timer beaconTimeoutTimer = new Timer(true);
    private Timer testTimer = new Timer(true);
    private Timer forRoute = new Timer(true);
    private Map<String, BeaconInfo> beaconInfos = new HashMap<>();
    private BeaconInfo nearestBeaconInfo;
    private ArrayList<Point> points = new ArrayList<Point>();
    public static String textconst = "";
    public static String textForTTS = "";
    String buildingId = "4";
    public static ArrayList<Example> examples = new ArrayList<Example>();
    public static ArrayList<com.example.bluehelperclients.Response> responses = new ArrayList<com.example.bluehelperclients.Response>();
    public static ArrayList<Vector> vectors = new ArrayList<Vector>();
    public static String pointForRoute = "";
    public static Boolean flag = false;
    public static String absoluteStartPoint = "";
    public static String direction = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_main);
        sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        startPointEditText = findViewById(R.id.editText2);
        endPointEditText = findViewById(R.id.editText);
        discoveryCheckBox = findViewById(R.id.checkBox);
        nearestBeaconLabel = findViewById(R.id.textView4);
        start = findViewById(R.id.button);
        start.setOnClickListener(this);
        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                canUseTTS = true;
            }
        });
        getBluetoothAdapter();

        discoveryCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    if ((bluetoothAdapter == null) || !bluetoothAdapter.isEnabled()) {
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                        return;
                    }
                    if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) !=
                            PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                REQUEST_LOCATION_PERMISSION);
                    }
                    startDiscovery();
                } else {
                    stopDiscovery();
                }
            }
        });

        beaconTimeoutTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        long curTime = System.currentTimeMillis();
                        Iterator<Map.Entry<String, MainActivity.BeaconInfo>> it = beaconInfos.entrySet().iterator();
                        boolean somethingChanged = false;
                        while (it.hasNext()) {
                            Map.Entry<String, MainActivity.BeaconInfo> entry = it.next();
                            MainActivity.BeaconInfo beaconInfo = entry.getValue();
                            if ((curTime - beaconInfo.lastSeen) > 5000) {
                                it.remove();
                                if (beaconInfo == nearestBeaconInfo) {
                                    nearestBeaconInfo = null;
                                    nearestBeaconLabel.setVisibility(View.INVISIBLE);
                                }
                                somethingChanged = true;
                            }
                        }
                        if (somethingChanged) {
                            beaconListAdapter.notifyDataSetChanged();
                        }
                    }
                });
            }
        }, 1000, 1000);


//        testTimer.schedule(new TimerTask() {
//            @Override
//            public void run() {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        tts.speak(textconst, TextToSpeech.QUEUE_FLUSH, null);
//                        textconst = "";
//                    }
//                });
//            }
//        }, 1000, 60000);

        map(buildingId);
        discoveryCheckBox.setChecked(true);

        CompletableFuture.runAsync(() -> test());
    }

    public void test() {
        while (true) {
            if (!nearestBeaconLabel.getText().toString().trim().equals(textconst)) {
                tts.speak(nearestBeaconLabel.getText().toString().trim(), TextToSpeech.QUEUE_FLUSH, null);
                tts.speak(direction, TextToSpeech.QUEUE_ADD, null);
            }
        }
    }

    @Override
    public void onClick(View v) {
        final String[] startPoint = {startPointEditText.getText().toString().trim()};
        final String[] endPoint = {endPointEditText.getText().toString().trim()};
        final String[] currentNP = {""};
        if (!startPoint[0].isEmpty() & !endPoint[0].isEmpty()) {
            if (!currentNP[0].equals(endPoint[0])) {
                CompletableFuture.runAsync(() -> forRoute.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (!nearestBeaconLabel.getText().toString().trim().equals(currentNP[0]) & !nearestBeaconLabel.getText().toString().trim().equals("")) {
                                    startPoint[0] = startPointEditText.getText().toString().trim();
                                    endPoint[0] = endPointEditText.getText().toString().trim();
                                    createRoute(startPoint[0], endPoint[0]);
                                    currentNP[0] = nearestBeaconLabel.getText().toString().trim();
                                }
                            }
                        });
                    }
                }, 3000, 1000));
            }
            //CompletableFuture.runAsync(() -> createRoute(startPoint, endPoint));
        } else {
            String textError = "Данные не введены";
            tts.speak(textError, TextToSpeech.QUEUE_FLUSH, null);
        }
    }


    private void createRoute(String startPoint, String endPoint) {
        String currentPoint = "";
        String prevPoint = "";
        String startPointId = "";
        String endPointId = "";
        String textDirection = "";

//        if (flag == false) {
//            for (int i = 0; i < responses.size(); i++) {
//                if (startPoint.equals(responses.get(i).getTitle())) {
//                    startPointId = responses.get(i).getId();
//                }
//            }
//            for (int i = 0; i < responses.size(); i++) {
//                if (endPoint.equals(responses.get(i).getTitle())) {
//                    endPointId = responses.get(i).getId();
//                }
//            }

        while (true) {
            if (startPoint.equals("точка1")) {
                direction = "Идите прямо";
                tts.speak(direction, TextToSpeech.QUEUE_FLUSH, null);
                direction = "";
                //currentPoint = nearestBeaconLabel.getText().toString().trim();
                break;
            } else if (startPoint.equals("точка2")) {
                direction = "Поверните налево и идите прямо";
                tts.speak(direction, TextToSpeech.QUEUE_FLUSH, null);
                direction = "";
                break;
            } else if (startPoint.equals("точка3") & endPoint.equals("точка4")) {
                direction = "Поверните направо и идите прямо";
                tts.speak(direction, TextToSpeech.QUEUE_FLUSH, null);
                direction = "";
                break;
            } else if (startPoint.equals(endPoint)) {
                direction = "Вы пришли";
                tts.speak(direction, TextToSpeech.QUEUE_FLUSH, null);
                direction = "";
                break;
            } else if (startPoint.equals("точка4") & !endPoint.equals("точка4")) {
                direction = "Вы пошли неправильно. Вернитесь назад";
                tts.speak(direction, TextToSpeech.QUEUE_FLUSH, null);
                direction = "";
                break;
            }

        }
//            for (int i = 0; i < responses.size(); i++) {
////                for (int j = 0; j < responses.get(i).getVectors().size(); j++) {
////                    if (startPointId.equals(responses.get(i).getVectors().get(j).getStartPoint()) & endPointId.equals(responses.get(i).getVectors().get(j).getEndPoint())) {
//////                        if (Integer.parseInt(responses.get(i).getVectors().get(j).getDirection()) == 0) {
//////                            textDirection = "Идите прямо";
//////                            tts.speak(textDirection, TextToSpeech.QUEUE_FLUSH, null);
//////                            break;
////                        if (Integer.parseInt(responses.get(i).getVectors().get(j).getDirection()) < 90 & Integer.parseInt(responses.get(i).getVectors().get(j).getDirection()) > 0) {
////                            textDirection = "Поверните правее и идите прямо";
////                            tts.speak(textDirection, TextToSpeech.QUEUE_FLUSH, null);
////                            break;
////                        } else if (Integer.parseInt(responses.get(i).getVectors().get(j).getDirection()) == 90) {
////                            textDirection = "Идите направо";
////                            tts.speak(textDirection, TextToSpeech.QUEUE_FLUSH, null);
////                            break;
////                        } else if (Integer.parseInt(responses.get(i).getVectors().get(j).getDirection()) < 180 & Integer.parseInt(responses.get(i).getVectors().get(j).getDirection()) > 90) {
////                            textDirection = "Сделайте почти полный оборот назад через правое плечо и идите прямо";
////                            tts.speak(textDirection, TextToSpeech.QUEUE_FLUSH, null);
////                            break;
////                        } else if (Integer.parseInt(responses.get(i).getVectors().get(j).getDirection()) == 180) {
////                            textDirection = "Поверните назад и идите прямо";
////                            tts.speak(textDirection, TextToSpeech.QUEUE_FLUSH, null);
////                            break;
////                        } else if (Integer.parseInt(responses.get(i).getVectors().get(j).getDirection()) < 270 & Integer.parseInt(responses.get(i).getVectors().get(j).getDirection()) > 180) {
////                            textDirection = "Сделайте почти полный оборот назад через левое плечо и идите прямо";
////                            tts.speak(textDirection, TextToSpeech.QUEUE_FLUSH, null);
////                            break;
//////                        } else if (Integer.parseInt(responses.get(i).getVectors().get(j).getDirection()) == 270) {
//////                            textDirection = "Поверните налево и идите прямо";
//////                            tts.speak(textDirection, TextToSpeech.QUEUE_FLUSH, null);
//////                            textDirection = "";
//////                            break;
////                        } else if (Integer.parseInt(responses.get(i).getVectors().get(j).getDirection()) <= 359 & Integer.parseInt(responses.get(i).getVectors().get(j).getDirection()) > 270) {
////                            textDirection = "Поверните левее и идите прямо";
////                            tts.speak(textDirection, TextToSpeech.QUEUE_FLUSH, null);
////                            break;
////                        }
////                    } else {
////                        if (absoluteStartPoint.equals("точка2") & startPoint.equals("точка1")) {
////                            textDirection = "Вы пошли неправильно, вернитесь назад";
////                            tts.speak(textDirection, TextToSpeech.QUEUE_FLUSH, null);
////                            textconst = "";
////                            absoluteStartPoint = "";
////                            textDirection = "";
////                            break;
////                        } else if ((textconst.equals(endPoint))) {
////                            textDirection = "Вы пришли";
////                            tts.speak(textDirection, TextToSpeech.QUEUE_FLUSH, null);
////                            textconst = "";
////                            flag = true;
////                            break;
////                        } else if (textconst.equals("точка2") & endPoint.equals("точка3")) {
////                            textDirection = "Поверните налево и идите прямо";
////                            tts.speak(textDirection, TextToSpeech.QUEUE_FLUSH, null);
////                            textconst = "";
////                            break;
////                        } else if (textconst.equals("точка1") & endPoint.equals("точка3") & absoluteStartPoint.equals("")) {
////                            textDirection = "Идите прямо";
////                            tts.speak(textDirection, TextToSpeech.QUEUE_FLUSH, null);
////                            textconst = "";
////                            break;
////                        }
////                    }
////                }
////            }
    }

    @Override
    protected void onDestroy() {
        stopDiscovery();
        beaconTimeoutTimer.purge();
        tts.shutdown();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (getBluetoothAdapter()) {
                    startDiscovery();
                }
                return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION_PERMISSION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startDiscovery();
                } else {
                    Toast toast = Toast.makeText(this, "Cannot perform bluetooth scan " +
                                    "without coarse location permission",
                            Toast.LENGTH_SHORT);
                    toast.show();
                    discoveryCheckBox.setChecked(false);
                }
                break;
        }
    }

    private boolean getBluetoothAdapter() {
        if (bluetoothAdapter != null) return true;
        BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
            if (bluetoothAdapter != null) {
                return true;
            }
        }
        Toast toast = Toast.makeText(this, "Failed to enable Bluetooth!",
                Toast.LENGTH_LONG);
        toast.show();
        discoveryCheckBox.setChecked(false);
        return false;
    }

    private void startDiscovery() {
        if (bluetoothAdapter != null) {
            discoveryCheckBox.setChecked(true);
            setProgressBarIndeterminateVisibility(Boolean.TRUE);
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
            bluetoothLeScanner.startScan(
                    null,
                    new ScanSettings.Builder()
                            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                            .build(),
                    scanCallback
            );
            Toast toast = Toast.makeText(this, "Discovery started...",
                    Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    private void stopDiscovery() {
        discoveryCheckBox.setChecked(false);
        setProgressBarIndeterminateVisibility(Boolean.FALSE);
        if (bluetoothAdapter != null) {
            bluetoothLeScanner.stopScan(scanCallback);
        }
    }

    private void processScanResult(final ScanResult result) {
        final String address = result.getDevice().getAddress();
        final int rssi = result.getRssi();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                String checked = MainActivity.this.checkPoint(beaconInfos);
                String text = "";

//                for (int i = 0; i < responses.size(); i++) {
//                    if (checked.equals(responses.get(i).getDeviceId())) {
//                        text = responses.get(i).getTitle();
//                        System.out.println(text);
//                    }
//                }
//
//                if (!checked.isEmpty()) {
//                    tts.speak(checked, TextToSpeech.QUEUE_FLUSH, null);
//                    nearestBeaconLabel.setVisibility(View.VISIBLE);
//                    nearestBeaconLabel.setText(checked);
//                } else {
//                    nearestBeaconLabel.setText("");
//                }

                MainActivity.BeaconInfo beaconInfo = beaconInfos.get(address);
                if (beaconInfo == null) {
                    beaconInfo = new MainActivity.BeaconInfo(address, rssi);
                    beaconInfos.put(beaconInfo.address, beaconInfo);
                    beaconInfo.title = sharedPreferences.getString(beaconInfo.address, null);
                } else {
                    beaconInfo.rssi = rssi;
                }
                beaconInfo.lastSeen = System.currentTimeMillis();
                beaconListAdapter.notifyDataSetChanged();
                if ((beaconInfo.rssi > -50) || (beaconInfo.rssi > -60) && (beaconInfo == nearestBeaconInfo)) {
                    if ((nearestBeaconInfo == null) || ((nearestBeaconInfo.rssi <= beaconInfo.rssi)) ||
                            (nearestBeaconInfo == beaconInfo)) {
                        if ((nearestBeaconInfo != beaconInfo) && (beaconInfo.title != null) && canUseTTS) {
                            for (int i = 0; i < responses.size(); i++) {
                                if (beaconInfo.address.equals(responses.get(i).getDeviceId())) {

                                }
                            }
                        }
                        nearestBeaconInfo = beaconInfo;
                        for (int i = 0; i < responses.size(); i++) {

                            if (beaconInfo.address.equals(responses.get(i).getDeviceId())) {
                                text = responses.get(i).getTitle();
                            }
                        }
                        nearestBeaconLabel.setText(text);
                        textconst = text;
                        startPointEditText.setText(text);
                        nearestBeaconLabel.setVisibility(View.VISIBLE);
                    }
                } else if (nearestBeaconInfo == beaconInfo) {
                    nearestBeaconInfo = null;
                    nearestBeaconLabel.setVisibility(View.INVISIBLE);
                    nearestBeaconLabel.setText("");
                    text = "";
                    textconst = "";
                }
            }
        });
    }

    public String checkPoint(Map<String, MainActivity.BeaconInfo> beaconInfos) {
        for (int i = 0; i < this.points.size(); i++) {
            Point point = this.points.get(i);

            int count = 0;

            for (Map.Entry<String, String> entry : point.beacons.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                MainActivity.BeaconInfo beacon = beaconInfos.get(key);

                if (beacon != null) {
                    int rssi = Integer.parseInt(value);

                    if (beacon.rssi >= rssi - 2 && beacon.rssi <= rssi + 2) {
                        count++;
                    }
                }
            }

            if (count == 3) {
                return point.name;
            }

        }
        return "";
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            processScanResult(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult scanResult : results) {
                processScanResult(scanResult);
            }
        }

        @Override
        public void onScanFailed(final int errorCode) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast toast = Toast.makeText(MainActivity.this,
                            "Scan error: " + errorCode, Toast.LENGTH_LONG);
                    toast.show();
                }
            });
        }
    };

    private final BaseAdapter beaconListAdapter = new BaseAdapter() {
        @Override
        public int getCount() {
            return beaconInfos.size();
        }

        @Override
        public Object getItem(int position) {
            return beaconInfos.values().toArray(new BeaconInfo[0])[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            MainListHolder mainListHolder;
            if (convertView == null) {
                mainListHolder = new MainListHolder();
                convertView = View.inflate(MainActivity.this, android.R.layout.two_line_list_item, null);
                mainListHolder.line1 = convertView.findViewById(android.R.id.text1);
                mainListHolder.line2 = convertView.findViewById(android.R.id.text2);
                convertView.setTag(mainListHolder);

            } else {
                mainListHolder = (MainListHolder) convertView.getTag();
            }
            BeaconInfo beaconInfo = (BeaconInfo) getItem(position);
            mainListHolder.line1.setText((beaconInfo.title != null) ?
                    beaconInfo.title : beaconInfo.address);
            mainListHolder.line2.setText(beaconInfo.rssi + " dbi, " + beaconInfo.lastSeen);
            return convertView;
        }
    };

    private static class BeaconInfo implements Serializable {
        final String address;
        String title;
        int rssi;
        long lastSeen;
        int[] avg = new int[3];
        int g = 0;

        public int getAvg() {
            for (int i = 0; i < avg.length; i++) {
                g = g + avg[i];
            }
            g = g / avg.length;
            return g;
        }

        public void setAvg(int[] avg) {
            this.avg = avg;
        }

        BeaconInfo(String address, int rssi) {
            this.address = address;
            this.rssi = rssi;
        }
    }

    private static class MainListHolder {
        private TextView line1;
        private TextView line2;
    }

    private void map(String buildingId) {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(MainActivity.baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        API api = retrofit.create(API.class);
        api.map(buildingId);

        Call<Example> call = api.map(buildingId);

        call.enqueue(new Callback<Example>() {
            @Override
            public void onResponse(Call<Example> call, Response<Example> response) {
                if (response.isSuccessful()) {
                    responses.clear();
                    for (int i = 0; i < response.body().getResponse().size(); i++) {
                        for (int j = 0; j < response.body().getResponse().get(i).getVectors().size(); j++) {
                            Vector vector = new Vector(
                                    response.body().getResponse().get(i).getVectors().get(j).getId(),
                                    response.body().getResponse().get(i).getVectors().get(j).getBuildingId(),
                                    response.body().getResponse().get(i).getVectors().get(j).getStartPoint(),
                                    response.body().getResponse().get(i).getVectors().get(j).getEndPoint(),
                                    response.body().getResponse().get(i).getVectors().get(j).getDistance(),
                                    response.body().getResponse().get(i).getVectors().get(j).getDirection(),
                                    response.body().getResponse().get(i).getVectors().get(j).getEditedBy(),
                                    response.body().getResponse().get(i).getVectors().get(j).getLastUpdate());
                            vectors.add(vector);
                        }
                        com.example.bluehelperclients.Response response1 = new com.example.bluehelperclients.Response(
                                response.body().getResponse().get(i).getId(),
                                response.body().getResponse().get(i).getDeviceId(),
                                response.body().getResponse().get(i).getTitle(),
                                response.body().getResponse().get(i).getBuildingId(),
                                response.body().getResponse().get(i).getEditedBy(),
                                response.body().getResponse().get(i).getLastUpdate(),
                                vectors);
                        responses.add(response1);
                    }
                }
                System.out.println(responses.get(0).getDeviceId());
            }


            @Override
            public void onFailure(Call<Example> call, Throwable t) {

            }
        });
    }
}

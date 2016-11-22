package com.example.nikolay.accgyro;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.TextView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.PriorityQueue;
import java.util.Queue;

import static android.hardware.Sensor.TYPE_ACCELEROMETER;

public class MainActivity extends Activity implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mGyroscope;

    private float xy;
    private float xz;
    private float zy;

    private TextView xyView;
    private TextView xzView;
    private TextView zyView;

    private TextView xyAccView;
    private TextView xzAccView;
    private TextView zyAccView;

    private TextView info;

    private File sdPathAcc;
    private File sdPathGyro;

    private File sdFileGyro;
    private File sdFileAcc;
    private BufferedWriter bwAcc;
    private BufferedWriter bwGyro;
    private int iAcc;
    private int iGyro;



    private long time0;
    private long timeStep;
    private long timeFall;
    private long timeAnalys;
    private float mediana;
    private int ocb;
    private float amplX, amplY, amplZ;
    private float max, min;
//    private int length;

//    private float maxX, maxY, maxZ;
//    private float minX, minY, minZ;

    private int steps;
    private int falls;

    private class Dot {
        private long time;
        private float [] values;

        public Dot(long t, float [] values){
            this.time = t;
            this.values = new float[3];
            this.values[0] = values[0];
            this.values[1] = values[1];
            this.values[2] = values[2];
            this.next = null;
        }

        private Dot next;
    }

    private Dot first;
    private Dot last;

    private void analys(){
        Dot d = first;
        float maxX,maxY,maxZ,minX,minY,minZ;
        minX = 1000;
        minY = 1000;
        minZ = 1000;
        maxX = -1000;
        maxY = -1000;
        maxZ = -1000;
        int length = 0;
        while(d != null){

            if (d.values[0] >= maxX){
                maxX = d.values[0];
            }
            if (d.values[0] <= minX){
                minX = d.values[0];
            }


            if (d.values[1] >= maxY){
                maxY = d.values[1];
            }
            if (d.values[1] <= minY){
                minY = d.values[1];
            }

            if (d.values[2] >= maxZ){
                maxZ = d.values[2];
            }
            if (d.values[2] <= minZ){
                minZ = d.values[2];
            }

            length += 1;
            d = d.next;
        }
//        info.setText(String.valueOf(length));

        amplX = maxX - minX;
        amplY = maxY - minY;
        amplZ = maxZ - minZ;

        if(amplX > amplY){
            if (amplX > amplZ){
                ocb = 0;
                max = maxX;
                min = minX;

            }else{
                ocb = 2;
                max = maxZ;
                min = minZ;
            }
        }else{
            if(amplY > amplZ){
                ocb = 1;
                max = maxY;
                min = minY;
            }else{
                ocb = 2;
                max = maxZ;
                min = minZ;
            }
        }
        d = first;

        float buf[] = new float[length];
        int i = 0;
        while (d != null){
            buf[i] = d.values[ocb];
            i += 1;
            d = d.next;
        }
//
        for(int l = 0; i < length; i++){
            for (int k = 0; k < length - 1; k++){
                if (buf[k] > buf[k+1]){
                    float tmp = buf[k];
                    buf[k] = buf[k+1];
                    buf[k+1] = tmp;
                }
            }
        }

        mediana = buf[length/2];
        d = first;
        float porogStep = 3;
        float porogFall = 15;
        while (d != null){
            if(d.time - timeFall > 1500) {
                if ((d.values[ocb] > mediana + porogFall) || (d.values[ocb] < mediana - porogFall)) {
                    this.falls += 1;
                    this.timeFall = d.time;
                } else {
                    if (d.time - timeStep > 300) {
                        if ((d.values[ocb] > mediana + porogStep) || (d.values[ocb] < mediana - porogStep)) {
                            steps++;
                            this.timeStep = d.time;
                        }
                    }
                }
            }

            d = d.next;
        }


    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        info = (TextView) findViewById(R.id.info);
        iAcc = 0;
        iGyro = 0;
        float f[] = {0,0,0};
        first = new Dot(0, f);
        last = first;
        Date date = new Date();
        timeAnalys = date.getTime();
        timeStep = 0;
        timeFall = 0;
//        minX = 1000;
//        minY = 1000;
//        minZ = 1000;
//        maxX = -1000;
//        maxY = -1000;
//        maxZ = -1000;

        try {

            File sdPath = android.os.Environment.getExternalStorageDirectory();
            sdPathGyro = new File(sdPath.getAbsolutePath() + "/" + "valuesXY" + "/" + "gyro");
            sdPathAcc = new File(sdPath.getAbsolutePath() + "/" + "valuesXY" + "/" + "acc");
            info.setText(String.valueOf(sdPath));
            bwAcc = null;
            bwGyro = null;

            newFile("gyro");
            newFile("acc");

        }catch(Exception e){

        }


        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(TYPE_ACCELEROMETER);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        xyView = (TextView) findViewById(R.id.xyValue);
        xzView = (TextView) findViewById(R.id.xzValue);
        zyView = (TextView) findViewById(R.id.zyValue);

        xyAccView = (TextView) findViewById(R.id.xyAccValue);
        xzAccView = (TextView) findViewById(R.id.xzAccValue);
        zyAccView = (TextView) findViewById(R.id.zyAccValue);


    }

    
    //create new file for acc or gyro values
    public void newFile(String name){
        try {
            if (name.equals("gyro")){
                sdFileGyro = new File(sdPathGyro, name + String.valueOf(iGyro) + ".txt");
                iGyro = iGyro + 1;
                if (bwGyro != null) {
                    bwGyro.close();
                }
                bwGyro = new BufferedWriter(new FileWriter(sdFileGyro, true));
            }
            if (name.equals("acc")){
                sdFileAcc = new File(sdPathAcc, name + String.valueOf(iAcc) + ".txt");
                iAcc = iAcc + 1;
                if (bwAcc != null) {
                    bwAcc.close();
                }
                bwAcc = new BufferedWriter(new FileWriter(sdFileAcc, true));

            }
        }catch(Exception e){

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy){

    }

    @Override
    protected void onResume(){
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
        try {
            bwAcc.close();
            bwGyro.close();
        }catch (Exception e){

        }
    }

    public File getFile(String name){
        if (name.equals("acc")){
            return sdFileAcc;
        }else{
            return sdFileGyro;
        }
    }

    public void writeFile(float[] values, long time, String name){
        try {
            File file = getFile(name);
            
     //for max file size ~1MB       
            while (file.length() > 1048701){
                newFile(name);
                file = getFile(name);
            }

            BufferedWriter bw;
            if (name.equals("acc")){
                bw = bwAcc;
            }else{
                bw = bwGyro;
            }
            bw.write(time + ";" + String.valueOf(values[0]) + ";" + String.valueOf(values[1]) + ";" + String.valueOf(values[2]) + ";" + "\n");
            bw.flush();
        }catch(IOException e){

        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Date date = new Date();
        long time = date.getTime();

        int type = event.sensor.getType();
        String s = "";
        if (first.values.equals(last.values)){
            s = "JOPA";
        }else{
            s = "HOROSHO";
        }

        info.setText(String.valueOf(steps) + "               " + String.valueOf(falls) + "         "
                +s);


        xy = event.values[0];
        xz = event.values[1];
        zy = event.values[2];

        if (type == Sensor.TYPE_GYROSCOPE) {
            xyView.setText(String.valueOf(xy));
            xzView.setText(String.valueOf(xz));
            zyView.setText(String.valueOf(zy));

            writeFile(event.values, event.timestamp, "gyro");
        }

        if (type == Sensor.TYPE_ACCELEROMETER) {
            xyAccView.setText(String.valueOf(xy));
            xzAccView.setText(String.valueOf(xz));
            zyAccView.setText(String.valueOf(zy));

//            queue.offer(new Dot(event.timestamp, event.values[0], event.values[1], event.values[2]));
//            if (queue.peek().time - event.timestamp < 200000000){
//                queue.poll();
//            }
//            if (event.timestamp - timeAnalys > 100000000){
//                analys();
//                timeAnalys = event.timestamp;
//            }

            Dot d = new Dot(time, event.values);
            last.next = d;
            last = d;
            if (time - first.time > 5000){
                first = first.next;
            }
            if (time - timeAnalys > 1000){
                analys();
                timeAnalys = time;
            }
            

            writeFile(event.values, event.timestamp, "acc");

        }
        String values = String.valueOf(xy);
        values.concat("\n");
    }
}

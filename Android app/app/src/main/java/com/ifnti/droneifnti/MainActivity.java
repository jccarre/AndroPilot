package com.ifnti.droneifnti;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private SensorManager sensorManager;
    private Handler mTimerHandler = new Handler();
    private Sensor sensor_gyro;
    private TextView textOrientationEstimée;
    private Button boutonReinitialiersAngles;
    private CommunicationUSB commUSB;
    /*private EditText editTextSendSms;
    private Button boutonenvoisms;*/

    private TextView messages_recus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.commUSB = new CommunicationUSB(this); //Les méthodes de CommunicationUSB ont besoin d'avoir accès à l'instance de l'activité en cours.
        this.commUSB.assignUsbManager();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor_gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        textOrientationEstimée = findViewById(R.id.textOrientationEstimée);
        messages_recus = (TextView)findViewById(R.id.messages_recus);
        EditText txtPhoneNo = (EditText) findViewById(R.id.txtPhoneNo);
        Button boutonNumTel = findViewById(R.id.boutonNumTel);
        Button boutonReinitialiersAngles = findViewById(R.id.boutonReinitialiersAngles);

        /*editTextSendSms = findViewById(R.id.editTextSendSms);
        boutonenvoisms = findViewById(R.id.boutonenvoisms);
        boutonenvoisms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RecepteurSMS.interpreterSMS(editTextSendSms.getText().toString());
            }
        });*/

        boutonNumTel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String num = txtPhoneNo.getText().toString();
                if(num.length() != 12){
                    Toast.makeText(MainActivity.this.getApplicationContext(), "Le numéro doit respecter la syntaxe +22893121871", Toast.LENGTH_SHORT).show();
                }else {
                    RecepteurSMS.setNumTelPilote(num);
                    String msg = "N°" + num + " enregistré";
                    Toast.makeText(MainActivity.this.getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                }
            }
        });

        boutonReinitialiersAngles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                KalmanFilter.setCurrentOrientation(0.0f, (float)(-10.0f * Math.PI / 180.0f), 0.0f);
            }
        });

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                mTimerHandler.post(new Runnable() {
                    public void run(){
                        //Le code écrit ici sera éxécuté à intervalle régulier (la période est spécifiée en bas).
                        float[] angles = KalmanFilter.getCurrentOrientation();
                        String azimuth = String.format("%6.1f│", angles[0] * 180.0f / Math.PI);
                        String pitch = String.format("%6.1f│", angles[1] * 180.0f / Math.PI);
                        String roll = String.format("%6.1f", angles[2] * 180.0f / Math.PI);
                        String msg = "Cap : " + azimuth + " Assiette : " + pitch + " Inclinaison : " + roll;
                        textOrientationEstimée.setText(msg);

                        afficher_sms_recu(RecepteurSMS.getMessage_reçu()); //Mise à jour de l'affichage du sms si jamais il y en a eu un nouveau.

                        commUSB.sendArduinoConsigneServos(Vol.calculerCommandesServos());
                    }
                });
            }
        },1, 250); //rafraichissement à 4Hz.
    }

    private SensorEventListener listener_rot = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                KalmanFilter.update_estimation(event);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    };

    public void afficher_sms_recu(String text){
        messages_recus.setText(text);
    }

    @Override
    protected void onResume() {
        super.onResume();

        //Si le onResume est invoqué, il est probable que ce soit parce qu'un périphérique USB vient d'être connecté. Il faut donc le prendre en compte.
        this.commUSB.resumeUSB();

        //Pour l'acquisition des données du gyromètre
        sensorManager.registerListener(listener_rot, sensor_gyro, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(listener_rot);
        }

    }
}
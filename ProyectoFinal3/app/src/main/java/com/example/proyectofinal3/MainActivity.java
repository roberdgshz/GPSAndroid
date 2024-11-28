package com.example.proyectofinal3;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.tasks.OnSuccessListener;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    //Uso de sensor
    private SensorManager sensorManager;
    //Contador de pasos
    private Sensor stepCounterSensor;
    //Evento del sensor
    private SensorEventListener stepListener;

    private FusedLocationProviderClient fusedLocationClient;

    // textviews que vamos a usar para el contador de pasos y localización
    private TextView stepCountTextView, locationTextView;

    //Mapa de google
    private GoogleMap mMap;

    //Permisos
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int ACTIVITY_RECOGNITION_REQUEST_CODE = 2;


    //Apartir de aquí, lo nuevo
    //Nuevos textfields
    private TextView distanceTextView, timeTextView, caloriesTextView, stepsByminutesTextView;

    //Boolean para controlar el botón del timer
    boolean isRunning = false;

    //Botones timer y reset
    private ImageView timer, reset;

    //Contador de degundos
    private int secondsElapsed = 0;
    private Handler handler = new Handler();

    //Variables Globales
    private int stepCount = 0;
    private double distance = 0.0;
    private double calories = 0.0;
    private double stepsPerMinute = 0.0;
    private int initialStepCount = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        stepCountTextView = findViewById(R.id.stepCountTextView);
        locationTextView = findViewById(R.id.locationTextView);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        // Inicializar el sensor de pasos
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        //Nuevos textviews
        distanceTextView = findViewById(R.id.distanceTextView);
        timeTextView = findViewById(R.id.timeTextView);
        caloriesTextView = findViewById(R.id.caloriesTextView);
        stepsByminutesTextView = findViewById(R.id.stepsByminutesTextView);

        //Botones
        timer = findViewById(R.id.buttonTimer);
        reset = findViewById(R.id.buttonReset);

        //to-do esto es lo que ya estaba
        if (stepCounterSensor != null) {
            stepListener = new SensorEventListener() {
                @Override
                public void onSensorChanged(SensorEvent event) {
                    if (initialStepCount == 0) {
                        initialStepCount = (int) event.values[0];
                    }
                    stepCount = (int) event.values[0] - initialStepCount;

                    //Calcular distancia
                    double stepLength = 0.50;
                    distance = stepCount * stepLength;

                    //Calcular calorías
                    double MET = 3.5;
                    int peso = 70;
                    double horas = (double) secondsElapsed / 3600;
                    calories = MET * peso * horas;

                    //Calcular pasos/minuto
                    double minutes = (double) secondsElapsed / 60;
                    stepsPerMinute = stepCount / minutes;

                    stepCountTextView.setText("Pasos: " + stepCount);
                    distanceTextView.setText("Distancia recorrida: "+ distance +" metros");
                    caloriesTextView.setText("Calorias consumidas: "+ calories);
                    stepsByminutesTextView.setText("Pasos por minuto: "+ stepsPerMinute);

                    //Boton timer
                    timer.setOnClickListener(new View.OnClickListener(){
                        @Override
                        public void onClick(View v) {
                            if (!isRunning) {
                                isRunning = true;
                                timer.setImageResource(R.drawable.stop);
                                startTimer();
                                reset.setEnabled(false);
                            } else {
                                timer.setImageResource(R.drawable.start);
                                isRunning = false;
                                handler.removeCallbacks(runnable);
                                reset.setEnabled(true);
                                event.values[0] = 0;
                            }
                        }
                    });

                    //Boton reset
                    reset.setOnClickListener(new View.OnClickListener(){
                        @Override
                        public void onClick(View v){
                            handler.removeCallbacks(runnable);
                            isRunning = false;
                            reset.setEnabled(true);

                            distanceTextView.setText("Distancia recorrida:-");
                            timeTextView.setText("Tiempo transcurrido: 00:00");
                            caloriesTextView.setText("Calorias consumidas:-");
                            stepsByminutesTextView.setText("Pasos por minuto:-");
                            stepCountTextView.setText("0");

                            initialStepCount = 0;
                            distance = 0;
                            stepCount = 0;
                            calories = 0;
                            stepsPerMinute = 0;
                            secondsElapsed = 0;
                        }
                    });

                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {
                }
            };

            sensorManager.registerListener(stepListener, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Toast.makeText(this, "Step counter sensor no disponible!", Toast.LENGTH_SHORT).show();
        }

        // Inicializar cliente de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Solicitar permisos
        requestLocationPermission();
        requestActivityRecognitionPermission();

        // Obtener la ubicación una vez que los permisos sean concedidos
        getLastKnownLocation();
    }

    private void startTimer(){
        runnable.run();
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                int minutes = secondsElapsed / 60;
                int seconds = secondsElapsed % 60;
                timeTextView.setText(String.format("Tiempo transcurrido: %02d:%02d", minutes, seconds));
                secondsElapsed++;
                handler.postDelayed(this, 1000);
            }
        }
    };
    
    // Solicitar permiso de ubicación
    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    // Solicitar permiso de reconocimiento de actividad
    private void requestActivityRecognitionPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACTIVITY_RECOGNITION},
                    ACTIVITY_RECOGNITION_REQUEST_CODE);
        }
    }

    // Obtener la ubicación
    private void getLastKnownLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                double latitude = location.getLatitude();
                                double longitude = location.getLongitude();
                                locationTextView.setText("Lat: " + latitude + ", Lon: " + longitude);
                            } else {
                                locationTextView.setText("Unable to get location.");
                            }
                        }
                    });
        }
    }

    // Manejar la respuesta de los permisos
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso de ubicación concedido, obtener la ubicación
                getLastKnownLocation();
            } else {
                Toast.makeText(this, "Location permission denied!", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == ACTIVITY_RECOGNITION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso de reconocimiento de actividad concedido
                Toast.makeText(this, "Activity recognition permission granted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Activity recognition permission denied!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Desregistrar listener del sensor para evitar fugas de memoria
        if (sensorManager != null && stepListener != null) {
            sensorManager.unregisterListener(stepListener);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Verifica si tienes permisos de ubicación
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);  // Activa la capa de ubicación en el mapa
        } else {
            // Si no tienes permisos, solicítalos
            requestLocationPermission();
        }
    }
}



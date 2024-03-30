package com.example.chatapp4;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class PreMainActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private FusedLocationProviderClient fusedLocationClient;
    private static final double KHANDAGIRI_LAT = 20.2569;
    private static final double KHANDAGIRI_LONG = 85.7792;
    private static final double KIITSQUARE_LAT = 20.3534;
    private static final double KIITSQUARE_LONG = 85.8268;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pre_main);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        Button navigateToMainButton = findViewById(R.id.navigate_to_main_button);
        Button sendSosButton = findViewById(R.id.send_sos_button);

        navigateToMainButton.setOnClickListener(v -> navigateToMainActivity());
        sendSosButton.setOnClickListener(v -> sendSOS());
    }

    private void navigateToMainActivity() {
        startActivity(new Intent(PreMainActivity.this, MainActivity.class));
        finish(); // Finish the current activity so the user cannot navigate back to it
    }

    private void sendSOS() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Confirm SOS");
            builder.setMessage("Are you sure you want to send an SOS message?");
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss(); // Dismiss the dialog
                    String userId = currentUser.getUid(); // Get the user ID
                    String username = currentUser.getDisplayName(); // Get the username
                    fetchUserLocationAndSendSOS(userId, username);
                }
            });
            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss(); // Dismiss the dialog
                    Toast.makeText(PreMainActivity.this, "SOS Cancelled", Toast.LENGTH_SHORT).show();
                }
            });
            builder.show(); // Show the confirmation dialog
        } else {
            // User is not authenticated, handle accordingly (e.g., redirect to login)
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("MissingPermission")
    private void fetchUserLocationAndSendSOS(String userId, String username) {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            double userLat = location.getLatitude();
                            double userLong = location.getLongitude();
                            String sosMessage = "SOS from User: " + username + "\nLive Location: https://maps.google.com/maps?q=" + userLat + "," + userLong;
                            sendSOSMessageToNearestDatabase(userLat, userLong, sosMessage);
                        } else {
                            Toast.makeText(PreMainActivity.this, "Unable to retrieve location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }


    private void sendSOSMessageToNearestDatabase(double userLat, double userLong, String sosMessage) {
        double distanceToKhandagiri = calculateDistance(userLat, userLong, KHANDAGIRI_LAT, KHANDAGIRI_LONG);
        double distanceToKiitsquare = calculateDistance(userLat, userLong, KIITSQUARE_LAT, KIITSQUARE_LONG);


//                                double distanceToKhandagiri = calculateDistance(userLat, userLong,  KIITSQUARE_LAT, KIITSQUARE_LONG);
//                                double distanceToKiitsquare = calculateDistance(userLat, userLong,KHANDAGIRI_LAT, KHANDAGIRI_LONG);


        DatabaseReference nearestDatabase;
        if (distanceToKhandagiri < distanceToKiitsquare) {
            nearestDatabase = mDatabase.child("khandagiri");
        } else {
            nearestDatabase = mDatabase.child("kiitsquare");
        }

        nearestDatabase.push().setValue(sosMessage)
                .addOnSuccessListener(aVoid -> Toast.makeText(PreMainActivity.this, "SOS Sent", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(PreMainActivity.this, "Error sending SOS", Toast.LENGTH_SHORT).show());
    }

    // Helper method to calculate distance (similar to previous code)
    private double calculateDistance(double userLat, double userLong, double targetLat, double targetLong) {
        double earthRadius = 6371.0; // Radius of the Earth in kilometers
        double latDistance = Math.toRadians(targetLat - userLat);
        double longDistance = Math.toRadians(targetLong - userLong);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(userLat)) * Math.cos(Math.toRadians(targetLat))
                * Math.sin(longDistance / 2) * Math.sin(longDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = earthRadius * c;
        return distance;
    }
}

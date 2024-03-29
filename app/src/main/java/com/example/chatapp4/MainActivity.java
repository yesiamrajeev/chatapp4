package com.example.chatapp4;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private DatabaseReference mKhandagiriDatabase;
    private DatabaseReference mKiitsquareDatabase;
    private EditText mEditText;
    private ListView mListView;
    private MyAdapter mAdapter;
    private List<String> mDataList = new ArrayList<>();
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private static final double KHANDAGIRI_LAT = 20.2569;
    private static final double KHANDAGIRI_LONG = 85.7792;
    private static final double KIITSQUARE_LAT = 20.3534;
    private static final double KIITSQUARE_LONG = 85.8268;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mKhandagiriDatabase = FirebaseDatabase.getInstance().getReference("khandagiri");
        mKiitsquareDatabase = FirebaseDatabase.getInstance().getReference("kiitsquare");

        mEditText = findViewById(R.id.edit_text);
        mListView = findViewById(R.id.list_view);

        mAdapter = new MyAdapter(this, mDataList);
        mListView.setAdapter(mAdapter);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        Button sendButton = findViewById(R.id.send_button);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkLocationPermission()) {
                    fetchUserLocationAndSendMessage();
                } else {
                    requestLocationPermission();
                }
            }
        });

        setupListView();
        fetchUsername();
    }

    @SuppressLint("MissingPermission")
    private void fetchUserLocationAndSendMessage() {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            double userLat = location.getLatitude();
                            double userLong = location.getLongitude();
                            sendMessage(userLat, userLong);
                        } else {
                            Toast.makeText(MainActivity.this, "Unable to retrieve location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void sendMessage(double userLat, double userLong) {
        String message = mEditText.getText().toString().trim();
        if (!TextUtils.isEmpty(message)) {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                String username = currentUser.getDisplayName();
//                mDataList.add(username + ": " + message);
                mAdapter.notifyDataSetChanged();
                mEditText.setText("");

                double distanceToKhandagiri = calculateDistance(userLat, userLong, KHANDAGIRI_LAT, KHANDAGIRI_LONG);
                double distanceToKiitsquare = calculateDistance(userLat, userLong, KIITSQUARE_LAT, KIITSQUARE_LONG);

                if (distanceToKhandagiri < distanceToKiitsquare) {
                    mKhandagiriDatabase.push().setValue(username + ": " + message);
                    Toast.makeText(MainActivity.this, "Message sent to Khandagiri", Toast.LENGTH_SHORT).show();
                } else {
                    mKiitsquareDatabase.push().setValue(username + ": " + message);
                    Toast.makeText(MainActivity.this, "Message sent to Kiitsquare", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchUserLocationAndSendMessage();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

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

    @SuppressLint("MissingPermission")
    private void setupListView() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userLocation = currentUser.getDisplayName();
            DatabaseReference khandagiriDatabase = mDatabase.child("khandagiri");
            DatabaseReference kiitsquareDatabase = mDatabase.child("kiitsquare");

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                double userLat = location.getLatitude();
                                double userLong = location.getLongitude();
                                double distanceToKhandagiri = calculateDistance(userLat, userLong, KHANDAGIRI_LAT, KHANDAGIRI_LONG);
                                double distanceToKiitsquare = calculateDistance(userLat, userLong, KIITSQUARE_LAT, KIITSQUARE_LONG);

                                DatabaseReference locationDatabase;
                                if (distanceToKhandagiri < distanceToKiitsquare) {
                                    locationDatabase = khandagiriDatabase;
                                } else {
                                    locationDatabase = kiitsquareDatabase;
                                }

                                locationDatabase.addChildEventListener(new ChildEventListener() {
                                    @Override
                                    public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                                        String message = snapshot.getValue(String.class);
                                        mDataList.add(message);
                                        mAdapter.notifyDataSetChanged();
                                        scrollToBottom();
                                    }

                                    @Override
                                    public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

                                    @Override
                                    public void onChildRemoved(@NonNull DataSnapshot snapshot) {}

                                    @Override
                                    public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {}
                                });
                            }
                        }
                    });
        }
    }


    private void fetchUsername() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String username = currentUser.getDisplayName();
            if (!TextUtils.isEmpty(username)) {
                mDataList.add(username);
                mAdapter.notifyDataSetChanged();
            }
        }
    }

    private void scrollToBottom() {
        mListView.post(new Runnable() {
            @Override
            public void run() {
                mListView.setSelection(mAdapter.getCount() - 1);
            }
        });
    }

    // Inner class for the custom adapter
    public class MyAdapter extends ArrayAdapter<String> {
        private Context mContext;
        private List<String> mData;

        public MyAdapter(Context context, List<String> data) {
            super(context, R.layout.list_item, data);
            mContext = context;
            mData = data;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View listItem = convertView;
            if (listItem == null) {
                listItem = LayoutInflater.from(mContext).inflate(R.layout.list_item, parent, false);
            }

            TextView itemText = listItem.findViewById(R.id.item_text);
            itemText.setText(mData.get(position));

            return listItem;
        }
    }
}

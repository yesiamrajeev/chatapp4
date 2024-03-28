package com.example.chatapp4;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private EditText mEditText;
    private ListView mListView;
    private MyAdapter mAdapter;
    private List<String> mDataList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        mEditText = findViewById(R.id.edit_text);
        mListView = findViewById(R.id.list_view);

        mAdapter = new MyAdapter(this, mDataList);
        mListView.setAdapter(mAdapter);

        Button sendButton = findViewById(R.id.send_button);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        setupListView();
        fetchUsername();
    }

    private void sendMessage() {
        String message = mEditText.getText().toString().trim();
        if (!TextUtils.isEmpty(message)) {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                String username = currentUser.getDisplayName();
                mDatabase.child("messages").push().setValue(username + ": " + message);
                mEditText.setText("");
            }
        }
    }

    private void setupListView() {
        mDatabase.child("messages").addChildEventListener(new ChildEventListener() {
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

    private void scrollToBottom() {
        mListView.post(new Runnable() {
            @Override
            public void run() {
                mListView.setSelection(mAdapter.getCount() - 1);
            }
        });
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
}

package com.example.lifeguard;

import android.database.Cursor;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    RecyclerView recycler;
    HistoryAdapter adapter;
    List<History> list;
    DatabaseHelper dbHelper;
    DatabaseReference firebaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        recycler = findViewById(R.id.recyclerHistory);
        dbHelper = new DatabaseHelper(this);
        firebaseRef = FirebaseDatabase.getInstance()
                .getReference("EmergencyHistory");

        list = new ArrayList<>();
        load();

        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(new HistoryAdapter(list));
    }

    private void load() {
        Cursor c = dbHelper.getAllHistory();
        while (c.moveToNext()) {
            History h = new History(
                    c.getInt(0),
                    c.getString(1),
                    c.getString(2)
            );
            list.add(h);
            firebaseRef.child(String.valueOf(h.id)).setValue(h);
        }
        c.close();
    }
}

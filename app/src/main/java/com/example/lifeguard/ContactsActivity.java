package com.example.lifeguard;

import android.database.Cursor;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ContactsActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    DatabaseHelper dbHelper;
    ContactsAdapter adapter;
    List<Contact> contacts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        recyclerView = findViewById(R.id.recyclerViewContacts);
        dbHelper = new DatabaseHelper(this);
        contacts = new ArrayList<>();

        loadContacts();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ContactsAdapter(contacts);
        recyclerView.setAdapter(adapter);
    }

    private void loadContacts() {
        contacts.clear();
        Cursor cursor = dbHelper.getAllContacts();

        if (cursor != null) {
            while (cursor.moveToNext()) {
                contacts.add(new Contact(
                        cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACT_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACT_NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACT_PHONE))
                ));
            }
            cursor.close();
        }
    }
}

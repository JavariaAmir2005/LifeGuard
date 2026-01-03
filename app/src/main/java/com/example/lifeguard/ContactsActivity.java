package com.example.lifeguard;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class ContactsActivity extends AppCompatActivity {

    EditText etName, etPhone, etSearch;
    Button btnAdd, btnSync;
    RecyclerView recyclerView;

    ContactsAdapter adapter;
    List<Contact> contacts;

    DatabaseHelper dbHelper;
    DatabaseReference contactsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etSearch = findViewById(R.id.etSearch);
        btnAdd = findViewById(R.id.btnAdd);
        btnSync = findViewById(R.id.btnSync);
        recyclerView = findViewById(R.id.recyclerViewContacts);
        dbHelper = new DatabaseHelper(this);
        contactsRef = FirebaseDatabase.getInstance().getReference("EmergencyContacts");

        contacts = new ArrayList<>();
        loadContacts();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ContactsAdapter(this, contacts, contactsRef);
        recyclerView.setAdapter(adapter);

        btnAdd.setOnClickListener(v -> addContact());


        etSearch.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int st, int b, int c) {
                adapter.filter(s.toString());
            }
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void afterTextChanged(Editable e) {}
        });


        btnSync.setOnClickListener(v -> {
            for(Contact c : contacts){
                contactsRef.child(String.valueOf(c.getId())).setValue(c);
            }
            Toast.makeText(this, "Contacts Synced", Toast.LENGTH_SHORT).show();

            startActivity(new Intent(this, SyncedContactsActivity.class));
        });

    }

    private void addContact() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "Enter name & phone", Toast.LENGTH_SHORT).show();
            return;
        }

        long id = dbHelper.addContact(name, phone);
        Contact c = new Contact((int) id, name, phone);

        contacts.add(c);
        adapter.notifyItemInserted(contacts.size() - 1);

        contactsRef.child(String.valueOf(id)).setValue(c);

        etName.setText("");
        etPhone.setText("");
    }

    private void loadContacts() {
        Cursor cursor = dbHelper.getAllContacts();
        while (cursor.moveToNext()) {
            contacts.add(new Contact(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getString(2)
            ));
        }
        cursor.close();
    }

    // ✅ EDIT DIALOG
    public void showEditDialog(Contact contact) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Edit Contact");

        EditText name = new EditText(this);
        EditText phone = new EditText(this);
        name.setText(contact.getName());
        phone.setText(contact.getPhone());

        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.addView(name);
        l.addView(phone);
        b.setView(l);

        b.setPositiveButton("Update", (d, i) -> {
            dbHelper.updateContact(contact.getId(),
                    name.getText().toString(),
                    phone.getText().toString());

            contact.setName(name.getText().toString());
            contact.setPhone(phone.getText().toString());

            adapter.notifyDataSetChanged();
            contactsRef.child(String.valueOf(contact.getId())).setValue(contact);
        });

        b.setNegativeButton("Cancel", null);
        b.show();
    }
}

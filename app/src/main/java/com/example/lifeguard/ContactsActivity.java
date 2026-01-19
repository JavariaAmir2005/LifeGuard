package com.example.lifeguard;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class ContactsActivity extends BaseActivity {

    EditText etName, etPhone, etSearch;
    Button btnAdd, btnSync;
    RecyclerView recyclerView;
    BottomNavigationView bottomNavigation;

    ContactsAdapter adapter;
    List<Contact> contacts;

    DatabaseHelper dbHelper;
    DatabaseReference contactsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);
        setupNavBar(); // Nav bar setup

        // Initialize views
        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etSearch = findViewById(R.id.etSearch);
        btnAdd = findViewById(R.id.btnAdd);
        btnSync = findViewById(R.id.btnSync);
        recyclerView = findViewById(R.id.recyclerViewContacts);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        dbHelper = new DatabaseHelper(this);

        // Initialize RecyclerView
        contacts = new ArrayList<>();
        adapter = new ContactsAdapter(this, contacts, null);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Setup Firebase reference safely
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            contactsRef = FirebaseDatabase.getInstance()
                    .getReference("Users")
                    .child(user.getUid())
                    .child("EmergencyContacts");

            adapter.contactsRef = contactsRef;

            // Load contacts from Firebase
            contactsRef.addValueEventListener(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    contacts.clear();
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        Contact c = ds.getValue(Contact.class);
                        if (c != null) contacts.add(c);
                    }
                    adapter.updateFullList();
                    adapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(ContactsActivity.this, "Failed to load contacts", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "User not signed in. Firebase unavailable.", Toast.LENGTH_SHORT).show();
        }

        // Add contact button
        btnAdd.setOnClickListener(v -> addContact());

        // Search filter
        etSearch.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int st, int b, int c) {
                adapter.filter(s.toString());
            }
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void afterTextChanged(Editable e) {}
        });

        // Sync contacts safely
        btnSync.setOnClickListener(v -> {
            if (contactsRef != null) {
                for (Contact c : contacts) {
                    contactsRef.child(String.valueOf(c.getId())).setValue(c);
                }
                Toast.makeText(this, "Contacts Synced", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, SyncedContactsActivity.class));
            } else {
                Toast.makeText(this, "Cannot sync. User not signed in.", Toast.LENGTH_SHORT).show();
            }
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

        // Only update Firebase if available
        if (contactsRef != null) {
            contactsRef.child(String.valueOf(id)).setValue(c);
        }

        etName.setText("");
        etPhone.setText("");
    }

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

            if (contactsRef != null) {
                contactsRef.child(String.valueOf(contact.getId())).setValue(contact);
            }
        });
        b.setNegativeButton("Cancel", null);
        b.show();
    }
}

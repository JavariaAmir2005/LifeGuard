package com.example.lifeguard;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;

import java.util.ArrayList;
import java.util.List;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ViewHolder> {

    Context context;
    List<Contact> contactList;
    List<Contact> fullList;
    DatabaseHelper dbHelper;
    DatabaseReference contactsRef;

    public ContactsAdapter(Context context, List<Contact> list, DatabaseReference ref) {
        this.context = context;
        this.contactList = list;
        this.fullList = new ArrayList<>();
        this.contactsRef = ref;
        this.dbHelper = new DatabaseHelper(context);
    }

    // 🔥 MUST be called after Firebase reload
    public void updateFullList() {
        fullList.clear();
        fullList.addAll(contactList);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context)
                .inflate(R.layout.item_contact, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Contact c = contactList.get(position);

        h.tvName.setText(c.getName());
        h.tvPhone.setText(c.getPhone());

        h.itemView.setOnLongClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Contact Options")
                    .setItems(new String[]{"Edit", "Delete"}, (d, i) -> {
                        if (i == 0) {
                            ((ContactsActivity) context).showEditDialog(c);
                        } else {
                            dbHelper.deleteContact(c.getId());

                            if (contactsRef != null) {
                                contactsRef.child(String.valueOf(c.getId())).removeValue();
                            }

                            contactList.remove(position);
                            updateFullList();
                            notifyItemRemoved(position);
                        }
                    }).show();
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return contactList.size();
    }

    public void filter(String text) {
        contactList.clear();

        if (text.isEmpty()) {
            contactList.addAll(fullList);
        } else {
            text = text.toLowerCase();
            for (Contact c : fullList) {
                if (c.getName().toLowerCase().contains(text) ||
                        c.getPhone().contains(text)) {
                    contactList.add(c);
                }
            }
        }
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPhone;

        ViewHolder(View v) {
            super(v);
            tvName = v.findViewById(R.id.tvName);
            tvPhone = v.findViewById(R.id.tvPhone);
        }
    }
}

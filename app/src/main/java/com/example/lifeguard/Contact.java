package com.example.lifeguard;

public class Contact {
    private int id;
    private String name;
    private String phone;

    // No-argument constructor required by Firebase
    public Contact() {
    }

    // Constructor with arguments (optional, still useful)
    public Contact(int id, String name, String phone) {
        this.id = id;
        this.name = name;
        this.phone = phone;
    }

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getPhone() { return phone; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setPhone(String phone) { this.phone = phone; }
}

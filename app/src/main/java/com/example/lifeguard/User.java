package com.example.lifeguard;


public class User {
    private String name;
    private String email;
    private String phone;
    private String bloodGroup;
    private String allergies;
    private String medicines;
    private String medicalNotes;

    // No-arg constructor required for Firebase
    public User() {}

    public User(String name, String email, String phone, String bloodGroup,
                String allergies, String medicines, String medicalNotes) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.bloodGroup = bloodGroup;
        this.allergies = allergies;
        this.medicines = medicines;
        this.medicalNotes = medicalNotes;
    }

    // Getters & setters
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getBloodGroup() { return bloodGroup; }
    public String getAllergies() { return allergies; }
    public String getMedicines() { return medicines; }
    public String getMedicalNotes() { return medicalNotes; }

    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }
    public void setAllergies(String allergies) { this.allergies = allergies; }
    public void setMedicines(String medicines) { this.medicines = medicines; }
    public void setMedicalNotes(String medicalNotes) { this.medicalNotes = medicalNotes; }
}

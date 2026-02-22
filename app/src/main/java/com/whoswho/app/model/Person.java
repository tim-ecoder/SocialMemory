package com.whoswho.app.model;

public class Person {
    private long id;
    private String firstName;
    private String lastName;
    private String photoPath;
    private String company;
    private String position;
    private String context;
    private String note;
    private String hobbies;
    private String interests;
    private String familyStatus;
    private String partnerName;
    private String childrenNames;
    private String petNames;
    private String religion;
    private String politicalViews;
    private long createdAt;
    private long photoUpdatedAt;

    public Person() {
        this.createdAt = System.currentTimeMillis();
        this.photoUpdatedAt = System.currentTimeMillis();
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getFullName() {
        if (lastName != null && !lastName.isEmpty()) {
            return firstName + " " + lastName;
        }
        return firstName;
    }

    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public String getContext() { return context; }
    public void setContext(String context) { this.context = context; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getHobbies() { return hobbies; }
    public void setHobbies(String hobbies) { this.hobbies = hobbies; }

    public String getInterests() { return interests; }
    public void setInterests(String interests) { this.interests = interests; }

    public String getFamilyStatus() { return familyStatus; }
    public void setFamilyStatus(String familyStatus) { this.familyStatus = familyStatus; }

    public String getPartnerName() { return partnerName; }
    public void setPartnerName(String partnerName) { this.partnerName = partnerName; }

    public String getChildrenNames() { return childrenNames; }
    public void setChildrenNames(String childrenNames) { this.childrenNames = childrenNames; }

    public String getPetNames() { return petNames; }
    public void setPetNames(String petNames) { this.petNames = petNames; }

    public String getReligion() { return religion; }
    public void setReligion(String religion) { this.religion = religion; }

    public String getPoliticalViews() { return politicalViews; }
    public void setPoliticalViews(String politicalViews) { this.politicalViews = politicalViews; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getPhotoUpdatedAt() { return photoUpdatedAt; }
    public void setPhotoUpdatedAt(long photoUpdatedAt) { this.photoUpdatedAt = photoUpdatedAt; }

    /** Returns a description string for quiz (company + position + context) */
    public String getDescription() {
        StringBuilder sb = new StringBuilder();
        if (position != null && !position.isEmpty()) sb.append(position);
        if (company != null && !company.isEmpty()) {
            if (sb.length() > 0) sb.append(" @ ");
            sb.append(company);
        }
        if (context != null && !context.isEmpty()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(context);
        }
        return sb.toString();
    }
}

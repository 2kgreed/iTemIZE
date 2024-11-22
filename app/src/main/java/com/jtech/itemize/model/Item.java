package com.jtech.itemize.model;

public class Item {
    private String id;
    private String name;
    private boolean isSignedOut;
    private String signedOutBy; // Name of the person who signed out
    private String signedOutFingerprint; // Placeholder for fingerprint token
    private String signedOutAt; // Timestamp for sign-out
    private String returnedAt; // Timestamp for return

    public Item() {}

    public Item(String id, String name) {
        this.id = id;
        this.name = name;
        this.isSignedOut = false;
        this.signedOutBy = null;
        this.signedOutFingerprint = null;
        this.signedOutAt = null;
        this.returnedAt = null;
    }

    // Getters and setters
    public String getId() { return id; }
    public String getName() { return name; }
    public boolean isSignedOut() { return isSignedOut; }
    public String getSignedOutBy() { return signedOutBy; }
    public String getSignedOutFingerprint() { return signedOutFingerprint; }
    public String getSignedOutAt() { return signedOutAt; }
    public String getReturnedAt() { return returnedAt; }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setSignedOut(boolean signedOut) { isSignedOut = signedOut; }
    public void setSignedOutBy(String signedOutBy) { this.signedOutBy = signedOutBy; }
    public void setSignedOutFingerprint(String signedOutFingerprint) { this.signedOutFingerprint = signedOutFingerprint; }
    public void setSignedOutAt(String signedOutAt) { this.signedOutAt = signedOutAt; }
    public void setReturnedAt(String returnedAt) { this.returnedAt = returnedAt; }
}

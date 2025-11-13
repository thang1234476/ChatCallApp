package com.chatapp.client.model;

public class Contact {
    private String username;
    private String displayName;
    public Contact() {}
    public Contact(String u, String d) { username = u; displayName = d; }
    public String getUsername() { return username; }
}

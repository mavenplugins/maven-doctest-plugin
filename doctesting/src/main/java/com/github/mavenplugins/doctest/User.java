package com.github.mavenplugins.doctest;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class User {
    
    protected String firstName;
    protected String lastName;
    protected Date birthday = new Date();
    protected Address address;
    protected List<Friend> friends = new ArrayList<Friend>();
    
    public User() {
    }
    
    public User(User other) {
        firstName = other.firstName;
        lastName = other.lastName;
        birthday = other.birthday;
        address = other.address;
        friends.addAll(other.friends);
    }
    
    public List<Friend> getFriends() {
        return friends;
    }
    
    public void setFriends(List<Friend> friends) {
        this.friends = friends;
    }
    
    public Date getBirthday() {
        return birthday;
    }
    
    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }
    
    public Address getAddress() {
        return address;
    }
    
    public void setAddress(Address address) {
        this.address = address;
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
}

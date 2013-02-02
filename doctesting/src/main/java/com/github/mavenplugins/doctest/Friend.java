package com.github.mavenplugins.doctest;

import java.util.Date;

public class Friend extends User {
    
    protected Date friendshipSince = new Date();
    
    public Friend() {
    }
    
    public Friend(User user) {
        super(user);
    }
    
    public Date getFriendshipSince() {
        return friendshipSince;
    }
    
    public void setFriendshipSince(Date friendshipSince) {
        this.friendshipSince = friendshipSince;
    }
    
}

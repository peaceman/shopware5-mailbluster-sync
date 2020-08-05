package com.n2305.swmb.mailbluster;

import org.springframework.util.Assert;

public class MBLead {
    private final String firstName;
    private final String lastName;
    private final String email;
    private final boolean subscribed;

    public MBLead(String firstName, String lastName, String email, boolean subscribed) {
        Assert.notNull(firstName, "Got empty firstName");
        Assert.notNull(lastName, "Got empty lastName");
        Assert.notNull(email, "Got empty email");

        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.subscribed = subscribed;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public boolean isSubscribed() {
        return subscribed;
    }
}

package com.n2305.swmb.mailbluster;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.util.Assert;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MBLead {
    private final String firstName;
    private final String lastName;
    private final String email;
    private final boolean subscribed;

    public MBLead(String firstName, String lastName, String email, boolean subscribed) {
        Assert.notNull(email, "Got empty email");

        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email.toLowerCase();
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

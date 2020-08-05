package com.n2305.swmb.shopware;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.time.OffsetDateTime;
import java.util.Objects;

@JsonDeserialize(builder = CustomerListItem.Builder.class)
public class CustomerListItem {
    private final int id;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final boolean newsletter;

    private OffsetDateTime fetchTime;

    private CustomerListItem(
        int id,
        String firstName,
        String lastName,
        String email,
        boolean newsletter
    ) {
        Assert.notNull(firstName, "Got empty firstName");
        Assert.notNull(firstName, "Got empty lastName");
        Assert.notNull(firstName, "Got empty email");

        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.newsletter = newsletter;
    }

    @JsonPOJOBuilder
    static class Builder {
        private static final Logger logger = LoggerFactory.getLogger(Builder.class);

        int id;
        String firstName;
        String lastName;
        String email;
        boolean newsletter;

        Builder withId(int id) {
            this.id = id;
            return this;
        }

        @JsonProperty("firstname")
        Builder withFirstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        @JsonProperty("lastname")
        Builder withLastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        Builder withEmail(String email) {
            this.email = email;
            return this;
        }

        Builder withNewsletter(boolean newsletter) {
            this.newsletter = newsletter;
            return this;
        }

        public CustomerListItem build() {
            return new CustomerListItem(
                id,
                firstName,
                lastName,
                email,
                newsletter
            );
        }
    }

    public int getId() {
        return id;
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

    public boolean isNewsletter() {
        return newsletter;
    }

    public OffsetDateTime getFetchTime() {
        return fetchTime;
    }

    public CustomerListItem setFetchTime(OffsetDateTime fetchTime) {
        this.fetchTime = fetchTime;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomerListItem that = (CustomerListItem) o;
        return id == that.id &&
            newsletter == that.newsletter &&
            firstName.equals(that.firstName) &&
            lastName.equals(that.lastName) &&
            email.equals(that.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, firstName, lastName, email, newsletter);
    }
}

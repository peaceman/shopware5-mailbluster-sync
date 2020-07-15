package com.n2305.swmb.mailbluster;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.apache.commons.text.WordUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@JsonInclude(Include.NON_NULL)
public class MBOrder {
    private final String id;
    private final Customer customer;
    private final Integer campaignId;
    private final String currency;
    private final double totalPrice;
    private final List<Product> items;

    private MBOrder(
        String id,
        Customer customer,
        Integer campaignId,
        String currency,
        double totalPrice,
        List<Product> items
    ) {
        this.id = id;
        this.customer = customer;
        this.campaignId = campaignId;
        this.currency = currency;
        this.totalPrice = totalPrice;
        this.items = items;
    }

    public static class Builder {
        String id;
        Customer customer;
        Integer campaignId;
        String currency;
        double totalPrice;
        List<Product> items;

        public Builder withId(String id) {
            this.id = id;
            return this;
        }

        public Builder withCustomer(Customer customer) {
            this.customer = customer;
            return this;
        }

        public Builder withCampaignId(Integer campaignId) {
            this.campaignId = campaignId;
            return this;
        }

        public Builder withCurrency(String currency) {
            this.currency = currency;
            return this;
        }

        public Builder withTotalPrice(double totalPrice) {
            this.totalPrice = totalPrice;
            return this;
        }

        public Builder withItems(List<Product> items) {
            this.items = items;
            return this;
        }

        public MBOrder build() {
            return new MBOrder(
                id,
                customer,
                campaignId,
                currency,
                totalPrice,
                items
            );
        }
    }

    public String getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public Integer getCampaignId() {
        return campaignId;
    }

    public String getCurrency() {
        return currency;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public List<Product> getItems() {
        return items;
    }

    @JsonInclude(Include.NON_NULL)
    public static class Customer {
        private final String firstName;
        private final String lastName;
        private final String email;
        private final String timezone;
        private final String ipAddress;
        private final Boolean subscribed;
        private final Map<String, String> meta;
        private final List<String> tags;

        private Customer(
            String firstName,
            String lastName,
            String email,
            String timezone,
            String ipAddress,
            Boolean subscribed,
            Map<String, String> meta,
            List<String> tags
        ) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
            this.timezone = timezone;
            this.ipAddress = ipAddress;
            this.subscribed = subscribed;
            this.meta = meta;
            this.tags = tags;
        }

        public static class Builder {
            String firstName;
            String lastName;
            String email;
            String timezone;
            String ipAddress;
            Boolean subscribed;
            Map<String, String> meta;
            List<String> tags;

            public Builder withFirstName(String firstName) {
                this.firstName = Optional.ofNullable(firstName)
                    .map(WordUtils::capitalizeFully)
                    .orElse(null);

                return this;
            }

            public Builder withLastName(String lastName) {
                this.lastName = Optional.ofNullable(lastName)
                    .map(WordUtils::capitalizeFully)
                    .orElse(null);

                return this;
            }

            public Builder withEmail(String email) {
                this.email = Optional.ofNullable(email)
                    .map(String::toLowerCase)
                    .orElse(null);

                return this;
            }

            public Builder withTimezone(String timezone) {
                this.timezone = timezone;
                return this;
            }

            public Builder withIpAddress(String ipAddress) {
                this.ipAddress = ipAddress;
                return this;
            }

            public Builder withSubscribed(Boolean subscribed) {
                this.subscribed = subscribed;
                return this;
            }

            public Builder withMeta(Map<String, String> meta) {
                this.meta = meta;
                return this;
            }

            public Builder withTags(List<String> tags) {
                this.tags = tags;
                return this;
            }

            public Customer build() {
                return new Customer(
                    firstName,
                    lastName,
                    email,
                    timezone,
                    ipAddress,
                    subscribed,
                    meta,
                    tags
                );
            }
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

        public String getTimezone() {
            return timezone;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public Boolean getSubscribed() {
            return subscribed;
        }

        public Map<String, String> getMeta() {
            return meta;
        }

        public List<String> getTags() {
            return tags;
        }
    }

    public static class Product {
        private final String id;
        private final String name;
        private final double price;
        private final int quantity;

        public Product(String id, String name, double price, int quantity) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.quantity = Math.max(1, quantity);
        }

        public static Product forShipping(double price) {
            return new Product(
                "shipping",
                "Versand",
                price,
                1
            );
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public double getPrice() {
            return price;
        }

        public int getQuantity() {
            return quantity;
        }
    }
}

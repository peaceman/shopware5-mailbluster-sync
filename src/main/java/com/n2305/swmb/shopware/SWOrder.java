package com.n2305.swmb.shopware;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@JsonDeserialize(builder = SWOrder.Builder.class)
public class SWOrder {
    private final int id;
    private final String number;
    private final Customer customer;
    private final String partnerID;
    private final String referer;
    private final String currency;
    private final String remoteAddress;
    private final double invoiceAmount;
    private final double invoiceShipping;
    private final Date orderTime;
    private final List<Article> details;
    private final Address billing;
    private final Address shipping;

    private OffsetDateTime fetchTime;
    private OffsetDateTime listFetchTime;

    private SWOrder(
        int id,
        String number,
        Customer customer,
        String partnerID,
        String referer,
        String currency,
        String remoteAddress,
        double invoiceAmount,
        double invoiceShipping,
        Date orderTime,
        List<Article> details,
        Address billing,
        Address shipping
    ) {
        this.id = id;
        this.number = number;
        this.customer = customer;
        this.partnerID = partnerID;
        this.referer = referer;
        this.currency = currency;
        this.remoteAddress = remoteAddress;
        this.invoiceAmount = invoiceAmount;
        this.invoiceShipping = invoiceShipping;
        this.orderTime = orderTime;
        this.details = details;
        this.billing = billing;
        this.shipping = shipping;
    }

    public SWOrder setFetchTime(OffsetDateTime fetchTime) {
        this.fetchTime = fetchTime;

        return this;
    }

    public SWOrder setListFetchTime(OffsetDateTime fetchTime) {
        this.listFetchTime = fetchTime;

        return this;
    }

    public OffsetDateTime getFetchTime() {
        return fetchTime;
    }

    public OffsetDateTime getListFetchTime() {
        return listFetchTime;
    }

    @JsonPOJOBuilder
    static class Builder {
        private static final Logger logger = LoggerFactory.getLogger(Builder.class);

        int id;
        String number;
        Customer customer;
        String partnerID;
        String referer;
        String currency;
        String remoteAddress;
        double invoiceAmount;
        double invoiceShipping;
        Date orderTime;
        List<Article> details;
        Address billing;
        Address shipping;

        Builder withId(int id) {
            this.id = id;
            return this;
        }

        Builder withNumber(String number) {
            this.number = number;
            return this;
        }

        Builder withCustomer(Customer customer) {
            this.customer = customer;
            return this;
        }

        Builder withPartnerId(String partnerID) {
            this.partnerID = partnerID;
            return this;
        }

        Builder withReferer(String referer) {
            this.referer = referer;
            return this;
        }

        Builder withCurrency(String currency) {
            this.currency = currency;
            return this;
        }

        Builder withRemoteAddress(String remoteAddress) {
            Optional.ofNullable(remoteAddress)
                .map(s -> s.replaceAll("(?<=::):$", ""))
                .map(s -> {
                    try {
                        return InetAddress.getByName(s);
                    } catch (UnknownHostException e) {
                        logger.info("Failed to sanitize remote address: {}", s);

                        return null;
                    }
                })
                .map(InetAddress::getHostAddress)
                .ifPresent(s -> this.remoteAddress = s);

            return this;
        }

        Builder withInvoiceAmount(double invoiceAmount) {
            this.invoiceAmount = invoiceAmount;
            return this;
        }

        Builder withInvoiceShipping(double invoiceShipping) {
            this.invoiceShipping = invoiceShipping;
            return this;
        }

        Builder withOrderTime(Date orderTime) {
            this.orderTime = orderTime;
            return this;
        }

        Builder withDetails(List<Article> details) {
            this.details = details;
            return this;
        }

        Builder withBilling(Address billing) {
            this.billing = billing;
            return this;
        }

        Builder withShipping(Address shipping) {
            this.shipping = shipping;
            return this;
        }

        public SWOrder build() {
            return new SWOrder(
                id,
                number,
                customer,
                partnerID,
                referer,
                currency,
                remoteAddress,
                invoiceAmount,
                invoiceShipping,
                orderTime,
                details,
                billing,
                shipping
            );
        }
    }

    public int getId() {
        return id;
    }

    public String getNumber() {
        return number;
    }

    public Customer getCustomer() {
        return customer;
    }

    public String getPartnerID() {
        return partnerID;
    }

    public String getReferer() {
        return referer;
    }

    public String getCurrency() {
        return currency;
    }

    public double getInvoiceAmount() {
        return invoiceAmount;
    }
    
    public double getInvoiceShipping() {
        return invoiceShipping;
    }

    public Date getOrderTime() {
        return orderTime;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public List<Article> getDetails() {
        return details;
    }

    public Address getBilling() {
        return billing;
    }

    public Address getShipping() {
        return shipping;
    }

    public boolean hasInvoiceShipping() {
        return invoiceShipping > 0;
    }

    public static class Customer {
        private final int id;
        private final String number;
        private final String email;
        private final String firstname;
        private final String lastname;

        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public Customer(
            @JsonProperty("id") int id,
            @JsonProperty("number") String number,
            @JsonProperty("email") String email,
            @JsonProperty("firstname") String firstname,
            @JsonProperty("lastname") String lastname
        ) {
            this.id = id;
            this.number = number;
            this.email = email;
            this.firstname = firstname;
            this.lastname = lastname;
        }

        public int getId() {
            return id;
        }

        public String getNumber() {
            return number;
        }

        public String getEmail() {
            return email;
        }

        public String getFirstname() {
            return firstname;
        }

        public String getLastname() {
            return lastname;
        }
    }

    public static class Article {
        private final String articleNumber;
        private final String articleName;
        private final double price;
        private final int quantity;

        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public Article(
            @JsonProperty("articleNumber") String articleNumber,
            @JsonProperty("articleName") String articleName,
            @JsonProperty("price") double price,
            @JsonProperty("quantity") int quantity
        ) {
            this.articleNumber = articleNumber;
            this.articleName = articleName;
            this.price = price;
            this.quantity = quantity;
        }

        public String getArticleNumber() {
            return articleNumber;
        }

        public String getArticleName() {
            return articleName;
        }

        public double getPrice() {
            return price;
        }

        public int getQuantity() {
            return quantity;
        }

        public boolean hasPositivePrice() {
            return this.price > 0;
        }
    }

    public static class Address {
        private final String zipCode;
        private final String city;
        private final Country country;

        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public Address(
            @JsonProperty("zipCode") String zipCode,
            @JsonProperty("city") String city,
            @JsonProperty("country") Country country
        ) {
            this.zipCode = zipCode;
            this.city = city;
            this.country = country;
        }

        public String getZipCode() {
            return zipCode;
        }

        public String getCity() {
            return city;
        }

        public Country getCountry() {
            return country;
        }

        public static class Country {
            private final String iso;

            @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
            public Country(
                @JsonProperty("iso") String iso
            ) {
                this.iso = iso;
            }

            public String getIso() {
                return iso;
            }
        }
    }
}

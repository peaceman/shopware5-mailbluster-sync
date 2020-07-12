package com.n2305.swmb.shopware;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.Objects;

public class OrderListItem {
    private final int id;
    private final String number;
    private final OffsetDateTime orderTime;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public OrderListItem(
        @JsonProperty("id") int id,
        @JsonProperty("number") String number,
        @JsonProperty("orderTime") OffsetDateTime orderTime
    ) {
        this.id = id;
        this.number = number;
        this.orderTime = orderTime;
    }

    public int getId() {
        return id;
    }

    public String getNumber() {
        return number;
    }

    public OffsetDateTime getOrderTime() {
        return orderTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderListItem that = (OrderListItem) o;
        return id == that.id &&
            Objects.equals(number, that.number);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, number);
    }
}

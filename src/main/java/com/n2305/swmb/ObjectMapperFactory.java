package com.n2305.swmb;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.n2305.swmb.utils.ISO8601OffsetDateTimeDeserializer;

import java.time.OffsetDateTime;
import java.util.function.Supplier;

public class ObjectMapperFactory implements Supplier<ObjectMapper> {
    @Override
    public ObjectMapper get() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule()
            .addDeserializer(OffsetDateTime.class, new ISO8601OffsetDateTimeDeserializer()))
            .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);

        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        return mapper;
    }
}

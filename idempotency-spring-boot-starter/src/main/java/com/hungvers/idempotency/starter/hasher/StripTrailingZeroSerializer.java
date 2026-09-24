package com.hungvers.idempotency.starter.hasher;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.math.BigDecimal;

public class StripTrailingZeroSerializer extends ValueSerializer<BigDecimal> {
    @Override
    public void serialize(BigDecimal value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
        gen.writeString(value == null ? null : value.stripTrailingZeros().toPlainString());
    }
}

package com.am.mcp.jackson;

import com.am.portfolio.client.invoker.ApiClient;
import com.am.portfolio.client.model.EquityBrokerHolding;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.time.OffsetDateTime;

public final class PortfolioObjectMappers {

    private PortfolioObjectMappers() {
    }

    /** Default OpenAPI client mapper plus lenient date/enum parsing for live portfolio JSON. */
    public static ObjectMapper create() {
        ObjectMapper mapper = ApiClient.createDefaultObjectMapper();
        mapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
        SimpleModule module = new SimpleModule("am-mcp-lenient-portfolio");
        module.addDeserializer(OffsetDateTime.class, new LenientOffsetDateTimeDeserializer());
        module.addDeserializer(
                EquityBrokerHolding.BrokerTypeEnum.class,
                new JsonDeserializer<EquityBrokerHolding.BrokerTypeEnum>() {
                    @Override
                    public EquityBrokerHolding.BrokerTypeEnum deserialize(
                            JsonParser p, DeserializationContext ctxt) throws IOException {
                        String text = p.getValueAsString();
                        if (text == null || text.isBlank()) {
                            return null;
                        }
                        try {
                            return EquityBrokerHolding.BrokerTypeEnum.fromValue(text);
                        } catch (IllegalArgumentException e) {
                            return null;
                        }
                    }
                });
        mapper.registerModule(module);
        return mapper;
    }
}

package de.hf.myfinance.marketdata.persistence.entities;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

@Configuration
public class ReactiveMongoConfig {

    @Bean
    public MappingMongoConverter mappingMongoConverter(
            ReactiveMongoDatabaseFactory factory,
            MongoMappingContext context,
            MongoCustomConversions conversions) {

        // Create a ReactiveMongoTemplate to get the converter
        ReactiveMongoTemplate template = new ReactiveMongoTemplate(factory);
        MappingMongoConverter converter = (MappingMongoConverter) template.getConverter();
        
        // Set custom conversions
        converter.setCustomConversions(conversions);

        // ✅ Replace dots in map keys (e.g., "MUV2.DEX" -> "MUV2__dot__DEX")
        converter.setMapKeyDotReplacement("__dot__");

        return converter;
    }
}

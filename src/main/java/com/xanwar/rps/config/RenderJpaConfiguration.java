package com.xanwar.rps.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnExpression("'${SPRING_DATASOURCE_URL:}'.isBlank() && !'${DATABASE_URL:}'.isBlank()")
public class RenderJpaConfiguration {

    @Bean
    public HibernatePropertiesCustomizer postgresDialectCustomizer() {
        return props -> props.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
    }
}

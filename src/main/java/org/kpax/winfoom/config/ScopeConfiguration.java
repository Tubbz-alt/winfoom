package org.kpax.winfoom.config;

import org.springframework.beans.factory.config.CustomScopeConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class ScopeConfiguration {

    private static final Map<String, Object> scopes = new LinkedHashMap<>(1);

    @Bean
    public static CustomScopeConfigurer customScopeConfigurer() {
        CustomScopeConfigurer configurer = new CustomScopeConfigurer();
        configurer.setScopes(scopes);
        configurer.addScope(ProxySessionScope.NAME, new ProxySessionScope());
        return configurer;
    }

    @Lazy
    @Bean
    public ProxySessionScope proxySessionScope () {
        return (ProxySessionScope)scopes.get(ProxySessionScope.NAME);
    }


}

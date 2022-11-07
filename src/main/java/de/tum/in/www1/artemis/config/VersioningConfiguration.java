package de.tum.in.www1.artemis.config;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.resource.ResourceUrlProvider;

import de.tum.in.www1.artemis.versioning.VersionRequestMappingHandlerMapping;

/**
 * Configures the available API versions and sets up their request mapping
 */
@Configuration
public class VersioningConfiguration extends DelegatingWebMvcConfiguration {

    /**
     * List of existing versions. Extend this if new version is created.
     */
    public final static List<Integer> API_VERSIONS = List.of(1);

    @Bean
    public List<Integer> apiVersions() {
        return API_VERSIONS;
    }

    /**
     * Registers the versioned request mapping handler mapping {@link VersionRequestMappingHandlerMapping}
     * @param contentNegotiationManager the content negotiation manager
     * @param conversionService the conversion service
     * @param resourceUrlProvider the resource url provider
     * @return the versioned request mapping handler mapping {@link VersionRequestMappingHandlerMapping}
     */
    @Bean
    @Override
    public @NotNull RequestMappingHandlerMapping requestMappingHandlerMapping(
            @Qualifier("mvcContentNegotiationManager") @NotNull ContentNegotiationManager contentNegotiationManager,
            @Qualifier("mvcConversionService") @NotNull FormattingConversionService conversionService,
            @Qualifier("mvcResourceUrlProvider") @NotNull ResourceUrlProvider resourceUrlProvider) {
        return new VersionRequestMappingHandlerMapping("api", "v");
    }
}

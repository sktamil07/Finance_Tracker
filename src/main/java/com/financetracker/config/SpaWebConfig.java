package com.financetracker.config;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Serves the Expo web bundle that the Docker build copies into {@code classpath:/static/}, so the
 * UI and the API ship as one deployable on one origin (which is also why the app needs no CORS).
 *
 * <p>The bundle is a single-page app: only {@code index.html} exists on disk, and paths like
 * {@code /dashboard} are routed in the browser. A plain resource handler would 404 those on a hard
 * refresh, so anything that is not a real file falls back to {@code index.html}.
 *
 * <p>API, OpenAPI and Swagger paths are explicitly excluded — they must keep returning their own
 * responses (including 401/404 as JSON) rather than being swallowed by the SPA fallback. Requests
 * handled by a {@code @Controller} never reach this resolver anyway, since resource handlers are
 * registered at the lowest precedence; the exclusion is belt-and-braces for unmapped subpaths.
 */
@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    private static final String STATIC_ROOT = "classpath:/static/";
    private static final String INDEX = "/static/index.html";

    private static final String[] NON_SPA_PREFIXES = {"api/", "v3/", "swagger-ui", "actuator/"};

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations(STATIC_ROOT)
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requested = location.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        for (String prefix : NON_SPA_PREFIXES) {
                            if (resourcePath.startsWith(prefix)) {
                                return null;
                            }
                        }
                        // No index.html means the web bundle was not built into this JAR (a plain
                        // `mvn package` does that). Return null so the API still behaves normally.
                        Resource index = new ClassPathResource(INDEX);
                        return index.exists() ? index : null;
                    }
                });
    }
}

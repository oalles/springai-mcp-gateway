package es.omarall.gateway.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import lombok.extern.slf4j.Slf4j;

/**
 * Main entry point for the MCP Gateway Spring Boot application.
 */
@Slf4j
@SpringBootApplication(
        exclude = {org.springframework.ai.mcp.client.autoconfigure.McpToolCallbackAutoConfiguration.class}
)
@EnableConfigurationProperties({McpGatewayProperties.class})
public class Application {

    /**
     * Starts the Spring Boot application.
     * @param args command line arguments
     */
    public static void main(String[] args) {
        log.info("Starting MCP Gateway Application");
        SpringApplication.run(Application.class, args);
    }

}

package es.omarall.mcp.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(McpGatewayProperties.class)
public class McpGatewayApplication {
    static void main(String[] args) {
        SpringApplication.run(McpGatewayApplication.class, args);
    }
}

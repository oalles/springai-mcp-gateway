package es.omarall.gateway.mcp;

import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpSyncClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.AsyncMcpToolCallbackProvider;
import org.springframework.ai.mcp.SyncMcpToolCallback;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Spring Boot configuration for registering ToolCallback providers for the gateway.
 */
@Slf4j
@SpringBootConfiguration
public class GatewayProvidersConfig {

    /**
     * Registers a ToolCallbackProvider bean that aggregates both synchronous and asynchronous MCP clients.
     * @param mcpClients list of synchronous MCP clients
     * @param mcpAsyncClients list of asynchronous MCP clients
     * @param props gateway properties
     * @return ToolCallbackProvider instance
     */
    @Bean
    @Primary
    public ToolCallbackProvider tcProvider(
            List<McpSyncClient> mcpClients,
            List<McpAsyncClient> mcpAsyncClients,
            McpGatewayProperties props) {

        log.info("Registering ToolCallbackProvider with {} sync and {} async clients", mcpClients.size(), mcpAsyncClients.size());
        final List<ToolCallback> tcs = new ArrayList<>(mcpClients.stream()
                .flatMap(mcpClient -> mcpClient.listTools()
                        .tools()
                        .stream()
                        .map(tool -> new SyncMcpToolCallback(mcpClient, tool)))
                .map(tc -> new PrefixedToolCallback(tc, props))
                .toList());

        ToolCallbackProvider asyncToolCallbackProvider = new AsyncMcpToolCallbackProvider(mcpAsyncClients);
        Arrays.stream(asyncToolCallbackProvider.getToolCallbacks()).forEach(tc -> {
            ToolCallback t = new PrefixedToolCallback(tc, props);
            tcs.add(t);
        });

        log.debug("Total ToolCallbacks registered: {}", tcs.size());
        return ToolCallbackProvider.from(tcs);
    }
}

package es.omarall.gateway.mcp;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the MCP Gateway.
 * <p>
 * Controls prefixing, delimiter, static prefix, and timeout settings for tool offloading.
 */
@Data
@ConfigurationProperties(prefix = "mcp.gateway")
public class McpGatewayProperties {

    /**
     * Prefix mode for tool names: NONE, STATIC, or ALIAS.
     */
    private PrefixMode prefixMode = PrefixMode.NONE; // NONE|STATIC|ALIAS
    /**
     * Delimiter used between prefix and tool name.
     */
    private String delimiter = "_";
    /**
     * Static prefix to use when prefixMode is STATIC.
     */
    private String staticPrefix = "gw";
    /**
     * Timeout in seconds for offloading calls.
     */
    private int timeoutSeconds = 30; // timeout por llamada para offloading

    /**
     * Enum for prefixing mode.
     */
    public enum PrefixMode {NONE, STATIC, ALIAS}

}

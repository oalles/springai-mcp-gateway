package es.omarall.gateway.mcp;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.Getter;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * ToolDefinition implementation that applies a prefix to the tool name.
 */
@Getter
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class PrefixedToolDefinition implements ToolDefinition {
    private final String name;
    private final String description;
    private final String inputSchema;

    /**
     * Constructs a new PrefixedToolDefinition.
     * @param name prefixed tool name
     * @param description tool description
     * @param inputSchema tool input schema
     */
    public PrefixedToolDefinition(String name, String description, String inputSchema) {
        this.name = name;
        this.description = description;
        this.inputSchema = inputSchema;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public String inputSchema() {
        return inputSchema;
    }
}
package es.omarall.gateway.mcp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.lang.Nullable;

/**
 * ToolCallback wrapper that applies a prefix to tool names to avoid collisions.
 */
@Slf4j
@RequiredArgsConstructor
public class PrefixedToolCallback implements ToolCallback {

    private final ToolCallback delegate;
    private final McpGatewayProperties props;

    @Override
    public ToolDefinition getToolDefinition() {
        ToolDefinition td = delegate.getToolDefinition();
        String mappedName = mapName(props, "gw", td.name());
        log.trace("Mapping tool name '{}' to '{}'", td.name(), mappedName);
        return new PrefixedToolDefinition(
                mappedName,
                td.description(),
                td.inputSchema()
        );
    }

    @Override
    public ToolMetadata getToolMetadata() {
        // Return tool metadata as is
        return delegate.getToolMetadata();
    }

    @Override
    public String call(String toolInput) {
        log.debug("Calling tool '{}' with input: {}", getToolDefinition().name(), toolInput);
        return delegate.call(toolInput);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        return call(toolInput);
    }

    /**
     * Maps the tool name according to the configured prefix mode.
     * @param props gateway properties
     * @param alias alias to use if ALIAS mode
     * @param toolName original tool name
     * @return mapped tool name
     */
    private String mapName(McpGatewayProperties props, String alias, String toolName) {
        return switch (props.getPrefixMode()) {
            case NONE -> toolName;
            case STATIC -> props.getStaticPrefix() + props.getDelimiter() + toolName;
            case ALIAS -> alias + props.getDelimiter() + toolName;
        };
    }
}

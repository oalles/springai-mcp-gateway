package es.omarall.gateway.mcp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * REST controller exposing the catalog of available ToolCallbacks.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class CatalogHttpController {

    private final ToolCallbackProvider provider;

    /**
     * Returns the list of available ToolCallbacks as JSON.
     * @return list of ToolCallbacks
     */
    @GetMapping(value = "/mcp/gateway/catalog", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ToolCallback> catalog() {
        log.debug("Fetching tool callback catalog");
        return Arrays.stream(provider.getToolCallbacks()).toList();
    }
}

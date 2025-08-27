# Building a Spring AI MCP Gateway

The goal of this project is educational: to simply show how to configure and connect MCP clients and servers using Spring AI. 
The creation of an MCP gateway is presented here as an interesting option to learn and experiment with these configurations, allowing you to see in practice how multiple tools can be orchestrated and exposed under a single interface.

GitHub Copilot and other AI assistants are increasingly capable of using external tools to enhance their answers. 
The Model Context Protocol (MCP) is making that possible. 
MCP is an open protocol that standardizes how AI applications connect to external data and services providing a universal way to plug in different tools.

In this post, we’ll walk through how to build a **Spring Boot–based MCP Gateway** that aggregates multiple AI tools under one roof 
and exposes them to clients (like GitHub Copilot) via a single endpoint. 

## What is MCP and Why a Gateway?
**MCP (Model Context Protocol)** is an open standard (backed by the AI community and organizations like Anthropic) for connecting AI assistants to external tools and data sources. Essentially, 
MCP defines a common language (using JSON-RPC over various transports) for AI clients (like an assistant or IDE plugin) to discover and invoke tools hosted by an MCP server. 
A **tool** in MCP is any operation or resource an `LLM` can call – for example, searching the web, querying a database, reading a file, or calling an external API.

In a typical setup, an **MCP server** offers a collection of tools, and an **MCP client** connects to that server to use those tools. 
The server handles tool discovery (listing what’s available) and executes the tool calls, while the client sends requests and receives results. 

But what if you have **multiple tool servers or processes you want to use together**? You could have one server for a memory store, another for web search, another for custom internal APIs, etc. 
Connecting each one separately to the AI (and managing multiple endpoints) can become cumbersome – not to mention the **potential for name collisions**  if two tools share the same name.

That’s where an **MCP gateway** comes in. This Spring-based MCP Gateway will act as a single MCP server that aggregates tools from 
multiple backend MCP servers (or processes) and presents them through one unified interface. It’s similar to an API gateway or service mesh but for AI tools: 
the gateway is the one place the AI client connects to, and under the hood it **routes tool requests** to the appropriate backend.

An MCP Gateway offers a few benefits:

* **Consolidation**: The AI (e.g. Copilot) only needs to connect to one endpoint (the gateway) to access a whole catalog of tools.
* **Name-spacing**: The gateway can apply prefixes or aliases to tool names to avoid collisions and group tools logically (e.g. having search_web and search_docs instead of two different “search” tools).
*  **Centralized config**: You can manage connections to various tool servers in one Spring Boot config, and apply common policies (like timeouts or logging) in one place.
* **Extensibility**: You can plug in new tools by just adding a new backend server or process, without changing the client setup.

However, if you’re looking at enterprise or production use-cases with multiple teams or stringent security requirements, 
you might consider the [Docker MCP Gateway](docker-mcp-gateway.md) offering instead. 

## Spring AI MCP: Servers and Clients Made Easy

Spring AI provides Spring AI’s MCP integration, autoconfiguration “starters” for MCP servers and clients, which handle a lot of boilerplate for us. 

By adding the **Spring AI MCP server** starter to our project, we get an MCP server up and running with minimal code: Spring Boot will auto-create the necessary 
components and even set up HTTP endpoints for tool communication (using Server-Sent Events for streaming).

Similarly, the **MCP client starter** allows our app to easily connect to external MCP servers, supporting both synchronous calls and reactive streams, as well as multiple transport types (STDIO, WebClient, etc.)

In practice, this means we can configure external tools in `application.yml` and have Spring launch and manage those connections.

For example, here’s a snippet of our YAML configuration where we define a few backend tool connections using the STDIO transport (which in our case runs each tool as a separate Docker container):

```yaml
spring:
  ai:
    mcp:
      server:
        enabled: true
        type: ASYNC
        transport: WEBFLUX    # Use reactive SSE endpoints for the server
      client:
        enabled: true
        type: SYNC            # Use synchronous clients for simplicity
        stdio:
          connections:
            memory:
              command: docker
              args: [ "run", "-i", "--rm",
                     "--volume", "/path/to/data:/data", "mcp/memory", "/data" ]
            duckduckgo:
              command: docker
              args: [ "run", "-i", "--rm", "mcp/duckduckgo" ]
            # ... (additional tool connectors like paper-search, etc.)
```

Breaking this down: we enable an MCP server in *async mode* (so our gateway can handle concurrent calls without blocking) 
and specify a WEBFLUX transport (which sets up an SSE endpoint at /sse for clients to connect). 
We also enable an MCP client in the same app, and under `client.stdio.connections` we list several mcp servers. For each tool (memory, duckduckgo, etc.) request, 
Spring will spawn a process via the given command – in this case, running a Docker image that implements the MCP server for that tool. 

These official `mcp/*` Docker images are pre-built MCP servers for common services (a vector memory store, DuckDuckGo search, scientific paper search, etc.), shared by the open-source MCP community. 

Spring AI’s MCP client support also allows other transports like HTTP – for instance, we could connect to a remote MCP server via SSE by configuring `mcp.client.sse.connections`.

## Designing the MCP Gateway

With the configuration above, by the time our application is running, we have:

* One MCP Server (the gateway’s own server) listening on an HTTP SSE endpoint (e.g. http://localhost:9090/sse).
* Multiple MCP Clients each connected to a different external tool provider.

The remaining task is to bridge these together: we want our MCP server to expose all the tools that those clients have fetched. 
Spring’s abstraction for tools is a **ToolCallback**, which is essentially a handle that the server can invoke when a particular tool 
is called (it includes the tool’s definition and knows how to execute (call) it). 

In our case, we override this to register a custom ToolCallbackProvider that aggregates the tool callbacks from all our MCP clients.

## Key Components in our Implementation

To achieve the above, we built a few key classes in our Spring Boot project (besides the usual Spring Boot application class):

* **GatewayProvidersConfig** – a configuration class that defines our custom `ToolCallbackProvider` bean. It injects all `McpSyncClient` and `McpAsyncClient` instances 
(these are provided by the MCP client starter based on our YAML config) and iterates over each to retrieve their tool list. For synchronous clients, we call `client.listTools()` 
(which returns metadata of tools that client’s server offers) and wrap each tool with a Spring AI `SyncMcpToolCallback` (this is a built-in adapter that turns an MCP client + tool definition into a callable tool). 
For asynchronous clients, we use Spring’s `AsyncMcpToolCallbackProvider` to get all tool callbacks in one go. We then collect all those tool callbacks into a single list. 
This collection is returned as a unified `ToolCallbackProvider` (using `ToolCallbackProvider.from(list)`), which the MCP server will use to advertise available tools and dispatch calls.

* **PrefixedToolCallback** – a wrapper class we created that implements `ToolCallback` and simply **delegates to another ToolCallback**, but adjusts the tool’s name. 
This is where we handle prefixing. We support three modes (configurable via properties): NONE (no prefix, use original names), STATIC (use a fixed static prefix for all tools), or ALIAS (intended for per-source prefixes). 
In our configuration we often use STATIC mode with a prefix like "gw", so a tool originally named "search" becomes "gw_search" when exposed by the gateway. 
This avoids collisions and can also make it clear that the tool is coming via the gateway. The `PrefixedToolCallback.getToolDefinition()` returns a custom `PrefixedToolDefinition` with the altered name, 
while pass-through other metadata unchanged.

* **McpGatewayProperties** – a simple @ConfigurationProperties class that holds our gateway settings (prefix mode, delimiter, static prefix string, etc. as described above, plus maybe a timeout setting for calls). 
This allows us to externalize those configurations (e.g. in application.yml under `mcp.gateway.prefixMode` etc.) and use them in `PrefixedToolCallback` logic.

* **CatalogHttpController** – a small Spring Web MVC controller that exposes an HTTP GET endpoint at `/mcp/gateway/catalog`. 
Hitting this returns a list of the currently registered ToolCallback objects (serialized to JSON). Added mostly for convenience and debugging – it lets us  
see what tools the gateway is exposing at runtime by visiting the URL in a browser or curl. Each tool entry includes its name (with prefix applied) and description. 
This endpoint isn’t part of MCP per se (MCP has its own discovery mechanism), but it’s handy for a quick sanity check that your gateway has aggregated everything expected.

After this setup, our gateway’s MCP server might be exposing (for example) tools named `gw_memoryStore`, `gw_duckduckgoSearch`, `gw_paperSearch`, etc., all accessible 
via one HTTP streaming endpoint. From the perspective of an AI assistant client, it’s just talking to one server.
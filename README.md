# Spring AI MCP Gateway (Streamable HTTP + OAuth 2.1)

This repository hosts a multi‑module Spring Boot project that implements an MCP Gateway using Spring AI and secures it
with OAuth 2.1. 

In this branch (OAUTH2.1_CHATGPT_TUNNELS) we keep the transport as **Streamable HTTP** and introduce:

- OAuth 2.1 Authorization Code + PKCE with a **public client** (no client secret) for ChatGPT Connectors.
- Exposure of the local environment via **Cloudflare Tunnels** (single hostname, path‑based routing).

Modules remain the same:

- `auth-server/` – OAuth 2.1 Authorization Server (JWT issuer) on port 9090.
- `mcp-gateway/` – Spring AI MCP server/client acting as a Resource Server on port 8080.

The public MCP endpoint is exposed at `https://<your-domain>/mcp` (through the tunnel) and protected with Bearer tokens
obtained by ChatGPT through the OAuth flow against the Authorization Server at `https://<your-domain>`.

## Branch History

This repository uses branches to illustrate the evolution of the repo:

- [NO_AUTH_SSE](https://github.com/oalles/springai-mcp-gateway/tree/NO_AUTH_SSE) — anchor for “SSE without security”. Spring AI configuration project.

- [OAUTH2.1_STREAMABLE](https://github.com/oalles/springai-mcp-gateway/tree/OAUTH2.1_STREAMABLE) — Streamable HTTP + OAuth 2.1 (Resource Server with local issuer) with Client Credentials.

- [OAUTH2.1_CHATGPT_TUNNELS](https://github.com/oalles/springai-mcp-gateway/tree/OAUTH2.1_CHATGPT_TUNNELS) — ChatGPT Connectors + Cloudflare Tunnels + OAuth 2.1 (this branch)
    - Keep Streamable HTTP at `/mcp` (Gateway on 8080). Several tools, exposed through the same endpoint.
    - Switch to Authorization Code + PKCE with a **public client** registered in the Authorization Server.
    - Use a public hostname via **Cloudflare Tunnel** and validate JWTs with `issuer=https://<your-domain>`.
    - ChatGPT handles the OAuth flow and automatically injects `Authorization: Bearer <token>`.

## What Changed From `OAUTH2.1_STREAMABLE` to `OAUTH2.1_CHATGPT_TUNNELS`

- Authentication flow: from Client Credentials → Authorization Code + PKCE with a public client (no secret).
- Issuer and URLs: from `http://localhost:9090` → public `https://<your-domain>` through Cloudflare Tunnel.
- ChatGPT integration: the connector completes the OAuth flow and manages token refresh automatically.
- Resource Server keeps Streamable HTTP at `/mcp` and validates JWTs from the public issuer.

## Project Structure

- Parent aggregator POM (`pom.xml`, packaging `pom`).
- Modules:
    - `auth-server/` – Authorization server configuration and keys. Config:
      `auth-server/src/main/resources/application.yml` (port 9090).
    - `mcp-gateway/` – MCP Gateway server/client and security. Config:
      `mcp-gateway/src/main/resources/application.yml` (port 8080).

Key classes in `mcp-gateway/`:

- `es.omarall.mcp.gateway.McpGatewayApplication`
- `es.omarall.mcp.gateway.SecurityConfiguration`
- `es.omarall.mcp.gateway.GatewayProvidersConfig`
- `es.omarall.mcp.gateway.McpGatewayProperties`
- `es.omarall.mcp.gateway.PrefixedToolCallback` / `PrefixedToolDefinition`

## Build

- Build all modules: `mvn -q clean package`

## Run (Local)

1) Start the Authorization Server (port 9090):

- `mvn -q -pl auth-server spring-boot:run`

2) Start the MCP Gateway (port 8080):

- `mvn -q -pl mcp-gateway spring-boot:run`

## Cloudflare Tunnel Setup

 For Cloudflare Tunnel setup and path‑based routing, see [CLOUDFLARE.md](./CLOUDFLARE.md).

## Register the Gateway in ChatGPT (OAuth 2.1 + PKCE)

![ChatGpt.gif](images/ChatGPT-config.png)

See [CHATGPT](./CHATGPT.md) for detailed steps.

## JetBrains GitHub Copilot (mcp.json)

If you also use Copilot MCP clients locally, keep using your preferred config. For this branch, tokens are managed by
ChatGPT during the OAuth flow; `mcp-remote` examples with Client Credentials do not apply.

## Configuration Snapshots

`mcp-gateway/src/main/resources/application.yml` (excerpt):

```yaml
server:
  port: 8080
spring:
  ai:
    mcp:
      server:
        enabled: true
        protocol: streamable
        name: ${spring.application.name}
      client:
        enabled: true
        name: mcp-client
        version: 1.0.0
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://<your-domain>
mcp:
  gateway:
    prefixMode: STATIC
    delimiter: "_"
    staticPrefix: "gw"
```

`auth-server/src/main/resources/application.yml` (excerpt):

```yaml
server:
  port: 9090
spring:
  security:
    oauth2:
      authorizationserver:
        issuer: https://<your-domain>
        client:
          default-client:
            token:
              access-token-time-to-live: 1h
            registration:
              client-id: springai-gateway-client
              client-authentication-methods: [ none ]
              authorization-grant-types: [ authorization_code, refresh_token ]
              scopes: [ mcp:read, mcp:write ]
              redirect-uris: [ https://chatgpt.com/connector_platform_oauth_redirect ]
            require-proof-key: true
```

## Notes

- Use environment variables to override configuration safely (e.g., `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI`,
  `SERVER_PORT`, `SPRING_AI_*`).
- Default local ports: 9090 (auth-server), 8080 (mcp-gateway).


## Screenshots

![run.gif](images/run.gif)

![copilot.gif](images/copilot.gif)

## References

* https://spring.io/blog/2025/09/16/spring-ai-mcp-intro-blog
* https://spring.io/blog/2025/09/19/spring-ai-1-1-0-M2-mcp-focused
* https://www.danvega.dev/blog/cyc-mcp-server-spring-ai
* https://github.com/spring-ai-community/mcp-security/
* https://spring.io/blog/2025/09/30/spring-ai-mcp-server-security

# MCP Gateway (Streamable HTTP + OAuth 2.1)

MCP server based on Spring AI that acts as an OAuth 2.1 Resource Server. It exposes a Streamable HTTP endpoint that
MCP clients use for discovery and tool invocation. In this branch the primary client is ChatGPT Connectors, which
obtain tokens via Authorization Code + PKCE (public client, no secret). The gateway only validates Bearer tokens.

- Port: `8080`
- MCP endpoint: `http://localhost:8080/mcp` (locally) / `https://<your-domain>/mcp` (via tunnel)
- Requires: `Authorization: Bearer <access_token>` issued by your Authorization Server (module `auth-server`).

## Run

1) Start the Authorization Server:

- `mvn -q -pl auth-server spring-boot:run`

2) Start the MCP Gateway:

- `mvn -q -pl mcp-gateway spring-boot:run`

Requirements: Java 25, Maven.

## Configuration (gateway)

File `mcp-gateway/src/main/resources/application.yml` (excerpt):

```yaml
server:
  port: 8080
spring:
  ai:
    mcp:
      server:
        enabled: true
        protocol: streamable   # exposes /mcp
        name: ${spring.application.name}
      client:
        enabled: true
        name: mcp-client
        version: 1.0.0
  security:
    oauth2:
      resourceserver:
        jwt:
          # Must match the public issuer of your Authorization Server
          issuer-uri: https://<your-domain>
mcp:
  gateway:
    prefixMode: STATIC
    delimiter: "_"
    staticPrefix: "gw"
```

Notes:

- The gateway aggregates tools from MCP client connections (e.g., official `mcp/*` images via stdio/docker) and exposes
  them with the `gw_` prefix to avoid collisions.
- Discovery is native to the MCP protocol; there is no separate REST catalog.

## Use with ChatGPT (Authorization Code + PKCE)

This branch integrates with ChatGPT Connectors. ChatGPT performs the OAuth 2.1 Authorization Code + PKCE flow against
your Authorization Server and injects `Authorization: Bearer <token>` on calls to `/mcp`.

- Ensure your Authorization Server `issuer` equals the public domain served by your tunnel (for example, `https://dev.example.com`).
- Configure Cloudflare Tunnel (single hostname, path-based routing) so that:
  - `https://<your-domain>/mcp` → MCP Gateway on `localhost:8080`
  - `https://<your-domain>/` → Authorization Server on `localhost:9090`
- Register the gateway in ChatGPT Developer Mode. See `CHATGPT.md` for step-by-step screenshots.

> Previous Client Credentials examples (curl + `mcp-remote`) from earlier branches do not apply here.

## Useful environment variables

- `SERVER_PORT` to change the port (default `8080`).
- `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI` to point the gateway to another issuer.
- `SPRING_AI_*` to override MCP client/server connections.

## Troubleshooting

- 401/invalid_token: verify the token is not expired and the `issuer`/`issuer-uri` is the public domain.
- redirect_uri_mismatch: ensure `https://chatgpt.com/connector_platform_oauth_redirect` is present in the Authorization Server client.
- 403: check the `Authorization` header uses the `Bearer <token>` format.
- Ports in use: adjust `SERVER_PORT` or free ports 8080/9090.
- Useful logs: enable `org.springframework.security=TRACE` and `org.springframework.ai=TRACE`.

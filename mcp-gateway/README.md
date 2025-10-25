# MCP Gateway (Streamable HTTP + OAuth 2.1)

MCP server based on Spring AI that acts as a Resource Server protected by OAuth 2.1. It exposes a Streamable HTTP
endpoint that MCP clients (e.g. `mcp-remote`, Copilot) can use for discovery and tool invocation.

- Port: `8080`
- MCP endpoint: `http://localhost:8080/mcp`
- Requires: `Authorization: Bearer <access_token>` issued by `http://localhost:9090` (module `auth-server`).

## Run

1) Start the Authorization Server first:

- `mvn -q -pl auth-server spring-boot:run`

2) Start the MCP Gateway:

- `mvn -q -pl mcp-gateway spring-boot:run`

Requirements: Java 25, Maven. For CLI testing: `npm`/`npx` and optionally `jq`.

## Main configuration

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
          issuer-uri: http://localhost:9090
mcp:
  gateway:
    prefixMode: STATIC
    delimiter: "_"
    staticPrefix: "gw"
```

Notes:

- The gateway adds tools from MCP client connections (for example, official images `mcp/*` via stdio/docker) and exposes
  them with the `gw_` prefix to avoid collisions.
- There is no catalog REST endpoint; discovery is native to the MCP protocol.

## Obtain a token and test with mcp-remote

1) Get a token from the `auth-server` (client credentials):

```bash
TOKEN=$(curl -s -u springai-gateway-client:my-secret \
  -d "grant_type=client_credentials" \
  http://localhost:9090/oauth2/token | jq -r .access_token)
```

2) Connect the MCP client:

```bash
npx mcp-remote http://localhost:8080/mcp \
  --header "Authorization: Bearer $TOKEN"
```

## JetBrains Copilot configuration (mcp.json)

Example file `~/.config/github-copilot/intellij/mcp.json`:

```json
{
  "servers": {
    "springai-mcp-gw": {
      "command": "npx",
      "args": [
        "mcp-remote",
        "http://127.0.0.1:8080/mcp",
        "--header",
        "Authorization: Bearer ${AUTH_TOKEN}"
      ],
      "env": {
        "AUTH_TOKEN": "<paste_your_access_token_here>"
      }
    }
  }
}
```

## Useful environment variables

- `SERVER_PORT` to change the port (default `8080`).
- `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI` to point to another issuer.
- `SPRING_AI_*` to override MCP client/server connections.

## Troubleshooting

- 401/invalid_token: verify the token is not expired and that `issuer-uri` is `http://localhost:9090`.
- 403: check the `Authorization` header and the `Bearer <token>` format.
- Ports in use: adjust `SERVER_PORT` or free ports 8080/9090.
- Useful logs: enable `org.springframework.security=TRACE` and `org.springframework.ai=TRACE`.


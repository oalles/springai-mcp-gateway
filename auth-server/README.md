# Authorization Server (OAuth 2.1)

Authorization server for the multi-module project. It issues JWT tokens used by the `mcp-gateway` module as a Resource
Server.

- Port: `9090`
- Issuer: `http://localhost:9090`
- Well-known metadata: `http://localhost:9090/.well-known/openid-configuration`

## Run

- `mvn -q -pl auth-server spring-boot:run`

Requirements: Java 25, Maven. Optionally `jq` and `npm`/`npx` if you want to test with `mcp-remote`.

## Preconfigured client

- `client_id`: `springai-gateway-client`
- `client_secret`: `my-secret`
- `grant_type`: `client_credentials`
- Access token TTL: 1h

Configured in `auth-server/src/main/resources/application.yml`.

## Obtain an access token

```bash
# The JSON response includes access_token, token_type=bearer and expires_in
curl -u springai-gateway-client:my-secret \
  -d "grant_type=client_credentials" \
  http://localhost:9090/oauth2/token

# Capture only the access_token (requires jq)
TOKEN=$(curl -s -u springai-gateway-client:my-secret \
  -d "grant_type=client_credentials" \
  http://localhost:9090/oauth2/token | jq -r .access_token)
echo $TOKEN
```

## Test integration with the MCP Gateway

Also start the `mcp-gateway` module on port 8080 and use `mcp-remote` against the Streamable HTTP endpoint passing the
Bearer token:

```bash
npx mcp-remote http://127.0.0.1:8080/mcp \
  --header "Authorization: Bearer $TOKEN"
```

## JetBrains Copilot configuration (mcp.json)

File `~/.config/github-copilot/intellij/mcp.json`:

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

> Do not commit real secrets to the repo. Use environment variables or profiles.


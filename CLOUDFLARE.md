# Cloudflare Tunnel + Path-based Routing

## What is Cloudflare Tunnel? (brief)

- Cloudflare Tunnel allows you to publish services running on your local machine or network to the Internet without 
opening ports or exposing your public IP. It does this by creating a secure outbound tunnel from `cloudflared` to the Cloudflare network.
- Requirement: you need a **domain managed by Cloudflare** (the zone must be added to your account, and the domain's nameservers must point to Cloudflare). We will use a subdomain (e.g., `dev.yourdomain.com`) to route traffic to the tunnel.
- Objective in this repo: expose our local MCP servers under a single public hostname using path-based routing:
  - `https://<your-domain>/mcp` → MCP Gateway on `localhost:8080` (Streamable HTTP)
  - `https://<your-domain>/` → Authorization Server on `localhost:9090` (`/.well-known`, `/oauth2/*`)
- Benefits: a single public domain, no need to open ports on the router/firewall, and Cloudflare Zero Trust in front 
if you decide to add policies.

## 1) Processes

* **Auth Server** on `localhost:9090` (issues tokens and serves `/.well-known` + `/oauth2/*`).
* **MCP Gateway** on `localhost:8080` (exposes **Streamable HTTP** at `/mcp`).

## 2) `cloudflared` configuration

Given my domain is: omarall.es

Install `cloudflared` from: https://developers.cloudflare.com/cloudflare-one/networks/connectors/cloudflare-tunnel/downloads/

Manually create or edit `~/.cloudflared/config.yml`:

```yaml
tunnel: mcp-gateway
credentials-file: /home/<usuario>/.cloudflared/<TUNNEL_ID>.json

ingress:
  # MCP stream → Gateway (8080)
  - hostname: dev.omarall.es
    path: /mcp*
    service: http://localhost:8080

  # OAuth2/Well-known → Auth Server (9090)
  - hostname: dev.omarall.es
    service: http://localhost:9090

  - service: http_status:404
```

> Keys:
>
> * The **order** matters: put the rule with `path: /mcp*` first.
> * The rest (token, jwks, authorize, etc.) goes to the AS (9090).
> * This pattern (Tunnel + path-based routing) is exactly the recommended approach to expose internal services behind a
    single hostname, avoiding opening ports and with Zero Trust in front.

### Essential commands

```bash
cloudflared tunnel login
cloudflared tunnel create mcp-gateway 
# Adjust config.yml as above
cloudflared tunnel route dns mcp-gateway dev.omarall.es
cloudflared tunnel run mcp-gateway
```

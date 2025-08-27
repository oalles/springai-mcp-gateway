# Docker MCP Gateway Overview

> **Purpose:** This note explains that Docker provides a robust, production-grade **MCP Gateway**.  
> Our Spring AI–based gateway is a clear, educational example that shows the moving parts and offers full flexibility,
> but Docker’s gateway can be a more **operationally hardened** choice when you need defense-in-depth, centralized
> management, and scale.

---

## What is the Docker MCP Gateway?

Docker’s MCP Gateway is a secure, centralized entry point for **Model Context Protocol (MCP)** servers. It standardizes
how client tools (e.g., GitHub Copilot, IDEs, CLI agents) connect to multiple MCP servers through a single, governed
endpoint. Think **service mesh** for MCP: policy, isolation, and observability in one place.

---

## Why consider it?

- **Security by default:** Container isolation, least-privilege access, and guardrails around CPU/memory/filesystem.
- **Centralized control:** Enable/disable servers, version/roll back, and apply uniform policies in one place.
- **Observability:** Unified logs and traces for MCP calls, useful for debugging, auditing, and SLOs.
- **Scalability & reliability:** Leverages Docker’s ecosystem (images, registries, CI/CD) to move from laptop to prod
  smoothly.
- **Interoperability:** Expose one gateway to many clients (Copilot, VS Code, internal tools) with consistent contracts.

---

## How it compares to our Spring AI Gateway

| Aspect            | Our Spring AI Example                            | Docker MCP Gateway                                     |
|-------------------|--------------------------------------------------|--------------------------------------------------------|
| **Goal**          | Learning, customization, rapid iteration         | Operational robustness, security, and scale            |
| **Deployment**    | JVM app (Spring Boot/Spring AI)                  | Container-native (Docker)                              |
| **Security**      | You implement policies/filters                   | Built-in isolation & policy hooks                      |
| **Observability** | Add your own logging/metrics                     | Centralized logs/traces out of the box                 |
| **Ops model**     | Code-first customization                         | Configuration-first governance                         |
| **When to pick**  | Prototyping, custom logic, tight domain behavior | Production readiness, multiple teams, compliance needs |

**Bottom line:** Start with our Spring AI gateway to learn and tailor behavior. Move to Docker’s gateway when you need
standardized, auditable operations across teams.

---

## Typical Architecture

Clients (Copilot / IDEs / Agents)
│
▼
Docker MCP Gateway ──► Authn/Policy/Observability
│ │
▼ ▼
MCP Server A MCP Server B ... (containerized)

The gateway consolidates connections and policies, while individual MCP servers remain separate, containerized services.

---

## Quick Start (Conceptual)

1. **Enable Docker MCP tooling** in your environment.
2. **Discover/enable MCP servers** you want to expose (public catalog or your own images).
3. **Run the gateway** with your chosen transport (e.g., SSE) and guardrails (verification, logging).
4. **Point clients** (e.g., GitHub Copilot) at the gateway URL instead of each server individually.

> This shifts configuration from “N clients × M servers” to “N clients → 1 gateway → M servers”.

---

## Using with GitHub Copilot (IntelliJ)

If you’re currently connecting Copilot to our Spring AI gateway using `mcp-remote` via:

```json
{
  "servers": {
    "springai-mcp-gw": {
      "command": "/npx",
      "args": ["mcp-remote", "http://localhost:9090/sse"]
    }
  }
}
```

you can later swap the endpoint to the Docker MCP Gateway URL, keeping the same pattern:

```json
{
  "servers": {
    "docker-mcp-gateway": {
      "command": "/npx",
      "args": ["mcp-remote", "http://localhost:PORT/sse"]
    }
  }
}
```

> The client-side experience remains the same; the difference is that the gateway now manages multiple servers, policies, and logs centrally.
 
## Security & Compliance Posture

* Container isolation for each MCP server reduces blast radius.
* Image provenance & version pinning support reproducible deployments.
* Central policy for timeouts, rate limits, and secret redaction helps standardize controls.
* Unified audit trail simplifies compliance and incident response.

## Observability & Operations

* Single pane of glass for logs and traces across all MCP calls.
* Consistent metrics enable SLOs (latency, error rates) and capacity planning.
* Easier debugging when client issues are traced through the same gateway.

## When to choose which

* Choose our Spring AI Gateway if you need:
    * Deep customization in Java/Spring,
    * Domain-specific composition logic, or
    * A learning/reference implementation you can extend freely.
* Choose Docker MCP Gateway if you need:
    * A hardened path to production with multiple teams,
    * Centralized governance, guardrails, and visibility,
    * Seamless move from local dev to CI/CD and clusters. 
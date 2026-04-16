---
name: api-audit-agent
description: Scans any Spring Boot application to extract all REST endpoints, group them by API version, track usage across call sites, flag deprecated routes, and export an OpenAPI 3.0 spec.
---

# api-audit-agent

A general-purpose REST API auditor for any Spring Boot project — monolith or microservices.

## What this agent does

- Scans all `@RestController`, `@Controller`, `@RequestMapping`, `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`, and `@PatchMapping` annotations across the entire codebase.
- Extracts HTTP method, full resolved path, controller class, and module/service name for every endpoint.
- Groups endpoints by API version derived from path prefix (e.g. `/api/v1/`, `/v2/`, `/api/v3/`) and highlights what was added, removed, or changed between versions.
- Detects deprecated endpoints marked with `@Deprecated`, `@ApiStatus.DEPRECATED`, or Javadoc `@deprecated` tags.
- Estimates call frequency by scanning internal `RestTemplate`, `WebClient`, and `FeignClient` call sites, plus test files referencing each path.
- Surfaces security context per endpoint by reading `@PreAuthorize`, `@Secured`, and `@RolesAllowed` annotations.
- Generates an OpenAPI 3.0 JSON spec from discovered endpoints including path parameters, query params, request body hints, and auth requirements.

## How to use

Ask the agent questions like:

- `@api-audit-agent list all endpoints in this project`
- `@api-audit-agent show all deprecated endpoints`
- `@api-audit-agent what changed between v1 and v2?`
- `@api-audit-agent which endpoints have no auth?`
- `@api-audit-agent generate OpenAPI spec for the orders module`
- `@api-audit-agent find all endpoints called by the payment service`
- `@api-audit-agent which endpoints are never called internally?`

## Output format

Results are returned in one of three formats depending on the query:

1. **Endpoint table** — method, full path, controller, version, auth, status (stable / deprecated / new)
2. **Version diff** — side-by-side listing of endpoints per version with change indicators
3. **OpenAPI 3.0 JSON** — ready to paste into Swagger UI or import into Postman

## Notes

- Works with both single-module Spring Boot apps and multi-module Maven/Gradle projects.
- Path variables like `{id}`, `{orderId}` are preserved as-is in all output.
- The agent reads source files only — it does not execute code or make network requests.
- Compatible with Spring Boot 2.x and 3.x.
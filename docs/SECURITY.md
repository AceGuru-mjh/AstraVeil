# AstraVeil Security Policy

## Reporting a Vulnerability

If you discover a security vulnerability in AstraVeil:

1. **DO NOT** open a public GitHub issue
2. Email: security@astraveil.project (encrypted preferred)
3. Include: description, reproduction steps, impact assessment
4. Response time: 48 hours acknowledgment, 7 days assessment

## Scope

- AstraVeil daemon (astrad) IPC authentication
- Module trust gate / signature verification
- Update chain verification
- Rust policy engine fail-closed guarantees
- ZIP/AVM extraction hardening

## Out of Scope

- Android OS / kernel vulnerabilities
- Physical device access attacks
- SELinux bypass via kernel exploits

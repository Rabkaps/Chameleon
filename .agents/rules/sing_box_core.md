# Sing-Box Core & VPN Rules

- **Explicit `"detour": "direct"` for Domestic DNS**: Domestic DNS servers in `dns.servers` (e.g. `dns-direct`) MUST explicitly set `"detour": "direct"` (matching MahsaNG and Nekobox client architecture) to force domestic queries straight out the physical network interface.
- **NEVER Release Special Edition**: NEVER include, attach, or upload `special` flavor APKs to GitHub releases, tags, or public assets. ONLY `standard` flavor APKs are permitted for public release.

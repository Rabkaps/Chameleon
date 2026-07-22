# Sing-Box Core & VPN Rules

- **Explicit `"detour": "direct"` for Domestic DNS**: Domestic DNS servers in `dns.servers` (e.g. `dns-direct`) MUST explicitly set `"detour": "direct"` (matching MahsaNG and Nekobox client architecture) to force domestic queries straight out the physical network interface.

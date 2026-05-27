#!/bin/bash
set -euo pipefail

# Only run in Claude Code remote (web) environments
if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

# Install web dependencies
cd "$CLAUDE_PROJECT_DIR/web"
npm install

# Write .mcp.json from env var so the key is never committed
if [ -n "${SUPABASE_ACCESS_TOKEN:-}" ]; then
  cat > "$CLAUDE_PROJECT_DIR/.mcp.json" << EOF
{
  "mcpServers": {
    "supabase": {
      "command": "npx",
      "args": ["-y", "@supabase/mcp-server-supabase@latest", "--access-token", "${SUPABASE_ACCESS_TOKEN}"]
    }
  }
}
EOF
  echo "supabase mcp configured"
else
  echo "WARNING: SUPABASE_ACCESS_TOKEN not set — Supabase MCP will not be available"
fi

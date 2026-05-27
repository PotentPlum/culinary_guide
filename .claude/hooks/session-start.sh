#!/bin/bash
set -euo pipefail

# Only run in Claude Code remote (web) environments
if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

if [ -z "${CLAUDE_PROJECT_DIR:-}" ]; then
  echo "ERROR: CLAUDE_PROJECT_DIR not set or empty"
  exit 1
fi

# Install web dependencies (non-fatal — MCP config is independent)
cd "$CLAUDE_PROJECT_DIR/web"
npm install || echo "WARNING: npm install failed — some features may not work"

# Write .mcp.json from env var so the key is never committed
if [ -n "${SUPABASE_ACCESS_TOKEN:-}" ]; then
  jq -n --arg token "$SUPABASE_ACCESS_TOKEN" \
    '{"mcpServers":{"supabase":{"command":"npx","args":["-y","@supabase/mcp-server-supabase@latest","--access-token",$token]}}}' \
    > "$CLAUDE_PROJECT_DIR/.mcp.json"
  echo "supabase mcp configured"
else
  echo "WARNING: SUPABASE_ACCESS_TOKEN not set — Supabase MCP will not be available"
fi

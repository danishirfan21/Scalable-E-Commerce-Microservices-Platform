#!/usr/bin/env bash
# Thin wrapper so `bash scripts/verify_codespaces.sh` (the command referenced when this repo is
# opened in GitHub Codespaces) runs the same verification as `scripts/verify.sh`. Codespaces
# ships Docker, Maven, Node, curl, and jq out of the box, so no extra setup is needed here.
set -euo pipefail
exec "$(dirname "${BASH_SOURCE[0]}")/verify.sh" "$@"

#!/usr/bin/env bash
set -euo pipefail
find . -path './.git' -prune -o -path './target' -prune -o -maxdepth 4 -print | sort

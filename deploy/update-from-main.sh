#!/usr/bin/env bash

set -euo pipefail

current_branch="$(git branch --show-current)"
if [[ "$current_branch" != "main" ]]; then
    echo "Production checkout must stay on main, but current branch is $current_branch."
    exit 1
fi

git fetch origin main
git pull --ff-only origin main
./deploy/deploy.sh

---
name: Git workflow — merge direct to develop, master PR after
description: Never raise a PR to develop; merge feature branches directly. Only raise PRs to master after all features are on develop.
type: feedback
originSessionId: b384e883-533b-4a22-9875-2a4b3249054c
---
Merge feature branches directly to develop — do not open PRs to develop first. After all features land on develop (and can be tested on dev environment), raise a single PR from develop → master for each repo.

**Why:** PRs to develop are unnecessary overhead. The dev environment (app-dev / api-dev) serves as the integration test surface. Only master-bound PRs need review gates.

**How to apply:** Use `gh pr merge <number> --merge --delete-branch` to merge each open PR in stack order. Once develop is up to date, create `gh pr create --base master --head develop`.

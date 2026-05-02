---
name: Coding & Workflow Feedback
description: Confirmed preferences around code style, autonomy, commit/PR flow, and response format
type: feedback
originSessionId: b384e883-533b-4a22-9875-2a4b3249054c
---
**No unsolicited cleanup.** A bug fix doesn't need surrounding refactors. A feature doesn't need extra error handling. Scope = what was asked.

**Why:** Solo dev, high velocity — unnecessary changes add review burden and risk regressions.

**How to apply:** Implement exactly what the ticket says. If you notice something clearly broken nearby, flag it as a spawn_task, don't fix it inline.

---

**Conventional commits + Jira key in message.** Format: `feat(LSB-XX): short description — key files if non-obvious`.

**Why:** Commit history is the changelog; Jira keys link context.

**How to apply:** Always include the ticket key. If multiple tickets in one commit, list all (e.g. `feat(LSB-31/33/35)`).

---

**After every PR: move Jira ticket to Ready for Review, comment the PR link.**

**Why:** Keeps board state accurate without manual effort.

**How to apply:** Do this immediately after `gh pr create` returns.

---

**Build before declaring done.** Backend: `./mvnw clean package -DskipTests`. Frontend: `npm run build`. Don't declare done without a successful build.

**Why:** TypeScript and Spring compile errors surface things tests don't catch.

**How to apply:** Run the build as the last step before reporting completion.

---

**Test failures don't block PRs without asking.** The test suite is actively being improved.

**Why:** Some tests are flaky or not yet updated for new features.

**How to apply:** Report failures, ask "proceed anyway?" — don't block.

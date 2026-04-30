Attempt to run the test suite (`./mvnw test`). If tests fail, show the failures and ask whether to proceed with the PR anyway — do not block automatically.

Then: commit any staged changes, push the branch, and create a PR targeting `develop`. Write a clear description covering what changed and why. If tests were failing, note it in the PR description.

After the PR is created: move all relevant Jira tickets (those referenced in commits on this branch) to "Ready for Review" and add a comment on each with the PR link.

Report: test result, PR URL, and which Jira tickets were updated.

# .githooks

`pre-push` refuses force-pushes to `main` and refuses deleting it on the remote.

GitHub's own branch protection needs a paid plan for private repositories, and
LushMCNetwork is on the free org plan, so this stands in for it. It is
client-side: it guards whichever machines have enabled it, and `--no-verify`
bypasses it on purpose. It exists to stop accidents, not a determined push.

## Enabling

Committed hooks are not active until git is told where to look. Once per clone:

```
git config core.hooksPath .githooks
```

## If a force-push does get through

GitHub records it regardless of plan. `Repo -> Insights -> Activity` (or
`gh api repos/LushMCNetwork/<repo>/activity`) lists every push with its before
and after SHA, so the overwritten commit can be recovered by SHA.

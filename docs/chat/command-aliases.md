# Command aliases

Typing `/sb 5` sends `/sellall bait 5` when an alias `sb` → `sellall bait` exists (2026-10-18, asked for by luna).

- Files: `input/CommandAliases` (entry, `expand`, the `ClientSendMessageEvents.MODIFY_COMMAND` hook), `screen/AliasScreen` (editor, "command aliases" link on the tools page of the settings), setting `aliases` (sanitized in `TackleboxSettings`).
- Only the first word is compared (case-insensitive, slashes in the editor ignored); the rest of the line is appended. One pass: an alias never expands another. Disabled or half-empty rows do nothing.
- An alias wins over a server or vanilla command of the same name. The hook also sees commands sent by key commands.
- It only rewrites a command the player sends; it sends nothing itself, so the no-automation rule is not touched.
- Tests: `CommandAliasesTest`, `CommandAliasIntegrationTest`. Not run in game yet: check that the hook fires for typed commands and that it beats a client command of the same name.

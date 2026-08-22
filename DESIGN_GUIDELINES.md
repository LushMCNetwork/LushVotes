# LushMC Raft Survival — House Style Guide

This document captures the visual/text conventions used across every plugin config on
this server (LushLobby, LushVanish, FancyHolograms, etc).
Apply it to any new or edited player-facing text — menus, item lore, chat
messages, holograms.

## Color palette

| Hex | Name | Meaning |
|---|---|---|
| `#00f396` | green-teal | Success, enabled, positive, confirmations |
| `#ff1155` | red-pink | Error, disabled, denial, danger, warnings-that-block |
| `#fdf700` | gold | Section headers, info, highlights, CTAs, universal "premium" accent |
| `#ff7200` | orange | Secondary CTA accent, fuel/charge, quick-access links |
| `#00a4fe` | blue | Accent color (kits, minions, general use) |
| `#9d73ff` | purple | Accent color (chat prefixes, rare tier, minions) |
| `#ff3cfe` | magenta | Accent color (minions, rare tier) |
| `#ffb700` | amber | Extension accent when the 7-color core palette runs out |

Rules:
- Never use raw Minecraft `&`-code neon colors (`&a`, `&c`, `&e`, `&b`, etc.) or
  MiniMessage named colors (`<red>`, `<green>`) in new work — always use a hex from
  this palette, or a deliberately-chosen vanilla color when it's the *literal* color
  of the thing being described (e.g. `&b` for an actual Diamond Block reward, `&9`
  for Lapis, `&8` for Netherite — natural material colors are fine for loot items).
- Rank/tier-specific colors (Castaway `#a8a8a8`, Driftwood `#c68642`, Navigator
  `#00f7f2`, Shipwright `#ffc200`, Leviathan `#8B00FF`; crate tiers Shallows
  `#a4f0ff`/`#5ec8e8`, Sea Foam `#f0fafa`, Abyssal `#9d73ff`) are locked once
  established — reuse them exactly, don't reinvent.
- When a set of items needs more distinct colors than the core palette provides
  (e.g. 8 AxMinions types), extend with a harmonious near-neighbor (`#ffb700` amber
  next to `#fdf700` gold) rather than reaching for an unrelated hue.

## Icon vocabulary

Use these — and only these — for status/action icons. Don't invent new glyphs or
reuse a plugin's own stock icon set (e.g. AxMinions' default `❙` bullet and `(!)`
callout are NOT house style; replace them).

| Icon | Meaning | Color pairing |
|---|---|---|
| `✔` | Success / confirmed / enabled | `#00f396` |
| `✘` | Error / denied / disabled | `#ff1155` |
| `⚠` | Warning (non-fatal caution, overwrite prompts) | `#fdf700` or `#ff1155` depending on severity |
| `⌚` | Pending / in-progress / cooldown | `#fdf700` |
| `⚡` | Quick-access link, lightning/interval callouts | `#ff7200` |
| `✦` | Generic stat/trade bullet point | matches the line's own accent |
| `▶` `◀` | "Click here" / link brackets | `#fdf700` |
| `⚓` | Nautical/raft-theme decorative bracket (title flourishes) | matches title color |
| `♛` | Leaderboard/rank crown | `#fdf700` |
| `⚔` | Combat/kills | `#ff1155` |

Reserve emoji-adjacent Minecraft-supported symbols (⚖, 🛠, 👑, etc.) for one-off
title flourishes only, mirrored on both sides of a heading — don't sprinkle them
into body text.

## DeluxeMenus GUI item lore skeleton

Every clickable menu item follows this shape:

```yaml
lore:
  - "&7<Subtitle — short category label, e.g. 'Rank Kit', 'Loot & Rewards'>"
  - ""
  - "&#fdf700Information"
  - "&f<description line 1>"
  - "&f<description line 2, if needed>"
  - ""
  - "&#fdf700▶ &l&nCLICK&r &#fdf700to <Verb — Purchase/Open/Claim/Continue/etc.>"
```

When the item has a price or stat block, insert it between Information and the CTA:

```yaml
  - ""
  - "&#ACCENT✦ &fCost: &#ACCENTn Gems"
  - ""
```

Quick-access command callouts (used on hub/guide menus) use this line instead of a
CTA:
```yaml
  - "&#ff7200⚡ &fQuick Access: &#ff7200/command"
```

### Filler / border pattern

```yaml
"filler3":
  material: WHITE_STAINED_GLASS_PANE
  slots: [<entire grid>]
  display_name: " "
"filler1":
  material: ORANGE_STAINED_GLASS_PANE
  slots: [<true corners>]
  display_name: " "
"filler2":
  material: YELLOW_STAINED_GLASS_PANE
  slots: [<border accent points>]
  display_name: " "
```
Content items are carved out of the filler ranges — never let a content slot overlap
a filler slot, and never let filler bleed into the content area.

### Purchase/economy items (click_requirement pattern)

```yaml
click_requirement:
  requirements:
    gems:
      type: "greater than or equal to"
      input: "%playerpoints_points%"
      output: "<cost>"
      deny_commands:
        - "[message] &#ff1155✘ You don't have <cost> Gems to purchase this item!"
click_commands:
  - "[close] "
  - "[console] gems take %player_name% <cost>"
  - "[console] <grant command>"
  - "[message] &#00f396✔ You have successfully purchased <#ACCENT><Item Name>&#00f396!"
```
Note: `type: "greater than or equal to"` is the *actual* DeluxeMenus requirement
type — `js constraint` does not exist and will silently fail to load. Verify any
unfamiliar requirement type or placeholder format in-game with `/papi parse` before
shipping it; several rounds of this project shipped guessed placeholder syntax that
turned out wrong (`%ajlb_top_..._score%` vs the real `%ajlb_lb_..._alltime_value_formatted%`,
`%tab_condition_...%` which doesn't resolve externally at all).

## Language file (chat message) conventions

- Usage/validation errors: `&#ff1155✘ Usage: /command <args>`
- Denial/error messages: `&#ff1155✘ <full sentence, not a fragment>`
- Success/confirmation: `&#00f396✔ <full sentence>`
- In-progress/pending action: `&#fdf700⌚ <doing-something>...`
- Non-fatal warning (e.g. "this will overwrite X"): `&#fdf700⚠ <sentence>`
- Write full sentences, not clipped fragments — "Purchase failed." becomes "Your
  purchase could not be completed. Please try again."
- Drop bracketed plugin-name prefixes (`[TAB]`, `[LushClear]`) — the icon already
  signals what kind of message it is; the color alone is enough branding.
- Keep `{placeholder}`/`%placeholder%` tokens exactly as-is; only reword the
  surrounding text.

## Holograms (FancyHolograms)

```yaml
text:
  - "&#ACCENT<icon> &l<TITLE> &#ACCENT<icon>"     # icon bracket both sides for major titles
  - ""
  - "&f<2-3 line description, plain white, one inline &#ACCENT highlight per line>"
  - ""
  - "&#fdf700&l<Secondary header — one per hologram, not one per bullet>"
  - "&f<follow-up line, inline accent highlights instead of separate mini-headers>"
```
Prefer flowing 2-3 line prose over stacked one-line-per-bullet mini-headers — a
hologram with five gold sub-headers each followed by one line reads as cluttered;
one secondary header framing a short paragraph reads as designed.

## General principles

1. **Verify placeholders before shipping.** Guessing a plugin's PAPI placeholder
   format from memory has failed multiple times on this project. Check the plugin's
   own command output, decompile the jar's `RequirementType`/`Placeholder` classes,
   or ask the user to `/papi parse` it in-game before wiring it into a menu/hologram.
2. **Reuse locked colors exactly.** Once a rank, crate tier, or minion has a color,
   every file referencing it (kits, keys, holograms, armor) must match — don't
   reinvent per-file.
3. **Match established structure over inventing new structure.** New menu items
   copy the nearest existing item's skeleton; new language keys copy the nearest
   existing key's icon/color choice for that message type (success/error/pending).
4. **Full sentences in player-facing text**, not clipped fragments — this applies
   to language files, hologram body text, and item lore alike.
5. **Don't restyle out-of-scope content** — gameplay numeric balance (enchant
   levels, prices, upgrade curves), ASCII art headers, and plugin-internal comments
   are not part of the visual style pass unless explicitly asked.

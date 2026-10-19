# soft-puk — brand set

Original artwork for **soft-puk**, the offline Android app that reads the
Breach Protocol minigame screen through the camera and shows the optimal
click order. Lime-on-dark terminal aesthetic.

> Not affiliated with CD Projekt Red. No game assets, screenshots, fonts or
> trademarks were used — every path below is original vector work inspired
> by the *idea* of a breach terminal (scanner frame, code matrix, buffer
> slots).

## Files

| File | Role | Use |
|---|---|---|
| `logo.svg` | **mother SVG (editable source)** — badge + `SOFT-PUK` wordmark in vector paths | edit here; never edit the files below, regenerate them |
| `logo-dark.svg` | full lockup for dark surfaces | README header, dark docs |
| `logo-light.svg` | full lockup for light surfaces (ink + deep-green inks) | light docs, print |
| `logo-plate.svg` | full lockup on the opaque ink plate (`#0A0D0A`) — one asset, both themes | README header and docs where the surface theme is unknown |
| `icon.svg` | symbol on its own plate (no wordmark) | app launcher source, avatars |
| `icon-mono.svg` | one-ink version of the symbol (`currentColor`) | themed icons, stamps, P&B |
| `favicon.svg` | minimal 2×2 matrix mark, legible at 16 px | tabs, shortcuts |
| `logo-mono.svg` | one-ink version of the full lockup (`currentColor`) | P&B, single-color print |

All lettering is vector paths — there is no `<text>` and no external font
dependency, so the files render identically anywhere.

## Palette

| Token | Hex | Role |
|---|---|---|
| Ink | `#0A0D0A` | background |
| Ink surface | `#121810` | panel gradient top |
| Lime | `#B6FF35` | primary — brackets, SP, locked slot |
| Lime Hi | `#D4FF3F` | gradient highlight |
| Lime Dim | `#6F9B22` | empty slots, small-size detail |
| Signal Yellow | `#F6E94D` | "next click" slot only |
| On-Ink | `#E9FFD9` | wordmark on dark |
| Deep Green | `#3E6B12` | brackets on light surfaces |

Light surfaces use `logo-light.svg` — never put the lime lockup on a light
background bare. When the surface theme is unknown (a README rendered by the
host), use `logo-plate.svg`: the ink plate travels with the artwork, so the
lockup stays legible on light and dark alike.

## Typography

Wordmark glyphs are custom angular paths drawn for this set (terminal
voice). Product UI type is system monospace with wide tracking for labels —
no webfont to license, nothing to download, consistent with the offline
app.

## Minimum rules

- Dark first: the mark is designed for dark, the light variant derives.
- Clear space = bracket arm width on all sides.
- Minimum sizes: full lockup 120 px wide · `icon.svg` 48 px · below 48 px
  use `favicon.svg` only.
- Never recolor outside the palette; never stretch; never put lettering
  over photography.
- Derivatives regenerate from `logo.svg` — editing a derivative file
  directly is a defect, not a shortcut.
# `@sniffy/theme`

This private workspace package owns design primitives shared by Sniffy applications. It has four explicit entry points:

- `@sniffy/theme/base.css` defines theme-independent typography, spacing, radii, shadow, and density primitives.
- `@sniffy/theme/dark.css` defines the default dark semantic palette and `color-scheme`.
- `@sniffy/theme/light.css` defines the opt-in light semantic palette and `color-scheme`.
- `@sniffy/theme/tailwind.css` maps the shared custom properties into Tailwind theme utilities.

Applications should import Tailwind first, followed by the base primitives, one explicit semantic palette, the Tailwind mapping, and shared UI styles:

```css
@import 'tailwindcss';
@import '@sniffy/theme/base.css';
@import '@sniffy/theme/dark.css';
@import '@sniffy/theme/tailwind.css';
@import '@sniffy/ui/styles.css';
```

`@sniffy/theme/tokens.css` remains a compatibility entry point that aggregates the base and dark files. Existing code may continue to use it, but new application entry points should select the files explicitly.

Applications that support both themes import dark first and light second:

```css
@import '@sniffy/theme/base.css';
@import '@sniffy/theme/dark.css';
@import '@sniffy/theme/light.css';
```

The dark palette remains the `:root`, `:host`, and `.sniffy-theme` default used by embedded applications. Docusaurus can select palettes with `[data-theme='dark']` and `[data-theme='light']`. Other Sniffy consumers can opt in explicitly with `.sniffy-theme-dark` and `.sniffy-theme-light`; a Shadow DOM host may use the same classes.

## Extension rules

- A token that expresses product identity or semantics, or is used by two or more Sniffy applications, belongs here.
- Application-specific layout and component tokens stay with that application and use an application-specific name.
- Applications may alias shared tokens and add local tokens. They must not change the meaning of a shared token or expose an application-local variable as a product-wide contract.
- A semantic palette must define the complete semantic contract verified by `theme.test.ts`; partial palettes are not supported.
- Primary text, muted text, links, control foregrounds, focus indicators, and status/domain colors must continue to satisfy the contrast matrix in `theme.test.ts`.
- The profiler and agent intentionally select only the dark palette. Applications must import `light.css` before exposing a light-theme selector.

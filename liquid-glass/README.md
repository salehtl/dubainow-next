# Liquid Glass for Paper

Companion files for the Paper page **A8 Apple · Liquid Glass** in *DubaiNow Next*.

| File | What it does |
| --- | --- |
| `live_glass.py` | Writes the live glass as Paper-safe HTML: Frost, Bezel lens, Sheen and Rim light sibling layers. It works on any surface but can only blur, brighten and saturate what's behind it. |
| `shader_glass.py` | Writes the refraction shader as one SVG layer: a filter graph doing lensing, RGB dispersion, frost, vibrancy and rim light over an embedded backdrop image. Re-run it whenever the glass moves or the backdrop changes. |
| `backdrop.swift` | Renders the golden-hour backdrop used on the refraction preview (`swift backdrop.swift out.png`). |
| `lab/` | Chromium test page. Glass A uses only CSS that Paper keeps. Glass B runs the same shader live through `backdrop-filter: url(#…)` on any surface, which Paper strips. Drag either one around. Run it with the `liquid-glass-lab` launch config (http://localhost:8765). Add `?crop=x,y,w,h` to magnify a region. |

## Why there are two versions

Paper reads the canvas behind a layer only through `backdrop-filter`. Its parser keeps blur, brightness, contrast, grayscale, hue-rotate, invert, saturate and sepia, and drops `url(#filter)`. Shaders are fixed components with an image input. SVG `<style>` and `data:` images are stripped on import. So in Paper:

- **Live glass** follows whatever is behind it, but frosts without bending.
- **Shader glass** bends light properly, but only refracts the image embedded in its own SVG.

If Paper starts accepting `backdrop-filter: url(#liquid-glass)`, the shader's filter graph works unchanged on any surface. Glass B in the lab is that case.

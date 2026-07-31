# Overrides for Minecraft 26.2

The mod's source lives once in [`shared/`](../../shared). Anything placed here at

```
overrides/<module>/src/main/<java|resources>/<same path as in shared/>
```

replaces its `shared/` twin **for this Minecraft version only** — `<module>` is
`common`, `fabric` or `neoforge`. The shared copy is excluded from compilation,
javadoc, the sources jar and resource processing, so there is never a duplicate
class. A file with no shared twin is simply added.

Use this for the handful of things 26.2 genuinely has to do differently — a mixin
whose target signature moved, an access transformer entry, a changed vanilla API
call — not as a place to fork the mod. Anything that is *not* version-specific
belongs in `shared/`, where both targets pick it up.

Check what is in effect:

```bash
gradlew.bat -p 26.2 sourceOverrides
```

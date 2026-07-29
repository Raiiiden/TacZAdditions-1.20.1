# Changelog

## 1.2.0 - 2026-07-28

Changes since 1.1.1.

### Server-owned common configuration

- Replaced the old server config with a common config that is available in singleplayer,
  dedicated-server, and client installations.
- Servers now send a complete config snapshot when a player joins. The server's values remain
  locked for that session and the player's local values are restored after disconnecting.
- Common-config reloads are sent to connected players without requiring them to reconnect.
- Moved gameplay-affecting settings into the synchronized common config, including recoil and
  recoil recovery, scope sway, gun tuck, muzzle-flash behavior, laser toggling and range, and
  fast-gun lighting thresholds.
- Kept purely cosmetic gun-movement preferences client-side.
- Added example values to gun-ID list settings so their expected syntax is visible in newly
  generated configs.

### Gun tuck gameplay

- Gun tuck is now calculated on the server as well as rendered on the client.
- Added an option for bullets to follow the raised barrel angle while the gun is tucked.
- Added an option to prevent firing after the gun passes a configurable tuck threshold.
- Client-side firing is cancelled immediately to avoid playing a shot animation or sound for a
  shot the server rejects.
- The authoritative tuck behavior applies to living TaCZ shooters, including non-player entities.

### Muzzle-flash lighting

- Dynamic-light integrations are now optional. The game can launch without either supported
  dynamic-light mod installed.
- Added reflected compatibility with AtomicStryker's Dynamic Lights to avoid hard-loading its API.
- Added optional Sodium/Embeddium Dynamic Lights integration without making it a required
  dependency.
- Muzzle flashes fall back to temporary block light when no supported dynamic-light mod is
  installed. The existing configurable block-light override for high-RPM guns remains available.
- Muzzle-flash packets now support any living shooter, allowing NPC gunfire to produce light.
- Limited muzzle-flash packet delivery to players within 128 blocks of the shooter.
- Added per-gun colored muzzle flashes through `coloredMuzzleFlashGunColors`, using entries such as
  `"example:gun_id=#FF8A33"`.
- Colored lighting requires Colorful Lighting: Sodium/Embeddium Edition on the client.
- Added compatibility for Colorful Lighting's dynamic-source and block-light color resolution.
- Fixed colored semi-automatic and burst flashes intermittently appearing white by retaining the
  RGB association long enough for Colorful Lighting to sample it without extending the visible
  flash.
- Added `silencedGunIds` for guns with integrated suppressors that do not expose TaCZ's silence
  modifier.
- Guns in `silencedGunIds` now expose TaCZ's global silence property, enabling their silenced sound
  and reduced muzzle-flash brightness while preserving TaCZ's calculated audible distance.

### Compatibility and networking

- Updated the network protocol for common-config snapshots and colored muzzle-flash packets.
- Improved optional-channel handling so packets are only sent to clients that expose the
  TacZ Additions channel.
- Updated the development and runtime baseline to MinecraftForge 47.4.20 for Minecraft 1.20.1.
- Added compatibility with Colorful Lighting's Forge constructor/runtime requirements.

### Fixes and internal improvements

- Shared gun-tuck collision and smoothing calculations between client and server to keep visual
  and authoritative behavior aligned.
- Restored local common-config values cleanly when leaving a server.
- Hardened temporary dynamic-light lifetime handling so expired lights do not become stuck.
- Prevented optional dynamic-light classes from being loaded when their providing mod is absent.
- Preserved normal uncolored lighting when Colorful Lighting is not installed or a gun has no
  configured color.

### Upgrade notes

- The previous server config is no longer used. Copy any customized values into
  `taczadditions-common.toml`.
- Clients and servers should both update to 1.2.0 because the network protocol changed.
- Colored muzzle flashes need Sodium/Embeddium Dynamic Lights plus Colorful Lighting:
  Sodium/Embeddium Edition when using dynamic lighting.

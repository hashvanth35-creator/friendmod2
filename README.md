# Friend Mod

Two parts in this repo:

- **`mod/`** — the Fabric client mod. Adds a "Friends" button to the main menu
  and pause menu, shows online/offline + what world or server each friend is
  on, and lets you chat with them (with local history saved to
  `.minecraft/config/friendmod/`).
- **`server/`** — the small relay server both of your game clients connect to
  over WebSocket, so status/chat can actually reach across two different
  computers. This is what should be deployed at `friendmod.onrender.com`.

## Deploying the server to Render

1. In your Render dashboard, point the existing `friendmod.onrender.com`
   service at this repo, with **Root Directory** set to `server`.
2. Build command: `npm install`
3. Start command: `npm start`
4. Once it deploys, visiting `https://friendmod.onrender.com` in a browser
   should show `friendmod relay ok` — that confirms it's live. The mod talks
   to it over `wss://friendmod.onrender.com` (WebSocket, not plain HTTPS).

Render's free tier spins a service down after ~15 min of no traffic and takes
a few seconds to wake back up on the next connection — so the first "Friends"
button click after a while idle might take a moment to show your friend's
status.

## Building the mod

Push to GitHub — `.github/workflows/gradle-publish.yml` builds it on every
push and attaches the compiled jar to any GitHub Release you create. Grab the
jar from that Release, or from the workflow run's **Artifacts** tab for a
quick test build without cutting a release.

To build locally: JDK 25, then from the `mod/` folder:
```
gradle build
```
Jar lands in `mod/build/libs/`.

## First-time setup (both of you)

1. Install the jar in `.minecraft/mods/` (Fabric Loader required).
2. Launch once, then quit — this creates
   `.minecraft/config/friendmod/settings.json` and `friends.json`.
3. Open the "Friends" screen in-game and use **Add Friend** to add each
   other's username. That's it — no manual JSON editing needed.

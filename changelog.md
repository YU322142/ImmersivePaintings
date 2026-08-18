# 0.7.14+1.21.1 (MineAstr NeoForge integration)

* Ported the complete MineAstr image-translation bridge to NeoForge 1.21.1
* Requires optional MineAstr NeoForge 0.6.28 or newer on client and server
* Detects targeted non-colliding paintings even when the backing block owns the vanilla hit result
* Compresses full painting images below MineAstr's request limit off the client thread
* Persists up to 512 translations keyed by server painting hash and language
* Shares cached translations across motive, full-image, and thumbnail aliases
* Displays translations beside the targeted painting with MineAstr's public display API

# 0.7.13+1.21.1 (community NeoForge fix)

* Matched NeoForge painting right-click behavior to the Fabric implementation
* Reserved crouching right-click for compatibility interactions and kept normal right-click for the editor
* Replaced the stale 0.7.8-migration.2 build that still shipped 189 bundled sample-art resources
* Verified that new uploads default to visible and bundled sample artwork is absent from the final JAR

# 0.7.12+1.21.1 (community NeoForge port)

* Based on the latest upstream Minecraft 1.21.1 branch
* Ported the local image picker, URL loading, and upload-permission landing page
* Loaded and sorted screenshots before building the screen, fixing the stale initial 0 / 0 indicator
* Standardized screenshot pagination at five entries per page and retained final partial pages
* Fixed screenshot thumbnail identifier collisions and page clamping
* Fixed painting-library filtering and pagination state leaking between screens
* Made newly uploaded paintings visible by default
* Added a three-step confirmation before operator delete-all actions
* Excluded upstream sample artwork from the NeoForge JAR while preserving external data packs
* Retained the public full-resolution painting cache accessor for compatibility consumers
* Added fork attribution, third-party notices, and the TwelveMonkeys 3.12.0 BSD-3-Clause license to the binary distribution
* Kept MineAstr compatibility out of NeoForge because the 1.21.11 bridge is Fabric-only

# 0.7.8

* Fixed odd resolution filter behavior
* Fixed identical images but different settings colliding
* Added `enableBundledPaintings` config to disable bundled paintings

# 0.7.7

* Added gold, crimson, weather copper, and amethyst frames
* Added thin frame
* Fixed pixel art import issues
* Fixed client desync on some painting placements
* Show error message on invalid images
* Library filters are now persistent
* Fixed rare crashes

# 0.7.6

* Fixed crash introduced in 0.7.5

# 0.7.5

* Fixed a crash on weird URLs on Windows
* Synced translations

# 0.7.4

* Fixed graffiti transparency issue
* Added automatic resize of huge images
* More error handling
* Fixed pixel-multiple-deduplication being confused on small images

# 0.7.0

* Updated to Minecraft 1.21 (Thanks turtletowerz!)

# 0.6.7

* fixed visibility of hidden paintings

# 0.6.6

* Max and Low res limits are now synced to the client
* Fixed a crash when exceeding the user limit
* Adventure mode now prevents changing paintings
* Added configurable permission level for uploading

# 0.6.5

* Fixed networking related crashes

# 0.6.4

* Fixed orientation of ceiling paintings being off

# 0.6.3

* Switched to PacketByteBuf serializer

# 0.6.2

* Synced translations and retired 1.18.2

# 0.6.1

* Block picking now gives the correct item
* Items on ceilings no longer glitch into the block when dropped

# 0.6.0

* Added glow graffiti

# 0.5.1

* Fixed issue with the cache and exception spam

# 0.5.0

* Added graffiti, transparent paintings without canvas
* Fixed typos
* Resizing the window no longer resets the editor screen
* Fixed networking issues
* Fixed exception spam
* Fixed excessive CPU usage in editor
* Fixed black line
* Fixed some editor pixelator artifacts

# 0.4.4

* Fixed translations

# 0.4.3

* Ported to stupid 1.19.3
* Synced translations from Crowdin

# 0.4.3

* Switched to PacketByteBuf serializer

# 0.4.2

* Fixed bugs introduced by 4.4.1 causing play paintings to not being synced
* OPs can always see player paintings
* Fixed admin delete gui being empty on 1.18.2 and 1.19.2

# 0.4.1

* Fixed a crash (thanks Thaums!)
* In 1.18.2 and 1.19.2 now uses Glow Ink Sac instead of Glowstone Dust for glowing paintings

# 0.4.0

* Added glowing paintings
* Added config to hide other players paintings
* Added permissions for server ops to delete other paintings
* Added option to hide a specific image from other players
* You can now place floor and wall paintings within the same block

# 0.3.1

* Fixed crash on Turkish system locale

# 0.3.0

* Improved networking performance, images up- and download now faster
* Paintings can now be put on the floor and ceiling
* Added Vietnamese (thanks baooduy)
* Added German
* Fixed crashes when joining a Paper/Bukkit server using Fabric
* Max resolution has been increased to 256, configurable
* Removed two broken images
* Fixed various issues

# 0.2.0

* Fixed server crashes
* Added compatibility for Joy of Painting

# 0.1.7

* Fixed image moving by a block
* Fixed resolution issue on certain FOVs

# 0.1.6

* Fixed wrong location for paintings on Forge
* Fixed a crash in editor

# 0.1.5

* Fixed crash on URLs (this time for real)

# 0.1.4

* Fixed crash on URLs
* Added Simplified Chinese translation
* Added Korean translation

# 0.1.3

* Ported to 1.19
* Ported to 1.16.5
* Fixed missing config on some servers
* Fixed issue when using different Java versions for server and client

# 0.1.2

* Fixed crash when supplying invalid images
* Fixed GIFs and some PNGs
* Offloaded screenshot loading and image processing to a thread
* Fixed missing translation

# 0.1.1

* Fixed crash on 32 KB payloads

* # 0.1.0

* Initial release

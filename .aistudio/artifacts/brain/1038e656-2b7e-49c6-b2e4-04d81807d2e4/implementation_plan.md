# Implementation Plan: MX Player Suite

Recreate the complete, authentic MX Player Android experience as documented across the 8 user reference screenshots, featuring the multi-folder media library, gesture-driven cinema video player, MX Share file transfer utility, quick playback controls side sheet, and dynamic Light/Dark theming.

---

## 1. Architecture & Design Direction

- **Framework**: Jetpack Compose with Material 3, dynamic theme switching (Dark & Light mode support).
- **Navigation Structure**:
  - Modal Navigation Drawer (Screenshot 4: App Theme, Local Music, MX Share, Private Folder, Equalizer, Network Stream, Local Player Settings).
  - Bottom Navigation Bar (Screenshot 3: **Local** [Folders], **Video** [All Videos], **Music** [Audio Tracks], **Me** [Settings & Theme]).
  - Fullscreen Gesture Player (Screenshots 1, 2, 7, 8) with immersive edge-to-edge landscape & portrait support.

---

## 2. Key Modules & Screens to Implement

### A. Local / Folders Media Library (`LocalFoldersScreen.kt` - Screenshots 3, 6, 8)
- **Top App Bar**:
  - Title "Folders", search action, grid/list view toggle, and 3-dot overflow menu.
  - Hamburger icon to open Navigation Drawer.
- **WhatsApp Status Saver Banner**:
  - One-tap banner to browse and save downloaded video statuses.
- **Continue Watching Section**:
  - Horizontal carousel showing recent media thumbnails, season/episode labels, watched timestamps, and red progress indicators.
- **Folder List & Grid**:
  - Folders with item count badges: `Download (1 video)`, `Video (8 videos)`, `Camera`, `Movies`, `Family Videos`, `Best Hindi TV Shows`.
  - Expandable into video lists with thumbnail, resolution badge (FHD, 4K, 8K), duration, and file size.
- **View Mode & Sort Dialog (Screenshot 6)**:
  - View Mode: `All Folders`, `Folders`, `Files`.
  - Layout: `List`, `Grid`.
  - Sort Criteria: `Title`, `Date`, `Played Time`, `Status`, `Length`, `Size`, `Resolution`.
  - Order: `A to Z`, `Z to A`.
- **Floating Action Play Button**: Quick-resume FAB at the bottom right.

---

### B. Ultra HD Gesture-Controlled Video Player (`VideoPlayerScreen.kt` - Screenshots 1, 2, 7, 8)
- **Interactive Gestures (Screenshot 8 / Gestures Guide)**:
  - **Left-Edge Vertical Swipe**: Adjust screen brightness (0%–100%) with HUD popup indicator.
  - **Right-Edge Vertical Swipe**: Adjust volume (0%–200% with Volume Boost) with HUD popup indicator.
  - **Horizontal Center Swipe**: Seek forward/backward with real-time `[+00:15 / 05:49]` HUD indicator.
  - **Pinch-to-Zoom**: Multi-touch zoom and aspect ratio scaling (Fit, Stretch, 100%, Zoom to Fill).
  - **Double-Tap**: Quick Play / Pause toggle with animated ripple indicator.
- **Player Overlays & Controls (Screenshots 1 & 2)**:
  - **Top Bar**: Back button, Media title (`Movie Cut`), HW/HW+ decoding badge (toggle between HW+, HW, SW), Audio track selector, Subtitle track selector (.srt/.vtt), Cast button, Overflow menu.
  - **Floating Action Pills (Screenshot 2)**:
    - Equalizer quick-access.
    - Playback Speed (`0.5x`, `0.75x`, `1.0x`, `1.25x`, `1.5x`, `2.0x`).
    - Video Cutter / Trimmer tool.
    - Screen Rotation lock.
    - Background Play / Headphones mode.
  - **Bottom Timeline & Controls (Screenshot 1)**:
    - Current time (`05:49`), interactive seekbar with preview scrubber, total duration (`42:35`).
    - Screen Lock padlock button (locks touch controls against accidental touches).
    - 10-second Rewind (`10`), Previous track (`|<`), Play/Pause (`||`), Next track (`>|`), 10-second Forward (`10`), Fullscreen / Aspect Ratio expand button.

---

### C. Player Settings Side Sheet (`PlayerSettingsSheet.kt` - Screenshot 7)
- Slide-in side drawer during video playback with checkbox toggles:
  - `Screen Rotation`
  - `Background Play`
  - `Mute`
  - `Equalizer`
  - `Sleep Timer` (15m, 30m, 45m, 60m)
  - `Night Mode`
  - `Screenshot` (captures current video frame)
  - `Vertical Flip` & `Mirror Mode`
  - `Playback Speed`
  - `Loop` & `Shuffle`
  - `A - B Repeat`
  - `Audio Effect` & `Customise Items`

---

### D. MX Share File Transfer (`MxShareScreen.kt` - Screenshot 5)
- **Send & Receive Action Hub**:
  - Prominent Green **SEND** button and Purple **RECEIVE** button.
  - Share with devices: `PC`, `iOS`, `Android`, `KaiOS`.
- **Live Transfer Progress Sheet**:
  - Transfer status: `Sent: 108 MB`, `Time Left: 120s`, `Files Completed: 3/14`.
  - Animated sharing progress bar and connection details (`ME -> L58041: 3 files, 3.6GB`).
  - Transfer history logs.

---

### E. Navigation Drawer & Settings (`NavDrawerContent.kt` - Screenshot 4)
- Header with MX Player logo and account status.
- Menu Options:
  - `App Theme` (Toggle between Light Mode & Dark Mode).
  - `App Language` (English, Hindi, Spanish, etc.).
  - `Local Music` (NEW badge).
  - `File Transfer (MX Share)` (NEW badge).
  - `Private Folder` (Secure password/PIN protected folder).
  - `Equalizer` (10-band graphic equalizer with Bass Boost & 3D Surround).
  - `Video Playlists` (Custom user playlists).
  - `Network Stream` & `Local Network (SMBv2)`.
  - `Local Player Settings`.

---

## 3. Verification & Validation Plan
1. **Compilation Check**: Run `compile_applet` to ensure zero compilation or syntax errors.
2. **Gesture Interaction Test**: Verify vertical brightness/volume gestures, horizontal seek, double-tap, and pinch zoom HUDs.
3. **Library & Navigation Test**: Validate folder grid/list toggles, Continue Watching carousel, Sort/Filter modal, and Drawer navigation.
4. **Theme Switcher Test**: Toggle between Dark Theme and Light Theme smoothly across all screens.

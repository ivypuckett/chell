# Chell

An Android home-screen launcher. A paged app drawer, a favourites row, search,
and drag-and-drop reordering between the two.

Status: experiment. Nothing is released and there is no versioning.

```bash
task setup:sdk    # one-time: install the Android SDK (no root needed)
task setup:avd    # one-time: create the emulator image
task emu          # start the emulator
task run          # build, install, set as home, and show it
task test         # unit tests
```

See [CLAUDE.md](CLAUDE.md) for the toolchain and build details.

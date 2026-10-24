# Changelog

## 0.1.1
- Fix: turning into a bat crashed the game ("Pose stack not empty") alongside mods that move the player model in
  RenderPlayerEvent (VoidClip 2.3.0 did). Bat form now hides the player before any other mod sees the render.
- Tackled mobs are only tilted over when their render can't be cancelled after us.
- Compat tests now run against VoidClip 2.3.1 and Chromeflesh 0.2.0.

## 0.1.0
- First version: neck-bite infection with a turning phase (thirst, sun sickness, red vision, heartbeat) and two cures.
- Blood instead of hunger, sun burning, skill tree with ten skills (speed, dash, mist step, night eyes, bat form,
  fangs, regeneration, blood sense, hypnosis, sun ward).
- Nightwalker: a vampire mob that circles behind you to bite.
- Chromeflesh: Bloodrush. liminal.: tackling Bacteria.

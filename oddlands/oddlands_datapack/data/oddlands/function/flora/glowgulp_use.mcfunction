# Give it a poke: it gulps, glows, and shares a little night vision.
playsound minecraft:entity.player.burp block @a ~ ~ ~ 0.8 0.5
particle minecraft:glow ~ ~0.4 ~ 0.25 0.25 0.25 0.02 14
effect give @s minecraft:night_vision 30 0 true

setblock ~ ~ ~ oddlands:wishing_water
particle minecraft:wax_off ~ ~0.6 ~ 0.6 0.4 0.6 0 40
particle minecraft:glow ~ ~0.6 ~ 0.5 0.6 0.5 0 30
playsound minecraft:block.amethyst_block.chime block @a ~ ~ ~ 1.5 0.6
playsound minecraft:block.bubble_column.upwards_inside block @a ~ ~ ~ 1 0.8
advancement grant @a[distance=..16] only oddlands:wishing_well

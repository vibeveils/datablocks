# as the player: step out beside the well, facing it
execute at @e[type=minecraft:marker,tag=od.arrival,distance=..600,limit=1,sort=nearest] run tp @s ~2 ~ ~ facing ~ ~0.6 ~
kill @e[type=minecraft:marker,tag=od.arrival,distance=..600]
tag @s remove od.travelling
scoreboard players set @s od.cd 60
effect clear @s minecraft:blindness
effect give @s minecraft:slow_falling 3 0 true
execute at @s run playsound minecraft:block.amethyst_block.resonate player @a ~ ~ ~ 1 0.7
execute at @s run particle minecraft:glow ~ ~1 ~ 0.6 1 0.6 0 30
execute if dimension oddlands:oddlands run title @s actionbar {"text":"The Oddlands","color":"light_purple","italic":true}

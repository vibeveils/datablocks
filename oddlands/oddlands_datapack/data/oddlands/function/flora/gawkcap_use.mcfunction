execute unless items entity @s weapon.mainhand minecraft:bone_meal run return run playsound minecraft:entity.frog.ambient block @a ~ ~ ~ 0.6 1.8
execute unless entity @s[gamemode=creative] run clear @s minecraft:bone_meal 1
particle minecraft:happy_villager ~ ~0.2 ~ 0.3 0.3 0.3 0 8
execute store result score #roll od.tmp run random value 1..3
execute unless score #roll od.tmp matches 1 run return 0
setblock ~ ~ ~ minecraft:air
execute store success score #ok od.tmp run place feature oddlands:giant_gawkcap ~ ~ ~
execute if score #ok od.tmp matches 0 run setblock ~ ~ ~ oddlands:gawkcap

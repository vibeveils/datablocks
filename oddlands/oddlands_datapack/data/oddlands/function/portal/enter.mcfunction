# Run by the wishing_water block's "entity_inside" function hook:
# as the entity inside the portal, positioned at the portal block's centre.
execute unless entity @s[type=minecraft:player] run return 0
execute if entity @s[tag=od.travelling] run return 0
execute if score @s od.cd matches 1.. run return 0
scoreboard players set @s od.cd 100
execute store result score @s od.tx run data get entity @s Pos[0]
execute store result score @s od.tz run data get entity @s Pos[2]
execute if dimension oddlands:oddlands run scoreboard players set @s od.dest 2
execute unless dimension oddlands:oddlands run scoreboard players set @s od.dest 1
scoreboard players set @s od.wait 0
tag @s add od.travelling
effect give @s minecraft:blindness 4 0 true
effect give @s minecraft:resistance 15 4 true
playsound minecraft:entity.player.splash.high_speed player @a ~ ~ ~ 1 0.6
playsound minecraft:block.portal.travel player @s ~ ~ ~ 0.25 1.8
function oddlands:portal/store_target
execute if score @s od.dest matches 1 run function oddlands:portal/go_oddlands with storage oddlands:tmp
execute if score @s od.dest matches 2 run function oddlands:portal/go_overworld with storage oddlands:tmp

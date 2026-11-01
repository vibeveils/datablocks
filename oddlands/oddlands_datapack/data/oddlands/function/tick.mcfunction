scoreboard players remove @a[scores={od.cd=1..}] od.cd 1
scoreboard players remove @a[scores={od.puff=1..}] od.puff 1
# A copper nugget landing in still water: is it a finished well?
execute as @e[type=item,tag=!od.checked] if items entity @s contents minecraft:copper_nugget at @s if block ~ ~ ~ minecraft:water run function oddlands:portal/try_activate
# Players falling through a well wait here until the far side has loaded.
execute as @a[tag=od.travelling] at @s run function oddlands:portal/travel_step

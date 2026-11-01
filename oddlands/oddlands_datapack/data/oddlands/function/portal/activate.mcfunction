# as: the nugget item. Light the well, then use up one nugget.
execute align xyz positioned ~0.5 ~ ~0.5 run function oddlands:portal/light_up
execute store result score #count od.tmp run data get entity @s Item.count
execute if score #count od.tmp matches ..1 run return run kill @s
execute store result entity @s Item.count int 1 run scoreboard players remove #count od.tmp 1
tag @s remove od.checked

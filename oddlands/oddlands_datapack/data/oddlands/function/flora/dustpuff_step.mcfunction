# step_on fires every tick; only poof once a second per entity
execute if score @s od.puff matches 1.. run return 0
scoreboard players set @s od.puff 20
particle minecraft:white_ash ~ ~0.3 ~ 0.4 0.2 0.4 0.02 24
particle minecraft:poof ~ ~0.2 ~ 0.2 0.1 0.2 0.01 6
playsound minecraft:block.wool.break block @a ~ ~ ~ 0.6 1.7

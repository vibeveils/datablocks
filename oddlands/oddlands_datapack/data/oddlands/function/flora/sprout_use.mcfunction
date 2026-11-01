# "use" as the player: bone meal speeds it up
execute unless items entity @s weapon.mainhand minecraft:bone_meal run return 0
execute unless entity @s[gamemode=creative] run clear @s minecraft:bone_meal 1
particle minecraft:happy_villager ~ ~0.2 ~ 0.3 0.3 0.3 0 8
playsound minecraft:item.bone_meal.use block @a ~ ~ ~
execute store result score #roll od.tmp run random value 1..2
execute if score #roll od.tmp matches 1 run function oddlands:flora/sprout_grow

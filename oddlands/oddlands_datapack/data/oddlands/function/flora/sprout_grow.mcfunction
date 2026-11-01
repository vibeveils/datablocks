setblock ~ ~ ~ minecraft:air
execute store success score #ok od.tmp run place feature oddlands:bulbwood_tree ~ ~ ~
execute if score #ok od.tmp matches 0 run setblock ~ ~ ~ oddlands:wobble_sprout
execute if score #ok od.tmp matches 1 run playsound minecraft:block.slime_block.place block @a ~ ~ ~ 1 0.6

# positioned at the ground block under the target column: the well's foundation replaces it
execute if dimension oddlands:oddlands positioned ~-2 ~ ~-2 run place template oddlands:well/oddlands ~ ~ ~
execute unless dimension oddlands:oddlands positioned ~-2 ~ ~-2 run place template oddlands:well/overworld ~ ~ ~
summon minecraft:marker ~0.5 ~1 ~0.5 {Tags:["od.arrival"]}
return 1

# positioned at the centre of the water block. Returns 1 for a finished well:
#   y+0: water source ringed by 8 #oddlands:well_rim blocks
#   y+1, y+2: #oddlands:well_posts on the 4 corners
#   y+3: 3x3 roof of #oddlands:well_roof (slabs)
execute unless block ~ ~ ~ minecraft:water[level=0] run return 0
execute unless block ~1 ~ ~ #oddlands:well_rim run return 0
execute unless block ~-1 ~ ~ #oddlands:well_rim run return 0
execute unless block ~ ~ ~1 #oddlands:well_rim run return 0
execute unless block ~ ~ ~-1 #oddlands:well_rim run return 0
execute unless block ~1 ~ ~1 #oddlands:well_rim run return 0
execute unless block ~1 ~ ~-1 #oddlands:well_rim run return 0
execute unless block ~-1 ~ ~1 #oddlands:well_rim run return 0
execute unless block ~-1 ~ ~-1 #oddlands:well_rim run return 0
execute unless block ~1 ~1 ~1 #oddlands:well_posts run return 0
execute unless block ~1 ~1 ~-1 #oddlands:well_posts run return 0
execute unless block ~-1 ~1 ~1 #oddlands:well_posts run return 0
execute unless block ~-1 ~1 ~-1 #oddlands:well_posts run return 0
execute unless block ~1 ~2 ~1 #oddlands:well_posts run return 0
execute unless block ~1 ~2 ~-1 #oddlands:well_posts run return 0
execute unless block ~-1 ~2 ~1 #oddlands:well_posts run return 0
execute unless block ~-1 ~2 ~-1 #oddlands:well_posts run return 0
execute unless block ~-1 ~3 ~-1 #oddlands:well_roof run return 0
execute unless block ~-1 ~3 ~ #oddlands:well_roof run return 0
execute unless block ~-1 ~3 ~1 #oddlands:well_roof run return 0
execute unless block ~ ~3 ~-1 #oddlands:well_roof run return 0
execute unless block ~ ~3 ~ #oddlands:well_roof run return 0
execute unless block ~ ~3 ~1 #oddlands:well_roof run return 0
execute unless block ~1 ~3 ~-1 #oddlands:well_roof run return 0
execute unless block ~1 ~3 ~ #oddlands:well_roof run return 0
execute unless block ~1 ~3 ~1 #oddlands:well_roof run return 0
return 1

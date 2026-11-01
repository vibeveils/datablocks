scoreboard players remove #steps od.tmp 1
execute if score #steps od.tmp matches ..0 run return 0
execute unless block ~ ~ ~ #oddlands:scan_passable run return run function oddlands:portal/build_here
execute positioned ~ ~-1 ~ run return run function oddlands:portal/find_ground

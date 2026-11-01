scoreboard players remove #steps od.tmp 1
execute if score #steps od.tmp matches ..0 run return 0
execute if block ~ ~ ~ oddlands:wishing_water run return run function oddlands:portal/mark_here
execute positioned ~ ~-1 ~ run return run function oddlands:portal/find_portal

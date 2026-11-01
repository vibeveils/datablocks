# positioned at the top of the target column. 1) look for a well portal already in this column
scoreboard players set #steps od.tmp 400
execute store result score #found od.tmp run function oddlands:portal/find_portal
execute if score #found od.tmp matches 1.. run return run function oddlands:portal/finish
# 2) otherwise build a well on the first solid ground, 3) or on a fresh islet if the column is open void
scoreboard players set #steps od.tmp 400
execute store result score #found od.tmp run function oddlands:portal/find_ground
execute if score #found od.tmp matches 0 run function oddlands:portal/build_island
function oddlands:portal/finish

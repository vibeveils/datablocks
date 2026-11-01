# as/at a travelling player, already in the destination dimension above the target column.
scoreboard players add @s od.wait 1
function oddlands:portal/store_target
function oddlands:portal/hold with storage oddlands:tmp
execute if score @s od.wait matches 600.. run return run function oddlands:portal/give_up
function oddlands:portal/try_arrive with storage oddlands:tmp

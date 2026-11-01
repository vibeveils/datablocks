# The far side never loaded (30 s). Land them anyway.
execute if dimension oddlands:oddlands positioned ~ 319 ~ run function oddlands:portal/build_island
execute if dimension oddlands:oddlands run return run function oddlands:portal/finish
tag @s remove od.travelling
effect give @s minecraft:slow_falling 30 0 true

# as: the copper nugget item, at: its position (inside a water block)
tag @s add od.checked
execute align xyz positioned ~0.5 ~ ~0.5 unless function oddlands:portal/is_well run return run playsound minecraft:entity.generic.splash neutral @a ~ ~ ~ 0.3 1.8
function oddlands:portal/activate

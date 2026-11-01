# random_tick on a wobble sprout: about 1 in 3 ticks tries to grow
execute store result score #roll od.tmp run random value 1..3
execute if score #roll od.tmp matches 1 run function oddlands:flora/sprout_grow

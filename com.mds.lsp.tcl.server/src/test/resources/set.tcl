set greeting hello
set greeting ;# ->hello
set person(name) bob
set person(name) ;#-> bob
set (name) bob ;# the is an array variable, where the array name is the empty string
set (name) ;#-> bob
set {} hello
set {} ;#->hello


set X "This is a string"

set Y 1.24

puts $X
puts $Y

puts "..............................."

set label "The value in Y is: "
puts "$label $Y"
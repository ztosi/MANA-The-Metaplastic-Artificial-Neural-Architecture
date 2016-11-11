file(REMOVE_RECURSE
  "../../bin/MANA_Net-cpu.pdb"
  "../../bin/MANA_Net-cpu"
)

# Per-language clean rules from dependency scanning.
foreach(lang CXX)
  include(CMakeFiles/MANA_Net-cpu.dir/cmake_clean_${lang}.cmake OPTIONAL)
endforeach()

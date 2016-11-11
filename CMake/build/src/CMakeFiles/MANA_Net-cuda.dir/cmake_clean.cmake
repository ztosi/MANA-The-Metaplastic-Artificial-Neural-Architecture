file(REMOVE_RECURSE
  "../../bin/MANA_Net-cuda.pdb"
  "../../bin/MANA_Net-cuda"
)

# Per-language clean rules from dependency scanning.
foreach(lang CXX)
  include(CMakeFiles/MANA_Net-cuda.dir/cmake_clean_${lang}.cmake OPTIONAL)
endforeach()

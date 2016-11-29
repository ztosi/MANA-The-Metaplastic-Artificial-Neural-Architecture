#include <arrayfire.h>
#include <vector>
#include <cstdint>
#include "../include/Neuron.hpp"
#include "../include/HPComponent.hpp"
#include "../include/IPComponent.hpp"
#include "../include/STDP.hpp"
#include "../include/SynMatrices.hpp"
#include "../include/UDFPlasticity.hpp"



int main () {

    Network *net = new Network();
    net->create_MANA_Mod(100, Position(0, 0, 0), Position(100, 100, 100));

}
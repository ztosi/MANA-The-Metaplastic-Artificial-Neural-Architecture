#include <arrayfire.h>
#include <vector>
#include <cstdint>
#include <stdio.h>
#include "../include/Neuron.hpp"
#include "../include/HPComponent.hpp"
#include "../include/IPComponent.hpp"
#include "../include/STDP.hpp"
#include "../include/SynMatrices.hpp"
#include "../include/UDFPlasticity.hpp"

using namespace af;

int main(int argc, char *argv[])
{
    try {

        // Select a device and display arrayfire info
        int device = argc > 1 ? atoi(argv[1]) : 0;
        af::setDevice(device);
        af::info();
        std::cout << "Begin Module Construction in Network" << '\n';
        std::cout << "Start" << '\n';
        Network* net = new Network();
        net->create_MANA_Mod(100, Position(0, 0, 0), Position(100, 100, 100));
        net->runForward();
        std::cout << "End" << '\n';
    } catch(af::exception& e) {

        fprintf(stderr, "%s\n", e.what());
        throw;
    }
    return 0;
}
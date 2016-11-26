#include <cstdint>
#include <math.h>
#include <arrayfire.h>

#ifndef UTILS_H_
#define UTILS_H_

using namespace af;

class Position
{

public:
    Position()
        : x(0)
        , y(0)
        , z(0)
    {
    }

    Position(const float _x, const float _y, const float _z)
        : x(_x)
        , y(_y)
        , z(_z)
    {
    }

    float getX() const
    {
        return x;
    }
    float getY() const
    {
        return y;
    }
    float getZ() const
    {
        return z;
    }

    static float euclidean(const Position& p1, const Position& p2)
    {
        return sqrt(pow(p1.x - p2.x, 2) + pow(p1.y - p2.y, 2) + pow(p1.z - p2.z, 2));
    }

    static float
    euclidean(const float& x1, const float& y1, const float& z1, const float& x2, const float& y2, const float& z2)
    {
        return sqrt(pow(x1 - x2, 2) + pow(y1 - y2, 2) + pow(z1 - z2, 2));
    }

    // Crude but quick...
    static double rand_float(double low, double high)
    {
        return ((double)rand() * (high - low)) / (double)RAND_MAX + low;
    }

private:
    float x;
    float y;
    float z;
};


class Utils
{
public:
    static array srcInd2dlysrcInd(const uint32_t srcSize, const array& srcInds, const array& dlys)
    {
        return dlys * srcSize + srcInds;
    }

    static array lin2IJ(const array& linInds, const uint32_t _srcSz)
    {
        array ijInds = constant(0, linInds.dims(0), 2, u32);
        ijInds(span, 1) = floor(linInds / _srcSz);
        ijInds(span, 0) = linInds % _srcSz;
        return ijInds;
    }

    static array findStAndEnds(const array& ijInds)
    {
        uint32_t* js = ijInds.col(1).host<uint32_t>();
        std::vector<uint32_t> start;
        std::vector<uint32_t> inDeg;
        uint32_t current = js[0];
        start.push_back(0);
        uint32_t inDc = 1;
        for(uint32_t i = 1; i < ijInds.col(1).dims(0); i++) {
            if(js[i] != current) {
                start.push_back(i);
                inDeg.push_back(inDc);
                current = js[i];
                inDc = 0;
            } else {
                inDc++;
            }
        }
        inDeg.push_back(inDc);
        array stArr(start.size(), 1, start.data(), afHost);
        array indArr(start.size(), 1, inDeg.data(), afHost);
        indArr = indArr + stArr - 1;
        array stAndInDs = constant(0, start.size(), 2, u32);
        stAndInDs(span, 0) = stArr;
        stAndInDs(span, 1) = indArr;
        delete[] js;
        return stAndInDs;
    }
};

class DataRecorder
{

    // TODO: Everybody is friends with this class, which will be used to record all data
};

#endif // UTILS_H_
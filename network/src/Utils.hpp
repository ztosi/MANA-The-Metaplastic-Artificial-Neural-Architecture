#include <cstdint>
#include <math.h>

#ifndef UTILS_H_
#define UTILS_H_


class Position {

	public:

		const float x;
		const float y;
		const float z;

		Position(	const float _x,
					const float _y,
					const float _z	)
			: x(_x), y(_y), z(_z) {}

		float euclidean (	const Position &p1,
							const Position &p2	)
		{
			return sqrt(pow(p1.x - p2.x, 2) 
				+ pow(p1.y - p2.y, 2) 
				+ pow(p1.z - p2.z, 2));
		}

		float euclidean (	const float &x1,
							const float &y1,
							const float &z1,
							const float &x2,
							const float &y2,
							const float &z2)
		{
			return sqrt(pow(x1 - x2, 2) 
				+ pow(y1 - y2, 2) 
				+ pow(z1 - z2, 2));
		}

		bool operator<(const Position &lhs, const Position &rhs)
		{
			if (lhs.x == rhs.x) {
				if (lhs.y == rhs.y) {
					if(lhs.z == rhs.z) {
						return 0;
					} else {
						return lhs.z < rhs.z;
					}
				} else {
					return lhs.y < rhs.y;
				}
			} else {
				return lhs.x < rhs.x;
			}
		} 
};

class DataRecorder {

	//TODO: Everybody is friends with this class, which will be used to record all data

};


#endif // UTILS_H_
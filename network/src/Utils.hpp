#include <cstdint>

#ifndef UTILS_H_
#define UTILS_H_


class Position {

	public:

		const float x;
		const float y;
		const float z;

		Position(const float _x, const float _y, const float _z)
			: x(_x), y(_y), z(_z)
		{}

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
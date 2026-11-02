# SEAS

The `se.vti.seas` package implements a simple a model of vessel traffic in a fairway. The model considers unidirectional traffic and can be applied to bidirectional flows by modeling each direction separately. Differently from multi-lane road traffic, there may not be lateral space constraints (limitation on the number of lanes) – vessels may spread out laterally. The model computes the required fairway geometry occupied by the vessels. A major current simplification is that vessel speeds are assumed to be constant, such that lateral evasive/overtaking maneuvers are the only way to resolve conflicts between vessels moving in the same direction at different speeds.

Contact: gunnar.flotterod@vti.se


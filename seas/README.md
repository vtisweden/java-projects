# SEAS

The `se.vti.seas` package implements a simple a model of vessel traffic in a fairway. The model considers unidirectional traffic and can be applied to bidirectional flows by modeling each direction separately. Differently from multi-lane road traffic, there may not be lateral space constraints (limitation on the number of lanes) – vessels may spread out laterally. The model computes the required fairway geometry occupied by the vessels. A major current simplification is that vessel speeds are assumed to be constant, such that lateral evasive/overtaking maneuvers are the only way to resolve conflicts between vessels moving in the same direction at different speeds.

This program has been developed as part of the Trafikverket-funded research project "Säkerhetsaspekter i samhällsekonomiska analyser för sjötransporter".
* A description of the model underlying this program is available here: [A simple model of lateral congestion in fairways](./docs/2026-11-01_lateral-congestion-in-fairways.pdf)
* The complete project report can be found here: [Säkerhetsaspekter i samhällsekonomiska analyser för sjötransporter: Andra delrapporten i SEAS-projektet](https://vti.diva-portal.org/smash/record.jsf?pid=diva2%3A2117340&dswid=-913)

Contact: gunnar.flotterod@vti.se


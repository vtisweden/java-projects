# se.vti.samgods

*2025-08-27  This documentation is work in progress. Current version is readable but not exhaustive.*


## Outline 

The program se.vti.samgods ("VTI-samgods") approximates and extends (primarily the logistics module of) the Swedish national freight model system Samgods ("TRV-samgods"). The national model is maintained and documented by Trafikverket, see [here](https://bransch.trafikverket.se/tjanster/system-och-verktyg/Prognos--och-analysverktyg/Samgods/).

The purpose of VTI-samgods is to enable lightweight experimentation with and further developments of TRV-Samgods in an open and flexible programming environment. VTI-samgods uses ony a subset of all parameters available in TRV-Samgods. VTI-samgods does not aim to reproduce Sweden's current freight transport system at the greatest possible level of detail; focus is more on the underlying models and their possible refinements.

One motivation for writing this program was to refine the modeling of consolidation (different shipments sharing the same vehicle). The currently implemented solution is still relatively simple (and the topic of continuous improvements). Vehicles are merely assumed to travel from an origin to a destination, and to then return back to the origin. A representation of richer driving patterns, including several loading/unloading episodes along the way, is the topic of ongoing work. Currently, consolidation works best for "small" vehicles (trucks) and worst for "large" vehicles (vessels).


## Functionality

The overall program flow of se.vti.samgods is illustrated below.

![](program-flow.png)

1. **Data preparation** In-data extraced from the Samgods production version (this is a different program operated by TRV) is extracted into csv files. These files are described further below.

2. **Routing** If routes between all terminals are not yet available, they are computed and stored in a json file. If they have been computed before, they can be loaded from a json file.

3. **Shipper choices** All shippers choose the transport chains along their goods are to be transported. In the first iteration, transport costs are guessed. In all following, they are decided by the carriers.

4. **Carrier choices** Given the transportation requests from the shippers, the carriers decide how many vehicles of different classes are deployed between all pairs of terminals.

5. **Transport costs** The unit cost of transport [monetary units per ton kilometer per commodity type] is obtained by distributing the operation costs of all used vehicles over all goods transported by these vehicles.


## Terminology Transport modes, Commodities, Chains, Episodes, Legs, ...

The program considers the transport modes *Road*, *Rail*, *Sea*. A *Ferry* mode represents transport of trucks or trains over water. There also is a not yet tested *Air* mode encoded. The current version of the model uses only a rudimentary representation of consolidation, which works best for (relatively small) trucks and worst for (relatively large) vessels. Consolidation refinement is ongoing work.

The program considers the same commodities as the Samgods production version: *Agriculture*, *Coal*, *Metal*, *Food*, *Textiles*, *Wood*, *Coke*, *Chemicals*, *Othermineral*, *Basicmetals*, *Machinery*, *Transport*, *Furniture*, *Secondaryraw*, *Timber*. The *Air* commodity is also encoded but not tested.

Transport happens along *transport chains*, which are composed of *transport episodes*, which in turn may be composed of *transport segments*.

*Transport chain* connects a producer (sender) to a consumer (receiver). It is defined by a *commodity*, if it uses *containers* or not, and by one or more *transport episodes*. Implemented in `se.vti.samgods.lgistics.TransportChain`.

*Transport episodes* connects a producer or a terminal to a consumer or a terminal, using a unique *transport mode*. A *transport episode* is defined by (i) its parent *transport chain*, (ii) its *transport mode*, (iii) the transport segments it contains.

The figure below illustrates a transport chain that consists of three transport episodes, using the modes Road/Rail/Road.

![](road-rail-road-chain.png)

During a *Transport segment*, the load of a vehicle does not change. In the majority of cases, a *transport episode* consists of a single *transport segment*. This also holds when trucks or trains are moved on a ferry -- their load does not change. The only exception are rail segments with intermediate marshalling, where the waggons of a train may be reassembled into a new train. Here the content of a waggon does not change, but the train containing the waggon does.

The figure below provides an example including a ferry and a marshalling episode:

![](road-rail-road-with-transfers-chain.png)

All consolidation is modeled within transport segments (details further below) because this couples a unique vehicle configuration to a unique load, allowing to distribute the vehicle operation etc. cost over its load for the computation of transport prices.


## Network

The network is represented by a node csv file and a link csv file. These files represent tables in the Samgods database; fields definitions are available from the Samgods production version documentation. Unavailable fields are left blank; no "magic numbers" are used.

The file formats indicated below are used for compatibility with the production version of Samgods. The program-internal datastructures representing this information can be populated without using these files. However, this requires coding in the Java environment.

An example node file is given below:

```
OBJECTID,N,X,Y,NORIG,SCBSTANN,ID_COUNTRY,ID_REGION,MODE_N,UI4,GEOMETRYSOURCE
1,1,665500,6600720,711400,114,1,114,0,0,1
2,2,665760,6599340,711401,114,1,114,1,0,1
3,3,663410,6600000,711402,114,1,114,1,0,1
...
```

An example link file is given below:

```
OBJECTID,A,B,SHAPE_Length,MODESTR,SPEED_1,SPEED_2,CATEGORY,FUNCTION,NLANES,UL2,UL3,GEOMETRYSOURCE,MODE
1,1,2049,264.7640458828415,xabc,50.0,50.0,110.0,81.0,1.0,0.26,0.0,1.0,1,Road
2,2,2028,1635.1758315239374,xabc,50.0,50.0,201.0,81.0,1.0,1.64,0.0,1.0,1,Road
3,3,2039,899.4442728707544,xabc,50.0,50.0,201.0,81.0,1.0,0.9,0.0,1.0,1,Road
...
```

The `se.vti.samgods.network` package contains network-related functionality. This comprises `NetworkReader.java` for loading the node and link tables, and `Router.java` for route computation. The router is parallelized but still takes a while to process an all-of-Sweden network; hence the option to store routes for re-use in a json file.


## Vehicles

The vehicle fleet is represented by one pair of csv files per transport mode.

The file formats indicated below are used for compatibility with the production version of Samgods. The program-internal datastructures representing this information can be populated without using these files. However, this requires coding in the Java environment.

The vehicle parameters file that defines the technical properties of the vehicle fleet for a given mode. An example vehicle parameter file for the Road transport mode is given below:

```
OBJECTID,ID,VEH_NR,DESCRIPTIO,LABEL,VESSELTYPE,CAPACITY,COORFACT,HOURS_COST,KM_COST,ONFER_H_C,ONFER_KM_C,POSICOST,DFLTFREQ,F_DUES_VH,F_DUES_TON,SPEED,VDF_SPEC,MODE_1,MODE_2,FUNC_FILE,EMPTY_V,MAX_SPEED
1,1.0,101,Lorry light LGV.< 3.5 ton,LGV3,0.0,2.0,1.0,369.0,3.03538,119.37548,0.10939,0.0,84.0,0.0,0.0,116.0,61.0,c,-,V101,1,116.0
2,2.0,102,Lorry medium 16 ton,MGV16,0.0,9.0,1.0,340.956,5.40375,155.84244,0.41451,0.0,84.0,0.0,0.0,116.0,62.0,a,-,V102,1,116.0
3,3.0,103,Lorry medium 24 ton,MGV24,0.0,15.0,1.0,340.956,6.88996,178.86268,0.69935,0.0,84.0,0.0,0.0,116.0,63.0,a,-,V102,1,116.0
4,
```

The transfer parameter file defines the cost of using the vehicles for a given transport mode. An example transfer paramter file for the Road transport mode is given below:

```
OBJECTID,ID,ID_COM,VEH_NR,CONT_LTI,CONT_LCO,CONT_LTI_T,CONT_LCO_T,NC_LTI,NC_LCO,NC_LTIT,NC_LCOT
1,1.0,1,101,,,,,0.25,10.60576,0.25,1.06058
2,2.0,1,102,,,,,0.25,10.60576,0.25,1.06058
3,3.0,1,103,,,,,0.25,10.60576,0.25,1.06058
```

The `se.vti.transportation.fleet` package contains vehicle fleet related functionality, primarily the `VehiclesReader.java` for loading the vehicle tables.


## Transport demand

The transport demand is represented by one csv table per commodity type. These files are the *output* of a run of the Samgods production model, which already contains an assignment of shipments to transport chains. This information is used to (i) identify nodes where the legs/episodes within a chain may be connected, and (ii) to sum up the total freight demand in one sender/receiver relation. 

The file formats indicated below are used for compatibility with the production version of Samgods. The program-internal datastructures representing this information can be populated without using these files. However, this requires coding in the Java environment.

An example file for a single transport commodity is given below:

```
Key	NRelations	AnnualVolume_(Tonnes)	Prob	ShipmentFreq_(per_year)	TransportCosts_(SEK)	AllCosts_(SEK)	MargCosts_(SEK)	ChainType	SubCell	Orig	Dest	VhclType1	NrVhcls1	Orig2	VhclType2	NrVhcls2	Orig3	VhclType3	NrVhcls3	Orig4	VhclType4	NrVhcls4	Orig5	VhclType5	NrVhcls5
1	1	1.24556	0.7267	1	62.5	1858.1	0.0	C	7	711400	711500	104	0.083037
1	1	1.24556	0.2733	1	228.1	2023.7	0.0	A	7	711400	711500	104	0.083037
2	1	0.90949	0.6975	1	47.4	1589.6	0.0	C	8	711400	711500	104	0.060633
...
```

The `se.vti.logistics`  package contrains transport demand related functionality, including `ChainChoiReader.java` for reading demand files from the Samgods production version.


## Shipper choices

Each sender/receiver (producer/consumer) pair of network nodes may have, per commodity, one or more shipment relations of different size. Shipper choices are modeled individually per shipment relation. They comprise (i) choice of transport chain, and (ii) choice of shipment size (and, resultingly, shipment frequency). The underlying choice model is of multinomial logit form, currently a linear-in-parameters adaptation of the model described [here](https://swopec.hhs.se/vtiwps/abs/vtiwps2024_005.htm). The choice model is implemented in the package `se.vti.samgods.logistics.choice`. This is parallelized code to evaluate the choices of many shippers simultaneously.


## Carrier choices and consolidation

The model makes the fundamental assumption that (i) vehicles move in loops and that (ii) each vehicle loop is operated by one carrier.  (An extension to carriers interacting across loops is underway.) The carrier operating a given loop which type of vehicle and how many vehicles to use to serve the transport demand arising along that loop.

The currently considered loop structure is very simple and serves as a placeholder for richer vehicle move patterns. Assume that shipments arrive at some network node for (further) transportation to some other network node. One loop is constructed for this node pair, with (i) vehicle movements and loads truly evaulated on the forward half-loop, and (ii) merely assuming that the return-half loop happens with the same vehicle loads and, hence, costs. It is illustrated below. 

![](half-loop-consolidation.png)

This is in principle similar to TRV-samgods. A difference is that VTI-samgods explicitly models the annual time-line of shipment days. In the consolidation model, this is accounted for as follows. (i) When a shipper chooses an annual shipment frequency, it is assumed that the realized number of shipments per year follows a Possion distribution with expectation equal to the chosen shipment frequency. (ii) Carriers can consolidate along the daily time line by consolidating all shipments arising within, say, one week. (iii) The probability that there at all arise shipments within a week, and the expected amount of shipments *given* that shipments arise at all are then the basis for dimensining the deployed vehicle fleet.



 






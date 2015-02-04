This is a bad implementation of a DIS to HLA RPR-FOM gateway.
It is nowhere near production quality, strictly a teaching
tool at this point. And not all that good of a teaching tool,
either; I'm not at all satisfied with the design.

This uses 

* Portico, an open source HLA 1.3 implementation
* RPR-FOM 1, a federation object model that borrows DIS
  semantics
* Open-DIS, an implementation of the DIS protocol in Java

The DISGateway object reads DIS packets from the wire, 
decodes them, and forwards them to the EntityDatabase
object. The EntityDatabase object informs the HLAGateway
object, which creates a new HLA entity if required, then
sends object attribute updates.

At this point there is not traffic in the reverse direction;
when an HLA object is created or updated there is no DIS
entity state PDU sent. There's also no interactions, such 
as fire and detonation events.

DMcG
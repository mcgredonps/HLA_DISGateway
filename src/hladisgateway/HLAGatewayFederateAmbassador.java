
package hladisgateway;


import hla.rti.ArrayIndexOutOfBounds;
import hla.rti.EventRetractionHandle;
import hla.rti.LogicalTime;
import hla.rti.ReceivedInteraction;
import hla.rti.ReflectedAttributes;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.NullFederateAmbassador;

// 1516 has a different interface for encoding/decoding
import hla.rti.jlc.*;


public class HLAGatewayFederateAmbassador extends NullFederateAmbassador
{
    
    public HLAGatewayFederateAmbassador()
    {
    }
    
/**
     * The RTI has informed us that time regulation is now enabled.
     */
    public void timeRegulationEnabled( LogicalTime theFederateTime )
    {
           System.out.println("Time regulation enabled");
    }

    public void timeConstrainedEnabled( LogicalTime theFederateTime )
    {
          System.out.println("Time constrained enabled");
    }

    public void discoverObjectInstance( int theObject,
                                        int theObjectClass,
                                        String objectName )
    {
            System.out.println( "Discoverd Object: handle=" + theObject + 
                                ", classHandle=" + theObjectClass + 
                                ", name=" + objectName );
    }

    
}

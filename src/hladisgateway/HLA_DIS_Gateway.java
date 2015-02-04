
package hladisgateway;
import java.io.*;

import hla.rti.AttributeHandleSet;
import hla.rti.FederatesCurrentlyJoined;
import hla.rti.FederationExecutionAlreadyExists;
import hla.rti.FederationExecutionDoesNotExist;
import hla.rti.LogicalTime;
import hla.rti.LogicalTimeInterval;
import hla.rti.RTIambassador;
import hla.rti.RTIexception;
import hla.rti.ResignAction;
import hla.rti.SuppliedAttributes;
import hla.rti.SuppliedParameters;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.RtiFactoryFactory;


/**
 * An entry point for the program. Creates a DIS listener, an HLA
 * listener, and an entity database to keep track of entities from both.
 * 
 * @author mcgredo
 */
public class HLA_DIS_Gateway {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) 
    {
        
        // create DIS listening
        DISGateway disGateway = DISGateway.getInstance();
       
        // Start HLA federate
        HLAGateway hlaGateway = new HLAGateway("rpr-fom.fed", "RprFederate", "GatewayFederate");
        
        // EntityDatabase object acts as a mediator between the DIS and HLA portions
        EntityDatabase entityDatabase = EntityDatabase.getInstance();
        
        entityDatabase.setDISGateway(disGateway);
        entityDatabase.setHLAGateway(hlaGateway);
        
        // Start listening for DIS
        Thread disThread = new Thread(disGateway);
        disThread.start();
        
    }
    
}

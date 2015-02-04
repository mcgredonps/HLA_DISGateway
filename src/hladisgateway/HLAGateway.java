
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

import edu.nps.moves.dis.*;

/**
 * A federate, with a RPR-FOM fed file, that communicates with DIS.
 * @author DMcG
 */
public class HLAGateway 
{
    /** Interface for this object talking to the RTI */
    public RTIambassador rtiAmbassador;
    
    /** Object that has an API that allows the RTI to inform it of changes */
    public HLAGatewayFederateAmbassador federateAmbassador;
    
    /** The fed file name, often RPR-FOM, that lists the objects, attributes, and interactions of the FOM */
    String fedFileName;
    
    /* Name of the federation this federate joins (or creates) */
    String federationName;
    
    /** Name of this federate */
    String federateName;
        
    /** constructor. Create or join a federate.
     * 
     * @param fedFileName
     * @param federationName
     * @param federateName 
     */
    public HLAGateway(String fedFileName, String federationName, String federateName)
    {
        this.federationName = federationName;
        this.fedFileName = fedFileName;
        this.federateName = federateName;
        
        try
        {
            // Get an interface for talking to the RTI
            rtiAmbassador = RtiFactoryFactory.getRtiFactory().createRtiAmbassador();
            
            // The fed file, containing all the objects and attributes
            File fom = new File( fedFileName );
            
            // This operation--creating the federation execution--may fail if
            // there is already a federation execution by that name. This is
            // harmless.
            try
            {
                rtiAmbassador.createFederationExecution( federationName,
                                                          fom.toURI().toURL() );
                 System.out.println( "Created Federation execution" );
            }
            catch(Exception e)
            {
                System.out.println("Unable to create federation execution " + federationName + ", probably because it already exists.");
            }
             
            // Create a new federate ambassador--the API by which the RTI talks to our code
             federateAmbassador = new HLAGatewayFederateAmbassador();
             
             // Have the rti ambassador join the federation execution under the give name, with federate amb.
             rtiAmbassador.joinFederationExecution( federateName, federationName, federateAmbassador );
             System.out.println( "Joined Federation " + federationName + " as federate " + federateName );
                 
                 
	    // Inform the RTI about the types of data that the federate will
	    // be creating, and the types of data we are interested in hearing about as other
	    //federates produce it.
	 
             // This is bogus. We want to publish the correct type of entity
            // based on the entity type. This is also only some of the attributes
            // of a ground entity. There's also velocity, acceleration, etc. but good
            // enough for now.
            int classHandle = rtiAmbassador.getObjectClassHandle( "ObjectRoot.BaseEntity.PhysicalEntity.MilitaryEntity.MilitaryPlatform.MilitaryGroundVehicle" );
            int entityTypeHandle = rtiAmbassador.getAttributeHandle( "EntityType", classHandle );
            int entityIDHandle = rtiAmbassador.getAttributeHandle("EntityIdentifier", classHandle);
            int entityLocationHandle = rtiAmbassador.getAttributeHandle("WorldLocation", classHandle);
            
            AttributeHandleSet attributes = RtiFactoryFactory.getRtiFactory().createAttributeHandleSet();
            attributes.add(entityTypeHandle);
            attributes.add(entityIDHandle);
            attributes.add(entityLocationHandle);
            
            // Announce to the RTI that this federate will be publishing objects of the type above
            rtiAmbassador.publishObjectClass(classHandle, attributes);
            System.out.println("Announced we will publish objects of type ObjectRoom.BaseEntity.PhysicalEntity.MilitaryEntity.MilitaryPlatform.MilitaryGroundVehicle, classID= " + classHandle);
            
            // Announce to the RTI that we want to receive updates for objects of
            // the above class
            rtiAmbassador.subscribeObjectClassAttributes( classHandle, attributes );
            System.out.println("Subscribed to objects of that type as well");
        }
        catch(Exception e)
        {
            System.out.println("Problems" + e);
        }
    }
    
    /**
     * A DIS update for an entity has been received. Send out an HLA 
     * update for the attributes.
     * 
     * @param gatewayData 
     */
    public void updateEntity(EntityDatabase.GatewayData gatewayData)
    {
        try
        {
            // create the collection to store the values in
            SuppliedAttributes attributes =
                    RtiFactoryFactory.getRtiFactory().createSuppliedAttributes();

            // generate the new values. Should be using encoding helpers for
            // surer compliance with encoding. However, the open-dis implmentation
            // has methods that encode the data in network byte order, so just
            // use those.
            
            // The central idea here is that we have attribute names that have
            // as their values the encoded data. It happens that open-dis is
            // able to encode the data in the correct (I hope) format.
            
            // Entity ID (rpr-fom and DIS) site, application, entity triplet
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            gatewayData.espdu.getEntityID().marshal(new DataOutputStream(baos));
            byte[] entityIDData = baos.toByteArray();
            
            // Entity type record
            baos = new ByteArrayOutputStream();
            gatewayData.espdu.getEntityType().marshal(new DataOutputStream(baos));
            byte[] entityTypeData = baos.toByteArray();
            
            // Location in world coordinates
            baos = new ByteArrayOutputStream();
            gatewayData.espdu.getEntityLocation().marshal(new DataOutputStream(baos));
            byte[] entityLocationData = baos.toByteArray();
            
            // Other fields as needed

            // get the handles
            // this line gets the object class of the instance identified by the
            // object instance the handle points to
            int classHandle = rtiAmbassador.getObjectClass( gatewayData.objectHandle );
            int entityIDHandle = rtiAmbassador.getAttributeHandle( "EntityIdentifier", classHandle );
            int entityTypeHandle = rtiAmbassador.getAttributeHandle( "EntityType", classHandle );
            int entityLocationHandle = rtiAmbassador.getAttributeHandle( "WorldLocation", classHandle );

            // put the values into the collection
            attributes.add( entityIDHandle, entityIDData );
            attributes.add( entityTypeHandle, entityTypeData );
            attributes.add( entityLocationHandle, entityLocationData );
            
            // do the actual update
            rtiAmbassador.updateAttributeValues( gatewayData.objectHandle, attributes, generateTag() );
            
        }
        catch(Exception e)
        {
            System.out.println("Unable to update object" + e);
        }
    }
    
    /**
     * Creates an HLA object and returns the handle, or unique ID, to the caller
     * 
     * @param gatewayData
     * @return 
     */
    public int createHLAEntity(EntityDatabase.GatewayData gatewayData)
    {
        int objectHandle = -1; // An invalid object handle
        try
        {
           int classHandle = rtiAmbassador.getObjectClassHandle( "ObjectRoot.BaseEntity.PhysicalEntity.MilitaryEntity.MilitaryPlatform.MilitaryGroundVehicle" );
           objectHandle = rtiAmbassador.registerObjectInstance( classHandle );
           System.out.println("created HLA entity to match DIS entity");
        }
        catch(Exception e)
        {
            System.out.println("Can't create HLA object");
            System.out.println(e);
        }
        
        return objectHandle;
    }
    
    private byte[] generateTag()
    {
            return (""+System.currentTimeMillis()).getBytes();
    }

}

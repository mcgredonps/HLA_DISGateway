
package hladisgateway;

import java.util.concurrent.*;
import java.util.*;

import edu.nps.moves.dis.*;

/**
 * a Singleton pattern, this represents information about all the entities
 * in the world, from both the DIS and HLA side. The entity database is keyed
 * by the DIS entity ID, and the value contains the most recent data from both
 * the HLA and DIS sides for that object.
 * 
 * @author DMcG
 */
public class EntityDatabase 
{
    /**
     * From which protocol a given entity comes--is this a DIS entity we're
     * forwarding to HLA, or an HLA entity we're forwarding to DIS?
     */
    public enum EntityOrigin  {DIS, HLA};
    
    /** singleton instance */
    private static EntityDatabase instance;
    
    /** Database, keyed by entity ID. This is thread-safe, so
     * two threads can write to it at once. I hope.
     */
    private ConcurrentHashMap<EntityID, GatewayData> entityDatabaseKeyedByEntityID;
    
    /** Database, keyed by HLA object ID. */
    private ConcurrentHashMap<Integer, GatewayData> entityDatabaseKeyedByObjectID;
    
    /** HLA federate connection */
    private HLAGateway hlaGateway = null;
    
    /** DIS network connection */
    private DISGateway disGateway = null;
    
    /** Get the single, shared instance. Use this rather than a constructor. */
    public static synchronized EntityDatabase getInstance()
    {
        if(instance == null)
        {
            instance = new EntityDatabase();
        }
        
        return instance;
    }
    
    /** Create the object, then set the HLA federate connection.
     * 
     * @param hlaGateway 
     */
    public void setHLAGateway(HLAGateway hlaGateway)
    {
        this.hlaGateway = hlaGateway;
        
        // Before the HLA gateway joins we may have had some
        // entities published by the DIS gateway. Inform the
        // HLA side of these.
        /*
        Iterator<GatewayData> it = entityDatabaseKeyedByEntityID.values().iterator();
        while(it.hasNext())
        {
            GatewayData gatewayData = it.next();
            int objectHandle = hlaGateway.createHLAEntity(gatewayData);
            if(objectHandle != -1)
            {
                gatewayData.objectHandle = objectHandle;
                entityDatabaseKeyedByObjectID.put(objectHandle, gatewayData);
            }
            else
            {
                System.out.println("Can't create HLA entity counterpart to " + gatewayData.espdu.getEntityID());
            }
        }
        */
    }
    
    /** After creating the entity database, set the DIS network connection.
     * The DIS network connection will inform us of any entities it discovers,
     * and we will in turn inform the HLA federate.
     * @param disGateway 
     */
    public void setDISGateway(DISGateway disGateway)
    {
        this.disGateway = disGateway;
    }
    
    /** Private constructor for the singleton class. Not accessible by
     * outsiders; use the getInstance() static method instead.
     */
    private EntityDatabase()
    {
        entityDatabaseKeyedByEntityID = new ConcurrentHashMap<EntityID, GatewayData>();
        entityDatabaseKeyedByObjectID = new ConcurrentHashMap<Integer, GatewayData>();
        
    }
   
    /** How the DIS network connection informs us of an ESPDU receipt. If
     * the espdu is new, add it and publish the object in HLA. If it's
     * known, update the object in HLA
     * 
     * @param espdu 
     */
    public void receiveDISPdu(EntityStatePdu espdu)
    {
        // The logic here seems more complicated than necessary.
        
        // Value to the eid key. Contains both HLA and DIS data.
        GatewayData gatewayData;
        
        // Unique DIS entity identifier
        EntityID eid = espdu.getEntityID();
        int objectID = -1;
        
        // Add it to the database if missing. Also publish the
        // object in HLA if the HLA federate exists.
        gatewayData = entityDatabaseKeyedByEntityID.get(eid);
        
        // Got null back? This is the first time we've heard of that entity
        if(gatewayData == null)
        {
            gatewayData = new EntityDatabase.GatewayData();
            gatewayData.entityOrigin = EntityOrigin.DIS;
            gatewayData.espdu = espdu;
            
            System.out.println("entity database size:" + entityDatabaseKeyedByEntityID.size());
            
            // Maybe the HLA gateway hasn't been set yet. If so, just punt for
            // now, and when it is set it will catch up on any published DIS
            // entities.
            if(hlaGateway != null)
            {
                // Publish in HLA, and get the HLA unqiue object ID
                objectID = hlaGateway.createHLAEntity(gatewayData);
                gatewayData.objectHandle = objectID;
            }
            entityDatabaseKeyedByEntityID.put(eid, gatewayData);
            
            // shared object in both databases
            if(objectID != -1)
            {
                entityDatabaseKeyedByObjectID.put(objectID, gatewayData);
            }
            
        }
        else // We already know about this. Reset the espdu to the most recent.
             // This updates the data in both keyedByEntityID and keyedByObjectID
        {
            gatewayData.espdu = espdu;
            
            // Possible the corresponding HLA entity has never been set due to
            // slow startup of hla gateway.
            if(gatewayData.objectHandle == -1 && hlaGateway != null)
            {
                int objectHandle = hlaGateway.createHLAEntity(gatewayData);
                gatewayData.objectHandle = objectHandle;
            }
        }
        
        // In any event, inform the HLA gateway that it should update the
        // values
        if(hlaGateway != null)
        {
            hlaGateway.updateEntity(gatewayData);
        }
    }
    
    /**
     * TBD
     * @param eid 
     */
    public void addFromHLA(EntityID eid)
    {
        // In this method we should discover the attributes associated with the
        // HLA object, put them in a DIS espdu, and inform the DISGateway
        // that it should send an update.
        System.out.println("Discovered entity from HLA, eventually should start updating DIS from that");
    }
    
    /**
     * Represents data about an entity, and includes information from both the
     * HLA and DIS side. For example, the HLA object handle, a unique identifier
     * for the object on the HLA side, and also the most recent entity state pdu
     * from the DIS side.<p>
     * 
     * This is placed into a hashmap, keyed by the entity ID.
     */
    public class GatewayData
    {
        /** Is this entity discovered from the DIS side or the HLA side? */
        public EntityOrigin entityOrigin;
        
        /** HLA object handle (unique ID). -1 means not set yet. */
        public int objectHandle = -1;
        
        /** Most recent entity state PDU from the DIS side */
        public EntityStatePdu espdu;
    }

}

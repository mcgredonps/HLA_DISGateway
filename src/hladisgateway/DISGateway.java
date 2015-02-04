
package hladisgateway;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import edu.nps.moves.dis.*;
import edu.nps.moves.disutil.*;

/**
 * Starts listening for DIS. Forwards espdus to the EntityDatabase
 * mediator for forwarding on to HLA.
 * 
 * This is a singleton pattern. Use getInstance() to retrieve the
 * single, shared instance of the object.
 * 
 * @author DMcG
 */
public class DISGateway implements Runnable
{
    /** 
     * Singleton shared instance
     */
    private static DISGateway instance;
    
    private DatagramSocket socket;
    
    // This stuff should be moved out to a properties file
    private int DIS_PORT = 3000;
    private String broadcastAddressString = "172.20.70.255";
    private InetAddress broadcastAddress;
    private PduFactory pduFactory;

    private static final int MAX_PDU_SIZE = 1500;
    
    /** 
     * Returns the single, shared instance
     */
    public synchronized static DISGateway getInstance()
    {
        if(instance == null)
        {
            instance = new DISGateway();
        }
        
        return instance;
    }
    
    /**
     * Private constructor prevents creating an instance any way other than
     * getInstance()
     */
    private DISGateway()
    {
        try
        {
            socket = new DatagramSocket(DIS_PORT);
            broadcastAddress = InetAddress.getByName(broadcastAddressString);
            pduFactory = new PduFactory();
        }
        catch(Exception e)
        {
            System.out.println(e);
            e.printStackTrace();
        }
        
    }
    
    public void sendDISPdu(Pdu aPdu)
    {
        try
        {
            byte[] disFormat = aPdu.marshalWithDisAbsoluteTimestamp();
            if(disFormat == null)
            {
                System.out.println("Failed marshal for pdu");
            }
            else
            {
                DatagramPacket packet = new DatagramPacket(disFormat, disFormat.length, broadcastAddress, DIS_PORT);
            } 
        }
        catch(Exception e)
        {
            System.out.println(e);
        }
        
    }
    
    @Override
    public void run()
    {
        EntityDatabase entityDatabase = EntityDatabase.getInstance();
        
        while(true)
        {
            try
            {
                Pdu aPdu;
                byte[] buffer = new byte[MAX_PDU_SIZE];
                
                
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                
                socket.receive(packet);
                aPdu = pduFactory.createPdu(packet.getData());
                
                //System.out.println("Got PDU");
                
                switch(aPdu.getPduType())
                {
                    case 1: // espdu
                        EntityID eid = (((EntityStatePdu)aPdu).getEntityID());
                        //System.out.println(eid.getSite() + "," + eid.getApplication() + "," + eid.getEntity());
                        entityDatabase.receiveDISPdu((EntityStatePdu)aPdu);
                        break;
                        
                    default:
                        break;
                }
                
            }
            catch(Exception e)
            {
                System.out.println(e);
            }
        }
        
    }

}

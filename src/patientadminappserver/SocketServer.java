/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package patientadminappserver;

import interfaces.IServerDatabase;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * This class sets up the server.
 * Each request is handled synchronously
 * @author TLohan
 */
public class SocketServer {
    
    IServerDatabase database;
    
    int portNumber = 4444;
    ServerSocket serverSocket = null;
    
    /**
     * Default constructor
     * @param database The database object to be used 
     */
    public SocketServer(IServerDatabase database){
        this.database = database;
    }
    
    /**
     * Runs the server to listen for requests and return responses.
     * Outputs the INET and IP address of the machine making the request.
     */
    public void runServer(){
        try {
            serverSocket = new ServerSocket(portNumber);
        } catch (IOException ioe){
            System.out.println(ioe.getMessage());
        }
        
        while (true) {            
            try {
                Socket clientSocket = serverSocket.accept();
                InetAddress inetAddress = clientSocket.getInetAddress();
                System.out.println("Connected with " + inetAddress.getHostName()+ ".\n IP address: " + inetAddress.getHostAddress() + "\n");
                new Thread(new RequestHandlerRunnable(clientSocket, database)).start();
            } catch (IOException ioe){
                System.out.println(ioe.getMessage());
            }
        }
        
    }
    
}

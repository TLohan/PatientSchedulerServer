/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package patientadminappserver;

import ORM.ObjectMapper;
import interfaces.IServerDatabase;

/**
 * This is the entry point for the program.
 * @author TLohan
 */
public class PatientAdminAppServer {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        IServerDatabase database = new Database();
        ObjectMapper.setDB(database);
        database.populateWaitLists();
        
        
        System.out.println("Welcome to the Patient Admin Server");
        SocketServer ss = new SocketServer(database);
        ss.runServer();
    }
    
}

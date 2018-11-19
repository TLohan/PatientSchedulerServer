/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package patientadminappserver;

import customDataStructures.DoubleLinkedList;
import entityClasses.HospitalEmployee;
import entityClasses.Patient;
import entityClasses.ValidServerRequest;
import interfaces.IServerDatabase;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An instance of this class is created to handle every request made to the server 
 * @author TLohan
 */
public class RequestHandlerRunnable implements Runnable {
    
    private Socket clientSocket = null;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private IServerDatabase db;
    
    /**
     * Default constructor
     * @param clientSocket The socket which initiated the connection
     * @param database The database object being used by the server
     */
    public RequestHandlerRunnable(Socket clientSocket, IServerDatabase database){
        this.db = database;
        this.clientSocket = clientSocket;
        try {
            oos = new ObjectOutputStream(clientSocket.getOutputStream());
            ois = new ObjectInputStream(clientSocket.getInputStream());
        } catch (IOException ex) {
            Logger.getLogger(RequestHandlerRunnable.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Runs the server to receive requests from clients.
     * Each request is assigned to a new Thread.
     */
    @Override
    public synchronized void run(){
       
        try {
            while (true){
                ValidServerRequest request;
                try {
                    request = (ValidServerRequest) ois.readObject();
                    handleRequest(request);

                } catch (ClassNotFoundException ex) {
                    Logger.getLogger(RequestHandlerRunnable.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
            
        } catch (IOException ioe){
            System.out.println(ioe.getMessage());
        }
    }
    /**
     * Processes each request made by the client.
     * @param request The ValidServerRequest made by the client.
     */
    private synchronized void handleRequest(ValidServerRequest request){
        try {     
            Patient patient;
            switch (request){
                case SAVE_PATIENT:
                    // System.out.println("saving patient to the database....");
                    patient = (Patient) ois.readObject();
                    db.savePatient(patient);
                    break;
                case GET_NEXT_PATIENT_FOR_TRIAGE_NURSE:
               //if (request.equals("getNextPatientForTriageNurse")){
                    //System.out.println("getting next patient for triageNurse");
                    patient = db.getNextPatientForTriageNurse();
                    if (patient == null){
                       patient = new Patient();
                    }
                    oos.writeObject(patient);
                    oos.reset();
                    break;
                case GET_NEXT_PATIENT_FOR_DOCTOR:
                    //System.out.println("getting next patient for doctor");
                    patient = db.getNextPatientForDoctor();
                    if (patient == null){
                       patient = new Patient();
                    }
                    oos.writeObject(patient);
                    oos.reset();
                    break;
                case GET_TRIAGE_NURSE_WAITING_LIST:
                   // System.out.println("Getting waiting list for triage Nurse");
                    DoubleLinkedList<Patient> tn_waitList = db.getTriageNurseWaitingList();
                    //System.out.println("in handleRequest() : " + tn_waitList.size());
                    oos.writeObject(tn_waitList);
                    oos.reset();
                    break;
                case GET_DOCTOR_WAITING_LIST:
                    //System.out.println("Getting waiting list for doctor");
                    DoubleLinkedList<Patient> doc_waitList = db.getDoctorWaitingList();
                    oos.writeObject(doc_waitList);
                    oos.reset();
                    break;
                case ADD_PATIENT_TO_TRIAGE_NURSE_WAITING_LIST:
                    //System.out.println("saving patient to triage nurse waiting list....");
                    patient = (Patient) ois.readObject();
                    db.addPatientToTriageNurseWaitingList(patient);
                    //System.out.println("Added!");
                    break;
                case ADD_PATIENT_TO_DOCTOR_WAITING_LIST:
                    //System.out.println("saving patient to doctor waiting list....");
                    patient = (Patient) ois.readObject();
                    db.addPatientToDoctorWaitingList(patient);
                    //System.out.println("Added!");
                    break;
                case GET_HOSPITAL_EMPLOYEE_BY_USERNAME:
                    //System.out.println("getting hospital employee...");
                    String employee_id = (String) ois.readObject();
                    HospitalEmployee employee = db.getHospitalEmployeeByUsername(employee_id);
                    oos.writeObject(employee);
                    oos.reset();
                    break;
                default:
                    System.out.println("Invalid request..... : " + request);

                } 
            } catch (IOException | ClassNotFoundException ioe){
                System.out.println(ioe.getMessage());
            }
    }
    
    
}

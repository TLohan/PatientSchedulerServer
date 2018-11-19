/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package patientadminappserver;

import customDataStructures.DoubleLinkedList;
import customDataStructures.Node;
import customDataStructures.PrioritySortedDoubleLinkedList;
import entityClasses.HospitalEmployee;
import entityClasses.Patient;
import entityClasses.PatientStatus;
import interfaces.IDoubleLinkedList;
import interfaces.IServerDatabase;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author TLohan
 */
public class Database implements IServerDatabase {
    
    private boolean successfulConnection;
    public Connection conn;
    public Statement stmt;
    
    private volatile DoubleLinkedList<Patient> triageNurseWaitingList;
    private volatile PrioritySortedDoubleLinkedList<Patient> doctorWaitingList;
    

    /**
     * Default constructor
     */
    public Database(){
        successfulConnection = connect();
    }
    
    /**
     * Populates the Doctor and Triage Nurse's Waiting Lists when the server is started 
     */
    @Override
    public void populateWaitLists(){
        triageNurseWaitingList = populateTriageNurseWaitingList();
        doctorWaitingList = populateDoctorWaitingList();
    }
    
    /**
     * Populates the Triage Nurses Waiting List
     * @return A double linked list of type Patient
     */
    private DoubleLinkedList<Patient> populateTriageNurseWaitingList(){
        connect();
        DoubleLinkedList<Patient> dll = new DoubleLinkedList<>();
        dll = (DoubleLinkedList<Patient>) getDLL(1, dll);
       return dll;  
    }
    
    /**
     * Populates the Doctor's Waiting List
     * @return A Priority Sorted Double Linked List of type Patient
     */
    private PrioritySortedDoubleLinkedList<Patient> populateDoctorWaitingList(){
        connect();
        PrioritySortedDoubleLinkedList<Patient> dll = new PrioritySortedDoubleLinkedList<>();
        dll = (PrioritySortedDoubleLinkedList<Patient>) getDLL(2, dll);
        return dll;  
    }
    
    /**
     * Connects to the SQLite Database File
     * @return Boolean true if the connection was successful
     */
    private boolean connect(){
        try {
            Class.forName("org.sqlite.JDBC");
            this.conn = DriverManager.getConnection("jdbc:sqlite:../src/SQLiteFiles/PatientAdminAppDatabase.sqlite");
            return true;
        } catch (SQLException e){
            System.out.println("here");
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
            return false;
        } catch (ClassNotFoundException e){
            System.err.println("Error - class not found"); // fix laters
            return false;
        }
    }
    /**
     * Checks if the server has made a valid connection to the database.
     * @return Boolean - true if the connection is valid
     */
    public boolean isSuccessfulConnection() {
        return successfulConnection;
    }
     
    @Override
    public void savePatient(Patient patient){
        IServerDatabase serverdb = this;
        patient.save(serverdb);
    }
    
    /**
     * Saves a HospitalEmployee object to the database
     * @param hospitalEmployee The HospitalEmployee object to be saved
     */
    @Override
    public void saveHospitalEmployee(HospitalEmployee hospitalEmployee){
        hospitalEmployee.save((interfaces.IServerDatabase) this);
    }
    
    /**
     * Pops the Patient from the top of the Triage Nurse's queue
     * @return Patient
     */
    @Override
    public Patient getNextPatientForTriageNurse(){
        Patient patient = null;
       if (triageNurseWaitingList.size() > 0){
            patient = (Patient) triageNurseWaitingList.pop();
            patient.setStatus(PatientStatus.WITH_TRIAGE_NURSE);
            patient.save(this);
            triageNurseWaitingList.save(this);
       }
       return patient;
    }
    
    /**
     * Pops the Patient from the top of the Doctor's queue
     * @return Patient
     */
    @Override
    public Patient getNextPatientForDoctor(){
        Patient patient = null;
        if (doctorWaitingList.size() > 0){
            patient = (Patient) doctorWaitingList.pop();
            patient.setStatus(PatientStatus.WITH_DOCTOR);
            patient.save(this);
            doctorWaitingList.save(this);
        }
       return patient;
    }
    
    /**
     * Gets the Triage Nurse's Waiting List from the database.
     * @return Double Linked List of type Patient
     */
    @Override
    public synchronized DoubleLinkedList<Patient> getTriageNurseWaitingList(){
        return triageNurseWaitingList;
    }
    
    /**
     * Gets the Doctor's Waiting List from the database.
     * @return Priority Sorted Double Linked List of type Patient
     */
    @Override
    public synchronized PrioritySortedDoubleLinkedList<Patient> getDoctorWaitingList(){
        return doctorWaitingList;
    }
    
    /**
     * Gets a Double Linked List from the database
     * @param id The id of the DoubleLinkedList to be retrieved
     * @param dll The class implementing the IDoubleLinkedList interface to be retrieved
     * @return A list implementing the IDoubleLinkedList interface
     */
    public IDoubleLinkedList getDLL(int id, IDoubleLinkedList dll){
        connect();
        String primaryKeyColumnName = "waitingList_id";
        int primaryKey = id;
        String tableName = "WaitingLists";
        String query = String.format("SELECT head_id FROM %s WHERE %s=%s;", tableName, primaryKeyColumnName, primaryKey);
        
        ResultSet rs = executeQuery(query);
        int head_id = 0;
        try {
            head_id = rs.getInt("head_id");
            try {
                 rs.close();
            } catch (SQLException ex) {
                Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
            }
            dll.setDoubleLinkedList_id(id);
            
            if (head_id != 0){
                Node head = getNode(head_id, null, dll);
                dll.setHead(head);
            }
            
            
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
       return dll;  
    }
    
    /**
     * Retrieves a Node from the database
     * @param id The id of the node to retrieve
     * @param previousNode The preceding node to the one being retrieved
     * @param dll The class implementing IDoubleLinkedList which the Node belongs in
     * @return Node
     */
    private Node getNode(int id, Node previousNode, IDoubleLinkedList dll){
        connect();
        Node node = new Node();
        String query = "SELECT patient_id, next_id FROM Nodes WHERE node_id=" + id + ";";
        node.setNode_id(id);
        node.setPrevious(previousNode);
        System.out.println(query);
        ResultSet rs = executeQuery(query);
         try {
            int patinet_id = rs.getInt("patient_id");
            node.setObject((Patient) get(Patient.class, patinet_id));
            try {
                dll.incrementSize();
                int next_id = (int) rs.getObject("next_id");
                if (next_id != 0){
                     node.setNext(getNode(next_id, node, dll));
                } else {
                     node.setNext(null);
                }
            } catch (NullPointerException ex) {
                 node.setNext(null);
             }
             
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
         
        try {
            rs.close();
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
         
        return node;
    }
    
    /**
     * Executes an SQL statement
     * @param statement The SQL statement to be executed
     * @return ResultSet - The response from the database
     */
    private ResultSet executeQuery(String statement){
        ResultSet rs = null;
        try {
            this.stmt = this.conn.createStatement();
            rs = this.stmt.executeQuery(statement);
        } catch (SQLException sqle){
            System.err.println(sqle.getMessage());
        } 
        return rs;
    }
   
    /**
     * Add a Patient to the TriageNurse's Waiting List
     * @param patient The patient to be added to the Database
     */
    @Override
    public void addPatientToTriageNurseWaitingList(Patient patient){
        if (!checkForDuplicate(triageNurseWaitingList, patient)){
            patient.setStatus(PatientStatus.WAITING_FOR_TRIAGE_NURSE);
            Node<Patient> node = triageNurseWaitingList.addNode(patient);
            if (triageNurseWaitingList.size() == 1){
                triageNurseWaitingList.save(this);
            }
            node.save((interfaces.IServerDatabase) this);
        } 
    }
    
    /**
     * Checks if the Patient already exists in the list
     * @param list The list to be checked
     * @param patientToCheck The Patient object to be checked for
     * @return 
     */
    private boolean checkForDuplicate(DoubleLinkedList<Patient> list, Patient patientToCheck){
        ArrayList<Patient> listOfPatients = list.getArrList();
        for (Patient patient : listOfPatients){
            if (patient.equals(patientToCheck)){
                return true;
            }
        }
        return false;
    }
    
    /**
     * Add a patient to the Doctor's Waiting List
     * @param patient The Patient object ti be added to the list
     */
    @Override
    public void addPatientToDoctorWaitingList(Patient patient){
       patient.setStatus(PatientStatus.WAITING_FOR_DOCTOR);
       Node<Patient> node = doctorWaitingList.addNode(patient);
        if (doctorWaitingList.size() == 1){
            doctorWaitingList.save(this);
        }
       node.save((interfaces.IServerDatabase) this);
    }
    
    /**
     * Get a patient from the database
     * @param patient_id The Id of the patient to retrieve
     * @return 
     */
    public Patient getPatient(int patient_id){
        Patient patient = (Patient) get(Patient.class, patient_id);
        
       return patient;
    }
    
    /**
     * Get a Hospital Employee from the database
     * @param employee_id The Id of the employee to retrieve
     * @return HospitalEmployee
     */
    @Override
    public HospitalEmployee getHospitalEmployee(int employee_id){
        HospitalEmployee hospitalEmployee = (HospitalEmployee) get(HospitalEmployee.class, employee_id);
        return hospitalEmployee;
    }  
    
    /**
     * Get a Hospital Employee from the database
     * @param employee_username The username of the hospital employee to retrieve
     * @return HospitalEmployee
     */
    @Override
    public HospitalEmployee getHospitalEmployeeByUsername(String employee_username){
        HospitalEmployee hospitalEmployee = null;
        connect();
        
        String query = "SELECT * FROM %s WHERE %s='%s'";
        String simpleClassName = HospitalEmployee.class.getSimpleName();
        String tableName = simpleClassName.substring(0, 1).toUpperCase() + simpleClassName.substring(1) + "s";
        String primaryKeyField = "employee_id";
        String selectStmt = String.format(query, tableName, primaryKeyField, employee_username);
        System.out.println(selectStmt);
        ResultSet rs = executeQuery(selectStmt);
        try {
            Constructor<HospitalEmployee> objectConstructor = HospitalEmployee.class.getConstructor(ResultSet.class);
            hospitalEmployee = objectConstructor.newInstance(rs);
        } catch (NoSuchMethodException nsme){
            System.err.println("Error: " + nsme.getMessage());
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }finally {
            try {
                rs.close();
            } catch (SQLException ex) {
                Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        
        return hospitalEmployee;
    }
    
    /**
     * Gets an object from the database
     * @param objectClass The class of the object to be retrieved
     * @param primaryKey The key of the object to be retrieved
     * @return Object
     */
    @Override
    public Object get(Class<?> objectClass, int primaryKey){
        connect();
        String query = "SELECT * FROM %s WHERE %s=%s";
        String simpleClassName = objectClass.getSimpleName();
        String tableName = simpleClassName.substring(0, 1).toUpperCase() + simpleClassName.substring(1) + "s";
        String primaryKeyField = simpleClassName.toLowerCase() + "_id";
        String selectStmt = String.format(query, tableName, primaryKeyField, primaryKey);
        Object object = null;
        ResultSet rs = executeQuery(selectStmt);
        try {
            Constructor<?> objectConstructor = objectClass.getConstructor(ResultSet.class);
            object = objectConstructor.newInstance(rs);
        } catch (NoSuchMethodException nsme){
            System.err.println("Error: " + nsme.getMessage());
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                rs.close();
                this.conn.close();
            } catch (SQLException ex) {
                Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return object;
    }
    
    
    /**
     * Adds an object to the database by executing an SQL statement
     * @param statement The SQL statement to be executed
     * @return int The foreign key of the object that was created.
     */
    @Override
    public int addToDB(String statement){
       int foreignKey = 0;
       connect();

        try {
            if (this.conn == null){
                System.out.println("bad connection");
            }
            System.out.println(statement);
            this.stmt = this.conn.createStatement();
            this.stmt.executeUpdate(statement);
            ResultSet rs = this.stmt.getGeneratedKeys();
            foreignKey = rs.getInt(1);
        } catch (SQLException sqle){
            System.err.println("Error: " +  sqle.getMessage());
        }finally {
           if (this.conn != null){
               try {
                   this.conn.close();
               } catch (SQLException ex) {
                   Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
               }
           }
       }
        return foreignKey;
    }
    
    
 
}

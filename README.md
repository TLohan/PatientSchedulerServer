# PatientSchedulerServer
Backend to the [PatientSchedulerClient](https://github.com/TLohan/PatientSchedulerClient) application. Designed to execute all CRUD commands perfomed by running instances of the client application. The server recieves a request from the client, handles the request and, if necessary, replies with the appropriate response. Each request creates a new thread of execution which is destroyed once the request has completed. This allows the server to attend to requests from multiple clients. Communication between the server and the client application is handled by server sockets. Objects sent to and from the server are seriazized. All data is persisted to a SQLite database using a Obect Relational Mapper I wrote for the application. This can be found in the shared library used by both applications. 

## Running the application
Download  and extract the project files. Navigate to the 'dist' folder and execute the PatientSchedulerServer.jar jar file.
__Please note the server must be running before a client application is started.__

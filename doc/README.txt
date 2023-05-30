How to compile and run this project:

1. Open one terminal, navigate to the src project's folder and execute the following commands:
    1. chmod +x ../scripts/compile.sh
    2. ../scripts/compile.sh
    
    When doing this the program will compile and save the compile files on the src/build project folder 

3. Open one or more terminals for all the peers you want to be executed and navigate to the src/build project 
folder.

4. In one of those terminals execute:
    1. rmiregistry &

5. Execute the following commands in each terminal:
    1. chmod +x ../../scripts/peer.sh 
    2. ../../scripts/peer.sh <version> <peer_id> <svc_access_point> <mc_addr> <mc_port> <mdb_addr> <mdb_port> 
    <mdr_addr> <mdr_port>

    When executing the second command a peer program will start executing waiting for the client to send a request 
    or recieve a message from other peers. This command can be executed several times in different terminals.
    

6. Open another terminal, navigate to the src/build project folder and execute the follwing commands:
    1. chmod +x ../../scripts/test.sh
    2.1. ../../scripts/test.sh <svc_access_point> BACKUP <file_path> <replication_deegre>
    2.2.  ../../scripts/test.sh <svc_access_point> RESTORE <file_path>
    2.3. ../../scripts/test.sh <svc_access_point> DELETE <file_path>
    2.4. ../../scripts/test.sh <svc_access_point> RECLAIM <reclaim_space>
    2.5. ../../scripts/test.sh <svc_access_point> STATE
    
    You can execute one of these commands(you can execute one while other is running) and this will send a request 
    to a peer to execute a service.
    

The cleanup shell script is used for delete the folder used by the peer to save the chunks sent by the remote peers
and the files it has recovered. This script recieves the peer's id. 


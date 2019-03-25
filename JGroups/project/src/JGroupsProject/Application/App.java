package JGroupsProject.Application;

import JGroupsProject.HashMap.DistributedMap;
import JGroupsProject.Operations.ComplexMessage;

import org.jgroups.*;
import org.jgroups.protocols.*;
import org.jgroups.protocols.pbcast.*;
import org.jgroups.stack.ProtocolStack;
import org.jgroups.util.Util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.*;

import static JGroupsProject.Operations.Operation.*;

public class App {
    private DistributedMap distributedMap;
    private JChannel jChannel;
    private static final String channelName = "jGroupsApp";

    public void start() throws Exception {
        //System.out.println("Start application");
        distributedMap = new DistributedMap();
        jChannel = new JChannel(false);

        ProtocolStack protocolStackStack = new ProtocolStack();
        jChannel.setProtocolStack(protocolStackStack);
        protocolStackStack
                .addProtocol(new UDP()
                    .setValue("mcast_group_addr",
                                InetAddress.getByName("230.100.200.10")))
                .addProtocol(new PING())
                .addProtocol(new MERGE3())
                .addProtocol(new FD_SOCK())
                .addProtocol(new FD_ALL()
                        .setValue("timeout", 12000)
                        .setValue("interval", 3000))
                .addProtocol(new VERIFY_SUSPECT())
                .addProtocol(new BARRIER())
                .addProtocol(new NAKACK2())
                .addProtocol(new UNICAST3())
                .addProtocol(new STABLE())
                .addProtocol(new GMS())
                .addProtocol(new UFC())
                .addProtocol(new MFC())
                .addProtocol(new FRAG2())
                .addProtocol(new STATE());

        protocolStackStack.init();

        channelListen();

        jChannel.setDiscardOwnMessages(true);
        jChannel.connect(channelName);
        jChannel.getState(null, 0);

        getInput();
    }

    public void channelListen() {
        jChannel.setReceiver(new ReceiverAdapter(){
                                public void receive(Message message){
                                    if(message.getObject() instanceof ComplexMessage) {
                                        ComplexMessage complexMessage = (ComplexMessage) message.getObject();
                                        switch(complexMessage.getOperation()){
                                            case PUT:
                                                distributedMap.put(complexMessage.getKey(), complexMessage.getValue());
                                                System.out.println("Received PUT operation from JGroup.");
                                                break;
                                            case REMOVE: System.out.println("REMOVE");
                                                distributedMap.remove(complexMessage.getKey());
                                                System.out.println("Received REMOVE operation from JGroup");
                                                break;
                                        }
                                    }
                                }

                                @Override
                                public void setState(InputStream in){
                                    try {
                                        HashMap<String, Integer> hashMap =
                                                (HashMap<String, Integer>) Util.objectFromStream
                                                        (new DataInputStream(in));
                                        distributedMap.setDistributedMap(hashMap);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }

                                @Override
                                public void getState(OutputStream out) throws Exception {
                                    Util.objectToStream(distributedMap.getDistributedMap(),
                                            new DataOutputStream(out));
                                }

                                @Override
                                public void viewAccepted(View view){
                                    if(view instanceof MergeView){
                                        AppHandler appHandler =
                                                new AppHandler(jChannel, (MergeView) view);
                                        appHandler.run();
                                    }
                                }
                             }
        );
    }

    public void getInput() {
        Scanner input = new Scanner(System.in);
        while(true){
            String command = input.nextLine();
            parseCommand(command);
        }
    }

    public void parseCommand(String command){
        String[] list = command.split("\\s");

        if(list.length == 0) return;

        switch(list[0].toUpperCase()){
            case "PUT":
                if(list.length >= 3) {
                    distributedMap.put(list[1], Integer.parseInt(list[2]));
                    try {
                        jChannel.send(new Message(null, null,
                                new ComplexMessage(list[1], Integer.parseInt(list[2]), PUT)));
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.err.println("Error during sending a message.");
                    }
                    if(distributedMap.containsKey(list[1])){
                        System.out.println("Element added succesfully.");
                    }
                }
                break;
            case "GET":
                if(list.length >= 2) {
                    if(distributedMap.containsKey(list[1])) {
                        System.out.println(list[1] + " : " + distributedMap.get(list[1]));
                    } else {
                        System.out.println("404 Not found: " + list[1]);
                    }
                }
                break;
            case "REMOVE":
                if(list.length >= 2){
                    if(distributedMap.containsKey(list[1])){
                        try {
                            jChannel.send(new Message(null, null,
                                    new ComplexMessage(list[1], REMOVE)));
                        } catch(Exception e){
                            System.err.println("Error during sending a message.");
                        }
                        distributedMap.remove(list[1]);
                        System.out.println("Correctly removed element: " + list[1]);
                    } else {
                        System.err.println("Cannot removed " + list[1] + ". Element doesn't exist.");
                    }
                }
                break;
            case "CONTAINS":
                if(list.length >= 2) {
                    if(distributedMap.containsKey(list[1])){
                        System.out.println("Distributed map contains element: " + list[1]);
                    } else {
                        System.out.println("404 Not found: " + list[1]);
                    }
                }
                break;
            case "ALL":
                distributedMap.getAllElements();
                break;
            default:
                System.err.println("Wrong input command!");
                break;
        }
    }

}

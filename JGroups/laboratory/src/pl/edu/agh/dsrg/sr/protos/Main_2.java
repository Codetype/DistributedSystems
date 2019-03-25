package pl.edu.agh.dsrg.sr.protos;

import org.jgroups.Message;
import org.jgroups.JChannel;
import org.jgroups.protocols.*;
import org.jgroups.protocols.pbcast.*;
import org.jgroups.stack.ProtocolStack;

public class Main_2 {
    public static void main(String[] args) throws Exception {
        System.setProperty("java.net.preferIPv4Stack", "true");

        JChannel operationCh = new JChannel(false);
        ProtocolStack protocolStack = new ProtocolStack();

        operationCh.setProtocolStack(protocolStack);
        protocolStack.addProtocol(new UDP())
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
                .addProtocol(new FRAG2());
        protocolStack.init();

        operationCh.connect("operation");

        System.out.println("Start sending");
        //<start of operations>
        for (int i = 0; i < 2000; ++i) {
            BankOperationProtos.BankOperation operation;
            operation = BankOperationProtos.BankOperation.newBuilder()
                    .setValue(Math.random() / 100 + 1.0)
                    .setType(BankOperationProtos.BankOperation.OperationType.ADD) //.setType(BankOperationProtos.BankOperation.OperationType.MULTIPLY)
                    .build();

            byte[] operationBuffer = operation.toByteArray();
            operationCh.send(null, operationBuffer);

            Message message = new Message(null, null, operationBuffer);
            operationCh.send(message);

            Thread.sleep((long)(Math.random()*10));
        }
        //<end of operations>
        System.out.println("Finished sending");

        Thread.sleep(5 * 60 * 1000); //wait 5 minutes before close

        operationCh.close();
    }
}

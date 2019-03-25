package JGroupsProject.Application;

import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.MergeView;
import org.jgroups.View;

public class AppHandler extends Thread{
    private JChannel jChannel;
    private MergeView mergeView;

    public AppHandler(JChannel jChannel, MergeView mergeView) {
        this.jChannel = jChannel;
        this.mergeView = mergeView;
    }

    public void run(){
        View view = mergeView.getSubgroups().get(0);
        Address address = jChannel.getAddress();

        if(!view.getMembers().contains(address)){
            try {
                jChannel.getState(null, 0);
            } catch (Exception e) {
                System.err.println("JChannel state exception");
            }
        }
    }
}

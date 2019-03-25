package JGroupsProject;

import JGroupsProject.Application.App;

public class Main {

    public static void main(String[] args) throws Exception {
        System.setProperty("java.net.preferIPv4Stack","true");

        App app = new App();
        app.start();
    }
}

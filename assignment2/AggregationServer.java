import java.net.*;
import java.io.*;

public class AggregationServer {
    private AggregationServer() {}

    public static void main(String[] args) {
        AggregationServer server = new AggregationServer();
        int port_number = server.set_port_number(args);

        try ( ServerSocket server_socket = new ServerSocket(port_number) ) {
            while (true) {
                new AggregationServerThread(server_socket.accept()).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return;
    }

    //method that determines if a port number has been supplied, and returns the appropriate number for the server
    private int set_port_number(String[] args) {
        int port_number = 4567;
        if (args.length >= 1)
            port_number = Integer.parseInt(args[0]);
        
        return port_number;
    }
}
import java.io.*;
import java.net.*;

public class ContentServer extends GETClient {
    public ContentServer() {}

    //method to turn an input line with format "x:y" into a json line with format
    //" "x" : "y" "
    //Takes a line which needs to be converted as an argument, and returns the converted string
    public String create_json_string(String line_to_json) {
        String[] key_and_value = line_to_json.split(":", 2);
        String result = "    \"";

        if (key_and_value.length == 2) {
            key_and_value[0].trim();
            key_and_value[1].trim();
            result += key_and_value[0] + "\" : \"" + key_and_value[1] +"\",\n";
            return result;
        } else {
            return result = null;
        }
    }

    //method that turns an input text file into a JSON object, only works for the predefined
    //textfile and JSON object formats for the assignment
    //Accepts the name of the file as an argument, and returns the JSON object as a string
    public String create_json_body(String file_name) {
        try {
            String result = "{\n";
            BufferedReader file_to_json = new BufferedReader(new FileReader(file_name));
            String current_line = file_to_json.readLine();
            String next_line = "";

            while (current_line != null) {
                next_line = file_to_json.readLine();
                
                if (next_line == null) {
                    result += create_json_string(current_line).replace(",\n", "\n");
                } else {
                    result += create_json_string(current_line);
                }
                
                current_line = next_line;
            }

            result += "}";
            return result;

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to open file");
        }
        return null;
    }

    //Method to create a put request, takes the body of the request as an argument, returns the assembled request.
    //Does this by finding the length of the body, appending the actual length of the body, and then the body.
    //Since everything but the body is the same for all requests, everything but the body and the content length (length of the body)
    //is hard coded
    public String create_put_request(String request_body) {
        String request = "PUT /weather.json HTTP/1.1\r\nUser-Agent: ATOMClient/1/0\r\nContent-Type: text/plain\r\nContent-Length: ";
        request += request_body.length();
        request += "\r\n\r\n" + request_body;
        return request;
    }

    //Main method of the content server which is run when the server is called, works the same as the GETClient as it inherits the 
    //functionality from that client. Only finishes when the aggregation server returns as OK status indicating that the PUT operation
    //was successful.
    public static void main(String[] args) {
        boolean success = false;
        while (!success) {
            try {
                ContentServer server = new ContentServer();
                String[] response;
                String body, hostname;
                int port_number, body_length;

                if (args.length < 2) {
                    System.out.println("No hostname/port, or file provided, server will close.");
                    return;
                }

                hostname = server.get_hostname(args[0]);
                port_number = server.get_port(args[0]);

                server.connect_socket(hostname, port_number);
                String request = server.create_put_request(server.create_json_body(args[1]));
                server.send_message(request);

                response = server.receive_response();

                //if the request was successful, stop trying
                if (response[0].contains(" 200 ") || response[0].contains(" 201 ") || response[0].contains(" 204 "))
                    success = true;

                server.close_connection();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
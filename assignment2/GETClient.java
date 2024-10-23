import java.io.*;
import java.net.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GETClient {
    public GETClient() {}
    private Socket get_socket;
    private PrintWriter socket_out;
    private BufferedReader socket_in;

    //Connects a socket using the hostname and port which are provided as arguments, also creates the required in and out streams for 
    //communication using the socket. Returns nothing as it exists only to setup connections.
    public void connect_socket(String hostname, int port) throws IOException {
        get_socket = new Socket(hostname, port);
        socket_out = new PrintWriter(get_socket.getOutputStream(), true);
        socket_in = new BufferedReader(new InputStreamReader(get_socket.getInputStream()));
        return;
    }

    //Method to find the port in a URL provided, takes the URL line as an argument and returns the port, if it fails it returns -1
    public int get_port(String arg_line) {
        //Regex pattern to find the port, works by looking for a set of numbers pre-appended by a colon
        Pattern port_pattern = Pattern.compile("(?<=:)[0-9]+");
        
        Matcher matcher = port_pattern.matcher(arg_line);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group());
        }
        return -1;
    }

    //Function to find the hostname in a provided format, takes the URL as the argument and returns the hostname.
    //Works by using regex to filter out the hostname, returns an empty string if hostname is not found
    public String get_hostname(String arg_line) {
        //Regex pattern to filter out the hostname, works by taking either the alphanumeric characters directly after the http://
        //or the alphanumeric characters at the very start of the string
        Pattern hostname_pattern = Pattern.compile("(?<=http://)[a-zA-Z0-9]+|^[a-zA-Z0-9]+(?!.*://)");
        Matcher matcher = hostname_pattern.matcher(arg_line);
        if (matcher.find()) {
            return matcher.group();
        }
        return "";
    }

    //Sends message to the out stream, uses write and then flush to avoid extra information being appended by the
    //PrintWriter methods. Takes the message as an argument, returns nothing.
    public void send_message(String message) throws IOException {
        socket_out.write(message);
        socket_out.flush();
        return;
    }

    //Requests always look like what is returned by this method, this is neater than having a floating String in the main
    //Takes hostname and port number as arguments, returns the request.
    public String create_request(String hostname, int port_number) {
        return "GET /weather.json HTTP/1.1\r\n Host: " + hostname + ":" + port_number + "\r\n\r\n";
    }

    //Method to receive the HTTP header portion of a response to a GET Request, reads until a combination
    //of four carriage return and newlines characters are read.
    //No arguments needed, and returns the String containing the headers including formatting
    public String receive_response_headers() throws IOException {
        String response = "";
        int current_char = socket_in.read();
        int consecutive = 0;
        while (current_char != -1) {
            response += (char) current_char;
            if (current_char == '\n' || current_char == '\r') {
                ++consecutive;
            } else {
                consecutive = 0;
            }
            if (consecutive > 3) {
                return response;
            }
            current_char = socket_in.read();
        }
        return "";
    }

    //Method to find the value of a header, accepts the String of headers, and header name that you are trying to find the value of.
    //Uses regex to find the value, and returns the value as a string, returns null if it fails to find a value
    public String find_header_value(String response_headers, String header_name) {
        //Regex pattern that matches any alphanumeric characters pre-appended by the header name, including whitespaces
        Pattern header_pattern = Pattern.compile("(?<=" + header_name + ": )[^\\r]+");
        Matcher matcher = header_pattern.matcher(response_headers);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }
    
    //Method to receive the body of a response/request, takes the length of the body as an argument, returns the body
    //Receives the response body by reading the BufferedReader inputstream by body_length chars
    public String receive_response_body(int body_length) throws IOException{
        String response_body = "";
        int current_char;
        for (int i = 0; i < body_length; ++i) {
            current_char = socket_in.read();
            response_body += (char) current_char;
        }
        return response_body;
    }

    //Prints a json file in a readable format, takes the JSON file as a string argument
    //Cuts off any leading or trailing whitespace on each line, removes quotes, then prints the key and value in order.
    public void print_json(String json_object) {
        String[] lines = json_object.split("\n");
        String[] current_line;

        for (int i = 0; i < lines.length; ++i) {
            current_line = lines[i].split(":", 2);
            
            if (current_line.length > 1) {
                current_line[0] = current_line[0].trim();
                current_line[0] = current_line[0].replace("\"", "");

                current_line[1] = current_line[1].trim();
                current_line[1] = current_line[1].replace("\"", "");

                System.out.println(current_line[0] + " : " + current_line[1]);
            }
        }
    }

    //method that collates the methods that receive the response headers and body, as the response consists of both portions
    //The method also doesn't have to throw an error, instead any errors can be handled here immediately
    //and the main function can immediately retry if the response makes no sense.
    //Takes no arguments, returns the a String array, [0] is the headers, [1] is the body.
    //Receives a body if the content-length header is sent, otherwise only receives headers (upto \r\n\r\n which is the end of http headers)
    public String[] receive_response() {
        String body_length_string = "";
        try {
            String response_headers = receive_response_headers();
            body_length_string = find_header_value(response_headers, "Content-Length");

            if (body_length_string == null) {
                String[] response = {response_headers};
                return response;
            }

            int body_length = Integer.parseInt(body_length_string);
            String response_body = receive_response_body(body_length);
            String[] response = {response_headers, response_body};
            return response;
        } catch (IOException e) {
            System.err.println("Failed to receive response!");
            return null;
        }
    }

    //method to close the connection for an open socket
    public void close_connection() throws IOException {
        socket_out.close();
        socket_in.close();
        get_socket.close();
        return;
    }

    //main function for the getclient, is what is called when the getclient is run.
    //Main function runs until a proper response is received, this can either be an error code (4** or 5**) or a success code (2**)
    //On each loop the client attempts to connect to the server, request a file using a get request, and then display the body of the reply if the request
    //is successful. If the reply is not successful or a body is not supplied to the client, then the client displays no content.
    public static void main(String[] args) {
        boolean success = false;
        while (!success) {
            try {
                GETClient client = new GETClient();
                String body, hostname;
                String[] response;
                int port_number, body_length;

                //if no URL is provided, exit
                if (args.length < 1) {
                    System.out.println("No hostname/port provided, client will close.");
                    return;
                }

                //resolves the hostname/port number from URL here
                hostname = client.get_hostname(args[0]);
                port_number = client.get_port(args[0]);

                //if hostname or port number is not valid/does not exist, client exits
                if (hostname.equals("") || port_number == -1) {
                    System.out.println("Failed to retrieve hostname/port number, client will close.");
                    return;
                }

                //attempts connection to server here
                client.connect_socket(hostname, port_number);

                //sends message to the server
                client.send_message(client.create_request(hostname, port_number));

                //waits on a response from the server
                response = client.receive_response();

                //stop try loop if response was successful (that can be any error or success code)
                if (response[0].contains(" 200 ") || response[0].contains(" 404 ") || response[0].contains(" 400 ") || response[0].contains(" 500 "))
                    success = true;

                //after any response, connection is ended (no pipelining for the client/server was specified)
                client.close_connection();

                //Either the body is displayed, or "no content received" (no body received in response)
                if (response.length > 1 && success) {
                    client.print_json(response[1]);
                } else if (response.length == 1 && success) {
                    System.out.println("No content received");
                }
                
            } catch (IOException e) {
                e.printStackTrace();
            }

            //wait after failure to try again
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

import java.net.*;
import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.LocalTime;

public class AggregationServerThread extends Thread {
    private Socket client_socket = null;
    private PrintWriter socket_out;
    private BufferedReader socket_in;

    //method that lets the AggregationServer call the Thread
    public AggregationServerThread(Socket client_socket) {
        super("AggregationServerThread");
        this.client_socket = client_socket;

        return;
    }

    //Method to receive the HTTP header portion of a response to a GET Request, reads until a combination
    //of four carriage return and newlines characters are read
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

    //method that collates the methods that receive the response headers and body, does not make
    //sense to keep the methods separate in the main as the reponse consists of the whole.
    //The method also doesn't have to throw an error, instead any errors can be handle here immediately
    //and the main function then knows to retry immediately and doesn't close
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

    //Sends message to the out stream, uses write and then flush to avoid extra information being appended by the
    //PrintWriter methods
    public void send_message(String message) throws IOException {
        socket_out.write(message);
        socket_out.flush();
        return;
    }

    //Finds the value of a header by using regex, matches whatever value is after the header up to
    //the carriage return
    public String find_header_value(String response_headers, String header_name) {
        Pattern header_pattern = Pattern.compile("(?<=" + header_name + ": )[^\\r]+");
        Matcher matcher = header_pattern.matcher(response_headers);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    //finds the request type by reading characters from a response up to the first whitespace
    public String find_request_type(String request_line) {
        String request_type = "";
        int i = 0;
        while (request_line.charAt(i) != ' ') {
            ++i;
        }
        request_type = request_line.substring(0, i);
        return request_type;
    }

    //reads a file character by character so that the formatting of a json file is maintained, otherwise using
    //a method such as readLine would delete newline characters causing formatting issues
    public String read_file(String file_name) throws IOException {
        String weather_file_contents = "";
        
        BufferedReader weather_file_stream = new BufferedReader(new FileReader(file_name));
        char current_char;

        while ((weather_file_stream.ready())) {
            weather_file_contents += (char) weather_file_stream.read();
        }

        weather_file_stream.close();

        return weather_file_contents;
    }

    //Writes a string to a new file, takes the name of the file as an argument, and the 
    //string to write to the file as an argument, returns nothing.
    public void write_new_file(String file_to_write, String to_write) throws IOException {
        File fresh_file = new File(file_to_write);

        if (fresh_file.createNewFile()) {
            FileWriter writer = new FileWriter(file_to_write);
            writer.write(to_write);
            writer.close();
        }
    }

    //Method to delete files, takes the name of the file to delete as an argument, returns nothing
    public void delete_file(String file_to_delete) {
        File file_ob = new File(file_to_delete);
        file_ob.delete();
    }

    //converts a JSON object to a String array. Takes the json object as a string, splits it around the 
    //colon and then trims any whitespace or commas before returning the String array created by the String
    //split function
    public String[] json_object_to_array(String json_object) {
        String[] split_json_object = json_object.split(":", 2);
        split_json_object[0] = split_json_object[0].trim();
        split_json_object[0] = split_json_object[0].replace("\"", "");
        split_json_object[1] = split_json_object[1].trim();
        split_json_object[1] = split_json_object[1].replace("\"", "");
        return split_json_object;
    }

    //updates a json file with a new json object, does this by reading in a file line by line, comparing each line key to
    //the given key, if there is a match, the entire line is replaced with the new one
    //Takes the name of the file to update, and the string to update it with as argument, since the output is to a file, the
    //server just assumes that it works and no return value is supplied
    public void update_json_file_with_object(String file_to_update, String update_string) {
        String file_contents = "";
        String result_file = "";
        boolean updated_line = false;
        String final_line_update_string = update_string;

        try {
            file_contents = read_file(file_to_update);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String[] file_lines = file_contents.split("\n");
        
        String[] update_string_key_value = json_object_to_array(update_string);
        String[] current_key_value;

        if (update_string.endsWith("\",")) {
            final_line_update_string = update_string.substring(0, update_string.length()-1);
        }

        //loops through each line in the file that is being updated, checking if the key matches with the line to be inserted,
        //if it does match, then the value for that key is updated in the stringified file which is written back to the file.
        //Otherwise if there is never a match, the JSON object is simply appended to the end of the file, before the closing curly brace.
        for (int i = 0; i < file_lines.length; ++i) {
            
            //if line contains a curly brace, it is not valid for processing, instead it is skipped over
            if (!file_lines[i].contains("{") && !file_lines[i].contains("}")) {
                current_key_value = json_object_to_array(file_lines[i]);
                
                if (current_key_value[0].equals(update_string_key_value[0])) {
                    file_lines[i] = update_string;
                    updated_line = true;
                }

                //remove comma at the end of the line, add it back later as it makes formatting easier
                if (file_lines[i].endsWith("\",")) {
                    file_lines[i] = file_lines[i].substring(0,file_lines[i].length()-1);
                }
            } 
        }

        //now another loop is used to create the resulting file from these operations
        for (int i = 0; i < file_lines.length; ++i) {

            //if this is the first, second to last, or last line, do not append a comma
            if (i == 0 || (i == file_lines.length-2 && updated_line)) {
                result_file += file_lines[i] + "\n";

            } else if (i == file_lines.length-1 && !updated_line) {
                result_file += final_line_update_string + "\n";
                result_file += file_lines[i];

            } else if (i == file_lines.length-1 && updated_line) {
                result_file += file_lines[i];

            } else {
                result_file += file_lines[i] + ",\n";
            }
        }

        //delete the old file
        delete_file(file_to_update);

        //then write a new one (hopefully)
        try {
            write_new_file(file_to_update, result_file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return;
    }

    //updates a json file by passing each valid line into the update_json_file_with_object method.
    //Takes the name of the file to update, the file to update it with as a string, and returns nothing
    public void update_json_file(String file_name, String update_file) {
        String[] update_file_lines = update_file.split("\n");

        for (int i = 0; i < update_file_lines.length; ++i) {
            
            if (!update_file_lines[i].matches("^ *[{}] *$")) {
                update_json_file_with_object(file_name, update_file_lines[i]);
            }
        }
        return;
    }

    //json parser which validates the basic JSON format in the assignment, basically just checks to make sure that the file
    //starts and ends with an open and closed curly brace respectively. Then checks if each line contains only a key and a value
    //and no extra characters that could affect parsing the file later, whitespaces are not checked as they can be anywhere and it
    //will still be a valid json. Takes the json file as a string argument, returns true if the json is valid, otherwise false.
    public boolean check_json_validity(String json_file) {
        String[] json_objects = json_file.split("\n");
        String[] key_and_value;

        //first line must contain a curly brace
        if (!json_objects[0].contains("{")) {
            return false;
        }

        //last line must contain a ending curly brace
        if (!json_objects[json_objects.length-1].contains("}")) {
            return false;
        }

        //now iterate through the list of objects
        for (int i = 1; i < json_objects.length-1; ++i) {
            key_and_value = json_objects[i].split(":", 2);
            if (key_and_value.length != 2) {
                return false;
            }

            //key can have whitespace, just needs to be surrounded quotes, the regex matches any whitespace before or after a key
            //but not any characters
            if (!key_and_value[0].matches("^ *(\".+\") *$")) {
                return false;
            }

            //checks value to see if it matches 
            if (!key_and_value[1].matches("^ *(\".+\")( *,* *)$") || key_and_value[1].matches("[^0-9., -]")) {
                return false;
            }
        }

        return true;
    }

    //Method to handle a GET request, takes a request that consists of soley headers (no body for a GET request), and returns a response string
    //
    public String handle_get_request(String request) {
        //regex pattern that matches anything after the GET up to the second whitespace
        String response = "HTTP/1.1 CODE MSG\r\n\r\n";

        //this regex pattern matches anything after the GET portion of a response aside from whitespace which terminates the match
        //works to find the resource string
        Pattern resource_pattern = Pattern.compile("(?<=(GET ))[A-Za-z0-9_./]+");

        //this regex pattern matches all alphanumeric characters and underscores appended by the .json file extention
        //works to find the name of the requested file, but lacks the .json extention at the end so it is added back later
        Pattern file_name_pattern = Pattern.compile("[\\w_]+(?=\\.json)");
        Matcher resource_matcher = resource_pattern.matcher(request);
        String new_response = "";

        //checks to see if a resource is found, and therefore the GET request is valid
        if (resource_matcher.find()) {
            String resource_location = resource_matcher.group();

            //then we search the resource for the file requested
            Matcher file_matcher = file_name_pattern.matcher(resource_location);

            //attempts to find the json file name
            if (file_matcher.find()) {
                //we find a json file here
                String requested_file = file_matcher.group();

                //add back the .json filetype (not matched with regex)
                requested_file += ".json";

                //open a Java file to this file, then we check if it exists
                File file_to_check = new File(requested_file);

                //here is a check to see if the file exists, if it is not then an error is thrown
                if (file_to_check.isFile()) {

                    try {
                        String file_contents = read_file(requested_file);
                        new_response = "HTTP/1.1 200 OK\r\n Content-Length: " + file_contents.length() + "\r\n\r\n" + file_contents;
                    } catch (IOException e) {
                        //if somehow the file is failed to be read, then internal server error is the response
                        response = response.replace("CODE", "500");
                        response = response.replace("MSG", "Internal Server Error");
                    }

                    response = new_response;
                } else {
                    //file doesn't exist, throw 404 error
                    response = response.replace("CODE", "404");
                    response = response.replace("MSG", "Not Found");
                }

            } else {
                //no .json file found in the requested resource, since only .json files are served, this request is not found
                response = response.replace("CODE", "404");
                response = response.replace("MSG", "Not Found");
            }

        } else {
            //if nothing is found after the GET, ie two whitespaces, the request is bad
            response = response.replace("CODE", "400");
            response = response.replace("MSG", "Bad Request");
        }
        
        return response;
    }

    //Method to handle a put request, takes a request as the argument, and returns a response to send
    //Works by first checking the request to see if it is valid (proper format, and request is to upload a json file)
    //Then handles it either as a new file or as an update to a file depending on if the file exists.
    //Also checks to see if the request actually has a body or not, and handles that accordingly
    public String handle_put_request(String[] request) {
        //regex pattern that matches anything after the GET up to the second whitespace
        String response = "HTTP/1.1 CODE MSG\r\n\r\n";

        //this regex pattern matches anything after the PUT portion of a response aside from whitespace which terminates the match
        //works to find the resource string
        Pattern resource_pattern = Pattern.compile("(?<=(PUT ))[A-Za-z0-9_./]+");

        //this regex pattern matches all alphanumeric characters and underscores appended by the .json file extention
        //works to find the name of the requested file, but lacks the .json extention at the end so it is added back later
        Pattern file_name_pattern = Pattern.compile("[\\w_]+(?=\\.json)");

        if (request.length != 2) {
            response = response.replace("CODE", "204");
            response = response.replace("MSG", "No Content");
            return response;
        }

        Matcher resource_matcher = resource_pattern.matcher(request[0]);
        String new_response = "";

        //check to see if the request has a valid resource
        if (resource_matcher.find()) {
            //if the request is valid (has a resource)
            String resource_location = resource_matcher.group();

            //then we search the resource for the file requested
            Matcher file_matcher = file_name_pattern.matcher(resource_location);

            if (file_matcher.find()) {

                //we find a json file here
                String requested_file = file_matcher.group();

                //add back the .json filetype (not matched with regex)
                requested_file += ".json";

                //checks if the json file is actually valid json or not
                if (!check_json_validity(request[1])) {
                    response = response.replace("CODE", "500");
                    response = response.replace("MSG", "Internal Server Error");
                    return response;
                }
                
                //if the file already exists, if so it is updated
                File check_file = new File(requested_file);
                if (check_file.isFile()) {
                    update_json_file(requested_file, request[1]);
                    store_timestamp(append_timestamp(find_ID_of_json(request[1])));
                    response = response.replace("CODE", "200");
                    response = response.replace("MSG", "OK");
                
                //otherwise, a new file is created
                } else {
                    try {
                        write_new_file(requested_file, request[1]);
                        store_timestamp(append_timestamp(find_ID_of_json(request[1])));
                        response = response.replace("CODE", "201");
                        response = response.replace("MSG", "HTTP_CREATED");

                    //if somehow the server fails properly write the file, appropriate response is sent
                    } catch (IOException e) {
                        response = response.replace("CODE", "500");
                        response = response.replace("MSG", "Internal Server Error");
                        return response;
                    }
                }

            } else {
                //requested to do something other than put a JSON file on the server, bad request
                response = response.replace("CODE", "400");
                response = response.replace("MSG", "Bad Request");
            }

        } else {
            //if the request is not properly formed (cannot find resource portion) send bad request
            response = response.replace("CODE", "400");
            response = response.replace("MSG", "Bad Request");
        }
        
        //response is always returned
        return response;
    }

    //Method to find the ID of a file, takes a json object as an argument, returns the value of the ID
    //Finds the ID using regex, how they work is explained in the method
    public String find_ID_of_json(String json_object) {
        //Pattern to find whatever is after the ID key, looks for "id": and then matches anything after that
        Pattern id_pattern = Pattern.compile("(?<=\"id\").+");

        //Since the value after the "id": match will be surrounded in quotes, this matches whatever is surrounded in quotes
        Pattern value_pattern = Pattern.compile("(?<=\").+(?=\")");

        Matcher id_pattern_matcher = id_pattern.matcher(json_object);
        
        if (id_pattern_matcher.find()) {
            Matcher value_pattern_matcher = value_pattern.matcher(id_pattern_matcher.group());

            if (value_pattern_matcher.find()) {
                return value_pattern_matcher.group();
            }
        }

        //if no ID is matched, nothing is returned
        return "";
    }

    //Method to append an expire time to an ID, takes an ID as an argument and returns the full string with timestamp
    public String append_timestamp(String id) {
        LocalTime time = LocalTime.now().plusSeconds(30);
        return id + ":" + time;
    }

    //Method that stores a timestamp, does this by appending the timestamp onto the end of the timestamp file by first reading
    //in the timestamp file as a string, then appending on the timestamp, then writing it back out into a new file. If the file
    //doesn't exist a new file is created and the timestamp is written to it.
    //Takes the timestamp to append as an argument, 
    public void store_timestamp(String timestamp) {
        File timestamp_file = new File("timestamp_file.txt");
        String file_contents = "";
        String[] id_stamp = timestamp.split(":", 2);

        if (timestamp_file.isFile()) {
            try {
                file_contents = read_file("timestamp_file.txt");
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (file_contents.contains(id_stamp[0])) {
                file_contents = file_contents.replaceAll(id_stamp[0] + ".+", timestamp);
            
            } else {
                file_contents += "\n" + timestamp;
            }

            timestamp_file.delete();
        } else {
            file_contents = timestamp;
        }

        try {
            FileWriter timestamp_writer = new FileWriter("timestamp_file.txt");
            timestamp_writer.write(file_contents.trim());
            timestamp_writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return;
    }

    //checks if an id is out of data based on if the local time is past the time stamp, takes the id as an argument and returns true if it is
    //out of date, or false if it is still valid.
    //Does this by reading in the timestamp for the ID from the timestamp file, and then comparing if the current time is after this time,
    //if it is after this time then 30 seconds has passed so the information is no longer valid
    public boolean check_id_invalid(String id) {
        Pattern id_pattern = Pattern.compile("(?<=" + id + ":).+");
        try {
            String file_contents = read_file("timestamp_file.txt");
            Matcher file_matcher = id_pattern.matcher(file_contents);

            if (file_matcher.find()) {
                String timestamp = file_matcher.group();

                LocalTime time = LocalTime.now();

                return time.isAfter(time.parse(timestamp));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    //Method that deletes any old weather files, takes no arguments and returns nothing
    //Iterates through any values in the timestamp file and deletes any old weather files with
    //information from out of date servers stored in the file
    public void delete_old_weather() {
        try {
            String timestamp_file = read_file("timestamp_file.txt");
            String weather_file = read_file("weather.json");

            String weather_file_id = find_ID_of_json(weather_file);
            
            if (check_id_invalid(weather_file_id)) {
                File old_weather_file = new File("weather.json");
                old_weather_file.delete();

                //now remove the old timestamp from the timestamp file by deleting it, then rewriting it without the outdated timestamp
                File old_timestamp_file = new File("timestamp_file.txt");
                old_timestamp_file.delete();

                timestamp_file = timestamp_file.replaceAll(("(" + weather_file_id + ".+)"), "");

                FileWriter timestamp_writer = new FileWriter("timestamp_file.txt");
                timestamp_writer.write(timestamp_file.trim());
                timestamp_writer.close();
            }

        } catch (IOException e) {
            return;
        }
        return;
    }

    //this is the code block that is actually run when AggregationServerThread is called, takes no parameters as those are handled
    //in the constructor. This method creates the IN and the OUT for the socket, before listening for a request from the connected client/server
    //then it handles the request appropriately by handing it to the suitable method (handle_put_request/hande_get_request), not suitable
    //requests get a bad request response.
    public void run() {
        try {
            socket_out = new PrintWriter(client_socket.getOutputStream(), true);
            socket_in = new BufferedReader(new InputStreamReader(client_socket.getInputStream()));
            boolean connected_to_client = true;
            String[] request;

            //while the server is connected to the client (unless the client drops out in which case the server cannot do anything)
            while (connected_to_client) {

                //wait for request
                request = receive_response();

                //check to see request type
                String request_type = find_request_type(request[0]);

                //pass the request to the appropriate method for it to be handled, then send the response and close the connection
                if (request_type.equals("GET")) {
                    delete_old_weather();
                    String response = handle_get_request(request[0]);
                    send_message(response);
                    connected_to_client = false;
                } else if (request_type.equals("PUT")) {
                    String response = handle_put_request(request);
                    send_message(response);
                    connected_to_client = false;
                } else {
                    //otherwise this request is not handled by the server
                    send_message("HTTP/1.1 400 Bad Request\r\n\r\n");
                    connected_to_client = false;
                }
            }

            //connection is closed here
            socket_in.close();
            socket_out.close();
            client_socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
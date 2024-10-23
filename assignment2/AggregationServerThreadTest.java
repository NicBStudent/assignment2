import java.io.*;
import java.net.*;
import static org.junit.Assert.assertEquals;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;
import java.time.LocalTime;

public class AggregationServerThreadTest {
    private static Socket placeholder_socket;

    //Creates placeholder sockets for the test as server requires one to be created
    @BeforeClass
    public static void setup_class() {
        placeholder_socket = new Socket();
    }

    //Test for the finding of a request type
    @Test
    public void find_request_type_test() {
        AggregationServerThread server_tester = new AggregationServerThread(placeholder_socket);
        String request = server_tester.find_request_type("GET /weather.json HTTP/1.1\r\n Host: hostname:4567\r\n\r\n");
        assertEquals("GET", request);
    }

    //Test to see if the read_file method properly reads a file
    @Test
    public void read_weather_file_test() {
        AggregationServerThread server_tester = new AggregationServerThread(placeholder_socket);
        String weather_file_contents = "";
        try {
            weather_file_contents = server_tester.read_file("weathertest.json");
        } catch (IOException e) {
            e.printStackTrace();
        }

        assertEquals("{\n    \"id\" : \"IDS60901\",\n    \"local_date_time\" : \"15/04:00pm\"\n}", weather_file_contents);
    }

    //Test 01 for handling a get request, this is the base case for a proper request
    @Test
    public void get_response_test01() {
        AggregationServerThread server_tester = new AggregationServerThread(placeholder_socket);
        String response = server_tester.handle_get_request("GET /weathertest.json HTTP/1.1\r\n Host: hostname:4567\r\n\r\n");
        String actual_response = "HTTP/1.1 200 OK\r\n Content-Length: 63\r\n\r\n{\n    \"id\" : \"IDS60901\",\n    \"local_date_time\" : \"15/04:00pm\"\n}";
        assertEquals(actual_response, response);
    }

    //Test 02 for handling a get request, this is for a resource that does not exist
    @Test
    public void get_response_test02() {
        AggregationServerThread server_tester = new AggregationServerThread(placeholder_socket);
        String response = server_tester.handle_get_request("GET garbage HTTP/1.1\r\n Host: hostname:4567\r\n\r\n");
        String actual_response = "HTTP/1.1 404 Not Found\r\n\r\n";
        assertEquals(actual_response, response);
    }

    //Test 03 for handling a get request, this is for a malformed GET request
    @Test
    public void get_response_test03() {
        AggregationServerThread server_tester = new AggregationServerThread(placeholder_socket);
        String response = server_tester.handle_get_request("GET   HTTP/1.1\r\n Host: hostname:4567\r\n\r\n");
        String actual_response = "HTTP/1.1 400 Bad Request\r\n\r\n";
        assertEquals(actual_response, response);
    }

    //Test 04 for handling a get request, this is for a json file that does not exist again but with a .json file extention
    @Test
    public void get_response_test04() {
        AggregationServerThread server_tester = new AggregationServerThread(placeholder_socket);
        String response = server_tester.handle_get_request("GET /notreal.json HTTP/1.1\r\n Host: hostname:4567\r\n\r\n");
        String actual_response = "HTTP/1.1 404 Not Found\r\n\r\n";
        assertEquals(actual_response, response);
    }

    //Test check if write_new_file method works properly, reads file back to compare
    @Test
    public void test_write_new_file() {
        AggregationServerThread server_tester = new AggregationServerThread(placeholder_socket);
        String actual_file_contents = "";
        try {
            server_tester.write_new_file("testwriter.json", "gbhdfsagbhdsfg.\n\r\n   &#$*Q*$#&*gjnfjdg1213143\n");
            actual_file_contents = server_tester.read_file("testwriter.json");
        } catch (IOException e) {
            e.printStackTrace();
        }

        assertEquals("gbhdfsagbhdsfg.\n\r\n   &#$*Q*$#&*gjnfjdg1213143\n", actual_file_contents);
    }

    //Test to check the split_json_object method, provides only a proper json object as this method will never see invalid objects
    @Test
    public void split_json_object_test() {
        AggregationServerThread server_tester = new AggregationServerThread(placeholder_socket);
        String json_object = "    \"local_date_time\" : \"15/04:00pm\"";
        String[] json_object_split = server_tester.json_object_to_array(json_object);
        String[] actual_split_object = {"local_date_time", "15/04:00pm"};
        assertEquals(actual_split_object[0], json_object_split[0]);
        assertEquals(actual_split_object[1], json_object_split[1]);
    }

    //Test 01 for update_json_file_with_object, this is for an object which replaces another object
    @Test
    public void update_json_file_with_object_test01() {
        AggregationServerThread server_tester = new AggregationServerThread(placeholder_socket);
        server_tester.update_json_file_with_object("weathertest2.json", "    \"local_date_time\" : \"12/04:00pm\"");
        String actual = "";
        String result = "";
        try {
            actual = server_tester.read_file("weathertest2result.json");
            result = server_tester.read_file("weathertest2.json");
        } catch (IOException e) {
            System.out.println("Test: update_json_file_with_object_test01: Exception");
        }
        assertEquals(actual, result);
    }

    //Test 01 for update_json_file_with_object, this is for an object which is appended to the end of the JSON file
    @Test
    public void update_json_file_with_object_test02() {
        AggregationServerThread server_tester = new AggregationServerThread(placeholder_socket);
        server_tester.update_json_file_with_object("weathertest3.json", "    \"state\" : \"SA\",");
        String actual = "";
        String result = "";
        try {
            actual = server_tester.read_file("weathertest3result.json");
            result = server_tester.read_file("weathertest3.json");
        } catch (IOException e) {
            System.out.println("Test: update_json_file_with_object_test02: Exception");
        }
        assertEquals(actual, result);
    }

    //Test 01 for check_json_validity, checks a valid JSON and compares to true
    @Test
    public void json_validity_test01() {
        AggregationServerThread server_tester = new AggregationServerThread(placeholder_socket);
        String json_file = "";
        try {
            json_file = server_tester.read_file("weathertest.json");
        } catch (IOException e) {
            System.out.println("Failed to read file in test: json_validity_test01");
        }
        assertEquals(true, server_tester.check_json_validity(json_file));
    }

    //Test 02 for check_json_validity, checks a invalid json that has invalid random characters
    @Test
    public void json_validity_test02() {
        AggregationServerThread server_tester = new AggregationServerThread(placeholder_socket);
        String json_file = "";
        try {
            json_file = server_tester.read_file("badweather.json");
        } catch (IOException e) {
            System.out.println("Failed to read file in test: json_validity_test02");
        }
        assertEquals(false, server_tester.check_json_validity(json_file));
    }

    //Test 03 for check_json_validity, checks an invalid json that is lacking a comma
    @Test
    public void json_validity_test03() {
        AggregationServerThread server_tester = new AggregationServerThread(placeholder_socket);
        String json_file = "";
        try {
            json_file = server_tester.read_file("badweather2.json");
        } catch (IOException e) {
            System.out.println("Failed to read file in test: json_validity_test03");
        }
        assertEquals(false, server_tester.check_json_validity(json_file));
    }

    //Test 04 for check_json_validity, checks an invalid json that is lacking a value for a key
    @Test
    public void json_validity_test04() {
        AggregationServerThread server_tester = new AggregationServerThread(placeholder_socket);
        String json_file = "";
        try {
            json_file = server_tester.read_file("badweather3.json");
        } catch (IOException e) {
            System.out.println("Failed to read file in test: json_validity_test04");
        }
        assertEquals(false, server_tester.check_json_validity(json_file));
    }

    //Test 05 for check_json_validity, checks an invalid json missing a close curly brace
    @Test
    public void json_validity_test05() {
        AggregationServerThread server_tester = new AggregationServerThread(placeholder_socket);
        String json_file = "";
        try {
            json_file = server_tester.read_file("badweather3.json");
        } catch (IOException e) {
            System.out.println("Failed to read file in test: json_validity_test04");
        }
        assertEquals(false, server_tester.check_json_validity(json_file));
    }

    //Test 06 for check_json_validity, checks an empty string
    @Test
    public void json_validity_test06() {
        AggregationServerThread server_tester = new AggregationServerThread(placeholder_socket);
        assertEquals(false, server_tester.check_json_validity(""));
    }

    //Test for the ID value finder, checks a regular json object and compares the result to what is expected
    @Test
    public void id_value_test() {
        AggregationServerThread server_tester = new AggregationServerThread(placeholder_socket);
        String id = server_tester.find_ID_of_json("{\n    \"id\" : \"IDS60901\",\n    \"local_date_time\" : \"15/04:00pm\"\n}");
        assertEquals("IDS60901", id);
    }

    //Test for appending timestamp to ID, compares against two values, time 35 seconds into the future as the append
    //method appends a time 30 seconds into the future, and 5 seconds into the past. This is because there is no 
    //guarantee by the system that there won't be slowdown between the calls, so 5 seconds is allowed as that would
    //not have a massive impact on actual functionality and prevents the test from throwing false negatives
    @Test
    public void append_timestamp_test() {
        AggregationServerThread server_tester = new AggregationServerThread(placeholder_socket);
        String time_stamp = server_tester.append_timestamp("IDS60901");
        String[] timestamp_id_and_time = time_stamp.split(":", 2);
        boolean valid = false;
        LocalTime timestamp_time = LocalTime.parse(timestamp_id_and_time[1]);

        LocalTime time_to_compare = LocalTime.now();

        LocalTime past_time = time_to_compare.minusSeconds(5);
        LocalTime future_time = time_to_compare.plusSeconds(35);

        if (timestamp_time.isBefore(future_time) && timestamp_time.isAfter(past_time)) {
            valid = true;
        } else {
            valid = false;
        }

        assertEquals(true, valid);
    }

    @Test
    //Test for storing timestamps
    public void timestamp_storage_test() {
        AggregationServerThread server_tester = new AggregationServerThread(placeholder_socket);
        String timestamp = server_tester.append_timestamp("ID78423");
        boolean valid = false;
        server_tester.store_timestamp(timestamp);
        
        try {
            String timestamp_file = server_tester.read_file("timestamp_file.txt");

            if (timestamp_file.contains(timestamp)) {
                valid = true;
            } else {
                valid = false;
            }
        } catch (IOException e) {
            System.out.println("Failed to read file at: timestamp_storage_test");
        }
        assertEquals(true, valid);
    }

    //Test 01 for handle_put_request, checks to see if a proper put request is handled properly for a new file
    @Test
    public void put_request_handling_test01() {
        AggregationServerThread server_tester = new AggregationServerThread(placeholder_socket);
        String actual_result = "";
        String expected_result = "";
        String[] request = {"PUT /puttest.json HTTP/1.1\r\n Host: hostname:4567\r\n\r\n", "{\n    \"id\" : \"IDS60901\",\n    \"local_date_time\" : \"15/04:00pm\"\n}"};
        String response = server_tester.handle_put_request(request);
        
        try {
            actual_result = server_tester.read_file("puttest.json");
            expected_result = "{\n    \"id\" : \"IDS60901\",\n    \"local_date_time\" : \"15/04:00pm\"\n}";
        } catch (IOException e) {
            System.out.println("Failed to read files for comparison: put_request_handling_test01");
        }
        assertEquals(expected_result, actual_result);
        assertEquals("HTTP/1.1 201 HTTP_CREATED\r\n\r\n", response);
    }

    //Test 02 for handle_put_request, checks to see if a proper put request is handled properly for pre-exisiting file
    @Test
    public void put_request_handling_test02() {
        AggregationServerThread server_tester = new AggregationServerThread(placeholder_socket);
        String[] request = {"PUT /puttest1.json HTTP/1.1\r\n Host: hostname:4567\r\n\r\n", "{\n    \"id\" : \"IDS60901\",\n    \"local_date_time\" : \"12/09:00pm\"\n}"};
        String response = server_tester.handle_put_request(request);
        String actual_result = "";
        String expected_result = "{\n    \"id\" : \"IDS60901\",\n    \"local_date_time\" : \"12/09:00pm\"\n}";
        
        try {
            actual_result = server_tester.read_file("puttest1.json");
        } catch (IOException e) {
            System.out.println("Failed to read file for comparison: put_request_handling_test02");
        }
        assertEquals(expected_result, actual_result);
        assertEquals("HTTP/1.1 200 OK\r\n\r\n", response);
    }

    //cleans up after testing, removes/resets files that were created/edited by first deleting all files, then
    //writing the fresh_file_contents string to each of them
    @AfterClass
    public static void cleanup() {
        //cleanup class
        File fresh_file = new File("testwriter.json");
        fresh_file.delete();

        File old_file2 = new File("weathertest2.json");
        File old_file3 = new File("weathertest3.json");
        File old_file4 = new File("puttest.json");
        File old_file5 = new File("puttest1.json");
        File old_file6 = new File("timestamp_file.txt");
        old_file2.delete();
        old_file3.delete();
        old_file4.delete();
        old_file5.delete();
        old_file6.delete();

        String fresh_file_contents = "{\n    \"id\" : \"IDS60901\",\n    \"local_date_time\" : \"15/04:00pm\"\n}";

        try {
            FileWriter writer2 = new FileWriter("weathertest2.json");
            writer2.write(fresh_file_contents);
            writer2.close();

            FileWriter writer3 = new FileWriter("weathertest3.json");
            writer3.write(fresh_file_contents);
            writer3.close();

            FileWriter writer4 = new FileWriter("puttest1.json");
            writer4.write(fresh_file_contents);
            writer4.close();
        } catch (IOException e) {
            System.out.println("failed to refresh testfile: weathertest2.json");
        }
    }
}
import java.io.*;
import java.net.*;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class GETClientTest {
    @Test
    public void check_port() {
        GETClient client = new GETClient();
        int port = client.get_port("hostname:4567");
        assertEquals(4567, port);
    }

    @Test
    public void check_hostname_type1() {
        GETClient client = new GETClient();
        String hostname = client.get_hostname("hostname:4567");
        assertEquals("hostname", hostname);
    }

    @Test
    public void check_hostname_type2() {
        GETClient client = new GETClient();
        String hostname = client.get_hostname("http://hostname:portnumber");
        assertEquals("hostname", hostname);
    }

    @Test
    public void check_hostname_type3() {
        GETClient client = new GETClient();
        String hostname = client.get_hostname("http://hostname.domain.domain:portnumber");
        assertEquals("hostname", hostname);
    }

    @Test
    public void check_print_json_output() {
        GETClient client = new GETClient();
        client.print_json("{\n    \"id\" : \"IDS60901\",\n    \"local_date_time\" : \"15/04:00pm\"\n}");
    }
/*
    @Test
    public void receive_test() {
        GETClient client = new GETClient();
        String message = "";
        try {
            client.connect_socket("127.0.0.1", 4567);
            client.send_message("hello\n");
            message = client.receive_message();
            client.send_message("Bye\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        assertEquals("HTTP/1.1 200 OK", message);
    }
*/
    @Test
    public void request_test() {
        GETClient client = new GETClient();
        String get_request = client.create_request("hostname", 4567);
        assertEquals(get_request, "GET /weather.json HTTP/1.1\r\n Host: hostname:4567\r\n\r\n");
    }
/*
    @Test
    public void receive_response_test() {
        GETClient client = new GETClient();
        String response = "";
        try {
            client.connect_socket("127.0.0.1", 4567);
            client.send_message("hello\n");
            response = client.receive_response();
            client.send_message("Bye\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        assertEquals("HTTP/1.1 200 OK\n\r Content-Length: 38\n\r\n\r", response);
    }
*/
    @Test
    public void find_header_value_test() {
        GETClient client = new GETClient();
        String header_value = client.find_header_value("HTTP/1.1 200 OK\r\n Content-Length: 123\r\n\r\n" ,"Content-Length");
        assertEquals("123", header_value);
    }

    @Test
    public void receive_response_body_test() {
        GETClient client = new GETClient();
        String body = "";
        int body_length = 0;
        String response = "";
        try {
            client.connect_socket("127.0.0.1", 4567);
            client.send_message("hello\n");
            response = client.receive_response_headers();
            body_length = Integer.parseInt(client.find_header_value(response, "Content-Length"));
            body = client.receive_response_body(body_length);
            client.send_message("Bye\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
  
        assertEquals("this is the body\n and this is the rest", body);
    }
}
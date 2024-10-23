import java.io.*;
import java.net.*;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ContentServerTest {

    @Test
    public void test_string_to_json() {
        ContentServer server = new ContentServer();
        String test_string = server.create_json_string("id:IDS60901");
        assertEquals("    \"id\" : \"IDS60901\",\n", test_string);
    }

    @Test
    public void test_string_to_json_special_chars() {
        ContentServer server = new ContentServer();
        String test_string = server.create_json_string("local_date_time:15/04:00pm");
        assertEquals("    \"local_date_time\" : \"15/04:00pm\",\n", test_string);
    }

    @Test
    public void test_create_json_body() {
        ContentServer server = new ContentServer();
        String json_object = server.create_json_body("testinputfile.txt");
        assertEquals("{\n    \"id\" : \"IDS60901\",\n    \"local_date_time\" : \"15/04:00pm\"\n}", json_object);
    }

    @Test
    public void test_create_put_request() {
        ContentServer server = new ContentServer();
        String request = server.create_put_request("{\n    \"id\" : \"IDS60901\",\n    \"local_date_time\" : \"15/04:00pm\"\n}");
        assertEquals("PUT /weather.json HTTP/1.1\r\nUser-Agent: ATOMClient/1/0\r\nContent-Type: text/plain\r\nContent-Length: 63\r\n\r\n{\n    \"id\" : \"IDS60901\",\n    \"local_date_time\" : \"15/04:00pm\"\n}", request);
    }
}
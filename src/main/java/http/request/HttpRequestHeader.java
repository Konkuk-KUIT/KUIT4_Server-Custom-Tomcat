package http.request;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HttpRequestHeader {

    private Map<String, String> headerMap;
    private BufferedReader br;

    private HttpRequestHeader(BufferedReader br) throws IOException {
        this.headerMap = new HashMap<>();
        this.br = br;
    }

    public static HttpRequestHeader from(BufferedReader br) throws IOException {
        return new HttpRequestHeader(br);
    }

    public String getValue(String key) {
        return headerMap.get(key);
    }

    public void parseHeader() throws IOException {

        String line;
        while (!(line = br.readLine()).isEmpty()) {

            String key = line.split(": ")[0];
            String value = line.split(": ")[1];

            headerMap.put(key, value);
        }
    }

    public boolean containsKey(String key) {
        return headerMap.containsKey(key);
    }
}

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class TestConnection {
    public static void main(String[] args) {
        String host = "172.187.233.15:8181";
        String token = "apiv3_a1PqQhJopy_7fAFCYvbU6Bj7b0tNrYuCdD_ZydtXoEe_nqTReOB29OMFcZ7o_VBkPVSwK3o-ODnu5Gy4eeFfuQ";
        String database = "test";
        String query = "SHOW MEASUREMENTS";
        
        try {
            System.out.println("Testing InfluxDB connection...");
            System.out.println("Host: " + host);
            System.out.println("Database: " + database);
            System.out.println("Query: " + query);
            System.out.println();
            
            String urlString = "http://" + host + "/query";
            
            // Build query parameters
            String params = String.format("p=%s&db=%s&q=%s",
                URLEncoder.encode(token, StandardCharsets.UTF_8),
                URLEncoder.encode(database, StandardCharsets.UTF_8),
                URLEncoder.encode(query, StandardCharsets.UTF_8)
            );
            
            URL url = new URL(urlString + "?" + params);
            System.out.println("Full URL: " + url);
            System.out.println();
            
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            // Set request method and headers
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            
            System.out.println("Connecting...");
            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read successful response
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                System.out.println("SUCCESS! Response:");
                System.out.println(response.toString());
            } else {
                // Read error response
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                System.out.println("ERROR! Response:");
                System.out.println(response.toString());
            }
            
        } catch (Exception e) {
            System.err.println("Exception occurred:");
            e.printStackTrace();
        }
    }
} 
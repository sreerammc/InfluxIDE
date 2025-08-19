import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class TestMoreQueries {
    public static void main(String[] args) {
        String host = "172.187.233.15:8181";
        String token = "apiv3_a1PqQhJopy_7fAFCYvbU6Bj7b0tNrYuCdD_ZydtXoEe_nqTReOB29OMFcZ7o_VBkPVSwK3o-ODnu5Gy4eeFfuQ";
        String database = "test";
        
        String[] queries = {
            "SHOW MEASUREMENTS",
            "SHOW TABLES",
            "SELECT * FROM sensor_f LIMIT 5",
            "SELECT * FROM temperature LIMIT 3",
            "SHOW SERIES FROM sensor_f",
            "SHOW FIELD KEYS FROM sensor_f"
        };
        
        for (String query : queries) {
            testQuery(host, token, database, query);
            System.out.println("=".repeat(80));
            System.out.println();
        }
    }
    
    private static void testQuery(String host, String token, String database, String query) {
        try {
            System.out.println("Testing Query: " + query);
            
            String urlString = "http://" + host + "/query";
            
            // Build query parameters
            String params = String.format("p=%s&db=%s&q=%s",
                URLEncoder.encode(token, StandardCharsets.UTF_8),
                URLEncoder.encode(database, StandardCharsets.UTF_8),
                URLEncoder.encode(query, StandardCharsets.UTF_8)
            );
            
            URL url = new URL(urlString + "?" + params);
            
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            
            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                System.out.println("SUCCESS! Response length: " + response.length() + " characters");
                if (response.length() < 500) {
                    System.out.println("Full response: " + response.toString());
                } else {
                    System.out.println("First 500 chars: " + response.substring(0, 500) + "...");
                }
            } else {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                System.out.println("ERROR! Response: " + response.toString());
            }
            
        } catch (Exception e) {
            System.err.println("Exception for query '" + query + "': " + e.getMessage());
            e.printStackTrace();
        }
    }
} 
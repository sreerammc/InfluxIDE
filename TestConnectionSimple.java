import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class TestConnectionSimple {
    public static void main(String[] args) {
        testConnection();
    }
    
    private static void testConnection() {
        String host = "172.187.233.15:8181";
        String token = "apiv3_a1PqQhJopy_7fAFCYvbU6Bj7b0tNrYuCdD_ZydtXoEe_nqTReOB29OMFcZ7o_VBkPVSwK3o-ODnu5Gy4eeFfuQ";
        String database = "test";
        String query = "SHOW MEASUREMENTS";
        
        System.out.println("=== Testing InfluxDB Connection ===");
        System.out.println("Host: " + host);
        System.out.println("Database: " + database);
        System.out.println("Query: " + query);
        System.out.println();
        
        try {
            // Build URL exactly like the application does
            String urlString = "http://" + host + "/query";
            String params = String.format("p=%s&db=%s&q=%s",
                URLEncoder.encode(token, StandardCharsets.UTF_8),
                URLEncoder.encode(database, StandardCharsets.UTF_8),
                URLEncoder.encode(query, StandardCharsets.UTF_8)
            );
            
            URL url = new URL(urlString + "?" + params);
            System.out.println("Full URL: " + url);
            System.out.println();
            
            // Test connection
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            
            System.out.println("Attempting connection...");
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
                
                System.out.println("SUCCESS! Response:");
                System.out.println(response.toString());
            } else {
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
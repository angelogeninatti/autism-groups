package utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class RecaptchaUtils {
    private final String V3_SECRET;
    private final String V2_SECRET;

    public RecaptchaUtils(String v3_secret, String v2_secret) {
        this.V3_SECRET = v3_secret;
        this.V2_SECRET = v2_secret;
    }

    public String getDateTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        return now.format(formatter);
    }

    public boolean verifyRecaptcha(String token, String version, String expectedAction) {
        return true;
        /*String secret = "v3".equals(version) ? V3_SECRET : V2_SECRET;
        try {
            String url = "https://www.google.com/recaptcha/api/siteverify";
            String params = "secret=" + secret + "&response=" + token;

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(params))
                    .build();

            HttpResponse<String> httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            JsonObject json = JsonParser.parseString(httpResponse.body()).getAsJsonObject();
            System.out.println("[ " + getDateTime() + "] " + "Recaptcha response: " + json);
            if ("v3".equals(version)) {
                return json.get("success").getAsBoolean()
                        && json.get("score").getAsFloat() > 0.5
                        && expectedAction.equals(json.get("action").getAsString());
            } else { // v2
                return json.get("success").getAsBoolean();
            }
        } catch (Exception e) {
            return false;
        }*/
    }
}
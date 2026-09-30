package br.com.marcusferreira.voting;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Drives the API the way the mobile app does (Annex 1): it only follows the screens. Selecting an
 * item or pressing a button POSTs to its url with its body plus the filled inputs keyed by id.
 */
public class MobileAppSimulator {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final TestRestTemplate restTemplate;
    private HttpStatusCode lastStatus;

    public MobileAppSimulator(TestRestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Map<String, Object> open(String path) {
        return read(restTemplate.getForEntity(path, String.class));
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> select(Map<String, Object> screen, String itemText) {
        assertThat(screen.get("tipo")).isEqualTo("SELECAO");
        Map<String, Object> item = items(screen).stream()
            .filter(candidate -> itemText.equals(candidate.get("texto")))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No item '" + itemText + "' in " + screen));
        Map<String, Object> body = item.get("body") != null ? (Map<String, Object>) item.get("body") : Map.of();
        return post((String) item.get("url"), body);
    }

    public Map<String, Object> pressOk(Map<String, Object> screen, Map<String, Object> inputs) {
        return press(screen, "botaoOk", inputs);
    }

    public Map<String, Object> pressCancel(Map<String, Object> screen) {
        return press(screen, "botaoCancelar", Map.of());
    }

    public HttpStatusCode lastStatus() {
        return lastStatus;
    }

    public static List<Map<String, Object>> items(Map<String, Object> screen) {
        return MAPPER.convertValue(screen.get("itens"), new TypeReference<>() {});
    }

    public static List<String> inputIds(Map<String, Object> screen) {
        return items(screen).stream()
            .filter(item -> String.valueOf(item.get("tipo")).startsWith("INPUT_"))
            .map(item -> (String) item.get("id"))
            .toList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> press(Map<String, Object> screen, String buttonName, Map<String, Object> inputs) {
        assertThat(screen.get("tipo")).isEqualTo("FORMULARIO");
        Map<String, Object> button = (Map<String, Object>) screen.get(buttonName);
        assertThat(button).as("button %s in %s", buttonName, screen).isNotNull();
        assertThat(inputIds(screen)).as("inputs of %s", screen).containsAll(inputs.keySet());

        Map<String, Object> body = new HashMap<>();
        items(screen).stream()
            .filter(item -> String.valueOf(item.get("tipo")).startsWith("INPUT_"))
            .forEach(item -> body.put((String) item.get("id"), inputs.getOrDefault(item.get("id"), item.get("valor"))));
        if (button.get("body") != null) {
            body.putAll((Map<String, Object>) button.get("body"));
        }
        return post((String) button.get("url"), body);
    }

    private Map<String, Object> post(String url, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Screen URLs are absolute on the configured public base URL; the test server runs on a
        // random port, so only the path and query are followed.
        URI uri = URI.create(url);
        String path = uri.getRawPath() + (uri.getRawQuery() != null ? "?" + uri.getRawQuery() : "");
        return read(restTemplate.postForEntity(path, new HttpEntity<>(body, headers), String.class));
    }

    private Map<String, Object> read(ResponseEntity<String> response) {
        lastStatus = response.getStatusCode();
        try {
            return MAPPER.readValue(response.getBody(), new TypeReference<>() {});
        } catch (Exception e) {
            throw new AssertionError("Not a screen (HTTP " + response.getStatusCode() + "): " + response.getBody(), e);
        }
    }
}

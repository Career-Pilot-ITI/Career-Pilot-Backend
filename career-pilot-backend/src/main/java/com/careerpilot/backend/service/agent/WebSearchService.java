package com.careerpilot.backend.service.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class WebSearchService {

    private final RestTemplate restTemplate;
    private final String provider;

    public WebSearchService(RestTemplate restTemplate,
                            @Value("${app.web-search.provider:duckduckgo}") String provider) {
        this.restTemplate = restTemplate;
        this.provider = provider;
    }

    public String search(String query) {
        if ("none".equalsIgnoreCase(provider)) {
            return "Web search is not configured.";
        }

        if ("duckduckgo".equalsIgnoreCase(provider)) {
            return searchDuckDuckGo(query);
        }

        return "Unknown search provider: " + provider;
    }

    private String searchDuckDuckGo(String query) {
        try {
            String url = "https://lite.duckduckgo.com/lite/?q="
                    + URLEncoder.encode(query, StandardCharsets.UTF_8);
            String html = restTemplate.getForObject(url, String.class);
            if (html == null || html.isBlank()) {
                return "No results found.";
            }
            return extractResults(html);
        } catch (Exception e) {
            log.warn("DuckDuckGo search failed: {}", e.getMessage());
            return "Web search unavailable. The LLM should rely on its training data.";
        }
    }

    private String extractResults(String html) {
        StringBuilder sb = new StringBuilder();
        int idx = 0;
        int resultCount = 0;
        String searchMarker = "<a rel=\"nofollow\" href=\"";
        while ((idx = html.indexOf(searchMarker, idx)) != -1 && resultCount < 5) {
            int hrefStart = idx + searchMarker.length();
            int hrefEnd = html.indexOf("\"", hrefStart);
            if (hrefEnd == -1) break;
            String link = html.substring(hrefStart, hrefEnd);

            int textStart = html.indexOf(">", hrefEnd) + 1;
            int textEnd = html.indexOf("</a>", textStart);
            if (textEnd == -1) break;

            String text = html.substring(textStart, textEnd)
                    .replaceAll("<[^>]+>", "")
                    .strip();

            if (!text.isBlank()) {
                sb.append(++resultCount).append(". ").append(text).append("\n   ").append(link).append("\n\n");
            }
            idx = textEnd + 4;
        }

        if (sb.isEmpty()) {
            return "No relevant web results found.";
        }
        return "Web search results for context (latest trends and practices):\n" + sb;
    }
}

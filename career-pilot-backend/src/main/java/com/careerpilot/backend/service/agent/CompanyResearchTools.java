package com.careerpilot.backend.service.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CompanyResearchTools {

    private final WebSearchService webSearchService;

    @Tool(description = "Search the web for public information about a company: funding, projects, "
            + "core values, products, or recent news. Use this to gather facts for a personalized "
            + "cover letter. Pass maxChars > 0 to trim the result to that many characters. "
            + "Returns the search results as text, or a message when nothing useful is found.")
    public String searchCompany(String query, int maxChars) {
        String results = webSearchService.search(query);
        if (results == null || results.isBlank()) {
            return "No usable company information found for query: " + query;
        }
        if (maxChars > 0 && results.length() > maxChars) {
            return results.substring(0, maxChars);
        }
        return results;
    }
}

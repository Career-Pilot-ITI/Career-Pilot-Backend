package com.careerpilot.backend.utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PiiRedactionUtil {

  record PiiPattern(String ruleId, Pattern pattern) {
  }

  record PiiMatch(String ruleId, String value, int start, int end) {
  }

  private static final List<PiiPattern> PATTERNS = List.of(
      new PiiPattern("email",
          Pattern.compile("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}")),

      new PiiPattern("url",
          Pattern.compile("https?://[\\w\\-]+(\\.[\\w\\-]+)+(/[^\\s]*)?")),

      new PiiPattern("phone-eg",
          Pattern.compile("\\+20\\s?1[0125]\\d{8}")),

      new PiiPattern("phone-intl",
          Pattern.compile("\\+[1-9]\\d{1,14}")),

      new PiiPattern("ipv4",
          Pattern.compile("\\b((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\b")),

      new PiiPattern("credit-card",
          Pattern.compile("\\b(?:\\d[ -]?){13,16}\\b")));

  /**
   * Scans content and returns all PII matches found, sorted longest-first
   * to prevent partial replacements corrupting longer overlapping matches.
   */
  private static List<PiiMatch> scan(String content) {
    List<PiiMatch> matches = new ArrayList<>();

    for (PiiPattern piiPattern : PATTERNS) {
      Matcher matcher = piiPattern.pattern().matcher(content);
      while (matcher.find()) {
        matches.add(new PiiMatch(
            piiPattern.ruleId(),
            matcher.group(),
            matcher.start(),
            matcher.end()));
      }
    }

    matches.sort(Comparator.comparingInt(m -> -m.value().length()));

    return matches;
  }

  /**
   * Replaces all detected PII in content with [REDACTED:rule-id] placeholders.
   * One-way no restoration. Use this for logging, analytics, etc.
   */
  public static String redact(String content) {
    if (content == null || content.isBlank())
      return content;

    List<PiiMatch> matches = scan(content);
    String redacted = content;

    for (PiiMatch match : matches) {
      redacted = redacted.replace(match.value(), "[REDACTED:" + match.ruleId() + "]");
    }

    return redacted;
  }

  /**
   * Replaces all detected PII with indexed placeholders so they can be restored
   * later.
   * Use this before sending to LLM when you need to restore the values afterward.
   *
   * @return a RedactionResult holding the redacted string + the index to restore
   *         from
   */
  public static RedactionResult redactWithIndex(String content) {
    if (content == null || content.isBlank()) {
      return new RedactionResult(content, List.of());
    }

    List<PiiMatch> matches = scan(content);
    String redacted = content;
    List<RedactionEntry> index = new ArrayList<>();
    Map<String, String> valueToPlaceholder = new LinkedHashMap<>();
    int counter = 0;

    for (PiiMatch match : matches) {
      if (valueToPlaceholder.containsKey(match.value())) {
        continue;
      }
      String placeholder = "[REDACTED:" + match.ruleId() + ":" + counter++ + "]";
      valueToPlaceholder.put(match.value(), placeholder);
      index.add(new RedactionEntry(placeholder, match.value()));
      redacted = redacted.replace(match.value(), placeholder);
    }

    return new RedactionResult(redacted, index);
  }

  public record RedactionEntry(String placeholder, String originalValue) {
  }

  public record RedactionResult(String redactedContent, List<RedactionEntry> index) {

    /**
     * Restores original values from the index — call this before executing LLM
     * output
     */
    public String restore(String content) {
      String restored = content;
      for (RedactionEntry entry : index) {
        restored = restored.replace(entry.placeholder(), entry.originalValue());
      }
      return restored;
    }
  }
}

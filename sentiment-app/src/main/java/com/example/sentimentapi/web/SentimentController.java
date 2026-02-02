package com.example.sentimentapi.web;

import com.example.sentimentapi.service.SentimentService;
import com.example.sentimentapi.service.SentimentService.SentimentResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for sentiment analysis endpoints.
 * Provides both simple and detailed sentiment analysis.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://127.0.0.1:5500", "http://localhost:5500"})
public class SentimentController {

    private final SentimentService sentimentService;

    public SentimentController(SentimentService sentimentService) {
        this.sentimentService = sentimentService;
    }

    /**
     * Simple sentiment classification endpoint.
     * Returns just the sentiment label.
     * 
     * @param text The text to analyze
     * @return Sentiment label: positive, negative, or neutral
     */
    @GetMapping("/sentiment")
    public Map<String, String> getSentiment(@RequestParam String text) {
        String sentiment = sentimentService.classify(text);
        return Map.of("sentiment", sentiment, "text", text);
    }

    /**
     * Detailed sentiment analysis endpoint.
     * Returns sentiment with confidence scores and probability distribution.
     * 
     * @param text The text to analyze
     * @return Detailed sentiment analysis result
     */
    @GetMapping("/sentiment/detailed")
    public ResponseEntity<Map<String, Object>> getDetailedSentiment(@RequestParam String text) {
        SentimentResult result = sentimentService.analyze(text);
        
        Map<String, Object> response = new HashMap<>();
        response.put("text", text);
        response.put("sentiment", result.sentiment());
        response.put("confidence", String.format("%.2f%%", result.confidence() * 100));
        
        Map<String, String> scores = new HashMap<>();
        scores.put("veryNegative", String.format("%.2f%%", result.scores()[0] * 100));
        scores.put("negative", String.format("%.2f%%", result.scores()[1] * 100));
        scores.put("neutral", String.format("%.2f%%", result.scores()[2] * 100));
        scores.put("positive", String.format("%.2f%%", result.scores()[3] * 100));
        scores.put("veryPositive", String.format("%.2f%%", result.scores()[4] * 100));
        
        response.put("scores", scores);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Batch sentiment analysis endpoint.
     * Analyzes multiple texts in a single request.
     * 
     * @param request Request body containing an array of texts
     * @return Array of sentiment results
     */
    @PostMapping("/sentiment/batch")
    public ResponseEntity<Map<String, Object>> analyzeBatch(@RequestBody BatchRequest request) {
        Map<String, Object> response = new HashMap<>();
        var results = request.texts().stream()
            .map(text -> {
                SentimentResult result = sentimentService.analyze(text);
                Map<String, Object> item = new HashMap<>();
                item.put("text", text);
                item.put("sentiment", result.sentiment());
                item.put("confidence", result.confidence());
                return item;
            })
            .toList();
        
        response.put("results", results);
        response.put("count", results.size());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Request body for batch sentiment analysis.
     */
    public record BatchRequest(java.util.List<String> texts) {
    }
}

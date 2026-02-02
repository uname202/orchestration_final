package com.example.sentimentapi.service;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.CoreMap;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Properties;

/**
 * Advanced sentiment analysis service using Stanford CoreNLP.
 * Provides deep learning-based sentiment classification with confidence scores.
 */
@Service
public class SentimentService {

    private static final Logger logger = LoggerFactory.getLogger(SentimentService.class);
    
    private StanfordCoreNLP pipeline;

    @PostConstruct
    public void init() {
        logger.info("Initializing Stanford CoreNLP pipeline for sentiment analysis...");
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, parse, sentiment");
        // Use default models - they will be loaded from the classpath
        this.pipeline = new StanfordCoreNLP(props);
        logger.info("Stanford CoreNLP pipeline initialized successfully");
    }

    /**
     * Analyzes the sentiment of the given text.
     * 
     * @param text The text to analyze
     * @return SentimentResult containing sentiment label and confidence scores
     */
    public SentimentResult analyze(String text) {
        if (text == null || text.isBlank()) {
            return new SentimentResult("neutral", 1.0, new double[]{0, 0, 1, 0, 0});
        }

        try {
            Annotation annotation = pipeline.process(text);
            
            // Aggregate sentiment across all sentences
            int totalSentiment = 0;
            int sentenceCount = 0;
            double[] aggregatedScores = new double[5]; // very negative, negative, neutral, positive, very positive
            
            for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
                Tree tree = sentence.get(SentimentCoreAnnotations.SentimentAnnotatedTree.class);
                int sentiment = RNNCoreAnnotations.getPredictedClass(tree);
                double[] scores = getScoresFromTree(tree);
                
                totalSentiment += sentiment;
                sentenceCount++;
                
                // Aggregate scores
                for (int i = 0; i < 5; i++) {
                    aggregatedScores[i] += scores[i];
                }
            }
            
            // Average the scores
            for (int i = 0; i < 5; i++) {
                aggregatedScores[i] /= sentenceCount;
            }
            
            // Calculate average sentiment
            double avgSentiment = (double) totalSentiment / sentenceCount;
            String label = getSentimentLabel(avgSentiment);
            double confidence = aggregatedScores[(int) Math.round(avgSentiment)];
            
            return new SentimentResult(label, confidence, aggregatedScores);
            
        } catch (Exception e) {
            logger.error("Error analyzing sentiment for text: {}", text, e);
            return new SentimentResult("error", 0.0, new double[]{0, 0, 0, 0, 0});
        }
    }

    /**
     * Classifies text into simple sentiment categories.
     * 
     * @param text The text to classify
     * @return Sentiment label: "positive", "negative", or "neutral"
     */
    public String classify(String text) {
        SentimentResult result = analyze(text);
        return result.sentiment();
    }

    private double[] getScoresFromTree(Tree tree) {
        double[] scores = new double[5];
        for (int i = 0; i < 5; i++) {
            scores[i] = RNNCoreAnnotations.getPredictions(tree).get(i);
        }
        return scores;
    }

    private String getSentimentLabel(double sentiment) {
        // Stanford CoreNLP uses 0-4 scale:
        // 0 = very negative, 1 = negative, 2 = neutral, 3 = positive, 4 = very positive
        if (sentiment < 1.5) {
            return "negative";
        } else if (sentiment > 2.5) {
            return "positive";
        } else {
            return "neutral";
        }
    }

    /**
     * Record class to hold sentiment analysis results.
     * 
     * @param sentiment The sentiment label
     * @param confidence The confidence score for the prediction
     * @param scores Array of probabilities for each sentiment class [very negative, negative, neutral, positive, very positive]
     */
    public record SentimentResult(String sentiment, double confidence, double[] scores) {
    }
}

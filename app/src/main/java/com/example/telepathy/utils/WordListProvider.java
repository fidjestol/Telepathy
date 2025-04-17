package com.example.telepathy.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WordListProvider {
    private static final Map<String, List<String>> categoryWords = new HashMap<>();

    static {
        // Initialize word lists for each category
        categoryWords.put("Animals", Arrays.asList(
                "cat", "dog", "elephant", "giraffe", "lion", "tiger", "zebra", "monkey",
                "bear", "wolf", "fox", "rabbit", "deer", "horse", "cow", "pig",
                "sheep", "goat", "chicken", "duck", "penguin", "eagle", "owl", "hawk"));

        categoryWords.put("Countries", Arrays.asList(
                "usa", "canada", "mexico", "brazil", "france", "germany", "italy", "spain",
                "china", "japan", "india", "russia", "australia", "egypt", "nigeria", "kenya",
                "norway", "sweden", "denmark", "finland", "ireland", "poland", "greece", "turkey"));

        categoryWords.put("Foods", Arrays.asList(
                "pizza", "pasta", "burger", "sushi", "rice", "bread", "salad", "soup",
                "cake", "cookie", "apple", "banana", "orange", "grape", "carrot", "potato",
                "tomato", "onion", "chicken", "beef", "fish", "pork", "cheese", "milk"));

        categoryWords.put("Sports", Arrays.asList(
                "soccer", "football", "basketball", "baseball", "tennis", "golf", "hockey", "rugby",
                "volleyball", "cricket", "boxing", "swimming", "cycling", "running", "skiing", "skating",
                "surfing", "climbing", "yoga", "karate", "judo", "wrestling", "rowing", "sailing"));
    }

    public static List<String> getWordsForCategory(String category) {
        if (category == null)
            return new ArrayList<>();
        return categoryWords.getOrDefault(category.toLowerCase(), new ArrayList<>());
    }
}
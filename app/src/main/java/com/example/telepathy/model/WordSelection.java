package com.example.telepathy.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class WordSelection {
    private static final Random random = new Random();

    // Maps categories to lists of words
    private static final Map<String, List<String>> categoryWords = new HashMap<>();

    // Initialize word lists
    static {
        // Animals category
        categoryWords.put("Animals", Arrays.asList(
                "dog", "cat", "elephant", "tiger", "lion", "giraffe", "zebra", "monkey",
                "bear", "wolf", "fox", "deer", "rabbit", "squirrel", "mouse", "rat",
                "eagle", "hawk", "owl", "penguin", "dolphin", "whale", "shark", "snake",
                "turtle", "crocodile", "frog", "spider", "ant", "bee", "butterfly"));

        // Countries category
        categoryWords.put("Countries", Arrays.asList(
                "usa", "canada", "mexico", "brazil", "argentina", "chile", "peru",
                "france", "germany", "italy", "spain", "portugal", "england", "ireland",
                "russia", "china", "japan", "india", "australia", "egypt", "nigeria",
                "kenya", "south africa", "morocco", "greece", "turkey", "sweden", "norway"));

        // Foods category
        categoryWords.put("Foods", Arrays.asList(
                "pizza", "burger", "pasta", "rice", "bread", "potato", "tomato", "onion",
                "carrot", "broccoli", "apple", "banana", "orange", "strawberry", "grape",
                "chicken", "beef", "pork", "fish", "egg", "milk", "cheese", "yogurt",
                "ice cream", "chocolate", "cake", "cookie", "pie", "soup", "salad"));

        // Sports category
        categoryWords.put("Sports", Arrays.asList(
                "soccer", "football", "basketball", "baseball", "tennis", "golf", "hockey",
                "volleyball", "swimming", "running", "cycling", "skiing", "snowboarding",
                "surfing", "boxing", "wrestling", "karate", "judo", "gymnastics", "cricket",
                "rugby", "badminton", "table tennis", "bowling", "skating", "climbing"));
    }

    /**
     * Get the entire list of words for the specified category
     * 
     * @param category The category to get words from
     * @return List of all words in the category
     */
    public static List<String> getAllWordsForCategory(String category) {
        List<String> wordList = categoryWords.get(category);

        if (wordList == null) {
            // Default to Animals if category not found
            wordList = categoryWords.get("Animals");
            System.out.println("TELEPATHY: Category not found, defaulting to Animals");
        }

        // Return the entire category list
        System.out.println("TELEPATHY: Returning " + wordList.size() + " words for category: " + category);
        return new ArrayList<>(wordList);
    }

    /**
     * Get a list of random words from the specified category
     * 
     * @param category The category to select words from
     * @param count    The number of words to select
     * @return List of randomly selected words
     */
    public static List<String> getRandomWords(String category, int count) {
        List<String> wordList = categoryWords.get(category);

        if (wordList == null) {
            // Default to Animals if category not found
            wordList = categoryWords.get("Animals");
            System.out.println("TELEPATHY: Category not found, defaulting to Animals");
        }

        // Create a copy of the word list
        List<String> availableWords = new ArrayList<>(wordList);
        List<String> selectedWords = new ArrayList<>();

        // Select random words
        for (int i = 0; i < count && !availableWords.isEmpty(); i++) {
            int index = random.nextInt(availableWords.size());
            selectedWords.add(availableWords.remove(index));
        }

        System.out.println("TELEPATHY: Selected " + selectedWords.size() + " random words for category: " + category);
        return selectedWords;
    }

    /**
     * Check if a word exists in the specified category
     * 
     * @param category The category to check
     * @param word     The word to check
     * @return True if the word exists in the category
     */
    public static boolean isWordInCategory(String category, String word) {

        List<String> wordList = categoryWords.get(category);

        if (wordList == null) {
            return false;
        }

        return wordList.contains(word.toLowerCase());
    }

    /**
     * Get all available categories
     * 
     * @return List of category names
     */
    public static List<String> getCategories() {
        return new ArrayList<>(categoryWords.keySet());
    }
}
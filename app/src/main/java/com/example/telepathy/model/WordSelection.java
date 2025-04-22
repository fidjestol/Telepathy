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
                "turtle", "crocodile", "frog", "spider", "ant", "bee", "butterfly",
                "gorilla", "chimpanzee", "baboon", "orangutan", "panda", "koala", "kangaroo",
                "wallaby", "platypus", "camel", "horse", "zebra", "rhino", "hippo",
                "bison", "buffalo", "cow", "pig", "goat", "sheep", "lamb", "donkey",
                "mule", "hedgehog", "porcupine", "skunk", "raccoon", "badger", "otter",
                "beaver", "mink", "weasel", "ferret", "armadillo", "sloth", "anteater",
                "opossum", "bat", "hyena", "jackal", "coyote", "leopard", "jaguar", "cheetah",
                "panther", "cougar", "lynx", "bobcat", "wombat", "tasmanian devil", "dingo",
                "moose", "elk", "caribou", "antelope", "gazelle", "impala", "wildebeest",
                "bison", "buffalo", "seal", "walrus", "sea lion", "manatee", "dugong",
                "narwhal", "beluga", "orca", "porpoise", "alligator", "chameleon", "iguana",
                "gecko", "toad", "salamander", "newt", "python", "cobra", "viper", "boa",
                "mamba", "anaconda", "rattlesnake", "turtle", "tortoise", "terrapin",
                "lobster", "crab", "shrimp", "crawfish", "scorpion", "centipede", "millipede",
                "wasp", "hornet", "ladybug", "beetle", "cockroach", "dragonfly", "moth",
                "grasshopper", "cricket", "locust", "praying mantis", "stick insect",
                "caterpillar", "firefly", "cicada", "earthworm", "leech", "slug", "snail",
                "starfish", "jellyfish", "octopus", "squid", "cuttlefish", "coral", "anemone",
                "clam", "oyster", "mussel", "scallop", "hummingbird", "sparrow", "robin",
                "cardinal", "bluejay", "woodpecker", "seagull", "pelican", "albatross",
                "flamingo", "crane", "stork", "heron", "swan", "goose", "duck", "turkey",
                "peacock", "quail", "pheasant", "parrot", "parakeet", "cockatoo", "toucan",
                "ostrich", "emu", "kiwi", "vulture", "condor", "falcon", "kestrel", "osprey",
                "loon", "puffin", "macaw", "finch", "canary", "crow", "raven", "magpie",
                "nightingale", "swallow", "swift", "kingfisher", "hamster", "gerbil", "mole",
                "shrew", "lemur", "meerkat", "mongoose", "gopher", "chipmunk", "prairie dog"));

        // Countries category
        categoryWords.put("Countries", Arrays.asList(
                "usa", "canada", "mexico", "brazil", "argentina", "chile", "peru",
                "france", "germany", "italy", "spain", "portugal", "england", "ireland",
                "russia", "china", "japan", "india", "australia", "egypt", "nigeria",
                "kenya", "south africa", "morocco", "greece", "turkey", "sweden", "norway",
                "colombia", "venezuela", "ecuador", "bolivia", "paraguay", "uruguay", "guyana",
                "suriname", "french guiana", "panama", "costa rica", "nicaragua", "honduras",
                "el salvador", "guatemala", "belize", "cuba", "jamaica", "haiti", "dominican republic",
                "puerto rico", "bahamas", "barbados", "trinidad and tobago", "grenada", "antigua",
                "st lucia", "poland", "ukraine", "belarus", "lithuania", "latvia", "estonia",
                "finland", "denmark", "iceland", "netherlands", "belgium", "luxembourg", "switzerland",
                "austria", "czechia", "slovakia", "hungary", "romania", "bulgaria", "serbia",
                "croatia", "slovenia", "bosnia", "montenegro", "albania", "north macedonia", "moldova",
                "malta", "cyprus", "monaco", "andorra", "liechtenstein", "san marino", "vatican city",
                "kazakhstan", "uzbekistan", "kyrgyzstan", "tajikistan", "turkmenistan", "azerbaijan",
                "armenia", "georgia", "mongolia", "north korea", "south korea", "taiwan", "philippines",
                "vietnam", "laos", "cambodia", "thailand", "myanmar", "malaysia", "singapore",
                "indonesia", "brunei", "papua new guinea", "new zealand", "fiji", "solomon islands",
                "vanuatu", "samoa", "tonga", "micronesia", "palau", "marshall islands", "kiribati",
                "tuvalu", "nauru", "bangladesh", "pakistan", "afghanistan", "nepal", "bhutan",
                "sri lanka", "maldives", "iran", "iraq", "syria", "lebanon", "israel", "jordan",
                "saudi arabia", "yemen", "oman", "uae", "qatar", "bahrain", "kuwait",
                "tunisia", "algeria", "libya", "sudan", "south sudan", "ethiopia", "eritrea",
                "djibouti", "somalia", "uganda", "rwanda", "burundi", "tanzania", "mozambique",
                "zimbabwe", "zambia", "malawi", "botswana", "namibia", "angola", "congo",
                "democratic republic of congo", "cameroon", "central african republic", "chad",
                "niger", "mali", "burkina faso", "ivory coast", "liberia", "sierra leone",
                "guinea", "guinea bissau", "gambia", "senegal", "mauritania", "cape verde",
                "equatorial guinea", "gabon", "madagascar", "mauritius", "seychelles", "comoros"));

        // Foods category
        categoryWords.put("Foods", Arrays.asList(
                "pizza", "burger", "pasta", "rice", "bread", "potato", "tomato", "onion",
                "carrot", "broccoli", "apple", "banana", "orange", "strawberry", "grape",
                "chicken", "beef", "pork", "fish", "egg", "milk", "cheese", "yogurt",
                "ice cream", "chocolate", "cake", "cookie", "pie", "soup", "salad",
                "sushi", "ramen", "curry", "taco", "burrito", "enchilada", "quesadilla", "guacamole",
                "salsa", "nachos", "fajita", "paella", "risotto", "lasagna", "spaghetti", "ravioli",
                "gnocchi", "carbonara", "pesto", "hummus", "falafel", "shawarma", "kebab", "couscous",
                "pita", "baklava", "halloumi", "tzatziki", "gyro", "moussaka", "tagine", "biryani",
                "naan", "tandoori", "samosa", "dal", "paneer", "chutney", "kimchi", "bibimbap",
                "bulgogi", "kimbap", "miso", "tempura", "teriyaki", "sashimi", "yakitori", "udon",
                "pho", "spring roll", "dim sum", "wonton", "dumpling", "fried rice", "chow mein",
                "pad thai", "satay", "laksa", "rendang", "borscht", "stroganoff", "pierogi", "goulash",
                "schnitzel", "pretzel", "sauerkraut", "bratwurst", "croissant", "baguette", "crepe",
                "quiche", "ratatouille", "escargot", "macaron", "eclair", "souffle", "bruschetta",
                "tiramisu", "gelato", "cannoli", "pancake", "waffle", "muffin", "bagel", "donut",
                "cinnamon roll", "scone", "custard", "pudding", "trifle", "crumble", "cobbler",
                "brownie", "cupcake", "tart", "truffle", "fudge", "brittle", "caramel", "honey",
                "jam", "jelly", "marmalade", "syrup", "ketchup", "mustard", "mayonnaise", "vinegar",
                "soy sauce", "hot sauce", "bbq sauce", "aioli", "pesto", "gravy", "avocado",
                "cucumber", "zucchini", "eggplant", "bell pepper", "mushroom", "spinach", "kale",
                "lettuce", "cabbage", "asparagus", "cauliflower", "celery", "corn", "peas", "beans",
                "lentils", "chickpeas", "quinoa", "oats", "barley", "millet", "couscous", "farro",
                "pear", "peach", "plum", "apricot", "cherry", "watermelon", "cantaloupe", "honeydew",
                "lemon", "lime", "grapefruit", "pineapple", "mango", "papaya", "kiwi", "passion fruit",
                "pomegranate", "fig", "date", "coconut", "blueberry", "raspberry", "blackberry",
                "cranberry", "gooseberry", "walnut", "almond", "cashew", "pistachio", "pecan",
                "hazelnut", "peanut", "chestnut", "macadamia", "lobster", "crab", "shrimp", "oyster",
                "mussel", "clam", "scallop", "squid", "octopus", "tuna", "salmon", "cod", "trout",
                "tilapia", "sea bass", "sardine", "anchovy", "caviar", "venison", "lamb", "duck",
                "turkey", "quail", "pheasant", "rabbit", "bison", "buffalo", "bacon", "ham", "sausage",
                "salami", "pepperoni", "prosciutto", "chorizo", "jerky", "butter", "cream", "sour cream",
                "cheddar", "mozzarella", "parmesan", "brie", "camembert", "gouda", "feta", "gorgonzola",
                "roquefort", "manchego", "gruyere", "cottage cheese", "ricotta", "mascarpone"));

        // Sports category
        categoryWords.put("Sports", Arrays.asList(
                "soccer", "football", "basketball", "baseball", "tennis", "golf", "hockey",
                "volleyball", "swimming", "running", "cycling", "skiing", "snowboarding",
                "surfing", "boxing", "wrestling", "karate", "judo", "gymnastics", "cricket",
                "rugby", "badminton", "table tennis", "bowling", "skating", "climbing",
                "archery", "fencing", "weightlifting", "powerlifting", "bodybuilding", "crossfit",
                "track and field", "marathon", "triathlon", "decathlon", "pentathlon", "rowing",
                "canoeing", "kayaking", "sailing", "windsurfing", "kitesurfing", "water polo",
                "diving", "synchronized swimming", "rafting", "skateboarding", "bmx", "motocross",
                "auto racing", "formula one", "nascar", "rallying", "karting", "drifting",
                "polo", "equestrian", "dressage", "show jumping", "rodeo", "bullfighting",
                "lacrosse", "handball", "squash", "racquetball", "pickleball", "padel", "curling",
                "ice skating", "figure skating", "speed skating", "bobsleigh", "luge", "skeleton",
                "biathlon", "snowshoeing", "ice climbing", "bouldering", "parkour", "orienteering",
                "hiking", "backpacking", "mountaineering", "canyoning", "spelunking", "scuba diving",
                "freediving", "snorkeling", "fishing", "fly fishing", "hunting", "shooting", "archery",
                "darts", "billiards", "pool", "snooker", "chess", "checkers", "backgammon", "poker",
                "bridge", "golf", "mini golf", "disc golf", "ultimate frisbee", "boomerang",
                "korfball", "netball", "aussie rules football", "gaelic football", "hurling",
                "camogie", "shinty", "bandy", "floorball", "field hockey", "roller hockey",
                "inline skating", "roller derby", "sumo", "taekwondo", "aikido", "kendo",
                "muay thai", "kickboxing", "mma", "brazilian jiu jitsu", "kung fu", "tai chi",
                "qigong", "yoga", "pilates", "zumba", "aerobics", "step aerobics", "calisthenics",
                "ballet", "modern dance", "ballroom dance", "breakdancing", "cheerleading",
                "rhythmic gymnastics", "trampoline", "tumbling", "acrobatics", "tug of war",
                "arm wrestling", "strongman", "highland games", "crossfit games", "dodgeball",
                "kickball", "four square", "tetherball", "capture the flag", "laser tag", "paintball",
                "airsoft", "esports", "competitive gaming", "drone racing", "dog sports", "agility",
                "flyball", "dock diving", "disc dog", "skijoring", "canicross", "falconry"));

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
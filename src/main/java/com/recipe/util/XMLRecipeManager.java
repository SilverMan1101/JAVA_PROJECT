package com.recipe.util;

import com.recipe.model.Recipe;
import com.recipe.model.Ingredient;
import com.recipe.model.Review;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class XMLRecipeManager {
    private static final String RECIPES_FILE = "recipes.xml";
    private static final String FILE_PATH;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    static {
        String userDir = System.getProperty("user.dir");
        FILE_PATH = userDir + File.separator + "data" + File.separator + RECIPES_FILE;
        initializeFile();
    }

    private static void initializeFile() {
        try {
            File file = new File(FILE_PATH);
            File parentDir = file.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }
            if (!file.exists()) {
                createInitialXMLFile(file);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void createInitialXMLFile(File file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();

        Element root = doc.createElement("recipes");
        doc.appendChild(root);

        saveDocument(doc, file);
    }

    public static List<Recipe> getAllRecipes() {
        List<Recipe> recipes = new ArrayList<>();
        try {
            File file = new File(FILE_PATH);
            if (!file.exists()) {
                return recipes;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);

            NodeList recipeNodes = doc.getElementsByTagName("recipe");
            for (int i = 0; i < recipeNodes.getLength(); i++) {
                Element recipeElement = (Element) recipeNodes.item(i);
                Recipe recipe = parseRecipe(recipeElement);
                recipes.add(recipe);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return recipes;
    }

    public static Recipe getRecipeById(String id) {
        List<Recipe> recipes = getAllRecipes();
        return recipes.stream()
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public static void saveRecipe(Recipe recipe) throws Exception {
        if (recipe == null) {
            throw new IllegalArgumentException("Recipe cannot be null");
        }
        if (recipe.getId() == null || recipe.getId().isEmpty()) {
            throw new IllegalArgumentException("Recipe ID cannot be null or empty");
        }
        
        File file = new File(FILE_PATH);
        File parentDir = file.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc;

        if (file.exists()) {
            try {
                doc = builder.parse(file);
            } catch (Exception e) {
                System.err.println("Error parsing existing XML file: " + e.getMessage());
                // Create new document if parsing fails
                doc = builder.newDocument();
                Element root = doc.createElement("recipes");
                doc.appendChild(root);
            }
        } else {
            doc = builder.newDocument();
            Element root = doc.createElement("recipes");
            doc.appendChild(root);
        }

        Element root = doc.getDocumentElement();
        if (root == null) {
            root = doc.createElement("recipes");
            doc.appendChild(root);
        }
        
        Element recipeElement = findOrCreateRecipeElement(doc, root, recipe.getId());
        updateRecipeElement(doc, recipeElement, recipe);
        saveDocument(doc, file);
        
        System.out.println("Recipe saved successfully: " + recipe.getId() + " to " + FILE_PATH);
    }

    private static Element findOrCreateRecipeElement(Document doc, Element root, String id) {
        NodeList recipes = root.getElementsByTagName("recipe");
        for (int i = 0; i < recipes.getLength(); i++) {
            Element recipe = (Element) recipes.item(i);
            if (recipe.getAttribute("id").equals(id)) {
                return recipe;
            }
        }

        Element newRecipe = doc.createElement("recipe");
        newRecipe.setAttribute("id", id);
        root.appendChild(newRecipe);
        return newRecipe;
    }

    private static void updateRecipeElement(Document doc, Element recipeElement, Recipe recipe) {
        clearElement(recipeElement);

        appendTextElement(doc, recipeElement, "title", recipe.getTitle());
        appendTextElement(doc, recipeElement, "description", recipe.getDescription());
        appendTextElement(doc, recipeElement, "cuisineType", recipe.getCuisineType());
        appendTextElement(doc, recipeElement, "difficultyLevel", recipe.getDifficultyLevel());
        appendTextElement(doc, recipeElement, "preparationTime", String.valueOf(recipe.getPreparationTime()));
        appendTextElement(doc, recipeElement, "cookingTime", String.valueOf(recipe.getCookingTime()));
        appendTextElement(doc, recipeElement, "servings", String.valueOf(recipe.getServings()));
        appendTextElement(doc, recipeElement, "category", recipe.getCategory());
        appendTextElement(doc, recipeElement, "photoPath", recipe.getPhotoPath() != null ? recipe.getPhotoPath() : "");
        appendTextElement(doc, recipeElement, "userId", recipe.getUserId());
        appendTextElement(doc, recipeElement, "authorName", recipe.getAuthorName());
        appendTextElement(doc, recipeElement, "averageRating", String.valueOf(recipe.getAverageRating()));
        appendTextElement(doc, recipeElement, "totalRatings", String.valueOf(recipe.getTotalRatings()));
        appendTextElement(doc, recipeElement, "approved", String.valueOf(recipe.isApproved()));
        appendTextElement(doc, recipeElement, "createdAt", recipe.getCreatedAt());

        // Ingredients
        Element ingredientsElement = doc.createElement("ingredients");
        for (Ingredient ingredient : recipe.getIngredients()) {
            Element ingElement = doc.createElement("ingredient");
            appendTextElement(doc, ingElement, "name", ingredient.getName());
            appendTextElement(doc, ingElement, "quantity", String.valueOf(ingredient.getQuantity()));
            appendTextElement(doc, ingElement, "unit", ingredient.getUnit());
            if (ingredient.getNotes() != null) {
                appendTextElement(doc, ingElement, "notes", ingredient.getNotes());
            }
            ingredientsElement.appendChild(ingElement);
        }
        recipeElement.appendChild(ingredientsElement);

        // Preparation Steps
        Element stepsElement = doc.createElement("preparationSteps");
        for (String step : recipe.getPreparationSteps()) {
            Element stepElement = doc.createElement("step");
            stepElement.setTextContent(step);
            stepsElement.appendChild(stepElement);
        }
        recipeElement.appendChild(stepsElement);

        // Tags
        Element tagsElement = doc.createElement("tags");
        for (String tag : recipe.getTags()) {
            Element tagElement = doc.createElement("tag");
            tagElement.setTextContent(tag);
            tagsElement.appendChild(tagElement);
        }
        recipeElement.appendChild(tagsElement);

        // Reviews
        Element reviewsElement = doc.createElement("reviews");
        for (Review review : recipe.getReviews()) {
            Element reviewElement = doc.createElement("review");
            reviewElement.setAttribute("id", review.getId());
            appendTextElement(doc, reviewElement, "userId", review.getUserId());
            appendTextElement(doc, reviewElement, "username", review.getUsername());
            appendTextElement(doc, reviewElement, "rating", String.valueOf(review.getRating()));
            appendTextElement(doc, reviewElement, "comment", review.getComment());
            appendTextElement(doc, reviewElement, "createdAt", review.getCreatedAt());
            reviewsElement.appendChild(reviewElement);
        }
        recipeElement.appendChild(reviewsElement);
    }

    private static Recipe parseRecipe(Element recipeElement) {
        Recipe recipe = new Recipe();
        recipe.setId(recipeElement.getAttribute("id"));
        recipe.setTitle(getTextContent(recipeElement, "title"));
        recipe.setDescription(getTextContent(recipeElement, "description"));
        recipe.setCuisineType(getTextContent(recipeElement, "cuisineType"));
        recipe.setDifficultyLevel(getTextContent(recipeElement, "difficultyLevel"));
        recipe.setPreparationTime(Integer.parseInt(getTextContent(recipeElement, "preparationTime", "0")));
        recipe.setCookingTime(Integer.parseInt(getTextContent(recipeElement, "cookingTime", "0")));
        recipe.setServings(Integer.parseInt(getTextContent(recipeElement, "servings", "1")));
        recipe.setCategory(getTextContent(recipeElement, "category"));
        recipe.setPhotoPath(getTextContent(recipeElement, "photoPath"));
        recipe.setUserId(getTextContent(recipeElement, "userId"));
        recipe.setAuthorName(getTextContent(recipeElement, "authorName"));
        recipe.setAverageRating(Double.parseDouble(getTextContent(recipeElement, "averageRating", "0.0")));
        recipe.setTotalRatings(Integer.parseInt(getTextContent(recipeElement, "totalRatings", "0")));
        recipe.setApproved(Boolean.parseBoolean(getTextContent(recipeElement, "approved", "false")));
        recipe.setCreatedAt(getTextContent(recipeElement, "createdAt"));

        // Parse ingredients
        NodeList ingredientNodes = recipeElement.getElementsByTagName("ingredient");
        for (int i = 0; i < ingredientNodes.getLength(); i++) {
            Element ingElement = (Element) ingredientNodes.item(i);
            Ingredient ingredient = new Ingredient();
            ingredient.setName(getTextContent(ingElement, "name"));
            ingredient.setQuantity(Double.parseDouble(getTextContent(ingElement, "quantity", "0")));
            ingredient.setUnit(getTextContent(ingElement, "unit"));
            ingredient.setNotes(getTextContent(ingElement, "notes"));
            recipe.getIngredients().add(ingredient);
        }

        // Parse preparation steps
        NodeList stepNodes = recipeElement.getElementsByTagName("step");
        for (int i = 0; i < stepNodes.getLength(); i++) {
            recipe.getPreparationSteps().add(stepNodes.item(i).getTextContent());
        }

        // Parse tags
        NodeList tagNodes = recipeElement.getElementsByTagName("tag");
        for (int i = 0; i < tagNodes.getLength(); i++) {
            recipe.getTags().add(tagNodes.item(i).getTextContent());
        }

        // Parse reviews
        NodeList reviewNodes = recipeElement.getElementsByTagName("review");
        for (int i = 0; i < reviewNodes.getLength(); i++) {
            Element reviewElement = (Element) reviewNodes.item(i);
            Review review = new Review();
            review.setId(reviewElement.getAttribute("id"));
            review.setRecipeId(recipe.getId());
            review.setUserId(getTextContent(reviewElement, "userId"));
            review.setUsername(getTextContent(reviewElement, "username"));
            review.setRating(Integer.parseInt(getTextContent(reviewElement, "rating", "0")));
            review.setComment(getTextContent(reviewElement, "comment"));
            review.setCreatedAt(getTextContent(reviewElement, "createdAt"));
            recipe.getReviews().add(review);
        }

        return recipe;
    }

    public static void deleteRecipe(String id) throws Exception {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            return;
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(file);

        Element root = doc.getDocumentElement();
        NodeList recipes = root.getElementsByTagName("recipe");

        for (int i = 0; i < recipes.getLength(); i++) {
            Element recipe = (Element) recipes.item(i);
            if (recipe.getAttribute("id").equals(id)) {
                root.removeChild(recipe);
                break;
            }
        }

        saveDocument(doc, file);
    }

    private static void appendTextElement(Document doc, Element parent, String tagName, String textContent) {
        Element element = doc.createElement(tagName);
        element.setTextContent(textContent != null ? textContent : "");
        parent.appendChild(element);
    }

    private static String getTextContent(Element parent, String tagName) {
        return getTextContent(parent, tagName, "");
    }

    private static String getTextContent(Element parent, String tagName, String defaultValue) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent();
        }
        return defaultValue;
    }

    private static void clearElement(Element element) {
        NodeList children = element.getChildNodes();
        for (int i = children.getLength() - 1; i >= 0; i--) {
            element.removeChild(children.item(i));
        }
    }

    private static void saveDocument(Document doc, File file) throws Exception {
        if (doc == null) {
            throw new IllegalArgumentException("Document cannot be null");
        }
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }
        
        // Ensure parent directory exists
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty("indent", "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        DOMSource source = new DOMSource(doc);
        
        try (FileOutputStream fos = new FileOutputStream(file)) {
            StreamResult result = new StreamResult(fos);
            transformer.transform(source, result);
        } catch (IOException e) {
            throw new Exception("Failed to write recipe to file: " + file.getAbsolutePath(), e);
        }
    }

    public static String generateId() {
        return "RECIPE_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }
}


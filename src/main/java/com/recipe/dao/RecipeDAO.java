package com.recipe.dao;

import com.recipe.model.Recipe;
import com.recipe.util.XMLRecipeManager;
import java.util.List;
import java.util.stream.Collectors;

public class RecipeDAO {
    
    public List<Recipe> getAllRecipes() {
        return XMLRecipeManager.getAllRecipes();
    }

    public List<Recipe> getApprovedRecipes() {
        return getAllRecipes().stream()
                .filter(Recipe::isApproved)
                .collect(Collectors.toList());
    }

    public List<Recipe> getRecipesForUser(String userId) {
        return getAllRecipes().stream()
                .filter(r -> {
                    // Show approved recipes to everyone
                    if (r.isApproved()) {
                        return true;
                    }
                    // Show user's own recipes even if not approved
                    if (userId != null && !userId.isEmpty() && 
                        r.getUserId() != null && !r.getUserId().isEmpty() && 
                        userId.equals(r.getUserId())) {
                        return true;
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }

    public Recipe getRecipeById(String id) {
        return XMLRecipeManager.getRecipeById(id);
    }

    public void saveRecipe(Recipe recipe) throws Exception {
        if (recipe.getId() == null || recipe.getId().isEmpty()) {
            recipe.setId(XMLRecipeManager.generateId());
        }
        if (recipe.getCreatedAt() == null || recipe.getCreatedAt().isEmpty()) {
            recipe.setCreatedAt(java.time.LocalDateTime.now().toString());
        }
        XMLRecipeManager.saveRecipe(recipe);
    }

    public void deleteRecipe(String id) throws Exception {
        XMLRecipeManager.deleteRecipe(id);
    }

    public List<Recipe> searchRecipes(String query) {
        String lowerQuery = query.toLowerCase();
        return getApprovedRecipes().stream()
                .filter(r -> r.getTitle().toLowerCase().contains(lowerQuery) ||
                            r.getDescription().toLowerCase().contains(lowerQuery) ||
                            r.getTags().stream().anyMatch(t -> t.toLowerCase().contains(lowerQuery)) ||
                            r.getCategory().toLowerCase().contains(lowerQuery))
                .collect(Collectors.toList());
    }

    public List<Recipe> searchRecipes(String query, String userId) {
        String lowerQuery = query.toLowerCase();
        return getRecipesForUser(userId).stream()
                .filter(r -> r.getTitle().toLowerCase().contains(lowerQuery) ||
                            r.getDescription().toLowerCase().contains(lowerQuery) ||
                            r.getTags().stream().anyMatch(t -> t.toLowerCase().contains(lowerQuery)) ||
                            r.getCategory().toLowerCase().contains(lowerQuery))
                .collect(Collectors.toList());
    }

    public List<Recipe> filterByCategory(String category) {
        return getApprovedRecipes().stream()
                .filter(r -> r.getCategory().equalsIgnoreCase(category))
                .collect(Collectors.toList());
    }

    public List<Recipe> filterByCategory(String category, String userId) {
        return getRecipesForUser(userId).stream()
                .filter(r -> r.getCategory().equalsIgnoreCase(category))
                .collect(Collectors.toList());
    }

    public List<Recipe> filterByCuisineType(String cuisineType) {
        return getApprovedRecipes().stream()
                .filter(r -> r.getCuisineType().equalsIgnoreCase(cuisineType))
                .collect(Collectors.toList());
    }

    public List<Recipe> filterByCuisineType(String cuisineType, String userId) {
        return getRecipesForUser(userId).stream()
                .filter(r -> r.getCuisineType().equalsIgnoreCase(cuisineType))
                .collect(Collectors.toList());
    }

    public List<Recipe> filterByDifficulty(String difficulty) {
        return getApprovedRecipes().stream()
                .filter(r -> r.getDifficultyLevel().equalsIgnoreCase(difficulty))
                .collect(Collectors.toList());
    }

    public List<Recipe> filterByDifficulty(String difficulty, String userId) {
        return getRecipesForUser(userId).stream()
                .filter(r -> r.getDifficultyLevel().equalsIgnoreCase(difficulty))
                .collect(Collectors.toList());
    }

    public List<Recipe> getRecipesByUser(String userId) {
        return getAllRecipes().stream()
                .filter(r -> r.getUserId().equals(userId))
                .collect(Collectors.toList());
    }
}


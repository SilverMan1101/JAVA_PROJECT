package com.recipe.servlet;

import com.recipe.dao.RecipeDAO;
import com.recipe.model.Recipe;
import com.recipe.model.Ingredient;
import com.recipe.model.User;
import com.recipe.model.Review;
import com.recipe.util.UserManager;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecipeServlet extends HttpServlet {
    private RecipeDAO recipeDAO = new RecipeDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        
        if (action == null) {
            action = "list";
        }

        switch (action) {
            case "view":
                viewRecipe(request, response);
                break;
            case "edit":
                editRecipe(request, response);
                break;
            case "form":
                showForm(request, response);
                break;
            case "delete":
                deleteRecipe(request, response);
                break;
            default:
                listRecipes(request, response);
                break;
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = null;
        List<FileItem> parsedItems = null;
        
        // For multipart requests, we need to parse the action parameter from the form data
        // Store parsed items in request attribute so saveRecipe can reuse them
        if (ServletFileUpload.isMultipartContent(request)) {
            try {
                DiskFileItemFactory factory = new DiskFileItemFactory();
                factory.setSizeThreshold(4096);
                factory.setRepository(new File(System.getProperty("java.io.tmpdir")));
                
                ServletFileUpload upload = new ServletFileUpload(factory);
                parsedItems = upload.parseRequest(request);
                
                // Store parsed items so saveRecipe can reuse them (can't parse request stream twice)
                request.setAttribute("parsedMultipartItems", parsedItems);
                
                for (FileItem item : parsedItems) {
                    if (item.isFormField() && "action".equals(item.getFieldName())) {
                        action = item.getString("UTF-8");
                        break;
                    }
                }
            } catch (FileUploadException e) {
                e.printStackTrace();
                System.err.println("Error parsing multipart request: " + e.getMessage());
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Error parsing form data");
                return;
            }
        } else {
            action = request.getParameter("action");
        }

        if ("save".equals(action)) {
            saveRecipe(request, response);
        } else if ("rate".equals(action)) {
            rateRecipe(request, response);
        } else if ("review".equals(action)) {
            addReview(request, response);
        } else {
            response.sendRedirect("recipes");
        }
    }

    private void listRecipes(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");
        
        String search = request.getParameter("search");
        String category = request.getParameter("category");
        String cuisineType = request.getParameter("cuisineType");
        String difficulty = request.getParameter("difficulty");

        List<Recipe> recipes;
        String userId = (user != null) ? user.getId() : null;

        if (user != null && user.isAdmin()) {
            recipes = recipeDAO.getAllRecipes();
        } else {
            recipes = recipeDAO.getRecipesForUser(userId);
        }

        if (search != null && !search.isEmpty()) {
            if (user != null && user.isAdmin()) {
                recipes = recipeDAO.searchRecipes(search);
            } else {
                recipes = recipeDAO.searchRecipes(search, userId);
            }
        } else if (category != null && !category.isEmpty()) {
            if (user != null && user.isAdmin()) {
                recipes = recipeDAO.filterByCategory(category);
            } else {
                recipes = recipeDAO.filterByCategory(category, userId);
            }
        } else if (cuisineType != null && !cuisineType.isEmpty()) {
            if (user != null && user.isAdmin()) {
                recipes = recipeDAO.filterByCuisineType(cuisineType);
            } else {
                recipes = recipeDAO.filterByCuisineType(cuisineType, userId);
            }
        } else if (difficulty != null && !difficulty.isEmpty()) {
            if (user != null && user.isAdmin()) {
                recipes = recipeDAO.filterByDifficulty(difficulty);
            } else {
                recipes = recipeDAO.filterByDifficulty(difficulty, userId);
            }
        }

        request.setAttribute("recipes", recipes);
        request.getRequestDispatcher("/jsp/recipe-list.jsp").forward(request, response);
    }

    private void viewRecipe(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String id = request.getParameter("id");
        Recipe recipe = recipeDAO.getRecipeById(id);

        if (recipe == null) {
            response.sendRedirect("recipes");
            return;
        }

        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");

        // Check if user has favorited/bookmarked this recipe
        if (user != null) {
            User currentUser = UserManager.getUserById(user.getId());
            request.setAttribute("isFavorite", currentUser.getFavoriteRecipeIds().contains(id));
            request.setAttribute("isBookmarked", currentUser.getBookmarkedRecipeIds().contains(id));
        }

        request.setAttribute("recipe", recipe);
        request.getRequestDispatcher("/jsp/recipe-detail.jsp").forward(request, response);
    }

    private void showForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");

        if (user == null) {
            response.sendRedirect("users?action=login");
            return;
        }

        String id = request.getParameter("id");
        if (id != null) {
            Recipe recipe = recipeDAO.getRecipeById(id);
            if (recipe != null && (user.isAdmin() || recipe.getUserId().equals(user.getId()))) {
                request.setAttribute("recipe", recipe);
            }
        }

        request.getRequestDispatcher("/jsp/recipe-form.jsp").forward(request, response);
    }

    private void editRecipe(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        showForm(request, response);
    }

    private void saveRecipe(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");

        if (user == null) {
            response.sendRedirect("users?action=login");
            return;
        }

        try {
            // Parse multipart request
            Map<String, String> formFields = new HashMap<>();
            Map<String, String[]> formArrays = new HashMap<>();
            String photoPath = null;
            
            // Check if request was already parsed in doPost (for multipart requests)
            @SuppressWarnings("unchecked")
            List<FileItem> preParsedItems = (List<FileItem>) request.getAttribute("parsedMultipartItems");
            List<FileItem> formItems = null;
            
            if (preParsedItems != null) {
                // Use pre-parsed items to avoid parsing request stream twice
                formItems = preParsedItems;
            } else if (ServletFileUpload.isMultipartContent(request)) {
                DiskFileItemFactory factory = new DiskFileItemFactory();
                factory.setSizeThreshold(4096);
                factory.setRepository(new File(System.getProperty("java.io.tmpdir")));
                
                ServletFileUpload upload = new ServletFileUpload(factory);
                upload.setFileSizeMax(5 * 1024 * 1024); // 5MB
                upload.setSizeMax(10 * 1024 * 1024); // 10MB
                
                try {
                    formItems = upload.parseRequest(request);
                } catch (FileUploadException e) {
                    throw new ServletException("Error parsing multipart request", e);
                }
            }
            
            // Process form items (either pre-parsed or newly parsed)
            if (formItems != null) {
                for (FileItem item : formItems) {
                    if (item.isFormField()) {
                        String fieldName = item.getFieldName();
                        String fieldValue = item.getString("UTF-8");
                        
                        // Handle array parameters
                        if (fieldName.equals("ingredientName") || 
                            fieldName.equals("ingredientQuantity") || 
                            fieldName.equals("ingredientUnit") ||
                            fieldName.equals("step")) {
                            if (!formArrays.containsKey(fieldName)) {
                                formArrays.put(fieldName, new String[]{fieldValue});
                            } else {
                                String[] existing = formArrays.get(fieldName);
                                String[] newArray = Arrays.copyOf(existing, existing.length + 1);
                                newArray[existing.length] = fieldValue;
                                formArrays.put(fieldName, newArray);
                            }
                        } else {
                            formFields.put(fieldName, fieldValue);
                        }
                    } else if (item.getFieldName().equals("photo") && item.getSize() > 0) {
                        // Handle file upload
                        String uploadDir = System.getProperty("user.dir") + File.separator + 
                                         "src" + File.separator + "main" + File.separator + 
                                         "webapp" + File.separator + "uploads";
                        File uploadPath = new File(uploadDir);
                        if (!uploadPath.exists()) {
                            uploadPath.mkdirs();
                        }
                        String fileName = System.currentTimeMillis() + "_" + 
                                         new File(item.getName()).getName();
                        File uploadedFile = new File(uploadPath, fileName);
                        item.write(uploadedFile);
                        photoPath = "uploads/" + fileName;
                    }
                }
            } else {
                // Non-multipart request (fallback)
                for (String paramName : request.getParameterMap().keySet()) {
                    String[] values = request.getParameterValues(paramName);
                    if (values.length == 1) {
                        formFields.put(paramName, values[0]);
                    } else {
                        formArrays.put(paramName, values);
                    }
                }
            }
            
            String id = formFields.get("id");
            Recipe recipe;

            if (id != null && !id.isEmpty()) {
                recipe = recipeDAO.getRecipeById(id);
                if (recipe == null || (!user.isAdmin() && !recipe.getUserId().equals(user.getId()))) {
                    response.sendRedirect("recipes");
                    return;
                }
            } else {
                recipe = new Recipe();
                recipe.setId(com.recipe.util.XMLRecipeManager.generateId());
                recipe.setCreatedAt(java.time.LocalDateTime.now().toString());
            }

            recipe.setTitle(formFields.get("title"));
            recipe.setDescription(formFields.get("description"));
            recipe.setCuisineType(formFields.get("cuisineType"));
            recipe.setDifficultyLevel(formFields.get("difficultyLevel"));
            recipe.setPreparationTime(Integer.parseInt(formFields.getOrDefault("preparationTime", "0")));
            recipe.setCookingTime(Integer.parseInt(formFields.getOrDefault("cookingTime", "0")));
            recipe.setServings(Integer.parseInt(formFields.getOrDefault("servings", "1")));
            recipe.setCategory(formFields.get("category"));
            recipe.setUserId(user.getId());
            recipe.setAuthorName(user.getFullName());
            
            if (photoPath != null) {
                recipe.setPhotoPath(photoPath);
            }

            // Parse ingredients
            String[] ingredientNames = formArrays.get("ingredientName");
            String[] quantities = formArrays.get("ingredientQuantity");
            String[] units = formArrays.get("ingredientUnit");

            List<Ingredient> ingredients = new ArrayList<>();
            if (ingredientNames != null) {
                for (int i = 0; i < ingredientNames.length; i++) {
                    if (ingredientNames[i] != null && !ingredientNames[i].isEmpty()) {
                        Ingredient ingredient = new Ingredient();
                        ingredient.setName(ingredientNames[i]);
                        ingredient.setQuantity(quantities != null && i < quantities.length && 
                                !quantities[i].isEmpty() ? Double.parseDouble(quantities[i]) : 0);
                        ingredient.setUnit(units != null && i < units.length ? units[i] : "");
                        ingredients.add(ingredient);
                    }
                }
            }
            recipe.setIngredients(ingredients);

            // Parse preparation steps
            String[] steps = formArrays.get("step");
            List<String> preparationSteps = new ArrayList<>();
            if (steps != null) {
                for (String step : steps) {
                    if (step != null && !step.trim().isEmpty()) {
                        preparationSteps.add(step);
                    }
                }
            }
            recipe.setPreparationSteps(preparationSteps);

            // Parse tags
            String tagsStr = formFields.get("tags");
            List<String> tags = new ArrayList<>();
            if (tagsStr != null && !tagsStr.isEmpty()) {
                tags = Arrays.asList(tagsStr.split(",\\s*"));
            }
            recipe.setTags(tags);

            // If admin is saving, they can approve directly
            if (user.isAdmin()) {
                recipe.setApproved(true);
            }

            // Validate required fields before saving
            if (recipe.getTitle() == null || recipe.getTitle().trim().isEmpty()) {
                throw new IllegalArgumentException("Recipe title is required");
            }
            if (recipe.getCategory() == null || recipe.getCategory().trim().isEmpty()) {
                throw new IllegalArgumentException("Recipe category is required");
            }
            
            System.out.println("Attempting to save recipe: " + recipe.getId() + ", Title: " + recipe.getTitle());
            recipeDAO.saveRecipe(recipe);
            System.out.println("Recipe saved successfully: " + recipe.getId());
            
            // Redirect to the recipe list so user can see their newly created recipe
            response.sendRedirect("recipes");

        } catch (NumberFormatException e) {
            e.printStackTrace();
            System.err.println("NumberFormatException while saving recipe: " + e.getMessage());
            request.setAttribute("error", "Invalid number format in recipe fields: " + e.getMessage());
            request.getRequestDispatcher("/jsp/recipe-form.jsp").forward(request, response);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            System.err.println("Validation error while saving recipe: " + e.getMessage());
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher("/jsp/recipe-form.jsp").forward(request, response);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Exception while saving recipe: " + e.getMessage());
            e.printStackTrace();
            request.setAttribute("error", "Error saving recipe: " + e.getMessage() + ". Please check server logs for details.");
            request.getRequestDispatcher("/jsp/recipe-form.jsp").forward(request, response);
        }
    }

    private void deleteRecipe(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");

        if (user == null) {
            response.sendRedirect("users?action=login");
            return;
        }

        String id = request.getParameter("id");
        Recipe recipe = recipeDAO.getRecipeById(id);

        if (recipe != null && (user.isAdmin() || recipe.getUserId().equals(user.getId()))) {
            try {
                recipeDAO.deleteRecipe(id);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (user.isAdmin()) {
            response.sendRedirect("admin?action=recipes");
        } else {
            response.sendRedirect("recipes");
        }
    }

    private void rateRecipe(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");

        if (user == null) {
            response.sendRedirect("users?action=login");
            return;
        }

        String recipeId = request.getParameter("recipeId");
        int rating = Integer.parseInt(request.getParameter("rating"));

        try {
            Recipe recipe = recipeDAO.getRecipeById(recipeId);
            if (recipe != null) {
                // Simple rating update - in production, you'd want to track individual user ratings
                int currentTotal = recipe.getTotalRatings();
                double currentAvg = recipe.getAverageRating();
                double newAvg = ((currentAvg * currentTotal) + rating) / (currentTotal + 1);
                
                recipe.setAverageRating(newAvg);
                recipe.setTotalRatings(currentTotal + 1);
                
                recipeDAO.saveRecipe(recipe);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        response.sendRedirect("recipes?action=view&id=" + recipeId);
    }

    private void addReview(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");

        if (user == null) {
            response.sendRedirect("users?action=login");
            return;
        }

        String recipeId = request.getParameter("recipeId");
        String comment = request.getParameter("comment");
        int rating = Integer.parseInt(request.getParameter("rating"));

        try {
            Recipe recipe = recipeDAO.getRecipeById(recipeId);
            if (recipe != null) {
                Review review = new Review();
                review.setId("REVIEW_" + System.currentTimeMillis());
                review.setRecipeId(recipeId);
                review.setUserId(user.getId());
                review.setUsername(user.getFullName());
                review.setRating(rating);
                review.setComment(comment);
                review.setCreatedAt(java.time.LocalDateTime.now().toString());

                recipe.getReviews().add(review);
                
                // Update average rating
                int total = recipe.getTotalRatings() + 1;
                double currentAvg = recipe.getAverageRating();
                double newAvg = ((currentAvg * recipe.getTotalRatings()) + rating) / total;
                recipe.setAverageRating(newAvg);
                recipe.setTotalRatings(total);

                recipeDAO.saveRecipe(recipe);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        response.sendRedirect("recipes?action=view&id=" + recipeId);
    }
}


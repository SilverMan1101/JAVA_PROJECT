<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="header.jsp" %>

<c:if test="${empty recipe}">
    <div class="container">
        <div class="error-message">Recipe not found.</div>
        <a href="${pageContext.request.contextPath}/recipes" class="btn btn-primary">Back to Recipes</a>
    </div>
</c:if>

<c:if test="${not empty recipe}">
<div class="container">
    <div class="recipe-detail">
        <div class="recipe-header">
            <h1>${recipe.title}</h1>
            <div class="recipe-meta-info">
                <span>By ${recipe.authorName}</span>
                <span>⭐ ${recipe.averageRating} (${recipe.totalRatings} ratings)</span>
                <span>⏱ ${recipe.totalTime} minutes</span>
                <span>👥 ${recipe.servings} servings</span>
            </div>
            
            <c:if test="${not empty user}">
                <div class="recipe-actions-top">
                    <form method="post" action="${pageContext.request.contextPath}/users" 
                          style="display: inline;">
                        <input type="hidden" name="action" value="favorite">
                        <input type="hidden" name="recipeId" value="${recipe.id}">
                        <input type="hidden" name="redirect" 
                               value="${pageContext.request.contextPath}/recipes?action=view&id=${recipe.id}">
                        <button type="submit" class="btn ${isFavorite ? 'btn-warning' : 'btn-outline'}">
                            ${isFavorite ? '❤️ Favorited' : '🤍 Favorite'}
                        </button>
                    </form>
                    <form method="post" action="${pageContext.request.contextPath}/users" 
                          style="display: inline;">
                        <input type="hidden" name="action" value="bookmark">
                        <input type="hidden" name="recipeId" value="${recipe.id}">
                        <input type="hidden" name="redirect" 
                               value="${pageContext.request.contextPath}/recipes?action=view&id=${recipe.id}">
                        <button type="submit" class="btn ${isBookmarked ? 'btn-warning' : 'btn-outline'}">
                            ${isBookmarked ? '🔖 Bookmarked' : '🔖 Bookmark'}
                        </button>
                    </form>
                    <a href="${pageContext.request.contextPath}/export?id=${recipe.id}" 
                       class="btn btn-outline">📥 Export</a>
                    <a href="javascript:window.print()" class="btn btn-outline">🖨️ Print</a>
                </div>
            </c:if>
        </div>

        <c:if test="${not empty recipe.photoPath}">
            <div class="recipe-photo">
                <img src="${pageContext.request.contextPath}/${recipe.photoPath}" alt="${recipe.title}">
            </div>
        </c:if>

        <div class="recipe-info-grid">
            <div class="recipe-info-card">
                <h3>Recipe Information</h3>
                <ul>
                    <li><strong>Category:</strong> ${recipe.category}</li>
                    <li><strong>Cuisine Type:</strong> ${recipe.cuisineType}</li>
                    <li><strong>Difficulty:</strong> ${recipe.difficultyLevel}</li>
                    <li><strong>Preparation Time:</strong> ${recipe.preparationTime} minutes</li>
                    <li><strong>Cooking Time:</strong> ${recipe.cookingTime} minutes</li>
                    <li><strong>Total Time:</strong> ${recipe.totalTime} minutes</li>
                    <li><strong>Servings:</strong> ${recipe.servings}</li>
                </ul>
            </div>

            <div class="recipe-info-card">
                <h3>Tags</h3>
                <div class="recipe-tags">
                    <c:forEach var="tag" items="${recipe.tags}">
                        <span class="tag">${tag}</span>
                    </c:forEach>
                </div>
            </div>
        </div>

        <div class="recipe-description-full">
            <h2>Description</h2>
            <p>${recipe.description}</p>
        </div>

        <div class="recipe-section">
            <h2>Ingredients</h2>
            <ul class="ingredient-list">
                <c:forEach var="ingredient" items="${recipe.ingredients}">
                    <li>
                        <strong>${ingredient.quantity} ${ingredient.unit}</strong> ${ingredient.name}
                        <c:if test="${not empty ingredient.notes}">
                            <em>(${ingredient.notes})</em>
                        </c:if>
                    </li>
                </c:forEach>
            </ul>
        </div>

        <div class="recipe-section">
            <h2>Preparation Steps</h2>
            <ol class="steps-list">
                <c:forEach var="step" items="${recipe.preparationSteps}">
                    <li>${step}</li>
                </c:forEach>
            </ol>
        </div>

        <c:if test="${not empty user}">
            <div class="recipe-section">
                <h2>Rate & Review</h2>
                

                <form method="post" action="${pageContext.request.contextPath}/recipes" class="review-form">
                    <input type="hidden" name="action" value="review">
                    <input type="hidden" name="recipeId" value="${recipe.id}">
                    <div class="form-group">
                        <label>Your Rating:</label>
                        <select name="rating" required>
                            <option value="5">5 ⭐</option>
                            <option value="4">4 ⭐</option>
                            <option value="3">3 ⭐</option>
                            <option value="2">2 ⭐</option>
                            <option value="1">1 ⭐</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label>Your Review:</label>
                        <textarea name="comment" rows="4" placeholder="Share your thoughts about this recipe..." required></textarea>
                    </div>
                    <button type="submit" class="btn btn-primary">Submit Review</button>
                </form>
            </div>
        </c:if>

        <div class="recipe-section">
            <h2>Reviews</h2>
            <c:if test="${empty recipe.reviews}">
                <p>No reviews yet. Be the first to review!</p>
            </c:if>
            <c:forEach var="review" items="${recipe.reviews}">
                <div class="review-item">
                    <div class="review-header">
                        <strong>${review.username}</strong>
                        <span class="review-rating">${review.rating} ⭐</span>
                        <span class="review-date">${review.createdAt}</span>
                    </div>
                    <p>${review.comment}</p>
                </div>
            </c:forEach>
        </div>
    </div>
</div>
</c:if>

<%@ include file="footer.jsp" %>


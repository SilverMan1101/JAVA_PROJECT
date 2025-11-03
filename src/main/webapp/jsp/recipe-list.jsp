<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="header.jsp" %>

<div class="container">
    <h1 class="page-title">Recipe Catalog</h1>
    
    <!-- Search and Filter Section -->
    <div class="search-filters">
        <form method="get" action="${pageContext.request.contextPath}/recipes" class="search-form">
            <input type="text" name="search" placeholder="Search recipes..." 
                   value="${param.search}" class="search-input">
            <button type="submit" class="btn btn-primary">Search</button>
            <a href="${pageContext.request.contextPath}/recipes" class="btn btn-secondary">Clear</a>
        </form>
        
        <div class="filter-buttons">
            <h3>Filters:</h3>
            <div class="filter-group">
                <strong>Category:</strong>
                <a href="?category=Vegetarian" class="filter-tag">Vegetarian</a>
                <a href="?category=Vegan" class="filter-tag">Vegan</a>
                <a href="?category=Dessert" class="filter-tag">Dessert</a>
                <a href="?category=Beverage" class="filter-tag">Beverage</a>
                <a href="?category=Main Course" class="filter-tag">Main Course</a>
            </div>
            <div class="filter-group">
                <strong>Difficulty:</strong>
                <a href="?difficulty=Easy" class="filter-tag">Easy</a>
                <a href="?difficulty=Medium" class="filter-tag">Medium</a>
                <a href="?difficulty=Hard" class="filter-tag">Hard</a>
            </div>
        </div>
    </div>

    <!-- Recipe Grid -->
    <c:choose>
        <c:when test="${empty recipes}">
            <div class="empty-state">
                <p>No recipes found. <a href="${pageContext.request.contextPath}/recipes?action=form">Create one now!</a></p>
            </div>
        </c:when>
        <c:otherwise>
            <div class="recipe-grid">
                <c:forEach var="recipe" items="${recipes}">
            <div class="recipe-card">
                <c:if test="${not empty recipe.photoPath}">
                    <img src="${pageContext.request.contextPath}/${recipe.photoPath}" 
                         alt="${recipe.title}" class="recipe-card-image">
                </c:if>
                <div class="recipe-card-content">
                    <h3><a href="${pageContext.request.contextPath}/recipes?action=view&id=${recipe.id}">${recipe.title}</a></h3>
                    <p class="recipe-meta">
                        <span>⏱ ${recipe.totalTime} min</span>
                        <span>👥 ${recipe.servings} servings</span>
                        <span>⭐ ${recipe.averageRating}</span>
                    </p>
                    <p class="recipe-description">${recipe.description}</p>
                    <div class="recipe-tags">
                        <span class="tag">${recipe.category}</span>
                        <span class="tag">${recipe.difficultyLevel}</span>
                        <c:forEach var="tag" items="${recipe.tags}" begin="0" end="2">
                            <span class="tag">${tag}</span>
                        </c:forEach>
                    </div>
                    <div class="recipe-actions">
                        <a href="${pageContext.request.contextPath}/recipes?action=view&id=${recipe.id}" 
                           class="btn btn-primary btn-sm">View Recipe</a>
                        <c:if test="${not empty user and (user.role == 'ADMIN' or recipe.userId == user.id)}">
                            <a href="${pageContext.request.contextPath}/recipes?action=edit&id=${recipe.id}" 
                               class="btn btn-secondary btn-sm">Edit</a>
                        </c:if>
                    </div>
                </div>
            </div>
                </c:forEach>
            </div>
        </c:otherwise>
    </c:choose>
</div>

<%@ include file="footer.jsp" %>


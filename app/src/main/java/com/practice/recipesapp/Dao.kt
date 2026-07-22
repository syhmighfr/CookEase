package com.practice.recipesapp

import androidx.room.Dao
import androidx.room.Query

@Dao
interface Dao {

    @Query("SELECT * FROM recipe")
    fun getAll(): List<Recipe?>

    // ✅ New method: find recipes that contain a keyword in ingredients
    @Query("SELECT * FROM recipe WHERE ing LIKE '%' || :ingredient || '%'")
    suspend fun findRecipesByIngredient(ingredient: String): List<Recipe>
}
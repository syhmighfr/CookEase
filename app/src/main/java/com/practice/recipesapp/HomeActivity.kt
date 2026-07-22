package com.practice.recipesapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.practice.recipesapp.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var rvAdapter: PopularAdapter
    private lateinit var dataList: ArrayList<Recipe>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()

        binding.search.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }

        binding.salad.setOnClickListener {
            startCategoryActivity("Salad")
        }

        binding.mainDish.setOnClickListener {
            startCategoryActivity("Dish")
        }

        binding.drinks.setOnClickListener {
            startCategoryActivity("Drinks")
        }

        binding.desserts.setOnClickListener {
            startCategoryActivity("Desserts")
        }

        binding.btnScanIngredients.setOnClickListener {
            startActivity(Intent(this, ScanIngredientsActivity::class.java))
        }

        binding.btnMenu.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menuInflater.inflate(R.menu.menu_home, popup.menu)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_settings -> {
                        startActivity(Intent(this, SettingsActivity::class.java))
                        true
                    }
                    R.id.menu_profile -> {
                        Toast.makeText(this, "Profile clicked", Toast.LENGTH_SHORT).show()
                        true
                    }
                    R.id.menu_logout -> {
                        FirebaseAuth.getInstance().signOut()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    private fun startCategoryActivity(category: String) {
        val intent = Intent(this, CategoryActivity::class.java)
        intent.putExtra("TITTLE", category)
        intent.putExtra("CATEGORY", category)
        startActivity(intent)
    }

    private fun setupRecyclerView() {
        dataList = ArrayList()
        binding.rvPopular.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        val db = Room.databaseBuilder(this, AppDatabase::class.java, "db_name")
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .createFromAsset("recipe.db")
            .build()

        // Assuming your AppDatabase has a method getDao() that returns your Dao interface
        // (e.g., RecipeDao)
        val daoObject = db.getDao()

        // --- MODIFIED SECTION START ---
        // First, get the result from the DAO. It can be a null list, or a list containing null recipes.
        val nullableRecipeList: List<Recipe?>? = daoObject.getAll()

        // Now, process it to get a non-null list of non-null Recipe objects.
        // 1. If nullableRecipeList is null, `recipes` becomes an empty list.
        // 2. If nullableRecipeList is not null, `filterNotNull()` removes any null Recipe objects from it.
        val recipes: List<Recipe> = nullableRecipeList?.filterNotNull() ?: emptyList()
        // --- MODIFIED SECTION END ---

        for (recipe in recipes) { // 'recipes' is now List<Recipe>, so 'recipe' is Recipe (non-null)
            // You still need to handle nullability for properties OF the Recipe object itself
            if (recipe.category?.contains("Popular") == true) {
                dataList.add(recipe)
            }
        }

        rvAdapter = PopularAdapter(dataList, this) // dataList will be ArrayList<Recipe>
        binding.rvPopular.adapter = rvAdapter
    }
}

package com.practice.recipesapp

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions
import com.google.mlkit.common.model.LocalModel
import kotlinx.coroutines.launch
import java.io.File

class ScanIngredientsActivity : AppCompatActivity() {

    private lateinit var btnTakePhoto: Button
    private lateinit var btnSubmitIngredients: Button
    private lateinit var btnGenerateRecipes: Button
    private lateinit var chipGroup: ChipGroup
    private lateinit var backBtn: ImageView
    private lateinit var etInputIngredient: EditText

    private var ingredientList = mutableSetOf<String>()
    private val requestImageCapture = 1001
    private val requestCameraPermission = 2001

    private lateinit var photoUri: Uri
    private lateinit var photoFile: File
    private var lastDetected = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_ingredients)

        btnTakePhoto = findViewById(R.id.btnTakePhoto)
        btnSubmitIngredients = findViewById(R.id.btnSubmitIngredients)
        btnGenerateRecipes = findViewById(R.id.btnGenerateRecipes)
        chipGroup = findViewById(R.id.chipGroup)
        backBtn = findViewById(R.id.back_btn)
        etInputIngredient = findViewById(R.id.etInputIngredient)

        backBtn.setOnClickListener { finish() }

        btnTakePhoto.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    requestCameraPermission
                )
            } else {
                launchCamera()
            }
        }

        btnSubmitIngredients.setOnClickListener {
            val text = etInputIngredient.text.toString().trim()
            if (text.isNotBlank() && ingredientList.add(text)) {
                addChip(text)
                etInputIngredient.text.clear()
            }
            if (lastDetected.isNotBlank() && ingredientList.add(lastDetected)) {
                addChip(lastDetected)
                Toast.makeText(this, "Added: $lastDetected", Toast.LENGTH_SHORT).show()
            }
        }

        btnGenerateRecipes.setOnClickListener {
            if (ingredientList.isEmpty()) {
                Toast.makeText(this, "No ingredients to generate recipes", Toast.LENGTH_SHORT).show()
            } else {
                val joinedIngredients = ingredientList.joinToString(", ")
                searchRecipes(joinedIngredients)
            }
        }
    }

    private fun launchCamera() {
        try {
            photoFile = File(getExternalFilesDir(null), "ingredient_photo.jpg")
            photoUri = FileProvider.getUriForFile(this, "${packageName}.provider", photoFile)

            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)

            if (intent.resolveActivity(packageManager) != null) {
                startActivityForResult(intent, requestImageCapture)
            } else {
                Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Camera error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestImageCapture && resultCode == Activity.RESULT_OK) {
            detectIngredient(photoUri)
        }
    }

    private fun detectIngredient(imageUri: Uri) {
        try {
            val image = InputImage.fromFilePath(this, imageUri)

            val localModel = LocalModel.Builder()
                .setAssetFilePath("model.tflite")
                .build()

            val options = CustomImageLabelerOptions.Builder(localModel)
                .setConfidenceThreshold(0.6f)
                .setMaxResultCount(5)
                .build()

            val labeler = ImageLabeling.getClient(options)

            labeler.process(image)
                .addOnSuccessListener { labels ->
                    if (labels.isNotEmpty()) {
                        for (label in labels) {
                            Log.d("ML_RESULT", "Label: ${label.text}, Confidence: ${label.confidence}")
                        }

                        val topLabel = labels[0]
                        if (topLabel.confidence >= 0.6f) {
                            lastDetected = topLabel.text.lowercase()
                            Toast.makeText(this, "Detected: $lastDetected", Toast.LENGTH_SHORT).show()
                        } else {
                            lastDetected = ""
                            Toast.makeText(this, "Low confidence detected", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        lastDetected = ""
                        Toast.makeText(this, "No ingredient detected", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    lastDetected = ""
                    Toast.makeText(this, "Detection error: ${it.message}", Toast.LENGTH_SHORT).show()
                    Log.e("MLKit", "Detection error", it)
                }

        } catch (e: Exception) {
            Toast.makeText(this, "Image error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun addChip(label: String) {
        val chip = Chip(this)
        chip.text = label
        chip.isCloseIconVisible = true
        chip.setOnCloseIconClickListener {
            chipGroup.removeView(chip)
            ingredientList.remove(label)
        }
        chipGroup.addView(chip)
    }

    private fun searchRecipes(ingredientText: String) {
        val keywords = ingredientText.split(",").map { it.trim().lowercase() }

        lifecycleScope.launch {
            val dao = AppDatabase.getDatabase(this@ScanIngredientsActivity).getDao()
            val allRecipes = dao.getAll().orEmpty().filterNotNull()

            val matchedRecipes = allRecipes.filter { recipe ->
                keywords.all { keyword ->
                    recipe.ing?.lowercase()?.contains(keyword) == true
                }
            }

            runOnUiThread {
                if (matchedRecipes.isNotEmpty()) {
                    val titles = matchedRecipes.joinToString("\n") { it.tittle }
                    Toast.makeText(this@ScanIngredientsActivity, "Recipes:\n$titles", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@ScanIngredientsActivity, "No matching recipes found", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

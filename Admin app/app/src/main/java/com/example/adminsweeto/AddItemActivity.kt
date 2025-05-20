package com.example.adminsweeto

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.text.isDigitsOnly
import com.example.adminsweeto.databinding.ActivityAddItemBinding
import com.example.adminsweeto.model.AllMenu
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException

class AddItemActivity : AppCompatActivity() {

    //Food item Details
    private lateinit var foodName: String
    private lateinit var foodPrice: String
    private lateinit var foodDescription: String
    private lateinit var foodIngredients: String
    private var foodImageUri: Uri? = null

    //firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private val binding: ActivityAddItemBinding by lazy {
        ActivityAddItemBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        //Initialize firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        binding.addItemButton.setOnClickListener {
            //Get data from fields
            foodName = binding.foodName.text.toString().trim()
            foodPrice = binding.foodPrice.text.toString().trim()
            foodDescription = binding.description.text.toString().trim()
            foodIngredients = binding.ingredients.text.toString().trim()

            if (!(foodName.isBlank() || foodPrice.isBlank() || foodDescription.isBlank() || foodIngredients.isBlank())) {
                uploadData()
                Toast.makeText(this, "Item Added SuccessFully", Toast.LENGTH_SHORT).show()
                finish()

            } else {
                Toast.makeText(this, "Fill ALl The Details", Toast.LENGTH_SHORT).show()
            }
        }
        binding.selectImage.setOnClickListener {
            pickImage.launch("image/*")
        }


        binding.backButton.setOnClickListener {
            finish()
        }

    }

    private fun uploadData() {
        if (foodImageUri == null) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show()
            return
        }

        val cloudName = "dlf78peeq" // ← thay bằng Cloud name của bạn
        val uploadPreset = "foodorderingapp" // ← thay bằng Upload Preset đã tạo trong Cloudinary

        val inputStream = contentResolver.openInputStream(foodImageUri!!)
        val imageBytes = inputStream?.readBytes()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file", "image.jpg",
                RequestBody.create("image/*".toMediaTypeOrNull(), imageBytes!!)
            )
            .addFormDataPart("upload_preset", uploadPreset)
            .build()

        val request = Request.Builder()
            .url("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
            .post(requestBody)
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@AddItemActivity, "Image Upload Failed", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                val json = JSONObject(responseData!!)
                val imageUrl = json.getString("secure_url")

                saveDataToFirebase(imageUrl)
            }
        })
    }
    private fun saveDataToFirebase(imageUrl: String) {
        val menuRef = database.getReference("menu")
        val newItemKey = menuRef.push().key

        val newItem = AllMenu(
            newItemKey,
            foodName = foodName,
            foodPrice = foodPrice,
            foodDescription = foodDescription,
            foodIngredient = foodIngredients,
            foodImage = imageUrl
        )

        newItemKey?.let { key ->
            menuRef.child(key).setValue(newItem)
                .addOnSuccessListener {
                    runOnUiThread {
                        Toast.makeText(this, "Item uploaded successfully!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
                .addOnFailureListener {
                    runOnUiThread {
                        Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            binding.selectedImage.setImageURI(uri)
            foodImageUri = uri
        }
    }
}
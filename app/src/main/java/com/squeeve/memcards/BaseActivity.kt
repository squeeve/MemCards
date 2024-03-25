package com.squeeve.memcards

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squeeve.memcards.Game.Companion.LEVELS
import kotlin.random.Random

// These are just for uploading and using profile pictures
import androidx.activity.result.ActivityResultLauncher
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask

abstract class BaseActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private val tag = "BaseActivity"

    private lateinit var auth: FirebaseAuth
    private lateinit var dbReference: DatabaseReference
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var profileImageView: ImageView
    private lateinit var actionBar: Toolbar
    private lateinit var drawerToggler: ActionBarDrawerToggle
    protected lateinit var authMan: AuthManager
    protected val currentUser by lazy { auth.currentUser }

    // for profile pictures...
    private var filePath: Uri? = null
    private lateinit var storage: FirebaseStorage
    private lateinit var storageReference: StorageReference

    @Suppress("unused")
    private val content: ActivityResultLauncher<String> = registerForActivityResult(
        ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            filePath = uri
            try {
                profileImageView.setImageBitmap(
                    BitmapFactory.decodeStream(contentResolver.openInputStream(filePath!!))
                )
                uploadImage()
            } catch (e: Exception) {
                Log.e(tag, "Error: $e")
            }
        }
    }

    private fun chooseImage() {
        content.launch("image/*") // This is a filetype filter
    }

    private fun uploadImage() {
        // Upload the image to Firebase Storage and save the reference to the user's profile
        filePath?.let {path ->
            storageReference.child("images/" + currentUser!!.uid).putFile(path)
                .continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                return@Continuation task.result?.storage?.downloadUrl
            }).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val downloadUri = task.result
                    Log.d(tag, "Profile picture uploaded: $downloadUri")
                    val you = User(this, currentUser!!.uid)
                    you.profilePicture = downloadUri.toString()
                    you.writeUserPrefs(true)
                    dbReference.child("Users").child(currentUser!!.uid).child("profilePicture").setValue(downloadUri.toString())
                }
            }
        }
    }

    abstract fun getLayoutId(): Int

    fun gotoGame(level: Int = 0, randomize: Boolean = false) {
        val startGame: Intent
        if (randomize) {
            val randLevel = LEVELS.entries.elementAt(Random.nextInt(LEVELS.size))
            Toast.makeText(
                this,
                "Starting game at ${randLevel.key}",
                Toast.LENGTH_SHORT).show()
            startGame = Intent(this, GameActivity::class.java)
            startGame.putExtra("level", randLevel.value)
        } else if (level in LEVELS.values) {
            startGame = Intent(this, GameActivity::class.java)
            startGame.putExtra("level", level)
        } else {
            startGame = Intent(this, MainActivity2::class.java)
        }
        startActivity(startGame)
    }

    private fun prefFileCheck(uid: String, username: String, email: String) {
        // Create the app internal file if it doesn't exist.
        val userJson = mapOf(
            "username" to username,
            "email" to email
        )
        Log.d("PrefFile", "Creating app's user pref file. Check ${this.filesDir}")
        val fh = FileHelper(this)
        fh.saveToFile(userJson, getString(R.string.app_domain)+".$uid", overwrite = false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getLayoutId())

        auth = FirebaseAuth.getInstance()
        dbReference = FirebaseDatabase.getInstance().reference
        authMan = AuthManager(this)
        if (currentUser == null) {
            authMan.startLoginActivity()
            finish()
        }

        // for profile pictures...
        storage = FirebaseStorage.getInstance()
        storageReference = storage.reference
        val you = User(this, currentUser!!.uid)

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.drawer_view)
        navigationView.setNavigationItemSelectedListener(this)
        actionBar = findViewById(R.id.topToolbar)
        setSupportActionBar(actionBar)

        drawerToggler = ActionBarDrawerToggle(this, drawerLayout, actionBar, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(drawerToggler)
        drawerToggler.syncState()

        // change username in header
        Log.d(tag, "NavigationView's header count: ${navigationView.headerCount}")
        val headerView = navigationView.getHeaderView(0)
        val nameTextView = headerView.findViewById<TextView>(R.id.profile_name)
        profileImageView = headerView.findViewById(R.id.profile_image)
        if (you.profilePicture.isNotBlank()) {
            val profileRef = storageReference.child("images/${you.uid}")
            profileRef.downloadUrl.addOnSuccessListener { url ->
                Glide.with(this).load(url.toString()).circleCrop().into(profileImageView)
            }.addOnFailureListener {
                Glide.with(this).load(R.drawable.poker_face).circleCrop().into(profileImageView)
            }
        } else {
            Glide.with(this).load(R.drawable.poker_face).circleCrop().into(profileImageView)
        }
        profileImageView.setOnClickListener {
            chooseImage()
        }
        currentUser!!.let { user ->
            val userId = user.uid
            val userRef = dbReference.child("Users").child(userId)
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnap: DataSnapshot) {
                    if (dataSnap.exists()) {
                        val username = dataSnap.child("username").getValue(String::class.java)
                        val email = dataSnap.child("email").getValue(String::class.java)
                        Log.d(tag, "Username acquired from current user: $username")
                        nameTextView.text = username
                        prefFileCheck(user.uid, username!!, email!!)
                    } else {
                        Log.e(tag, "User ID $userId doesn't match any db entries.")
                        authMan.startLoginActivity()
                        finish()
                    }
                }
                override fun onCancelled(dbError: DatabaseError) {
                    Log.e(tag, "userRef:: Database error: $dbError")
                    Toast.makeText(this@BaseActivity,
                        "Couldn't access your profile. Please register.",
                        Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_home -> {
                Log.d(tag, "Main menu clicked")
                gotoGame()
                finish()
            }
            R.id.menu_game -> {
                Log.d(tag, "Random Game clicked")
                gotoGame(randomize = true)
                finish()
            }
            R.id.menu_profile -> {
                Log.d(tag, "Profile clicked")
                startActivity(Intent(this, ProfileActivity::class.java))
                finish()
            }
            R.id.menu_leaderboard -> {
                Log.d(tag, "Leaderboard clicked")
                startActivity(Intent(this, LeaderboardActivity2::class.java))
                finish()
            }
            R.id.menu_signout -> {
                Log.d(tag, "Logout clicked")
                authMan.logout()
                authMan.startLoginActivity()
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

}
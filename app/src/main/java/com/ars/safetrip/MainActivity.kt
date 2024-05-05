package com.ars.safetrip

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.room.Room
import com.ars.safetrip.database.AppDataBase
import com.ars.safetrip.database.SafeTripUser
import com.ars.safetrip.databinding.ActivityMainBinding
import org.mindrot.jbcrypt.BCrypt

class MainActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityMainBinding
    private lateinit var db: AppDataBase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        initDatabase()

        viewBinding.btLogin.setOnClickListener {
            val username = viewBinding.etUsername.text.toString()
            val password = viewBinding.etPassword.text.toString()

            if (verifyLogin(username, password)) {
                val intent = Intent(this, VideoActivity::class.java)
                startActivity(intent)
            }
        }

        viewBinding.tvNewUser.setOnClickListener {
            val intent = Intent(this, UserActivity::class.java)
            startActivity(intent)
        }
    }

    private fun checkPassword(password: String, hashedPassword: String): Boolean {
        return BCrypt.checkpw(password, hashedPassword)
    }

    private fun initDatabase() {
        db = Room.databaseBuilder(
            applicationContext,
            AppDataBase::class.java, "database-user"
        )
            .allowMainThreadQueries()
            .build()
    }

    private fun verifyLogin(username: String, password: String): Boolean {
        val user = db.safeTripUser().getUserByUsername(username)
        if (user is SafeTripUser) {
            if (checkPassword(password, user.password)) {
                Toast.makeText(this, getString(R.string.login_sucesso), Toast.LENGTH_SHORT).show()
                return true
            }
            Toast.makeText(this, getString(R.string.senha_incorreta), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, getString(R.string.usuario_incorreto), Toast.LENGTH_SHORT).show()
        }
        return false
    }
}
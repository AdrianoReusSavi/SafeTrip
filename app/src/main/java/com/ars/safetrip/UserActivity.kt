package com.ars.safetrip

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.room.Room
import com.ars.safetrip.database.AppDataBase
import com.ars.safetrip.database.SafeTripUser
import com.ars.safetrip.databinding.ActivityUserBinding
import org.mindrot.jbcrypt.BCrypt

class UserActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityUserBinding
    private lateinit var db: AppDataBase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityUserBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        initDatabase()

        viewBinding.btSalvar.setOnClickListener {
            val username = viewBinding.etUsername.text.toString()
            val password = viewBinding.etPassword.text.toString()
            val confirmPassword = viewBinding.etConfirmPassword.text.toString()

            if (password != confirmPassword) {
                Toast.makeText(this, getString(R.string.senha_diferente), Toast.LENGTH_SHORT).show()
            } else if (existingUser(username)) {
                Toast.makeText(this, getString(R.string.nome_existente), Toast.LENGTH_SHORT).show()
            } else{
                saveUser(username, password)
                Toast.makeText(this, getString(R.string.usuario_salvo), Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        viewBinding.tvExistingUser.setOnClickListener { finish() }
    }

    private fun existingUser(user: String): Boolean {
        val qtd = db.safeTripUser().getUserCountByUsername(user)
        return qtd > 0
    }

    private fun hashPassword(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt())
    }

    private fun initDatabase() {
        db = Room.databaseBuilder(
            applicationContext,
            AppDataBase::class.java, "database-user"
        )
            .allowMainThreadQueries()
            .build()
    }

    private fun saveUser(username: String, password: String) {
        val hashedPassword = hashPassword(password)
        val user = SafeTripUser(username = username, password = hashedPassword)
        db.safeTripUser().insertAll(user)
    }
}
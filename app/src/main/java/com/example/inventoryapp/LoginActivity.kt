package com.example.inventoryapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.fragment_scanner_result_dialog_list_dialog.*
import org.json.JSONObject
import java.io.BufferedReader

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        btnLogin.setOnClickListener {
            val fileReader: BufferedReader = application.assets.open("url.txt")?.bufferedReader()
            val url = fileReader.readLine()
            val urlPath = "$url/login"

            val user = JSONObject()
            user.put("Email", etEmail.text.toString())
            user.put("Password", etPassword.text.toString())

            val que = Volley.newRequestQueue(this)
            val req = JsonObjectRequest(
                    Request.Method.POST, urlPath, user,
                    { response ->
                        if (response["responseServer"].toString().equals("Yes")) {
                            startActivity(Intent(this, MainActivity::class.java))
                        } else {
                            Toast.makeText(this, "Your account or password is incorrect!", Toast.LENGTH_LONG).show()
                        }
                        println("Response from server -> " + response["responseServer"])
                    }, {
                println("Error from server")
            }
            )
            que.add(req)
        }
    }
}
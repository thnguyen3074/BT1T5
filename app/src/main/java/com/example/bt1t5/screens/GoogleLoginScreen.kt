package com.example.bt1t5.screens

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

@Composable
fun GoogleLoginScreen(
    onLoginSuccess: ((String, String, String?) -> Unit)? = null
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    var loginResult by remember { mutableStateOf<LoginState>(LoginState.Initial) }
    var isLoading by remember { mutableStateOf(false) }

    val googleSignInClient = remember { Identity.getSignInClient(context) }
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        isLoading = true
        try {
            val credential = googleSignInClient.getSignInCredentialFromIntent(result.data)
            val googleToken = credential.googleIdToken

            googleToken?.let {
                val firebaseCredential = GoogleAuthProvider.getCredential(it, null)
                auth.signInWithCredential(firebaseCredential)
                    .addOnCompleteListener { task ->
                        isLoading = false
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            if (user != null) {
                                val loginState = LoginState.Success(
                                    userId = user.uid,
                                    email = user.email ?: "No email",
                                    displayName = user.displayName,
                                    photoUrl = user.photoUrl?.toString()
                                )
                                loginResult = loginState

                                // Call success callback if provided
                                onLoginSuccess?.invoke(
                                    user.uid,
                                    user.email ?: "No email",
                                    user.displayName
                                )
                            } else {
                                loginResult = LoginState.Error("Authentication failed. User is null.")
                            }
                        } else {
                            // Log the full exception for debugging
                            task.exception?.let { exception ->
                                Log.e("GoogleLoginScreen", "Sign-in error", exception)
                                loginResult = when (exception) {
                                    is ApiException -> LoginState.Error("Google Sign-In Failed: ${exception.statusCode}")
                                    else -> LoginState.Error("Authentication failed: ${exception.message}")
                                }
                            } ?: run {
                                loginResult = LoginState.Error("Unknown authentication error")
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        isLoading = false
                        Log.e("GoogleLoginScreen", "Sign-in failure", e)
                        loginResult = LoginState.Error("Sign-in failed: ${e.message}")
                    }
            } ?: run {
                isLoading = false
                loginResult = LoginState.Error("Google token is null")
            }
        } catch (e: Exception) {
            isLoading = false
            Log.e("GoogleLoginScreen", "Sign-in error", e)
            loginResult = LoginState.Error("Google Sign-In Error: ${e.message}")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Login Button
        Button(
            onClick = {
                // Reset error state before attempting sign-in
                loginResult = LoginState.Initial
                isLoading = true

                val signInRequest = BeginSignInRequest.builder()
                    .setGoogleIdTokenRequestOptions(
                        BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                            .setSupported(true)
                            .setServerClientId("61093155991-pq671rp29ova6ejp6jg32j2coledo86k.apps.googleusercontent.com")
                            .setFilterByAuthorizedAccounts(false)
                            .build()
                    )
                    .build()

                googleSignInClient.beginSignIn(signInRequest)
                    .addOnSuccessListener { result ->
                        googleSignInLauncher.launch(
                            IntentSenderRequest.Builder(result.pendingIntent).build()
                        )
                    }
                    .addOnFailureListener { e ->
                        isLoading = false
                        Log.e("GoogleLoginScreen", "Begin sign-in failed", e)
                        loginResult = LoginState.Error("Google Sign-In Failed: ${e.message}")
                    }
            },
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
            shape = RoundedCornerShape(24.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    "Login by Gmail",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Result Display
        loginResult.let { state ->
            when (state) {
                is LoginState.Success -> {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .background(
                                color = Color(0xFFD1ECF1),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Success!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.Black
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Hi ${state.email}",
                                fontSize = 14.sp,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row {
                                Text(
                                    text = "Welcome to ",
                                    fontSize = 14.sp,
                                    color = Color.Black
                                )
                                Text(
                                    text = "UTHSmartTasks",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }

                is LoginState.Error -> {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .background(
                                color = Color(0xFFF8D7DA),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Google Sign-In Failed",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.Black
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "User canceled the Google sign-in process.",
                                fontSize = 14.sp,
                                color = Color.Black,
                            )
                        }
                    }
                }
                LoginState.Initial -> {} // Do nothing
            }
        }
    }
}

sealed class LoginState {
    object Initial : LoginState()
    data class Success(
        val userId: String,
        val email: String,
        val displayName: String?,
        val photoUrl: String?
    ) : LoginState()
    data class Error(val message: String) : LoginState()
}
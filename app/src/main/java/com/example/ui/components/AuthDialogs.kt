package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.AuthManager
import com.example.ui.theme.ThemeConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthDialog(
    onDismiss: () -> Unit,
    config: ThemeConfig
) {
    var isSignUpMode by remember { mutableStateOf(false) }
    
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            config = config
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Key lock icon
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(config.primary.copy(alpha = 0.15f))
                        .border(1.dp, config.primary.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSignUpMode) Icons.Filled.PersonAdd else Icons.Filled.Lock,
                        contentDescription = "Auth Icon",
                        tint = config.accent,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (isSignUpMode) "Create Your Account" else "Welcome to NextCut",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (isSignUpMode) "Sign up to save projects & render in cloud" else "Sign in to access your saved video edits",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )
                }

                if (successMessage != null) {
                    Text(
                        text = successMessage!!,
                        color = Color(0xFF10B981),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )
                }

                // Inputs Group
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSignUpMode) {
                        // Full Name Input
                        TextField(
                            value = name,
                            onValueChange = { 
                                name = it
                                errorMessage = null
                            },
                            placeholder = { Text("Full Name", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = Color.White.copy(alpha = 0.5f)) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = config.surface,
                                unfocusedContainerColor = config.surface.copy(alpha = 0.5f),
                                focusedIndicatorColor = config.primary,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                        )
                    }

                    // Email Input
                    TextField(
                        value = email,
                        onValueChange = { 
                            email = it
                            errorMessage = null
                        },
                        placeholder = { Text("Email Address", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null, tint = Color.White.copy(alpha = 0.5f)) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = config.surface,
                            unfocusedContainerColor = config.surface.copy(alpha = 0.5f),
                            focusedIndicatorColor = config.primary,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                    )

                    // Password Input
                    TextField(
                        value = password,
                        onValueChange = { 
                            password = it
                            errorMessage = null
                        },
                        placeholder = { Text("Password", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Filled.LockOpen, contentDescription = null, tint = Color.White.copy(alpha = 0.5f)) },
                        trailingIcon = {
                            val icon = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(icon, contentDescription = "Toggle password visibility", tint = Color.White.copy(alpha = 0.5f))
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = config.surface,
                            unfocusedContainerColor = config.surface.copy(alpha = 0.5f),
                            focusedIndicatorColor = config.primary,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                    )

                    if (isSignUpMode) {
                        // Confirm Password Input
                        TextField(
                            value = confirmPassword,
                            onValueChange = { 
                                confirmPassword = it
                                errorMessage = null
                            },
                            placeholder = { Text("Confirm Password", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Filled.LockOpen, contentDescription = null, tint = Color.White.copy(alpha = 0.5f)) },
                            trailingIcon = {
                                val icon = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(icon, contentDescription = "Toggle password visibility", tint = Color.White.copy(alpha = 0.5f))
                                }
                            },
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = config.surface,
                                unfocusedContainerColor = config.surface.copy(alpha = 0.5f),
                                focusedIndicatorColor = config.primary,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Submit Button
                GlassButton(
                    onClick = {
                        if (isSignUpMode) {
                            if (name.isBlank() || email.isBlank() || password.isBlank()) {
                                errorMessage = "All fields are required"
                            } else if (!email.contains("@")) {
                                errorMessage = "Please enter a valid email"
                            } else if (password.length < 6) {
                                errorMessage = "Password must be at least 6 characters"
                            } else if (password != confirmPassword) {
                                errorMessage = "Passwords do not match"
                            } else {
                                val success = AuthManager.signUp(name, email, password)
                                if (success) {
                                    successMessage = "Account created successfully!"
                                    onDismiss()
                                } else {
                                    errorMessage = "This email is already registered"
                                }
                            }
                        } else {
                            if (email.isBlank() || password.isBlank()) {
                                errorMessage = "Please fill in all fields"
                            } else {
                                val success = AuthManager.signIn(email, password)
                                if (success) {
                                    successMessage = "Signed in successfully!"
                                    onDismiss()
                                } else {
                                    errorMessage = "Invalid email or password"
                                }
                            }
                        }
                    },
                    text = if (isSignUpMode) "Register Account" else "Sign In Now",
                    config = config,
                    isPrimary = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Toggle Mode button
                Row(
                    modifier = Modifier.clickable {
                        isSignUpMode = !isSignUpMode
                        errorMessage = null
                        successMessage = null
                    },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isSignUpMode) "Already have an account? " else "Don't have an account? ",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                    Text(
                        text = if (isSignUpMode) "Sign In" else "Sign Up",
                        color = config.accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileDialog(
    onDismiss: () -> Unit,
    config: ThemeConfig
) {
    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            config = config
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // User Avatar Placeholder
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                listOf(config.primary, config.accent, config.primary)
                            )
                        )
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(config.background),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = AuthManager.currentUserName.take(2).uppercase(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = AuthManager.currentUserName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = AuthManager.currentUserEmail,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Subscription Badge
                val isPremium = com.example.ui.theme.SubscriptionManager.isPremium
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isPremium) config.primary.copy(alpha = 0.15f) else config.surface)
                        .border(1.dp, if (isPremium) config.primary.copy(alpha = 0.3f) else config.glassBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isPremium) Icons.Filled.AutoAwesome else Icons.Filled.StarOutline,
                                contentDescription = null,
                                tint = config.accent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isPremium) "NextCut Pro Account" else "NextCut Free Plan",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        
                        Text(
                            text = if (isPremium) "ACTIVE" else "UPGRADE",
                            color = config.accent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.clickable {
                                if (!isPremium) {
                                    com.example.ui.theme.SubscriptionManager.showPaywallDialog = true
                                    onDismiss()
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Sign Out Button
                GlassButton(
                    onClick = {
                        AuthManager.logOut()
                        onDismiss()
                    },
                    text = "Sign Out",
                    config = config,
                    isPrimary = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Thank you for using NextCut Studio!",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.3f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

package com.iqbotpro.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iqbotpro.app.ui.theme.*

private val PracticeOrange = Color(0xFFFF9800)
private val RealGreen = Color(0xFF4CAF50)

@Composable
fun SettingsScreen(
    practiceAccountId: String,
    hasPracticePassword: Boolean,
    realAccountId: String,
    hasRealPassword: Boolean,
    currentLanguage: String,
    onSavePractice: (String, String) -> Unit,
    onSaveReal: (String, String) -> Unit,
    onClearPractice: () -> Unit,
    onClearReal: () -> Unit,
    onChangeLanguage: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    var practiceId by remember(practiceAccountId) {
        mutableStateOf(practiceAccountId)
    }
    var practicePassword by remember { mutableStateOf("") }
    var realId by remember(realAccountId) {
        mutableStateOf(realAccountId)
    }
    var realPassword by remember { mutableStateOf("") }

    var practiceVisible by remember { mutableStateOf(false) }
    var realVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlackBackground)
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(Modifier.height(20.dp))

        Text(
            "Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = TealPrimary
        )

        Text(
            "Manage your trading accounts securely",
            color = TextSecondary,
            fontSize = 13.sp
        )

        Spacer(Modifier.height(20.dp))

        // PRACTICE ACCOUNT
        AccountCard(
            title = "PRACTICE ACCOUNT",
            color = PracticeOrange,
            accountId = practiceId,
            onAccountIdChange = { practiceId = it },
            password = practicePassword,
            onPasswordChange = { practicePassword = it },
            passwordVisible = practiceVisible,
            onTogglePassword = { practiceVisible = !practiceVisible },
            hasSavedPassword = hasPracticePassword,
            passwordHint = "Practice password",
            onSave = {
                onSavePractice(practiceId.trim(), practicePassword)
                practicePassword = ""
            },
            onClear = {
                onClearPractice()
                practiceId = ""
                practicePassword = ""
            }
        )

        Spacer(Modifier.height(18.dp))

        // REAL ACCOUNT
        AccountCard(
            title = "REAL ACCOUNT",
            color = RealGreen,
            accountId = realId,
            onAccountIdChange = { realId = it },
            password = realPassword,
            onPasswordChange = { realPassword = it },
            passwordVisible = realVisible,
            onTogglePassword = { realVisible = !realVisible },
            hasSavedPassword = hasRealPassword,
            passwordHint = "Real account password",
            onSave = {
                onSaveReal(realId.trim(), realPassword)
                realPassword = ""
            },
            onClear = {
                onClearReal()
                realId = ""
                realPassword = ""
            }
        )

        Spacer(Modifier.height(20.dp))

        // LANGUAGE
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = DarkSurfaceCard
            )
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "Language / اللغة",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val isEnglish = currentLanguage == "en"

                    Button(
                        onClick = { onChangeLanguage("en") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isEnglish) TealPrimary else DarkSurface
                        )
                    ) {
                        Text("English")
                    }

                    Button(
                        onClick = { onChangeLanguage("ar") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isEnglish) TealPrimary else DarkSurface
                        )
                    ) {
                        Text("العربية")
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = DarkSurface
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Security,
                    contentDescription = null,
                    tint = TealSecondary
                )

                Spacer(Modifier.width(10.dp))

                Text(
                    "Account passwords are stored using Android encrypted storage. " +
                        "Real trading must be explicitly confirmed before starting.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun AccountCard(
    title: String,
    color: Color,
    accountId: String,
    onAccountIdChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onTogglePassword: () -> Unit,
    hasSavedPassword: Boolean,
    passwordHint: String,
    onSave: () -> Unit,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurfaceCard
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            color.copy(alpha = 0.55f)
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = color
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    title,
                    color = color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(Modifier.height(16.dp))

            Text("Account email / ID", color = TextSecondary)

            Spacer(Modifier.height(6.dp))

            OutlinedTextField(
                value = accountId,
                onValueChange = onAccountIdChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email
                ),
                placeholder = {
                    Text("Enter account email", color = TextMuted)
                },
                colors = accountFieldColors(color)
            )

            Spacer(Modifier.height(14.dp))

            Text("Password", color = TextSecondary)

            Spacer(Modifier.height(6.dp))

            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = {
                    Text(
                        if (hasSavedPassword) "Saved securely - enter only to change"
                        else passwordHint,
                        color = TextMuted
                    )
                },
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password
                ),
                trailingIcon = {
                    IconButton(onClick = onTogglePassword) {
                        Icon(
                            if (passwordVisible) Icons.Default.VisibilityOff
                            else Icons.Default.Visibility,
                            contentDescription = "Toggle password visibility",
                            tint = TextSecondary
                        )
                    }
                },
                colors = accountFieldColors(color)
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = color,
                        contentColor = DeepBlackBackground
                    )
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Save")
                }

                if (hasSavedPassword || accountId.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onClear,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFE57373)
                        )
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Clear account"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun accountFieldColors(color: Color) =
    OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = color,
        unfocusedBorderColor = DarkSurfaceBorder,
        focusedContainerColor = DarkSurface,
        unfocusedContainerColor = DarkSurface
    )

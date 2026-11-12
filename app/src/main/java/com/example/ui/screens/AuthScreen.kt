package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.attemptAutoSignIn
import com.example.auth.onGoogleSignInClicked
import com.example.data.model.HscBoards
import com.example.data.model.HscStream
import com.example.ui.viewmodel.RegistrationDraft

private enum class AuthTab(val label: String) {
    SIGN_IN("Sign In"),
    SIGN_UP("Sign Up")
}

/**
 * Optional authentication screen for Cloud Sync and Admin Panel access.
 * Supports Sign In, Sign Up, Continue with Google, and returning to Guest Mode.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onPrepareRegistration: (RegistrationDraft?) -> Unit,
    onAuthSuccess: () -> Unit,
    onContinueAsGuest: (() -> Unit)? = null
) {
    if (onContinueAsGuest != null) {
        BackHandler(onBack = onContinueAsGuest)
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var activeTab by remember { mutableStateOf(AuthTab.SIGN_IN) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showForgotDialog by remember { mutableStateOf(false) }

    // Sign Up form state
    var fullName by remember { mutableStateOf("") }
    var emailHint by remember { mutableStateOf("") }
    var selectedStream by remember { mutableStateOf(HscStream.SCIENCE) }
    var selectedBoard by remember { mutableStateOf("Dhaka") }
    var boardDropdownExpanded by remember { mutableStateOf(false) }
    var collegeName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope
        )
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(w * 0.85f, h * 0.14f),
                        radius = w * 0.65f
                    ),
                    radius = w * 0.65f,
                    center = Offset(w * 0.85f, h * 0.14f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(secondaryColor.copy(alpha = 0.12f), Color.Transparent),
                        center = Offset(w * 0.12f, h * 0.78f),
                        radius = w * 0.55f
                    ),
                    radius = w * 0.55f,
                    center = Offset(w * 0.12f, h * 0.78f)
                )
                drawCircle(
                    color = primaryColor.copy(alpha = 0.12f),
                    radius = w * 0.48f,
                    center = Offset(w * 0.88f, h * 0.12f),
                    style = Stroke(width = 1.5f)
                )
                drawCircle(
                    color = tertiaryColor.copy(alpha = 0.10f),
                    radius = w * 0.34f,
                    center = Offset(w * 0.88f, h * 0.12f),
                    style = Stroke(width = 1.2f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 560.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Bolt,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "HSC EDGE",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Bangladesh Intermediate MCQ Portal",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f))
                        ) {
                            Text(
                                text = "NCTB HSC",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    // Prism Hero Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Image(
                                painter = painterResource(id = R.drawable.img_auth_cosmos),
                                contentDescription = "HSC Edge academic prism backdrop",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(148.dp),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(148.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.background.copy(alpha = 0.90f),
                                                MaterialTheme.colorScheme.background.copy(alpha = 0.35f)
                                            )
                                        )
                                    )
                            )
                            Column(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(18.dp)
                            ) {
                                Text(
                                    text = if (activeTab == AuthTab.SIGN_IN) "Sign In to Practice" else "Create Student Account",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "ICT, Bangla, English, Physics, Chemistry, Biology & Higher Math MCQs — protected until login.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.84f)
                                )
                            }
                        }
                    }

                    // Two-tab Auth Switcher (Sign In / Sign Up)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AuthTab.entries.forEach { tab ->
                                val selected = activeTab == tab
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .testTag("auth_mode_${tab.name.lowercase()}")
                                        .clickable {
                                            activeTab = tab
                                            errorMessage = null
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = if (tab == AuthTab.SIGN_IN) Icons.AutoMirrored.Rounded.Login else Icons.Rounded.PersonAdd,
                                            contentDescription = null,
                                            tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = tab.label,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (errorMessage != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.errorContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }

                    // Main Auth Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
                    ) {
                        AnimatedContent(
                            targetState = activeTab,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "auth_tab_anim"
                        ) { tab ->
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (tab == AuthTab.SIGN_IN) {
                                    Text(
                                        text = "Student & Admin Sign In",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Continue with your verified Google account to access all HSC subjects, timed exams, bookmarks, and saved results.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(
                                            onClick = { showForgotDialog = true },
                                            modifier = Modifier.testTag("forgot_password_button")
                                        ) {
                                            Icon(
                                                Icons.Rounded.HelpOutline,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Forgot Password / Help?")
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "Create Account (Step 1: Scholar Profile)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    OutlinedTextField(
                                        value = fullName,
                                        onValueChange = { fullName = it },
                                        label = { Text("Full Name") },
                                        placeholder = { Text("e.g., Tanvir Ahmed") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("signup_name_input")
                                    )
                                    OutlinedTextField(
                                        value = emailHint,
                                        onValueChange = { emailHint = it },
                                        label = { Text("Google Email Address") },
                                        placeholder = { Text("name@gmail.com") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("signup_email_input")
                                    )

                                    ExposedDropdownMenuBox(
                                        expanded = boardDropdownExpanded,
                                        onExpandedChange = { boardDropdownExpanded = it }
                                    ) {
                                        OutlinedTextField(
                                            value = "$selectedBoard Board",
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Education Board") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = boardDropdownExpanded) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        )
                                        ExposedDropdownMenu(
                                            expanded = boardDropdownExpanded,
                                            onDismissRequest = { boardDropdownExpanded = false }
                                        ) {
                                            HscBoards.all.forEach { board ->
                                                DropdownMenuItem(
                                                    text = { Text("$board Board") },
                                                    onClick = {
                                                        selectedBoard = board
                                                        boardDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    OutlinedTextField(
                                        value = collegeName,
                                        onValueChange = { collegeName = it },
                                        label = { Text("College Name (Optional)") },
                                        placeholder = { Text("e.g., Notre Dame College") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Text("HSC Group / Stream", style = MaterialTheme.typography.labelLarge)
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf(HscStream.SCIENCE, HscStream.BUSINESS, HscStream.HUMANITIES, HscStream.ALL).forEach { stream ->
                                            FilterChip(
                                                selected = selectedStream == stream,
                                                onClick = { selectedStream = stream },
                                                label = { Text(stream.label) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                                )
                                            )
                                        }
                                    }

                                    Text(
                                        text = "Your account is verified passwordlessly via Google Sign-In in this build.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

                                // Third option & primary action: Continue with Google (Credential Manager)
                                Button(
                                    onClick = {
                                        if (activeTab == AuthTab.SIGN_UP && fullName.trim().isBlank()) {
                                            errorMessage = "Please enter your Full Name before creating your account."
                                            return@Button
                                        }
                                        errorMessage = null
                                        if (activeTab == AuthTab.SIGN_UP) {
                                            onPrepareRegistration(
                                                RegistrationDraft(
                                                    displayName = fullName.trim(),
                                                    preferredStream = selectedStream,
                                                    board = selectedBoard,
                                                    college = collegeName.trim()
                                                )
                                            )
                                        } else {
                                            onPrepareRegistration(null)
                                        }
                                        isLoading = true
                                        onGoogleSignInClicked(
                                            context = context,
                                            credentialManager = credentialManager,
                                            onAuthSuccess = {
                                                isLoading = false
                                                onAuthSuccess()
                                            },
                                            onAuthError = { msg ->
                                                isLoading = false
                                                errorMessage = msg
                                            },
                                            scope = scope,
                                            onAuthCancelled = { isLoading = false }
                                        )
                                    },
                                    enabled = !isLoading,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .testTag("google_sign_in_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(22.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Authenticating...")
                                    } else {
                                        Icon(Icons.Rounded.Security, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = if (activeTab == AuthTab.SIGN_IN)
                                                "Continue with Google"
                                            else
                                                "Create Account with Google",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                if (onContinueAsGuest != null) {
                                    OutlinedButton(
                                        onClick = onContinueAsGuest,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("continue_as_guest_button"),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Text("Continue Without Sign-In", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }

                    // Auth Footer
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "HSC Edge • Bangladesh Intermediate Exam Prep",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "MCQs, timed exams, and bookmarks are unlocked after sign-in.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = { showForgotDialog = false },
            title = { Text("Account & Password Recovery") },
            text = {
                Text(
                    "This app uses Google Sign-In (Android Credential Manager), so no separate app password is stored. " +
                        "If you cannot access your Google account, use Google Account Recovery at accounts.google.com/signin/recovery."
                )
            },
            confirmButton = {
                OutlinedButton(onClick = { showForgotDialog = false }) {
                    Text("Got it")
                }
            }
        )
    }
}

package lab.insan.safeexit.ui.screen

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import lab.insan.safeexit.auth.BiometricAuthManager
import lab.insan.safeexit.ui.theme.DarkNavy
import lab.insan.safeexit.ui.theme.PrimaryRed
import lab.insan.safeexit.ui.theme.SecondaryBlue
import lab.insan.safeexit.ui.theme.TextPrimary
import lab.insan.safeexit.ui.theme.TextSecondary

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as FragmentActivity
    val biometricAuthManager = remember { BiometricAuthManager(activity) }

    var visible by remember { mutableStateOf(false) }
    var failureCount by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val maxAttempts = 3

    fun attemptAuth() {
        biometricAuthManager.authenticate(
            onSuccess = {
                onAuthSuccess()
            },
            onError = { message ->
                errorMessage = message
            },
            onFailed = {
                failureCount++
                if (failureCount >= maxAttempts) {
                    (context as? Activity)?.finishAffinity()
                }
            },
        )
    }

    LaunchedEffect(Unit) {
        visible = true
        attemptAuth()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavy),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Shield",
                    tint = PrimaryRed,
                    modifier = Modifier.size(72.dp),
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "SafeExit",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Authenticate to continue",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                )

                Spacer(modifier = Modifier.height(32.dp))

                if (failureCount > 0 && failureCount < maxAttempts) {
                    Text(
                        text = "${maxAttempts - failureCount} of $maxAttempts attempts remaining",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PrimaryRed,
                        textAlign = TextAlign.Center,
                    )
                }

                errorMessage?.let { message ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryBlue,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

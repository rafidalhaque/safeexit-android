package lab.insan.safeexit

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import lab.insan.safeexit.ui.navigation.SafeExitNavigation
import lab.insan.safeexit.ui.screen.AuthScreen
import lab.insan.safeexit.ui.theme.SafeExitTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SafeExitTheme {
                var isAuthenticated by remember { mutableStateOf(false) }

                if (!isAuthenticated) {
                    AuthScreen(onAuthSuccess = { isAuthenticated = true })
                } else {
                    SafeExitNavigation()
                }
            }
        }
    }
}

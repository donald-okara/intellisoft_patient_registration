package ke.don.intellisoft_patient_registration.feature.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

/**
 * Route composable: the [viewModel] parameter is required even though nothing here reads its
 * state - injecting it via Koin inside the Compose tree is what constructs SplashViewModel and
 * runs its init{} (the hydrate-then-navigate logic) in the first place.
 */
@Composable
fun SplashScreen(viewModel: SplashViewModel) {
    SplashComponent()
}

@Composable
private fun SplashComponent(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Preview
@Composable
private fun SplashComponentPreview() {
    SplashComponent()
}

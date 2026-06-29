package com.teamsabily.safeexit.auth

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class BiometricAuthManager(private val activity: FragmentActivity) {

    /**
     * Represents the availability status of biometric authentication on the device.
     */
    sealed class BiometricStatus {
        /** Biometric authentication is available and ready to use. */
        object Available : BiometricStatus()

        /** The device does not have biometric hardware. */
        object NoHardware : BiometricStatus()

        /** No biometric credentials are enrolled on the device. */
        object NotEnrolled : BiometricStatus()

        /** A security vulnerability has been discovered and biometrics are unavailable until a security update is applied. */
        object SecurityUpdateRequired : BiometricStatus()
    }

    private val authenticators =
        BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

    /**
     * Checks whether the device can perform biometric authentication.
     *
     * @return A [BiometricStatus] indicating availability.
     */
    fun canAuthenticate(): BiometricStatus {
        val biometricManager = BiometricManager.from(activity)
        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS ->
                BiometricStatus.Available

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                BiometricStatus.NoHardware

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                BiometricStatus.NotEnrolled

            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED ->
                BiometricStatus.SecurityUpdateRequired

            else ->
                BiometricStatus.NoHardware
        }
    }

    /**
     * Launches the biometric authentication prompt.
     *
     * When using [BiometricManager.Authenticators.DEVICE_CREDENTIAL] the system
     * provides its own cancel/negative button, so we must NOT call
     * [BiometricPrompt.PromptInfo.Builder.setNegativeButtonText].
     *
     * @param onSuccess Called when the user successfully authenticates.
     * @param onError   Called when an irrecoverable error occurs, with the error message.
     * @param onFailed  Called when a biometric is recognized but does not match (e.g. wrong finger).
     */
    fun authenticate(
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onFailed: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errString.toString())
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onFailed()
            }
        }

        val biometricPrompt = BiometricPrompt(activity, executor, callback)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock SafeExit")
            .setSubtitle("Authenticate to access the app")
            .setAllowedAuthenticators(authenticators)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}

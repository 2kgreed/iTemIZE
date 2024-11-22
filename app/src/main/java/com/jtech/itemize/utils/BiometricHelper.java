
package com.jtech.itemize.utils;

import android.content.Context;

import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import java.util.concurrent.Executor;

public class BiometricHelper {

    private BiometricPrompt biometricPrompt;

    public interface AuthenticationCallback {
        void onAuthenticationSuccess();
        void onAuthenticationFailure(String errorMessage);
    }

    public BiometricHelper(Context context, AuthenticationCallback callback) {
        Executor executor = ContextCompat.getMainExecutor(context);

        biometricPrompt = new BiometricPrompt((androidx.fragment.app.FragmentActivity) context, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        callback.onAuthenticationSuccess();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        callback.onAuthenticationFailure(errString.toString());
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        callback.onAuthenticationFailure("Authentication failed");
                    }
                });
    }

    public void authenticate(String title, String description) {
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setDescription(description)
                .setNegativeButtonText("Cancel")
                .build();
        biometricPrompt.authenticate(promptInfo);
    }
}

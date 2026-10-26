package com.itismob.s01.group01.tripfit;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Sprint 1 UI for the sign-in flow. Both the email/password form and the Google
 * button are UI only here; real authentication wiring comes in a later task.
 */
public class LoginActivity extends AppCompatActivity {

    /**
     * TESTING ONLY: skip credential validation and go straight to Home.
     * Gated on BuildConfig.DEBUG so it is compiled out of release builds and can never ship.
     */
    private static final boolean DEV_BYPASS_LOGIN = BuildConfig.DEBUG;

    private TextInputLayout layoutEmail;
    private TextInputLayout layoutPassword;
    private TextInputEditText inputEmail;
    private TextInputEditText inputPassword;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        layoutEmail = findViewById(R.id.layout_email);
        layoutPassword = findViewById(R.id.layout_password);
        inputEmail = findViewById(R.id.input_email);
        inputPassword = findViewById(R.id.input_password);

        findViewById(R.id.button_sign_in).setOnClickListener(v -> attemptSignIn());
        findViewById(R.id.button_google_sign_in).setOnClickListener(v -> goToHome());
    }

    private void attemptSignIn() {
        if (DEV_BYPASS_LOGIN) {
            goToHome();
            return;
        }

        boolean valid = true;

        if (TextUtils.isEmpty(safeText(inputEmail))) {
            layoutEmail.setError(getString(R.string.error_email_required));
            valid = false;
        } else {
            layoutEmail.setError(null);
        }

        if (TextUtils.isEmpty(safeText(inputPassword))) {
            layoutPassword.setError(getString(R.string.error_password_required));
            valid = false;
        } else {
            layoutPassword.setError(null);
        }

        if (!valid) {
            return;
        }

        goToHome();
    }

    private void goToHome() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }

    private String safeText(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }
}

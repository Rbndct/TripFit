package com.itismob.s01.group01.tripfit;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;

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
            // IME inset keeps the Sign In button reachable above the keyboard (edge-to-edge).
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        layoutEmail = findViewById(R.id.layout_email);
        layoutPassword = findViewById(R.id.layout_password);
        inputEmail = findViewById(R.id.input_email);
        inputPassword = findViewById(R.id.input_password);

        ViewCompat.setAccessibilityHeading(findViewById(R.id.text_brand), true);
        clearErrorOnEdit(inputEmail, layoutEmail);
        clearErrorOnEdit(inputPassword, layoutPassword);
        inputPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_DONE) {
                return false;
            }
            attemptSignIn();
            return true;
        });

        findViewById(R.id.button_sign_in).setOnClickListener(v -> attemptSignIn());
        findViewById(R.id.button_google_sign_in).setOnClickListener(v -> goToHome());
    }

    private void attemptSignIn() {
        if (DEV_BYPASS_LOGIN) {
            goToHome();
            return;
        }

        boolean emailMissing = TextUtils.isEmpty(safeText(inputEmail));
        boolean passwordMissing = TextUtils.isEmpty(safeText(inputPassword));

        layoutEmail.setError(emailMissing ? getString(R.string.error_email_required) : null);
        layoutPassword.setError(
                passwordMissing ? getString(R.string.error_password_required) : null);

        if (emailMissing) {
            inputEmail.requestFocus();
            return;
        }
        if (passwordMissing) {
            inputPassword.requestFocus();
            return;
        }

        goToHome();
    }

    /** Drops a field's error as soon as the user edits it, so stale errors never linger. */
    private void clearErrorOnEdit(TextInputEditText editText, TextInputLayout layout) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                layout.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void goToHome() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }

    private String safeText(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }
}

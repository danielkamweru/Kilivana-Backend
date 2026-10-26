package com.kilivana.backend.common.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves a self-contained HTML password reset page.
 *
 * <p>This page accepts a reset token from the URL query parameter and provides a form
 * for users to set a new password. The token field is masked and non-copyable for security.
 * The password field includes a toggle to show/hide the password for user convenience.
 * The form submits to the backend API endpoint to complete the password reset.
 */
@RestController
public class PasswordResetPageController {

    /**
     * Returns the password reset HTML page with the pre-filled token.
     *
     * @param token the password reset token from the email link (optional)
     * @return HTML page with password reset form
     */
    @GetMapping(value = "/reset-password", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> page(@RequestParam(required = false) String token) {
        String safeToken = token == null ? "" : token.replace("\"", "&quot;");
        String html = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>Reset Password</title>
              <style>
                body { font-family: system-ui, sans-serif; max-width: 420px; margin: 40px auto; padding: 0 16px; }
                label { display: block; margin-top: 12px; }
                .input-wrapper { position: relative; margin-top: 4px; }
                input { width: 100%%; padding: 8px; box-sizing: border-box; padding-right: 40px; }
                .toggle-password { position: absolute; right: 8px; top: 50%%; transform: translateY(-50%%); background: none; border: none; cursor: pointer; font-size: 18px; padding: 0; }
                button[type="submit"] { margin-top: 16px; padding: 10px 16px; }
                #msg { margin-top: 12px; }
              </style>
            </head>
            <body>
              <h1>Reset Password</h1>
              <form id="f">
                <label>Reset token
                  <div class="input-wrapper">
                    <input id="token" name="token" type="password" value="%s" required oncopy="return false;" oncut="return false;" onpaste="return false;">
                  </div>
                </label>
                <label>New password
                  <div class="input-wrapper">
                    <input id="password" name="password" type="password" required minlength="8">
                    <button type="button" class="toggle-password" onclick="togglePassword()">👁️</button>
                  </div>
                </label>
                <button type="submit">Reset password</button>
              </form>
              <div id="msg"></div>
              <script>
                function togglePassword() {
                  const passwordInput = document.getElementById('password');
                  const toggleBtn = document.querySelector('.toggle-password');
                  if (passwordInput.type === 'password') {
                    passwordInput.type = 'text';
                    toggleBtn.textContent = '🙈';
                  } else {
                    passwordInput.type = 'password';
                    toggleBtn.textContent = '👁️';
                  }
                }
                document.getElementById('f').addEventListener('submit', async (e) => {
                  e.preventDefault();
                  const token = document.getElementById('token').value;
                  const password = document.getElementById('password').value;
                  const res = await fetch('/api/v1/auth/reset-password', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ token, newPassword: password })
                  });
                  const data = await res.json().catch(() => ({}));
                  document.getElementById('msg').textContent = res.ok ? 'Password reset successful. You can now log in.' : (data.message || 'Reset failed.');
                });
              </script>
            </body>
            </html>
            """.formatted(safeToken);
        return ResponseEntity.ok(html);
    }
}

package com.kilivana.backend.common.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PasswordResetPageController {

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
                input { width: 100%%; padding: 8px; margin-top: 4px; box-sizing: border-box; }
                button { margin-top: 16px; padding: 10px 16px; }
                #msg { margin-top: 12px; }
              </style>
            </head>
            <body>
              <h1>Reset Password</h1>
              <form id="f">
                <label>Reset token<input id="token" name="token" value="%s" required></label>
                <label>New password<input id="password" name="password" type="password" required minlength="8"></label>
                <button type="submit">Reset password</button>
              </form>
              <div id="msg"></div>
              <script>
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

package com.kilivana.backend.mail;

import com.kilivana.backend.common.enums.UserRole;
import org.springframework.stereotype.Component;

@Component
public class EmailVerificationEmailTemplate {
    public String roleLabel(UserRole role) {
        return switch(role) {
            case FARMER -> "Farmer"; case SUPPLIER -> "Supplier"; case INSPECTOR -> "Inspector";
            case BUYER -> "Customer"; case DRIVER -> "Driver"; case ADMIN -> "Administrator";
        };
    }
    public String subject(UserRole role) { return "Kilivana \u2014 "+roleLabel(role)+" Email Verification"; }
    public String render(UserRole role,String code) {
        if(code==null || !code.matches("[0-9]{6}")) throw new IllegalArgumentException("Email verification requires a valid code");
        return """
            <!doctype html><html lang="en"><head><meta name="viewport" content="width=device-width, initial-scale=1"><title>Kilivana Email Verification</title></head>
            <body style="margin:0;background:#f4f7f4;font-family:Arial,sans-serif;color:#24352a">
            <table role="presentation" width="100%%" cellpadding="20"><tr><td align="center">
            <table role="presentation" width="100%%" style="max-width:520px;background:white;border-radius:12px" cellpadding="24">
            <tr><td><h1 style="color:#24713e;font-size:26px">KILIVANA</h1>
            <h2 style="font-size:21px">%s account email verification</h2>
            <p>Verify the email address for your Kilivana <strong>%s account</strong> using this six-digit code:</p>
            <p style="font-size:32px;letter-spacing:7px;font-weight:bold;background:#edf6ef;padding:18px;text-align:center">%s</p>
            <p>This code expires in <strong>10 minutes</strong> and can be used only once.</p>
            <p><strong>Do not share this code.</strong> Kilivana staff will never ask you to send it to them.</p>
            <p>If you did not request this verification, ignore this email. Your account will not be verified by ignoring it.</p>
            <p style="font-size:13px;color:#5d6b61">Email verification confirms your email address. Any required administrator approval remains a separate step.</p>
            <p>The Kilivana Team</p></td></tr></table></td></tr></table></body></html>
            """.formatted(roleLabel(role),roleLabel(role),code);
    }
}

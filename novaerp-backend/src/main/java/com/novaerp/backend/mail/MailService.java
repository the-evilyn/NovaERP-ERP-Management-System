package com.novaerp.backend.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    public void sendWelcomeEmail(String to, String fullName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("Welcome to NovaERP");
        message.setText("Hi " + fullName + ",\n\nYour NovaERP account has been created successfully.\n\nRegards,\nNovaERP Team");

        try {
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("Failed to send welcome email to {}: {}", to, e.getMessage());
        }
    }

    public void sendPasswordResetEmail(String to, String fullName, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("Reset your NovaERP password");
        message.setText("Hi " + fullName + ",\n\nWe received a request to reset your NovaERP password. "
                + "Click the link below to choose a new one. This link expires in 30 minutes.\n\n"
                + resetLink
                + "\n\nIf you didn't request this, you can safely ignore this email.\n\nRegards,\nNovaERP Team");

        try {
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("Failed to send password reset email to {}: {}", to, e.getMessage());
        }
    }
}

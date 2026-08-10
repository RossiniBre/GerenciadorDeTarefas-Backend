package com.taskmanager.domain.notification;

import com.taskmanager.domain.security.PasswordResetTokenSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

public class SmtpPasswordResetTokenSender implements PasswordResetTokenSender {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public SmtpPasswordResetTokenSender(JavaMailSender mailSender, String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void send(String toEmail, String rawToken) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("Recuperação de senha - Task Manager");
        message.setText(buildBody(rawToken));
        mailSender.send(message);
    }

    private String buildBody(String rawToken) {
        return "Você solicitou a recuperação de senha da sua conta.\n\n"
                + "Use o código abaixo para redefinir sua senha (válido por 30 minutos):\n\n"
                + rawToken + "\n\n"
                + "Se você não fez essa solicitação, ignore este e-mail — sua senha continua a mesma.";
    }
}

package com.utility.billing.service;

import com.utility.billing.config.MailProperties;
import com.utility.billing.entity.Bill;
import com.utility.billing.entity.Customer;
import com.utility.billing.entity.User;
import com.utility.billing.enums.RoleName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final MailProperties mailProperties;

    public void sendStaffCredentialsEmail(User user, RoleName role, String temporaryPassword) {
        String body = String.format("""
                Dear %s,

                Your WASAC/REG Utility Billing System account has been created.

                Account Details
                ---------------
                Username (Email): %s
                Temporary Password: %s
                Assigned Role: %s

                Role Responsibilities
                -------------------
                %s

                How to Access the System
                ------------------------
                1. Open: %s/swagger-ui.html
                2. Login via POST /api/auth/login with your email and temporary password
                3. Change your password immediately via POST /api/auth/change-password

                Do not share your credentials.

                Regards,
                WASAC/REG IT Administration
                """,
                user.getFullNames(), user.getEmail(), temporaryPassword, role.name(),
                roleResponsibilities(role), mailProperties.getBaseUrl());
        send(user.getEmail(), "Your WASAC/REG System Login Credentials", body);
    }

    public void sendRoleChangeEmail(User user, RoleName previousRole, RoleName newRole) {
        String body = String.format("""
                Dear %s,

                Your system role has been updated.

                Previous Role: %s
                New Role: %s

                Updated Responsibilities
                ------------------------
                %s

                Login: %s/swagger-ui.html

                Regards,
                WASAC/REG IT Administration
                """,
                user.getFullNames(),
                previousRole != null ? previousRole.name() : "None",
                newRole.name(),
                roleResponsibilities(newRole),
                mailProperties.getBaseUrl());
        send(user.getEmail(), "Your WASAC/REG Role Has Been Updated", body);
    }

    public void sendCustomerOtpEmail(User user, String otp, int expiryMinutes) {
        String body = String.format("""
                Dear %s,

                Thank you for registering with WASAC/REG Utility Billing.

                Your one-time verification code is:

                %s

                This code expires in %d minutes.

                To complete registration:
                1. Open %s/swagger-ui.html
                2. Call POST /api/auth/verify-otp with your email and this code
                3. After verification, log in with the password you chose during registration

                Do not share this code with anyone.

                Regards,
                WASAC/REG Customer Services
                """, user.getFullNames(), otp, expiryMinutes, mailProperties.getBaseUrl());
        send(user.getEmail(), "Your WASAC/REG Verification Code", body);
    }

    public void sendBillApprovalEmail(Bill bill) {
        Customer customer = bill.getCustomer();
        if (customer.getEmail() == null || customer.getEmail().isBlank()) {
            return;
        }
        String body = String.format(
                "Dear %s,%n%nYour utility bill of FRW %s has been approved.%nBill: %s%nDue: %s",
                customer.getFullNames(), bill.getTotalAmount(), bill.getBillNumber(), bill.getDueDate());
        send(customer.getEmail(), "Utility Bill Approved - " + bill.getBillNumber(), body);
    }

    public void sendBillGenerationEmail(Bill bill) {
        sendBillProcessedEmail(bill, "Utility Bill Generated");
    }

    public void sendBillPaidEmail(Bill bill) {
        sendBillProcessedEmail(bill, "Utility Bill Paid");
    }

    private void sendBillProcessedEmail(Bill bill, String subjectPrefix) {
        Customer customer = bill.getCustomer();
        if (customer == null || customer.getEmail() == null || customer.getEmail().isBlank()) {
            return;
        }
        String body = buildUtilityBillMessage(customer.getFullNames(), bill);
        send(customer.getEmail(), subjectPrefix + " - " + bill.getBillNumber(), body);
    }

    private String buildUtilityBillMessage(String customerName, Bill bill) {
        String period = YearMonth.of(bill.getBillingYear(), bill.getBillingMonth())
                .format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH));
        return String.format(
                "Dear %s,%nYour %s utility bill of %s FRW has been successfully processed.",
                customerName, period, bill.getTotalAmount());
    }

    private void send(String to, String subject, String body) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("Mail not configured; skipping email to {}", to);
            log.info("Email body for {}: {}", to, body);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailProperties.getFrom());
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent to {} - {}", to, subject);
        } catch (Exception ex) {
            log.error("Failed to send email to {} [{}]: {}", to, subject, ex.getMessage(), ex);
        }
    }

    private String roleResponsibilities(RoleName role) {
        return switch (role) {
            case ADMIN -> "- Configure tariffs and manage users\n- Manage customers and meters";
            case OPERATOR -> "- Capture meter readings\n- View meter reading history";
            case FINANCE -> "- Approve bills\n- Record and reconcile payments";
            case CUSTOMER -> "- View bills and payment history\n- Make payments";
        };
    }
}

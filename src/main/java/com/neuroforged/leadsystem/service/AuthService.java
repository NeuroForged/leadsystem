package com.neuroforged.leadsystem.service;

public interface AuthService {
    void changePassword(String email, String currentPassword, String newPassword);
}

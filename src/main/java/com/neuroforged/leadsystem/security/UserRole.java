package com.neuroforged.leadsystem.security;

public enum UserRole {
    ADMIN,
    CLIENT;

    public static boolean isClient(String role) {
        return CLIENT.name().equalsIgnoreCase(role);
    }

    public static boolean isAdmin(String role) {
        return ADMIN.name().equalsIgnoreCase(role);
    }
}

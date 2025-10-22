package com.roominate.models;

import java.util.ArrayList;
import java.util.List;

public class Admin extends User {
    private String adminLevel; // "super_admin", "moderator", "support"
    private List<String> permissions;
    private int resolvedIssues;
    private int verifiedListings;

    // Constructors
    public Admin() {
        super();
        this.permissions = new ArrayList<>();
        this.resolvedIssues = 0;
        this.verifiedListings = 0;
    }

    public Admin(String id, String email, String fullName, String phoneNumber, String adminLevel) {
        super(id, email, fullName, phoneNumber, "admin");
        this.adminLevel = adminLevel;
        this.permissions = new ArrayList<>();
        this.resolvedIssues = 0;
        this.verifiedListings = 0;
        initializePermissions();
    }

    // Initialize permissions based on admin level
    private void initializePermissions() {
        permissions.clear();
        switch (adminLevel) {
            case "super_admin":
                permissions.add("manage_users");
                permissions.add("manage_listings");
                permissions.add("manage_admins");
                permissions.add("view_reports");
                permissions.add("content_moderation");
                permissions.add("system_settings");
                break;
            case "moderator":
                permissions.add("manage_listings");
                permissions.add("view_reports");
                permissions.add("content_moderation");
                break;
            case "support":
                permissions.add("view_reports");
                permissions.add("content_moderation");
                break;
        }
    }

    // Getters and Setters
    public String getAdminLevel() {
        return adminLevel;
    }

    public void setAdminLevel(String adminLevel) {
        this.adminLevel = adminLevel;
        initializePermissions();
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    public void addPermission(String permission) {
        if (!this.permissions.contains(permission)) {
            this.permissions.add(permission);
        }
    }

    public void removePermission(String permission) {
        this.permissions.remove(permission);
    }

    public boolean hasPermission(String permission) {
        return this.permissions.contains(permission);
    }

    public int getResolvedIssues() {
        return resolvedIssues;
    }

    public void setResolvedIssues(int resolvedIssues) {
        this.resolvedIssues = resolvedIssues;
    }

    public void incrementResolvedIssues() {
        this.resolvedIssues++;
    }

    public int getVerifiedListings() {
        return verifiedListings;
    }

    public void setVerifiedListings(int verifiedListings) {
        this.verifiedListings = verifiedListings;
    }

    public void incrementVerifiedListings() {
        this.verifiedListings++;
    }

    @Override
    public String toString() {
        return "Admin{" +
                "id='" + getId() + '\'' +
                ", fullName='" + getFullName() + '\'' +
                ", adminLevel='" + adminLevel + '\'' +
                ", permissions=" + permissions.size() +
                ", resolvedIssues=" + resolvedIssues +
                ", verifiedListings=" + verifiedListings +
                '}';
    }
}

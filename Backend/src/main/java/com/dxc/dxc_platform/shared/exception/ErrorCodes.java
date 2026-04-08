package com.dxc.dxc_platform.shared.exception;

public final class ErrorCodes {

    private ErrorCodes() {
    }

    // =========================
    // COMMON / GENERIC
    // =========================
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
    public static final String ACCESS_DENIED = "ACCESS_DENIED";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
    public static final String CONFLICT = "CONFLICT";

    // =========================
    // USER
    // =========================
    public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
    public static final String USER_DELETED = "USER_DELETED";
    public static final String USER_LOCKED = "USER_LOCKED";
    public static final String USER_DISABLED = "USER_DISABLED";
    public static final String USER_NO_ROLE = "USER_NO_ROLE";
    public static final String USER_ALREADY_IN_TEAM = "USER_ALREADY_IN_TEAM";
    public static final String USER_NOT_IN_TEAM = "USER_NOT_IN_TEAM";
    public static final String USER_NOT_ASSIGNED_TO_PROJECT = "USER_NOT_ASSIGNED_TO_PROJECT";
    public static final String USER_NOT_ASSIGNED_TO_TASK = "USER_NOT_ASSIGNED_TO_TASK";
    public static final String USER_CANNOT_MODIFY_TASK = "USER_CANNOT_MODIFY_TASK";
    public static final String USER_ALREADY_ASSIGNED_TO_PROJECT = "USER_ALREADY_ASSIGNED_TO_PROJECT";
    public static final String USER_ALREADY_ASSIGNED_TO_TASK = "USER_ALREADY_ASSIGNED_TO_TASK";

    // =========================
    // ROLE / PERMISSION / PROFILE
    // =========================
    public static final String ROLE_NOT_FOUND = "ROLE_NOT_FOUND";
    public static final String PERMISSION_NOT_FOUND = "PERMISSION_NOT_FOUND";
    public static final String ROLE_REQUIRED = "ROLE_REQUIRED";
    public static final String PERMISSION_REQUIRED = "PERMISSION_REQUIRED";
    public static final String INSUFFICIENT_PERMISSIONS = "INSUFFICIENT_PERMISSIONS";
    public static final String PROFILE_NOT_FOUND = "PROFILE_NOT_FOUND";
    public static final String PROFILE_ALREADY_EXISTS = "PROFILE_ALREADY_EXISTS";

    // =========================
    // TEAM
    // =========================
    public static final String TEAM_NOT_FOUND = "TEAM_NOT_FOUND";
    public static final String TEAM_ALREADY_EXISTS = "TEAM_ALREADY_EXISTS";
    public static final String TEAM_NAME_REQUIRED = "TEAM_NAME_REQUIRED";
    public static final String TEAM_PROJECT_MANAGER_REQUIRED = "TEAM_PROJECT_MANAGER_REQUIRED";
    public static final String TEAM_PROJECT_MANAGER_ALREADY_ASSIGNED = "TEAM_PROJECT_MANAGER_ALREADY_ASSIGNED";
    public static final String TEAM_HAS_MEMBERS = "TEAM_HAS_MEMBERS";
    public static final String TEAM_HAS_PROJECTS = "TEAM_HAS_PROJECTS";
    public static final String TEAM_MEMBER_NOT_FOUND = "TEAM_MEMBER_NOT_FOUND";
    public static final String TEAM_MEMBER_ALREADY_EXISTS = "TEAM_MEMBER_ALREADY_EXISTS";

    // =========================
    // CLIENT
    // =========================
    public static final String CLIENT_NOT_FOUND = "CLIENT_NOT_FOUND";
    public static final String CLIENT_REQUIRED = "CLIENT_REQUIRED";
    public static final String CLIENT_ALREADY_EXISTS = "CLIENT_ALREADY_EXISTS";

    // =========================
    // PROJECT
    // =========================
    public static final String PROJECT_NOT_FOUND = "PROJECT_NOT_FOUND";
    public static final String PROJECT_ALREADY_EXISTS = "PROJECT_ALREADY_EXISTS";
    public static final String PROJECT_NAME_REQUIRED = "PROJECT_NAME_REQUIRED";
    public static final String PROJECT_DESCRIPTION_REQUIRED = "PROJECT_DESCRIPTION_REQUIRED";
    public static final String PROJECT_TEAM_REQUIRED = "PROJECT_TEAM_REQUIRED";
    public static final String PROJECT_CLIENT_REQUIRED = "PROJECT_CLIENT_REQUIRED";
    public static final String PROJECT_START_DATE_REQUIRED = "PROJECT_START_DATE_REQUIRED";
    public static final String PROJECT_END_DATE_REQUIRED = "PROJECT_END_DATE_REQUIRED";
    public static final String PROJECT_INVALID_DATE_RANGE = "PROJECT_INVALID_DATE_RANGE";
    public static final String PROJECT_STATUS_INVALID = "PROJECT_STATUS_INVALID";
    public static final String PROJECT_VALIDATION_COMMENT_REQUIRED = "PROJECT_VALIDATION_COMMENT_REQUIRED";
    public static final String PROJECT_REJECTION_COMMENT_REQUIRED = "PROJECT_REJECTION_COMMENT_REQUIRED";
    public static final String PROJECT_CANNOT_CLOSE_WITH_UNFINISHED_TASKS = "PROJECT_CANNOT_CLOSE_WITH_UNFINISHED_TASKS";
    public static final String PROJECT_REJECTED_BACK_TO_PREVALIDATED = "PROJECT_REJECTED_BACK_TO_PREVALIDATED";
    public static final String PROJECT_NOT_VISIBLE_FOR_USER = "PROJECT_NOT_VISIBLE_FOR_USER";
    public static final String PROJECT_NOT_VALIDATED = "PROJECT_NOT_VALIDATED";
    public static final String PROJECT_DATES_REQUIRED_FOR_CALENDAR = "PROJECT_DATES_REQUIRED_FOR_CALENDAR";

    // =========================
    // TASK
    // =========================
    public static final String TASK_NOT_FOUND = "TASK_NOT_FOUND";
    public static final String TASK_ALREADY_EXISTS = "TASK_ALREADY_EXISTS";
    public static final String TASK_TITLE_REQUIRED = "TASK_TITLE_REQUIRED";
    public static final String TASK_DESCRIPTION_REQUIRED = "TASK_DESCRIPTION_REQUIRED";
    public static final String TASK_PROJECT_REQUIRED = "TASK_PROJECT_REQUIRED";
    public static final String TASK_STATUS_INVALID = "TASK_STATUS_INVALID";
    public static final String TASK_ASSIGNMENT_MIN_MEMBERS = "TASK_ASSIGNMENT_MIN_MEMBERS";
    public static final String TASK_ASSIGNEE_REQUIRED = "TASK_ASSIGNEE_REQUIRED";
    public static final String TASK_NOT_LINKED_TO_PROJECT = "TASK_NOT_LINKED_TO_PROJECT";
    public static final String TASK_REJECTION_COMMENT_REQUIRED = "TASK_REJECTION_COMMENT_REQUIRED";
    public static final String TASK_VALIDATION_COMMENT_REQUIRED = "TASK_VALIDATION_COMMENT_REQUIRED";
    public static final String TASK_REJECTED_BACK_TO_IN_PROGRESS = "TASK_REJECTED_BACK_TO_IN_PROGRESS";
    public static final String TASK_MUST_BE_SUBMITTED_FOR_VALIDATION = "TASK_MUST_BE_SUBMITTED_FOR_VALIDATION";
    public static final String TASK_ONLY_ASSIGNED_MEMBERS_CAN_UPDATE = "TASK_ONLY_ASSIGNED_MEMBERS_CAN_UPDATE";
    public static final String TASK_ALREADY_COMPLETED = "TASK_ALREADY_COMPLETED";
    public static final String TASK_DEADLINE_EXCEEDED = "TASK_DEADLINE_EXCEEDED";

    // =========================
    // VALIDATION / AUDIT
    // =========================
    public static final String AUDIT_REQUIRED = "AUDIT_REQUIRED";
    public static final String AUDIT_NOT_FOUND = "AUDIT_NOT_FOUND";
    public static final String VALIDATION_COMMENT_REQUIRED = "VALIDATION_COMMENT_REQUIRED";
    public static final String REJECTION_COMMENT_REQUIRED = "REJECTION_COMMENT_REQUIRED";
    public static final String VALIDATION_ACTION_INVALID = "VALIDATION_ACTION_INVALID";

    // =========================
    // MESSAGING
    // =========================
    public static final String MESSAGE_NOT_FOUND = "MESSAGE_NOT_FOUND";
    public static final String MESSAGE_CONTENT_REQUIRED = "MESSAGE_CONTENT_REQUIRED";
    public static final String MESSAGE_SENDER_REQUIRED = "MESSAGE_SENDER_REQUIRED";
    public static final String MESSAGE_RECIPIENT_REQUIRED = "MESSAGE_RECIPIENT_REQUIRED";
    public static final String MESSAGE_RECIPIENTS_REQUIRED = "MESSAGE_RECIPIENTS_REQUIRED";
    public static final String MESSAGE_EMPTY_CONTENT = "MESSAGE_EMPTY_CONTENT";
    public static final String MESSAGE_SELF_SEND_FORBIDDEN = "MESSAGE_SELF_SEND_FORBIDDEN";
    public static final String MESSAGE_ACCESS_DENIED = "MESSAGE_ACCESS_DENIED";

    // =========================
    // NOTIFICATION / MAIL
    // =========================
    public static final String MAIL_SEND_FAILED = "MAIL_SEND_FAILED";
    public static final String NOTIFICATION_FAILED = "NOTIFICATION_FAILED";
    public static final String AUTOMATIC_MAIL_REQUIRED = "AUTOMATIC_MAIL_REQUIRED";

    // =========================
    // DASHBOARD / REPORTING
    // =========================
    public static final String DASHBOARD_DATA_UNAVAILABLE = "DASHBOARD_DATA_UNAVAILABLE";
    public static final String REPORT_NOT_FOUND = "REPORT_NOT_FOUND";

    // =========================
    // CALENDAR
    // =========================
    public static final String CALENDAR_ACCESS_DENIED = "CALENDAR_ACCESS_DENIED";
    public static final String CALENDAR_PROJECT_FILTER_INVALID = "CALENDAR_PROJECT_FILTER_INVALID";
}
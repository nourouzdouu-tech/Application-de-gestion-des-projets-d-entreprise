package com.dxc.dxc_platform.dto;

public class MemberWorkloadDto {

    private Long userId;
    private String fullName;
    private String email;
    private int activeTasks;
    private int overdueTasks;
    private int todoTasks;
    private int totalTasks;

    public MemberWorkloadDto() {}

    public MemberWorkloadDto(Long userId, String fullName, String email,
                             int activeTasks, int overdueTasks,
                             int todoTasks, int totalTasks) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.activeTasks = activeTasks;
        this.overdueTasks = overdueTasks;
        this.todoTasks = todoTasks;
        this.totalTasks = totalTasks;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public int getActiveTasks() { return activeTasks; }
    public void setActiveTasks(int activeTasks) { this.activeTasks = activeTasks; }

    public int getOverdueTasks() { return overdueTasks; }
    public void setOverdueTasks(int overdueTasks) { this.overdueTasks = overdueTasks; }

    public int getTodoTasks() { return todoTasks; }
    public void setTodoTasks(int todoTasks) { this.todoTasks = todoTasks; }

    public int getTotalTasks() { return totalTasks; }
    public void setTotalTasks(int totalTasks) { this.totalTasks = totalTasks; }
}
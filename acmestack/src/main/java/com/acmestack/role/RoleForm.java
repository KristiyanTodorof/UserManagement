package com.acmestack.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RoleForm {

    private Long id;

    @NotBlank(message = "Name is required")
    @Size(max = 50, message = "Maximum 50 characters")
    private String name;

    @Size(max = 255, message = "Maximum 255 characters")
    private String description;

    private String icon = "bi-shield";
    private String color = "indigo";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}
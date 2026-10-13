package com.acmestack.settings;

import jakarta.persistence.*;

@Entity
@Table(name = "integrations")
public class Integration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    private String icon;

    @Enumerated(EnumType.STRING)
    private IntegrationStatus status;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getIcon() {
        return icon;
    }

    public IntegrationStatus getStatus() {
        return status;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public void setStatus(IntegrationStatus status) {
        this.status = status;
    }
}
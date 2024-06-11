package com.justinrudolph.discordbot.models;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Table(name = "aston_dev_member")
@Data
public class AstonDevMember {

    @Id
    private Long id;

    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> roles;

    public void addRoles(List<String> roles) {
        this.roles.addAll(roles);
    }

    public void removeRoles (List<String> roles) {
        this.roles.removeAll(roles);
    }

}

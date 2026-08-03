package net.bitnp.guildofpioneers.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_departments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDepartment {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "department")
    private String department;
}

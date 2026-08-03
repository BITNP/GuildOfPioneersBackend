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
@Table(name = "user_students")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStudent {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "student_id")
    private String studentId;
}

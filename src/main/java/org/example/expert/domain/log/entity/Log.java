package org.example.expert.domain.log.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.user.entity.User;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "log")
@NoArgsConstructor
public class Log {
    /**
     * 컬럼 - 연관관계 컬럼을 제외한 컬럼을 정의합니다.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String message;

    private Long userId;

    private Long todoId;

    private LocalDateTime createdAt;

    public Log (String message, Long userId, Long todoId) {
        this.message = message;
        this.userId = userId;
        this.todoId = todoId;
        this.createdAt = LocalDateTime.now();
    }
}

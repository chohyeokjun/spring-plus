package org.example.expert.domain.todo.service;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.expert.client.WeatherClient;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.todo.dto.request.TodoSaveRequest;
import org.example.expert.domain.todo.dto.response.TodoResponse;
import org.example.expert.domain.todo.dto.response.TodoSaveResponse;
import org.example.expert.domain.todo.entity.QTodo;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.todo.repository.TodoRepository;
import org.example.expert.domain.user.dto.response.UserResponse;
import org.example.expert.domain.user.entity.QUser;
import org.example.expert.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TodoService {

    private final TodoRepository todoRepository;
    private final WeatherClient weatherClient;
    private final JPAQueryFactory jpaQueryFactory;

    @Transactional
    public TodoSaveResponse saveTodo(AuthUser authUser, TodoSaveRequest todoSaveRequest) {
        User user = User.fromAuthUser(authUser);

        String weather = weatherClient.getTodayWeather();

        Todo newTodo = new Todo(
                todoSaveRequest.getTitle(),
                todoSaveRequest.getContents(),
                weather,
                user
        );
        Todo savedTodo = todoRepository.save(newTodo);

        return new TodoSaveResponse(
                savedTodo.getId(),
                savedTodo.getTitle(),
                savedTodo.getContents(),
                weather,
                new UserResponse(user.getId(), user.getEmail(), user.getNickname())
        );
    }

    @Transactional(readOnly = true)
    public Page<TodoResponse> getTodos(int page, int size, String weather, LocalDateTime startDate, LocalDateTime endDate) {
        Pageable pageable = PageRequest.of(page - 1, size);

        if (weather != null && startDate != null && endDate != null) {
            throw new IllegalArgumentException("날씨 또는 기간 검색 중 하나만 선택하여 검색하세요.");
        }

        if ((startDate != null && endDate == null) || (startDate == null && endDate != null)) {
            throw new IllegalArgumentException("시작 날짜와 종료 날짜 둘 다 입력하거나 둘 다 미입력해야 합니다.");
        }

        // weather 로 검색
        if (weather != null) {
            return todoRepository.findAllByWeather(weather, pageable)
                    .map(todo -> new TodoResponse(
                            todo.getId(),
                            todo.getTitle(),
                            todo.getContents(),
                            todo.getWeather(),
                            new UserResponse(todo.getUser().getId(), todo.getUser().getEmail(), todo.getUser().getNickname()),
                            todo.getCreatedAt(),
                            todo.getModifiedAt()
                    ));
        }

        // 기간 검색
        if (startDate != null) {
            return todoRepository.findAllByStartDateAndEndDate(startDate, endDate, pageable)
                    .map(todo -> new TodoResponse(
                            todo.getId(),
                            todo.getTitle(),
                            todo.getContents(),
                            todo.getWeather(),
                            new UserResponse(todo.getUser().getId(), todo.getUser().getEmail(), todo.getUser().getNickname()),
                            todo.getCreatedAt(),
                            todo.getModifiedAt()
                    ));
        }

        // 일반 검색
        Page<Todo> todos = todoRepository.findAllByOrderByModifiedAtDesc(pageable);

        return todos.map(todo -> new TodoResponse(
                todo.getId(),
                todo.getTitle(),
                todo.getContents(),
                todo.getWeather(),
                new UserResponse(todo.getUser().getId(), todo.getUser().getEmail(), todo.getUser().getNickname()),
                todo.getCreatedAt(),
                todo.getModifiedAt()
        ));
    }

    @Transactional(readOnly = true)
    public TodoResponse getTodo(long todoId) {
        QTodo qTodo = QTodo.todo;
        QUser qUser = QUser.user;
        Todo todo = jpaQueryFactory
                .select(qTodo)
                .from(qTodo)
                .leftJoin(qTodo.user, qUser)
                .fetchJoin()
                .where(qTodo.id.eq(todoId))
                .fetchOne();

        if (todo == null) {
            throw new NullPointerException("일정이 존재하지 않습니다.");
        }

        return new TodoResponse(
                todo.getId(),
                todo.getTitle(),
                todo.getContents(),
                todo.getWeather(),
                new UserResponse(todo.getUser().getId(), todo.getUser().getEmail(), todo.getUser().getNickname()),
                todo.getCreatedAt(),
                todo.getModifiedAt()
        );
    }
}

package org.example.expert.domain.todo.service;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.expert.client.WeatherClient;
import org.example.expert.domain.comment.entity.QComment;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.manager.entity.QManager;
import org.example.expert.domain.todo.dto.request.TodoSaveRequest;
import org.example.expert.domain.todo.dto.response.TodoInfoDto;
import org.example.expert.domain.todo.dto.response.TodoResponse;
import org.example.expert.domain.todo.dto.response.TodoSaveResponse;
import org.example.expert.domain.todo.entity.QTodo;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.todo.repository.TodoRepository;
import org.example.expert.domain.user.dto.response.UserResponse;
import org.example.expert.domain.user.entity.QUser;
import org.example.expert.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

    @Transactional(readOnly = true)
    public Page<TodoInfoDto> searchTodos(int page, int size, String keyword, LocalDate startDate, LocalDate endDate, String nickname) {
        Pageable pageable = PageRequest.of(page - 1, size);

        QTodo qtodo = QTodo.todo;
        QManager qmanager = QManager.manager;
        QComment qcomment = QComment.comment;

        // 동적 쿼리를 위한 빌더
        BooleanBuilder builder = new BooleanBuilder();

        // 아무 조건도 입력하지 않은 경우 예외
        if (keyword == null && startDate == null && endDate == null && nickname == null) {
            throw new IllegalArgumentException("키워드, 기간, 닉네임 중 하나는 필수 입력입니다.");
        }

        // 기간 검색 시 startDate 또는 endDate만 입력한 경우 예외
        if ((startDate != null && endDate == null) || (startDate == null && endDate != null)) {
            throw new IllegalArgumentException("기간 검색 시 시작일과 종료일을 모두 입력하세요.");
        }

        // 모두 입력시
        if (keyword != null && (startDate != null || endDate != null || nickname != null)) {
            throw new IllegalArgumentException("키워드, 기간, 닉네임 중 하나만 입력하세요");
        }


        // keyword 검색
        if (keyword != null && startDate == null && endDate == null && nickname == null) {
            builder.and(qtodo.title.contains(keyword));
        }

        // nickname 검색
        if (keyword == null && startDate == null && endDate == null && nickname != null) {
            builder.and(qtodo.managers.any().user.nickname.contains(nickname));
        }

        // query
        JPAQuery<TodoInfoDto> todoInfoDtoJPAQuery = jpaQueryFactory.select(Projections.constructor(
                        TodoInfoDto.class,
                        qtodo.title,
                        qmanager.count(),
                        qcomment.count()
                ))
                .from(qtodo)
                .leftJoin(qtodo.managers, qmanager)
                .leftJoin(qtodo.comments, qcomment)
                .groupBy(qtodo.title)
                .offset(pageable.getOffset()) // page 처리 필수
                .limit(pageable.getPageSize()) // page 처리 필수
                .where(builder)
                .groupBy(qtodo.title);

        // 기간 검색
        if (keyword == null && startDate != null && endDate != null && nickname == null) {
            builder.and(qtodo.createdAt
                    .between(startDate.atStartOfDay(), LocalDateTime.of(endDate, LocalTime.MAX))); // atStartOfDay -> 00시 시작 , LocalTime.MAX -> 23:59:59까지
            todoInfoDtoJPAQuery.orderBy(qtodo.createdAt.max().desc());
        }

        // 결과 반환
        List<TodoInfoDto> todoInfoDtoList = todoInfoDtoJPAQuery.fetch();

        // 전체 데이터 개수(전체 page 표시 위해)
        long total = jpaQueryFactory.select(qtodo.title.count())
                .from(qtodo)
                .leftJoin(qtodo.managers, qmanager)
                .leftJoin(qtodo.comments, qcomment)
                .where(builder)
                .groupBy(qtodo.title)
                .fetchOne();

        // page 객체로 반환
        return new PageImpl<>(todoInfoDtoList, pageable, total);
    }
}

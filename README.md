# SPRING PLUS

### LV1 - 1 @Transactional의 이해

1. TodoService 클래스가 @Transactional(readOnly = true) 로 묶여 있어 삭제
    * reanOnly는 읽기 전용으로 Get 메서드에서만 사용! 생성이나 수정, 삭제 메서드에서는 사용 불가!
2. saveTodo 메서드에 @Transactional 사용
3. 나중에 알아보니 전체가 readOnly로 묶여 있어도 C,U,D 메서드에서만 따로 @Transactional 붙여도 동작 가능

***

### LV1 - 2 JWT의 이해

1. User의 정보에 nickname 추가
2. JWT 에서도 User의 nickname 꺼낼 수 있도록 Claims에 저장

***

### LV1 - 3 JPA의 이해 (JPQL 사용)

1. 예외 처리
    *  ~~~
       if (weather != null && startDate != null && endDate != null) {
       throw new IllegalArgumentException("날씨 또는 기간 검색 중 하나만 선택하여 검색하세요.");
       }

    *  ~~~
       if ((startDate != null && endDate == null) || (startDate == null && endDate != null)) {
       throw new IllegalArgumentException("시작 날짜와 종료 날짜 둘 다 입력하거나 둘 다 미입력해야 합니다.");
       }
2. 할 일 검색 시 weather 조건(weather 조건은 있을 수도 있고, 없을 수도 있다.)
    * @RequestParam(required = false) String weather 수정 -> required = false 로 설정하여 param 값이 필수가 아니여도 동작 가능하게 설정
    * ~~~
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
3. 할 일 검색 시 수정일 기준으로 기간 검색(기간의 시작과 끝 조건은 있을 수도 있고, 없을 수도 있다.)
    * ~~~
      @RequestParam(required = false) LocalDateTime startDate,
      @RequestParam(required = false) LocalDateTime endDate // 필수 값이 아니여도 가능한 조건 추가
    * ~~~
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

***

### LV1 - 4 테스트 코드 퀴즈 - 컨트롤러 테스트의 이해

1. 테스트 실패 이유
    * 상태코드가 200 OK를 반환하여 기대값과 실제 값이 다르게 반환.
2. Ok 부분을 BadRequest로 수정
    * ~~~
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.name()))
      .andExpect(jsonPath("$.code").value(HttpStatus.BAD_REQUEST.value()))
      .andExpect(jsonPath("$.message").value("Todo not found"));

***

### LV1 - 5  코드 개선 퀴즈 - AOP의 이해

1. UserAdminController 클래스의 changeUserRole() 메서드가 실행 전 동작
    * 현재 @After로 설정 되어 메서드 이후 실행되는 문제 발생 -> @Before 로 수정하여 메서드 실행 전 동작하도록 수정
2. 현재 경로가 org.exampal.expert.domain.user.controller.UserController.getUser(...)으로 설정
    * 개발 의도에 맞게 org.example.expert.domain.user.controller.UserAdminController.changeUserRole()로 수정

***

### LV2 - 6 JPA Cascade

1. 할 일을 새로 저장할 시, 할 일을 생성한 유저는 담당자로 자동 등록되어야 한다.
    * 등록 이므로 cascade 기능 중 persist 사용 -> @OneToMany(mappedBy = "todo", cascade = CascadeType.PERSIST)

***

### LV2 - 7 N+1

1. CommentController 클래스의 getComments() API를 호출할 때 N+1 문제가 발생
    * Entity에는 @ManyToOne(fetch = FetchType.LAZY) private User user; 으로 지연 설정이 잘 되어 있음
    * 그러면 repository에서 문제가 발생!
    * JPQL을 사용하여 조회 기능이 구현되어 있다. 하지만 comment를 조회할 때 fetch join을 사용하지 않아 N+1 문제 발생
    * @Query("SELECT c FROM Comment c JOIN c.user WHERE c.todo.id = :todoId") -> @Query("SELECT c FROM Comment c JOIN
      FETCH c.user WHERE c.todo.id = :todoId")

***

### LV2 - 8 QueryDSL(JPQL로 작성된 findByIdWithUser 를 QueryDSL로 변경)

1. gradle 의존성 추가
    * ~~~
      implementation 'com.querydsl:querydsl-apt:5.0.0'
      implementation 'com.querydsl:querydsl-jpa:5.0.0:jakarta'
      implementation 'com.querydsl:querydsl-core:5.0.0'
      annotationProcessor "com.querydsl:querydsl-apt:${dependencyManagement.importedProperties['querydsl.version']}:jakarta"
      annotationProcessor "jakarta.annotation:jakarta.annotation-api"
      annotationProcessor "jakarta.persistence:jakarta.persistence-api"
2. Q타입 클래스 사용을 위한 Bean 등록

    * ~~~
      @Configuration
      public class JPAConfiguration {

         @PersistenceContext
         private EntityManager entityManager;
     
         @Bean
         public JPAQueryFactory jpaQueryFactory() {
         return new JPAQueryFactory(entityManager);
         }
      }
3. JPQL 삭제
    * ~~~
      @Query("SELECT t FROM Todo t " +
      "LEFT JOIN t.user " +
      "WHERE t.id = :todoId")
      Optional<Todo> findByIdWithUser(@Param("todoId") Long todoId);
4. TodoService 수정
    * ~~~
      public TodoResponse getTodo(long todoId) {

      Todo todo = todoRepository.findByIdWithUser(todoId)
      .orElseThrow(() -> new InvalidRequestException("Todo not found"));

      User user = todo.getUser(); 
      return new TodoResponse(
      todo.getId(),
      todo.getTitle(),
      todo.getContents(),
      todo.getWeather(),
      new UserResponse(user.getId(), user.getEmail(), user.getNickname()),
      todo.getCreatedAt(),
      todo.getModifiedAt()
      );
      }

    * ~~~
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
    * 여기서도 마찬가지로 N+1 문제를 해결하기 위해 .fetchjoin() 사용!

***

### LV2 - 9 Spring Security

1. gradle 의존성 추가
    * ~~~
      implementation "org.springframework.boot:spring-boot-starter-security"
2. SecurityConfig 생성
    * ~~~
      @EnableWebSecurity
      @EnableMethodSecurity
      @Configuration
      @RequiredArgsConstructor
      public class WebSecurityConfig {
        private final JwtFilter jwtFilter;
        private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
        private final CustomAccessDeniedHandler customAccessDeniedHandler;
      
        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
      
        httpSecurity
        .csrf(csrf -> csrf.disable())
      
        .authorizeHttpRequests(authorize -> authorize
        // 인증X
        .requestMatchers("/auth/**").permitAll()
        // 인가(ADMIN)
        .requestMatchers("/admin/**").hasRole("ADMIN")
        .anyRequest().authenticated()
        )
      
        .formLogin(formLogin -> formLogin.disable())
      
        .httpBasic(httpBasic -> httpBasic.disable())
      
        // 동일한 페이지 내에서 접근 허용
        .headers(headers -> headers
        .frameOptions(frameOptions -> frameOptions.deny())
        )
      
        .sessionManagement(session -> session
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )
      
        .exceptionHandling(exceptions -> exceptions
        .authenticationEntryPoint(customAuthenticationEntryPoint)
        .accessDeniedHandler(customAccessDeniedHandler)
        )
      
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return httpSecurity.build();
        }
      }
      
3. filter 에서 중복된 코드 삭제

4. FilterConfig 클래스 삭제

***

### LV3 - 10 QueryDSL 을 사용하여 검색 기능 만들기(Projections를 활용해서 필요한 필드만 반환)

1. 새로운 API 생성
    * ~~~
      @GetMapping("/todos/search")
      public ResponseEntity<Page<TodoInfoDto>> searchTodos(

      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) LocalDate startDate,
      @RequestParam(required = false) LocalDate endDate,
      @RequestParam(required = false) String nickname
      ) {
      return ResponseEntity.ok(todoService.searchTodos(page, size, keyword, startDate, endDate, nickname));
      }
      
2. TodoInfoDto 클래스 생성
    * ~~~
      @Getter <br>
      public class TodoInfoDto {
      private final String title;
      private final Long managerCount;
      private final Long commentCount;

        public TodoInfoDto(String title, Long managerCount, Long commentCount) {
        this.title = title;
        this.managerCount = managerCount;
        this.commentCount = commentCount;
        }
      }

3. 기본 설정
    * q타입 클래스 선언 및 동적 쿼리 사용을 위한 빌더 설정
    * 일정에 대한 모든 정보가 아닌, 제목만
    * 해당 일정의 담당자 수 포함
    * 해당 일정의 총 댓글 개수 포함
    * 검색 결과는 페이징 처리되어 반환
    * ~~~
      @Transactional(readOnly = true)
      public Page<TodoInfoDto> searchTodos(int page, int size, String keyword, LocalDate startDate, LocalDate endDate,
      String nickname) {
      
       Pageable pageable = PageRequest.of(page - 1, size);

       QTodo qtodo = QTodo.todo;
       QManager qmanager = QManager.manager;
       QComment qcomment = QComment.comment;

       // 동적 쿼리를 위한 빌더
       BooleanBuilder builder = new BooleanBuilder();

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

3. 예외처리
    * ~~~
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

4. 검색 키워드로 일정의 제목을 검색(제목은 부분적으로 일치해도 검색이 가능)
    * ~~~
      // keyword 검색
      if (keyword != null && startDate == null && endDate == null && nickname == null) {
      builder.and(qtodo.title.contains(keyword));
      }

5. 일정의 생성일 범위로 검색(일정을 생성일 최신순으로 정렬)
   * order by 사용 하기 위해 Query 생성한 이후에 작성
     ~~~
     // 기간 검색
     if (keyword == null && startDate != null && endDate != null && nickname == null) {
     builder.and(qtodo.createdAt
     .between(startDate.atStartOfDay(), LocalDateTime.of(endDate, LocalTime.MAX))); // atStartOfDay -> 00시 시작 , LocalTime.MAX -> 23:59:59까지
     todoInfoDtoJPAQuery.orderBy(qtodo.createdAt.max().desc());
     }
     
6. 담당자의 닉네임으로도 검색이 가능(닉네임은 부분적으로 일치해도 검색이 가능)
   * ~~~
     // nickname 검색
     if (keyword == null && startDate == null && endDate == null && nickname != null) {
     builder.and(qtodo.managers.any().user.nickname.contains(nickname));
     }
     
***

### LV3 - 11 Transaction 심화(@Transactional의 옵션 중 하나를 활용하여 매니저 등록과 로그 기록이 각각 독립적으로 처리)
1. 매니저 등록 요청을 기록하는 로그 테이블 생성
   * Entity
   ~~~
   @Entity
   @Getter
   @Table(name = "log")
   @NoArgsConstructor
   public class Log {

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
   ~~~
   * Service
   ~~~
   @Service
   @RequiredArgsConstructor
   public class LogService {

    private final LogRepository logRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveLog (String message, Long userId, Long todoId) {
        Log log = new Log(message, userId, todoId);
        logRepository.save(log);
    }
   }
   ~~~
   * Repository
   ~~~
   public interface LogRepository extends JpaRepository<Log, Long> {
   }
   ~~~

2. ManagerService에서 saveManager 메서드 실행 시 최상단에 로그 추가
   ~~~
   logService.saveLog("매니저 등록 요청", authUser.getId(), todoId);
   

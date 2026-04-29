# 后端编码规范

## 基础约定

1. 后端基于 Spring Boot 3、Java 17、Spring Modulith、MyBatis-Flex、Lombok。
2. 根包为 `com.sakura.passage_creator`，新增代码必须放在清晰的业务模块或基础设施模块下。
3. 编写代码必须加中文注释：类、公开方法、关键字段、复杂业务判断必须有简洁说明。
4. 保持现有命名风格：实体 `Xxx`，请求 DTO `XxxAddRequest`、`XxxUpdateRequest`、`XxxQueryRequest`，视图对象 `XxxVO`，Mapper `XxxMapper`，服务 `XxxService` / `XxxServiceImpl`。
5. 不引入与现有技术栈重复的新框架；优先复用 `shared`、`infrastructure` 和各模块已有工具。
6. 所有用户可见响应、错误提示和接口契约变更，都要同步考虑前端兼容和测试覆盖。

## 模块边界

1. 顶层包必须是明确模块名，不允许随意新增。
2. 当前基础顶层包为 `shared`、`infrastructure`、`system`，旧 `support` 顶层包已经废弃，禁止恢复。
3. `shared` 只放真正无业务归属的基础类型、异常、注解、上下文、常量和工具。
4. `infrastructure` 只放技术适配、第三方配置、框架集成、认证基础设施和代码生成工具，不表达业务流程。
5. 业务模块放本领域能力，例如 `user`、`auth`、`dict`、`audit`、`notification`、`agreement`、`file`、`wxmp`。
6. 每个业务模块必须在模块根包创建 `package-info.java`，并使用 `@ApplicationModule` 声明模块说明和允许依赖。
7. 登录拦截、权限切面、Token 解析、WebMVC 拦截器注册属于 `infrastructure`，禁止放回 `user`。

## 依赖规则

1. 模块对外暴露内容只能放在 `api` 包。
2. `api` 包只能包含接口、事件、只读 DTO、简单枚举，不放 Entity、Mapper、ServiceImpl。
3. `repository`、`service.impl`、`internal` 包禁止被其他模块引用。
4. Controller 只调用本模块 Service 或允许依赖模块的 `api`，不调用其他模块的 ServiceImpl。
5. 跨模块同步调用只能依赖 `xxx.api`；跨模块副作用优先使用领域事件。
6. Entity 默认模块内部使用，不作为跨模块参数或返回值。
7. DTO 分两类：接口入参 DTO 属于本模块 `model/dto`；跨模块 DTO 属于 `api`。
8. 事件命名使用过去式，例如 `UserDisabledEvent`、`FileUploadedEvent`。
9. 事件监听涉及数据库一致性时使用 `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`。
10. `infrastructure` 不直接依赖业务模块；若基础设施需要业务数据，优先在基础设施定义端口接口，由业务模块提供适配实现。
11. 登录用户查询统一通过 `infrastructure.auth.LoginUserProvider` 端口或 `LoginUserContext`，不要把用户模块内部 Service 暴露给基础设施。

## 分层与接口

1. Controller 负责 HTTP 入参、权限注解、审计注解、统一响应封装，不承载复杂业务规则。
2. 管理端 Controller 按现有习惯放 `controller/admin`，用户端放 `controller/app`，公共开放接口放 `controller/publicapi`。
3. Controller 类使用 `@RestController`、`@RequestMapping`、`@Validated`，请求体使用 `@Valid @RequestBody`。
4. 接口返回统一使用 `BaseResponse<T>` 和 `ResultUtils.success(...)`，不要直接返回实体或 Map。
5. 需要管理员权限时使用 `@AuthCheck(mustRole = UserConstant.ADMIN_ROLE)`。
6. 需要记录管理操作时使用 `@AuditLogRecord`，明确 `description`、`module`、`operationType`。
7. Service 接口定义业务能力，ServiceImpl 承载业务规则、参数校验、事务和实体转换。
8. 对外展示数据优先返回 VO，使用 `BeanUtils.copyProperties` 按现有方式转换。
9. 批量实体转 VO 时，空集合返回空列表，不返回 `null`。

## 数据与持久化

1. 实体放 `model/entity`，使用 MyBatis-Flex 注解：`@Table`、`@Id(keyType = KeyType.Generator, value = KeyGenerators.flexId)`。
2. 逻辑删除字段统一使用 `isDelete`，实体字段标注 `@Column(isLogicDelete = true)`。
3. Mapper 放 `repository`，继承 `BaseMapper<Entity>` 并加 `@Mapper`。
4. 自定义 SQL 放 `src/main/resources/mapper`，namespace 必须对应 Mapper 全限定名。
5. 查询条件使用 MyBatis-Flex `QueryWrapper`，按字段非空条件追加。
6. 排序字段必须先用 `SqlUtils.validSortField` 校验，默认排序优先使用 `id` 倒序。
7. 分页请求继承 `PageRequest`，前后端统一使用 `page`、`pageSize`。
8. 涉及多表或多服务写操作的方法使用 `@Transactional(rollbackFor = Exception.class)`。
9. Long 类型 id 已由 `JsonConfig` 序列化为字符串，不要绕过统一 ObjectMapper。

## 校验、异常与响应

1. 参数校验优先使用 Jakarta Validation 注解，例如 `@NotBlank`、`@Positive`、`@Min`。
2. 业务校验失败使用 `BusinessException` 或 `ThrowUtils.throwIf`，错误码来自 `ErrorCode`。
3. 不要在 Controller 中吞异常或手写错误响应，交给 `GlobalExceptionHandler` 统一处理。
4. 错误信息优先使用消息 key，必要时可使用明确中文兜底文案。
5. 登录态从 `LoginUserContext.getLoginUser()` 或认证服务获取，不信任客户端传入的用户身份字段。
6. 密码、Token、审计失败等敏感流程必须避免泄露内部细节，日志中不要输出明文密码或完整 token。
7. 认证和审计类逻辑失败不能破坏主流程时，要像现有登录审计一样捕获并记录日志。

## 配置与资源

1. 主配置在 `application.yml`，环境差异放 `application-dev.yml`、`application-test.yml`、`application-prod.yml`。
2. 敏感配置通过 `.env`、`.env.<profile>`、`.env.local` 注入，不要写死真实密钥。
3. 国际化消息放 `messages*.properties`，错误码和响应文案要保持 key 可追踪。
4. 新增 `@ConfigurationProperties` 或配置项时，同步维护必要的元数据和注释。
5. OSS、Redis、微信、数据源等第三方集成配置归属 `infrastructure.config`。

## 注释与代码风格

1. 类注释说明职责，公开方法注释说明参数和返回值，复杂私有方法说明业务意图。
2. 注释解释“为什么这样做”，避免只重复代码表面含义。
3. 使用 Lombok `@Data`、`@EqualsAndHashCode` 等保持 DTO、VO、Entity 简洁。
4. 依赖注入优先遵循现有代码风格；新增复杂服务更推荐构造器注入以便测试。
5. 字符串判空使用 `StringUtils`，集合判空可使用 Hutool `CollUtil`。
6. 常量放对应模块或 `shared.constant`，枚举放对应模块 `enums` 或 `shared.enums`。
7. 不要在业务代码中硬编码魔法值；状态、角色、类型优先使用常量或枚举。

## 测试规范

1. 后端测试放 `src/test/java`，包路径与被测代码保持一致。
2. 跨模块依赖调整后必须运行 `ModulithArchitectureTest`。
3. 响应结构、分页字段、Long 序列化等前后端契约变更必须补充或更新契约测试。
4. 业务规则优先写聚焦单元测试，可使用 JUnit 5、Mockito、`ArgumentCaptor`。
5. 测试方法命名使用 `should...` 风格，必要时配合 `@DisplayName` 描述业务场景。
6. 不提交临时验证代码、临时测试数据或本地环境密钥。
7. 测试完后，如果用户需要删除测试类，就删除掉对应的测试类。

## 新增模块流程

1. 在 `com.sakura.passage_creator.<module>` 下创建模块根包。
2. 创建 `package-info.java` 并声明 `@ApplicationModule(displayName = "...", allowedDependencies = {...})`。
3. 默认只允许依赖 `shared`；确实需要技术适配时才依赖 `infrastructure`；需要业务能力时依赖目标模块的 `xxx::api`。
4. 将本模块内部实现放在 `controller`、`service`、`service.impl`、`repository`、`model` 等内部包。
5. 只有被其他模块调用的接口、事件或跨模块 DTO 才放入 `api` 包。
6. 新增或调整跨模块依赖后，必须运行 `ModulithArchitectureTest`。
7. 每次新增模块或跨模块依赖，必须更新 Modulith 架构测试和必要的迁移结构测试。

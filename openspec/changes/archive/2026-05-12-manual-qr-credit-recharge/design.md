## Context

当前项目分为 Vue 前端 `passage-creator` 和 Spring Boot 后端 `passage-creator-api`。积分能力集中在后端 `billing` 模块：用户端 `CreditClientController` 提供积分概览与流水，管理端 `CreditAdminController` 提供管理员直接手动充值、账户分页和流水分页；最终积分入账由 `CreditAccountService.recharge(...)` 完成，并在 `CreditAccountServiceImpl` 内使用 `Db.tx(...)` 更新账户与流水。前端用户积分页是 `src/pages/billing/index.vue`，管理端积分页是 `src/pages/credit-management/index.vue`，接口封装在 `src/services/api/credit.api.ts` 与 `src/services/types/credit.type.ts`。

这次变更面向内测人工充值，不接入微信支付、支付宝开放平台或支付回调。关键约束是：充值申请号必须由后端生成；前端只能传 `packageId`，金额和积分必须由后端套餐配置决定；管理员审核通过与积分入账必须在同一事务中完成；重复审核不能重复加积分。

## Goals / Non-Goals

**Goals:**

- 让用户在积分中心选择后端配置的套餐，创建人工充值申请，并看到充值申请号、收款码和人工审核提示。
- 为管理员提供可分页、可按状态筛选的充值申请列表，以及通过/拒绝审核操作。
- 复用现有积分账户和流水能力完成最终入账，减少账本规则重复实现。
- 通过状态机、事务和条件更新保证审核通过的幂等性，防止重复点击导致重复加积分。
- 前端沿用现有卡片、表格、按钮、提示组件，不引入新的 UI 框架。

**Non-Goals:**

- 不接入微信支付或支付宝开放平台。
- 不实现支付成功自动到账。
- 不实现真实支付回调、支付状态同步、退款或对账文件导入。
- 不让用户上传付款截图，除非后续另立变更。
- 不重构现有积分账本模型和管理员直接充值入口。

## Decisions

1. **将充值申请放在 `billing` 模块内，而不是新增支付模块。**  
   该能力本质是积分入账前的人工审核申请，强依赖积分账户与流水；放在 `billing` 内可以直接复用 `CreditAccountService`，也避免为内测人工流程创建过重的跨模块边界。备选方案是新增 `payment` 或 `recharge` 模块，但当前没有第三方支付和支付订单生命周期，模块会过早抽象。

2. **套餐和收款码使用后端配置作为可信源。**  
   新增 `ManualRechargeProperties`（建议前缀 `billing.manual-recharge`），包含启用状态、套餐列表、微信收款码 URL、支付宝收款码 URL。用户端查询套餐接口返回可选套餐；创建申请只接收 `packageId`、`payMethod` 和 `userRemark`。备选方案是前端硬编码套餐，但这会直接违反“金额和积分以后端为准”，也会让后续调价必须发前端版。

3. **新增 `credit_recharge_application` 表保存申请生命周期。**  
   字段包括 `id`、`recharge_no`、`user_id`、`package_id`、`amount`、`credits`、`pay_method`、`status`、`user_remark`、`admin_remark`、`audit_time`、`auditor`、`create_time`、`update_time`、`is_delete`。`recharge_no` 建唯一索引，`user_id/create_time`、`status/create_time` 建查询索引。申请创建时写入配置快照金额和积分，后续审核以申请快照为准，避免套餐调价影响已创建申请。

4. **审核通过使用状态条件更新包住幂等边界。**  
   `ManualRechargeApplicationService.approve(...)` 在同一 `Db.tx(...)` 中先读取申请并校验 `PENDING`，再执行状态条件更新：`id = ? AND status = 'PENDING'` 改为 `APPROVED`。只有更新成功的线程才能调用 `CreditAccountService.recharge(...)`，充值备注带上 `rechargeNo`，流水 `bizType` 建议为 `MANUAL_RECHARGE`、`bizId` 使用充值申请号。备选方案是先充值再改状态，但会在状态更新失败或并发点击时产生重复入账风险。

5. **用户端和管理端接口保持现有风格。**  
   用户端挂在 `/app/credit/recharge/**`，使用 `LoginUserContext` 取当前用户，并在查询详情时强制 `userId` 等于当前用户；管理端挂在 `/credit/admin/recharge-applications/**`，每个接口使用 `@AuthCheck(mustRole = UserConstant.ADMIN_ROLE)`，审核操作补充 `@AuditLogRecord`。响应继续使用 `BaseResponse<T>` 和 `ResultUtils.success(...)`。

6. **前端在现有页面内增量扩展。**  
   `billing/index.vue` 增加套餐选择、申请创建结果和申请列表，不做独立支付页；`credit-management/index.vue` 保留现有管理员直接充值卡片，并新增“人工充值申请”管理区。接口和类型扩展在现有 `credit.api.ts`、`credit.type.ts`，状态标签提供 `PENDING`、`APPROVED`、`REJECTED` 显示。

## Risks / Trade-offs

- **并发审核重复入账** → 通过同事务内 `PENDING` 条件更新作为唯一入账闸门，并为核心服务补充重复审核测试。
- **套餐配置调整影响历史申请** → 创建申请时保存金额和积分快照；审核只使用申请快照，不重新计算历史订单金额。
- **用户付款备注漏填申请号** → 前端创建申请后突出显示“付款备注必须填写充值申请号”，但系统不能自动校验真实收款备注，管理员仍需人工核对。
- **收款码资源路径配置错误** → 后端返回配置路径，前端只展示，不硬编码外链；部署时通过配置或静态资源检查确认图片可访问。
- **当前 `CreditAccountService.recharge(...)` 只接收管理员请求 DTO** → 可以最小扩展一个内部充值命令方法，或新增服务内部方法避免人工申请服务拼装面向 Controller 的 DTO；优先选择最小签名扩展，保持现有管理员接口兼容。
- **OpenSpec 初始化新增 `.codex` 文件** → 这是运行 `/opsx:propose` 所需的本地 OpenSpec/Codex 指令结构，不应与功能实现混淆。

## Migration Plan

1. 添加 MySQL 和 PostgreSQL 的 `credit_recharge_application` 建表 SQL。
2. 添加后端配置默认套餐与收款码路径，生产环境可用 `.env` 或 profile 配置覆盖图片地址。
3. 发布后端，确保表结构已创建且套餐接口可返回配置。
4. 发布前端，用户端显示人工充值入口，管理端显示审核列表。
5. 如需回滚，先下线前端入口；后端保留表和接口不影响现有管理员直接充值与积分流水。

## Open Questions

- 收款码图片最终放在前端 `public` 静态资源、后端静态目录，还是对象存储公开 URL，需要部署环境确认；实现时只要求从配置读取，不写死第三方外链。
- 是否保留现有“管理员直接手动充值”入口与人工申请审核入口并行。建议保留，作为运营补账和异常处理工具，但管理端文案需区分两者。

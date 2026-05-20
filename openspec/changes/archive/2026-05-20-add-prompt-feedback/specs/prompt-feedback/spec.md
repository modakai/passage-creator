## ADDED Requirements

### Requirement: 用户可以在指定创作环节提交提示词反馈
系统 SHALL 允许已登录用户在标题生成完毕待选择、大纲生成完毕待编辑、正文融合完成三个环节提交对应提示词效果反馈。

#### Scenario: 标题环节提交非常满意反馈
- **WHEN** 已登录用户在标题生成完毕待选择阶段提交 `TITLE_SELECTION` 环节的 `VERY_SATISFIED` 反馈
- **THEN** 系统记录该用户、创作任务、反馈环节、评价结果和提交时间

#### Scenario: 大纲环节提交不满意反馈
- **WHEN** 已登录用户在大纲生成完毕待编辑阶段提交 `OUTLINE_EDITING` 环节的 `UNSATISFIED` 反馈
- **THEN** 系统记录评价结果，且评价与对应创作任务和反馈环节关联

#### Scenario: 正文融合完成后提交反馈
- **WHEN** 已登录用户在正文融合完成后提交 `CONTENT_MERGED` 环节反馈
- **THEN** 系统记录该反馈并允许管理端按正文融合完成环节查询和统计

#### Scenario: rednote 完成后一次提交三类 Prompt 反馈
- **WHEN** 已登录用户在 rednote 全自动生成完成后选择满意度反馈
- **THEN** 前端 SHALL 分别提交 `REDNOTE_CONTENT`、`REDNOTE_NORMAL_IMAGE_PROMPT` 和 `REDNOTE_COVER_IMAGE_PROMPT` 三个反馈环节
- **AND** 系统 SHALL 分别关联 `rednote.content.system`、`rednote.normal-image-prompt.system` 和 `rednote.cover-image-prompt.system` 对应的 Prompt 使用日志

### Requirement: 提示词反馈是非强制的
系统 MUST NOT 要求用户必须填写提示词反馈才能继续或完成创作流程。

#### Scenario: 用户跳过标题反馈
- **WHEN** 标题生成完毕待选择阶段展示反馈弱提示
- **AND** 用户关闭、忽略或跳过反馈入口
- **THEN** 系统仍允许用户选择标题并继续创作流程

#### Scenario: 反馈提交失败不阻塞流程
- **WHEN** 用户提交反馈时接口失败或网络异常
- **THEN** 前端 SHALL 提示反馈提交失败，且不得阻止当前创作流程继续

### Requirement: 反馈记录关联提示词上下文
系统 SHALL 将反馈记录关联到可回溯的提示词上下文，包括创作任务、反馈环节、提示词模板标识和版本信息。

#### Scenario: 提交反馈时存在提示词使用日志
- **WHEN** 用户提交反馈且系统能通过创作任务和反馈环节匹配到提示词使用日志
- **THEN** 反馈记录 SHALL 保存提示词使用日志 ID、模板 ID、模板标识、版本和环境快照

#### Scenario: 提交反馈时缺少提示词使用日志
- **WHEN** 用户提交反馈但系统无法匹配到提示词使用日志
- **THEN** 系统 SHALL 仍保存反馈记录，并将提示词关联字段标记为空或未关联状态

### Requirement: 同一环节反馈按最终结果统计
系统 SHALL 对同一用户、同一创作任务、同一反馈环节只保留一条有效反馈记录。

#### Scenario: 用户重复提交同一环节反馈
- **WHEN** 用户对同一创作任务的同一反馈环节再次提交评价
- **THEN** 系统更新该环节已有反馈的评价结果、备注和更新时间，而不是新增重复统计记录

#### Scenario: 用户提交不同环节反馈
- **WHEN** 用户分别提交标题、大纲和正文融合三个环节反馈
- **THEN** 系统分别保存三个环节的有效反馈记录

#### Scenario: rednote 同一次反馈保存为三个模板记录
- **WHEN** 用户对同一个 rednote 任务完成态提交一次反馈
- **THEN** 系统按三个 rednote Prompt 环节分别保存有效反馈记录，且每条记录只关联一个 Prompt 模板快照

### Requirement: 管理员可以查询提示词反馈记录
系统 SHALL 为管理员提供提示词反馈记录分页查询能力，并 MUST 要求管理员权限。

#### Scenario: 管理员分页查询反馈记录
- **WHEN** 管理员请求提示词反馈记录分页列表
- **THEN** 系统返回反馈 ID、用户信息、创作任务 ID、反馈环节、评价结果、备注、提示词模板标识、版本和时间信息

#### Scenario: 管理员按环节筛选反馈
- **WHEN** 管理员按文章环节或 rednote Prompt 环节筛选反馈记录
- **THEN** 系统仅返回对应环节的反馈记录

#### Scenario: 非管理员不能访问反馈记录
- **WHEN** 非管理员请求管理端提示词反馈记录接口
- **THEN** 系统拒绝访问

### Requirement: 管理员可以查看提示词反馈四档满意度占比
系统 SHALL 为管理员提供提示词反馈统计能力，按已提交反馈计算非常满意、满意、一般和不满意的数量及占比。

#### Scenario: 管理员查看全部环节统计
- **WHEN** 管理员请求提示词反馈统计
- **THEN** 系统按反馈环节返回 `verySatisfiedCount`、`satisfiedCount`、`neutralCount`、`unsatisfiedCount`、`totalCount` 以及对应占比字段

#### Scenario: 管理员按提示词版本统计
- **WHEN** 管理员按模板标识和版本筛选统计
- **THEN** 系统基于匹配的已提交反馈计算四档满意度数量和占比

#### Scenario: 跳过反馈不计入满意度占比
- **WHEN** 用户未提交某个环节反馈
- **THEN** 系统不得将该未提交行为计入四档满意度数量或 `totalCount`

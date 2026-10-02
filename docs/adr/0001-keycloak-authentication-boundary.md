# Keycloak 认证与本站成员资格分离

本站登录改用 Keycloak OIDC，替换本地密码登录；本站继续拥有用户资料、部门、角色和业务权限。Keycloak 认证成功但尚未关联本站用户的访问者，需要提交有效注册票据并填写必要资料，才能取得本站成员资格；统一身份本身不授予业务权限。新部署中经维护者明确指定的首个管理员可以在完善资料后免票据初始化管理入口。

采用后端授权码登录并建立本站 Cookie 会话，沿用现有前后端的会话交互。[参考项目](https://github.com/BITNP/wordle-competition/blob/main/internal/auth/auth.go)采用 OIDC 后换发本站 Bearer token，但本站已有用户表和 Cookie 会话，继续使用会话可以避免前端保存令牌，也不需要迁移整个 API 的凭证方式。

用户名、手机号、邮箱和头像继续由本站维护；首次准入可预填 Keycloak 资料，后续登录不自动覆盖。保留本站用户名唯一、手机号必填及现有资料编辑规则。

使用已有 Keycloak 服务，通过环境变量配置 issuer、client ID、client secret 和本站公开地址。本决定及以上资料归属细则已由用户确认。

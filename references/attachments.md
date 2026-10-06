# 附件与能力

## NeoForge：Attachment 为主

注册入口 `registry/<ModId>Attachments.java`，数据类放 `registry/attachment/`：

```
registry/
├── <ModId>Attachments.java       doRegister(eventBus)
└── attachment/
    ├── BindStatusData.java
    ├── ChatModeData.java
    └── sub/
        ├── EntityDTO.java
        └── TieStats.java
```

- 数据类属于「注册时必需的数据」，因此放在 `registry` 下（判定见 [registry.md](registry.md)）。
- 嵌套的辅助数据放 `attachment/sub/`。

## Forge：Capability

Forge 1.20.1 线用 Capability：注册入口 `registry/<ModId>Capabilities.java`，能力实现放 `capability/` 包。

```
registry/
└── <ModId>Capabilities.java
capability/
├── <Feature>DataCapability.java
└── StatsDataCapability.java
```

## 选择原则

- NeoForge 世代优先用 Attachment，不要为了「统一」而在 NeoForge 上用 Capability 风格。
- Forge 世代用 Capability。
- 具体采用哪种取决于目标 MC 世代，不确定时先问用户。

## 同步

需要客户端可见的数据，通过 Attachment 的同步能力或网络包（见 [network.md](network.md)）同步；服务端必须重新校验来自客户端的数据。

## 完成前检查

- 附件数据是否真的需要持久化 / 同步，还是可以每次推导？
- 是否在正确的端读写？
- 客户端提交的数据是否在服务端重新校验？
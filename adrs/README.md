# Architectural Decision Records — Planora

Este diretório contém os ADRs (Architectural Decision Records) do backend do Planora.
Cada arquivo documenta uma decisão arquitetural significativa: o contexto que a motivou,
a decisão tomada, e as consequências positivas e negativas.

| ADR | Título | Status |
|-----|--------|--------|
| [ADR-001](ADR-001.md) | Uso de PostgreSQL como banco de dados relacional | Aceito |
| [ADR-002](ADR-002.md) | Arquitetura Monolítica Modular | Aceito |
| [ADR-003](ADR-003.md) | Autenticação via GitHub OAuth2 com JWT RS256 próprio | Aceito |
| [ADR-004](ADR-004.md) | Cliente GitHub como Interface Declarativa com Spring HTTP Exchange | Aceito |
| [ADR-005](ADR-005.md) | Sincronização de Issues via Webhook do GitHub | Aceito |
| [ADR-006](ADR-006.md) | Identificação de Repositório no Formato owner/repository | Aceito |
| [ADR-007](ADR-007.md) | Geração de Backlog via Chamada Síncrona à API Python | **Substituído pelo ADR-008** |
| [ADR-008](ADR-008.md) | Geração de Backlog via Sistema de Jobs com Callback Assíncrono | Aceito |

<div align="center">

<img src="https://img.shields.io/badge/version-1.0.0-blue?style=for-the-badge" alt="Version" />
<img src="https://img.shields.io/badge/status-em%20desenvolvimento-yellow?style=for-the-badge" alt="Status" />
<img src="https://img.shields.io/badge/integração-GitHub%20API-black?style=for-the-badge&logo=github" alt="GitHub API" />

<br/><br/>

# Planora

### *Transforme documentação em backlogs estruturados.*

**Planora** é uma plataforma inteligente que processa documentos de requisitos via IA e gera, de forma autônoma, issues completas e sincronizadas diretamente no seu repositório GitHub — eliminando o trabalho manual entre gestores e desenvolvedores.

<br/>

[📖 Documentação](#-documentação) · [✨ Funcionalidades](#-funcionalidades) · [🛠 Stack](#-stack-tecnológica) · [🤝 Contribuição](#-contribuindo)

</div>

---

## 📋 Índice

- [Sobre o Projeto](#-sobre-o-projeto)
- [Funcionalidades](#-funcionalidades)
- [Stack Tecnológica](#-stack-tecnológica)
- [Licença](#-licença)

---

## 💡 Sobre o Projeto

O mercado de tecnologia enfrenta um gap crítico: **a gestão de projetos e o desenvolvimento técnico raramente se comunicam de forma automatizada**. Gestores perdem horas digitando tarefas repetitivas, e desenvolvedores recebem issues incompletas ou mal descritas.

**IssueForge AI** resolve esse problema combinando:

- 📄 **Processamento inteligente de documentos** — Analisa arquivos `.txt` com requisitos funcionais, não-funcionais e regras de negócio
- 🧠 **Geração automatizada de backlog** — A IA converte requisitos brutos em issues com título, descrição técnica e critérios de aceitação
- 🔗 **Integração nativa com GitHub** — Sincroniza tudo diretamente no repositório via API, sem copiar e colar

> *"O gestor deixa de ser um digitador de tarefas e passa a ser um tomador de decisão estratégica."*

### O problema que resolvemos

| Antes | Depois |
|---|---|
| Horas criando issues manualmente | Issues geradas em segundos |
| Requisitos perdidos em documentos | Backlog estruturado e rastreável |
| Gap entre negócio e técnico | Tradução automática e precisa |
| Planejamento de sprint empírico | Estimativas baseadas em dados |
| Inconsistências entre documentos | Alertas proativos de integridade |

---

## ✨ Funcionalidades

### 🔄 Automação de Ingestão de Dados
Converte arquivos `.txt` brutos em issues completas, contendo:
- Título objetivo e descritivo
- Descrição técnica detalhada
- Critérios de aceitação padronizados (Gherkin / checklist)
- Labels, milestones e assignees sugeridos

### 📅 Otimização de Planejamento
- Estimativas de tempo total de produção baseadas na densidade do backlog
- Sugestões automáticas de divisão de Sprints
- Priorização inteligente de issues por complexidade e dependência

### 📐 Engenharia de Requisitos Autônoma
Extrai e documenta automaticamente:
- Requisitos funcionais
- Requisitos não-funcionais
- Regras de negócio
- Restrições técnicas

### 🔍 Monitoramento de Integridade
- Detecção proativa de inconsistências entre documentos
- Sugestões de melhorias de arquitetura
- Relatórios executivos on-demand

### 🌉 Eliminação do Gap de Comunicação
- Tradução automática entre necessidades de negócio e linguagem técnica
- Issues que refletem fielmente os objetivos comerciais
- Histórico rastreável de decisões

---

## 🛠 Stack Tecnológica

| Camada | Tecnologia |
|---|---|
| **Back-end** | Java Spring Boot |
| **IA / LLM** | Fine-Tuning |
| **Integração** | GitHub REST API v3 |
| **Autenticação** | GitHub OAuth · Personal Access Token |
| **Processamento** | Parsing de `.txt` · NLP Pipeline |
| **Infraestrutura** |  |

---

## Rondando o projeto

### Criando chave pública e privada

```
# 1. Generate PKCS#8 private key 
openssl genpkey -algorithm RSA -out app.key -pkeyopt rsa_keygen_bits:2048 

# 2. Extract the public key 
openssl rsa -pubout -in app.key -out app.pub
```

### Variáveis de ambiente

```
DB_PASSWORD=MinhaSenh@Segura123
DB_URL=jdbc:postgresql://localhost:5432/meu_banco
DB_USERNAME=postgres
GITHUB_CLIENT_ID=Ov23liABCDEF1234567
GITHUB_CLIENT_SECRET=a1b2c3d4e5f6789012345678901234567890abcd
```

---

## 🤝 Contribuindo

Contribuições são muito bem-vindas! Siga os passos abaixo:

1. **Fork** o repositório
2. Crie uma branch para sua feature:
   ```bash
   git checkout -b feature/minha-feature
   ```
3. Faça seus commits com mensagens claras:
   ```bash
   git commit -m "feat: adiciona suporte a arquivos PDF"
   ```
4. Envie para sua branch:
   ```bash
   git push origin feature/minha-feature
   ```
5. Abra um **Pull Request** descrevendo suas mudanças

Por favor, leia o [CONTRIBUTING.md](CONTRIBUTING.md) para mais detalhes sobre nosso código de conduta e processo de revisão.

### Padrão de Commits

Seguimos o [Conventional Commits](https://www.conventionalcommits.org/):

| Prefixo | Uso |
|---|---|
| `feat:` | Nova funcionalidade |
| `fix:` | Correção de bug |
| `docs:` | Alteração na documentação |
| `refactor:` | Refatoração de código |
| `test:` | Adição ou correção de testes |

---

## 📄 Licença

Distribuído sob a licença **GNU GENERAL PUBLIC**. Veja o arquivo [LICENSE](https://github.com/llucascr/Planora/blob/lucas/LICENSE.md) para mais detalhes.

---

## 📬 Contato

Dúvidas, sugestões ou problemas? Abra uma [issue](https://github.com/seu-usuario/issueforge-ai/issues) ou entre em contato:

- 📧 Email: `seu-email@exemplo.com`
- 🌐 LinkedIn: [LinkedIn](https://linkedin.com/in/seu-perfil)

---

<div align="center">

<a href="#-issueforge-ai">Voltar ao topo ↑</a>

</div>

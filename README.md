# Votos — API de Votação em Assembleias Cooperativas

API REST em Spring Boot para cadastro de pautas, abertura de sessões de
votação e apuração de resultados, com o contrato de "telas" descrito no
Anexo 1 do desafio.

## Como rodar

```bash
docker compose up -d
./gradlew bootRun
```

A aplicação detecta o `compose.yaml` automaticamente (Spring Boot Docker
Compose support) e injeta as credenciais do Postgres. A API fica
disponível em `http://localhost:8080`, com Swagger UI em
`http://localhost:8080/swagger-ui.html`.

Para rodar a aplicação também em container (build multi-stage via
`Dockerfile`):

```bash
docker compose --profile app up -d --build
```

A imagem é otimizada para startup: o build roda o **Spring AOT**
(`processAot`, definições de beans geradas em tempo de build) e uma
execução de treino gera o **cache AOT da JVM** (Java 25, JEP 483/514/515)
com as classes já carregadas e linkadas. Medido contra o Postgres do
compose: ~5,2 s na JVM pura → ~4,6 s só com Spring AOT → **~2,0 s** com
Spring AOT + cache AOT.

## Configuração

| Propriedade | Default | Descrição |
|---|---|---|
| `voting.session.default-duration` | `60s` | Duração da sessão quando não informada na abertura |
| `voting.member.base-url` | `https://user-info.herokuapp.com` | Base URL do serviço externo de verificação de CPF (bônus 1) |
| `voting.member.verification-enabled` | `false` | Habilita a verificação de CPF no serviço externo antes de registrar cada voto (bônus 1) |
| `spring.http.clients.connect-timeout` | `3s` | Timeout de conexão dos clientes HTTP (inclui o de verificação de CPF) |
| `spring.http.clients.read-timeout` | `5s` | Timeout de leitura dos clientes HTTP (inclui o de verificação de CPF) |

### Verificação de CPF (bônus 1) desabilitada por padrão

O serviço de demonstração indicado no desafio para verificar CPFs
(`https://user-info.herokuapp.com`) foi descontinuado — o Heroku encerrou
seu plano gratuito e o app não existe mais (responde 404 "No such app").
Por isso o bônus 1 é entregue **desabilitado por padrão**: as
funcionalidades principais (pautas, sessões, votos e apuração) funcionam
imediatamente, sem depender de um serviço externo inexistente. A
integração continua completa e coberta por testes (com o cliente
mockado); para ativá-la, defina
`voting.member.verification-enabled=true` e aponte
`voting.member.base-url` para um serviço substituto (real ou stub) que
implemente `GET /users/{cpf}` respondendo
`{"status": "ABLE_TO_VOTE" | "UNABLE_TO_VOTE"}` (ou 404 para CPF
inválido).

## Endpoints principais

- `POST /api/v1/pautas` — cadastra pauta
- `GET /api/v1/pautas` — lista pautas (tela SELECAO)
- `GET /api/v1/pautas/{id}` — detalhe contextual da pauta (tela FORMULARIO/SELECAO)
- `POST /api/v1/pautas/{id}/sessoes` — abre sessão de votação
- `POST /api/v1/pautas/{id}/votos` — registra voto
- `GET /api/v1/pautas/{id}/resultado` — resultado da votação (tela FORMULARIO)

### Como o cliente mobile vota a partir das telas

A tela SELECAO de uma pauta com sessão aberta traz itens cujo `body`
contém apenas o voto (`{"voto": "SIM"}` / `{"voto": "NAO"}`). Isso é
intencional: `associadoId` e `cpf` **não** fazem parte do JSON da tela,
porque o app mobile (fora do escopo do desafio) já conhece a identidade
do associado logado e deve mesclar esses campos ao `body` antes de fazer
o POST — conforme a especificação, `associadoId` é uma String livre
fornecida pelo cliente. Ao testar o endpoint de voto diretamente (Postman,
curl), informe os três campos manualmente, por exemplo:

```bash
curl -X POST http://localhost:8080/api/v1/pautas/1/votos \
  -H 'Content-Type: application/json' \
  -d '{"associadoId": "associado-1", "cpf": "12345678900", "voto": "SIM"}'
```

## Testes

```bash
./gradlew test
```

Testes de integração usam Testcontainers (sobe um Postgres real
automaticamente — requer Docker disponível). O benchmark de performance
(`VotePerformanceTest`) roda junto com a suíte padrão e imprime os tempos
de insert/agregação para 100 mil votos no console. Resultado medido
durante o desenvolvimento: insert de 100.000 votos ≈ 3455 ms e apuração
via `GROUP BY` ≈ 17 ms.

O benchmark mede diretamente o caminho de banco (inserts em lote via
JDBC e a agregação), não um ciclo completo de requisição pela API — que
incluiria ainda o overhead HTTP/validação e, quando habilitada, a
chamada externa de verificação de CPF a cada voto.

## Teste de carga (k6)

O script `k6/vote-load.js` exercita a API rodando em container e, por meio
dela, o banco: cria uma pauta, abre uma sessão, dispara votos concorrentes
de associados distintos, confirma que um voto repetido retorna 409 e, após
o encerramento da sessão, confere que a apuração bate com o total de votos
enviados. O k6 roda em container, sem instalação local:

```bash
docker compose --profile app up -d --build   # app + Postgres
docker compose run --rm k6                   # 1000 votos, 50 VUs, sessão de 30s
docker compose run --rm -e VOTES=10000 -e VUS=200 -e SESSION_SECONDS=60 k6
```

| Variável | Padrão | Descrição |
|---|---|---|
| `VOTES` | `1000` | Total de votos enviados |
| `VUS` | `50` | Usuários virtuais concorrentes |
| `SESSION_SECONDS` | `30` | Duração da sessão; os votos precisam terminar antes dela fechar |
| `BASE_URL` | `http://app:8080` (no compose) | Endereço da API |

Com o k6 instalado localmente, `k6 run k6/vote-load.js` usa
`http://localhost:8080`. Para conferir os votos direto no banco:

```bash
docker compose exec postgres psql -U myuser -d mydatabase \
  -c "SELECT s.agenda_id, v.vote, count(*) FROM votes v JOIN voting_sessions s ON s.id = v.session_id GROUP BY 1, 2 ORDER BY 1, 2"
```

## Principais decisões de arquitetura

- **Persistência híbrida**: `Agenda`/`VotingSession` usam Spring Data JPA
  (baixo volume, mapeamento simples); `Vote` usa SQL nativo via
  `NamedParameterJdbcTemplate` — o caminho de alto volume evita overhead
  de hidratação de entidades e apura resultado com um único `GROUP BY`
  no banco.
- **Sem scheduler**: uma sessão "aberta" é derivada comparando
  `closesAt` com o relógio no momento da leitura, não um campo de status
  mutável atualizado por um job.
- **Unicidade de voto garantida no banco**: constraint única
  `votes(session_id, member_id)`, não uma checagem em memória — elimina
  race conditions sob concorrência. Somada à constraint única em
  `voting_sessions(agenda_id)` (cada pauta admite uma única sessão, nunca
  reaberta), garante um voto por associado **por pauta**.
- **Contrato de telas (Anexo 1)**: endpoints de leitura/navegação
  retornam telas FORMULARIO/SELECAO (o foco declarado da avaliação);
  endpoints de escrita retornam JSON REST convencional, mais simples de
  validar diretamente.
- **Versionamento via URI** (`/api/v1`): simples, visível no Swagger,
  fácil de fixar no cliente mobile.
- **Código em inglês, contrato em português**: pacotes, classes, tabelas e
  colunas estão em inglês; rotas, nomes de campos JSON (via
  `@JsonProperty`), valores expostos (`SIM`/`NAO`, `FORMULARIO`/`SELECAO`)
  e mensagens de erro seguem em português, conforme a especificação do
  desafio. Erros de validação também são reportados com o nome do campo
  JSON (ex.: `erros.associadoId`).
- **Open Session in View desabilitado** (`spring.jpa.open-in-view:
  false`): com ele ligado, cada voto segurava a conexão JPA da requisição
  inteira enquanto o insert JDBC pedia uma segunda conexão — sob votos
  concorrentes acima do tamanho do pool, as requisições esgotavam o pool
  (encontrado pelo teste de carga com k6, coberto por teste de
  integração).
- **Erros via `ProblemDetail`** (RFC 7807): dispensa um DTO de erro
  próprio e já é suportado nativamente pelo Spring.

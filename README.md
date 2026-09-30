# Votos — API de Votação em Assembleias Cooperativas

API REST em Spring Boot para cadastro de pautas, abertura de sessões de
votação e apuração de resultados. Toda a navegação do app mobile acontece
pelo contrato de "telas" (FORMULARIO/SELECAO) descrito no Anexo 1 do
desafio.

## Como rodar

**Pré-requisitos:** Docker e JDK 25. Se o JDK 25 não estiver instalado, o
Gradle o baixa automaticamente (toolchain resolvida pelo plugin
`foojay-resolver-convention`); o Gradle vem pelo wrapper (`./gradlew`).

```bash
./gradlew bootRun
```

A aplicação sobe o Postgres do `compose.yaml` automaticamente (Spring Boot
Docker Compose support) e injeta as credenciais. A API fica disponível em
`http://localhost:8080`, com Swagger UI em
`http://localhost:8080/swagger-ui.html`. Os dados ficam no volume nomeado
`postgres-data` e sobrevivem a `docker compose down` (só `down -v` os apaga).

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
Spring AOT + cache AOT. A otimização fica isolada no `Dockerfile`: o
`bootRun` e os testes seguem o caminho normal da JVM.

## Configuração

| Propriedade | Default | Descrição |
|---|---|---|
| `voting.public-base-url` | `http://localhost:8080` | Base das URLs de callback embutidas nas telas. Emulador Android: `http://10.0.2.2:8080`; dispositivo físico: `http://<ip-da-máquina>:8080`; publicado: `https://<domínio>` |
| `voting.session.default-duration` | `60s` | Duração da sessão quando não informada na abertura |
| `voting.member.base-url` | `https://user-info.herokuapp.com` | Base URL do serviço externo de verificação de CPF (bônus 1) |
| `voting.member.verification-enabled` | `false` | Habilita a verificação de CPF no serviço externo antes de registrar cada voto (bônus 1) |
| `spring.http.clients.connect-timeout` | `3s` | Timeout de conexão dos clientes HTTP (inclui o de verificação de CPF) |
| `spring.http.clients.read-timeout` | `5s` | Timeout de leitura dos clientes HTTP (inclui o de verificação de CPF) |
| `spring.datasource.hikari.maximum-pool-size` | `40` | Tamanho do pool de conexões, dimensionado com teste de carga (ver abaixo) |

Todas podem ser definidas por variável de ambiente, por exemplo
`VOTING_PUBLIC_BASE_URL=http://10.0.2.2:8080`.

## Contrato de telas (Anexo 1)

O app não monta URLs nem corpos de requisição: ele exibe a tela recebida e,
ao selecionar um item (SELECAO) ou apertar um botão (FORMULARIO), faz um
**POST** na `url` do item/botão enviando o seu `body` junto com os valores
dos campos de entrada preenchidos, cada um com o `id` do campo como chave.
Todo POST responde com a próxima tela.

Jornada completa, a partir de `GET /api/v1/pautas`:

| Tela | Conteúdo | Ação | Próxima tela |
|---|---|---|---|
| SELECAO "Pautas" | "Nova pauta", uma entrada por pauta (mais recentes primeiro), "Próxima página" | item | formulário de nova pauta ou detalhe da pauta |
| FORMULARIO "Nova pauta" | `INPUT_TEXTO` `titulo` e `descricao` | Cadastrar → `POST /pautas` (201 + `Location`) | abertura de sessão |
| FORMULARIO da pauta sem sessão | descrição + `INPUT_NUMERO` `duracaoSegundos` (padrão 60) | Abrir sessão → `POST /pautas/{id}/sessoes` (201) | votação |
| SELECAO da pauta com sessão aberta | "Sim" / "Não" com `body` `{"voto": "SIM"}` / `{"voto": "NAO"}` | item → `POST /pautas/{id}/votos/formulario` | identificação do associado |
| FORMULARIO de voto | `INPUT_TEXTO` `associadoId` e `cpf`; o botão carrega `{"voto": ...}` | Confirmar voto → `POST /pautas/{id}/votos` (201) | voto registrado |
| FORMULARIO "Voto registrado" | confirmação | Ver resultado / Voltar | resultado ou lista |
| FORMULARIO "Resultado" | "Sessão em andamento até ..." ou "Sim: X / Não: Y — Vencedor: ..." | Atualizar / Voltar às pautas | resultado ou lista |

Premissas e decisões:

- As URLs das telas são **absolutas**, construídas num único lugar
  (`ScreenUrls`) sobre `voting.public-base-url`, para funcionar no
  emulador, em dispositivo físico ou publicado apenas por configuração.
- Como o app faz POST em toda URL de tela, os endpoints de navegação
  (lista, detalhe, formulário de nova pauta, resultado) aceitam GET e
  POST. Na lista, POST com `pagina` navega; POST sem `pagina` cadastra.
- O associado é identificado pelo `associadoId` digitado no formulário de
  voto, como define o enunciado ("identificado por um id único"); o CPF é o
  dado usado na verificação de elegibilidade (bônus 1).
- Erros continuam respondendo `application/problem+json` (RFC 7807) com
  mensagens em português, inclusive para JSON malformado, parâmetros
  inválidos e rotas inexistentes.

Chamando a API diretamente (curl/Postman), basta enviar o que o app
enviaria:

```bash
# cria a pauta (a URL da nova pauta volta no header Location)
curl -i -X POST http://localhost:8080/api/v1/pautas \
  -H 'Content-Type: application/json' -d '{"titulo": "Reforma do estatuto"}'

# abre a sessão por 5 minutos
curl -X POST http://localhost:8080/api/v1/pautas/1/sessoes \
  -H 'Content-Type: application/json' -d '{"duracaoSegundos": 300}'

# vota (campos do formulário + body do botão)
curl -X POST http://localhost:8080/api/v1/pautas/1/votos \
  -H 'Content-Type: application/json' \
  -d '{"associadoId": "associado-1", "cpf": "12345678900", "voto": "SIM"}'

# resultado (disponível após o encerramento da sessão)
curl http://localhost:8080/api/v1/pautas/1/resultado
```

Validações de entrada: `duracaoSegundos` entre 1 e 86.400 (24 h);
`titulo` obrigatório; `associadoId` obrigatório, até 64 caracteres; `cpf`
com 11 dígitos numéricos; paginação com `pagina ≥ 0` e `tamanho` de 1 a 100.

## Verificação de CPF (bônus 1)

O serviço de demonstração indicado no desafio para verificar CPFs
(`https://user-info.herokuapp.com`) foi descontinuado — o Heroku encerrou
seu plano gratuito e o app não existe mais (responde 404 "No such app").
Por isso o bônus 1 é entregue **desabilitado por padrão**: as
funcionalidades principais funcionam imediatamente, sem depender de um
serviço externo inexistente. A integração continua completa e coberta por
testes; para ativá-la, defina `voting.member.verification-enabled=true` e
aponte `voting.member.base-url` para um serviço substituto (real ou stub)
que implemente `GET /users/{cpf}` respondendo
`{"status": "ABLE_TO_VOTE" | "UNABLE_TO_VOTE"}` (ou 404 para CPF inválido).

Com a verificação ligada:

| Resposta do serviço | Resposta da API |
|---|---|
| `ABLE_TO_VOTE` | voto segue para registro |
| `UNABLE_TO_VOTE` | 403 — associado não apto |
| 404 | 404 — CPF inválido |
| timeout, 5xx, status ou corpo inesperado | **503** — verificação indisponível, tente novamente |

A política é **fail-closed**: se a elegibilidade não pode ser confirmada,
o voto é recusado em vez de aceito sem verificação. A sessão é checada
antes da chamada externa, para não gastar a chamada com sessão encerrada.

Outras garantias em torno do CPF:

- **Vínculo com o voto**: o CPF é gravado com o voto e é único por sessão
  (`votes(session_id, cpf)`), então um CPF apto não pode ser reaproveitado
  para votar de novo sob outro `associadoId` (409).
- **Privacidade**: mensagens de erro e logs exibem o CPF mascarado
  (`*********00`); o log de indisponibilidade registra apenas o tipo do
  erro, porque a mensagem original contém a URL com o CPF.

## Endpoints

| Método | Caminho | Resposta |
|---|---|---|
| GET, POST | `/api/v1/pautas?pagina=&tamanho=` | SELECAO com as pautas |
| GET, POST | `/api/v1/pautas/formulario` | FORMULARIO de nova pauta |
| POST | `/api/v1/pautas` | 201 + `Location` + FORMULARIO de abertura de sessão |
| GET, POST | `/api/v1/pautas/{id}` | tela conforme o estado da pauta (sem sessão / aberta / encerrada) |
| POST | `/api/v1/pautas/{id}/sessoes` | 201 + SELECAO de votação |
| POST | `/api/v1/pautas/{id}/votos/formulario` | FORMULARIO de voto |
| POST | `/api/v1/pautas/{id}/votos` | 201 + FORMULARIO "Voto registrado" |
| GET, POST | `/api/v1/pautas/{id}/resultado` | FORMULARIO com o resultado ou o andamento |

Códigos de erro: 400 (validação), 403 (associado não apto), 404 (pauta,
sessão ou CPF inexistente), 409 (sessão já existente ou encerrada, voto ou
CPF repetido), 503 (verificação de CPF indisponível).

## Testes

```bash
./gradlew test              # suíte completa + relatório de cobertura
./gradlew performanceTest   # benchmark de banco com 100 mil votos (sob demanda)
```

Testes de integração usam Testcontainers (sobe um Postgres real
automaticamente — requer Docker disponível). O relatório de cobertura do
JaCoCo fica em `build/reports/jacoco/test/html/index.html` (~96% das
linhas). O relógio da aplicação é injetado (`Clock`), então os testes
avançam o tempo para encerrar sessões em vez de dormir.

Destaques da suíte:

- **Jornada pelo app**: `MobileFlowIntegrationTest` usa um simulador do app
  que só segue as telas — seleciona itens, preenche campos pelo `id` e
  aperta botões — da lista de pautas até o resultado.
- **Concorrência**: votos simultâneos do mesmo associado pelo caminho de
  produção resultam em exatamente um voto; votos simultâneos acima do
  tamanho do pool são todos aceitos.
- **Integração externa**: timeout real do cliente HTTP e mapeamento de
  cada resposta do serviço de CPF.

O `performanceTest` mede diretamente o caminho de banco (inserts em lote via
JDBC e a agregação): insert de 100.000 votos em ~4,5 s e apuração via
`GROUP BY` em ~25 ms nesta máquina. O ciclo completo pela API é medido com
o k6, abaixo.

## Teste de carga (k6)

O script `k6/vote-load.js` exercita a API rodando em container e, por meio
dela, o banco: cria uma pauta, abre uma sessão, dispara votos concorrentes
de associados distintos (cada um com seu CPF), confirma que um voto
repetido retorna 409 e, após o encerramento da sessão, confere que a
apuração bate com o total de votos enviados. O k6 roda em container, sem
instalação local:

```bash
docker compose --profile app up -d --build   # app + Postgres
docker compose run --rm k6                   # 1000 votos, 50 VUs, sessão de 30s
docker compose run --rm -e VOTES=100000 -e VUS=200 -e SESSION_SECONDS=60 k6
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

### Resultados medidos (ambiente local)

> **Atenção:** números obtidos em uma única máquina de desenvolvimento, não
> em ambiente de produção. Servem para comparar versões da aplicação entre
> si e demonstrar correção sob carga — não como estimativa de capacidade em
> produção.

**Ambiente:** notebook com Intel Core i5-1135G7 (4 núcleos / 8 threads),
16 GB de RAM, Fedora 43, Docker 29.8.1. Aplicação, Postgres e o próprio k6
rodando em containers **na mesma máquina**, disputando a mesma CPU, sem
limites de recursos por container. Verificação de CPF (bônus 1)
**desabilitada** — com ela, cada voto dependeria da latência do serviço
externo. Pool de 40 conexões e demais configurações padrão do repositório.

Versão atual (voto vinculado ao CPF, respostas em tela):

| Votos | VUs | Tempo de envio | Vazão | Latência média | p95 | Erros |
|---|---|---|---|---|---|---|
| 100.000 | 200 | ~16 s | ~6.250 votos/s | 32 ms | 55 ms | 0% |

Versão anterior (antes de vincular o CPF ao voto e de responder com telas):

| Votos | VUs | Tempo de envio | Vazão | Latência média | p95 | Erros |
|---|---|---|---|---|---|---|
| 100.000 | 200 | 12,3 s | ~8.100 votos/s | 24 ms | 41 ms | 0% |
| 500.000 | 300 | 1 min 38 s | ~5.100 votos/s | 59 ms | 123 ms | 0% |
| 1.000.000 | 200 | 2 min 03 s | ~8.150 votos/s | 24 ms | 41 ms | 0% |

A diferença de ~30% entre as versões é o custo de a correção ter mais
trabalho por voto: a segunda constraint única (`session_id, cpf`) é
verificada em cada insert, e a resposta passou a ser uma tela.

Em todas as execuções: todos os votos retornaram `201`, o voto repetido
retornou `409` e a apuração bateu com o total enviado. A persistência foi
conferida direto no Postgres — por exemplo, no teste de 1 milhão, 500.000
`YES` + 500.000 `NO` de 1.000.000 de associados distintos, gravados entre o
primeiro e o último segundo da carga.

Observações:

- A vazão é o total de votos dividido pelo tempo de envio (barra `votes`
  do k6). A taxa `/s` do resumo final do k6 é menor porque inclui a espera
  pelo encerramento da sessão antes da conferência da apuração.
- Com 1 milhão de votos (tabela já com 4,4 milhões de linhas) vazão e p95
  foram iguais aos de 100 mil: o volume acumulado não degradou o
  desempenho. A queda no teste de 500 mil veio dos 300 VUs — acima de ~200,
  as requisições extras só enfileiram no pool de conexões e disputam CPU.
- Execuções repetidas variaram alguns segundos entre si nesta máquina.

## Principais decisões de arquitetura

- **Persistência híbrida**: `Agenda`/`VotingSession` usam Spring Data JPA
  (baixo volume, mapeamento simples); `Vote` usa SQL nativo via
  `NamedParameterJdbcTemplate` — o caminho de alto volume evita overhead
  de hidratação de entidades e apura resultado com um único `GROUP BY`
  no banco.
- **Voto atômico no banco**: o voto é um único
  `INSERT ... SELECT ... WHERE closes_at > :agora`, sem checar a sessão
  antes e gravar depois. A unicidade vem de constraints (`votes(session_id,
  member_id)`, `votes(session_id, cpf)` e `voting_sessions(agenda_id)`),
  não de checagens em memória — elimina race conditions sob concorrência e
  garante um voto por associado **por pauta**.
- **Sem scheduler**: uma sessão "aberta" é derivada comparando
  `closes_at` com o relógio (`Clock` injetado) no momento da leitura, não
  um campo de status mutável atualizado por um job. As datas são
  `TIMESTAMPTZ`, então a comparação independe do fuso de quem gravou.
- **Uma sessão por pauta**: a sessão não é reaberta. Por isso a duração é
  validada na entrada (1 s a 24 h) — um valor zerado ou negativo
  inutilizaria a pauta — e só a violação da constraint de sessão única é
  traduzida para 409; outras falhas de integridade não são mascaradas.
- **Contrato de telas (Anexo 1)**: toda a jornada, inclusive as escritas,
  responde com telas, e a montagem delas fica num único componente
  (`VotingScreens`); os controllers só decidem qual tela devolver.
- **Versionamento via URI** (`/api/v1`): as telas carregam URLs absolutas
  já versionadas, e o app apenas segue as URLs que recebe. Uma v2 com
  contrato de tela diferente vive em `/api/v2`, com sua própria tela de
  entrada; o app antigo continua abrindo `/api/v1/pautas`, e todas as telas
  que recebe apontam para a v1 — ele segue funcionando enquanto a v1
  existir. Mudanças compatíveis (novos itens, campos opcionais) entram na
  própria v1; a v1 só é removida quando não houver mais versões antigas do
  app em uso.
- **Código em inglês, contrato em português**: pacotes, classes, tabelas e
  colunas estão em inglês; rotas, nomes de campos JSON (via
  `@JsonProperty`), valores expostos (`SIM`/`NAO`, `FORMULARIO`/`SELECAO`)
  e mensagens de erro seguem em português, conforme a especificação do
  desafio. Erros de validação também são reportados com o nome do campo
  JSON (ex.: `erros.associadoId`).
- **Erros via `ProblemDetail`** (RFC 7807) em todos os casos, inclusive os
  tratados pelo próprio Spring MVC; erros inesperados respondem 500 com
  mensagem genérica, sem expor detalhes internos.
- **Open Session in View desabilitado** (`spring.jpa.open-in-view:
  false`): com ele ligado, cada voto segurava a conexão JPA da requisição
  inteira enquanto o insert JDBC pedia uma segunda conexão — sob votos
  concorrentes acima do tamanho do pool, as requisições esgotavam o pool
  (encontrado pelo teste de carga com k6, coberto por teste de
  integração).
- **Caminho do voto otimizado com base em medição** (k6, 100 mil votos,
  200 VUs, média das execuções, antes do vínculo com o CPF):

  | Etapa | Tempo | p95 |
  |---|---|---|
  | Antes (sessão via JPA + insert, pool 20) | 24,4 s | ~110 ms |
  | Voto em um único `INSERT ... SELECT` condicional | 24,3 s | ~100 ms |
  | Índice redundante em `votes(session_id)` removido | 24,5 s | ~100 ms |
  | Pool redimensionado para 40 | 17,3 s | ~95 ms |
  | Log por voto em `DEBUG` | 17,3 s | ~90 ms |
  | Virtual threads (`spring.threads.virtual.enabled`) | **12,9 s** | **~45 ms** |

  O `INSERT` condicional sozinho não reduziu o tempo, mas derrubou o uso de
  CPU da aplicação de ~4 para ~1,7 núcleo — o que moveu o gargalo para a
  latência de commit por conexão e permitiu o ganho com o pool maior. A
  apuração segue usando o índice da constraint única (~21 ms para 100 mil
  votos numa tabela de 2,8 milhões).
- **Pool de conexões com 40 conexões** (`spring.datasource.hikari.maximum-pool-size`),
  dimensionado com o k6 (100 mil votos, 200 VUs, máquina de 8 CPUs). Com o
  voto feito em um único `INSERT`, o limite passa a ser a latência de commit
  por conexão (a aplicação usa ~1,7 CPU), então mais conexões em paralelo
  aumentam a vazão até o ganho virar ruído:

  | Pool | Tempo (média de 2 execuções) |
  |---|---|
  | 20 | 24,4 s |
  | **40** | **17,3 s** |
  | 60 | 16,4 s (dentro da variação entre execuções) |

  Pode ser sobrescrito com `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE`.

INSTITUTO FEDERAL DE EDUCAÇÃO, CIÊNCIA E TECNOLOGIA DO RIO GRANDE DO NORTE
Curso Superior de Tecnologia em Análise e Desenvolvimento de Sistemas
**PROJETO INTEGRADOR 2026.2**
**Desenvolvimento de Sistema Corporativo**
Especificação do trabalho  •  2º semestre de 2026

**Disciplina:** Desenvolvimento de Sistemas Corporativos
**Disciplinas vinculadas:** Teste de Software; Gerência de Projetos



**Versão do documento:** V0 — rascunho para discussão

# 1. O que é este trabalho

O Projeto Integrador de Desenvolvimento de Sistema Corporativo é uma atividade prevista no Projeto Pedagógico do Curso (Anexo VI do PPC, 30 h), obrigatória no último período. Ele é um único projeto avaliado por três disciplinas:

| **Disciplina** | **O que ela avalia no projeto** |                                                                                                                      |
| ----------------------------------------- | -------------------------------------------------------------------------------------------------------------------- |
| Desenvolvimento de Sistemas Corporativos  | O sistema em si: arquitetura, código, persistência, segurança, integração, mensageria, observabilidade e implantação |
| Teste de Software                         | A estratégia e a evidência de qualidade: plano de testes, casos, automação, cobertura, teste de carga e defeitos     |
| Gerência de Projetos                      | O processo: escopo, cronograma, riscos, papéis, comunicação e acompanhamento                                         |

**Não são três trabalhos diferentes. É um produto só, olhado por três ângulos.**
**Importante:** os entregáveis das disciplinas de Teste de Software e Gerência de Projetos descritos nas seções 9.2 e 9.3 são **uma sugestão do professor de DSC** e **dependem de aprovação dos respectivos professores**. A versão final deste documento sairá depois desse alinhamento.
De acordo com o PPC, a avaliação do projeto integrador se dá por registro contínuo das atividades e por apresentação perante banca de professores. É isso que faremos.

# 2. Objetivos

Ao final do projeto, cada estudante deve ser capaz de:

1. **Construir um sistema corporativo** usando uma plataforma de aplicações corporativas (Java + Spring), com todas as características esperadas desse tipo de sistema.
2. **Justificar decisões de arquitetura**, e não apenas implementá-las: por que este limite de módulo, por que assíncrono aqui e síncrono ali, por que este padrão.
3. **Aplicar requisitos não funcionais corporativos**: segurança, auditoria, integridade transacional, escalabilidade, disponibilidade, conformidade (LGPD) e observabilidade.
4. **Integrar sistemas** de forma síncrona (REST) e assíncrona (mensageria), lidando com falhas parciais.
5. **Comprovar com dados** que o sistema escala, por meio de teste de carga, em vez de apenas afirmar que escala.
6. **Trabalhar em equipe com disciplina de engenharia**: controle de versão, revisão de código, integração contínua, testes automatizados e rastreabilidade do trabalho de cada integrante.
7. **Entregar em produção**, não apenas na máquina do desenvolvedor.

# 3. Equipes e tema

- **Equipes de 4 estudantes.** Composição definida na Semana 1 e registrada com o professor. Mudanças posteriores só com justificativa e aprovação.
- **O tema é livre e escolhido pelo grupo**, desde que o problema justifique um sistema corporativo. Deve ser aprovado pelo professor de DSC até a Semana 3.
- Um tema é adequado quando tem: múltiplos perfis de usuário com permissões diferentes; regras de negócio não triviais; pelo menos um processo demorado ou custoso que justifique execução assíncrona; e dados que precisam de trilha de auditoria.

**Exemplos de temas viáveis** (apenas para calibrar o nível):

- Sistema de gestão de processos acadêmicos com emissão de documentos e relatórios
- Plataforma de gestão de ordens de serviço com anexos, SLA e notificações
- Sistema de compras com fluxo de aprovação e histórico imutável
- Gestão de laboratório clínico com upload de exames e geração de laudos
- Plataforma de cursos com emissão de certificados e processamento de vídeo ou imagem

**Temas que normalmente não atendem:** CRUD simples sem regras, clone de rede social, blog, agenda pessoal, lista de tarefas.

# 4. O que o sistema precisa ser

## 4.1. Arquitetura

O sistema deve ser composto por cinco elementos:

1. **Uma aplicação principal (monólito modular)** em Spring Boot, organizada em módulos coesos com fronteiras explícitas, expondo uma API REST documentada.
2. **Pelo menos um microsserviço** responsável por uma tarefa de alto custo de processamento — geração de relatório pesado, processamento de imagem ou vídeo, cálculo em lote, envio em massa, OCR, exportação de grandes volumes. O objetivo é demonstrar que essa parte pode escalar de forma independente do resto.
3. **Comunicação assíncrona por mensageria** entre a aplicação principal e o microsserviço. A aplicação principal publica o evento e responde imediatamente ao usuário; o microsserviço consome, processa e devolve o resultado por outra fila, por *callback* ou por persistência do resultado.
4. **Um frontend** que consuma a API. Tecnologia livre (React, Angular, Vue, Thymeleaf, Next.js, Flutter Web). O frontend não é o foco da avaliação de DSC: ele precisa funcionar e demonstrar os fluxos, mas a nota de DSC olha o backend.
5. **Todos os componentes empacotados em contêineres Docker.** O uso de Docker é **obrigatório**, não opcional. Aplicação principal, microsserviço, banco de dados, *broker* de mensagens, Keycloak, *object storage* e a stack de observabilidade devem subir a partir de um único docker compose up, clonando o repositório, sem instalação manual de dependências na máquina. Um projeto que só roda depois de o avaliador instalar banco, broker e JDK à mão não atende a este requisito.

## 4.2. Requisitos funcionais mínimos

- No mínimo 3 perfis de usuário com permissões distintas.
- No mínimo 1 fluxo de negócio completo que atravesse várias entidades e envolva regra de negócio real — não apenas salvar e listar.
- Upload e download de arquivos com armazenamento em *object storage*.
- Pelo menos 1 relatório gerencial exportável (PDF, Excel ou CSV).
- Trilha de auditoria: quem fez, o quê, quando, sobre qual registro.
- Pelo menos 1 rotina agendada (fechamento, expurgo, notificação ou reprocessamento).

## 4.3. Requisitos não funcionais obrigatórios

| **Requisito** | **O que precisa estar demonstrado** |                                                                                                                          |
| -------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------ |
| Segurança                                    | Autenticação e autorização via provedor de identidade externo, com papéis; endpoints protegidos; segredos fora do código |
| Integridade transacional                     | Uso consciente de @Transactional, propagação, isolamento e tratamento de concorrência                                    |
| Auditoria                                    | Registro rastreável das operações sensíveis                                                                              |
| Conformidade (LGPD)                          | Identificação dos dados pessoais tratados, base legal, política de retenção e criptografia de dados sensíveis            |
| Resiliência                                  | *Timeout*, *retry* e *circuit breaker* nas chamadas externas; *dead letter queue* e idempotência no consumo de mensagens |
| Observabilidade                              | Logs estruturados, *health checks*, métricas expostas e um painel funcionando                                            |
| Escalabilidade                               | Comprovação, por teste de carga, de que o microsserviço melhora o desempenho ao rodar em múltiplas instâncias            |

# 5. Teste de carga do microsserviço

Este é o item que fecha o argumento do projeto. Colocar mensageria e um microsserviço no desenho não prova nada sozinho; o que prova é a medição. A equipe precisa medir o comportamento do sistema sob carga e mostrar o efeito de escalar o microsserviço horizontalmente.

## 5.1. Ferramenta

Use **k6** (recomendado, script em JavaScript, fácil de versionar e rodar em contêiner) ou **Apache JMeter**. Gatling é aceito mediante justificativa. O script do teste deve estar **versionado no repositório**, junto com o código.

## 5.2. O que precisa ser medido

O cenário obrigatório é o do processamento pesado: uma carga de requisições que dispara o trabalho assíncrono, sustentada por tempo suficiente para a fila crescer.

| **MétricaPor que importa**               |                                                                                |
| ---------------------------------------- | ------------------------------------------------------------------------------ |
| Tempo de resposta da API (média e p95)   | Mostra se a assincronia realmente protegeu o usuário do custo do processamento |
| Vazão (requisições por segundo)          | Capacidade de admissão do sistema                                              |
| Taxa de erro                             | Sob carga, o sistema degrada ou quebra?                                        |
| Profundidade da fila ao longo do tempo   | Evidencia o acúmulo de trabalho pendente                                       |
| Tempo de drenagem da fila                | Quanto tempo o sistema leva para processar tudo depois que a carga cessa       |
| Consumo de CPU e memória dos contêineres | Onde está o gargalo real                                                       |

## 5.3. O experimento obrigatório

1. **Definir o critério de aceitação antes de rodar.** Exemplo: “com 50 usuários virtuais durante 5 minutos, o p95 da API de submissão deve ficar abaixo de 800 ms, a taxa de erro abaixo de 1% e a fila deve drenar em até 10 minutos.” O número é escolhido pela equipe e justificado; o que não vale é definir o critério depois de ver o resultado.
2. **Rodar a carga com 1 instância do microsserviço** e registrar todas as métricas acima.
3. **Rodar exatamente a mesma carga com 2 ou 3 instâncias** do microsserviço (docker compose up --scale ou equivalente), sem alterar mais nada.
4. **Comparar os dois cenários** e explicar o que mudou — e, tão importante quanto, o que não mudou. Se o tempo de drenagem não caiu proporcionalmente, por quê? Banco saturado? Trabalho não paralelizável? Contenção de lock? **Um teste de carga com resultado ruim, bem explicado, vale mais do que um gráfico bonito sem análise.**
5. **Correlacionar com o painel do Grafana**, mostrando as métricas da aplicação durante a execução do teste.

## 5.4. Entregável

Um relatório curto (3 a 5 páginas) contendo: descrição do cenário, configuração do ambiente de teste, critério de aceitação definido previamente, tabela comparativa dos dois cenários, gráficos, análise dos resultados e conclusão sobre o gargalo identificado.
**Cuidado:** teste de carga é feito no ambiente publicado ou em um ambiente equivalente, nunca contra serviços de terceiros. Rodar carga contra APIs externas que vocês não controlam é abuso de serviço — e, dependendo do alvo, ilegal.

# 6. Stack tecnológica obrigatória

Todas as tecnologias abaixo são obrigatórias e todas serão trabalhadas em aula antes de serem cobradas.

## 6.1. Núcleo da aplicação

| **Item** | **Tecnologia** |                                  |
| ----------------------- | -------------------------------- |
| Linguagem               | Java 21 (LTS)                    |
| Framework               | Spring Boot 3.x                  |
| Build                   | Maven (ou Gradle)                |
| Banco de dados          | PostgreSQL                       |
| Persistência            | Spring Data JPA / Hibernate      |
| Versionamento de schema | Flyway                           |
| Documentação da API     | OpenAPI / Swagger UI (springdoc) |
| Validação               | Bean Validation                  |

## 6.2. Serviços de apoio

| **Item** | **Tecnologia** |                                                                   |
| ------------------------ | ----------------------------------------------------------------- |
| Identidade e acesso      | Keycloak (OAuth 2.1 / OIDC, aplicação como *resource server* JWT) |
| Mensageria               | RabbitMQ (Kafka mediante justificativa)                           |
| Armazenamento de objetos | MinIO ou AWS S3                                                   |
| Resiliência              | Resilience4j                                                      |
| Métricas e painel        | Actuator + Micrometer + Prometheus + Grafana                      |
| Cache                    | Redis (opcional, recomendado)                                     |
| Relatórios               | JasperReports ou equivalente                                      |

## 6.3. Qualidade e entrega

| **Item** | **Tecnologia** |                                                     |
| -------------------- | --------------------------------------------------- |
| Testes unitários     | JUnit 5 + Mockito                                   |
| Testes de integração | Testcontainers                                      |
| Cobertura            | JaCoCo                                              |
| Análise estática     | SpotBugs, PMD ou SonarQube/SonarCloud               |
| **Teste de carga**   | **k6 ou Apache JMeter**                             |
| Contêineres          | Docker + Docker Compose (build *multi-stage*)       |
| CI/CD                | GitHub Actions                                      |
| Repositório          | GitHub (público ou com acesso aos três professores) |

## 6.4. Implantação

O sistema **deve estar publicado e acessível por uma URL** no dia da apresentação final. Não basta rodar em localhost.
Opções sugeridas, a escolher e justificar: Oracle Cloud Free Tier, AWS (Free Tier ou AWS Academy), Google Cloud Free Tier, Azure for Students, Render, Railway, Fly.io ou uma VPS contratada pelo grupo.
O docker compose up local também deve continuar funcionando, para permitir avaliação offline caso o ambiente de nuvem falhe no dia.

# 7. Padrões e boas práticas exigidos

Estes itens são avaliados diretamente. Não são detalhes de estilo.

## 7.1. Controle de versão

- **Git Flow** (ou *trunk-based* com *feature branches*, mediante justificativa): main (produção), develop, feature/\*, hotfix/\*.
- **Conventional Commits**: feat:, fix:, refactor:, test:, docs:, chore:.
- Nada de *commit* direto na main. Toda mudança entra por *Pull Request*.
- Todo PR precisa de revisão de pelo menos um outro integrante antes do *merge*.
- **Todos os quatro integrantes devem ter commits significativos ao longo de todo o semestre.** Um repositório em que uma pessoa fez a quase totalidade dos commits será tratado como problema de avaliação individual.

## 7.2. Código

- Separação clara de responsabilidades (*controller* / *service* / *repository*, ou *ports & adapters*).
- DTOs na fronteira da API — entidades JPA não são expostas nos *endpoints*.
- Tratamento centralizado de exceções (@RestControllerAdvice) com respostas padronizadas.
- Configuração externalizada (application.yml, *profiles*, variáveis de ambiente). Nenhum segredo versionado no repositório.
- Aplicação dos princípios SOLID e dos padrões de projeto vistos no curso, quando fizerem sentido — não force padrão onde não precisa.

## 7.3. Documentação técnica

- **README** com: descrição do sistema, arquitetura, pré-requisitos, como subir o ambiente, como rodar os testes, como rodar o teste de carga, URL do ambiente publicado e credenciais de teste.
- **ADRs** (*Architecture Decision Records*): no mínimo 5 decisões arquiteturais registradas, no formato contexto, decisão e consequências.
- **Diário de bordo**: registro contínuo (semanal ou por *sprint*) do que foi feito, por quem, quais dificuldades e quais decisões. Compartilhado com os três professores desde o início. Não vale escrever tudo na véspera — a continuidade do registro é parte do critério.

# 8. Marcos de acompanhamento

Já previstos no cronograma da disciplina. Não valem nota isolada; servem para a equipe receber devolutiva antes de valer nota.

- **Checkpoint 1 — 01/10/2026:** CRUD, persistência e testes funcionando. Demonstração para a turma.
- **Checkpoint 2 — 26/11/2026:** segurança, integração e mensageria funcionando.

# 9. Entregas avaliadas

São duas entregas, ambas avaliadas pelos professores das três disciplinas.

## 9.1. Primeira entrega — Planejamento e Arquitetura

**Data: a definir** (sugestão: 10/09/2026, Semana 6).
**Formato:** apresentação de 15 a 20 minutos e documento entregue com antecedência.
**Objetivo:** apresentar o tema, validar o escopo e defender as escolhas técnicas antes de construir.

### Conteúdo — DSC

1. **Visão geral**

- Problema, justificativa e público-alvo
- Objetivos do sistema e escopo, explicitando o que está fora
- Perfis de usuário e permissões

2. **Modelagem**

- Diagrama de casos de uso ou histórias de usuário priorizadas
- Modelo de dados inicial (DER)
- Diagrama de arquitetura do sistema (modelo C4, níveis 1 e 2, é bem-vindo)

3. **Decisões técnicas**

- Stack detalhada e justificativa de cada escolha
- Qual será o microsserviço, por que ele existe e por que ele precisa escalar separado
- Desenho do fluxo de mensageria: quem publica, quem consome, o que acontece se falhar
- Estratégia de autenticação e autorização
- Dados pessoais tratados e implicações de LGPD

4. **Organização**

- Repositório criado, estrutura inicial, docker compose subindo os serviços de infraestrutura
- Divisão de responsabilidades entre os quatro integrantes
- *Pipeline* de CI iniciado, ao menos com *build* e testes rodando no GitHub Actions

### 9.2. Sugestão — Teste de Software (sujeito a aprovação do professor)

- Plano de testes do projeto: escopo, níveis, critérios de entrada e saída, ambiente
- Estratégia de testes e definição da pirâmide adotada
- Casos de teste iniciais derivados dos requisitos
- Cenário e critério de aceitação preliminar do teste de carga descrito na seção 5

### 9.3. Sugestão — Gerência de Projetos (sujeito a aprovação do professor)

- Termo de abertura do projeto e declaração de escopo
- EAP e cronograma (metodologia ágil ou preditiva, a critério do professor)
- Matriz de responsabilidades (RACI) e plano de comunicação
- Registro de riscos com probabilidade, impacto e resposta
- Ferramenta de acompanhamento configurada (GitHub Projects, Jira ou Trello)

## 9.4. Segunda entrega — Produto desenvolvido (banca final)

**Data: 16 e 17/12/2026**, conforme o cronograma da disciplina.
**Formato:** demonstração ao vivo de 25 a 30 minutos, seguida de arguição individual de cada integrante. Todos os integrantes apresentam.

### O que deve ser demonstrado — DSC

1. **Sistema em funcionamento**

- Acesso pela URL do ambiente publicado
- Login via Keycloak com pelo menos dois perfis diferentes, mostrando o que cada um pode e não pode fazer
- Execução completa do fluxo de negócio principal

2. **Aspectos corporativos**

- Disparo de um processamento pesado, com resposta imediata ao usuário e resultado chegando depois
- Fila no *broker*, mensagem sendo consumida e o que acontece quando o consumidor falha (DLQ e reprocessamento)
- Upload e download de arquivo via MinIO ou S3
- Relatório gerencial exportado
- Trilha de auditoria consultável
- Painel de métricas no Grafana com a aplicação sob uso

3. **Escalabilidade comprovada**

- Apresentação do relatório de teste de carga
- Comparação entre 1 instância e N instâncias do microsserviço
- Identificação e explicação do gargalo encontrado

4. **Qualidade e entrega**

- *Pipeline* de CI/CD executando *build*, testes, análise e *deploy*
- Suíte de testes rodando, com relatório de cobertura
- Histórico do Git: *branches*, PRs, revisões e distribuição de commits entre os integrantes

5. **Defesa das decisões**

- Apresentação dos ADRs
- O que a equipe faria diferente hoje

6. **Documentação**

- Diário de bordo completo, README atualizado e lições aprendidas

# 10. Critérios de avaliação

A avaliação de DSC considera, nas duas entregas, os seguintes critérios:

1. **Complexidade e completude do sistema.** Profundidade das regras de negócio e dos fluxos efetivamente entregues.
2. **Arquitetura.** Modularização, clareza das fronteiras, coerência com o que foi planejado na primeira entrega e qualidade dos ADRs.
3. **Persistência e transações.** Modelagem, desempenho das consultas, uso correto de transações e tratamento de concorrência.
4. **Segurança e conformidade.** Integração com Keycloak, autorização por papel, hardening, trilha de auditoria e tratamento de dados pessoais.
5. **Integração e mensageria.** Assincronia real, resiliência, idempotência e dead letter queue.
6. **Escalabilidade comprovada.** Qualidade do teste de carga, honestidade na apresentação dos resultados e profundidade da análise do gargalo.
7. **Qualidade de código e testes.** Organização, aplicação de padrões, cobertura e testes de integração.
8. **DevOps e implantação.** CI/CD funcionando, contêineres e ambiente publicado e acessível.
9. **Processo e documentação.** Git Flow, PRs revisados, diário de bordo contínuo e README.
10. **Apresentação.** Clareza, domínio do conteúdo e capacidade de defender as decisões diante da banca.

## 10.1. Avaliação individual

A nota é da equipe, mas pode ser ajustada individualmente com base no histórico de commits e PRs no repositório, nos registros do diário de bordo e no desempenho na arguição individual da banca.
Integrante que não souber explicar as partes do sistema em que trabalhou terá a nota individual reduzida, independentemente do resultado da equipe.

# 11. Referências de apoio

- The Twelve-Factor App — https://12factor.net/pt_br/
- Spring Boot Reference Documentation — https://docs.spring.io/spring-boot/
- FOWLER, M. Patterns of Enterprise Application Architecture (catálogo) — https://martinfowler.com/eaaCatalog/
- RICHARDSON, C. Microservices Patterns — https://microservices.io/patterns/
- OWASP Top 10 — https://owasp.org/Top10/
- Keycloak Documentation — https://www.keycloak.org/documentation
- RabbitMQ Tutorials — https://www\.rabbitmq.com/tutorials
- Testcontainers for Java — https://java.testcontainers.org/
- k6 Documentation — https://grafana.com/docs/k6/latest/
- Apache JMeter User Manual — https://jmeter.apache.org/usermanual/
- Conventional Commits — https://www\.conventionalcommits.org/pt-br/
- NYGARD, M. Architecture Decision Records — https://github.com/joelparkerhenderson/architecture-decision-record
- BRASIL. Lei nº 13.709/2018 (LGPD) e orientações da ANPD — https://www\.gov.br/anpd/

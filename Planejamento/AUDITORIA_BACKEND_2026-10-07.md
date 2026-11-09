# Auditoria Backend — PROMPTUARIO CORPORATIVO
Data: 07/10/2026
Escopo: gateway, iam, patient, clinical, patient-clinical, ai, reporting, shared, compose.

CRITICO 1: backend/.env com segredos reais (JWT, GOOGLE, LLM gsk_). Revogar, gitignore, secrets manager.

CRITICO 2: gateway proxy_rabbitmq sem auth (main 361-398) -> guest:guest. Exigir ADMIN ou remover.

CRITICO 3: iam routers 267/283 _sf inexistente + event_broker inexistente + ordem errada; auth_service 494-518 logger/Cognito inexistentes. DELETE/reactivate sempre 500.

CRITICO 4: gateway defaults tudo 8001 (main 38-42), route table no import (117), var morta PATIENT_CLINICAL_URL. Sem env -> 404.

CRITICO 5: JWT_SECRET divergente (gateway change-me sem validador, iam exige diferente). Unificar validador.

CRITICO 6: iam main 72-99 seed reseta senha/ativo do admin todo restart. So criar se nao existe.

CRITICO 7: CORS * + credentials True em todos mains. Whitelist FRONTEND_URL, fechar portas internas.

CRITICO 8: Redis SPOF gateway (rate-limit/blacklist/cache sem try, startup 162). Fail-open cache, retry, healthcheck.

ALTO 9: cache GET 60s sem invalidacao (main 256-302). Invalidar no write.

ALTO 10: breaker por URL, ignora 5xx (main 307-356). Chave logica + abrir em 5xx.

ALTO 11: broker requeue True infinito + set sem limite (77-78,128-143). DLQ + retry limitado + dedup Redis.

ALTO 12: publisher/consumer while True sem timeout (28-46,80-98). Timeout + background.

ALTO 13: S3 minioadmin vs promptuario divergente. Unificar env, fail-fast 403.

ALTO 14: clinical-worker DB db-clinical errado (compose 272-279), certo db-patient-clinical. Alinhar.

ALTO 15: senha truncada 72B + strip (security 27-41, auth 50-52). Rejeitar >72, nao strip.

ALTO 16: reset URL hardcoded localhost:3000 (auth 254). Usar FRONTEND_CALLBACK_URL.

ALTO 17: llm_client 79 json_mode False vira True, Groq gpt-oss-20b cai p/ mock. Usar is not None.

ALTO 18: ai create_task solto (main 56-58, consumers 194/213). Celery/track+retry.

MEDIO: Base poluida c/ AuditLog (database 25); sessao sem rollback; duplo refresh (84/155); metricas path com ID (obs 197); /metrics sombreado (ROUTE 95); upload RAM + filename cru (patient 554); assinatura incoerente (clinical 101 vs 869); on_event + engine sem dispose; reporting @get duplo (1015/1079); docs abertos em prod; email sincrono.

CHECKLIST: 1 revogar .env+unificar secrets 2 fechar /rabbitmq+corrigir _sf/workerDB 3 gateway rotas/cache/breaker/Redis/CORS 4 broker DLQ/dedup 5 AI json_mode/task.

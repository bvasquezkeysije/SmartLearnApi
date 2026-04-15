# Group Practice V50 - Plan estructural DB + Backend

## 1) Diagnostico (estado actual)

- El flujo de fases (`open`/`review`) depende de llamadas de cliente (`/state`) y no de un motor de transicion central.
- `exam_group_session_answers` guarda respuesta por `(session_id, user_id, question_id)` pero no version de ronda; esto permite mezclar estado cuando hay cambios de ronda/tiempo en condiciones de red inestable.
- Existe doble fuente de verdad para token de sala:
  - `exam_group_room_sessions` (correcto para historico/seguridad)
  - columnas legacy en `exam_group_session_members` (`room_session_*`) que deben retirarse.
- Hay indices duplicados (`uq_exam_group_session_answers_unique` y `_v26`) en la misma clave.
- El modelo no tiene tabla de rondas con deadlines inmutables por pregunta/version.
- No existe ledger de eventos de sesion para auditar y reconstruir errores de sincronizacion.

## 2) Objetivo V50

Hacer que el backend/DB sean la fuente de verdad del flujo grupal, con transiciones atomicas y datos por ronda, para eliminar:

- desincronizacion de temporizador entre clientes,
- arrastre de respuesta de pregunta anterior,
- avance/revision inconsistentes bajo latencia.

## 3) Modelo propuesto

### 3.1 Nueva tabla: `exam_group_session_rounds`

Una fila por ronda/pregunta efectiva.

Campos clave:

- `id`
- `session_id` (FK)
- `round_number` (1..N, unico por sesion)
- `question_id` (FK)
- `phase` (`open`/`review`/`closed`)
- `open_started_at`, `open_ends_at`
- `review_started_at`, `review_ends_at`
- `close_reason` (`all_answered`, `timer_expired`, `manual_next`, `session_closed`)
- `created_at`, `updated_at`

Restricciones:

- `UNIQUE(session_id, round_number)`
- `CHECK` de coherencia temporal (`*_ends_at >= *_started_at`)

### 3.2 Evolucion de respuestas: `exam_group_session_answers`

Agregar:

- `round_number` (NOT NULL)
- `question_version` (NOT NULL) (opcional si se mantiene para compatibilidad API; internamente usar `round_number` como canonico)
- `submitted_at` (usar este campo en vez de inferir por `updated_at`)
- `is_final` (default true, para soportar cambios futuros)

Nueva unicidad:

- `UNIQUE(session_id, round_number, user_id)`

Eliminar unicidad antigua por `(session_id, user_id, question_id)` despues de backfill.

### 3.3 Limpieza de token de sala

- Mantener `exam_group_room_sessions` como unico storage de token.
- Deprecar y eliminar en fase final:
  - `exam_group_session_members.room_session_token`
  - `room_session_issued_at`
  - `room_session_expires_at`

### 3.4 Tabla de eventos: `exam_group_session_events`

Append-only para auditoria y replay.

Campos:

- `id`, `session_id`, `round_number` nullable
- `event_type`
- `payload_json`
- `created_at`
- `actor_user_id` nullable

Eventos minimos:

- `SESSION_CREATED`, `SESSION_STARTED`, `ROUND_OPENED`, `ANSWER_SUBMITTED`, `ROUND_REVIEW_STARTED`, `ROUND_CLOSED`, `SESSION_FINISHED`, `SESSION_CLOSED`.

## 4) Motor de transicion (backend)

Implementar `GroupSessionStateMachine` con transicion atomica por sesion:

- Cargar sesion + ronda activa con lock (`SELECT ... FOR UPDATE` via JPA/Pessimistic Write).
- Evaluar reglas:
  - `open -> review` si `now >= open_ends_at` o `all_connected_answered`.
  - `review -> next_open` si `now >= review_ends_at` y hay siguiente ronda.
  - `review -> finished` si no hay siguiente ronda.
- Persistir transicion + evento en la misma transaccion.

Punto clave: endpoints `state`, `answer`, `next`, `close` deben llamar primero al state machine antes de responder.

## 5) Plan de migracion (sin romper produccion)

### Fase A (expand)

- Crear `exam_group_session_rounds`.
- Agregar nuevas columnas a `exam_group_session_answers`.
- Crear `exam_group_session_events`.
- Crear nuevos indices.

### Fase B (backfill)

- Backfill de `round_number` en respuestas existentes usando `session.current_question_index` historico disponible + heuristica de timestamps.
- Si no se puede inferir exacto para historico antiguo, marcar `round_number=1` y dejar solo como historico (no afecta sesiones nuevas).

### Fase C (dual-write temporal)

- Backend escribe en modelo nuevo y mantiene compatibilidad de lectura vieja durante una ventana corta.

### Fase D (cutover)

- Lectura solo de rondas nuevas.
- Quitar indices duplicados y unicidad vieja.
- Eliminar columnas legacy de token en `exam_group_session_members`.

## 6) Riesgos actuales que V50 corrige

- Temporizador visual diferente entre clientes: se corrige leyendo siempre `open_ends_at/review_ends_at` del servidor.
- "Respuesta enviada" arrastrada: se corrige aislando por `round_number`.
- Bloqueo por usuarios desconectados: regla "all_connected_answered" se evalua sobre presencia vigente en DB.
- Estados opacos: eventos permiten diagnostico postmortem.

## 7) Orden de implementacion recomendado

1. Migracion SQL Fase A.
2. Repositorios/entidades para rondas y eventos.
3. `GroupSessionStateMachine`.
4. Refactor de `ExamGroupPracticeService` para usar rondas.
5. Limpieza final (Fase D).

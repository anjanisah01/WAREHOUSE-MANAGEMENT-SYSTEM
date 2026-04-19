# Enterprise Warehouse Management System (WMS)

Production-oriented Java WMS foundation designed around warehouse workflows:
inbound GRN, putaway, inventory control, outbound picking/shipping, analytics, and AI/voice interfaces.

## 1) Architecture

- **Controller Layer**: REST APIs for warehouse operations + auth + AI/voice.
- **Service Layer**: Workflow orchestration (`InboundService`, `PutawayService`, `OutboundService`, `AnalyticsService`).
- **Repository Layer**: JPA repositories for transactional persistence.
- **Domain Layer**: Warehouse entities and enums.
- **AI Module**: Java rule engine for low stock and expiry alerts.
- **LLM Integration Layer**: OpenAI intent parser (`OpenAiActionService`) maps NL to structured action.
- **Voice Layer**: STT/TTS adapter entry points (`VoiceService`).

## 2) Database Schema

`src/main/resources/schema.sql` includes:

- `product`, `warehouse`, `location`
- `inventory`, `lot_batch`
- `movement_history`
- `sales_order`, `picking_task`
- `sales_order_line`
- `app_user`, `role`, `user_roles`
- `alert`
- `audit_log`

## 3) Core Warehouse Modules

- **Inbound**: GRN receiving with barcode, lot/expiry capture, movement event logging.
- **Putaway**: automatic location suggestion + putaway execution update.
- **Slotting optimization**: velocity + weight-based slot scoring.
- **Inventory**: real-time quantity + batch tracking and movement history basis.
- **Cycle counting**: adjustment endpoint records audit movement.
- **Outbound**: picking task creation with SINGLE/BATCH/WAVE strategy; shipping transition.
- **FEFO allocation**: outbound reservations allocate earliest-expiry lots first.
- **Wave planning**: grouped order release in warehouse waves.
- **Task assignment**: picking task includes worker assignment.

## 4) AI + LLM + Voice

- **Rule-based AI**:
  - low stock detection
  - expiry alerts (7-day horizon)
  - replenishment suggestions
  - fast vs slow movers
  - dead stock list
- **LLM**:
  - text query -> structured action object (OpenAI API)
  - fallback Java rule parser when API key missing
- **Voice**:
  - STT input + intent parsing + TTS output hooks
  - provider-backed HTTP adapters via environment config
  - free offline STT via Vosk (`STT_PROVIDER=vosk`)
  - Google Speech-to-Text mode via `STT_PROVIDER=google`

## Auditability

- API request audit logging interceptor persists method/path/status/user into `audit_log`.

## Security

- Spring Security + JWT auth
- Role model: `ADMIN`, `MANAGER`, `WORKER`
- Endpoint protection policies in `SecurityConfig`

## Run

1. Configure PostgreSQL and update `application.yml`.
2. Add API keys to `.env` in project root.
3. Start app (it auto-loads `.env`):
   - `mvn spring-boot:run`

### Google STT setup

Set these in `.env`:

- `STT_PROVIDER=google`
- `GOOGLE_STT_API_KEY=<your_google_api_key>`
- `GOOGLE_STT_LANGUAGE=en-US`
- `GOOGLE_STT_SAMPLE_RATE=16000`
- `GOOGLE_STT_ENCODING=LINEAR16`

### Free STT (Vosk) setup

1. Download a Vosk model (for example `vosk-model-small-en-us-0.15`) from [Vosk Models](https://alphacephei.com/vosk/models).
2. Extract it locally.
3. Set these in `.env`:

- `STT_PROVIDER=vosk`
- `VOSK_MODEL_PATH=/absolute/path/to/vosk-model-small-en-us-0.15`
- `VOSK_SAMPLE_RATE=16000`

## Key APIs

- `POST /api/auth/login`
- `POST /api/wms/inbound/grn`
- `GET /api/wms/putaway/suggest/{warehouseCode}`
- `GET /api/wms/putaway/suggest/{warehouseCode}/{productId}`
- `POST /api/wms/outbound/order`
- `POST /api/wms/outbound/picking-task`
- `POST /api/wms/outbound/wave-plan`
- `POST /api/wms/outbound/ship/{orderId}`
- `GET /api/wms/inventory/stock`
- `GET /api/wms/inventory/movements`
- `POST /api/wms/inventory/cycle-count/{inventoryId}`
- `POST /api/wms/analytics/run-rules`
- `GET /api/wms/analytics/replenishment`
- `GET /api/wms/analytics/movement-classification`
- `GET /api/wms/audit/logs`
- `POST /api/wms/llm/parse-action`
- `POST /api/wms/voice/query`
- `POST /api/wms/voice/respond`

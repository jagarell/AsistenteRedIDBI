---
name: evidence-checklist-and-unified-analysis
description: 2026-09-14 session — unified the AS-IS/TO-BE analysis engine, added vision-based equipment brand/model detection, rebuilt the dynamic evidence checklist from scratch, fixed chat interaction bugs, added a dormant RUC-validation seam
metadata:
  node_type: memory
  type: project
---

Implemented (2026-09-14, branch `feature/roles-minutas-topologia`), one large
approved plan (`~/.claude/plans/spicy-imagining-falcon.md`) executed straight
through across all 3 repos, six pieces:

**1. Chat answers persisted server-side.** `evaluations.chat_answers_json`
(new column, `Evaluation.java`) filled by `ChatService.persistAnswers()`
when the chat completes. Fixed a real bug: `MinutaViewModel.loadAnalysis()`
called `/analysis` with no answers on every load of the main proposal
screen, silently overwriting the real analysis computed in Evidencias with a
near-empty one. `AnalysisService.analyze()` now falls back to the persisted
answers instead of an empty map when the caller sends none.

**2. Unified analysis engine.** `app/analysis.py`'s `compute_analysis()` now
calls `chat.proposal.generate_proposal()` internally for
score/asIsFindings/recommendations — one source of truth for both the chat's
own proposal and the `/analyze` pipeline behind the main screen's
"Recomendaciones IA" card. See [asis-tobe-proposal-engine](project_asis_tobe_engine.md)
for detail — this closed out that memory's "deferred" item.

**3. Vision-based equipment identification.** `app/vision.py` now asks
OpenAI for structured JSON (`{description, brand, model}`) for equipment
categories, not just free text. Detected brand/model persist on
`evidence_photos.detected_brand/model` (gateway) and get merged into a
`detectedEquipment` map by `AnalysisService.buildDetectedEquipment()` before
every `/analyze` call — `_compute_as_is()` in `proposal.py` adds findings
like "technician reported X, photo shows Y — verify" when they conflict.
**Not verified with a real photo** — no `OPENAI_API_KEY` configured in this
dev environment; the merge/contrast logic was verified by writing
`detected_brand`/`detected_model` directly into Postgres and confirming
`/analysis` picked them up.

**4. Dynamic evidence checklist, rebuilt from scratch.** This was the
biggest piece. Replaced the flat 7-fixed-category evidence screen with a
real Fase A (free selection: add/remove areas and equipment) → Fase B
(each item needs ≥1 photo, multi-photo allowed, "Analizar con IA" gates on
every item having a photo) flow, seeded automatically from the chat's real
answers (wifi_zones → areas, has_switches/pos_count/etc. → equipment) via a
new FastAPI endpoint `POST /evidence-checklist/seed`
(`app/chat/checklist.py::build_evidence_checklist`). New gateway package
`evidence.checklist` (`EvidenceArea`/`EvidenceEquipmentItem` JPA entities,
`EvidenceChecklistService`, `EvidenceChecklistController` — 11 endpoints
under `/api/v1/evaluations/{id}/evidence/...` + `GET .../minuta`). Android:
rewrote `EvidenceApiService`/`EvidenceRepository`/`EvidenceViewModel`/
`EvidenceFragment` to actually drive this (previously fully-written but
**zero-backend, zero-UI dead code** — see the feedback memory on stacked
dead code for how this was diagnosed). Deleted the old flat pipeline
entirely (`EvidenceUploadApiService`/`EvidenceUploadRepository`/gateway's old
`EvidenceController`) rather than leaving two competing evidence systems.
**Fully verified end-to-end by curl**: real chat → real seed (6 areas, 7
equipment items matching reported counts) → add custom area → lock → upload
photo → `GET minuta` returning 21 real conversation responses + equipment
table with photo counts. **Not walked by hand in the emulator** — the new
Fase A dialogs (add area/equipment) and Fase B capture flow have never been
tapped through on a real screen, only driven via the REST API.

**5. Chat interaction bugs (Android-only, root-caused not guessed).** In
`TechnicalChatFragment.kt`: (a) `etTextInput` was never cleared/hidden on
the quick-reply (YES_NO/CHOICE) send path — only the manual-submit path
cleared it — so text typed for an abandoned question could silently carry
into and get submitted as the answer to the *next* question. Fixed by
hiding the text row for YES_NO/CHOICE and clearing on every `currentStep`
change. (b) Quick-reply buttons gave zero visual feedback on tap (state
updates synchronously, buttons vanish same-frame) — this was very likely the
literal cause of the user's "a veces seleccionas una respuesta y no la
pinta" report, distinct from the already-fixed MULTI_SELECT chip
double-toggle bug from the prior session. Fixed with a ~120ms delay + a
solid-blue tint before calling `sendAnswer()`. **Not visually confirmed in
the emulator either** — logic reviewed and it compiles, but nobody watched
it render.

**6. RUC/business-name validation — seam only, dormant by design.** New
`RucValidationService` (gateway) with `RUC_VALIDATION_ENABLED=false` +
`RUC_VALIDATION_API_KEY=` (empty) in `.env`. Local format validation (reject
blank/too-short/all-digits/repeated-char names) is live now, wired into
`EvaluationController.createEvaluation/updateEvaluation`. The real-RUC path
targets `api.apis.net.pe/v2/sunat/ruc` (a known, documented Peru RUC-lookup
API) but the user explicitly said "no credential yet, leave it prepared but
off" — same `MAIL_ENABLED`/`OPENAI_API_KEY` seam pattern already used
elsewhere in this project. It is **not called from any real chat node**
today — the chat asks for a business *name*, not a RUC number, and adding
that question wasn't in scope. Don't assume this seam does anything until
someone (a) gets an APIs Peru token and (b) adds a RUC-number chat question
wired to `validateRuc()`.

**Why this session happened:** user wants the "Asistente IA" to genuinely
act like a network architect — after last session's AS-IS/TO-BE engine
landed, this session closed the two biggest remaining gaps: (1) that engine
being invisible on the screen technicians actually use day-to-day (fixed by
unifying), and (2) the technician's self-reported chat answers being the
*only* signal feeding the diagnosis, with no way to verify against reality
(fixed by vision-based brand/model extraction + a checklist that's actually
driven by what was really reported instead of a generic fixed list).

**How to apply:** if asked to continue this — the two explicitly-flagged
gaps are (a) get a real `OPENAI_API_KEY` and walk an equipment photo through
end-to-end to see real brand/model extraction, and (b) manually walk the new
Evidencias UI (both phases) and the chat fixes in the emulator per
[android-emulator-automation-approach](feedback_android_ui_automation.md).
Everything else in this memory was verified against the real backend, not
guessed.

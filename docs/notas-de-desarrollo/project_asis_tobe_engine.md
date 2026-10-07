---
name: asis-tobe-proposal-engine
description: Status of the AS-IS/TO-BE rule-based network-architect proposal engine (no OpenAI) added to the technical chat
metadata: 
  node_type: memory
  type: project
---

Implemented (2026-09-08/09, branch `feature/roles-minutas-topologia`): the
20-node technical chat's rule-based proposal engine (`idbi-fastapi/app/chat/`)
now separates an **AS-IS** diagnosis (`ChatProposal.asIsFindings`) from the
**TO-BE** recommendations (existing `recommendations` field), reasoning with
explicit engineering thresholds instead of just counting equipment — TIA/EIA-568
100m cable limit, AP coverage by wall material, bandwidth demand vs contracted
speed, PoE budget, POS-count redundancy risk. Chat grew from 20 to 23 nodes
(added `establishment_area_m2`, `router_to_farthest_distance_m`, `wall_type`).
All thresholds live as named constants at the top of `app/chat/proposal.py`
and `app/chat/topology.py`, explicitly commented as industry-standard
defaults, not IDBI's real catalog numbers.

**Why:** the user wants the chat's "Asistente IA" to actually reason like a
network architect / structured cabling engineer producing an AS-IS→TO-BE
narrative, but explicitly refused an OpenAI dependency for it (see
[feedback: cross-repo-planning-approach](feedback_cross_repo_planning.md) for
how this was scoped). The engine stays 100% local rules
(`CHAT_PROPOSAL_ENGINE=builtin` in idbi-fastapi's `.env`).

Threaded through additively across all 3 repos: gateway (`ChatProposal.java`,
`ProposalPdfRequest.java`, new "Diagnóstico actual (AS-IS)" PDF section in
`PdfService.java`) and Android (DTO/mapper/domain model, the chat-completion
Toast, `MinutaContentPayload`, the PDF request chain). No DB migrations
needed — the proposal is transient/passthrough end to end, never persisted
server-side.

**Verified:** full 23-question flow driven via curl through the real gateway
(JWT auth + real evaluation row) end to end, and a real PDF generated via
`/api/proposals/pdf` showing both sections in order. All 3 repos compile.
**Not yet done:** nobody has walked the 23-question flow by hand inside the
Android emulator UI to see the actual on-screen Toast — only the backend
response and PDF were checked directly.

**UPDATE 2026-09-14 — no longer deferred, now unified:** the "Recomendaciones
IA" card's pipeline (`app/analysis.py` → `/analyze` → `AnalysisResponse`) was
rewritten to call `chat.proposal.generate_proposal()` internally for
score/asIsFindings/recommendations, instead of having its own separate
scoring logic. `AnalysisResult` gained `asIsFindings: List[str]`, threaded
through the gateway (`AnalysisResponse.java`) and Android
(`AnalysisResponseDto`, shown as a second RecyclerView card
"Diagnóstico Actual (AS-IS)" on `MinutaProposalFragment`, next to the
existing "Recomendaciones IA"/TO-BE card). `analysis.py` still computes its
own 4-area score breakdown (Conectividad/Infraestructura/Equipamiento/WiFi)
for that screen's area cards, but the global score/diagnosis/recommendations
are now single-sourced from `proposal.py`. See
[unified analysis + evidence checklist rebuild](project_evidence_checklist_and_unified_analysis.md)
for the full session that did this.

**How to apply:** if asked to continue this (walk the emulator flow, or tune
the engineering constants to IDBI's real catalog), start from
`idbi-fastapi/app/chat/proposal.py` and re-verify the constants are still
sane before trusting them — they were estimates, not sourced from IDBI. The
"two separate pipelines" landmine described above is resolved; don't
re-warn about it as if it's still open.

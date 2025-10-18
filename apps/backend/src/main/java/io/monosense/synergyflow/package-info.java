/**
 * SynergyFlow - Unified ITSM+PM Platform with Intelligent Workflow Automation.
 *
 * <p>This modular monolith uses Spring Modulith to enforce module boundaries.
 * All cross-module communication happens via ApplicationEvents with transactional outbox pattern.
 *
 * <h2>Module Structure:</h2>
 * <ul>
 *   <li><b>common</b> - Shared types, events, and utilities (foundation module)</li>
 *   <li><b>incident</b> - Incident management (ITSM)</li>
 *   <li><b>change</b> - Change management (ITSM)</li>
 *   <li><b>knowledge</b> - Knowledge base</li>
 *   <li><b>task</b> - Task and project management (PM)</li>
 *   <li><b>time</b> - Time tracking</li>
 *   <li><b>audit</b> - Audit logging and decision receipts</li>
 *   <li><b>workflow</b> - Workflow orchestration (Flowable)</li>
 * </ul>
 *
 * @see org.springframework.modulith.Modulith
 */
package io.monosense.synergyflow;

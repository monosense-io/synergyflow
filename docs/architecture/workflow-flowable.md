---
title: Workflow Orchestration (Flowable 7.x)
owner: Architect
status: Draft
last_updated: 2025-09-18
links:
  - module-architecture.md
  - api-architecture.md
  - deployment-architecture.md
  - ../prd/02-itil-v4-core-features-specification.md
  - ../prd/03-system-architecture-monolithic-approach.md
---

Overview

- Flowable 7.x (BPMN/DMN/CMMN) is embedded in the `workflow` module to orchestrate
  ITIL processes (incidents, requests, changes, knowledge approvals).
- Administrative users define and version workflows using Flowable Modeler (admin only),
  then deploy to the embedded engine.

Integration Patterns

- Event‑driven process start: domain events kick off and advance BPMN processes.
- Human tasks for approvals (e.g., CAB), with assignment rules and SLAs via timers.
- External tasks for calling module services; responses correlate to process signals.

Spring Boot Setup (workflow module)

```groovy
// build.gradle.kts (workflow module)
dependencies {
  implementation("org.flowable:flowable-spring-boot-starter-process")
  implementation("org.flowable:flowable-spring-boot-starter-dmn")
  implementation("org.flowable:flowable-spring-boot-starter-security")
  testImplementation("org.flowable:flowable-spring-boot-starter-test")
}
```

```properties
# application.yml (selected)
flowable.process.async-executor-activate=true
flowable.database-schema-update=true
flowable.async-executor.default-async-job-acquire-wait-time-in-millis=5000
flowable.content.storage.root-folder=/data/flowable/content
```

Engine Configuration (example)

```java
@Configuration
public class FlowableConfig {
  @Bean
  public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> processEngineConfigurer() {
    return cfg -> {
      cfg.setAsyncExecutorActivate(true);
      cfg.setDeploymentMode("single-resource");
    };
  }
}
```

Event → Process Example

```java
@Component
@RequiredArgsConstructor
public class IncidentWorkflowStarter {
  private final RuntimeService runtimeService;

  @ApplicationModuleListener
  public void on(IncidentCreatedEvent event) {
    runtimeService.startProcessInstanceByKey(
      "incident-intake",
      Map.of(
        "incidentId", event.incidentId(),
        "severity", event.severity(),
        "teamId", event.getTeamId()
      )
    );
  }
}
```

Example BPMN (CAB Approval)

The file `architecture/workflows/cab-approval.bpmn` contains a minimal CAB flow:
start → user task (CAB approve) → gateway (approve or reject) → end.

```xml
<!-- excerpt -->
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" id="Defs" targetNamespace="http://synergyflow/cab">
  <process id="cab-approval" name="CAB Approval" isExecutable="true">
    <startEvent id="start" />
    <userTask id="cabTask" name="CAB Approval" />
    <exclusiveGateway id="gw" />
    <endEvent id="approved" />
    <endEvent id="rejected" />
    <sequenceFlow id="f1" sourceRef="start" targetRef="cabTask" />
    <sequenceFlow id="f2" sourceRef="cabTask" targetRef="gw" />
    <sequenceFlow id="f3" sourceRef="gw" targetRef="approved">
      <conditionExpression xsi:type="tFormalExpression"><![CDATA[${approved}]]></conditionExpression>
    </sequenceFlow>
    <sequenceFlow id="f4" sourceRef="gw" targetRef="rejected">
      <conditionExpression xsi:type="tFormalExpression"><![CDATA[${!approved}]]></conditionExpression>
    </sequenceFlow>
  </process>
</definitions>
```

Deployment & Security (Admin Modeling)

- Deploy Flowable Modeler/IDM for admin users only (SSO protected).
- Place behind Envoy Gateway with external auth or JWT validation.
- Use GitOps to version BPMN/DMN artifacts alongside promotion to environments.

Operations & Observability

- Monitor job executor, async job queues, process instance counts and durations.
- Emit Micrometer metrics per process (starts, completions, timeouts, retries).
- Retention policy for historic process data aligned with compliance.

Testing

- Use Flowable Spring test support to assert path conditions and timers.
- Simulate domain events to validate event‑driven process initiation.

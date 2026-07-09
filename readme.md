# Scheduler

This repository contains a Dockerized task scheduler application with a React frontend, Spring Boot microservices, PostgreSQL databases, JWT authentication, task management, saved locations, scheduler onboarding, Scheduling Profiles with Planning Windows, schedule generation, and a morning briefing for reviewing the generated day plan.

## Current Hand-In Build

Start the full application with Docker Desktop running:

```powershell
docker compose up --build
```

Then open:

```text
http://localhost:5173
```

Use the Register form to create a fresh account, then log in and try the main workflow:

1. Create tasks.
2. Add saved locations.
3. Configure Scheduler Preferences: category priority, normal planning time, Planning Windows, and pauses.
4. Open Home to review the morning briefing and confirm today's generated day plan.
5. Use Schedule to correct the detailed timeline, skip tasks for today if needed, reserve `Free time`, mark scheduled tasks complete, and preview the proposed week.

For evaluator-facing instructions, see [HAND_IN.md](HAND_IN.md).
For release notes and known limitations, see [RELEASE_NOTES.md](RELEASE_NOTES.md).
For troubleshooting local startup, see [LOCAL_STARTUP.md](LOCAL_STARTUP.md).
For scheduler reliability concepts, tests, and seed scenarios, see [docs/scheduler-reliability.md](docs/scheduler-reliability.md).

Phase 3 adds a database-backed in-app notification MVP; see [docs/notifications-mvp.md](docs/notifications-mvp.md).

Phase 4a adds routing feasibility checks and travel-time warnings; see [docs/routing-feasibility-mvp.md](docs/routing-feasibility-mvp.md).

Phase 5a adds mobile daily-flow and Quick Add Task behavior; see [docs/mobile-daily-flow.md](docs/mobile-daily-flow.md).

## Verification

```powershell
mvn test
```

```powershell
cd web
pnpm run build
```

```powershell
docker compose up --build
```

## Implemented MVP Status

The current build supports:

- Registration/login with JWT authentication.
- Task creation for fixed, flexible, recurring-style, multi-session, and project-style workflows.
- Saved locations with membership-based limits.
- Scheduler onboarding for category priority ranking, normal planning time, optional Planning Window setup, and pause duration.
- Scheduling Profiles that group one or more Planning Windows. A Scheduling Profile can be activated, edited, or deleted from Settings.
- Planning Windows with a main focus category, also-allowed categories, strict/preferred behavior, urgent override, and target placement mode.
- Schedule generation with fixed tasks placed first, flexible tasks guided by Planning Windows, category priority, task priority, deadlines, and pauses.
- Scheduler reliability support with deterministic test time, category importance, effective priority, urgent override checks based on effective priority, lightweight unscheduled-task reports, and backend/test-visible scheduling explanations.
- Home daily dashboard with morning briefing, next task, today-at-a-glance preview, and quick links.
- Schedule correction workspace with the detailed Today timeline plus Week calendar view, category filters, and automatic visible time range.
- Settings area for profile, Scheduler Preferences, Scheduling Profiles, saved locations, and notification placeholder settings.
- Backend-persisted `DayPlan` and `DayPlanItem` records in the scheduling service.
- Morning briefing actions backed by API calls:
  - fixed tasks can be kept, opened, or marked completed;
  - flexible tasks can be kept, opened, marked completed, skipped for today, or replaced by a fixed `Free time` block;
  - future-only actions such as move, shorten, and replace are shown as disabled until occurrence/allocation persistence exists.
- Confirmed day plans persist per customer and date in PostgreSQL, including chronological scheduled items, free-gap minutes, tight spots, and a plan signature.
- Skip-today decisions persist in the backend per customer/date. A skipped task stays pending in the task backend, but it is excluded from regeneration for that same day.
- `Keep this time free` creates a normal fixed `Free time` task through task-service so regenerated schedules keep the slot blocked.

Notifications are not a completed user-facing feature yet. The notification service is part of the Docker stack and tasks have `reminderDate` fields, but there is no finished reminder delivery UI, WebSocket push flow, or in-app notification inbox.

## Important Current Rules

- Fixed tasks block real occupied time.
- Scheduling Profiles hold Planning Windows; the active Scheduling Profile guides flexible task placement.
- Planning Windows guide flexible task placement; they do not make whole categories fixed.
- A strict Planning Window permits the main focus and also-allowed categories, plus unrelated priority-5 tasks when urgent override is enabled.
- A preferred Planning Window tries the main focus and also-allowed categories first, then allows other suitable tasks when needed.
- Target placement controls fallback behavior:
  - `ALLOW_ELSEWHERE`: target categories may also be scheduled outside this Planning Window.
  - `PREFER_INSIDE_WINDOW`: target categories are currently allowed elsewhere, with a future TODO to rank outside-window placement lower.
  - `KEEP_INSIDE_WINDOW`: target categories are excluded from fallback/default segments.
- Weekday-only Planning Windows only constrain those weekdays; they no longer block the same category on unrelated days.
- Category priority affects flexible task ordering, not whether a task is fixed.
- Fixedness belongs to the individual task type. Zones and categories do not convert flexible tasks into fixed tasks.
- "Skip today" is a day-plan decision. It does not delete, complete, or globally cancel the backend task.
- The scheduler is heuristic-based, not a full mathematical constraint optimizer.

## Daily Workflow And Confirmed Plans

Home automatically loads today's persisted day plan. If none exists, it generates and persists one. Home is the main daily review surface: it shows the morning briefing, next upcoming task, and a compact today-at-a-glance preview.

Schedule is the correction and planning workspace. It still loads the same persisted day plan, but it shows a compact status banner instead of duplicating the full morning briefing. The Today view contains the chronological timeline and task actions. The Week view contains the proposed weekly calendar, category filters, and schedule statistics.

The day plan is displayed chronologically. Each scheduled item has a dropdown with actions that match the task type. Skipping a flexible task updates the backend day-plan item as `SKIPPED`, keeps the underlying task pending, and prevents that task from reappearing when the same day is regenerated.

`Keep this time free` creates a normal fixed task named `Free time` for the selected slot. This blocks the slot during future schedule generation and survives reloads because it is persisted by task-service.

Confirmed day plans are now backend records owned by scheduling-service. Browser storage is no longer the primary persistence path for confirmations, skip-today decisions, completion updates, or `Free time` reservations.

## Scheduling Profiles And Planning Windows

The old user-facing "Zone" settings have been replaced by clearer language:

- A **Scheduling Profile** is the active scheduling setup for a customer, such as Regular, Exam Phase, Vacation, or Recovery Mode.
- A **Default flexible planning window** is the broad fallback time range where flexible tasks may be considered.
- A **Planning Window** is a guided rule for part of the week, such as Work mornings, Sport evenings, Duties on Saturday, or Recovery time.

Planning Windows collect:

- title;
- days and start/end time;
- main focus category;
- also-allowed categories;
- Preferred or Strict behavior;
- optional Strict-only urgent override;
- target placement mode.

The normal UI intentionally hides raw implementation details such as `dayMask`, `allowedCategories`, `excludedCategories`, `priorityOverrideThreshold`, `behaviorMode`, and backend "zone definition" terminology. Existing backend endpoints remain under `/customers/zones/**` for compatibility.

Current scheduler behavior:

- Fixed tasks still block real occupied time first.
- Preferred Planning Windows guide flexible placement but no longer trap their target category inside the window.
- Strict Planning Windows reject unrelated flexible tasks unless urgent override is enabled and the task has priority 5.
- `KEEP_INSIDE_WINDOW` excludes target categories from fallback/default segments.
- `ALLOW_ELSEWHERE` and the Phase 1 version of `PREFER_INSIDE_WINDOW` allow target categories to use fallback/default segments.

## Current Limitations

- Day-plan persistence is implemented in scheduling-service, but there is not yet a separate reviewed/evening-check workflow.
- Regeneration can report that a plan differs from the confirmed signature, but the UI still keeps the review flow intentionally simple.
- Recurrence is stored as text. There is no durable occurrence model yet, so true per-occurrence skip/move/complete behavior is future work.
- Move-to-another-day, shorten-duration, and replace-task actions are visible as future actions but disabled in the MVP UI.
- Notifications are not a completed user-facing feature yet. The notification service is part of the Docker stack and tasks have `reminderDate` fields, but there is no finished reminder delivery UI, WebSocket push flow, or in-app notification inbox.
- The Planning Window wizard is the current editing surface. The older backend route and class names still use "zone" internally for compatibility.
- `PREFER_INSIDE_WINDOW` does not yet lower the ranking of outside-window placement; it behaves like `ALLOW_ELSEWHERE` for Phase 1 and is prepared for future refinement.
- Routing uses an internal distance estimate, not a live external maps API.
- The scheduler is heuristic-based, not a full mathematical constraint optimizer.

## Evening Check Options

Possible ways to implement the evening check on top of backend day plans:

- Next-login review: when the user opens the app in the evening or the next day, show yesterday/today's confirmed plan and ask about tasks that are still pending.
- Scheduled in-app reminder: use the notification service to create an evening review reminder that opens a review screen.
- Calendar-based checklist: store confirmed plans server-side, compare them with completed task statuses, and show only unresolved scheduled blocks.
- Lightweight MVP: reuse the persisted backend day plan and show unresolved `PLANNED`/`KEPT` items in an evening review panel.

Recommended next step: build the evening review on top of persisted day plans so the user can confirm what was completed without manually marking everything during the day.

## Recommended Next Technical Steps

- Add a recurrence occurrence model so daily/weekly recurring tasks can be skipped, moved, or completed per occurrence.
- Implement the evening review screen using persisted day plans and task completion status.
- Productize notifications with an in-app inbox or push/WebSocket delivery.
- Add browser-level tests for schedule generation, briefing actions, skip-today behavior, and `Free time` reservation.
- Improve `PREFER_INSIDE_WINDOW` ranking so outside-window placements are allowed but less attractive than matching Planning Window placements.

## Original Planning Notes

The remaining sections are original planning notes and should be read as background/future direction, not as an exact list of completed product features.

---

## 1. High-Level Overview

Your application aims to provide customers with a calendar that not only records tasks and appointments but also dynamically rearranges “flexible” tasks based on real-time data (e.g., delays, travel time, priorities). The system must handle both fixed (non-negotiable) and flexible (reschedulable) tasks, support route optimization, and push real-time notifications to clients (mobile and web).

**Key Responsibilities:**
- **Task Management:** CRUD operations for tasks and metadata management.
- **Dynamic Scheduling:** Intelligent task reordering based on customer state, priorities, and external factors.
- **Real-time Notifications:** Inform customers of changes, conflicts, or reminders.
- **Route Optimization:** Optimize task sequences based on travel times and geographic distances.
- **Frontend Integration:** Provide responsive interfaces on both web and mobile.

---

## 2. System Architecture

### A. **Microservices Architecture**
Design your system as several loosely coupled services. This not only helps in scaling and maintenance but also allows you to update parts of the system independently.

**Potential Microservices:**
1. **Task Management Service:**
    - Handles creation, reading, updating, and deletion of tasks.
    - Stores metadata like task type (fixed, flexible, recurring), priorities, and dependencies.
    - **Technologies:** Java + Spring Boot, PostgreSQL.

2. **Scheduling & Optimization Service:**
    - Implements dynamic scheduling algorithms.
    - Recalculates schedules in response to real-time events.
    - Integrates with a routing API for travel time/distance calculations.
    - **Technologies:** Java + Spring Boot, Hazelcast (for in-memory task state), multithreading for concurrent scheduling tasks.

3. **Notification Service:**
    - Manages real-time alerts and notifications.
    - Uses message brokers for asynchronous event handling.
    - **Technologies:** Kafka (messaging), WebSocket (for client push notifications).

4. **Routing Integration Service:**
    - Communicates with external route optimization APIs (e.g., Google Maps API) to fetch travel distances and times.
    - Can be integrated as a part of the Scheduling Service or stand-alone if the routing logic is complex.
    - **Technologies:** Java + Spring Boot, REST clients for API integration.

5. **Customer Interface Gateway:**
    - Acts as the bridge between frontend clients and backend services.
    - Handles API routing, load balancing, and security/authentication (if needed).
    - **Technologies:** API Gateway frameworks (such as Spring Cloud Gateway or similar).

### B. **Data & Caching Layers**
- **Primary Storage:**
    - PostgreSQL for task persistence and customer data.
- **Caching/Shared State:**
    - Hazelcast to cache frequently accessed schedules and share the state among services, especially for real-time scheduling adjustments.

### C. **Communication Between Services**
- **Synchronous:**
    - gRPC/REST for inter-service communication when immediate responses are needed (e.g., fetching task details).
- **Asynchronous:**
    - Kafka for event-driven communication (e.g., when a task is updated, a “schedule re-calc” event is published).

### D. **Containerization & Deployment**
- **Local Development:**
    - Use Docker Compose for orchestrating multi-container environments.
- **Production:**
    - Docker Swarm (or consider Kubernetes for more advanced orchestration if scaling further) for load balancing, scaling, and service discovery.

---

## 3. Detailed Component Breakdown

### A. **Task Management Service**
- **Features:**
    - CRUD operations for various task types.
    - Handling recurring tasks (e.g., daily medications).
    - Categorization of tasks (Appointments, Medications, Lectures, etc.).
- **Database Schema Considerations:**
    - Tables for tasks, customers, and schedules.
    - Fields for task properties: type, priority, dependencies, estimated duration, fixed/flexible flag, and metadata (location, recurrence).

### B. **Dynamic Scheduling & Optimization**
- **Algorithm Considerations:**
    - Use a scheduling algorithm that considers fixed time slots and flexible tasks.
    - Incorporate travel times (fetched via routing service) and real-time updates.
    - Prioritize tasks based on urgency, proximity, and deadlines.
    - Possibly use heuristics or optimization libraries (e.g., solving a variant of the Traveling Salesman Problem with time windows) for route optimization.
- **Reactivity:**
    - Implement triggers (via event messaging) that re-run the scheduler when:
        - A task is completed.
        - A task takes longer than planned.
        - New data (like waking up later than expected) is received.
- **Concurrency:**
    - Use multithreading for recalculating the schedule while keeping the system responsive.

### C. **Real-time Notifications**
- **Features:**
    - Alert customers of upcoming tasks, rescheduled activities, or conflicts.
    - Use Kafka for publishing events and WebSocket to push real-time notifications.
- **Integration:**
    - Notification Service subscribes to events from Task and Scheduling Services.
    - Clients (mobile/web) maintain persistent WebSocket connections to receive push messages.

### D. **Routing Integration**
- **Features:**
    - API integration to fetch real-time travel data.
    - Provide endpoints that return route optimization results.
- **Considerations:**
    - Handle API limits and caching of route information where possible.
    - Fallback strategies if external API is unavailable.

### E. **Frontend Integration**
- **Web Interface:**
    - Although you’re new to React, consider starting with tutorials and small projects to build up your proficiency.
    - Alternatives: Frameworks like Vue.js or Angular if you find them easier, but React has a vast ecosystem.
- **Mobile Interface:**
    - React Native is a strong candidate for cross-platform development.
- **Communication:**
    - Use REST or gRPC to communicate with your backend.
    - Implement WebSocket clients for real-time notifications.

---

## 4. Project Planning & Milestones

### Phase 1: **Requirements & Prototyping**
- **Duration:** 2–3 weeks
- **Activities:**
    - Finalize requirements and use cases.
    - Sketch UI wireframes for both mobile and web.
    - Define data models and API contracts (e.g., using Swagger/OpenAPI).
    - Create a proof-of-concept for a core feature (e.g., a simple task CRUD service).

### Phase 2: **Backend Services Development**
- **Duration:** 6–8 weeks
- **Activities:**
    - Develop the Task Management Service with a PostgreSQL database.
    - Implement the Scheduling & Optimization Service:
        - Develop a basic scheduling algorithm.
        - Integrate with Hazelcast for in-memory caching.
    - Build the Notification Service and integrate Kafka for asynchronous events.
    - Set up the Routing Integration Service and connect with an external API.
    - Write unit and integration tests for each service.

### Phase 3: **Containerization & DevOps Setup**
- **Duration:** 2–3 weeks (can overlap with backend development)
- **Activities:**
    - Containerize each microservice using Docker.
    - Set up Docker Compose for local development/testing.
    - Prepare deployment scripts/configuration for Docker Swarm (or your chosen orchestration tool).
    - Implement CI/CD pipelines for automated testing and deployment.

### Phase 4: **Frontend Development**
- **Duration:** 6–8 weeks
- **Activities:**
    - Begin with a simple web client; use React (or your chosen framework) to build a dashboard.
    - Develop core features: displaying tasks, notifications, and schedule adjustments.
    - Build mobile interfaces using React Native (or an alternative) with the same core functionalities.
    - Integrate with backend via REST/gRPC and WebSocket for notifications.
    - Perform usability testing and refine UI/UX.

### Phase 5: **Integration & Testing**
- **Duration:** 3–4 weeks
- **Activities:**
    - End-to-end testing of the integrated system.
    - Simulate real-world scenarios (delays, task overruns, route changes) to test dynamic scheduling.
    - Performance and load testing.
    - Fix bugs and optimize performance.

### Phase 6: **Beta Release & Iteration**
- **Duration:** 4–6 weeks
- **Activities:**
    - Launch a beta version with a limited customer base.
    - Gather customer feedback and usage data.
    - Iterate on features and scheduling algorithms based on real-world usage.
    - Prepare for a broader production release.

---

## 5. Additional Considerations

### A. **Learning & Tooling for Frontend**
- **React Learning Resources:**
    - Consider online courses, documentation, and building small demo apps to build your confidence.
- **Alternatives:**
    - If React feels overwhelming, Vue.js is a more beginner-friendly option with similar capabilities.
- **Integration Tips:**
    - Use libraries like Redux (or Context API) for state management.
    - Leverage component libraries (e.g., Material-UI) to speed up development.

### B. **Dynamic Scheduling Algorithm**
- **Algorithm Design:**
    - Start with a basic rule-based system (e.g., if a task overruns, shift all subsequent flexible tasks).
    - Gradually add complexity (considering travel times, priority weighting, deadlines).
    - Explore existing libraries for scheduling or optimization if needed.
- **Simulation & Testing:**
    - Build simulation tools to test various scenarios (e.g., delay propagation, travel time variations).
    - Validate that rescheduling logic aligns with customer expectations and constraints.

### C. **Scalability & Fault Tolerance**
- **Service Isolation:**
    - Ensure each microservice has clear API boundaries.
    - Use circuit breakers or fallback mechanisms when external dependencies (like routing APIs) fail.
- **Load Balancing:**
    - Utilize Docker Swarm’s built-in load balancing features, or consider Kubernetes if the application grows.

---

## 6. Final Thoughts

This plan outlines the high-level design and development phases for your dynamic human task scheduler. By breaking the project into manageable microservices and following iterative development cycles, you can focus on core functionalities first and gradually introduce more advanced features like dynamic rescheduling and route optimization.

Remember to:
- Keep the interfaces between services well-documented.
- Implement robust logging and monitoring to quickly diagnose issues, especially as you add real-time dynamic features.
- Engage in frequent testing and customer feedback loops, particularly when handling scheduling edge cases.

Feel free to ask for further details on any section or assistance with specific technical challenges as you progress with your project. Happy coding!

# **Customer-Service and Zones**

Below is a **conceptual and implementation outline** for managing **“zones”**—time-based scheduling constraints—for your customers. We’ll walk through the **data model**, possible **entity classes**, and some **best practices** for handling multiple zone configurations (e.g., “Regular times,” “Holiday times,” etc.). We’ll also discuss how these zones might interact with your scheduling logic.

---

## 1. High-Level Concept

A **zone** is essentially a **customer-defined time window** (e.g., “7 AM – 9 AM on weekdays”) with **rules** about which tasks are allowed, forbidden, or conditionally allowed based on **categories** and/or **minimum priority**.

Common examples:

- **General scheduling hours**: “From 7 AM to 9 PM, flexible tasks can be placed—unless it’s Sunday.”
- **Sport zone**: “Sport tasks are only allowed between 7 AM and 8:30 AM or 5 PM and 9 PM.”
- **Work zone**: “Working hours are 9 AM to 5 PM on weekdays.”
- **Recreational zone**: “Between 5 PM and 7 PM, no tasks should be scheduled except priority >= 3.”

You also want **multiple** zone configurations (e.g., “Regular times” vs. “Holidays”), so a customer can **switch** or the system can apply different sets of constraints.

---

## 2. Data Model & Entities

### 2.1 `ZoneConfiguration` (AKA “CustomerScheduleConfig”)

- **Represents** one entire configuration (e.g., “Regular,” “Holiday,” “Vacation”).
- **Owned** by a single customer (assuming multi-customer app).
- Contains a **name** or **title** so the customer can identify it.
- Possibly contains metadata like “active” flag if the customer can only have one active configuration at a time.

**Example**:

```java
@Entity
@Table(name = "zone_configurations")
public class ZoneConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;  // "Regular times", "Holiday times", etc.

    // Example: If you track which config is the default or currently active
    private boolean active = false;

    // If you have a Customer entity
    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer owner;

    // One-to-many relationship to individual zone definitions
    @OneToMany(mappedBy = "zoneConfiguration", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ZoneDefinition> zones = new HashSet<>();

    // Constructors, getters, setters
}
```

### 2.2 `ZoneDefinition`

Each `ZoneDefinition` describes **one** zone/time window plus its rules:

1. **Title** / **name**: e.g., “Sport zone,” “Work zone.”
2. **Time range**: start time, end time (in daily format, e.g., `LocalTime`), or full `LocalDateTime` if needed.
3. **Weekday** or day-of-week pattern: e.g., “Monday,” “Sunday,” or multiple days. Could be an enum or a bitmask for multiple days.
4. **Allowed categories**: e.g., `Set<String>` or an **enum** set. If empty, maybe it means all categories.
5. **Minimum priority override**: tasks with priority >= X can override the normal restriction.
6. Possibly a **rule type** (ALLOWED, FORBIDDEN, etc.) or “No tasks except priority >= Y.”

**Example**:

```java
@Entity
@Table(name = "zone_definitions")
public class ZoneDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;  // e.g. "Sport zone"

    // e.g. 07:00, 09:00
    private LocalTime startTime;
    private LocalTime endTime;

    // e.g. "MONDAY", "WEDNESDAY"
    // or we can store an integer (1=Mon, 2=Tue, etc.)
    @Enumerated(EnumType.STRING)
    private DayOfWeek dayOfWeek;

    // If you allow multiple days in one definition, you might store a list or bitmask
    // For simplicity: one dayOfWeek per row.

    // Categories allowed or restricted
    // Could store these in a separate table as a ManyToMany, or a comma-separated string
    @ElementCollection
    @CollectionTable(name = "zone_categories", joinColumns = @JoinColumn(name = "zone_definition_id"))
    @Column(name = "category")
    private Set<String> allowedCategories = new HashSet<>();

    // If a task has priority >= this, it can override the restriction
    private Integer priorityOverrideThreshold;

    // Possibly store an enum or boolean for "allowAll" vs. "denyAll"
    // e.g., ALLOW_ONLY_LISTED_CATEGORIES, DENY_LISTED_CATEGORIES, etc.
    // or a simpler approach is: "no tasks except priority >= X"

    @ManyToOne
    @JoinColumn(name = "zone_config_id", nullable = false)
    private ZoneConfiguration zoneConfiguration;

    // Constructors, getters, setters
}
```

**Additional fields** you might add:

- `excludeCategories` if you want a difference between “allowed” and “excluded.”
- `allowOnSunday` or a more general **flag** “appliesToWeekends” if you want advanced logic.
- `allDay` or `noTasksAllowed` if it’s a blanket restriction (like “no tasks on Sundays at all”).

---

## 3. Creating Multiple Configurations

### 3.1 Customer Flow

- **Customer** goes to a “Zone Configurations” screen, clicks “New Configuration,” enters a name (“Holiday Mode”).
- They add multiple `ZoneDefinition`s for that config. Example: “In holiday mode, no tasks on Sundays,” “Working hours are 10 AM – 3 PM,” etc.
- **Customer** can select which configuration is “active.” The scheduling engine references the active configuration to see which zones apply.

### 3.2 Database Representation

You’ll have:
- **`zone_configurations`** table with columns: `id`, `name`, `active`, `customer_id`, etc.
- **`zone_definitions`** table with columns: `id`, `title`, `start_time`, `end_time`, `day_of_week`, `priority_override_threshold`, `zone_config_id`, etc.
- Possibly a `zone_categories` join table (or an `element_collection`) for storing the categories.

---

## 4. Interacting with Your Scheduler

**Pseudocode** for how your scheduling logic might use this:

1. **Retrieve** the customer’s **active** `ZoneConfiguration` (or the one they selected).
2. For each day/hour you want to schedule a task, check if **any** `ZoneDefinition` says it’s disallowed or allowed under certain conditions.
3. If the task’s category is not in the allowed categories, check if `task.priority >= zoneDefinition.getPriorityOverrideThreshold()`. If yes, you can schedule anyway. If not, skip that slot.
4. If the day is Sunday and there’s a zone that forbids tasks unless priority >= X, apply that logic.

**Example** check:

```java
boolean canSchedule(Task t, LocalDateTime slot, ZoneConfiguration config) {
    DayOfWeek day = slot.toLocalDate().getDayOfWeek();
    LocalTime time = slot.toLocalTime();

    // Iterate over relevant zone definitions for that day
    for (ZoneDefinition zone : config.getZones()) {
        if (zone.getDayOfWeek() == day) {
            // Check time range
            if (time.isAfter(zone.getStartTime()) && time.isBefore(zone.getEndTime())) {
                // Now check categories and priority override
                if (!zone.getAllowedCategories().contains(t.getCategory())) {
                    // If the task priority is below threshold, deny
                    if (t.getPriority() < zone.getPriorityOverrideThreshold()) {
                        return false; 
                    }
                }
            }
        }
    }
    // If no zone forbids it, we can schedule
    return true;
}
```

> This is a **very simplified** example. Real logic can be more sophisticated (e.g., multiple overlapping zones, more advanced rules).

---

## 5. Implementation Steps

1. **Entities**
    - Create `ZoneConfiguration` and `ZoneDefinition` (as shown above).
    - Ensure you store them in a repository (`ZoneConfigRepository`, `ZoneDefinitionRepository`) or manage them through the same service.

2. **Customer Flows**
    - Provide endpoints:
        - `POST /zone-configs` to create a new config.
        - `POST /zone-configs/{id}/zones` to add new `ZoneDefinition`.
        - `PUT /zone-configs/{id}/active` to mark one config as active.

3. **In the Scheduler**
    - When scheduling tasks, look up the customer’s **active** configuration.
    - For each potential slot, check the zone definitions (like the example above).
    - If the task category/priority meets or exceeds zone rules, schedule it. Otherwise, skip or try another slot.

4. **Multiple Zones**
    - If you have multiple zones that overlap, decide how to handle conflicts. For instance, do you take the **most restrictive** rule? Or do you allow scheduling if *any* zone is OK?

5. **Validation**
    - For each `ZoneDefinition`, ensure `startTime < endTime`.
    - Some customers may define contradictory zones—how to handle that is up to your business logic.

---

## 6. Possible Extensions

1. **Multiple days in one definition**: Instead of having a single `DayOfWeek`, you might store a list or a “days mask” (e.g., Monday + Wednesday).
2. **Category Approach**: Instead of `Set<String> allowedCategories`, you might store an enum `Category { SPORT, WORK, HEALTH, ...}` and use an `EnumSet<Category>` or a `@ManyToMany` to a `Category` table.
3. **Time Zone**: If your customer can be in different time zones, store the zone in `ZoneConfiguration` so your scheduling logic uses the correct offsets.
4. **Recurring or special date ranges**: e.g., “Holiday config is only valid from Dec 20–Jan 5.” Then your scheduler automatically picks it if the current date is in that range.
5. **Front-End**: Provide a UI for customers to define these zones easily (drag-and-drop on a timeline, pick categories from a dropdown, etc.).

---

## 7. Summary

- **Data Model**: Two main entities—`ZoneConfiguration` for grouping, `ZoneDefinition` for each time window/rule.
- **Customer** can create multiple configurations (e.g., “Regular,” “Holiday”). Each config has multiple zone definitions.
- **Each ZoneDefinition**: dayOfWeek, startTime, endTime, allowedCategories, priorityOverrideThreshold, etc.
- **Scheduling Logic**: The scheduler consults the customer’s active zone configuration and checks each zone’s constraints.
- **Implementation**: Provide REST endpoints or services to manage creation, editing, and activation of these zones. The scheduling microservice or logic will read these definitions to enforce constraints.

This design gives you **flexibility** for future expansions (like more complex rules, multiple day-of-week patterns, or different fallback priorities). By storing each set of constraints in a `ZoneConfiguration`, you can easily switch the customer’s scheduling regime from “Work Week” to “Vacation” with minimal changes in your scheduling code.





services:

# ========== Databases ==========

postgres-task:
image: postgres:latest
container_name: postgres-task
environment:
POSTGRES_DB: task_service_db
POSTGRES_CUSTOMER: customer
POSTGRES_PASSWORD: password
ports:
- "5433:5432"  # Expose externally on 5433
volumes:
# Optional: If you have init scripts or want to persist data
# - ./database/task_service.sql:/docker-entrypoint-initdb.d/init.sql
# Or named volumes:
- task_db_data:/var/lib/postgresql/data
restart: unless-stopped

postgres-customer:
image: postgres:latest
container_name: postgres-customer
environment:
POSTGRES_DB: customer_service_db
POSTGRES_CUSTOMER: customer
POSTGRES_PASSWORD: password
ports:
- "5435:5432"
volumes:
- customer_db_data:/var/lib/postgresql/data
restart: unless-stopped

postgres-notification:
image: postgres:latest
container_name: postgres-notification
environment:
POSTGRES_DB: notification_service_db
POSTGRES_CUSTOMER: customer
POSTGRES_PASSWORD: password
ports:
- "5434:5432"
volumes:
- notification_db_data:/var/lib/postgresql/data
restart: unless-stopped

# Add more Postgres containers if you want separate DBs
# for routing-service, scheduling-service, etc.
# Or reuse one DB with multiple schemas if that suits you better.

# ========== Services ==========

task-service:
build: ./task-service
container_name: task-service
ports:
- "8081:8080"
depends_on:
- postgres-task
environment:
SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-task:5432/task_service_db
SPRING_DATASOURCE_CUSTOMERNAME: customer
SPRING_DATASOURCE_PASSWORD: password
restart: unless-stopped

customer-service:
build: ./customer-service
container_name: customer-service
ports:
- "8082:8080"
depends_on:
- postgres-customer
environment:
SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-customer:5432/customer_service_db
SPRING_DATASOURCE_CUSTOMERNAME: customer
SPRING_DATASOURCE_PASSWORD: password
restart: unless-stopped

notification-service:
build: ./notification-service
container_name: notification-service
ports:
- "8083:8080"
depends_on:
- postgres-notification
environment:
SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-notification:5432/notification_service_db
SPRING_DATASOURCE_CUSTOMERNAME: customer
SPRING_DATASOURCE_PASSWORD: password
restart: unless-stopped

# If your routing-service & scheduling-service each need a DB,
# add them similarly. If they don't, just define them with no DB container:

routing-service:
build: ./routing-service
container_name: routing-service
ports:
- "8084:8080"
#depends_on:
# if no DB needed, skip
# or reference a DB if it does
#environment:
# If no DB, skip. Otherwise, define as above.
restart: unless-stopped

scheduling-service:
build: ./scheduling-service
container_name: scheduling-service
ports:
- "8085:8080"
#depends_on:
# if it has a DB, reference it
#environment:
# ...
restart: unless-stopped

# API Gateway
api-gateway:
build: ./api-gateway
container_name: api-gateway
ports:
- "8080:8080"
depends_on:
# ensure it waits for the services it routes to, if needed
- task-service
- customer-service
- notification-service
- routing-service
- scheduling-service
restart: unless-stopped

# Named volumes for DB persistence
volumes:
task_db_data:
customer_db_data:
notification_db_data:


Below is a **comprehensive** look at the **Tasks**, **Zones**, and **Scheduling** logic we discussed, **focusing** on how each piece fits together without diving into microservice deployment, code-level details, or enhancements outside the scope of tasks, zones, customers, and scheduling.

---

## 1. **Task Structure and Types**

### 1.1 Base `Task` (Abstract)

- **Purpose**: Represents a general task with shared fields that apply to all subtypes.
- **Key Fields**:
    - **Title** (`String`, not blank)
    - **Priority** (`int` or an enum) — might affect scheduling order
    - **Due Date** (`LocalDateTime`, mandatory) — the deadline by which the task must be completed
    - **Reminder Date** (`LocalDateTime`, mandatory) — for notifications or reminders
    - **Description** (`String`, optional) — more detailed info about the task
    - **Category** (`String`) — e.g., “Work,” “Health,” “Recreation,” etc.
    - **Recurrence Pattern** (`String`) — e.g., “NONE,” “DAILY,” “RRULE:...,” or “MON,WED,FRI”
    - **Status** (enum: `PENDING`, `COMPLETED`, `DELAYED`)
    - **Type** (enum: `FIXED`, `FLEXIBLE`, `PROJECT`)
      > Even though single-table inheritance uses a `task_discriminator`, storing `type` in the same record can help logic or debugging.

- **Dependencies**: A self-referencing **many-to-many** allows tasks to specify other tasks that must occur first. For example, “Pick up prescription” must happen before “Go on holiday.”

- **Project Relationship**:
    - A `Task` may belong to a `ProjectTask` (via `parentProject`). This allows nesting in the case of **ProjectTask** subtypes.

### 1.2 Single-Table Inheritance and Subclasses

We use **single-table inheritance**, meaning one DB table (`tasks`) holds columns for all fields from the base `Task` and from each subtype. We **discriminate** which subtype is stored in each row via a `task_discriminator` column.

1. **`FixedTask`**
    - **startDateTime** and **endDateTime** are set. The task must occur precisely in that window.
    - Example: a doctor’s appointment from 9:00 AM to 9:30 AM.

2. **`FlexibleTask`**
    - Has **estimatedDuration** (minutes) and optional **earliestStartDateTime** / **latestEndDateTime**.
    - Example: “Study for 2 hours,” which can be scheduled any time before 8 PM, or within a daily zone.

3. **`ProjectTask`**
    - A container for multiple sub-tasks (which can be `FixedTask`, `FlexibleTask`, or nested `ProjectTask`).
    - Typically has a `dueDate` that covers the entire project. All sub-tasks must finish prior to that deadline.

### 1.3 How Task Structure Influences Scheduling

- **Fixed** tasks block out **non-negotiable** time, which must remain as-is in the calendar.
- **Flexible** tasks can shift to any open slot that meets customer constraints, up until their due date (and factoring in earliest starts, transitions, etc.).
- **Projects** require that all sub-tasks be scheduled (either fixed or flexible) before the project due date, respecting each sub-task’s constraints.

---

## 2. **Zones and How They Influence Scheduling**

### 2.1 Concept of Zones

A **Zone** defines **time-based constraints** on scheduling, typically derived from customer preferences. Examples:

- **Working Hours**: “From 9 AM to 5 PM on weekdays, tasks of category ‘Work’ have priority; outside these hours, work tasks are only allowed if priority ≥ some threshold.”
- **Recreation Time**: “No tasks from 5 PM to 7 PM except those with priority >= 8.”
- **Sports Slot**: “I can do sports tasks only between 7 AM – 8:30 AM or 5 PM – 9 PM.”

### 2.2 Zone Configuration vs. Zone Definition

1. **ZoneConfiguration**
    - A **named** set of zone definitions (e.g., “Regular Times,” “Holiday Times”).
    - A customer can switch between multiple configurations (for example, different scheduling profiles).
    - Has an **“active”** flag to indicate which configuration is currently used by the scheduling logic.

2. **ZoneDefinition**
    - The **individual** rule or time window.
    - Typically includes:
        - **dayMask** (bitmask representing days of the week; e.g. Monday+Wednesday)
        - **startTime** and **endTime** (e.g. “07:00” to “09:00”)
        - **allowedCategories** (Set of categories permissible in this window)
        - **excludedCategories** (Optional set of categories disallowed)
        - **priorityOverrideThreshold** (an integer, if tasks with priority >= X can break the zone restrictions)
    - Tied to one `ZoneConfiguration`.

### 2.3 Zones’ Role in Scheduling

When placing tasks, the scheduler must respect each zone’s constraints. For each day/time:

- If a **Flexible** task’s category is **not** in the allowed set (or is in the excluded set), it generally **cannot** be scheduled in that zone **unless** the task’s `priority >= priorityOverrideThreshold`.
- If the day/time is outside any zone definitions, or the zone definitions permit that category, scheduling can proceed.

Essentially, these **zones** define *where, when, and under what conditions* tasks can be slotted. If a customer says “No tasks on Sunday,” that might appear as a dayMask excluding Sunday. If “Sport tasks are only in the morning or evening,” the zone definitions codify that.

---

### 2.4 MVP Clarification: Fixed Tasks vs. Zones

Fixed tasks block real occupied time. Examples include a work shift, doctor appointment, lecture, meeting, or a user-created `Free time` block from the daily briefing. Fixed tasks are placed before flexible scheduling and define which intervals are unavailable.

Zones guide flexible task placement. A zone can prefer Work in the morning, Sport in the evening, Duty tasks during admin hours, or only allow urgent tasks in a protected window. Zones do not make an entire category fixed; fixedness belongs to the individual task.

Work should follow the same rule. Real work shifts are fixed Work tasks, including recurring fixed Work tasks for regular hours and manually entered or imported fixed Work tasks for changing shifts. Flexible work or flextime should be flexible Work tasks. Work zones define preferred or allowed windows for flexible Work tasks; they do not block actual work time by themselves.

### 2.5 Generalized Zones

The MVP zone model supports:

- **primaryCategory**: the first category the scheduler tries in that zone.
- **secondaryCategories**: backup categories that may fill the zone when no primary task fits or needs the slot.
- **behaviorMode**: `STRICT` or `PREFERRED`.
- **priorityOverrideThreshold**: the minimum task priority that may override a strict category restriction.

In a `STRICT` zone, the scheduler tries primary tasks first, then secondary tasks, and only allows unrelated flexible tasks when the priority override threshold is reached. In a `PREFERRED` zone, the scheduler still prefers primary and secondary tasks first, but may place other suitable flexible tasks when preferred tasks do not fit or have no current demand.

Older zones with only `allowedCategories` continue to behave as strict allowed-category windows. New zones derive `allowedCategories` from primary plus secondary categories so older API consumers remain compatible.

### 2.6 Daily Briefing MVP

The daily briefing previews the generated plan before the user works with it. It shows fixed commitments, suggested flexible tasks, scheduled project tasks where available, remaining free time, skipped or delayed tasks where derivable, and tight spots.

The briefing does not offer permanent deletion; deletion belongs in task details. If the user chooses to keep a slot free, the app creates a fixed task named `Free time` for that exact time slot so regeneration does not refill it.

Future enhancements may add weekly Work-hour targets, overtime confirmation once a Work target is exceeded, internal lunch breaks inside fixed Work blocks, calendar import for changing shifts, full unavailable-time blocks, and advanced recurrence occurrence handling. For now, unavailable time can be reserved through fixed tasks such as `Free time`.

---

## 3. **Scheduling Logic: Combining Tasks and Zones**

### 3.1 Overview of Scheduling Flow

1. **Gather Tasks**: The system collects tasks from the **task** domain—both fixed and flexible, plus any project tasks.
2. **Check Active ZoneConfiguration**: The customer has exactly one or zero “active” zone config. The scheduling algorithm retrieves that set of zone definitions.
3. **Handle Fixed Tasks First**:
    - Place them in the calendar at their required start/end.
    - Mark those intervals as occupied.
4. **Allocate Flexible Tasks**:
    - For each flexible task, the system looks for time slots that align with the zone definitions and the task’s earliest/latest windows.
    - The customer’s zone constraints might forbid scheduling certain categories at certain times, or might allow them only if priority is high.
    - The scheduler attempts to place the flexible task **before** its due date.
5. **Project Tasks**:
    - If a task is a `ProjectTask`, schedule each sub-task. They must collectively finish before the project’s overall due date.
    - If any sub-task is **flexible**, it also follows zone constraints. If sub-tasks are **fixed**, they must fit in their time windows.

### 3.2 Zone Checks in Detail

For a given day and time slot:

- **Day-of-week**: The scheduler matches `dayMask` with the day in question. If the day isn’t included, that zone definition doesn’t apply. If multiple zone definitions overlap, the scheduler merges rules or applies the most restrictive logic.
- **Start/End Time**: The time must fall between `startTime` and `endTime`. If it’s outside that range, the zone doesn’t apply.
- **Allowed vs. Excluded Categories**: If the task’s category is in `excludedCategories`, that zone forbids scheduling unless priority meets or exceeds `priorityOverrideThreshold`. If the category is in `allowedCategories`, it’s presumably permitted.
- **Priority Override**: If the customer assigned a high priority (≥ threshold), tasks can override typical restrictions.

### 3.3 Handling Recurrence

- If a task has a recurrence pattern, the scheduling logic might:
    1. Generate repeated tasks or time slots for each occurrence, or
    2. On the fly, place it in each relevant day/time that meets the zone constraints.

For example, “Gym every weekday from 7 AM – 8 AM if available, or 8 PM – 9 PM otherwise,” would check each day’s zone definitions for valid morning/evening slots.

---

## 4. **Customer Context**

A customer typically has:

- **Profile** info (name, email).
- **Multiple** `ZoneConfiguration`s (like “Regular routine,” “Vacation,” “Exam period”).
- One **active** configuration at a time. Scheduling queries that active config to see how tasks can be placed.

**Why**: This design allows each customer to define custom daily constraints—someone who works nights might prefer tasks scheduled in the afternoon, etc.

---

## 5. **Summary of the Overall Logic**

1. **Tasks** are stored with:
    - A **type**: `FIXED` (strict window), `FLEXIBLE` (movable by duration), or `PROJECT` (container of sub-tasks).
    - **Dependencies** that must be completed beforehand.
    - Possibly a **recurrence** pattern for repeated tasks.
2. **Zone configurations** define **when** tasks of certain categories or priorities can be scheduled. They have multiple **zone definitions** for specific days or times.
3. **Scheduling** merges these constraints:
    - Places all **Fixed** tasks first.
    - Iterates over **Flexible** tasks to find open slots within the customer’s zones (or overridden by priority).
    - For **Project** tasks, schedules sub-tasks individually, ensuring they finish before the project’s due date.
4. The customer’s **active** zone config is essential for deciding the valid times each day. If no config is active, the system might default to scheduling tasks at any time.

---

## Final Note

- **Tasks** revolve around a **single-table** inheritance structure, ensuring minimal duplication of fields.
- **Zones** revolve around customer-defined time windows, categories, and priority thresholds.
- **Scheduling** merges these two domains by checking each `Task` or sub-task against the customer’s **current** set of zone definitions to determine allowed times.

This approach provides a **flexible** system where each customer can define highly customized scheduling constraints and see their tasks arranged accordingly.



Below is a **deeper dive** into how your **Flexible Task** scheduling might evolve given these **two points** you’ve clarified. We’ll break down (1) how a **taskNature** that’s “fixed estimate” or “open ended” impacts scheduling frequency and completion and (2) the proposed **zone-based** approach for creating time slots after fixed tasks are placed.

---

## 1. Flexible Tasks Not Strictly Bound by Estimated Time

### 1.1 TaskNature “fixed estimate”

- **Definition**: You have a _target_ estimated duration (e.g., 5 hours) that you want to allocate **before** the due date.
- **Reality**: Sometimes the customer might not complete the task within those 5 hours—maybe they needed more time. So the system can re-schedule or continue scheduling it, even if the original “estimate” was exhausted.
- **Benefit of “Estimate”**: The estimate provides a **hint** for how to block out initial time, but if the customer **still** marks it incomplete, the system can allocate additional blocks.
- **Progressive** scheduling: As you approach the due date, you can increase block sizes or schedule more frequent sessions (since the customer evidently needed more time than initially estimated).

**Practical Approach**
1. Initially, you schedule **X** hours (the “fixed estimate”), spread over daily or weekly blocks.
2. If the customer logs that the task is still incomplete after using up X hours, the scheduling logic tries to find additional blocks.
3. The logic or your UI might prompt: “Task not done yet—allocate more time?” Then it extends the schedule.
4. If you are close to the due date, your scheduling algorithm can allocate bigger or more frequent blocks (the “progressive” approach).

### 1.2 TaskNature “open ended”

- **Definition**: The customer didn’t provide a strict estimate. The task just needs repeated scheduling until either the customer manually marks it complete or the due date passes.
- **Ongoing Scheduling**: Each day (or each scheduling cycle), the system can allocate a block of time to this task if it’s still not marked complete.
- **No Hard Cap**: You don’t have a maximum of, say, 5 hours total. Instead, you keep scheduling small blocks or progressive blocks as time allows.
- **Progressive Flag**: If set, block size or frequency might increase as the deadline approaches. If not, maybe you keep scheduling the same size block repeatedly.

**Practical Approach**
1. Every day (or on each scheduling run), the logic sees the task is still `PENDING` and not completed.
2. It tries to place a **block** of time in your “open” time slots, respecting the customer’s category constraints (zones).
3. If the customer never finishes (or the due date arrives), you stop scheduling once the date is reached or the customer says “Done.”

### 1.3 The “Progressive Flag” for Both Types

- **“Fixed Estimate”** tasks can adopt progressive block sizing if the original plan fails to complete in time. For instance, if the customer is behind schedule, the system ramps up from 1-hour daily sessions to 2-hour daily sessions.
- **“Open Ended”** tasks can start small but keep increasing block size as you near the due date.

**Example**
- Day 1 to Day 10: 30-minute daily blocks.
- Day 10 to Day 20: 1-hour daily blocks if still not done.
- Day 20 to Day 30 (final stretch): 2-hour daily blocks.

---

## 2. Zone-Based TimeSlot Creation After Placing Fixed Tasks

### 2.1 Placing Fixed Tasks First

1. **Identify** all customer-defined fixed tasks (appointments, events with strict start/end times).
2. **Insert** them into the calendar. Mark those intervals as “occupied” or “unavailable.”
3. **Result**: You have a timeline with blocks of time that are already taken, and gaps between them.

### 2.2 Converting Remaining Time + Customer Scheduling Hours → TimeSlots

Once fixed tasks are placed, you have “free” periods in each day. You also have customer-defined zones specifying categories, times, and rules. Your logic might:

1. **Slice** the daily schedule by **zone** boundaries. For instance, the customer might say:
    - **Study Zone**: 2 PM – 5 PM, Monday–Friday, for tasks with category = “study.”
    - **Unspecified Zone**: 7 AM – 2 PM (or leftover times that aren’t assigned to a named zone).
2. **Subtract** any intervals overlapped by fixed tasks from these zones.
    - E.g., if a fixed task occupies 2:30 PM – 3:30 PM, you split that zone into 2 PM – 2:30 PM and 3:30 PM – 5 PM as separate segments.
3. **Label** each free segment with the category or priority rules from the zone definition.
    - If it’s a “Study Zone,” then tasks with category “study” are strongly preferred. Possibly other tasks might be disallowed or only allowed if they have a high priority override.
    - If it’s an “Unspecified Zone,” maybe all tasks are allowed, subject to general constraints.

### 2.3 Assign Flexible Tasks to the Created TimeSlots

- **Preferred** approach: If a zone specifically matches a task’s category, place that task first in that zone. (Unless you have a higher priority task that also wants that slot and is permissible under that zone’s rules.)
- If a slot is “unspecified,” you can schedule any category there, but maybe you apply priority logic to decide which tasks get that time.
- **Priority overrides**: If a zone normally forbids certain categories, a task with a high enough priority might still be allowed.

**Example**
- You discovered a 3-hour gap from 2 PM to 5 PM. The customer has a “study zone” from 2 PM – 4 PM and “unspecified” from 4 PM – 5 PM.
    - For tasks with category “study,” you put them in the 2–4 PM “study zone.”
    - For other tasks, you can use 4–5 PM or any leftover in the “study zone” if the customer’s zone rules permit.

### 2.4 Managing Partial Fills

If a 2-hour “study zone” remains, but your flexible “study” task only needs 1 hour, you place it for 1 hour. The leftover hour remains an available slot. Possibly you split it into multiple segments if needed.

---

## Putting It All Together

1. **Place Fixed Tasks**:
    - Non-negotiable times. They carve out “occupied” intervals in the day.

2. **Create TimeSlots for Flexible Tasks**:
    - Based on the customer’s **zone definitions** (study zone, work zone, free zone, etc.).
    - Subtract the intervals used by fixed tasks, leaving smaller blocks of “free” time.
    - Label them with the zone’s rules: allowed category, priority override, etc.

3. **Schedule Flexible Tasks**:
    - For each flexible task, check the relevant slots (matching category or “unspecified”).
    - If the task can be splitted (`canBeSeparated`), break the total needed time into multiple blocks. Possibly use a “progressive” approach if the due date is near.
    - If the task is “fixed estimate,” allocate the initially estimated hours. If still incomplete, schedule more. If it’s “open ended,” keep scheduling small blocks until the customer says done (or the due date hits).

4. **Progressive**:
    - You can increase block size or frequency of scheduling as time gets short. This logic can be uniform for both “open ended” and “fixed estimate” tasks.

**End Result**: You fill each day’s “open” slots with your flexible tasks in an order that respects category-based zones and priority. Over time (especially if you recalculate daily or weekly), the tasks keep receiving allocated blocks until they’re completed or the deadline passes.

---

## Summary

- **Flexible Task** logic is not strictly bound by the “estimated time.” You can keep scheduling it until the customer finishes or the due date arrives.
- **Fixed Estimate** tasks can still get more hours if not completed. **Open Ended** tasks never had a strict hour limit in the first place. Both can use a “progressive” approach to ramp up scheduling intensity.
- **Zone-based** scheduling starts by placing fixed tasks, then divides the leftover day into zone-labeled time slots. You match flexible tasks (by category and priority) to those slots, possibly splitting tasks into smaller blocks or partial segments if `canBeSeparated` is true.

This strategy should yield a **comprehensive** scheduling approach that respects customer-defined zones, accommodates flexible tasks that may need repeated scheduling, and escalates scheduling as the due date looms.





Below is a **Markdown** summary of our **routing** discussions, including **address storage**, **membership limits**, **geocoding**, **distance matrix** generation, and **integration** with your scheduling logic.

---

# Routing Deliberations

## 1. Storing and Managing Addresses

- **Address Entity**
    - We decided to create an entity/class to store customer addresses.
    - Fields might include:
        - `addressLine` (the full string, e.g., "123 Elm Street, City, Country")
        - `latitude`, `longitude` (for geocoded coordinates)
    - **Optional**: A `@ManyToOne` relationship to `Customer`, so each customer can have multiple addresses.

- **Membership Constraints**
    - **Plus** customers can store up to 5 addresses.
    - **Premium** customers can store an **unlimited** number of addresses.
    - Enforce these rules in your service layer (e.g., disallow creation of a 6th address for Plus).

- **Geocoding**
    - When a new address is saved, call a **geocoding** API (e.g., Google Geocoding, Mapbox Geocoding) to get lat/long.
    - Save these coordinates so you only do the geocode operation **once**.
    - This approach prevents repeated overhead and any risk of rate-limit issues with external APIs.

## 2. Distance Matrix Generation

- **Purpose**: Provide travel times or distances between stored addresses.
- **Data Structure**:
    - A `DistanceMatrix` object/DTO containing a list of addresses (row/column order) and a 2D array of distances or travel times.
- **External API**:
    - Use **Google Distance Matrix API** (or a similar provider) for retrieving travel times between each pair of addresses.
    - Make sure to handle rate limits and API costs (especially for large sets of addresses).
- **Pairwise or Bulk Request**:
    - Ideally, send multiple origins/destinations in one request to Google’s Distance Matrix to reduce calls.
    - For small sets of addresses (e.g., up to 5 for Plus customers), you can also do pairwise calls if needed.

## 3. Routing Service Module

You decided to build a dedicated **routing-service** module that handles:
1. **Address Management** (CRUD for addresses, geocoding on save)
2. **Distance Matrix** generation (calling the Google Distance Matrix API or a local routing engine)

**Potential Classes**:
- `AddressRepository`: Spring Data JPA for address entities.
- `GeocodingService`: A service class that calls an external geocoding API to populate lat/long.
- `RoutingService`:
    - `buildDistanceMatrix(List<Address>)` → returns a `DistanceMatrix`
    - Possibly a method like `getTravelTime(Address, Address)` internally or for pairwise calls.

**Controller**:
- Could expose endpoints like:
    - `POST /routing/addresses` (creates an address, geocodes it, saves)
    - `GET /routing/matrix?addressIds=...` (returns a JSON distance matrix for the given addresses)

## 4. Caching and Performance

- **Caching**:
    - You can store `(originLat, originLon, destLat, destLon) -> travelTime` in an in-memory or DB table (e.g., `DistanceCache`).
    - If the customer repeatedly requests the same pair, you serve it from cache and only call Google if the entry is stale.
- **Limitations**:
    - If the customer has a large number of addresses (for Premium customers), the matrix might become big (N×N).
    - Consider building or updating it periodically (e.g., once per day) rather than on every request.
    - Some customers may only keep a few addresses, so the cost is manageable.

## 5. Integration with Scheduling

- **Scheduling-Service** can call **routing-service** for:
    - **Distance** or **travel times** between tasks’ addresses.
    - Building a matrix for the customer’s known addresses (if tasks reference them).
- **In Scheduling Logic**:
    - When placing tasks, include `travelTime(A → B)` as part of the time calculations (e.g., `endOfA + travelTime = earliestStartOfB`).
    - For advanced route optimization, you might use TSP heuristics or a library (Google OR-Tools, OptaPlanner, etc.) with data from the routing-service.

## 6. Alternative: Google Navigation SDK

- **Navigation SDK** is typically for **real-time** directions (turn-by-turn), more relevant for **mobile** usage.
- For **backend scheduling** or **distance matrix** generation, the standard **Distance Matrix API** is often enough.
- You might still integrate the Navigation SDK if you want the customer (on mobile) to get real-time directions after scheduling. But for server-based route calculations, the REST-based Distance Matrix or Directions API is simpler.

---

### Final Summary

1. **Addresses**: Let customers store addresses (geocoded once).
2. **Membership** tiers limit how many addresses they can store.
3. **Routing-Service**:
    - Stores addresses, calls a geocoding API, and can build a distance matrix via an external API (Google Distance Matrix).
4. **Caching**: recommended to reduce API calls.
5. **Scheduling** then uses the computed travel times to place tasks in an order that accounts for travel overhead.
6. **Navigation SDK** is optional if you want real-time turn-by-turn guidance in a mobile scenario; for server scheduling, the Distance Matrix API is typically sufficient.

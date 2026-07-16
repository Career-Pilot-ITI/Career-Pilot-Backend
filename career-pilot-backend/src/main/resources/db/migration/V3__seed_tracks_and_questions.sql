-- Seed initial tracks
INSERT INTO tracks (name, description, is_active, created_at) VALUES
('Junior Backend Developer', 'Entry-level backend development track covering core programming, databases, and API design.', TRUE, NOW()),
('Senior Backend Engineer',    'Advanced backend track covering system design, microservices, and distributed systems.', TRUE, NOW()),
('Frontend Developer',         'Frontend development track covering JavaScript, CSS, and modern frameworks.', TRUE, NOW()),
('Data Analyst',               'Data analysis track covering SQL, statistics, and data visualization.', TRUE, NOW()),
('DevOps Engineer',            'DevOps track covering CI/CD, containerization, and cloud infrastructure.', TRUE, NOW()),
('Mobile Developer',           'Mobile development track covering Android and iOS fundamentals and architecture.', TRUE, NOW());

-- Junior Backend Developer questions
WITH t AS (SELECT id FROM tracks WHERE name = 'Junior Backend Developer')
INSERT INTO question_bank (track_id, question_text, difficulty_level, category, expected_keywords, is_active, created_at) VALUES
((SELECT id FROM t), 'What is the difference between an array and a linked list?', 'EASY', 'TECHNICAL', 'array, linked-list, data-structures, memory-allocation, time-complexity', TRUE, NOW()),
((SELECT id FROM t), 'Explain what a REST API is and its key principles.', 'EASY', 'TECHNICAL', 'rest, api, http, stateless, crud, endpoints', TRUE, NOW()),
((SELECT id FROM t), 'What is the difference between SQL and NoSQL databases? When would you use each?', 'MEDIUM', 'TECHNICAL', 'sql, nosql, relational, mongodb, postgresql, scalability', TRUE, NOW()),
((SELECT id FROM t), 'Explain dependency injection and why it is useful.', 'MEDIUM', 'TECHNICAL', 'dependency-injection, ioc, spring, loose-coupling, testability', TRUE, NOW()),
((SELECT id FROM t), 'What are the SOLID principles in object-oriented programming?', 'MEDIUM', 'TECHNICAL', 'solid, single-responsibility, open-closed, liskov, interface-segregation, dependency-inversion', TRUE, NOW()),
((SELECT id FROM t), 'Explain how garbage collection works in Java.', 'MEDIUM', 'TECHNICAL', 'garbage-collection, jvm, heap, mark-sweep, generational, stop-the-world', TRUE, NOW()),
((SELECT id FROM t), 'What is the difference between authentication and authorization?', 'EASY', 'TECHNICAL', 'authentication, authorization, jwt, oauth, session, token', TRUE, NOW()),
((SELECT id FROM t), 'How would you design a simple URL shortening service?', 'MEDIUM', 'PROJECT', 'url-shortener, system-design, hashing, database, redirect', TRUE, NOW()),
((SELECT id FROM t), 'What is an ORM and when should you use it?', 'EASY', 'TECHNICAL', 'orm, jpa, hibernate, sql, abstraction, lazy-loading, n-plus-one', TRUE, NOW()),
((SELECT id FROM t), 'Explain the difference between PUT and PATCH in HTTP.', 'EASY', 'TECHNICAL', 'put, patch, http, idempotent, partial-update, rest', TRUE, NOW());

-- Senior Backend Engineer questions
WITH t AS (SELECT id FROM tracks WHERE name = 'Senior Backend Engineer')
INSERT INTO question_bank (track_id, question_text, difficulty_level, category, expected_keywords, is_active, created_at) VALUES
((SELECT id FROM t), 'How would you design a URL shortener service like bit.ly?', 'MEDIUM', 'PROJECT', 'system-design, url-shortener, hashing, database-sharding, caching', TRUE, NOW()),
((SELECT id FROM t), 'How do you handle database schema migrations in a production system?', 'MEDIUM', 'TECHNICAL', 'flyway, liquibase, migration, rollback, version-control, zero-downtime', TRUE, NOW()),
((SELECT id FROM t), 'Describe how you would design a real-time notification system.', 'HARD', 'PROJECT', 'websocket, message-queue, kafka, push-notification, event-driven', TRUE, NOW()),
((SELECT id FROM t), 'How do you ensure data consistency across microservices?', 'HARD', 'TECHNICAL', 'saga-pattern, distributed-transactions, eventual-consistency, outbox, kafka', TRUE, NOW()),
((SELECT id FROM t), 'Explain the CAP theorem and its implications for distributed databases.', 'HARD', 'TECHNICAL', 'cap-theorem, consistency, availability, partition-tolerance, distributed-systems, trade-offs', TRUE, NOW()),
((SELECT id FROM t), 'How would you design a rate limiter for a high-traffic API?', 'MEDIUM', 'PROJECT', 'rate-limiting, token-bucket, leaky-bucket, redis, throttling, scalability', TRUE, NOW()),
((SELECT id FROM t), 'Explain how you would debug a memory leak in a production Java application.', 'HARD', 'SITUATIONAL', 'memory-leak, heap-dump, profiler, jvm, gc, oom, mat', TRUE, NOW()),
((SELECT id FROM t), 'How do you handle versioning in a REST API?', 'MEDIUM', 'TECHNICAL', 'api-versioning, uri, header, backward-compatibility, deprecation, semver', TRUE, NOW()),
((SELECT id FROM t), 'Describe how you would implement a caching layer with Redis.', 'MEDIUM', 'PROJECT', 'redis, caching, cache-aside, ttl, invalidation, redis-cluster', TRUE, NOW()),
((SELECT id FROM t), 'What is the difference between synchronous and asynchronous communication between services?', 'EASY', 'TECHNICAL', 'sync, async, rest, messaging, kafka, rabbitmq, blocking, non-blocking', TRUE, NOW());

-- Frontend Developer questions
WITH t AS (SELECT id FROM tracks WHERE name = 'Frontend Developer')
INSERT INTO question_bank (track_id, question_text, difficulty_level, category, expected_keywords, is_active, created_at) VALUES
((SELECT id FROM t), 'What is the difference between let, const, and var in JavaScript?', 'EASY', 'TECHNICAL', 'javascript, scoping, hoisting, es6, block-scope', TRUE, NOW()),
((SELECT id FROM t), 'Explain the CSS box model.', 'EASY', 'TECHNICAL', 'css, box-model, margin, padding, border, content', TRUE, NOW()),
((SELECT id FROM t), 'What is the virtual DOM and how does it improve performance?', 'MEDIUM', 'TECHNICAL', 'virtual-dom, react, reconciliation, diffing, batching', TRUE, NOW()),
((SELECT id FROM t), 'Explain state management in React. When would you use Redux?', 'MEDIUM', 'TECHNICAL', 'react, state-management, redux, context-api, props, flux', TRUE, NOW()),
((SELECT id FROM t), 'What is the difference between controlled and uncontrolled components in React?', 'MEDIUM', 'TECHNICAL', 'controlled, uncontrolled, form, react, ref, state', TRUE, NOW()),
((SELECT id FROM t), 'Explain how promises work in JavaScript and how they differ from callbacks.', 'MEDIUM', 'TECHNICAL', 'promises, callbacks, async, await, javascript, event-loop, microtask', TRUE, NOW()),
((SELECT id FROM t), 'What is CSS specificity and how does it affect styling?', 'EASY', 'TECHNICAL', 'css, specificity, cascade, inline-styles, id, class, important', TRUE, NOW()),
((SELECT id FROM t), 'How would you optimize a React application for performance?', 'MEDIUM', 'PROJECT', 'react, performance, memo, usememo, usecallback, code-splitting, lazy-loading', TRUE, NOW()),
((SELECT id FROM t), 'What is the difference between == and === in JavaScript?', 'EASY', 'TECHNICAL', 'loose-equality, strict-equality, type-coercion, javascript, comparison', TRUE, NOW()),
((SELECT id FROM t), 'Explain the concept of closures in JavaScript with an example.', 'MEDIUM', 'TECHNICAL', 'closure, lexical-scope, javascript, function, scope, encapsulation', TRUE, NOW());

-- Data Analyst questions
WITH t AS (SELECT id FROM tracks WHERE name = 'Data Analyst')
INSERT INTO question_bank (track_id, question_text, difficulty_level, category, expected_keywords, is_active, created_at) VALUES
((SELECT id FROM t), 'What is the difference between WHERE and HAVING in SQL?', 'EASY', 'TECHNICAL', 'sql, where, having, group-by, filter, aggregation', TRUE, NOW()),
((SELECT id FROM t), 'Explain the difference between mean, median, and mode.', 'EASY', 'TECHNICAL', 'statistics, mean, median, mode, average, distribution', TRUE, NOW()),
((SELECT id FROM t), 'How would you clean a dataset with missing values?', 'MEDIUM', 'SITUATIONAL', 'data-cleaning, missing-values, imputation, pandas, outlier-detection', TRUE, NOW()),
((SELECT id FROM t), 'Explain the different types of JOINs in SQL with examples.', 'MEDIUM', 'TECHNICAL', 'sql, inner-join, left-join, right-join, full-outer-join, cross-join', TRUE, NOW()),
((SELECT id FROM t), 'What is the difference between correlation and causation?', 'EASY', 'TECHNICAL', 'correlation, causation, statistics, spurious, confounding-variable', TRUE, NOW()),
((SELECT id FROM t), 'How do you detect and handle outliers in a dataset?', 'MEDIUM', 'TECHNICAL', 'outliers, iqr, z-score, boxplot, winsorization, anomaly-detection', TRUE, NOW()),
((SELECT id FROM t), 'Explain what a window function is in SQL and give an example.', 'MEDIUM', 'TECHNICAL', 'window-function, row-number, rank, partition-by, over, sql', TRUE, NOW()),
((SELECT id FROM t), 'What is the difference between supervised and unsupervised learning?', 'EASY', 'TECHNICAL', 'supervised, unsupervised, classification, regression, clustering, labeled-data', TRUE, NOW()),
((SELECT id FROM t), 'How would you design an A/B test to measure feature impact?', 'MEDIUM', 'PROJECT', 'ab-testing, hypothesis, control-group, statistical-significance, sample-size', TRUE, NOW()),
((SELECT id FROM t), 'What is overfitting and how do you prevent it?', 'MEDIUM', 'TECHNICAL', 'overfitting, underfitting, cross-validation, regularization, bias-variance-tradeoff', TRUE, NOW());

-- DevOps Engineer questions
WITH t AS (SELECT id FROM tracks WHERE name = 'DevOps Engineer')
INSERT INTO question_bank (track_id, question_text, difficulty_level, category, expected_keywords, is_active, created_at) VALUES
((SELECT id FROM t), 'What is the difference between Docker and a virtual machine?', 'EASY', 'TECHNICAL', 'docker, vm, container, hypervisor, isolation, os-kernel', TRUE, NOW()),
((SELECT id FROM t), 'Explain what CI/CD is and why it matters.', 'EASY', 'TECHNICAL', 'ci-cd, jenkins, github-actions, automation, deployment, testing', TRUE, NOW()),
((SELECT id FROM t), 'How would you set up monitoring and alerting for a production application?', 'MEDIUM', 'SITUATIONAL', 'monitoring, prometheus, grafana, alerting, slo, observability', TRUE, NOW()),
((SELECT id FROM t), 'Explain Infrastructure as Code and tools like Terraform.', 'MEDIUM', 'TECHNICAL', 'iac, terraform, ansible, cloudformation, declarative, idempotent', TRUE, NOW()),
((SELECT id FROM t), 'What is Kubernetes and how does it differ from Docker Compose?', 'MEDIUM', 'TECHNICAL', 'kubernetes, docker-compose, orchestration, pods, cluster, scaling', TRUE, NOW()),
((SELECT id FROM t), 'How do you manage secrets in a CI/CD pipeline?', 'MEDIUM', 'SITUATIONAL', 'secrets, vault, env-vars, github-secrets, hashicorp-vault, encryption', TRUE, NOW()),
((SELECT id FROM t), 'Explain the difference between blue-green deployment and canary releases.', 'MEDIUM', 'TECHNICAL', 'blue-green, canary, deployment, rollback, zero-downtime, traffic-shifting', TRUE, NOW()),
((SELECT id FROM t), 'What is a sidecar pattern and when would you use it?', 'HARD', 'TECHNICAL', 'sidecar, service-mesh, envoy, istio, proxy, logging, monitoring', TRUE, NOW()),
((SELECT id FROM t), 'How do you troubleshoot a pod that is stuck in CrashLoopBackOff?', 'EASY', 'SITUATIONAL', 'crashloopbackoff, kubernetes, logs, events, pod, debugging, liveness-probe', TRUE, NOW()),
((SELECT id FROM t), 'Explain how you would secure a Docker container in production.', 'MEDIUM', 'TECHNICAL', 'container-security, non-root, read-only-fs, image-scanning, seccomp, apparmor', TRUE, NOW());

-- Mobile Developer questions
WITH t AS (SELECT id FROM tracks WHERE name = 'Mobile Developer')
INSERT INTO question_bank (track_id, question_text, difficulty_level, category, expected_keywords, is_active, created_at) VALUES
((SELECT id FROM t), 'What is the difference between an Activity and a Fragment in Android?', 'EASY', 'TECHNICAL', 'android, activity, fragment, lifecycle, ui-component', TRUE, NOW()),
((SELECT id FROM t), 'Explain the delegate pattern in iOS development.', 'EASY', 'TECHNICAL', 'ios, delegate, protocol, uitableview, delegation, swift', TRUE, NOW()),
((SELECT id FROM t), 'How would you handle offline data synchronization in a mobile app?', 'MEDIUM', 'PROJECT', 'offline-first, caching, room, core-data, sync, conflict-resolution', TRUE, NOW()),
((SELECT id FROM t), 'Explain the MVVM architecture pattern and its benefits.', 'MEDIUM', 'TECHNICAL', 'mvvm, architecture, viewmodel, databinding, separation-of-concerns, testability', TRUE, NOW()),
((SELECT id FROM t), 'What is the difference between a ViewModel and a SavedStateHandle in Android?', 'MEDIUM', 'TECHNICAL', 'viewmodel, savedstatehandle, android, configuration-change, state-preservation', TRUE, NOW()),
((SELECT id FROM t), 'Explain how Auto Layout works in iOS and what constraints are.', 'MEDIUM', 'TECHNICAL', 'ios, auto-layout, constraints, uikit, frames, safe-area, intrinsic-content-size', TRUE, NOW()),
((SELECT id FROM t), 'What is Jetpack Compose and how is it different from the traditional View system?', 'MEDIUM', 'TECHNICAL', 'jetpack-compose, declarative-ui, android, view, composable, state', TRUE, NOW()),
((SELECT id FROM t), 'How do you manage memory in a mobile app to avoid leaks?', 'EASY', 'TECHNICAL', 'memory-leak, android, ios, arc, weak-reference, leakcanary, instruments', TRUE, NOW()),
((SELECT id FROM t), 'Explain the Coordinator pattern in iOS navigation.', 'HARD', 'TECHNICAL', 'coordinator, ios, navigation, uinavigationcontroller, flow, dependency-injection', TRUE, NOW()),
((SELECT id FROM t), 'How would you implement push notifications for both Android and iOS?', 'EASY', 'PROJECT', 'push-notification, fcm, apns, firebase, notification-channel, permission', TRUE, NOW());

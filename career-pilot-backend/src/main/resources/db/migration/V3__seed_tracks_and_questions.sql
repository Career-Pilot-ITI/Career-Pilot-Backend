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
((SELECT id FROM t), 'Explain dependency injection and why it is useful.', 'MEDIUM', 'TECHNICAL', 'dependency-injection, ioc, spring, loose-coupling, testability', TRUE, NOW());

-- Senior Backend Engineer questions
WITH t AS (SELECT id FROM tracks WHERE name = 'Senior Backend Engineer')
INSERT INTO question_bank (track_id, question_text, difficulty_level, category, expected_keywords, is_active, created_at) VALUES
((SELECT id FROM t), 'How would you design a URL shortener service like bit.ly?', 'MEDIUM', 'PROJECT', 'system-design, url-shortener, hashing, database-sharding, caching', TRUE, NOW()),
((SELECT id FROM t), 'How do you handle database schema migrations in a production system?', 'MEDIUM', 'TECHNICAL', 'flyway, liquibase, migration, rollback, version-control, zero-downtime', TRUE, NOW()),
((SELECT id FROM t), 'Describe how you would design a real-time notification system.', 'HARD', 'PROJECT', 'websocket, message-queue, kafka, push-notification, event-driven', TRUE, NOW()),
((SELECT id FROM t), 'How do you ensure data consistency across microservices?', 'HARD', 'TECHNICAL', 'saga-pattern, distributed-transactions, eventual-consistency, outbox, kafka', TRUE, NOW());

-- Frontend Developer questions
WITH t AS (SELECT id FROM tracks WHERE name = 'Frontend Developer')
INSERT INTO question_bank (track_id, question_text, difficulty_level, category, expected_keywords, is_active, created_at) VALUES
((SELECT id FROM t), 'What is the difference between let, const, and var in JavaScript?', 'EASY', 'TECHNICAL', 'javascript, scoping, hoisting, es6, block-scope', TRUE, NOW()),
((SELECT id FROM t), 'Explain the CSS box model.', 'EASY', 'TECHNICAL', 'css, box-model, margin, padding, border, content', TRUE, NOW()),
((SELECT id FROM t), 'What is the virtual DOM and how does it improve performance?', 'MEDIUM', 'TECHNICAL', 'virtual-dom, react, reconciliation, diffing, batching', TRUE, NOW()),
((SELECT id FROM t), 'Explain state management in React. When would you use Redux?', 'MEDIUM', 'TECHNICAL', 'react, state-management, redux, context-api, props, flux', TRUE, NOW());

-- Data Analyst questions
WITH t AS (SELECT id FROM tracks WHERE name = 'Data Analyst')
INSERT INTO question_bank (track_id, question_text, difficulty_level, category, expected_keywords, is_active, created_at) VALUES
((SELECT id FROM t), 'What is the difference between WHERE and HAVING in SQL?', 'EASY', 'TECHNICAL', 'sql, where, having, group-by, filter, aggregation', TRUE, NOW()),
((SELECT id FROM t), 'Explain the difference between mean, median, and mode.', 'EASY', 'TECHNICAL', 'statistics, mean, median, mode, average, distribution', TRUE, NOW()),
((SELECT id FROM t), 'How would you clean a dataset with missing values?', 'MEDIUM', 'SITUATIONAL', 'data-cleaning, missing-values, imputation, pandas, outlier-detection', TRUE, NOW()),
((SELECT id FROM t), 'Explain the different types of JOINs in SQL with examples.', 'MEDIUM', 'TECHNICAL', 'sql, inner-join, left-join, right-join, full-outer-join, cross-join', TRUE, NOW());

-- DevOps Engineer questions
WITH t AS (SELECT id FROM tracks WHERE name = 'DevOps Engineer')
INSERT INTO question_bank (track_id, question_text, difficulty_level, category, expected_keywords, is_active, created_at) VALUES
((SELECT id FROM t), 'What is the difference between Docker and a virtual machine?', 'EASY', 'TECHNICAL', 'docker, vm, container, hypervisor, isolation, os-kernel', TRUE, NOW()),
((SELECT id FROM t), 'Explain what CI/CD is and why it matters.', 'EASY', 'TECHNICAL', 'ci-cd, jenkins, github-actions, automation, deployment, testing', TRUE, NOW()),
((SELECT id FROM t), 'How would you set up monitoring and alerting for a production application?', 'MEDIUM', 'SITUATIONAL', 'monitoring, prometheus, grafana, alerting, slo, observability', TRUE, NOW()),
((SELECT id FROM t), 'Explain Infrastructure as Code and tools like Terraform.', 'MEDIUM', 'TECHNICAL', 'iac, terraform, ansible, cloudformation, declarative, idempotent', TRUE, NOW());

-- Mobile Developer questions
WITH t AS (SELECT id FROM tracks WHERE name = 'Mobile Developer')
INSERT INTO question_bank (track_id, question_text, difficulty_level, category, expected_keywords, is_active, created_at) VALUES
((SELECT id FROM t), 'What is the difference between an Activity and a Fragment in Android?', 'EASY', 'TECHNICAL', 'android, activity, fragment, lifecycle, ui-component', TRUE, NOW()),
((SELECT id FROM t), 'Explain the delegate pattern in iOS development.', 'EASY', 'TECHNICAL', 'ios, delegate, protocol, uitableview, delegation, swift', TRUE, NOW()),
((SELECT id FROM t), 'How would you handle offline data synchronization in a mobile app?', 'MEDIUM', 'PROJECT', 'offline-first, caching, room, core-data, sync, conflict-resolution', TRUE, NOW()),
((SELECT id FROM t), 'Explain the MVVM architecture pattern and its benefits.', 'MEDIUM', 'TECHNICAL', 'mvvm, architecture, viewmodel, databinding, separation-of-concerns, testability', TRUE, NOW());

CREATE TABLE IF NOT EXISTS contest_meta (
    contest_id VARCHAR(255) PRIMARY KEY,
    title VARCHAR(255),
    start_time TIMESTAMP,
    end_time TIMESTAMP
);

CREATE TABLE IF NOT EXISTS contest_problems (
    id VARCHAR(255) PRIMARY KEY,
    contest_id VARCHAR(255),
    problem_id VARCHAR(255),
    label VARCHAR(10),
    problem_order INT
);

CREATE TABLE IF NOT EXISTS problem_attempts (
    id VARCHAR(255) PRIMARY KEY,
    contest_id VARCHAR(255),
    problem_id VARCHAR(255),
    user_id VARCHAR(255),
    verdict VARCHAR(50),
    submitted_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS leaderboard_entries (
    id VARCHAR(255) PRIMARY KEY,
    contest_id VARCHAR(255),
    user_id VARCHAR(255),
    rank INT,
    solved_count INT,
    total_time BIGINT,
    last_updated TIMESTAMP
);

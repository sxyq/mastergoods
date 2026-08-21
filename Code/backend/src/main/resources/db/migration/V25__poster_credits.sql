-- 海报生成与积分域（多租户：所有表含 owner_user_id + owner-scoped 索引）

-- 用户积分表
CREATE TABLE IF NOT EXISTS user_credits (
    id BIGINT NOT NULL PRIMARY KEY,
    owner_user_id BIGINT NOT NULL,
    balance DECIMAL(12,2) NOT NULL DEFAULT 0,
    total_recharged DECIMAL(12,2) NOT NULL DEFAULT 0,
    total_consumed DECIMAL(12,2) NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    updated_at BIGINT NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_user_credits_owner ON user_credits(owner_user_id);
CREATE INDEX IF NOT EXISTS idx_user_credits_owner ON user_credits(owner_user_id);

-- 积分流水表
CREATE TABLE IF NOT EXISTS credit_transactions (
    id BIGINT NOT NULL PRIMARY KEY,
    owner_user_id BIGINT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    type VARCHAR(20) NOT NULL,
    ref_type VARCHAR(50),
    ref_id BIGINT,
    balance_before DECIMAL(12,2) NOT NULL,
    balance_after DECIMAL(12,2) NOT NULL,
    note VARCHAR(500),
    created_at BIGINT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_credit_transactions_owner ON credit_transactions(owner_user_id, created_at DESC);

-- 海报生成记录表
CREATE TABLE IF NOT EXISTS poster_generations (
    id BIGINT NOT NULL PRIMARY KEY,
    owner_user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    prompt_text TEXT NOT NULL,
    reference_image_asset_ids TEXT,
    result_image_url TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    credits_cost DECIMAL(10,2) NOT NULL DEFAULT 0,
    iteration INT NOT NULL DEFAULT 1,
    parent_generation_id BIGINT,
    created_at BIGINT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_poster_generations_owner ON poster_generations(owner_user_id, created_at DESC);

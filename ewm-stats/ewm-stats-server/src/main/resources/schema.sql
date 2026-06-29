CREATE TABLE IF NOT EXISTS endpoint_hits (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    app VARCHAR(255) NOT NULL,
    uri VARCHAR(512) NOT NULL,
    ip VARCHAR(45) NOT NULL,
    created_on TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_endpoint_hits_created_on ON endpoint_hits(created_on);
CREATE INDEX IF NOT EXISTS idx_endpoint_hits_uri ON endpoint_hits(uri);

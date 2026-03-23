#!/bin/bash
redis-server \
  --bind 0.0.0.0 :: \
  --requirepass "${REDIS_PASSWORD}" \
  --daemonize yes

export ZINC_FIRST_ADMIN_USER="${ZINCSEARCH_USER}"
export ZINC_FIRST_ADMIN_PASSWORD="${ZINCSEARCH_PASSWORD}"
export ZINC_DATA_PATH="/data/zinc"
mkdir -p /data/zinc

exec zincsearch
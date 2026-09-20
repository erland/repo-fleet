# Targeted refresh queue concurrency

## Supported topology

RepoFleet currently supports **one backend application instance**.

The supported deployment profiles all model a single backend consumer:

- root `docker-compose.yml`
- `deploy/docker-compose.server.yml`
- `deploy/coolify/compose.yaml`

The targeted refresh queue is PostgreSQL-backed, persistent across restarts and supports retries/recovery. Its current claim operation in `RepositoryRefreshQueueService.claimNext()` selects the first due `PENDING`/`RETRY` job and then changes that entity to `RUNNING` in the same transaction.

That claim pattern is adequate for the supported single-backend topology. It is **not the supported claim model for multiple backend instances**, because two consumers could select the same due row before either update is visible to the other.

## Scaling gate

Do not horizontally scale the RepoFleet backend until queue claiming has been hardened for multiple consumers.

Before multiple backend instances are supported:

1. replace the current read-then-mutate claim with an atomic PostgreSQL claim, such as:
   - `SELECT ... FOR UPDATE SKIP LOCKED`, or
   - an equivalent atomic conditional `UPDATE ... RETURNING`;
2. add a concurrency test with independent transactions/consumers proving one due job is claimed exactly once;
3. verify retry-attempt counting remains exactly once per successful claim;
4. verify restart recovery remains correct for jobs left in `RUNNING`;
5. update the deployment documentation to explicitly allow more than one backend instance.

Until those conditions are met, the single-backend restriction is part of RepoFleet's supported runtime contract.

## Design-review decision

This resolves design-review finding DR-003 for the current supported topology by making the single-instance assumption explicit.

Implementation-plan step IP-014 (multi-instance atomic queue claiming) is **deferred by design** and becomes mandatory only when horizontal backend scaling is introduced.

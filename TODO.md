## Before 0.1.0:

- Render errors
  - unreachable servers
  - any api call can result in an error
- Process control
  - start   [all]
  - stop    [all]
  - restart [all]
  - clear log
- Better test coverage!
  - Schemas for api endpoints

## Planned features

- Tail stdout/stderr via websockets
  - use chord library?
- Drill-down process detail
- Clientside Filtering
  - by app
  - by host

## Other ideas

- in memory caching (reduce api requests for concurrent users)
- public api
- auth
- optional supervisord discovery
  - serf?

## Before 0.1.0:

- Render Messages/Errors
  - any api call can result in an error
  - components should be able to bubble up errors/messages

- Process control
  - start-all
  - stop-all
  - restart
  - restart-all
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
- api for service discovery/live configuration
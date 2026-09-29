# TicketRush 🎟️

A ticket booking platform (like BookMyShow), built as microservices to demonstarateall the core concepts of backend engineering.

## Services

| Service | Tech | Job |
|---|---|---|
| user-service | Django | signup, login, JWT |
| catalog-service | FastAPI | movies, theatres, shows |
| booking-service | FastAPI | seat booking |
| payment-service | Spring Boot | payments |
| notification-service | Spring Boot | ticket emails |

## Folders

- `services/` – one folder per microservice
- `learning/` – experiments, like the raw HTTP server
# Feasto PostgreSQL setup

The four persistence services use separate PostgreSQL databases. The API
Gateway and Eureka discovery server do not use a database.

## Requirements

- PostgreSQL server reachable from the services
- Java 21 or later
- Network access to Maven Central on the first wrapper/build run

The services use the PostgreSQL JDBC driver and Hibernate schema mode `update`.
This mode does not drop existing tables, but may add or adjust schema objects at
startup. Back up databases before deploying schema changes. The Restaurant
Service previously used `create`, which can replace existing tables; it now
uses `update` to preserve existing data.

## Database names and defaults

| Service | Database | Default JDBC endpoint |
|---|---|---|
| User | `feasto_user_db` | `localhost:5432` |
| Restaurant | `feasto_restaurant` | `localhost:5432` |
| Order | `feasto_order` | `localhost:5432` |
| Payment | `feasto_payment` | `localhost:5432` |

These are separate databases. Override a service's URL if your PostgreSQL
server uses another port.

To create any missing database without dropping or resetting existing
databases, connect to a maintenance database with `psql` and run the matching
statement. PostgreSQL does not support `CREATE DATABASE IF NOT EXISTS`:

```sql
SELECT 'CREATE DATABASE feasto_user_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'feasto_user_db')
\gexec

SELECT 'CREATE DATABASE feasto_restaurant'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'feasto_restaurant')
\gexec

SELECT 'CREATE DATABASE feasto_order'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'feasto_order')
\gexec

SELECT 'CREATE DATABASE feasto_payment'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'feasto_payment')
\gexec
```

`\gexec` is a `psql` command. These statements create only databases that do not
already exist; they do not alter or reset existing databases.

## Configure credentials

Set the database username and password in each persistence service's shell
before starting it. Do not put database passwords in source files:

```bash
export FEASTO_DB_USERNAME='postgres'
export FEASTO_DB_PASSWORD='your-local-postgres-password'
```

The username defaults to `postgres`; `FEASTO_DB_PASSWORD` is required. Set
service-specific URL variables when your host, port, or database name differs
from the defaults:

```bash
export FEASTO_USERS_DB_URL='jdbc:postgresql://localhost:5432/feasto_user_db'
export FEASTO_RESTAURANTS_DB_URL='jdbc:postgresql://localhost:5432/feasto_restaurant'
export FEASTO_ORDERS_DB_URL='jdbc:postgresql://localhost:5432/feasto_order'
export FEASTO_PAYMENTS_DB_URL='jdbc:postgresql://localhost:5432/feasto_payment'
```

The URLs above show the default endpoint for each service.

## Configure JWT and service credentials

Generate one high-entropy JWT signing secret and use the same value in the
Gateway and all four persistence services. The secret is Base64-encoded and
must decode to at least 32 random bytes:

```bash
export FEASTO_JWT_SECRET="$(openssl rand -base64 32)"
```

The Order Service also calls the Payment Service directly. Configure the same
separate internal-service token in both services:

```bash
export FEASTO_INTERNAL_SERVICE_TOKEN="$(openssl rand -hex 32)"
```

Only the Order Service attaches that token to its Payment Service calls. The
Gateway removes externally supplied identity and internal-service headers.
Never reuse the JWT signing key as the internal-service token.

Public registration always creates a `CUSTOMER`. To bootstrap the initial
`SUPER_ADMIN`, optionally set `FEASTO_INITIAL_ADMIN_EMAIL`,
`FEASTO_INITIAL_ADMIN_PHONE` (10 digits), and a
`FEASTO_INITIAL_ADMIN_PASSWORD` (at least 12 characters) in the User Service
environment. The bootstrap runs only when that email does not exist and never
changes an existing account's password. Remove the bootstrap variables after
the first successful start.

Earlier versions of UserService created or reset `admin@feasto.com` to a
known default password. This version no longer does that, but it does not
change passwords already stored in PostgreSQL. If that account exists from an
earlier run, rotate its password through a trusted administrative process
before exposing the service.

`CUSTOMER` is the only role available through public registration.
`SUPER_ADMIN` can perform administrative operations across services.
`REGIONAL_MANAGER` retains its existing region-scoped user-management
permissions; because Restaurant and Order do not model corresponding region
ownership, their administrative operations are limited to `SUPER_ADMIN`.

Swagger exposes HTTP Bearer JWT authorization for User, Restaurant, Order, and
Payment APIs. Register and log in through User Service, then paste the returned
token into Swagger's **Authorize** dialog.

## Start services

Start each command in its own terminal. Start Eureka before services that
register with it. Export credentials in each persistence service terminal:

```bash
cd EurekaService && sh ./mvnw spring-boot:run
cd UserService && sh ./mvnw spring-boot:run
cd RestaurantService && sh ./mvnw spring-boot:run
cd OrderService && sh ./mvnw spring-boot:run
cd PaymentService && sh ../EurekaService/mvnw -f "$PWD/pom.xml" spring-boot:run
cd APIGateway && sh ../EurekaService/mvnw -f "$PWD/pom.xml" spring-boot:run
```

APIGateway and PaymentService use the EurekaService Maven wrapper because they
do not include their own wrapper.

## Tests and builds

The User, Restaurant, and Order context tests configure isolated in-memory H2
databases. They do not connect to PostgreSQL or verify production CRUD
persistence. Run each service independently:

```bash
(cd APIGateway && sh ../EurekaService/mvnw -f "$PWD/pom.xml" clean test)
(cd EurekaService && sh ./mvnw clean test)
(cd UserService && sh ./mvnw clean test)
(cd RestaurantService && sh ./mvnw clean test)
(cd OrderService && sh ./mvnw clean test)
(cd PaymentService && sh ../EurekaService/mvnw -f "$PWD/pom.xml" clean test)
```

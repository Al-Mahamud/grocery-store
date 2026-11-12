-- Text messages (an outbox sent in the background) and one-time codes that prove a phone number.

create table sms_messages (
    id          bigint generated always as identity primary key,
    phone       text        not null,
    kind        text        not null check (kind in ('CODE', 'ORDER_PLACED', 'ORDER_ON_THE_WAY', 'ORDER_CANCELLED')),
    body        text        not null,
    -- Codes are masked in the body once sent, so they can't be read back later.
    sensitive   boolean     not null default false,
    order_id    bigint references orders (id) on delete set null,
    status      text        not null default 'QUEUED' check (status in ('QUEUED', 'SENDING', 'SENT', 'FAILED')),
    attempts    integer     not null default 0,
    provider    text,
    provider_id text,
    error       text,
    created_at  timestamptz not null default now(),
    sent_at     timestamptz
);
create index sms_queue on sms_messages (created_at) where status in ('QUEUED', 'SENDING');
create index sms_recent on sms_messages (created_at desc);

create table phone_codes (
    id         bigint generated always as identity primary key,
    phone      text        not null,
    purpose    text        not null check (purpose in ('REGISTER', 'RESET')),
    code_hash  text        not null,
    attempts   integer     not null default 0,
    expires_at timestamptz not null,
    used_at    timestamptz,
    created_at timestamptz not null default now()
);
create index phone_codes_lookup on phone_codes (phone, purpose, created_at desc);

-- Accounts made before codes existed have no proven number.
alter table customers add column phone_verified_at timestamptz;

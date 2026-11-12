-- The admin area: staff accounts, per-day slot capacity, and refunds of online payments.

create table staff (
    id             bigint generated always as identity primary key,
    name           text        not null,
    phone          text        not null unique check (phone ~ '^01[3-9][0-9]{8}$'),
    password_hash  text        not null,
    active         boolean     not null default true,
    created_at     timestamptz not null default now(),
    last_signed_in timestamptz
);

-- A slot's capacity on one day, when it differs from the usual (store.slots.capacity). 0 closes it.
create table slot_capacity (
    slot_date  date        not null,
    slot_start time        not null,
    capacity   integer     not null check (capacity between 0 and 500),
    note       text,
    updated_at timestamptz not null default now(),
    primary key (slot_date, slot_start)
);

alter table orders add column refunded_at timestamptz;
alter table orders add column refund_reference text;
create index orders_refund_due on orders (updated_at) where status = 'CANCELLED' and payment_status = 'PAID';
create index orders_created on orders (created_at desc);
create index packs_low_stock on packs (stock);

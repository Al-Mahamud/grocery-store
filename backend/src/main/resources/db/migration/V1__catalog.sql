-- Catalog: two-level categories and products sold in packs. Prices are whole taka.

create table categories (
    id        bigint generated always as identity primary key,
    parent_id bigint references categories (id),
    name      text    not null,
    slug      text    not null unique,
    icon      text    not null default 'basket',   -- icon name the web app understands
    color     text    not null default '#FFE8D6' check (color ~ '^#[0-9A-Fa-f]{6}$'),
    image_url text,
    position  integer not null default 0
);
create index categories_parent on categories (parent_id);

create table products (
    id          bigint generated always as identity primary key,
    name        text        not null,
    local_name  text,                               -- Bangla name, e.g. টমেটো
    slug        text        not null unique,
    category_id bigint      not null references categories (id),
    brand       text,
    description text        not null default '',
    storage     text,                               -- "Keep refrigerated"
    origin      text,                               -- "Jessore, Bangladesh"
    organic     boolean     not null default false,
    local       boolean     not null default false,
    fresh       boolean     not null default false, -- arrives daily
    weighed     boolean     not null default false, -- final weight may differ slightly
    image_url   text,
    status      text        not null default 'DRAFT' check (status in ('DRAFT', 'ACTIVE', 'ARCHIVED')),
    featured    boolean     not null default false,
    sold_count  integer     not null default 0,
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now(),
    search      tsvector generated always as (
        setweight(to_tsvector('simple', name), 'A') ||
        setweight(to_tsvector('simple', coalesce(brand, '')), 'B') ||
        setweight(to_tsvector('simple', description), 'C')) stored
);
create index products_category on products (category_id);
create index products_status_sold on products (status, sold_count desc);
create index products_search on products using gin (search);

-- A pack is what goes in the bag: "1 kg", "500 ml", "12 pcs".
create table packs (
    id               bigint generated always as identity primary key,
    product_id       bigint        not null references products (id) on delete cascade,
    label            text          not null,
    amount           numeric(10,3) not null check (amount > 0),
    unit             text          not null check (unit in ('G', 'KG', 'ML', 'L', 'PCS')),
    sku              text          not null unique,
    price            integer       not null check (price > 0),
    compare_at_price integer check (compare_at_price is null or compare_at_price > price),
    stock            integer       not null default 0 check (stock >= 0),
    max_per_order    integer       not null default 20 check (max_per_order between 1 and 99),
    position         integer       not null default 0,
    unique (product_id, label)
);
create index packs_product on packs (product_id, position);

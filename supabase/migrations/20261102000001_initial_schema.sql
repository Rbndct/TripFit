-- TripFit canonical schema (API-Integration-Plan.md §3)
create extension if not exists pgcrypto;

-- Pipeline-owned, shared reference data
create table cities (
    id uuid primary key default gen_random_uuid(),
    name text not null,
    country text not null,
    lat double precision not null,
    lng double precision not null,
    bbox jsonb,
    sources_json jsonb,
    updated_at timestamptz not null default now(),
    unique (name, country)
);

create table places (
    id uuid primary key default gen_random_uuid(),
    city_id uuid not null references cities(id) on delete cascade,
    name text not null,
    category text,
    lat double precision not null,
    lng double precision not null,
    address text,
    price_tier smallint,
    rating numeric,
    is_hidden_gem boolean not null default false,
    description text,
    image_url text,
    wikidata_id text,
    osm_id text,
    sources_json jsonb,
    cleaned_at timestamptz
);
create index places_city_id_idx on places(city_id);
create unique index places_wikidata_id_idx on places(wikidata_id) where wikidata_id is not null;

-- Per-user app data
create table trips (
    id uuid primary key default gen_random_uuid(),
    user_id uuid references auth.users(id) on delete cascade,
    name text not null,
    start_date date,
    end_date date,
    home_currency text not null default 'USD',
    budget numeric,
    created_at timestamptz not null default now()
);
create index trips_user_id_idx on trips(user_id);

create table itinerary_items (
    id uuid primary key default gen_random_uuid(),
    trip_id uuid not null references trips(id) on delete cascade,
    place_id uuid references places(id),
    custom_name text,
    day int not null,
    time time,
    duration_min int,
    is_booked boolean not null default false,
    transport_mode text check (transport_mode in ('walk', 'cycle', 'drive')),
    buffer_min int,
    position int not null default 0,
    created_at timestamptz not null default now(),
    constraint itinerary_items_place_or_name check (place_id is not null or custom_name is not null)
);
create index itinerary_items_trip_id_idx on itinerary_items(trip_id);

create table expenses (
    id uuid primary key default gen_random_uuid(),
    trip_id uuid not null references trips(id) on delete cascade,
    itinerary_item_id uuid references itinerary_items(id) on delete set null,
    amount numeric not null,
    currency text not null,
    converted_amount numeric,
    category text,
    created_at timestamptz not null default now()
);
create index expenses_trip_id_idx on expenses(trip_id);

-- RLS
alter table cities enable row level security;
alter table places enable row level security;
alter table trips enable row level security;
alter table itinerary_items enable row level security;
alter table expenses enable row level security;

-- cities/places: public read, writes only via service_role (pipeline), which bypasses RLS.
create policy cities_public_read on cities for select using (true);
create policy places_public_read on places for select using (true);

-- trips: owner-only CRUD.
create policy trips_owner_select on trips for select using (auth.uid() = user_id);
create policy trips_owner_insert on trips for insert with check (auth.uid() = user_id);
create policy trips_owner_update on trips for update using (auth.uid() = user_id);
create policy trips_owner_delete on trips for delete using (auth.uid() = user_id);

-- itinerary_items: scoped through the parent trip's ownership.
create policy itinerary_items_owner_select on itinerary_items for select using (
    exists (select 1 from trips t where t.id = itinerary_items.trip_id and t.user_id = auth.uid())
);
create policy itinerary_items_owner_insert on itinerary_items for insert with check (
    exists (select 1 from trips t where t.id = itinerary_items.trip_id and t.user_id = auth.uid())
);
create policy itinerary_items_owner_update on itinerary_items for update using (
    exists (select 1 from trips t where t.id = itinerary_items.trip_id and t.user_id = auth.uid())
);
create policy itinerary_items_owner_delete on itinerary_items for delete using (
    exists (select 1 from trips t where t.id = itinerary_items.trip_id and t.user_id = auth.uid())
);

-- expenses: scoped through the parent trip's ownership.
create policy expenses_owner_select on expenses for select using (
    exists (select 1 from trips t where t.id = expenses.trip_id and t.user_id = auth.uid())
);
create policy expenses_owner_insert on expenses for insert with check (
    exists (select 1 from trips t where t.id = expenses.trip_id and t.user_id = auth.uid())
);
create policy expenses_owner_update on expenses for update using (
    exists (select 1 from trips t where t.id = expenses.trip_id and t.user_id = auth.uid())
);
create policy expenses_owner_delete on expenses for delete using (
    exists (select 1 from trips t where t.id = expenses.trip_id and t.user_id = auth.uid())
);

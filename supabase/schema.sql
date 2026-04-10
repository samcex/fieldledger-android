create table if not exists public.profiles (
  user_id uuid primary key references auth.users (id) on delete cascade,
  email text,
  company_name text not null default '',
  currency_code text not null default 'USD',
  theme_mode text not null default 'light',
  updated_at timestamptz not null default timezone('utc', now())
);

create table if not exists public.jobs (
  id text primary key,
  user_id uuid not null references auth.users (id) on delete cascade,
  payload jsonb not null default '{}'::jsonb,
  updated_at timestamptz not null default timezone('utc', now())
);

create index if not exists jobs_user_updated_idx
  on public.jobs (user_id, updated_at desc);

alter table public.profiles enable row level security;
alter table public.jobs enable row level security;

create policy "profiles_select_own"
  on public.profiles
  for select
  using (auth.uid() = user_id);

create policy "profiles_insert_own"
  on public.profiles
  for insert
  with check (auth.uid() = user_id);

create policy "profiles_update_own"
  on public.profiles
  for update
  using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

create policy "profiles_delete_own"
  on public.profiles
  for delete
  using (auth.uid() = user_id);

create policy "jobs_select_own"
  on public.jobs
  for select
  using (auth.uid() = user_id);

create policy "jobs_insert_own"
  on public.jobs
  for insert
  with check (auth.uid() = user_id);

create policy "jobs_update_own"
  on public.jobs
  for update
  using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

create policy "jobs_delete_own"
  on public.jobs
  for delete
  using (auth.uid() = user_id);

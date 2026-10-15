-- READ-ONLY preflight; psql variable expected_port must be explicitly supplied.
-- Allowed review targets: native loopback PostgreSQL, kilivana_app/kilivana only.
SELECT set_config('search_path','public,pg_catalog',false);
SELECT set_config('kilivana.schema_review_expected_port', :'expected_port', false);
DO $$
DECLARE actual_signature TEXT;
BEGIN
    IF current_database() <> 'kilivana' OR current_user <> 'kilivana_app'
       OR inet_server_addr() IS NULL OR host(inet_server_addr()) NOT IN ('127.0.0.1','::1')
       OR inet_server_port()::text IS DISTINCT FROM current_setting('kilivana.schema_review_expected_port')
       OR current_setting('kilivana.schema_review_expected_port') NOT IN ('5432','55432') THEN
        RAISE EXCEPTION 'Unexpected database identity or port; no migration permitted';
    END IF;
    IF NOT has_schema_privilege('public','CREATE') OR NOT has_schema_privilege('public','USAGE')
       OR NOT has_database_privilege(current_database(),'TEMP')
       OR NOT has_table_privilege('public.orders','REFERENCES') THEN
        RAISE EXCEPTION 'Missing schema/TEMP/REFERENCES permissions; do not escalate';
    END IF;
    IF EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
               WHERE n.nspname='public' AND c.relkind IN ('r','S')
                 AND NOT pg_has_role(c.relowner,'USAGE')) THEN
        RAISE EXCEPTION 'Application role does not own every application table/sequence';
    END IF;
    IF (SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
        WHERE n.nspname='public' AND c.relkind='r') <> 35 THEN
        RAISE EXCEPTION 'Expected the reviewed 35-table baseline; stop on schema drift';
    END IF;
    actual_signature := (SELECT md5(string_agg(line,E'\n' ORDER BY line)) AS schema_signature FROM (
SELECT concat_ws('|','C',c.relname,a.attname,format_type(a.atttypid,a.atttypmod),a.attnotnull::text,coalesce(pg_get_expr(d.adbin,d.adrelid),'')) line FROM pg_attribute a JOIN pg_class c ON c.oid=a.attrelid JOIN pg_namespace n ON n.oid=c.relnamespace LEFT JOIN pg_attrdef d ON d.adrelid=a.attrelid AND d.adnum=a.attnum WHERE n.nspname='public' AND c.relkind='r' AND a.attnum>0 AND NOT a.attisdropped
UNION ALL SELECT concat_ws('|','K',c.relname,k.conname,k.contype,k.convalidated::text,pg_get_constraintdef(k.oid)) FROM pg_constraint k JOIN pg_class c ON c.oid=k.conrelid JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='public'
UNION ALL SELECT concat_ws('|','I',c.relname,i.relname,x.indisvalid::text,pg_get_indexdef(i.oid)) FROM pg_index x JOIN pg_class c ON c.oid=x.indrelid JOIN pg_class i ON i.oid=x.indexrelid JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='public'
UNION ALL SELECT concat_ws('|','S',sequencename,data_type,increment_by,start_value,min_value,max_value,cycle::text) FROM pg_sequences WHERE schemaname='public'
) shape);
    -- PostgreSQL18 dump/restore redistributes equivalent enum-array casts.
    -- Both exact signatures were reviewed; all other structural drift is rejected.
    IF actual_signature NOT IN ('7a3b48703b3f4a7e711a98bc08f0aca6', '7152b9b7de31b4f352c52970d34e5a0e') THEN
        RAISE EXCEPTION 'Schema differs from reviewed baseline; do not overwrite or replay migrations';
    END IF;
    IF EXISTS (SELECT 1 FROM public.order_items oi LEFT JOIN public.orders o ON o.id=oi.order_id
               WHERE oi.order_id IS NULL OR o.id IS NULL) THEN
        RAISE EXCEPTION 'Unlinked order lines require review, not invented parent records';
    END IF;
    IF EXISTS (SELECT 1 FROM public.payments WHERE status NOT IN ('PENDING','PAID','HELD','SETTLED','FAILED','REFUNDED'))
       OR EXISTS (SELECT 1 FROM public.orders WHERE payment_status NOT IN ('PENDING','PAID','HELD','SETTLED','FAILED','REFUNDED')) THEN
        RAISE EXCEPTION 'Unrecognized payment state; preserve data and stop';
    END IF;
END $$;
SELECT current_database() database,current_user username,inet_server_addr() host,inet_server_port() port;

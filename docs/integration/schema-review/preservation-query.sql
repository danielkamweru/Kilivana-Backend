-- Read-only aggregate fingerprints. No raw records, password hashes, images or identifiers are emitted.
SELECT 'table:addresses'::text object_name,count(*)::bigint row_count,md5(coalesce(string_agg(md5((to_jsonb(r)-ARRAY[]::text[])::text),'' ORDER BY r.id),'')) fingerprint FROM public.addresses r
UNION ALL
SELECT 'table:audit_logs'::text object_name,count(*)::bigint row_count,md5(coalesce(string_agg(md5((to_jsonb(r)-ARRAY[]::text[])::text),'' ORDER BY r.id),'')) fingerprint FROM public.audit_logs r
UNION ALL
SELECT 'table:buyer_profiles'::text object_name,count(*)::bigint row_count,md5(coalesce(string_agg(md5((to_jsonb(r)-ARRAY[]::text[])::text),'' ORDER BY r.id),'')) fingerprint FROM public.buyer_profiles r
UNION ALL
SELECT 'table:cart_items'::text object_name,count(*)::bigint row_count,md5(coalesce(string_agg(md5((to_jsonb(r)-ARRAY[]::text[])::text),'' ORDER BY r.id),'')) fingerprint FROM public.cart_items r
UNION ALL
SELECT 'table:carts'::text object_name,count(*)::bigint row_count,md5(coalesce(string_agg(md5((to_jsonb(r)-ARRAY[]::text[])::text),'' ORDER BY r.id),'')) fingerprint FROM public.carts r
UNION ALL
SELECT 'table:categories'::text object_name,count(*)::bigint row_count,md5(coalesce(string_agg(md5((to_jsonb(r)-ARRAY[]::text[])::text),'' ORDER BY r.id),'')) fingerprint FROM public.categories r
UNION ALL
SELECT 'table:category_images'::text object_name,count(*)::bigint row_count,md5(coalesce(string_agg(md5((to_jsonb(r)-ARRAY[]::text[])::text),'' ORDER BY r.id),'')) fingerprint FROM public.category_images r
UNION ALL
SELECT 'table:crop_types'::text object_name,count(*)::bigint row_count,md5(coalesce(string_agg(md5((to_jsonb(r)-ARRAY[]::text[])::text),'' ORDER BY r.id),'')) fingerprint FROM public.crop_types r
UNION ALL
SELECT 'table:crops'::text object_name,count(*)::bigint row_count,md5(coalesce(string_agg(md5((to_jsonb(r)-ARRAY[]::text[])::text),'' ORDER BY r.id),'')) fingerprint FROM public.crops r
UNION ALL
SELECT 'table:disputes'::text object_name,count(*)::bigint row_count,md5(coalesce(string_agg(md5((to_jsonb(r)-ARRAY[]::text[])::text),'' ORDER BY r.id),'')) fingerprint FROM public.disputes r
UNION ALL
SELECT 'table:driver_profile_images'::text object_name,count(*)::bigint row_count,md5(coalesce(string_agg(md5((to_jsonb(r)-ARRAY[]::text[])::text),'' ORDER BY r.id),'')) fingerprint FROM public.driver_profile_images r
UNION ALL
SELECT 'table:driver_profiles'::text object_name,count(*)::bigint row_count,md5(coalesce(string_agg(md5((to_jsonb(r)-ARRAY[]::text[])::text),'' ORDER BY r.id),'')) fingerprint FROM public.driver_profiles r
UNION ALL
SELECT 'table:farm_images'::text object_name,count(*)::bigint row_count,md5(coalesce(string_agg(md5((to_jsonb(r)-ARRAY[]::text[])::text),'' ORDER BY r.id),'')) fingerprint FROM public.farm_images r
UNION ALL
SELECT 'table:farmer_profile_images'::text object_name,count(*)::bigint row_count,md5(coalesce(string_agg(md5((to_jsonb(r)-ARRAY[]::text[])::text),'' ORDER BY r.id),'')) fingerprint FROM public.farmer_profile_images r
UNION ALL
SELECT 'table:farmer_profiles'::text object_name,count(*)::bigint row_count,md5(coalesce(string_agg(md5((to_jsonb(r)-ARRAY[]::text[])::text),'' ORDER BY r.id),'')) fingerprint FROM public.farmer_profiles r
UNION ALL
SELECT 'table:farms'::text object_name,count(*)::bigint row_count,md5(coalesce(string_agg(md5((to_jsonb(r)-ARRAY[]::text[])::text),'' ORDER BY r.id),'')) fingerprint FROM public.farms r
UNION ALL
SELECT 'table:inspection_evidence_images'::text object_name,count(*)::bigint row_count,md5(coalesce(string_agg(md5((to_jsonb(r)-ARRAY[]::text[])::text),'' ORDER BY r.id),'')) fingerprint FROM public.inspection_evidence_images r
UNION ALL
SELECT 'table:inspections'::text object_name,count(*)::bigint row_count,md5(coalesce(string_agg(md5((to_jsonb(r)-ARRAY[]::text[])::text),'' ORDER BY r.id),'')) fingerprint FROM public.inspections r
UNION ALL
SELECT 'table:inspector_profile_images'::text object_name,count(*)::bigint row_count,md5(coalesce(string_agg(md5((to_jsonb(r)-ARRAY[]::text[])::text),'' ORDER BY r.id),'')) fingerprint FROM public.inspector_profile_images r
UNION ALL
SELECT 'table:inspector_profiles'::text object_name,count(*)::bigint row_count,md5(coalesce(string_agg(md5((to_jsonb(r)-ARRAY[]::text[])::text),'' ORDER BY r.id),'')) fingerprint FROM public.inspector_profiles r
UNION ALL
SELECT 'table:kyc_documents'::text object_name,count(*)::bigint row_count,md5(coalesce(string_agg(md5((to_jsonb(r)-ARRAY[]::text[])::text),'' ORDER BY r.id),'')) fingerprint FROM public.kyc_documents r
UNION ALL
SELECT 'table:logistics_jobs'::text object_name,count(*)::bigint row_count,md5(coalesce(string_agg(md5((to_jsonb(r)-ARRAY[]::text[])::text),'' ORDER BY r.id),'')) fingerprint FROM public.logistics_jobs r
UNION ALL
SELECT 'table:notifications'::text object_name,count(*)::bigint row_count,md5(coalesce(string_agg(md5((to_jsonb(r)-ARRAY[]::text[])::text),'' ORDER BY r.id),'')) fingerprint FROM public.notifications r
UNION ALL
SELECT 'table:order_events'::text object_name,count(*)::bigint row_count,md5(coalesce(string_agg(md5((to_jsonb(r)-ARRAY[]::text[])::text),'' ORDER BY r.id),'')) fingerprint FROM public.order_events r
UNION ALL
SELECT 'table:order_items'::text object_name,count(*)::bigint row_count,md5(coalesce(string_agg(md5((to_jsonb(r)-ARRAY[]::text[])::text),'' ORDER BY r.id),'')) fingerprint FROM public.order_items r
UNION ALL
SELECT 'table:orders'::text object_name,count(*)::bigint row_count,md5(coalesce(string_agg(md5((to_jsonb(r)-ARRAY[]::text[])::text),'' ORDER BY r.id),'')) fingerprint FROM public.orders r
UNION ALL
SELECT 'table:payments'::text object_name,count(*)::bigint row_count,md5(coalesce(string_agg(md5((to_jsonb(r)-ARRAY['merchant_request_id','checkout_request_id','phone_number','mpesa_receipt_number','transaction_date','callback_status']::text[])::text),'' ORDER BY r.id),'')) fingerprint FROM public.payments r
UNION ALL
SELECT 'table:product_images'::text object_name,count(*)::bigint row_count,md5(coalesce(string_agg(md5((to_jsonb(r)-ARRAY[]::text[])::text),'' ORDER BY r.id),'')) fingerprint FROM public.product_images r
UNION ALL
SELECT 'table:products'::text object_name,count(*)::bigint row_count,md5(coalesce(string_agg(md5((to_jsonb(r)-ARRAY['farm_id','harvest_date','last_inspection_id','low_stock_threshold','quality_grade','status_changed_at','status_changed_by','status_reason','verified_at','verified_by','verified_qty']::text[])::text),'' ORDER BY r.id),'')) fingerprint FROM public.products r
UNION ALL
SELECT 'table:proof_of_deliveries'::text object_name,count(*)::bigint row_count,md5(coalesce(string_agg(md5((to_jsonb(r)-ARRAY[]::text[])::text),'' ORDER BY r.id),'')) fingerprint FROM public.proof_of_deliveries r
UNION ALL
SELECT 'table:stored_images'::text object_name,count(*)::bigint row_count,md5(coalesce(string_agg(md5((to_jsonb(r)-ARRAY[]::text[])::text),'' ORDER BY r.id),'')) fingerprint FROM public.stored_images r
UNION ALL
SELECT 'table:supplier_profile_images'::text object_name,count(*)::bigint row_count,md5(coalesce(string_agg(md5((to_jsonb(r)-ARRAY[]::text[])::text),'' ORDER BY r.id),'')) fingerprint FROM public.supplier_profile_images r
UNION ALL
SELECT 'table:supplier_profiles'::text object_name,count(*)::bigint row_count,md5(coalesce(string_agg(md5((to_jsonb(r)-ARRAY[]::text[])::text),'' ORDER BY r.id),'')) fingerprint FROM public.supplier_profiles r
UNION ALL
SELECT 'table:tracking_events'::text object_name,count(*)::bigint row_count,md5(coalesce(string_agg(md5((to_jsonb(r)-ARRAY[]::text[])::text),'' ORDER BY r.id),'')) fingerprint FROM public.tracking_events r
UNION ALL
SELECT 'table:users'::text object_name,count(*)::bigint row_count,md5(coalesce(string_agg(md5((to_jsonb(r)-ARRAY[]::text[])::text),'' ORDER BY r.id),'')) fingerprint FROM public.users r
UNION ALL
SELECT 'sequence:addresses_id_seq'::text,1::bigint,md5(last_value::text||'|'||is_called::text) FROM public.addresses_id_seq
UNION ALL
SELECT 'sequence:audit_logs_id_seq'::text,1::bigint,md5(last_value::text||'|'||is_called::text) FROM public.audit_logs_id_seq
UNION ALL
SELECT 'sequence:buyer_profiles_id_seq'::text,1::bigint,md5(last_value::text||'|'||is_called::text) FROM public.buyer_profiles_id_seq
UNION ALL
SELECT 'sequence:cart_items_id_seq'::text,1::bigint,md5(last_value::text||'|'||is_called::text) FROM public.cart_items_id_seq
UNION ALL
SELECT 'sequence:carts_id_seq'::text,1::bigint,md5(last_value::text||'|'||is_called::text) FROM public.carts_id_seq
UNION ALL
SELECT 'sequence:categories_id_seq'::text,1::bigint,md5(last_value::text||'|'||is_called::text) FROM public.categories_id_seq
UNION ALL
SELECT 'sequence:category_images_id_seq'::text,1::bigint,md5(last_value::text||'|'||is_called::text) FROM public.category_images_id_seq
UNION ALL
SELECT 'sequence:crop_types_id_seq'::text,1::bigint,md5(last_value::text||'|'||is_called::text) FROM public.crop_types_id_seq
UNION ALL
SELECT 'sequence:crops_id_seq'::text,1::bigint,md5(last_value::text||'|'||is_called::text) FROM public.crops_id_seq
UNION ALL
SELECT 'sequence:disputes_id_seq'::text,1::bigint,md5(last_value::text||'|'||is_called::text) FROM public.disputes_id_seq
UNION ALL
SELECT 'sequence:driver_profile_images_id_seq'::text,1::bigint,md5(last_value::text||'|'||is_called::text) FROM public.driver_profile_images_id_seq
UNION ALL
SELECT 'sequence:driver_profiles_id_seq'::text,1::bigint,md5(last_value::text||'|'||is_called::text) FROM public.driver_profiles_id_seq
UNION ALL
SELECT 'sequence:farm_images_id_seq'::text,1::bigint,md5(last_value::text||'|'||is_called::text) FROM public.farm_images_id_seq
UNION ALL
SELECT 'sequence:farmer_profile_images_id_seq'::text,1::bigint,md5(last_value::text||'|'||is_called::text) FROM public.farmer_profile_images_id_seq
UNION ALL
SELECT 'sequence:farmer_profiles_id_seq'::text,1::bigint,md5(last_value::text||'|'||is_called::text) FROM public.farmer_profiles_id_seq
UNION ALL
SELECT 'sequence:farms_id_seq'::text,1::bigint,md5(last_value::text||'|'||is_called::text) FROM public.farms_id_seq
UNION ALL
SELECT 'sequence:inspection_evidence_images_id_seq'::text,1::bigint,md5(last_value::text||'|'||is_called::text) FROM public.inspection_evidence_images_id_seq
UNION ALL
SELECT 'sequence:inspections_id_seq'::text,1::bigint,md5(last_value::text||'|'||is_called::text) FROM public.inspections_id_seq
UNION ALL
SELECT 'sequence:inspector_profile_images_id_seq'::text,1::bigint,md5(last_value::text||'|'||is_called::text) FROM public.inspector_profile_images_id_seq
UNION ALL
SELECT 'sequence:inspector_profiles_id_seq'::text,1::bigint,md5(last_value::text||'|'||is_called::text) FROM public.inspector_profiles_id_seq
UNION ALL
SELECT 'sequence:kyc_documents_id_seq'::text,1::bigint,md5(last_value::text||'|'||is_called::text) FROM public.kyc_documents_id_seq
UNION ALL
SELECT 'sequence:logistics_jobs_id_seq'::text,1::bigint,md5(last_value::text||'|'||is_called::text) FROM public.logistics_jobs_id_seq
UNION ALL
SELECT 'sequence:notifications_id_seq'::text,1::bigint,md5(last_value::text||'|'||is_called::text) FROM public.notifications_id_seq
UNION ALL
SELECT 'sequence:order_events_id_seq'::text,1::bigint,md5(last_value::text||'|'||is_called::text) FROM public.order_events_id_seq
UNION ALL
SELECT 'sequence:order_items_id_seq'::text,1::bigint,md5(last_value::text||'|'||is_called::text) FROM public.order_items_id_seq
UNION ALL
SELECT 'sequence:orders_id_seq'::text,1::bigint,md5(last_value::text||'|'||is_called::text) FROM public.orders_id_seq
UNION ALL
SELECT 'sequence:payments_id_seq'::text,1::bigint,md5(last_value::text||'|'||is_called::text) FROM public.payments_id_seq
UNION ALL
SELECT 'sequence:product_images_id_seq'::text,1::bigint,md5(last_value::text||'|'||is_called::text) FROM public.product_images_id_seq
UNION ALL
SELECT 'sequence:products_id_seq'::text,1::bigint,md5(last_value::text||'|'||is_called::text) FROM public.products_id_seq
UNION ALL
SELECT 'sequence:proof_of_deliveries_id_seq'::text,1::bigint,md5(last_value::text||'|'||is_called::text) FROM public.proof_of_deliveries_id_seq
UNION ALL
SELECT 'sequence:stored_images_id_seq'::text,1::bigint,md5(last_value::text||'|'||is_called::text) FROM public.stored_images_id_seq
UNION ALL
SELECT 'sequence:supplier_profile_images_id_seq'::text,1::bigint,md5(last_value::text||'|'||is_called::text) FROM public.supplier_profile_images_id_seq
UNION ALL
SELECT 'sequence:supplier_profiles_id_seq'::text,1::bigint,md5(last_value::text||'|'||is_called::text) FROM public.supplier_profiles_id_seq
UNION ALL
SELECT 'sequence:tracking_events_id_seq'::text,1::bigint,md5(last_value::text||'|'||is_called::text) FROM public.tracking_events_id_seq
UNION ALL
SELECT 'sequence:users_id_seq'::text,1::bigint,md5(last_value::text||'|'||is_called::text) FROM public.users_id_seq
UNION ALL
SELECT 'image-large-object-bytes'::text,count(*)::bigint,md5(coalesce(string_agg(md5(lo_get(data)),'' ORDER BY id),'')) FROM public.stored_images
;

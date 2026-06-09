-- ============================================================
-- Production release hardening
-- Run this AFTER the existing setup scripts.
-- It replaces broad development RLS policies and adds server-only
-- wallet debit support for backend checkout.
-- Compatibility note: return-images remains public because the current
-- Android app stores public URLs. Migrate that bucket to private signed
-- URLs before using it for highly sensitive evidence.
-- ============================================================

BEGIN;

ALTER TABLE public.retailers
    ADD COLUMN IF NOT EXISTS is_rejected BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS verification_images TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[];

ALTER TABLE public.delivery_partners
    ADD COLUMN IF NOT EXISTS is_verified BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS is_rejected BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS verification_images TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[];

CREATE OR REPLACE FUNCTION public.is_admin()
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT EXISTS (
        SELECT 1
        FROM public.users
        WHERE id = auth.uid()
          AND role = 'admin'
    );
$$;

CREATE OR REPLACE FUNCTION public.owns_retailer(retailer_uuid UUID)
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT EXISTS (
        SELECT 1
        FROM public.retailers
        WHERE id = retailer_uuid
          AND user_id = auth.uid()
          AND is_verified = true
    );
$$;

CREATE OR REPLACE FUNCTION public.owns_delivery_partner(partner_uuid UUID)
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT EXISTS (
        SELECT 1
        FROM public.delivery_partners
        WHERE user_id = auth.uid()
          AND (id = partner_uuid OR user_id = partner_uuid)
          AND is_verified = true
    );
$$;

CREATE OR REPLACE FUNCTION public.is_verified_retailer()
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT EXISTS (
        SELECT 1
        FROM public.retailers
        WHERE user_id = auth.uid()
          AND is_verified = true
    );
$$;

CREATE OR REPLACE FUNCTION public.is_verified_delivery_partner()
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT EXISTS (
        SELECT 1
        FROM public.delivery_partners
        WHERE user_id = auth.uid()
          AND is_verified = true
    );
$$;

CREATE OR REPLACE FUNCTION public.can_access_delivery_order(
    order_delivery_partner_id UUID,
    order_status TEXT
)
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT
        public.owns_delivery_partner(order_delivery_partner_id)
        OR (
            public.is_verified_delivery_partner()
            AND order_delivery_partner_id IS NULL
            AND order_status IN ('READY_FOR_PICKUP', 'RETURN_APPROVED')
        );
$$;

CREATE OR REPLACE FUNCTION public.uuid_or_null(input_text TEXT)
RETURNS UUID
LANGUAGE plpgsql
IMMUTABLE
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    RETURN input_text::UUID;
EXCEPTION WHEN invalid_text_representation THEN
    RETURN NULL;
END;
$$;

CREATE OR REPLACE FUNCTION public.drop_all_policies(target_schema TEXT, target_table TEXT)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    policy_record RECORD;
BEGIN
    FOR policy_record IN
        SELECT policyname
        FROM pg_policies
        WHERE schemaname = target_schema
          AND tablename = target_table
    LOOP
        EXECUTE format(
            'DROP POLICY IF EXISTS %I ON %I.%I',
            policy_record.policyname,
            target_schema,
            target_table
        );
    END LOOP;
END;
$$;

CREATE OR REPLACE FUNCTION public.prevent_user_role_self_update()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    IF auth.role() <> 'service_role' AND NOT public.is_admin() THEN
        IF NEW.role IS DISTINCT FROM OLD.role THEN
            RAISE EXCEPTION 'Only admins can change user roles';
        END IF;

        IF NEW.email IS DISTINCT FROM OLD.email THEN
            RAISE EXCEPTION 'Only admins can change profile email';
        END IF;
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS prevent_user_role_self_update ON public.users;
CREATE TRIGGER prevent_user_role_self_update
    BEFORE UPDATE OF role, email ON public.users
    FOR EACH ROW EXECUTE FUNCTION public.prevent_user_role_self_update();

CREATE OR REPLACE FUNCTION public.prevent_retailer_self_approval()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    IF auth.role() <> 'service_role' AND NOT public.is_admin() THEN
        IF NEW.is_verified IS DISTINCT FROM OLD.is_verified
           OR NEW.is_rejected IS DISTINCT FROM OLD.is_rejected THEN
            RAISE EXCEPTION 'Only admins can change retailer approval';
        END IF;

        IF NEW.total_revenue IS DISTINCT FROM OLD.total_revenue
           OR NEW.rating IS DISTINCT FROM OLD.rating THEN
            RAISE EXCEPTION 'Only admins can change retailer metrics';
        END IF;
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS prevent_retailer_self_approval ON public.retailers;
CREATE TRIGGER prevent_retailer_self_approval
    BEFORE UPDATE OF is_verified, is_rejected, total_revenue, rating ON public.retailers
    FOR EACH ROW EXECUTE FUNCTION public.prevent_retailer_self_approval();

CREATE OR REPLACE FUNCTION public.prevent_delivery_self_approval()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    IF auth.role() <> 'service_role' AND NOT public.is_admin() THEN
        IF NEW.is_verified IS DISTINCT FROM OLD.is_verified
           OR NEW.is_rejected IS DISTINCT FROM OLD.is_rejected THEN
            RAISE EXCEPTION 'Only admins can change delivery partner approval';
        END IF;

        IF NEW.rating IS DISTINCT FROM OLD.rating
           OR NEW.total_deliveries IS DISTINCT FROM OLD.total_deliveries THEN
            RAISE EXCEPTION 'Only admins can change delivery partner metrics';
        END IF;
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS prevent_delivery_self_approval ON public.delivery_partners;
CREATE TRIGGER prevent_delivery_self_approval
    BEFORE UPDATE OF is_verified, is_rejected, rating, total_deliveries ON public.delivery_partners
    FOR EACH ROW EXECUTE FUNCTION public.prevent_delivery_self_approval();

CREATE OR REPLACE FUNCTION public.debit_wallet_for_order(
    p_user_id UUID,
    p_amount NUMERIC,
    p_description TEXT DEFAULT 'Order payment'
)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_wallet_id UUID;
BEGIN
    IF p_user_id IS NULL THEN
        RAISE EXCEPTION 'User id is required';
    END IF;

    IF p_amount IS NULL OR p_amount <= 0 THEN
        RAISE EXCEPTION 'Amount must be greater than zero';
    END IF;

    UPDATE public.user_wallets
    SET balance = balance - p_amount,
        updated_at = NOW()
    WHERE user_id = p_user_id
      AND balance >= p_amount
    RETURNING id INTO v_wallet_id;

    IF v_wallet_id IS NULL THEN
        RAISE EXCEPTION 'Insufficient wallet balance';
    END IF;

    INSERT INTO public.wallet_transactions (
        user_id,
        wallet_id,
        amount,
        type,
        description
    ) VALUES (
        p_user_id,
        v_wallet_id,
        p_amount,
        'DEBIT',
        p_description
    );
END;
$$;

REVOKE ALL ON FUNCTION public.debit_wallet_for_order(UUID, NUMERIC, TEXT) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.debit_wallet_for_order(UUID, NUMERIC, TEXT) FROM authenticated;
GRANT EXECUTE ON FUNCTION public.debit_wallet_for_order(UUID, NUMERIC, TEXT) TO service_role;

ALTER TABLE public.orders
    ADD COLUMN IF NOT EXISTS safepay_tracker TEXT;

ALTER TABLE public.payment_transactions
    ADD COLUMN IF NOT EXISTS safepay_tracker TEXT,
    ADD COLUMN IF NOT EXISTS safepay_token TEXT,
    ADD COLUMN IF NOT EXISTS gateway TEXT DEFAULT 'CASH_ON_DELIVERY';

ALTER TABLE public.push_notifications
    ADD COLUMN IF NOT EXISTS recipient_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS order_id UUID REFERENCES public.orders(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_payment_transactions_safepay_tracker
    ON public.payment_transactions(safepay_tracker)
    WHERE safepay_tracker IS NOT NULL;

ALTER TABLE public.orders DROP CONSTRAINT IF EXISTS valid_status;
ALTER TABLE public.orders ADD CONSTRAINT valid_status CHECK (status IN (
    'PENDING',
    'CONFIRMED',
    'PROCESSING',
    'PACKED',
    'READY_FOR_PICKUP',
    'SHIPPED',
    'OUT_FOR_DELIVERY',
    'DELIVERED',
    'CANCELLED',
    'RETURN_REQUESTED',
    'RETURN_APPROVED',
    'RETURN_IN_TRANSIT',
    'RETURN_RECEIVED',
    'RETURNED',
    'RETURN_REJECTED'
));

ALTER TABLE public.orders DROP CONSTRAINT IF EXISTS valid_payment_method;
ALTER TABLE public.orders ADD CONSTRAINT valid_payment_method CHECK (
    payment_method IN ('EASYPAISA', 'JAZZCASH', 'CASH_ON_DELIVERY', 'SAFEPAY', 'VAULT')
);

ALTER TABLE public.payment_transactions DROP CONSTRAINT IF EXISTS valid_payment_method;
ALTER TABLE public.payment_transactions DROP CONSTRAINT IF EXISTS valid_pt_payment_method;
ALTER TABLE public.payment_transactions ADD CONSTRAINT valid_pt_payment_method CHECK (
    payment_method IN ('EASYPAISA', 'JAZZCASH', 'CASH_ON_DELIVERY', 'SAFEPAY', 'VAULT')
);

ALTER TABLE public.payment_transactions DROP CONSTRAINT IF EXISTS valid_transaction_status;
ALTER TABLE public.payment_transactions ADD CONSTRAINT valid_transaction_status CHECK (
    status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'REFUNDED')
);

CREATE OR REPLACE FUNCTION public.prevent_unsafe_order_client_update()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    customer_fields TEXT[] := ARRAY[
        'status',
        'cancellation_reason',
        'return_reason',
        'return_images',
        'updated_at'
    ];
    retailer_fields TEXT[] := ARRAY[
        'status',
        'admin_return_note',
        'payment_status',
        'updated_at'
    ];
    delivery_fields TEXT[] := ARRAY[
        'status',
        'delivery_partner_id',
        'updated_at'
    ];
BEGIN
    IF auth.role() = 'service_role' OR public.is_admin() THEN
        RETURN NEW;
    END IF;

    IF OLD.user_id = auth.uid() THEN
        IF (to_jsonb(OLD) - customer_fields) <> (to_jsonb(NEW) - customer_fields) THEN
            RAISE EXCEPTION 'Customers cannot change protected order fields';
        END IF;

        IF NEW.status = OLD.status THEN
            RETURN NEW;
        END IF;

        IF NEW.status = 'CANCELLED'
           AND OLD.status IN ('PENDING', 'CONFIRMED', 'PROCESSING') THEN
            RETURN NEW;
        END IF;

        IF NEW.status = 'RETURN_REQUESTED'
           AND OLD.status = 'DELIVERED' THEN
            RETURN NEW;
        END IF;

        RAISE EXCEPTION 'This customer order status transition is not allowed';
    END IF;

    IF public.owns_retailer(OLD.retailer_id) THEN
        IF (to_jsonb(OLD) - retailer_fields) <> (to_jsonb(NEW) - retailer_fields) THEN
            RAISE EXCEPTION 'Retailers cannot change protected order fields';
        END IF;

        IF NEW.status = OLD.status THEN
            RETURN NEW;
        END IF;

        IF (OLD.status = 'PENDING' AND NEW.status IN ('CONFIRMED', 'CANCELLED'))
           OR (OLD.status = 'CONFIRMED' AND NEW.status IN ('PROCESSING', 'CANCELLED'))
           OR (OLD.status = 'PROCESSING' AND NEW.status IN ('PACKED', 'CANCELLED'))
           OR (OLD.status = 'PACKED' AND NEW.status = 'READY_FOR_PICKUP')
           OR (
               OLD.status = 'RETURN_REQUESTED'
               AND NEW.status IN ('RETURN_APPROVED', 'RETURN_REJECTED')
           )
           OR (OLD.status = 'RETURN_RECEIVED' AND NEW.status = 'RETURNED') THEN
            RETURN NEW;
        END IF;

        RAISE EXCEPTION 'This retailer order status transition is not allowed';
    END IF;

    IF public.is_verified_delivery_partner() THEN
        IF (to_jsonb(OLD) - delivery_fields) <> (to_jsonb(NEW) - delivery_fields) THEN
            RAISE EXCEPTION 'Delivery partners cannot change protected order fields';
        END IF;

        IF OLD.delivery_partner_id IS NULL
           AND NEW.delivery_partner_id = auth.uid()
           AND (
               (OLD.status = 'READY_FOR_PICKUP' AND NEW.status = 'SHIPPED')
               OR (OLD.status = 'RETURN_APPROVED' AND NEW.status = 'RETURN_IN_TRANSIT')
           ) THEN
            RETURN NEW;
        END IF;

        IF OLD.delivery_partner_id = auth.uid()
           AND NEW.delivery_partner_id = OLD.delivery_partner_id
           AND (
               (OLD.status = 'SHIPPED' AND NEW.status = 'OUT_FOR_DELIVERY')
               OR (OLD.status = 'OUT_FOR_DELIVERY' AND NEW.status = 'DELIVERED')
               OR (OLD.status = 'RETURN_IN_TRANSIT' AND NEW.status = 'RETURN_RECEIVED')
           ) THEN
            RETURN NEW;
        END IF;

        RAISE EXCEPTION 'This delivery order status transition is not allowed';
    END IF;

    RAISE EXCEPTION 'Order update is not allowed';
END;
$$;

DROP TRIGGER IF EXISTS prevent_unsafe_order_client_update ON public.orders;
CREATE TRIGGER prevent_unsafe_order_client_update
    BEFORE UPDATE ON public.orders
    FOR EACH ROW EXECUTE FUNCTION public.prevent_unsafe_order_client_update();

ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
SELECT public.drop_all_policies('public', 'users');
CREATE POLICY "users_select_own_or_admin"
ON public.users FOR SELECT TO authenticated
USING (id = auth.uid() OR public.is_admin());
CREATE POLICY "users_insert_own_customer"
ON public.users FOR INSERT TO authenticated
WITH CHECK (id = auth.uid() AND role = 'customer');
CREATE POLICY "users_update_own_profile_or_admin"
ON public.users FOR UPDATE TO authenticated
USING (id = auth.uid() OR public.is_admin())
WITH CHECK (id = auth.uid() OR public.is_admin());
CREATE POLICY "users_delete_own_or_admin"
ON public.users FOR DELETE TO authenticated
USING (id = auth.uid() OR public.is_admin());

ALTER TABLE public.products ENABLE ROW LEVEL SECURITY;
SELECT public.drop_all_policies('public', 'products');
CREATE POLICY "products_public_read"
ON public.products FOR SELECT TO public
USING (true);
CREATE POLICY "products_insert_admin_or_verified_retailer"
ON public.products FOR INSERT TO authenticated
WITH CHECK (
    public.is_admin()
    OR public.owns_retailer(retailer_id)
);
CREATE POLICY "products_update_admin_or_owner"
ON public.products FOR UPDATE TO authenticated
USING (
    public.is_admin()
    OR public.owns_retailer(retailer_id)
)
WITH CHECK (
    public.is_admin()
    OR public.owns_retailer(retailer_id)
);
CREATE POLICY "products_delete_admin_or_owner"
ON public.products FOR DELETE TO authenticated
USING (
    public.is_admin()
    OR public.owns_retailer(retailer_id)
);

ALTER TABLE public.cart_items ENABLE ROW LEVEL SECURITY;
SELECT public.drop_all_policies('public', 'cart_items');
CREATE POLICY "cart_items_manage_own"
ON public.cart_items FOR ALL TO authenticated
USING (user_id = auth.uid())
WITH CHECK (user_id = auth.uid());

ALTER TABLE public.reviews ENABLE ROW LEVEL SECURITY;
SELECT public.drop_all_policies('public', 'reviews');
CREATE POLICY "reviews_public_read"
ON public.reviews FOR SELECT TO public
USING (true);
CREATE POLICY "reviews_insert_own"
ON public.reviews FOR INSERT TO authenticated
WITH CHECK (user_id = auth.uid());
CREATE POLICY "reviews_update_own"
ON public.reviews FOR UPDATE TO authenticated
USING (user_id = auth.uid())
WITH CHECK (user_id = auth.uid());
CREATE POLICY "reviews_delete_own_or_admin"
ON public.reviews FOR DELETE TO authenticated
USING (user_id = auth.uid() OR public.is_admin());

ALTER TABLE public.retailers ENABLE ROW LEVEL SECURITY;
SELECT public.drop_all_policies('public', 'retailers');
CREATE POLICY "retailers_public_read_verified"
ON public.retailers FOR SELECT TO public
USING (is_verified = true OR user_id = auth.uid() OR public.is_admin());
CREATE POLICY "retailers_insert_own"
ON public.retailers FOR INSERT TO authenticated
WITH CHECK (
    user_id = auth.uid()
    AND COALESCE(is_verified, false) = false
    AND COALESCE(is_rejected, false) = false
);
CREATE POLICY "retailers_update_own_or_admin"
ON public.retailers FOR UPDATE TO authenticated
USING (user_id = auth.uid() OR public.is_admin())
WITH CHECK (user_id = auth.uid() OR public.is_admin());
CREATE POLICY "retailers_delete_admin"
ON public.retailers FOR DELETE TO authenticated
USING (public.is_admin());

ALTER TABLE public.delivery_partners ENABLE ROW LEVEL SECURITY;
SELECT public.drop_all_policies('public', 'delivery_partners');
CREATE POLICY "delivery_select_own_or_admin"
ON public.delivery_partners FOR SELECT TO authenticated
USING (user_id = auth.uid() OR public.is_admin());
CREATE POLICY "delivery_insert_own"
ON public.delivery_partners FOR INSERT TO authenticated
WITH CHECK (
    user_id = auth.uid()
    AND COALESCE(is_verified, false) = false
    AND COALESCE(is_rejected, false) = false
);
CREATE POLICY "delivery_update_own_or_admin"
ON public.delivery_partners FOR UPDATE TO authenticated
USING (user_id = auth.uid() OR public.is_admin())
WITH CHECK (user_id = auth.uid() OR public.is_admin());
CREATE POLICY "delivery_delete_admin"
ON public.delivery_partners FOR DELETE TO authenticated
USING (public.is_admin());

ALTER TABLE public.shipping_addresses ENABLE ROW LEVEL SECURITY;
SELECT public.drop_all_policies('public', 'shipping_addresses');
CREATE POLICY "addresses_select_related"
ON public.shipping_addresses FOR SELECT TO authenticated
USING (
    user_id = auth.uid()
    OR public.is_admin()
    OR EXISTS (
        SELECT 1
        FROM public.orders o
        WHERE o.shipping_address_id = shipping_addresses.id
          AND (
              o.user_id = auth.uid()
              OR public.owns_retailer(o.retailer_id)
              OR public.can_access_delivery_order(o.delivery_partner_id, o.status)
          )
    )
);
CREATE POLICY "addresses_insert_own"
ON public.shipping_addresses FOR INSERT TO authenticated
WITH CHECK (user_id = auth.uid());
CREATE POLICY "addresses_update_own"
ON public.shipping_addresses FOR UPDATE TO authenticated
USING (user_id = auth.uid())
WITH CHECK (user_id = auth.uid());
CREATE POLICY "addresses_delete_own"
ON public.shipping_addresses FOR DELETE TO authenticated
USING (user_id = auth.uid());

ALTER TABLE public.orders ENABLE ROW LEVEL SECURITY;
SELECT public.drop_all_policies('public', 'orders');
CREATE POLICY "orders_select_related"
ON public.orders FOR SELECT TO authenticated
USING (
    user_id = auth.uid()
    OR public.is_admin()
    OR public.owns_retailer(retailer_id)
    OR public.can_access_delivery_order(delivery_partner_id, status)
);
CREATE POLICY "orders_update_admin_retailer_delivery"
ON public.orders FOR UPDATE TO authenticated
USING (
    user_id = auth.uid()
    OR public.is_admin()
    OR public.owns_retailer(retailer_id)
    OR public.can_access_delivery_order(delivery_partner_id, status)
)
WITH CHECK (
    user_id = auth.uid()
    OR public.is_admin()
    OR public.owns_retailer(retailer_id)
    OR public.owns_delivery_partner(delivery_partner_id)
);

ALTER TABLE public.order_items ENABLE ROW LEVEL SECURITY;
SELECT public.drop_all_policies('public', 'order_items');
CREATE POLICY "order_items_select_related"
ON public.order_items FOR SELECT TO authenticated
USING (
    EXISTS (
        SELECT 1
        FROM public.orders o
        WHERE o.id = order_items.order_id
          AND (
              o.user_id = auth.uid()
              OR public.is_admin()
              OR public.owns_retailer(o.retailer_id)
              OR public.can_access_delivery_order(o.delivery_partner_id, o.status)
          )
    )
);

ALTER TABLE public.payment_transactions ENABLE ROW LEVEL SECURITY;
SELECT public.drop_all_policies('public', 'payment_transactions');
CREATE POLICY "payments_select_related"
ON public.payment_transactions FOR SELECT TO authenticated
USING (
    EXISTS (
        SELECT 1
        FROM public.orders o
        WHERE o.id = payment_transactions.order_id
          AND (
              o.user_id = auth.uid()
              OR public.is_admin()
              OR public.owns_retailer(o.retailer_id)
          )
    )
);

ALTER TABLE public.user_wallets ENABLE ROW LEVEL SECURITY;
SELECT public.drop_all_policies('public', 'user_wallets');
CREATE POLICY "wallets_select_own_or_admin"
ON public.user_wallets FOR SELECT TO authenticated
USING (user_id = auth.uid() OR public.is_admin());

ALTER TABLE public.wallet_transactions ENABLE ROW LEVEL SECURITY;
SELECT public.drop_all_policies('public', 'wallet_transactions');
CREATE POLICY "wallet_transactions_select_own_or_admin"
ON public.wallet_transactions FOR SELECT TO authenticated
USING (user_id = auth.uid() OR public.is_admin());

ALTER TABLE public.push_notifications ENABLE ROW LEVEL SECURITY;
SELECT public.drop_all_policies('public', 'push_notifications');
CREATE POLICY "notifications_insert_authorized_sender"
ON public.push_notifications FOR INSERT TO authenticated
WITH CHECK (
    sender_id = auth.uid()
    AND (
        (sender_role = 'admin' AND public.is_admin())
        OR (sender_role = 'retailer' AND public.is_verified_retailer())
        OR (sender_role = 'delivery_partner' AND public.is_verified_delivery_partner())
    )
);
CREATE POLICY "notifications_read_targeted"
ON public.push_notifications FOR SELECT TO authenticated
USING (
    (recipient_role = 'all' AND recipient_id IS NULL)
    OR recipient_id = auth.uid()
    OR (
        recipient_id IS NULL
        AND recipient_role = (
            SELECT role FROM public.users WHERE id = auth.uid()
        )
    )
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_publication_tables
        WHERE pubname = 'supabase_realtime'
          AND schemaname = 'public'
          AND tablename = 'push_notifications'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.push_notifications;
    END IF;
END;
$$;

ALTER TABLE public.user_fcm_tokens ENABLE ROW LEVEL SECURITY;
SELECT public.drop_all_policies('public', 'user_fcm_tokens');
CREATE POLICY "fcm_tokens_manage_own"
ON public.user_fcm_tokens FOR ALL TO authenticated
USING (user_id = auth.uid())
WITH CHECK (user_id = auth.uid());

SELECT public.drop_all_policies('storage', 'objects');

CREATE POLICY "storage_profiles_public_read"
ON storage.objects FOR SELECT TO public
USING (bucket_id = 'profiles');
CREATE POLICY "storage_profiles_insert_own"
ON storage.objects FOR INSERT TO authenticated
WITH CHECK (
    bucket_id = 'profiles'
    AND (storage.foldername(name))[1] = auth.uid()::text
);
CREATE POLICY "storage_profiles_update_own"
ON storage.objects FOR UPDATE TO authenticated
USING (
    bucket_id = 'profiles'
    AND (storage.foldername(name))[1] = auth.uid()::text
)
WITH CHECK (
    bucket_id = 'profiles'
    AND (storage.foldername(name))[1] = auth.uid()::text
);
CREATE POLICY "storage_profiles_delete_own"
ON storage.objects FOR DELETE TO authenticated
USING (
    bucket_id = 'profiles'
    AND (storage.foldername(name))[1] = auth.uid()::text
);

CREATE POLICY "storage_return_images_public_read"
ON storage.objects FOR SELECT TO public
USING (bucket_id = 'return-images');
CREATE POLICY "storage_return_images_insert_order_owner"
ON storage.objects FOR INSERT TO authenticated
WITH CHECK (
    bucket_id = 'return-images'
    AND EXISTS (
        SELECT 1
        FROM public.orders o
        WHERE o.id = public.uuid_or_null((storage.foldername(name))[2])
          AND (o.user_id = auth.uid() OR public.is_admin())
    )
);
CREATE POLICY "storage_return_images_update_order_owner"
ON storage.objects FOR UPDATE TO authenticated
USING (
    bucket_id = 'return-images'
    AND EXISTS (
        SELECT 1
        FROM public.orders o
        WHERE o.id = public.uuid_or_null((storage.foldername(name))[2])
          AND (o.user_id = auth.uid() OR public.is_admin())
    )
)
WITH CHECK (
    bucket_id = 'return-images'
    AND EXISTS (
        SELECT 1
        FROM public.orders o
        WHERE o.id = public.uuid_or_null((storage.foldername(name))[2])
          AND (o.user_id = auth.uid() OR public.is_admin())
    )
);
CREATE POLICY "storage_return_images_delete_order_owner"
ON storage.objects FOR DELETE TO authenticated
USING (
    bucket_id = 'return-images'
    AND EXISTS (
        SELECT 1
        FROM public.orders o
        WHERE o.id = public.uuid_or_null((storage.foldername(name))[2])
          AND (o.user_id = auth.uid() OR public.is_admin())
    )
);

CREATE POLICY "storage_product_images_public_read"
ON storage.objects FOR SELECT TO public
USING (bucket_id = 'product-images');
CREATE POLICY "storage_product_images_write_admin_or_retailer"
ON storage.objects FOR INSERT TO authenticated
WITH CHECK (
    bucket_id = 'product-images'
    AND (
        public.is_admin()
        OR EXISTS (
            SELECT 1 FROM public.retailers
            WHERE user_id = auth.uid()
              AND is_verified = true
        )
    )
);
CREATE POLICY "storage_product_images_update_admin_or_retailer"
ON storage.objects FOR UPDATE TO authenticated
USING (
    bucket_id = 'product-images'
    AND (
        public.is_admin()
        OR EXISTS (
            SELECT 1 FROM public.retailers
            WHERE user_id = auth.uid()
              AND is_verified = true
        )
    )
)
WITH CHECK (
    bucket_id = 'product-images'
    AND (
        public.is_admin()
        OR EXISTS (
            SELECT 1 FROM public.retailers
            WHERE user_id = auth.uid()
              AND is_verified = true
        )
    )
);
CREATE POLICY "storage_product_images_delete_admin"
ON storage.objects FOR DELETE TO authenticated
USING (bucket_id = 'product-images' AND public.is_admin());

DROP FUNCTION IF EXISTS public.drop_all_policies(TEXT, TEXT);

COMMIT;
